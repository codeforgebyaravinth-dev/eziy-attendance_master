using System;
using System.IO;
using System.Linq;
using System.Media;
using System.Threading;
using Linphone;

namespace EziyAttendanceMaster.Linphone;

public class EziyLinphoneManager : IDisposable
{
    private Factory? _factory;
    private Core? _core;
    private Timer? _iterateTimer;
    private SoundPlayer? _ringtonePlayer;

    public Action<bool, string>? OnRegistrationStateChanged { get; set; }
    public Action<Call.State, string>? OnCallStateChanged { get; set; }

    public bool IsInitialized => _core != null;

    public EziyLinphoneManager()
    {
        InitializeLinphoneCore();
    }

    private void InitializeLinphoneCore()
    {
        try
        {
            _factory = Factory.Instance;
            _factory.SetDebugMode(true, "EziyLinphone");

            var appData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            var linphoneDir = Path.Combine(appData, "EziyAttendanceMaster", "linphone");
            Directory.CreateDirectory(linphoneDir);

            var configPath = Path.Combine(linphoneDir, "linphonerc");
            var factoryPath = Path.Combine(linphoneDir, "linphone-factory");

            _core = _factory.CreateCore(configPath, factoryPath, IntPtr.Zero);

            // Enable Linphone Video Policy & Self View Preview
            _core.VideoCaptureEnabled = true;
            _core.VideoDisplayEnabled = true;
            _core.SelfViewEnabled = true;

            // Configure Instant Video Activation Policy
            var videoPolicy = _core.VideoActivationPolicy;
            if (videoPolicy != null)
            {
                videoPolicy.AutomaticallyAccept = true;
                videoPolicy.AutomaticallyInitiate = true;
                _core.VideoActivationPolicy = videoPolicy;
            }

            // Tune Video Performance (30 FPS, 1500 kbps, 640x480)
            _core.PreferredFramerate = 30.0f;
            _core.UploadBandwidth = 1500;
            _core.DownloadBandwidth = 1500;

            var vdef = _core.PreferredVideoDefinition;
            if (vdef != null)
            {
                vdef.Width = 640;
                vdef.Height = 480;
                _core.PreferredVideoDefinition = vdef;
            }

            // Enable VP8 and H264 Codecs
            try
            {
                foreach (var payload in _core.VideoPayloadTypes)
                {
                    if (string.Equals(payload.MimeType, "VP8", StringComparison.OrdinalIgnoreCase) ||
                        string.Equals(payload.MimeType, "H264", StringComparison.OrdinalIgnoreCase))
                    {
                        payload.Enable(true);
                    }
                }
            }
            catch { }

            // Configure NAT Policy: Disable blocking STUN/ICE for Flexisip symmetric NAT routing
            var natPolicy = _core.CreateNatPolicy();
            if (natPolicy != null)
            {
                natPolicy.StunEnabled = false;
                natPolicy.IceEnabled = false;
                _core.NatPolicy = natPolicy;
            }

            // Hook Event Listeners
            _core.AccountRegistrationStateChanged += (Core c, Account account, RegistrationState state, string message) =>
            {
                bool isOk = state == RegistrationState.Ok;
                string msg = string.IsNullOrWhiteSpace(message) ? (isOk ? "Online" : "Disconnected") : message;
                System.Diagnostics.Debug.WriteLine($"SIP Registration: {state} ({msg})");
                OnRegistrationStateChanged?.Invoke(isOk, msg);
            };

            _core.CallStateChanged += (Core c, Call call, Call.State state, string message) =>
            {
                var callerAddress = call.RemoteAddress?.Username;
                if (string.IsNullOrWhiteSpace(callerAddress))
                {
                    callerAddress = call.RemoteAddress?.AsStringUriOnly() ?? message;
                }

                System.Diagnostics.Debug.WriteLine($"SIP Call State: {state} ({callerAddress})");

                if (state is Call.State.IncomingReceived or Call.State.IncomingEarlyMedia)
                {
                    PlayRingtone();
                }
                else if (state is Call.State.Connected or Call.State.StreamsRunning or Call.State.End or Call.State.Released or Call.State.Error)
                {
                    StopRingtone();
                }

                OnCallStateChanged?.Invoke(state, callerAddress);
            };

            _core.Start();

            // Run Linphone Core iterate loop on Windows every 20ms
            _iterateTimer = new Timer(_ =>
            {
                try
                {
                    _core?.Iterate();
                }
                catch { }
            }, null, 0, 20);
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Linphone Core init error: {ex.Message}");
        }
    }

    public void PlayRingtone()
    {
        try
        {
            if (_ringtonePlayer == null)
            {
                _ringtonePlayer = new SoundPlayer();
                // Play Windows system ringtone sound or default beep
                SystemSounds.Beep.Play();
            }
        }
        catch { }
    }

    public void StopRingtone()
    {
        try
        {
            _ringtonePlayer?.Stop();
            _ringtonePlayer = null;
        }
        catch { }
    }

    public bool IsRegistered()
    {
        var account = _core?.DefaultAccount;
        if (account == null) return false;
        return account.State == RegistrationState.Ok;
    }

