package `in`.eziy.attendancemaster.sip

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import org.linphone.core.*

class EziyLinphoneManager(private val context: Context) {

    private val factory: Factory = Factory.instance()
    private var core: Core? = null
    private var ringtonePlayer: Ringtone? = null

    var onRegistrationStateChanged: ((Boolean, String) -> Unit)? = null
    var onCallStateChanged: ((Call.State?, String) -> Unit)? = null

    init {
        try {
            factory.setDebugMode(true, "EziyLinphone")
            val linphoneCore = factory.createCore(null, null, context)

            // Enable Linphone Video Policy & Self View Preview
            linphoneCore.isVideoCaptureEnabled = true
            linphoneCore.isVideoDisplayEnabled = true
            linphoneCore.isSelfViewEnabled = true

            // Configure Instant Video Activation Policy
            val videoPolicy = linphoneCore.videoActivationPolicy
            if (videoPolicy != null) {
                videoPolicy.automaticallyAccept = true
                videoPolicy.automaticallyInitiate = true
                linphoneCore.videoActivationPolicy = videoPolicy
            }

            // Tune Video Performance (30 FPS, 1500 kbps, 640x480)
            linphoneCore.preferredFramerate = 30.0f
            linphoneCore.uploadBandwidth = 1500
            linphoneCore.downloadBandwidth = 1500

            val vdef = linphoneCore.preferredVideoDefinition
            if (vdef != null) {
                vdef.width = 640
                vdef.height = 480
                linphoneCore.preferredVideoDefinition = vdef
            }

            // Enable VP8 and H264 Video Codecs
            try {
                linphoneCore.videoPayloadTypes.forEach { payload ->
                    if (payload.mimeType.equals("VP8", ignoreCase = true) ||
                        payload.mimeType.equals("H264", ignoreCase = true)
                    ) {
                        payload.enable(true)
                    }
                }
            } catch (_: Exception) {}

            // Select Front Camera Device
            val cameras = linphoneCore.videoDevicesList
            if (cameras.isNotEmpty()) {
                val frontCam = cameras.firstOrNull { it.contains("front", ignoreCase = true) } ?: cameras.first()
                linphoneCore.videoDevice = frontCam
                Log.d("EziyLinphone", "Selected video device: $frontCam")
            }

            // Configure NAT Policy: Disable blocking STUN/ICE for Flexisip symmetric NAT proxy routing
            val natPolicy = linphoneCore.createNatPolicy()
            if (natPolicy != null) {
                natPolicy.isStunEnabled = false
                natPolicy.isIceEnabled = false
                linphoneCore.natPolicy = natPolicy
            }

            linphoneCore.addListener(object : CoreListenerStub() {
                override fun onAccountRegistrationStateChanged(
                    core: Core,
                    account: Account,
                    state: RegistrationState?,
                    message: String
                ) {
                    val isOk = state == RegistrationState.Ok
                    val msg = message.ifBlank { if (isOk) "Online" else "Disconnected" }
                    Log.d("EziyLinphone", "SIP Registration: $state ($msg)")
                    onRegistrationStateChanged?.invoke(isOk, msg)
                }

                override fun onCallStateChanged(
                    core: Core,
                    call: Call,
                    state: Call.State?,
                    message: String
                ) {
                    Log.d("EziyLinphone", "SIP Call State: ${state?.name} ($message)")

                    if (state == Call.State.IncomingReceived || state == Call.State.IncomingEarlyMedia) {
                        playRingtone()
                    } else if (state == Call.State.Connected || state == Call.State.StreamsRunning || state == Call.State.End || state == Call.State.Released || state == Call.State.Error) {
                        stopRingtone()
                    }

                    onCallStateChanged?.invoke(state, message)
                }
            })

            linphoneCore.start()
            core = linphoneCore
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Linphone Core initialization failed: ${e.message}", e)
        }
    }

    fun playRingtone() {
        try {
            if (ringtonePlayer == null) {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ringtonePlayer = RingtoneManager.getRingtone(context, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtonePlayer?.isLooping = true
                }
            }
            ringtonePlayer?.play()
            Log.d("EziyLinphone", "System Ringtone Playing...")
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Error playing ringtone: ${e.message}", e)
        }
    }

    fun stopRingtone() {
        try {
            ringtonePlayer?.stop()
            ringtonePlayer = null
            Log.d("EziyLinphone", "System Ringtone Stopped.")
        } catch (_: Exception) {}
    }

    fun isRegistered(): Boolean {
        val account = core?.defaultAccount ?: return false
        return account.state == RegistrationState.Ok
    }

    fun setVideoSurfaces(remoteView: Any?, previewView: Any?) {
        try {
            val linphoneCore = core ?: return
            if (remoteView != null) linphoneCore.nativeVideoWindowId = remoteView
            if (previewView != null) linphoneCore.nativePreviewWindowId = previewView
            linphoneCore.isSelfViewEnabled = previewView != null
            linphoneCore.reloadVideoDevices()
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Error setting video surfaces: ${e.message}", e)
        }
    }

