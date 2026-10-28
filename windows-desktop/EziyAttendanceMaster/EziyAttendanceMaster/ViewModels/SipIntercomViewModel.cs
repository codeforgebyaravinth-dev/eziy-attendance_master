using System;
using System.Collections.ObjectModel;
using System.Text.Json;
using System.Windows.Threading;
using CommunityToolkit.Mvvm.Input;
using EziyAttendanceMaster.Linphone;
using EziyAttendanceMaster.Models;
using EziyAttendanceMaster.Services;
using Linphone;

namespace EziyAttendanceMaster.ViewModels;

public class SipIntercomViewModel : ViewModelBase
{
    private readonly EziyLinphoneManager _linphoneManager;
    private readonly ISecureStorageService _prefs;
    private readonly DispatcherTimer _callTimer;

    private string _companyName = "VSPL";
    private string _sipDomain = "sip.linphone.org";
    private string _sipUsername = "";
    private string _sipPassword = "";
    private string _targetAddress = "reception";

    private bool _isRegistered;
    private bool _isConnecting;
    private bool _isCalling;
    private bool _isCallConnected;
    private bool _isIncomingCall;
    private string _incomingCallerAddress = "";
    private bool _isVideoCall;
    private int _callDurationSeconds;
    private bool _isMuted;
    private bool _isSpeakerOn;
    private string? _toastMessage;

    public ObservableCollection<SpeedDialItem> SpeedDialItems { get; } = new();
    public ObservableCollection<SipContact> Contacts { get; } = new();
    public ObservableCollection<SipCallLog> CallLogs { get; } = new();

    public string CompanyName
    {
        get => _companyName;
        set => SetProperty(ref _companyName, value);
    }

    public string SipDomain
    {
        get => _sipDomain;
        set => SetProperty(ref _sipDomain, value);
    }

    public string SipUsername
    {
        get => _sipUsername;
        set => SetProperty(ref _sipUsername, value);
    }

    public string SipPassword
    {
        get => _sipPassword;
        set => SetProperty(ref _sipPassword, value);
    }

    public string TargetAddress
    {
        get => _targetAddress;
        set => SetProperty(ref _targetAddress, value);
    }

    public bool IsRegistered
    {
        get => _isRegistered;
        set => SetProperty(ref _isRegistered, value);
    }

    public bool IsConnecting
    {
        get => _isConnecting;
        set => SetProperty(ref _isConnecting, value);
    }

    public bool IsCalling
    {
        get => _isCalling;
        set => SetProperty(ref _isCalling, value);
    }

    public bool IsCallConnected
    {
        get => _isCallConnected;
        set => SetProperty(ref _isCallConnected, value);
    }

    public bool IsIncomingCall
    {
        get => _isIncomingCall;
        set => SetProperty(ref _isIncomingCall, value);
    }

    public string IncomingCallerAddress
    {
        get => _incomingCallerAddress;
        set => SetProperty(ref _incomingCallerAddress, value);
    }

    public bool IsVideoCall
    {
        get => _isVideoCall;
        set => SetProperty(ref _isVideoCall, value);
    }

    public int CallDurationSeconds
    {
        get => _callDurationSeconds;
        set
        {
            if (SetProperty(ref _callDurationSeconds, value))
            {
                OnPropertyChanged(nameof(CallDurationFormatted));
            }
        }
    }

    public string CallDurationFormatted
    {
        get
        {
            var m = CallDurationSeconds / 60;
            var s = CallDurationSeconds % 60;
            return $"{m:D2}:{s:D2}";
        }
    }

    public bool IsMuted
    {
        get => _isMuted;
        set => SetProperty(ref _isMuted, value);
    }

    public bool IsSpeakerOn
    {
        get => _isSpeakerOn;
        set => SetProperty(ref _isSpeakerOn, value);
    }

    public string? ToastMessage
    {
        get => _toastMessage;
        set => SetProperty(ref _toastMessage, value);
    }

    public bool IsCallActive => IsCalling || IsCallConnected || IsIncomingCall;

    public IRelayCommand ConnectSipCommand { get; }
    public IRelayCommand DisconnectSipCommand { get; }
    public IRelayCommand<string?> StartAudioCallCommand { get; }
    public IRelayCommand<string?> StartVideoCallCommand { get; }
    public IRelayCommand AnswerAudioCallCommand { get; }
    public IRelayCommand AnswerVideoCallCommand { get; }
    public IRelayCommand HangUpCommand { get; }
    public IRelayCommand ToggleMuteCommand { get; }
    public IRelayCommand ToggleSpeakerCommand { get; }
    public IRelayCommand ToggleVideoMidCallCommand { get; }

