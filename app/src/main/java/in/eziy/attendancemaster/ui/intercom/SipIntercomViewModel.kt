package `in`.eziy.attendancemaster.ui.intercom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.eziy.attendancemaster.data.local.SecurePreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SipIntercomUiState(
    val sipDomain: String = "sip.linphone.org",
    val sipUsername: String = "",
    val sipPassword: String = "",
    val targetAddress: String = "sip:reception@sip.linphone.org",
    val isRegistered: Boolean = false,
    val isConnecting: Boolean = false,
    val isCalling: Boolean = false,
    val isCallConnected: Boolean = false,
    val callDurationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val toastMessage: String? = null
)

class SipIntercomViewModel(
    private val prefs: SecurePreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SipIntercomUiState())
    val uiState: StateFlow<SipIntercomUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadSipCredentials()
    }

    fun loadSipCredentials() {
        val domain = prefs.sipDomain.ifBlank { "sip.linphone.org" }
        val user = prefs.sipUsername
        val pass = prefs.sipPassword
        val registered = prefs.isSipRegistered()

        _uiState.value = _uiState.value.copy(
            sipDomain = domain,
            sipUsername = user,
            sipPassword = pass,
            isRegistered = registered
        )
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

        if (domain.isBlank()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please enter SIP Server Domain/IP")
            return
        }
        if (user.isBlank()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please enter SIP Extension/Username")
            return
        }

        _uiState.value = _uiState.value.copy(isConnecting = true)

        viewModelScope.launch {
            delay(1200) // Simulates SIP REGISTER request
            prefs.saveSipDetails(domain, user, pass)
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                isRegistered = true,
                toastMessage = "SIP Online: $user@$domain"
            )
        }
    }

    fun disconnectSip() {
        prefs.clearSipDetails()
        hangUp()
        _uiState.value = _uiState.value.copy(
            isRegistered = false,
            sipUsername = "",
            sipPassword = "",
            toastMessage = "SIP Intercom Offline"
        )
    }

    fun startCall() {
        val target = _uiState.value.targetAddress.trim()
        if (target.isBlank()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please enter target SIP extension")
            return
        }

        _uiState.value = _uiState.value.copy(isCalling = true)

        viewModelScope.launch {
            delay(1800) // Simulates SIP INVITE dialing
            _uiState.value = _uiState.value.copy(
                isCalling = false,
                isCallConnected = true,
                callDurationSeconds = 0
            )
            startCallTimer()
        }
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
        _uiState.value = _uiState.value.copy(
            isCalling = false,
            isCallConnected = false,
            callDurationSeconds = 0,
            toastMessage = "Call Ended"
        )
    }

    fun toggleMute() {
        _uiState.value = _uiState.value.copy(isMuted = !_uiState.value.isMuted)
    }

    fun toggleSpeaker() {
        _uiState.value = _uiState.value.copy(isSpeakerOn = !_uiState.value.isSpeakerOn)
    }
}
