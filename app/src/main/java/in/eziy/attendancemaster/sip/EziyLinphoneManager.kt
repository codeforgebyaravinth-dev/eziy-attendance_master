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
                    onCallStateChanged?.invoke(stateStr, message)
                }
            })
            linphoneCore.start()
            core = linphoneCore
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Linphone Core initialization failed: ${e.message}", e)
        }
    }

    fun register(user: String, domain: String, pass: String) {
        val linphoneCore = core ?: return
        try {
            val authInfo = factory.createAuthInfo(user, null, pass, null, null, domain)
            linphoneCore.addAuthInfo(authInfo)

            val params = linphoneCore.createAccountParams()
            val identity = factory.createAddress("sip:$user@$domain") ?: return
            params.identityAddress = identity

            val serverAddr = factory.createAddress("sip:$domain") ?: return
            params.serverAddress = serverAddr
            params.isRegisterEnabled = true

            val account = linphoneCore.createAccount(params)
            linphoneCore.addAccount(account)
            linphoneCore.defaultAccount = account
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

    fun makeCall(targetSipAddress: String, isVideo: Boolean = false) {
        val linphoneCore = core ?: return
        try {
            val address = linphoneCore.interpretUrl(targetSipAddress) ?: return
            val params = linphoneCore.createCallParams(null)
            if (params != null) {
                params.isVideoEnabled = isVideo
                linphoneCore.inviteAddressWithParams(address, params)
            }
        } catch (e: Exception) {
            Log.e("EziyLinphone", "Make call error: ${e.message}", e)
        }
    }

    fun endCall() {
        try {
            core?.currentCall?.terminate()
        } catch (_: Exception) {}
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
