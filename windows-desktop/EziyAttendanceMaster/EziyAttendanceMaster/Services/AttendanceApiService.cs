using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Threading.Tasks;
using EziyAttendanceMaster.Models;

namespace EziyAttendanceMaster.Services;

public class AttendanceApiService : IAttendanceApiService
{
    private readonly ISecureStorageService _prefs;
    private readonly HttpClient _httpClient;
    private bool _mockCheckedIn = false;
    private string _lastPunchTimeStr = "Today at 09:00 AM";

    public AttendanceApiService(ISecureStorageService prefs)
    {
        _prefs = prefs;
        _httpClient = new HttpClient
        {
            Timeout = TimeSpan.FromSeconds(30)
        };
    }

    private bool IsDemoMode(string? url = null)
    {
        var target = (url ?? _prefs.ServerUrl).ToLowerInvariant();
        return target.Contains("demo");
    }

    private string CurrentTime()
    {
        return DateTime.Now.ToString("hh:mm tt");
    }

    private string CleanBaseUrl(string rawUrl)
    {
        var cleaned = rawUrl.Trim().TrimEnd('/');
        if (cleaned.EndsWith("/face_attendance", StringComparison.OrdinalIgnoreCase))
        {
            cleaned = cleaned[..^"/face_attendance".Length];
        }
        return cleaned.TrimEnd('/') + "/";
    }

    private string BearerToken => $"Bearer {_prefs.Token}";

    public async Task<(bool Success, string? Message, LoginResponse? Response)> LoginAsync(
        string baseUrl,
        string employeeId,
        string pin)
    {
        if (IsDemoMode(baseUrl))
        {
            await Task.Delay(800);
            const string mockCompany = "Eziy Demo Corp";
            const string mockName = "Demo Employee";
            const string mockToken = "mock_eziy_jwt_token_123456";

            _prefs.SaveAuthDetails(
                server: baseUrl,
                company: mockCompany,
                name: mockName,
                id: employeeId,
                pinCode: pin,
                authToken: mockToken
            );

            var demoResp = new LoginResponse
            {
                Success = true,
                Message = "Demo Login Successful",
                Token = mockToken,
                Employee = new EmployeeDto
                {
                    Name = mockName,
                    CompanyName = mockCompany,
                    EmployeeId = employeeId
                }
            };
            return (true, demoResp.Message, demoResp);
        }

        try
        {
            var targetUrl = CleanBaseUrl(baseUrl) + "face_attendance/api/login";
            var form = new FormUrlEncodedContent(new[]
            {
                new KeyValuePair<string, string>("employee_id", employeeId),
                new KeyValuePair<string, string>("pin", pin)
            });

            var response = await _httpClient.PostAsync(targetUrl, form);
            var content = await response.Content.ReadAsStringAsync();

            if (response.IsSuccessStatusCode)
            {
                var body = JsonSerializer.Deserialize<LoginResponse>(content);
                if (body != null && body.Success && !string.IsNullOrWhiteSpace(body.Token))
                {
                    var cleanedServer = CleanBaseUrl(baseUrl).TrimEnd('/');
                    _prefs.SaveAuthDetails(
                        server: cleanedServer,
                        company: body.Employee?.CompanyName ?? "",
                        name: body.Employee?.Name ?? "",
                        id: employeeId,
                        pinCode: pin,
                        authToken: body.Token
                    );
                    return (true, body.Message, body);
                }
                return (false, body?.Message ?? "Login failed", null);
            }

            return (false, $"Authentication failed ({response.StatusCode}): {content}", null);
        }
        catch (Exception ex)
        {
            return (false, ex.Message, null);
        }
    }

    public async Task<(bool Success, string? Message, StatusResponse? Response)> GetStatusAsync()
    {
        if (IsDemoMode())
        {
            await Task.Delay(500);
            var state = _mockCheckedIn ? "checked_in" : "checked_out";
            var demoResp = new StatusResponse
            {
                Success = true,
                State = state,
                LastCheckIn = _mockCheckedIn ? _lastPunchTimeStr : "Today at 09:00 AM",
                LastCheckOut = !_mockCheckedIn ? _lastPunchTimeStr : "—",
                Message = "Demo status retrieved"
            };
            return (true, demoResp.Message, demoResp);
        }

        try
        {
            var targetUrl = CleanBaseUrl(_prefs.ServerUrl) + "face_attendance/api/status";
            using var req = new HttpRequestMessage(HttpMethod.Post, targetUrl);
            req.Headers.Authorization = AuthenticationHeaderValue.Parse(BearerToken);

            var response = await _httpClient.SendAsync(req);
            var content = await response.Content.ReadAsStringAsync();

            if (response.IsSuccessStatusCode)
            {
                var body = JsonSerializer.Deserialize<StatusResponse>(content);
                return (true, body?.Message, body);
            }

            return (false, $"Could not fetch status ({response.StatusCode}): {content}", null);
        }
        catch (Exception ex)
        {
            return (false, ex.Message, null);
        }
    }

