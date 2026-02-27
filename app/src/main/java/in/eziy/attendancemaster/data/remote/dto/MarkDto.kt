package `in`.eziy.attendancemaster.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MarkAttendanceRequest(
    @SerializedName("face_image") val faceImage: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("gps_accuracy") val gpsAccuracy: String
)

data class MarkAttendanceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("employee_name") val employeeName: String?
)
