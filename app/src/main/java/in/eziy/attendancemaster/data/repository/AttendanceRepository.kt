package `in`.eziy.attendancemaster.data.repository

import `in`.eziy.attendancemaster.data.remote.dto.*

interface AttendanceRepository {
    suspend fun login(baseUrl: String, employeeId: String, pin: String): Result<LoginResponse>
    suspend fun getStatus(): Result<StatusResponse>
    suspend fun getHistory(): Result<HistoryResponse>
    suspend fun markAttendance(
        base64Image: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ): Result<MarkAttendanceResponse>
}
