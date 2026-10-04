package `in`.eziy.attendancemaster.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.eziy.attendancemaster.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SetupUiState(
    val serverUrl: String = "",
    val companyName: String = "",
    val employeeName: String = "",
    val employeeId: String = "",
    val pin: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class SetupViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onServerUrlChange(v: String) { _uiState.value = _uiState.value.copy(serverUrl = v) }
    fun onCompanyNameChange(v: String) { _uiState.value = _uiState.value.copy(companyName = v) }
    fun onEmployeeNameChange(v: String) { _uiState.value = _uiState.value.copy(employeeName = v) }
    fun onEmployeeIdChange(v: String) { _uiState.value = _uiState.value.copy(employeeId = v) }
    fun onPinChange(v: String) { _uiState.value = _uiState.value.copy(pin = v) }
    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
    fun resetSuccess() { _uiState.value = _uiState.value.copy(isSuccess = false) }

    fun authenticate() {
        val state = _uiState.value
        val server = state.serverUrl.trim()
        val id = state.employeeId.trim()
        val pinCode = state.pin

        if (!server.startsWith("https://")) {
            _uiState.value = state.copy(errorMessage = "Please enter an HTTPS server URL")
            return
        }
        if (id.length != 8) {
            _uiState.value = state.copy(errorMessage = "Employee ID must be 8 digits")
            return
        }
        if (pinCode.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your Attendance PIN")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.login(server, id, pinCode)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.message ?: "Authentication failed"
                )
            }
        }
    }
}
