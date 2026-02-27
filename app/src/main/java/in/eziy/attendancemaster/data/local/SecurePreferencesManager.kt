package `in`.eziy.attendancemaster.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        // Fallback to standard prefs if crypto initialization fails on legacy ROMs
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trimEnd('/')).apply()

    var companyName: String
        get() = prefs.getString(KEY_COMPANY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_COMPANY_NAME, value).apply()

    var employeeName: String
        get() = prefs.getString(KEY_EMPLOYEE_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMPLOYEE_NAME, value).apply()

    var employeeId: String
        get() = prefs.getString(KEY_EMPLOYEE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMPLOYEE_ID, value.trim()).apply()

    var pin: String
        get() = prefs.getString(KEY_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PIN, value).apply()

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    fun isLoggedIn(): Boolean = token.isNotBlank() && serverUrl.isNotBlank()

    fun saveAuthDetails(
        server: String,
        company: String,
        name: String,
        id: String,
        pinCode: String,
        authToken: String
    ) {
        prefs.edit()
            .putString(KEY_SERVER_URL, server.trimEnd('/'))
            .putString(KEY_COMPANY_NAME, company)
            .putString(KEY_EMPLOYEE_NAME, name)
            .putString(KEY_EMPLOYEE_ID, id.trim())
            .putString(KEY_PIN, pinCode)
            .putString(KEY_TOKEN, authToken)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILENAME = "eziy_secure_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_EMPLOYEE_NAME = "employee_name"
        private const val KEY_EMPLOYEE_ID = "employee_id"
        private const val KEY_PIN = "pin"
        private const val KEY_TOKEN = "token"
    }
}
