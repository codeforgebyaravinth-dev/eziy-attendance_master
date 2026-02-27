package `in`.eziy.attendancemaster.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StatusResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("state") val state: String?,
    @SerializedName("last_check_in") val lastCheckIn: String?,
    @SerializedName("last_check_out") val lastCheckOut: String?,
    @SerializedName("message") val message: String?
)