    public SipIntercomViewModel(EziyLinphoneManager linphoneManager, ISecureStorageService prefs)
    {
        _linphoneManager = linphoneManager;
        _prefs = prefs;

        _callTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
        _callTimer.Tick += (s, e) => CallDurationSeconds++;

        ConnectSipCommand = new RelayCommand(ConnectSip);
        DisconnectSipCommand = new RelayCommand(DisconnectSip);
        StartAudioCallCommand = new RelayCommand<string?>(ext => StartCall(ext, isVideo: false));
        StartVideoCallCommand = new RelayCommand<string?>(ext => StartCall(ext, isVideo: true));
        AnswerAudioCallCommand = new RelayCommand(() => AnswerCall(isVideo: false));
        AnswerVideoCallCommand = new RelayCommand(() => AnswerCall(isVideo: true));
        HangUpCommand = new RelayCommand(HangUp);
        ToggleMuteCommand = new RelayCommand(ToggleMute);
        ToggleSpeakerCommand = new RelayCommand(ToggleSpeaker);
        ToggleVideoMidCallCommand = new RelayCommand(ToggleVideoMidCall);

        SetupLinphoneCallbacks();
        LoadInitialData();
    }

    private void SetupLinphoneCallbacks()
    {
        _linphoneManager.OnRegistrationStateChanged = (isOk, msg) =>
        {
            App.Current.Dispatcher.Invoke(() =>
            {
                IsRegistered = isOk;
                IsConnecting = false;
                ToastMessage = isOk ? "SIP Connected" : $"SIP Error: {msg}";
            });
        };

        _linphoneManager.OnCallStateChanged = (state, caller) =>
        {
            App.Current.Dispatcher.Invoke(() =>
            {
                switch (state)
                {
                    case Call.State.IncomingReceived:
                    case Call.State.IncomingEarlyMedia:
                        IsIncomingCall = true;
                        IncomingCallerAddress = caller;
                        IsCalling = false;
                        IsCallConnected = false;
                        OnPropertyChanged(nameof(IsCallActive));
                        break;

                    case Call.State.OutgoingInit:
                    case Call.State.OutgoingProgress:
                    case Call.State.OutgoingRinging:
                    case Call.State.OutgoingEarlyMedia:
                        IsCalling = true;
                        IsIncomingCall = false;
                        IsCallConnected = false;
                        OnPropertyChanged(nameof(IsCallActive));
                        break;

                    case Call.State.Connected:
                    case Call.State.StreamsRunning:
                        IsCalling = false;
                        IsIncomingCall = false;
                        IsCallConnected = true;
                        _callTimer.Start();
                        OnPropertyChanged(nameof(IsCallActive));
                        break;

                    case Call.State.End:
                    case Call.State.Released:
                    case Call.State.Error:
                        _callTimer.Stop();
                        RecordCallLog();
                        IsCalling = false;
                        IsIncomingCall = false;
                        IsCallConnected = false;
                        CallDurationSeconds = 0;
                        OnPropertyChanged(nameof(IsCallActive));
                        break;
                }
            });
        };
    }

