using System.Threading.Tasks;
using EziyAttendanceMaster.Models;

namespace EziyAttendanceMaster.Services;

public interface IAttendanceApiService
{
    Task<(bool Success, string? Message, LoginResponse? Response)> LoginAsync(string baseUrl, string employeeId, string pin);
    Task<(bool Success, string? Message, StatusResponse? Response)> GetStatusAsync();
    Task<(bool Success, string? Message, HistoryResponse? Response)> GetHistoryAsync();
    Task<(bool Success, string? Message, MarkAttendanceResponse? Response)> MarkAttendanceAsync(string base64Image, double latitude, double longitude, float accuracy);
}