    public async Task<(bool Success, string? Message, HistoryResponse? Response)> GetHistoryAsync()
    {
        if (IsDemoMode())
        {
            await Task.Delay(600);
            var mockList = new List<AttendanceRecordDto>
            {
                new()
                {
                    CheckIn = "Today " + (_mockCheckedIn ? _lastPunchTimeStr : "09:00 AM"),
                    CheckOut = _mockCheckedIn ? "—" : ("Today " + _lastPunchTimeStr),
                    WorkedHours = _mockCheckedIn ? 4.5 : 8.0
                },
                new() { CheckIn = "Yesterday 08:58 AM", CheckOut = "05:30 PM", WorkedHours = 8.5 },
                new() { CheckIn = "2 days ago 09:05 AM", CheckOut = "05:15 PM", WorkedHours = 8.2 },
                new() { CheckIn = "3 days ago 08:50 AM", CheckOut = "05:40 PM", WorkedHours = 8.8 }
            };

            var demoResp = new HistoryResponse
            {
                Success = true,
                Attendance = mockList,
                Message = "Demo history loaded"
            };
            return (true, demoResp.Message, demoResp);
        }

        try
        {
            var targetUrl = CleanBaseUrl(_prefs.ServerUrl) + "face_attendance/api/history";
            using var req = new HttpRequestMessage(HttpMethod.Post, targetUrl);
            req.Headers.Authorization = AuthenticationHeaderValue.Parse(BearerToken);
            req.Content = new FormUrlEncodedContent(new[]
            {
                new KeyValuePair<string, string>("limit", "60")
            });

            var response = await _httpClient.SendAsync(req);
            var content = await response.Content.ReadAsStringAsync();

            if (response.IsSuccessStatusCode)
            {
                var body = JsonSerializer.Deserialize<HistoryResponse>(content);
                return (true, body?.Message, body);
            }

            return (false, $"Could not fetch history ({response.StatusCode}): {content}", null);
        }
        catch (Exception ex)
        {
            return (false, ex.Message, null);
        }
    }

    public async Task<(bool Success, string? Message, MarkAttendanceResponse? Response)> MarkAttendanceAsync(
        string base64Image,
        double latitude,
        double longitude,
        float accuracy)
    {
        if (IsDemoMode())
        {
            await Task.Delay(1000);
            _mockCheckedIn = !_mockCheckedIn;
            _lastPunchTimeStr = CurrentTime();
            var statusStr = _mockCheckedIn ? "CHECKED IN" : "CHECKED OUT";

            var demoResp = new MarkAttendanceResponse
            {
                Success = true,
                Message = "Demo Attendance Marked",
                Status = statusStr,
                EmployeeName = string.IsNullOrWhiteSpace(_prefs.EmployeeName) ? "Demo Employee" : _prefs.EmployeeName
            };
            return (true, demoResp.Message, demoResp);
        }

        try
        {
            var targetUrl = CleanBaseUrl(_prefs.ServerUrl) + "face_attendance/api/mark";
            using var req = new HttpRequestMessage(HttpMethod.Post, targetUrl);
            req.Headers.Authorization = AuthenticationHeaderValue.Parse(BearerToken);
            req.Content = new FormUrlEncodedContent(new[]
            {
                new KeyValuePair<string, string>("face_image", base64Image),
                new KeyValuePair<string, string>("latitude", latitude.ToString()),
                new KeyValuePair<string, string>("longitude", longitude.ToString()),
                new KeyValuePair<string, string>("gps_accuracy", accuracy.ToString())
            });

            var response = await _httpClient.SendAsync(req);
            var content = await response.Content.ReadAsStringAsync();

            if (response.IsSuccessStatusCode)
            {
                var body = JsonSerializer.Deserialize<MarkAttendanceResponse>(content);
                if (body != null && body.Success)
                {
                    return (true, body.Message, body);
                }
                return (false, body?.Message ?? "Attendance marking failed", null);
            }

            return (false, $"Attendance failed ({response.StatusCode}): {content}", null);
        }
        catch (Exception ex)
        {
            return (false, ex.Message, null);
        }
    }
}

