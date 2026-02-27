package `in`.eziy.attendancemaster.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("pin") val pin: String
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("token") val token: String?,
    @SerializedName("employee") val employee: EmployeeDto?
)

data class EmployeeDto(
    @SerializedName("name") val name: String?,
    @SerializedName("company_name") val companyName: String?,
    @SerializedName("employee_id") val employeeId: String?
)
