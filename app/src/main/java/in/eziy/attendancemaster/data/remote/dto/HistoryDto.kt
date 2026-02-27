package `in`.eziy.attendancemaster.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HistoryResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("attendance") val attendance: List<AttendanceRecordDto> = emptyList(),
    @SerializedName("message") val message: String?
)

data class AttendanceRecordDto(
    @SerializedName("check_in") val checkIn: String?,
    @SerializedName("check_out") val checkOut: String?,
    @SerializedName("worked_hours") val workedHours: Double?
)