    private void LoadInitialData()
    {
        var rawComp = _prefs.CompanyName;
        CompanyName = string.IsNullOrWhiteSpace(rawComp) ? "VSPL" : rawComp.Split(' ')[0];

        SipDomain = string.IsNullOrWhiteSpace(_prefs.SipDomain) ? "sip.linphone.org" : _prefs.SipDomain;
        SipUsername = string.IsNullOrWhiteSpace(_prefs.SipUsername) ? _prefs.EmployeeId : _prefs.SipUsername;
        SipPassword = string.IsNullOrWhiteSpace(_prefs.SipPassword) ? _prefs.Pin : _prefs.SipPassword;

        // Populate 3x3 Speed Dial Items (9 items)
        SpeedDialItems.Clear();
        SpeedDialItems.Add(new SpeedDialItem { Id = 1, Name = "Reception", Extension = "reception", IconEmoji = "🏢" });
        SpeedDialItems.Add(new SpeedDialItem { Id = 2, Name = "Front Gate", Extension = "gate", IconEmoji = "🚪" });
        SpeedDialItems.Add(new SpeedDialItem { Id = 3, Name = "Security", Extension = "security", IconEmoji = "🛡️" });
        SpeedDialItems.Add(new SpeedDialItem { Id = 4, Name = "CEO Office", Extension = "101", IconEmoji = "💼" });
        SpeedDialItems.Add(new SpeedDialItem { Id = 5, Name = "IT Support", Extension = "102", IconEmoji = "💻" });
        SpeedDialItems.Add(new SpeedDialItem { Id = 6, Name = "Factory Floor", Extension = "103", IconEmoji = "🏭" });
        SpeedDialItems.Add(new SpeedDialItem { Id = 7, Name = "HR Desk", Extension = "104", IconEmoji = "📋" });
        SpeedDialItems.Add(new SpeedDialItem { Id = 8, Name = "Shipping", Extension = "105", IconEmoji = "🚚" });
        SpeedDialItems.Add(new SpeedDialItem { Id = 9, Name = "Warehouse", Extension = "106", IconEmoji = "📦" });

        // Directory Contacts
        Contacts.Clear();
        Contacts.Add(new SipContact { Name = "Elena Rostova", Role = "Design Director", Extension = "reception", IsOnline = true });
        Contacts.Add(new SipContact { Name = "Marcus Chen", Role = "VP of Engineering", Extension = "101", IsOnline = true });
        Contacts.Add(new SipContact { Name = "Security Desk", Role = "Front Gate & Patrol", Extension = "gate", IsOnline = true });
        Contacts.Add(new SipContact { Name = "IT Helpdesk", Role = "Systems & Hardware", Extension = "102", IsOnline = true });
        Contacts.Add(new SipContact { Name = "HR Operations", Role = "People & Talent", Extension = "104", IsOnline = false });

        if (_linphoneManager.IsRegistered())
        {
            IsRegistered = true;
        }
        else if (!string.IsNullOrWhiteSpace(SipUsername) && !string.IsNullOrWhiteSpace(SipDomain))
        {
            ConnectSip();
        }
    }

    public void ConnectSip()
    {
        var domain = SipDomain.Trim();
        var user = SipUsername.Trim();
        var pass = SipPassword.Trim();

        if (string.IsNullOrWhiteSpace(domain) || string.IsNullOrWhiteSpace(user))
        {
            ToastMessage = "Please enter Domain & Extension ID";
            return;
        }

        IsConnecting = true;
        _prefs.SaveSipDetails(domain, user, pass);
        _linphoneManager.Register(user, domain, pass);
    }

    public void DisconnectSip()
    {
        _linphoneManager.Unregister();
        _prefs.ClearSipDetails();
        HangUp();
        IsRegistered = false;
        ToastMessage = "SIP Disconnected";
    }

    public void StartCall(string? targetExtension, bool isVideo)
    {
        var target = (targetExtension ?? TargetAddress).Trim();
        if (string.IsNullOrWhiteSpace(target))
        {
            ToastMessage = "Please enter a target extension";
            return;
        }

        TargetAddress = target;
        IsCalling = true;
        IsVideoCall = isVideo;
        OnPropertyChanged(nameof(IsCallActive));

        _linphoneManager.MakeCall(target, isVideo);
    }

    public void AnswerCall(bool isVideo)
    {
        IsVideoCall = isVideo;
        _linphoneManager.AnswerCall(isVideo);
    }

    public void HangUp()
    {
        _callTimer.Stop();
        _linphoneManager.EndCall();
        IsCalling = false;
        IsIncomingCall = false;
        IsCallConnected = false;
        CallDurationSeconds = 0;
        OnPropertyChanged(nameof(IsCallActive));
    }

    public void ToggleMute()
    {
        IsMuted = !IsMuted;
        _linphoneManager.SetMicrophoneMuted(IsMuted);
    }

    public void ToggleSpeaker()
    {
        IsSpeakerOn = !IsSpeakerOn;
        _linphoneManager.SetSpeakerphoneEnabled(IsSpeakerOn);
    }

    public void ToggleVideoMidCall()
    {
        IsVideoCall = !IsVideoCall;
        _linphoneManager.ToggleVideoMidCall(IsVideoCall);
    }

    private void RecordCallLog()
    {
        var duration = CallDurationFormatted;
        var address = !string.IsNullOrWhiteSpace(IncomingCallerAddress) ? IncomingCallerAddress : TargetAddress;
        CallLogs.Insert(0, new SipCallLog
        {
            Title = address.Contains("reception", StringComparison.OrdinalIgnoreCase) ? "🏢 Reception" : $"👤 {address}",
            Extension = address,
            TimeAgo = "Just now",
            DurationText = $"{duration} mins",
            IsOutgoing = string.IsNullOrWhiteSpace(IncomingCallerAddress),
            IsVideo = IsVideoCall
        });
    }
}

