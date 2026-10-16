package `in`.eziy.attendancemaster.ui.intercom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.eziy.attendancemaster.data.local.SecurePreferencesManager
import `in`.eziy.attendancemaster.sip.EziyLinphoneManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class SipCallLog(
    val title: String,
    val extension: String,
    val timeAgo: String,
    val durationText: String,
    val isOutgoing: Boolean = true,
    val isVideo: Boolean = false
)

data class SipIntercomUiState(
    val sipDomain: String = "sip.linphone.org",
    val sipUsername: String = "",
    val sipPassword: String = "",
    val targetAddress: String = "reception",
    val isRegistered: Boolean = false,
    val isConnecting: Boolean = false,
    val isCalling: Boolean = false,
    val isCallConnected: Boolean = false,
    val isVideoCall: Boolean = false,
    val callDurationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val toastMessage: String? = null,
    val callLogs: List<SipCallLog> = emptyList()
)

class SipIntercomViewModel(
    private val linphoneManager: EziyLinphoneManager,
    private val prefs: SecurePreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SipIntercomUiState())
    val uiState: StateFlow<SipIntercomUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        setupLinphoneListeners()
        loadInitialData()
    }

    private fun loadInitialData() {
        loadSipCredentials()
        // Default Call Logs
        val defaultLogs = listOf(
            SipCallLog("🏢 Reception Intercom", "reception@sip.linphone.org", "12:42 PM", "02:15 mins", isOutgoing = true),
            SipCallLog("🚪 Front Gate Intercom", "gate@sip.linphone.org", "Yesterday", "01:05 mins", isOutgoing = false),
            SipCallLog("🛡️ Security Desk", "security@sip.linphone.org", "03 Oct", "00:45 mins", isOutgoing = true)
        )
        _uiState.value = _uiState.value.copy(callLogs = defaultLogs)
    }

    private fun setupLinphoneListeners() {
        linphoneManager.onRegistrationStateChanged = { isOk, msg ->
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                isRegistered = isOk,
                toastMessage = if (isOk) "SIP Online: $msg" else "SIP Offline: $msg"
            )
        }

        linphoneManager.onCallStateChanged = { stateStr, _ ->
            when (stateStr) {
                "OutgoingInit", "OutgoingProgress", "OutgoingRinging" -> {
                    _uiState.value = _uiState.value.copy(isCalling = true)
                }
                "StreamsRunning", "Connected" -> {
                    _uiState.value = _uiState.value.copy(
                        isCalling = false,
                        isCallConnected = true,
                        callDurationSeconds = 0
                    )
                    startCallTimer()
                }
                "End", "Released", "Error" -> {
                    val duration = _uiState.value.callDurationSeconds
                    if (duration > 0) {
                        recordCallLog(duration)
                    }
                    timerJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        isCalling = false,
                        isCallConnected = false,
                        callDurationSeconds = 0,
                        toastMessage = "Call $stateStr"
                    )
                }
            }
        }
    }

    private fun recordCallLog(durationSec: Int) {
        val mins = durationSec / 60
        val secs = durationSec % 60
        val durationStr = String.format(Locale.US, "%02d:%02d mins", mins, secs)
        val target = _uiState.value.targetAddress.ifBlank { "Intercom Extension" }

        val newLog = SipCallLog(
            title = if (target.contains("reception")) "🏢 Reception Intercom" else "📞 $target",
            extension = target,
            timeAgo = "Just now",
            durationText = durationStr,
            isOutgoing = true,
            isVideo = _uiState.value.isVideoCall
        )

        val updated = listOf(newLog) + _uiState.value.callLogs
        _uiState.value = _uiState.value.copy(callLogs = updated.take(10))
    }

    fun loadSipCredentials() {
        val domain = prefs.sipDomain.ifBlank { "sip.linphone.org" }
        val user = prefs.sipUsername
        val pass = prefs.sipPassword

        _uiState.value = _uiState.value.copy(
            sipDomain = domain,
            sipUsername = user,
            sipPassword = pass
        )

        if (user.isNotBlank()) {
            connectSip()
        }
    }

    fun onDomainChanged(v: String) { _uiState.value = _uiState.value.copy(sipDomain = v) }
    fun onUsernameChanged(v: String) { _uiState.value = _uiState.value.copy(sipUsername = v) }
    fun onPasswordChanged(v: String) { _uiState.value = _uiState.value.copy(sipPassword = v) }
    fun onTargetAddressChanged(v: String) { _uiState.value = _uiState.value.copy(targetAddress = v) }
    fun clearToast() { _uiState.value = _uiState.value.copy(toastMessage = null) }

    fun connectSip() {
        val domain = _uiState.value.sipDomain.trim()
        val user = _uiState.value.sipUsername.trim()
        val pass = _uiState.value.sipPassword.trim()

        if (domain.isBlank() || user.isBlank()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please enter Domain & Extension ID")
            return
        }

        _uiState.value = _uiState.value.copy(isConnecting = true)
        prefs.saveSipDetails(domain, user, pass)
        linphoneManager.register(user, domain, pass)
    }

    fun disconnectSip() {
        linphoneManager.unregister()
        prefs.clearSipDetails()
        hangUp()
        _uiState.value = _uiState.value.copy(
            isRegistered = false,
            sipUsername = "",
            sipPassword = "",
            toastMessage = "SIP Disconnected"
        )
    }

    fun startCall(targetExtension: String? = null, isVideo: Boolean = false) {
        val target = (targetExtension ?: _uiState.value.targetAddress).trim()
        if (target.isBlank()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please enter target extension")
            return
        }

        _uiState.value = _uiState.value.copy(targetAddress = target, isCalling = true, isVideoCall = isVideo)
        linphoneManager.makeCall(target, isVideo)
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isCallConnected) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    callDurationSeconds = _uiState.value.callDurationSeconds + 1
                )
            }
        }
    }

    fun hangUp() {
        timerJob?.cancel()
        linphoneManager.endCall()
        _uiState.value = _uiState.value.copy(
            isCalling = false,
            isCallConnected = false,
            callDurationSeconds = 0
        )
    }

    fun toggleMute() {
        val newMute = !_uiState.value.isMuted
        _uiState.value = _uiState.value.copy(isMuted = newMute)
        linphoneManager.setMicrophoneMuted(newMute)
    }

    fun toggleSpeaker() {
        val newSpeaker = !_uiState.value.isSpeakerOn
        _uiState.value = _uiState.value.copy(isSpeakerOn = newSpeaker)
        linphoneManager.setSpeakerphoneEnabled(newSpeaker)
    }
}
