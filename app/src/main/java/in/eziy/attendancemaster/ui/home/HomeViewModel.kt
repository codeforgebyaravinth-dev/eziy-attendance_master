package `in`.eziy.attendancemaster.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.eziy.attendancemaster.data.local.SecurePreferencesManager
import `in`.eziy.attendancemaster.data.repository.AttendanceRepository
import `in`.eziy.attendancemaster.location.EziyLocationClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

data class HomeUiState(
    val employeeName: String = "",
    val companyName: String = "",
    val employeeId: String = "",
    val isCheckedIn: Boolean = false,
    val timeText: String = "Loading status...",
    val lastCheckInTime: String = "—",
    val lastCheckOutTime: String = "—",
    val todayWorkedHours: Double = 0.0,
    val shiftProgress: Float = 0f,
    val isLoadingStatus: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val isSubmittingAttendance: Boolean = false,
    val toastMessage: String? = null,
    val shouldTriggerCamera: Boolean = false
)

class HomeViewModel(
    private val repository: AttendanceRepository,
    private val prefs: SecurePreferencesManager,
    private val locationClient: EziyLocationClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var pendingLocation: Location? = null

    init {
        loadUserInfo()
        refreshStatus()
    }

    fun loadUserInfo() {
        _uiState.value = _uiState.value.copy(
            employeeName = prefs.employeeName.ifBlank { "Employee" },
            companyName = prefs.companyName.ifBlank { "Eziy Enterprise" },
            employeeId = prefs.employeeId
        )
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun resetCameraTrigger() {
        _uiState.value = _uiState.value.copy(shouldTriggerCamera = false)
    }

    fun refreshStatus() {
        _uiState.value = _uiState.value.copy(isLoadingStatus = true)
        viewModelScope.launch {
            val result = repository.getStatus()
            result.onSuccess { status ->
                val checkedIn = status.state == "checked_in"
                val checkInStr = status.lastCheckIn ?: "—"
                val checkOutStr = if (checkedIn) "In Progress (Active Shift)" else (status.lastCheckOut ?: "—")
                val text = if (checkedIn) "Since: $checkInStr" else "Last: $checkOutStr"

                // Estimate progress
                val worked = if (checkedIn) 4.5 else 0.0
                val progress = if (checkedIn) (worked / 8.0).toFloat().coerceIn(0f, 1f) else 0f

                _uiState.value = _uiState.value.copy(
                    isCheckedIn = checkedIn,
                    timeText = text,
                    lastCheckInTime = checkInStr,
                    lastCheckOutTime = checkOutStr,
                    todayWorkedHours = worked,
                    shiftProgress = progress,
                    isLoadingStatus = false
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    timeText = "Status unavailable: ${err.message}",
                    isLoadingStatus = false
                )
            }
        }
    }

    fun beginAttendanceFlow() {
        if (!locationClient.hasLocationPermission()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Camera and Precise Location permissions are required.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isFetchingLocation = true,
            toastMessage = "Acquiring GPS location..."
        )

        viewModelScope.launch {
            val loc = locationClient.fetchCurrentLocation()
            _uiState.value = _uiState.value.copy(isFetchingLocation = false)

            if (loc == null) {
                _uiState.value = _uiState.value.copy(toastMessage = "Fresh GPS location unavailable. Please check GPS settings.")
                return@launch
            }

            pendingLocation = loc
            _uiState.value = _uiState.value.copy(shouldTriggerCamera = true)
        }
    }

    private fun loadCorrectlyOrientedBitmap(filePath: String): Bitmap? {
        val rawBmp = BitmapFactory.decodeFile(filePath) ?: return null
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            if (!matrix.isIdentity) {
                Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, matrix, true)
            } else rawBmp
        } catch (_: Exception) {
            rawBmp
        }
    }

    fun onPhotoCaptured(file: File) {
        val loc = pendingLocation
        if (loc == null) {
            _uiState.value = _uiState.value.copy(toastMessage = "Location data lost. Please try again.")
            return
        }

        _uiState.value = _uiState.value.copy(isSubmittingAttendance = true)

        viewModelScope.launch {
            val base64Image = withContext(Dispatchers.IO) {
                val bmp = loadCorrectlyOrientedBitmap(file.absolutePath) ?: return@withContext null
                val scaled = Bitmap.createScaledBitmap(bmp, 640, 480, true)
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
                "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            }

            if (base64Image == null) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingAttendance = false,
                    toastMessage = "Failed to process photo"
                )
                return@launch
            }

            val result = repository.markAttendance(
                base64Image = base64Image,
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracy = loc.accuracy
            )

            result.onSuccess { resp ->
                val name = resp.employeeName ?: prefs.employeeName
                val statusStr = resp.status ?: "Recorded"
                _uiState.value = _uiState.value.copy(
                    isSubmittingAttendance = false,
                    toastMessage = "$name — $statusStr\nGPS Saved"
                )
                refreshStatus()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSubmittingAttendance = false,
                    toastMessage = err.message ?: "Attendance submission failed"
                )
            }
        }
    }
}
