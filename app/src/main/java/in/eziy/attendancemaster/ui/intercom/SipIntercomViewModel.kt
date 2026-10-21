package `in`.eziy.attendancemaster.ui.intercom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import `in`.eziy.attendancemaster.data.local.SecurePreferencesManager
import `in`.eziy.attendancemaster.sip.EziyLinphoneManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.linphone.core.Call
import java.util.Locale

data class SipCallLog(
    val title: String,
    val extension: String,
    val timeAgo: String,
    val durationText: String,
    val isOutgoing: Boolean = true,
    val isVideo: Boolean = false
)

data class SpeedDialItem(
    val id: Int,
    val name: String,
    val extension: String,
    val iconEmoji: String = "📞"
)

data class SipContact(
    val name: String,
    val role: String,
    val extension: String,
    val isOnline: Boolean = true
)

data class SipIntercomUiState(
    val companyName: String = "VSPL",
    val sipDomain: String = "",
    val sipUsername: String = "",
    val sipPassword: String = "",
    val targetAddress: String = "reception",
    val isRegistered: Boolean = false,
    val isConnecting: Boolean = false,
    val isCalling: Boolean = false,
    val isCallConnected: Boolean = false,
    val isIncomingCall: Boolean = false,
    val incomingCallerAddress: String = "",
    val isVideoCall: Boolean = false,
    val callDurationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val toastMessage: String? = null,
    val callLogs: List<SipCallLog> = emptyList(),
    val speedDialItems: List<SpeedDialItem> = emptyList(),
    val contacts: List<SipContact> = emptyList()
)