    public void SetVideoSurfaces(IntPtr remoteViewHwnd, IntPtr previewViewHwnd)
    {
        if (_core == null) return;
        try
        {
            if (remoteViewHwnd != IntPtr.Zero)
            {
                _core.NativeVideoWindowId = remoteViewHwnd;
            }
            if (previewViewHwnd != IntPtr.Zero)
            {
                _core.NativePreviewWindowId = previewViewHwnd;
                _core.SelfViewEnabled = true;
            }
            _core.ReloadVideoDevices();
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Error setting video surfaces: {ex.Message}");
        }
    }

    public void Register(string user, string domain, string pass)
    {
        if (_core == null || _factory == null) return;
        try
        {
            Unregister();

            var authInfo = _factory.CreateAuthInfo(user, null, pass, null, null, domain);
            _core.AddAuthInfo(authInfo);

            var param = _core.CreateAccountParams();
            var identity = _factory.CreateAddress($"sip:{user}@{domain}");
            if (identity == null) return;
            param.IdentityAddress = identity;

            var serverUri = domain.Contains("linphone.org", StringComparison.OrdinalIgnoreCase)
                ? $"sip:{domain};transport=tls"
                : $"sip:{domain}";

            var serverAddr = _factory.CreateAddress(serverUri) ?? _factory.CreateAddress($"sip:{domain}");
            if (serverAddr == null) return;

            param.ServerAddress = serverAddr;
            param.RegisterEnabled = true;
            param.Expires = 3600;

            var account = _core.CreateAccount(param);
            _core.AddAccount(account);
            _core.DefaultAccount = account;

            System.Diagnostics.Debug.WriteLine($"Registering SIP: {user}@{domain}...");
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"SIP Registration error: {ex.Message}");
            OnRegistrationStateChanged?.Invoke(false, ex.Message);
        }
    }

    public void Unregister()
    {
        if (_core == null) return;
        try
        {
            var account = _core.DefaultAccount;
            if (account != null)
            {
                var p = account.Params.Clone();
                p.RegisterEnabled = false;
                account.Params = p;
            }
            _core.ClearAccounts();
            _core.ClearAllAuthInfo();
        }
        catch { }
    }

    public void MakeCall(string targetInput, bool isVideo = false)
    {
        if (_core == null) return;
        try
        {
            var currentAccount = _core.DefaultAccount;
            var domain = currentAccount?.Params?.IdentityAddress?.Domain ?? "sip.linphone.org";

            var fullAddress = targetInput.Trim();
            if (!fullAddress.StartsWith("sip:", StringComparison.OrdinalIgnoreCase))
            {
                fullAddress = fullAddress.Contains('@') ? $"sip:{fullAddress}" : $"sip:{fullAddress}@{domain}";
            }

            System.Diagnostics.Debug.WriteLine($"Dialing SIP: {fullAddress} (Video: {isVideo})");
            var address = _core.InterpretUrl(fullAddress);
            if (address == null) return;

            var callParams = _core.CreateCallParams(null);
            if (callParams != null)
            {
                callParams.VideoEnabled = isVideo;
                _core.InviteAddressWithParams(address, callParams);
            }
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Make call error: {ex.Message}");
        }
    }

    public void ToggleVideoMidCall(bool enableVideo)
    {
        if (_core == null) return;
        try
        {
            var currentCall = _core.CurrentCall;
            if (currentCall == null) return;

            var callParams = _core.CreateCallParams(currentCall);
            if (callParams != null)
            {
                callParams.VideoEnabled = enableVideo;
                currentCall.Update(callParams);
            }
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Error toggling video: {ex.Message}");
        }
    }

    public void AnswerCall(bool isVideo = false)
    {
        if (_core == null) return;
        try
        {
            StopRingtone();
            var call = _core.CurrentCall;
            if (call != null)
            {
                var callParams = _core.CreateCallParams(call);
                if (callParams != null)
                {
                    bool enableVid = isVideo || (call.RemoteParams?.VideoEnabled == true);
                    callParams.VideoEnabled = enableVid;
                    call.AcceptWithParams(callParams);
                }
                else
                {
                    call.Accept();
                }
            }
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Error answering call: {ex.Message}");
        }
    }

    public void EndCall()
    {
        try
        {
            StopRingtone();
            _core?.CurrentCall?.Terminate();
        }
        catch { }
    }

    public void SwitchCamera()
    {
        if (_core == null) return;
        try
        {
            var currentDevice = _core.VideoDevice;
            var cameras = _core.VideoDevicesList.ToList();
            if (cameras.Count > 1)
            {
                var nextCam = cameras.FirstOrDefault(c => c != currentDevice) ?? cameras.First();
                _core.VideoDevice = nextCam;
            }
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Error switching camera: {ex.Message}");
        }
    }

    public void SetMicrophoneMuted(bool muted)
    {
        try
        {
            if (_core != null)
            {
                _core.MicEnabled = !muted;
            }
        }
        catch { }
    }

    public void SetSpeakerphoneEnabled(bool speakerOn)
    {
        // On Windows Desktop, Linphone automatically directs to active default audio output device
    }

    public void Dispose()
    {
        _iterateTimer?.Dispose();
        StopRingtone();
        try
        {
            _core?.Stop();
        }
        catch { }
    }
}

