package `in`.eziy.attendancemaster.fakes

import `in`.eziy.attendancemaster.data.remote.dto.*
import `in`.eziy.attendancemaster.data.repository.AttendanceRepository

class FakeAttendanceRepository : AttendanceRepository {

    var shouldReturnError = false
    var mockStatusState = "checked_out"
    val mockAttendanceList = mutableListOf(
        AttendanceRecordDto("Today 09:00 AM", "05:00 PM", 8.0),
        AttendanceRecordDto("Yesterday 08:55 AM", "05:30 PM", 8.5)
    )

    override suspend fun login(
        baseUrl: String,
        employeeId: String,
        pin: String
    ): Result<LoginResponse> {
        return if (shouldReturnError) {
            Result.failure(Exception("Invalid credentials"))
        } else {
            Result.success(
                LoginResponse(
                    success = true,
                    message = "Success",
                    token = "fake_jwt_token_123",
                    employee = EmployeeDto("Test Employee", "Test Corp", employeeId)
                )
            )
        }
    }

    override suspend fun getStatus(): Result<StatusResponse> {
        return if (shouldReturnError) {
            Result.failure(Exception("Network error"))
        } else {
            Result.success(
                StatusResponse(
                    success = true,
                    state = mockStatusState,
                    lastCheckIn = "09:00 AM",
                    lastCheckOut = "05:00 PM",
                    message = "Success"
                )
            )
        }
    }

    override suspend fun getHistory(): Result<HistoryResponse> {
        return if (shouldReturnError) {
            Result.failure(Exception("Could not load history"))
        } else {
            Result.success(
                HistoryResponse(
                    success = true,
                    attendance = mockAttendanceList,
                    message = "Success"
                )
            )
        }
    }

    override suspend fun markAttendance(
        base64Image: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ): Result<MarkAttendanceResponse> {
        return if (shouldReturnError) {
            Result.failure(Exception("Submission failed"))
        } else {
            mockStatusState = if (mockStatusState == "checked_in") "checked_out" else "checked_in"
            Result.success(
                MarkAttendanceResponse(
                    success = true,
                    message = "Attendance marked",
                    status = mockStatusState,
                    employeeName = "Test Employee"
                )
            )
        }
    }
}
