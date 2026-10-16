package `in`.eziy.attendancemaster.sip

import android.content.Context
import android.util.Log
import org.linphone.core.*

class EziyLinphoneManager(private val context: Context) {

    private val factory: Factory = Factory.instance()
    private var core: Core? = null

    var onRegistrationStateChanged: ((Boolean, String) -> Unit)? = null
    var onCallStateChanged: ((String, String) -> Unit)? = null

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

            // Configure STUN/ICE NAT Policy for cellular & Wi-Fi NAT traversal
            val natPolicy = linphoneCore.createNatPolicy()
            if (natPolicy != null) {
                natPolicy.stunServer = "stun.linphone.org"
                natPolicy.isStunEnabled = true
                natPolicy.isIceEnabled = true
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
                    val stateStr = state?.name ?: "Idle"
                    Log.d("EziyLinphone", "SIP Call State: $stateStr ($message)")

                    if (state == Call.State.StreamsRunning || state == Call.State.Connected) {
                        try {
                            if (call.currentParams.isVideoEnabled) {
                                val params = core.createCallParams(call)
                                params?.isVideoEnabled = true
                                params?.videoDirection = MediaDirection.SendRecv
                                call.update(params)
                            }
                        } catch (_: Exception) {}
                    }

                    onCallStateChanged?.invoke(stateStr, message)
                }
            })

            linphoneCore.start()
            core = linphoneCore
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Linphone Core initialization failed: ${e.message}", e)
        }
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
                if (isVideo) {
                    params.videoDirection = MediaDirection.SendRecv
                }
                linphoneCore.inviteAddressWithParams(address, params)
            }
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Make call error: ${e.message}", e)
        }
    }

    fun answerCall(isVideo: Boolean = false) {
        val linphoneCore = core ?: return
        try {
            val call = linphoneCore.currentCall
            if (call != null) {
                val params = linphoneCore.createCallParams(call)
                if (params != null) {
                    val enableVid = isVideo || call.remoteParams?.isVideoEnabled == true
                    params.isVideoEnabled = enableVid
                    if (enableVid) {
                        params.videoDirection = MediaDirection.SendRecv
                    }
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