class SipIntercomViewModel(
    private val linphoneManager: EziyLinphoneManager,
    private val prefs: SecurePreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SipIntercomUiState())
    val uiState: StateFlow<SipIntercomUiState> = _uiState.asStateFlow()

    private val gson = Gson()
    private var timerJob: Job? = null

    init {
        setupLinphoneListeners()
        loadInitialData()
    }

    private fun loadInitialData() {
        loadSipCredentials()

        // Company Name from prefs
        val rawComp = prefs.companyName.ifBlank { "VSPL" }
        val shortCompName = rawComp.trim().split(" ").firstOrNull() ?: "VSPL"

        // Persistent 3x3 Speed Dial Items (9 items)
        var speedDialList: List<SpeedDialItem> = emptyList()
        val sdJson = prefs.speedDialJson
        if (sdJson.isNotBlank()) {
            try {
                val type = object : TypeToken<List<SpeedDialItem>>() {}.type
                speedDialList = gson.fromJson(sdJson, type)
            } catch (_: Exception) {}
        }

        if (speedDialList.size < 9) {
            speedDialList = listOf(
                SpeedDialItem(1, "Reception", "reception", "🏢"),
                SpeedDialItem(2, "Front Gate", "gate", "🚪"),
                SpeedDialItem(3, "Security", "security", "🛡️"),
                SpeedDialItem(4, "CEO Office", "101", "💼"),
                SpeedDialItem(5, "IT Support", "102", "💻"),
                SpeedDialItem(6, "Factory Floor", "103", "🏭"),
                SpeedDialItem(7, "HR Desk", "104", "📋"),
                SpeedDialItem(8, "Shipping", "105", "🚚"),
                SpeedDialItem(9, "Warehouse", "106", "📦")
            )
        }

        // Office Directory Contacts List
        val directoryContacts = listOf(
            SipContact("Elena Rostova", "Design Director", "reception", isOnline = true),
            SipContact("Marcus Chen", "VP of Engineering", "101", isOnline = true),
            SipContact("Security Desk", "Front Gate & Patrol", "gate", isOnline = true),
            SipContact("IT Helpdesk", "Systems & Hardware", "102", isOnline = true),
            SipContact("HR Operations", "People & Talent", "104", isOnline = false)
        )

        // Persistent Call Logs from prefs
        var logsList: List<SipCallLog> = emptyList()
        val json = prefs.callLogsJson
        if (json.isNotBlank()) {
            try {
                val type = object : TypeToken<List<SipCallLog>>() {}.type
                logsList = gson.fromJson(json, type)
            } catch (_: Exception) {}
        }

        if (logsList.isEmpty()) {
            logsList = listOf(
                SipCallLog("🏢 Reception Intercom", "reception", "12:42 PM", "02:15 mins", isOutgoing = true),
                SipCallLog("🚪 Front Gate Intercom", "gate", "Yesterday", "01:05 mins", isOutgoing = false),
                SipCallLog("🛡️ Security Desk", "security", "03 Oct", "00:45 mins", isOutgoing = true)
            )
        }

        _uiState.value = _uiState.value.copy(
            companyName = shortCompName,
            speedDialItems = speedDialList,
            contacts = directoryContacts,
            callLogs = logsList
        )
    }

    private fun setupLinphoneListeners() {
        linphoneManager.onRegistrationStateChanged = { isOk, msg ->
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                isRegistered = isOk,
                toastMessage = if (!isOk) "SIP Offline: $msg" else null
            )
        }

        linphoneManager.onCallStateChanged = { state, msg ->
            when (state) {
                Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> {
                    _uiState.value = _uiState.value.copy(
                        isIncomingCall = true,
                        incomingCallerAddress = msg.ifBlank { "Incoming Intercom Call" }
                    )
                }
                Call.State.OutgoingInit, Call.State.OutgoingProgress, Call.State.OutgoingRinging, Call.State.OutgoingEarlyMedia -> {
                    _uiState.value = _uiState.value.copy(isCalling = true)
                }
                Call.State.StreamsRunning, Call.State.Connected, Call.State.UpdatedByRemote -> {
                    if (!_uiState.value.isCallConnected) {
                        _uiState.value = _uiState.value.copy(
                            isCalling = false,
                            isIncomingCall = false,
                            isCallConnected = true,
                            callDurationSeconds = 0
                        )
                        startCallTimer()
                    }
                }
                Call.State.End, Call.State.Released, Call.State.Error -> {
                    val duration = _uiState.value.callDurationSeconds
                    if (duration > 0) {
                        recordCallLog(duration)
                    }
                    timerJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        isCalling = false,
                        isIncomingCall = false,
                        isCallConnected = false,
                        callDurationSeconds = 0
                    )
                }
                else -> {}
            }
        }
    }

    fun updateSpeedDialItem(id: Int, newName: String, newExtension: String, newIcon: String) {
        val updated = _uiState.value.speedDialItems.map { item ->
            if (item.id == id) item.copy(name = newName, extension = newExtension, iconEmoji = newIcon)
            else item
        }
        _uiState.value = _uiState.value.copy(speedDialItems = updated)
        try {
            prefs.speedDialJson = gson.toJson(updated)
        } catch (_: Exception) {}
    }

    fun answerIncomingCall(isVideo: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            isIncomingCall = false,
            isVideoCall = isVideo,
            isCallConnected = true,
            callDurationSeconds = 0
        )
        linphoneManager.answerCall(isVideo)
        startCallTimer()
    }

    fun declineIncomingCall() {
        _uiState.value = _uiState.value.copy(isIncomingCall = false)
        linphoneManager.endCall()
    }

    fun setVideoSurfaces(remoteView: Any?, previewView: Any?) {
        linphoneManager.setVideoSurfaces(remoteView, previewView)
    }

    fun switchCamera() {
        linphoneManager.switchCamera()
    }

    fun toggleMidCallVideo() {
        val newVid = !_uiState.value.isVideoCall
        _uiState.value = _uiState.value.copy(isVideoCall = newVid)
        linphoneManager.toggleVideoMidCall(newVid)
    }

    private fun recordCallLog(durationSec: Int) {
        val mins = durationSec / 60
        val secs = durationSec % 60
        val durationStr = String.format(Locale.US, "%02d:%02d mins", mins, secs)

        val isIncoming = _uiState.value.incomingCallerAddress.isNotBlank() && !_uiState.value.isCalling
        val rawAddress = if (isIncoming) _uiState.value.incomingCallerAddress else _uiState.value.targetAddress.ifBlank { "Intercom Extension" }
        val displayAddress = rawAddress.substringBefore("@")
        val displayTitle = if (displayAddress.contains("reception", ignoreCase = true)) "🏢 Reception Intercom" else "👤 $displayAddress"

        val newLog = SipCallLog(
            title = displayTitle,
            extension = rawAddress,
            timeAgo = "Just now",
            durationText = durationStr,
            isOutgoing = !isIncoming,
            isVideo = _uiState.value.isVideoCall
        )

        val updated = listOf(newLog) + _uiState.value.callLogs
        val persistentList = updated.take(20)
        _uiState.value = _uiState.value.copy(callLogs = persistentList)

        try {
            prefs.callLogsJson = gson.toJson(persistentList)
        } catch (_: Exception) {}
    }

    fun loadSipCredentials() {
        val rawComp = prefs.companyName.ifBlank { "VSPL" }
        val shortCompName = rawComp.trim().split(" ").firstOrNull() ?: "VSPL"

        val domain = prefs.sipDomain
        val user = prefs.sipUsername.ifBlank { prefs.employeeId }
        val pass = prefs.sipPassword.ifBlank { prefs.pin }

        val alreadyRegistered = linphoneManager.isRegistered()

        _uiState.value = _uiState.value.copy(
            companyName = shortCompName,
            sipDomain = domain,
            sipUsername = user,
            sipPassword = pass,
            isRegistered = alreadyRegistered,
            isConnecting = !alreadyRegistered && user.isNotBlank() && domain.isNotBlank()
        )

        if (!alreadyRegistered && user.isNotBlank() && domain.isNotBlank()) {
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
            isIncomingCall = false,
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
