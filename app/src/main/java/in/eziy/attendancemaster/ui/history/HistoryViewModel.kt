package `in`.eziy.attendancemaster.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.eziy.attendancemaster.data.remote.dto.AttendanceRecordDto
import `in`.eziy.attendancemaster.data.repository.AttendanceRepository
import `in`.eziy.attendancemaster.util.EziyDateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val records: List<AttendanceRecordDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HistoryViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.getHistory()
            result.onSuccess { response ->
                val formattedRecords = response.attendance.map { rec ->
                    rec.copy(
                        checkIn = EziyDateTimeUtils.formatUtcToLocal(rec.checkIn),
                        checkOut = EziyDateTimeUtils.formatUtcToLocal(rec.checkOut)
                    )
                }
                _uiState.value = _uiState.value.copy(
                    records = formattedRecords,
                    isLoading = false
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    records = emptyList(),
                    isLoading = false,
                    errorMessage = err.message ?: "Failed to load history"
                )
            }
        }
    }
}
