using System.Text.Json.Serialization;

namespace EziyAttendanceMaster.Models;

public class SipCallLog
{
    [JsonPropertyName("title")]
    public string Title { get; set; } = string.Empty;

    [JsonPropertyName("extension")]
    public string Extension { get; set; } = string.Empty;

    [JsonPropertyName("timeAgo")]
    public string TimeAgo { get; set; } = "Just now";

    [JsonPropertyName("durationText")]
    public string DurationText { get; set; } = "00:00 mins";

    [JsonPropertyName("isOutgoing")]
    public bool IsOutgoing { get; set; } = true;

    [JsonPropertyName("isVideo")]
    public bool IsVideo { get; set; } = false;
}

public class SpeedDialItem
{
    [JsonPropertyName("id")]
    public int Id { get; set; }

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    [JsonPropertyName("extension")]
    public string Extension { get; set; } = string.Empty;

    [JsonPropertyName("iconEmoji")]
    public string IconEmoji { get; set; } = "📞";
}

public class SipContact
{
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    [JsonPropertyName("role")]
    public string Role { get; set; } = string.Empty;

    [JsonPropertyName("extension")]
    public string Extension { get; set; } = string.Empty;

    [JsonPropertyName("isOnline")]
    public bool IsOnline { get; set; } = true;
}

