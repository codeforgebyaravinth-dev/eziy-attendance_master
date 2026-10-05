package `in`.eziy.attendancemaster.util

import java.text.SimpleDateFormat
import java.util.*

object EziyDateTimeUtils {

    fun formatUtcToLocal(utcTimeString: String?): String {
        if (utcTimeString.isNullOrBlank() || utcTimeString == "—") return "—"
        if (utcTimeString.startsWith("Today") || utcTimeString.startsWith("Yesterday") || utcTimeString.startsWith("Since") || utcTimeString.startsWith("Last")) {
            return utcTimeString
        }

        return try {
            // Odoo ISO-8601 format e.g. "2026-10-03T03:53:20" or "2026-10-03 03:53:20"
            val cleanStr = utcTimeString.replace("Z", "").replace(" ", "T")
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = inputFormat.parse(cleanStr) ?: return utcTimeString

            val outputFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            outputFormat.format(date)
        } catch (_: Exception) {
            utcTimeString
        }
    }

    fun calculateElapsedHours(utcCheckInString: String?): Double {
        if (utcCheckInString.isNullOrBlank() || utcCheckInString == "—") return 0.0
        return try {
            val cleanStr = utcCheckInString.replace("Z", "").replace(" ", "T")
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = inputFormat.parse(cleanStr) ?: return 0.0
            val now = Date()
            val diffMs = now.time - date.time
            if (diffMs <= 0) return 0.0
            val hours = diffMs.toDouble() / (1000.0 * 60 * 60)
            Math.round(hours * 10.0) / 10.0
        } catch (_: Exception) {
            0.0
        }
    }
}
