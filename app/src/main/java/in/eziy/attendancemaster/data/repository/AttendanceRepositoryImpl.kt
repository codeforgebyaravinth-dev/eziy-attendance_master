package `in`.eziy.attendancemaster.data.repository

import `in`.eziy.attendancemaster.data.local.SecurePreferencesManager
import `in`.eziy.attendancemaster.data.remote.ApiService
import `in`.eziy.attendancemaster.data.remote.dto.*
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AttendanceRepositoryImpl(
    private val prefs: SecurePreferencesManager
) : AttendanceRepository {

    private var mockCheckedIn = false
    private var lastPunchTimeStr = "Today at 09:00 AM"

    private fun isDemoMode(url: String? = null): Boolean {
        val target = (url ?: prefs.serverUrl).lowercase()
        return target.contains("demo")
    }

    private fun currentTime(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    private fun cleanBaseUrl(rawUrl: String): String {
        var cleaned = rawUrl.trim().trimEnd('/')
        if (cleaned.lowercase().endsWith("/face_attendance")) {
            cleaned = cleaned.substring(0, cleaned.length - "/face_attendance".length)
        }
        return cleaned.trimEnd('/') + "/"
    }

    private fun <T> parseOdooErrorMessage(response: Response<T>): String? {
        return try {
            val errorJson = response.errorBody()?.string()
            if (!errorJson.isNullOrBlank()) {
                val json = JSONObject(errorJson)
                json.optString("message").ifBlank { null }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun getApiService(baseUrl: String? = null): ApiService {
        val raw = (baseUrl ?: prefs.serverUrl).ifBlank { "https://eziy.in" }
        val targetUrl = cleanBaseUrl(raw)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(targetUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun bearerToken(): String = "Bearer ${prefs.token}"

    override suspend fun login(
        baseUrl: String,
        employeeId: String,
        pin: String
    ): Result<LoginResponse> {
        if (isDemoMode(baseUrl)) {
            delay(800)
            val mockCompany = "Eziy Demo Corp"
            val mockName = "Demo Employee"
            val mockToken = "mock_eziy_jwt_token_123456"

            prefs.saveAuthDetails(
                server = baseUrl,
                company = mockCompany,
                name = mockName,
                id = employeeId,
                pinCode = pin,
                authToken = mockToken
            )

            return Result.success(
                LoginResponse(
                    success = true,
                    message = "Demo Login Successful",
                    token = mockToken,
                    employee = EmployeeDto(mockName, mockCompany, employeeId)
                )
            )
        }

        return try {
            val api = getApiService(baseUrl)
            val response = api.login(employeeId, pin)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.success && !body.token.isNullOrBlank()) {
                    val cleanedServer = cleanBaseUrl(baseUrl).trimEnd('/')
                    prefs.saveAuthDetails(
                        server = cleanedServer,
                        company = body.employee?.companyName ?: "",
                        name = body.employee?.name ?: "",
                        id = employeeId,
                        pinCode = pin,
                        authToken = body.token
                    )
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.message ?: "Login failed"))
                }
            } else {
                val errorMsg = parseOdooErrorMessage(response) ?: "Authentication failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStatus(): Result<StatusResponse> {
        if (isDemoMode()) {
            delay(500)
            val state = if (mockCheckedIn) "checked_in" else "checked_out"
            return Result.success(
                StatusResponse(
                    success = true,
                    state = state,
                    lastCheckIn = if (mockCheckedIn) lastPunchTimeStr else "Today at 09:00 AM",
                    lastCheckOut = if (!mockCheckedIn) lastPunchTimeStr else "—",
                    message = "Demo status retrieved"
                )
            )
        }

        return try {
            val api = getApiService()
            val response = api.getStatus(bearerToken())
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMsg = parseOdooErrorMessage(response) ?: "Could not fetch status (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistory(): Result<HistoryResponse> {
        if (isDemoMode()) {
            delay(600)
            val mockList = listOf(
                AttendanceRecordDto(
                    checkIn = "Today " + (if (mockCheckedIn) lastPunchTimeStr else "09:00 AM"),
                    checkOut = if (mockCheckedIn) "—" else ("Today " + lastPunchTimeStr),
                    workedHours = if (mockCheckedIn) 4.5 else 8.0
                ),
                AttendanceRecordDto(checkIn = "Yesterday 08:58 AM", checkOut = "05:30 PM", workedHours = 8.5),
                AttendanceRecordDto(checkIn = "2 days ago 09:05 AM", checkOut = "05:15 PM", workedHours = 8.2),
                AttendanceRecordDto(checkIn = "3 days ago 08:50 AM", checkOut = "05:40 PM", workedHours = 8.8)
            )
            return Result.success(
                HistoryResponse(
                    success = true,
                    attendance = mockList,
                    message = "Demo history loaded"
                )
            )
        }

        return try {
            val api = getApiService()
            val response = api.getHistory(bearerToken())
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMsg = parseOdooErrorMessage(response) ?: "Could not fetch history (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAttendance(
        base64Image: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ): Result<MarkAttendanceResponse> {
        if (isDemoMode()) {
            delay(1000)
            mockCheckedIn = !mockCheckedIn
            lastPunchTimeStr = currentTime()
            val statusStr = if (mockCheckedIn) "CHECKED IN" else "CHECKED OUT"
            return Result.success(
                MarkAttendanceResponse(
                    success = true,
                    message = "Demo Attendance Marked",
                    status = statusStr,
                    employeeName = prefs.employeeName.ifBlank { "Demo Employee" }
                )
            )
        }

        return try {
            val api = getApiService()
            val response = api.markAttendance(
                bearerToken = bearerToken(),
                faceImage = base64Image,
                latitude = latitude.toString(),
                longitude = longitude.toString(),
                gpsAccuracy = accuracy.toString()
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.message ?: "Attendance failed"))
                }
            } else {
                val errorMsg = parseOdooErrorMessage(response) ?: "Attendance failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