    fun register(user: String, domain: String, pass: String) {
        val linphoneCore = core ?: return
        try {
            unregister()

            val authInfo = factory.createAuthInfo(user, null, pass, null, null, domain)
            linphoneCore.addAuthInfo(authInfo)

            val params = linphoneCore.createAccountParams()
            val identity = factory.createAddress("sip:$user@$domain") ?: return
            params.identityAddress = identity

            // Use TransportType.Tls or Tcp for sip.linphone.org
            val serverUri = if (domain.contains("linphone.org")) "sip:$domain;transport=tls" else "sip:$domain"
            val serverAddr = factory.createAddress(serverUri) ?: factory.createAddress("sip:$domain") ?: return
            params.serverAddress = serverAddr
            params.isRegisterEnabled = true
            params.expires = 3600

            val account = linphoneCore.createAccount(params)
            linphoneCore.addAccount(account)
            linphoneCore.defaultAccount = account
            Log.d("EziyLinphone", "Registering $user@$domain...")
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Registration error: ${e.message}", e)
            onRegistrationStateChanged?.invoke(false, e.message ?: "Registration error")
        }
    }

    fun unregister() {
        val linphoneCore = core ?: return
        try {
            val account = linphoneCore.defaultAccount
            if (account != null) {
                val params = account.params.clone()
                params.isRegisterEnabled = false
                account.params = params
            }
            linphoneCore.clearAccounts()
            linphoneCore.clearAllAuthInfo()
        } catch (_: Exception) {}
    }

    fun makeCall(targetInput: String, isVideo: Boolean = false) {
        val linphoneCore = core ?: return
        try {
            val currentAccount = linphoneCore.defaultAccount
            val domain = currentAccount?.params?.identityAddress?.domain ?: "sip.linphone.org"

            var fullAddress = targetInput.trim()
            if (!fullAddress.startsWith("sip:")) {
                fullAddress = if (fullAddress.contains("@")) "sip:$fullAddress" else "sip:$fullAddress@$domain"
            }

            Log.d("EziyLinphone", "Dialing target SIP URI: $fullAddress (Video: $isVideo)")
            val address = linphoneCore.interpretUrl(fullAddress) ?: return
            val params = linphoneCore.createCallParams(null)
            if (params != null) {
                params.isVideoEnabled = isVideo
                linphoneCore.inviteAddressWithParams(address, params)
            }
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Make call error: ${e.message}", e)
        }
    }

    fun toggleVideoMidCall(enableVideo: Boolean) {
        val linphoneCore = core ?: return
        try {
            val currentCall = linphoneCore.currentCall ?: return
            val params = linphoneCore.createCallParams(currentCall)
            if (params != null) {
                params.isVideoEnabled = enableVideo
                currentCall.update(params)
                Log.d("EziyLinphone", "Toggled mid-call video: $enableVideo")
            }
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Error toggling mid-call video: ${e.message}", e)
        }
    }

    fun answerCall(isVideo: Boolean = false) {
        val linphoneCore = core ?: return
        try {
            stopRingtone()
            val call = linphoneCore.currentCall
            if (call != null) {
                val params = linphoneCore.createCallParams(call)
                if (params != null) {
                    val enableVid = isVideo || call.remoteParams?.isVideoEnabled == true
                    params.isVideoEnabled = enableVid
                    call.acceptWithParams(params)
                } else {
                    call.accept()
                }
            }
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Error answering call: ${e.message}", e)
        }
    }

    fun endCall() {
        try {
            stopRingtone()
            core?.currentCall?.terminate()
        } catch (_: Exception) {}
    }

    fun switchCamera() {
        try {
            val linphoneCore = core ?: return
            val currentDevice = linphoneCore.videoDevice
            val cameras = linphoneCore.videoDevicesList
            if (cameras.size > 1) {
                val nextCam = cameras.firstOrNull { it != currentDevice } ?: cameras.first()
                linphoneCore.videoDevice = nextCam
                Log.d("EziyLinphone", "Switched camera device to: $nextCam")
            }
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Error switching camera: ${e.message}", e)
        }
    }

    fun setMicrophoneMuted(muted: Boolean) {
        try {
            core?.isMicEnabled = !muted
        } catch (_: Exception) {}
    }

    fun setSpeakerphoneEnabled(speakerOn: Boolean) {
        try {
            val linphoneCore = core ?: return
            val targetType = if (speakerOn) AudioDevice.Type.Speaker else AudioDevice.Type.Earpiece
            val targetDevice = linphoneCore.audioDevices.firstOrNull { it.type == targetType }
            if (targetDevice != null) {
                linphoneCore.outputAudioDevice = targetDevice
            }
        } catch (_: Exception) {}
    }
}
