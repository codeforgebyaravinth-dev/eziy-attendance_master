package `in`.eziy.attendancemaster.ui.settings

import androidx.lifecycle.ViewModel
import `in`.eziy.attendancemaster.data.local.SecurePreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val serverUrl: String = "",
    val companyName: String = "",
    val employeeName: String = "",
    val employeeId: String = "",
    val sipDomain: String = "sip.linphone.org",
    val sipUsername: String = "",
    val sipPassword: String = "",
    val isSaved: Boolean = false,
    val isLoggedOut: Boolean = false
)

class SettingsViewModel(
    private val prefs: SecurePreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        _uiState.value = SettingsUiState(
            serverUrl = prefs.serverUrl,
            companyName = prefs.companyName,
            employeeName = prefs.employeeName,
            employeeId = prefs.employeeId,
            sipDomain = prefs.sipDomain.ifBlank { "sip.linphone.org" },
            sipUsername = prefs.sipUsername,
            sipPassword = prefs.sipPassword,
            isSaved = false,
            isLoggedOut = false
        )
    }

    fun onServerUrlChange(v: String) { _uiState.value = _uiState.value.copy(serverUrl = v) }
    fun onCompanyNameChange(v: String) { _uiState.value = _uiState.value.copy(companyName = v) }
    fun onEmployeeNameChange(v: String) { _uiState.value = _uiState.value.copy(employeeName = v) }
    fun onSipDomainChange(v: String) { _uiState.value = _uiState.value.copy(sipDomain = v) }
    fun onSipUsernameChange(v: String) { _uiState.value = _uiState.value.copy(sipUsername = v) }
    fun onSipPasswordChange(v: String) { _uiState.value = _uiState.value.copy(sipPassword = v) }

    fun saveSettings() {
        prefs.serverUrl = _uiState.value.serverUrl
        prefs.companyName = _uiState.value.companyName
        prefs.employeeName = _uiState.value.employeeName
        prefs.saveSipDetails(
            _uiState.value.sipDomain,
            _uiState.value.sipUsername,
            _uiState.value.sipPassword
        )
        _uiState.value = _uiState.value.copy(isSaved = true)
    }

    fun resetSaveState() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }

    fun logout() {
        prefs.clear()
        _uiState.value = _uiState.value.copy(isLoggedOut = true)
    }
}
