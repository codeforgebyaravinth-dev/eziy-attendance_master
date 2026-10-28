namespace EziyAttendanceMaster.Services;

public interface ISecureStorageService
{
    string ServerUrl { get; set; }
    string CompanyName { get; set; }
    string EmployeeName { get; set; }
    string EmployeeId { get; set; }
    string Pin { get; set; }
    string Token { get; set; }
    string SipDomain { get; set; }
    string SipUsername { get; set; }
    string SipPassword { get; set; }
    string CallLogsJson { get; set; }
    string SpeedDialJson { get; set; }
    string ContactsJson { get; set; }

    bool IsLoggedIn { get; }
    bool IsSipRegistered { get; }

    void SaveAuthDetails(string server, string company, string name, string id, string pinCode, string authToken);
    void SaveSipDetails(string domain, string user, string pass);
    void ClearSipDetails();
    void ClearAll();
}

