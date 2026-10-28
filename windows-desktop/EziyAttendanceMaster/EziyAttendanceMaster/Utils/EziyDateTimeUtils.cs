using System;
using System.Globalization;

namespace EziyAttendanceMaster.Utils;

public static class EziyDateTimeUtils
{
    public static string FormatUtcToLocal(string? utcTimeString)
    {
        if (string.IsNullOrWhiteSpace(utcTimeString) || utcTimeString == "—")
            return "—";

        if (utcTimeString.StartsWith("Today", StringComparison.OrdinalIgnoreCase) ||
            utcTimeString.StartsWith("Yesterday", StringComparison.OrdinalIgnoreCase) ||
            utcTimeString.StartsWith("Since", StringComparison.OrdinalIgnoreCase) ||
            utcTimeString.StartsWith("Last", StringComparison.OrdinalIgnoreCase))
        {
            return utcTimeString;
        }

        try
        {
            // Odoo ISO-8601 format e.g. "2026-10-03T03:53:20" or "2026-10-03 03:53:20"
            string cleanStr = utcTimeString.Replace("Z", "", StringComparison.OrdinalIgnoreCase).Trim();
            if (cleanStr.Contains(' '))
            {
                cleanStr = cleanStr.Replace(' ', 'T');
            }

            if (DateTime.TryParse(cleanStr, CultureInfo.InvariantCulture, DateTimeStyles.AdjustToUniversal, out var parsedUtc))
            {
                var local = parsedUtc.ToLocalTime();
                return local.ToString("dd MMM, hh:mm tt", CultureInfo.InvariantCulture);
            }

            return utcTimeString;
        }
        catch
        {
            return utcTimeString;
        }
    }

    public static double CalculateElapsedHours(string? utcCheckInString)
    {
        if (string.IsNullOrWhiteSpace(utcCheckInString) || utcCheckInString == "—")
            return 0.0;

        try
        {
            string cleanStr = utcCheckInString.Replace("Z", "", StringComparison.OrdinalIgnoreCase).Trim();
            if (cleanStr.Contains(' '))
            {
                cleanStr = cleanStr.Replace(' ', 'T');
            }

            if (DateTime.TryParse(cleanStr, CultureInfo.InvariantCulture, DateTimeStyles.AdjustToUniversal, out var parsedUtc))
            {
                var nowUtc = DateTime.UtcNow;
                var diff = nowUtc - parsedUtc;
                if (diff.TotalMilliseconds <= 0) return 0.0;
                return Math.Round(diff.TotalHours, 1);
            }

            return 0.0;
        }
        catch
        {
            return 0.0;
        }
    }
}

