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

    var sipDomain: String
        get() = prefs.getString(KEY_SIP_DOMAIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SIP_DOMAIN, value.trim()).apply()

    var sipUsername: String
        get() = prefs.getString(KEY_SIP_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SIP_USERNAME, value.trim()).apply()

    var sipPassword: String
        get() = prefs.getString(KEY_SIP_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SIP_PASSWORD, value).apply()

    var callLogsJson: String
        get() = prefs.getString(KEY_CALL_LOGS_JSON, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CALL_LOGS_JSON, value).apply()

    var speedDialJson: String
        get() = prefs.getString(KEY_SPEED_DIAL_JSON, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SPEED_DIAL_JSON, value).apply()

    fun isSipRegistered(): Boolean = sipUsername.isNotBlank() && sipDomain.isNotBlank()

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

    fun saveSipDetails(domain: String, user: String, pass: String) {
        prefs.edit()
            .putString(KEY_SIP_DOMAIN, domain.trim())
            .putString(KEY_SIP_USERNAME, user.trim())
            .putString(KEY_SIP_PASSWORD, pass)
            .apply()
    }

    fun clearSipDetails() {
        prefs.edit()
            .remove(KEY_SIP_DOMAIN)
            .remove(KEY_SIP_USERNAME)
            .remove(KEY_SIP_PASSWORD)
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
        private const val KEY_SIP_DOMAIN = "sip_domain"
        private const val KEY_SIP_USERNAME = "sip_username"
        private const val KEY_SIP_PASSWORD = "sip_password"
        private const val KEY_CALL_LOGS_JSON = "call_logs_json"
        private const val KEY_SPEED_DIAL_JSON = "speed_dial_json"
    }
}
