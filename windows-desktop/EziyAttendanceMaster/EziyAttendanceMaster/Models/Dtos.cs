using System.Text.Json.Serialization;

namespace EziyAttendanceMaster.Models;

public class LoginRequest
{
    [JsonPropertyName("employee_id")]
    public string EmployeeId { get; set; } = string.Empty;

    [JsonPropertyName("pin")]
    public string Pin { get; set; } = string.Empty;
}

public class LoginResponse
{
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    [JsonPropertyName("message")]
    public string? Message { get; set; }

    [JsonPropertyName("token")]
    public string? Token { get; set; }

    [JsonPropertyName("employee")]
    public EmployeeDto? Employee { get; set; }
}

public class EmployeeDto
{
    [JsonPropertyName("name")]
    public string? Name { get; set; }

    [JsonPropertyName("company_name")]
    public string? CompanyName { get; set; }

    [JsonPropertyName("employee_id")]
    public string? EmployeeId { get; set; }
}

public class StatusResponse
{
    [JsonPropertyName("success")]
    public bool Success { get; set; } = true;

    [JsonPropertyName("state")]
    public string? State { get; set; }

    [JsonPropertyName("last_check_in")]
    public string? LastCheckIn { get; set; }

    [JsonPropertyName("last_check_out")]
    public string? LastCheckOut { get; set; }

    [JsonPropertyName("message")]
    public string? Message { get; set; }
}

public class HistoryResponse
{
    [JsonPropertyName("success")]
    public bool Success { get; set; } = true;

    [JsonPropertyName("attendance")]
    public List<AttendanceRecordDto> Attendance { get; set; } = new();

    [JsonPropertyName("message")]
    public string? Message { get; set; }
}

public class AttendanceRecordDto
{
    [JsonPropertyName("check_in")]
    public string? CheckIn { get; set; }

    [JsonPropertyName("check_out")]
    public string? CheckOut { get; set; }

    [JsonPropertyName("worked_hours")]
    public double? WorkedHours { get; set; }
}

public class MarkAttendanceRequest
{
    [JsonPropertyName("face_image")]
    public string FaceImage { get; set; } = string.Empty;

    [JsonPropertyName("latitude")]
    public string Latitude { get; set; } = "0.0";

    [JsonPropertyName("longitude")]
    public string Longitude { get; set; } = "0.0";

    [JsonPropertyName("gps_accuracy")]
    public string GpsAccuracy { get; set; } = "10.0";
}

public class MarkAttendanceResponse
{
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    [JsonPropertyName("message")]
    public string? Message { get; set; }

    [JsonPropertyName("status")]
    public string? Status { get; set; }

    [JsonPropertyName("employee_name")]
    public string? EmployeeName { get; set; }
}

public class MqttAlertMessage
{
    [JsonPropertyName("employee_name")]
    public string? EmployeeName { get; set; }

    [JsonPropertyName("status")]
    public string? Status { get; set; }

    [JsonPropertyName("timestamp")]
    public string? Timestamp { get; set; }
}

