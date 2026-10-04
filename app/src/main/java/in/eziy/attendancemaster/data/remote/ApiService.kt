package `in`.eziy.attendancemaster.data.remote

import `in`.eziy.attendancemaster.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST("face_attendance/api/login")
    suspend fun login(
        @Field("employee_id") employeeId: String,
        @Field("pin") pin: String
    ): Response<LoginResponse>

    @POST("face_attendance/api/status")
    suspend fun getStatus(
        @Header("Authorization") bearerToken: String
    ): Response<StatusResponse>

    @FormUrlEncoded
    @POST("face_attendance/api/history")
    suspend fun getHistory(
        @Header("Authorization") bearerToken: String,
        @Field("limit") limit: String = "60"
    ): Response<HistoryResponse>

    @FormUrlEncoded
    @POST("face_attendance/api/mark")
    suspend fun markAttendance(
        @Header("Authorization") bearerToken: String,
        @Field("face_image") faceImage: String,
        @Field("latitude") latitude: String,
        @Field("longitude") longitude: String,
        @Field("gps_accuracy") gpsAccuracy: String
    ): Response<MarkAttendanceResponse>
}
