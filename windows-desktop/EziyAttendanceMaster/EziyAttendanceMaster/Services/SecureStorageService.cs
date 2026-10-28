using System;
using System.Collections.Generic;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace EziyAttendanceMaster.Services;

public class SecureStorageService : ISecureStorageService
{
    private readonly string _storageFilePath;
    private readonly Dictionary<string, string> _inMemorySettings = new(StringComparer.OrdinalIgnoreCase);
    private readonly object _lock = new();

    private const string KeyServerUrl = "server_url";
    private const string KeyCompanyName = "company_name";
    private const string KeyEmployeeName = "employee_name";
    private const string KeyEmployeeId = "employee_id";
    private const string KeyPin = "pin";
    private const string KeyToken = "token";
    private const string KeySipDomain = "sip_domain";
    private const string KeySipUsername = "sip_username";
    private const string KeySipPassword = "sip_password";
    private const string KeyCallLogsJson = "call_logs_json";
    private const string KeySpeedDialJson = "speed_dial_json";
    private const string KeyContactsJson = "contacts_json";

    public SecureStorageService()
    {
        var appData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        var dir = Path.Combine(appData, "EziyAttendanceMaster");
        Directory.CreateDirectory(dir);
        _storageFilePath = Path.Combine(dir, "secure_settings.dat");
        LoadFromDisk();
    }

    public string ServerUrl
    {
        get => GetValue(KeyServerUrl);
        set => SetValue(KeyServerUrl, value.TrimEnd('/'));
    }

    public string CompanyName
    {
        get => GetValue(KeyCompanyName);
        set => SetValue(KeyCompanyName, value);
    }

    public string EmployeeName
    {
        get => GetValue(KeyEmployeeName);
        set => SetValue(KeyEmployeeName, value);
    }

    public string EmployeeId
    {
        get => GetValue(KeyEmployeeId);
        set => SetValue(KeyEmployeeId, value.Trim());
    }

    public string Pin
    {
        get => GetValue(KeyPin);
        set => SetValue(KeyPin, value);
    }

    public string Token
    {
        get => GetValue(KeyToken);
        set => SetValue(KeyToken, value);
    }

    public string SipDomain
    {
        get => GetValue(KeySipDomain);
        set => SetValue(KeySipDomain, value.Trim());
    }

    public string SipUsername
    {
        get => GetValue(KeySipUsername);
        set => SetValue(KeySipUsername, value.Trim());
    }

    public string SipPassword
    {
        get => GetValue(KeySipPassword);
        set => SetValue(KeySipPassword, value);
    }

    public string CallLogsJson
    {
        get => GetValue(KeyCallLogsJson);
        set => SetValue(KeyCallLogsJson, value);
    }

    public string SpeedDialJson
    {
        get => GetValue(KeySpeedDialJson);
        set => SetValue(KeySpeedDialJson, value);
    }

    public string ContactsJson
    {
        get => GetValue(KeyContactsJson);
        set => SetValue(KeyContactsJson, value);
    }

    public bool IsLoggedIn => !string.IsNullOrWhiteSpace(Token) && !string.IsNullOrWhiteSpace(ServerUrl);

    public bool IsSipRegistered => !string.IsNullOrWhiteSpace(SipUsername) && !string.IsNullOrWhiteSpace(SipDomain);

    public void SaveAuthDetails(string server, string company, string name, string id, string pinCode, string authToken)
    {
        lock (_lock)
        {
            _inMemorySettings[KeyServerUrl] = server.TrimEnd('/');
            _inMemorySettings[KeyCompanyName] = company;
            _inMemorySettings[KeyEmployeeName] = name;
            _inMemorySettings[KeyEmployeeId] = id.Trim();
            _inMemorySettings[KeyPin] = pinCode;
            _inMemorySettings[KeyToken] = authToken;
            SaveToDisk();
        }
    }

    public void SaveSipDetails(string domain, string user, string pass)
    {
        lock (_lock)
        {
            _inMemorySettings[KeySipDomain] = domain.Trim();
            _inMemorySettings[KeySipUsername] = user.Trim();
            _inMemorySettings[KeySipPassword] = pass;
            SaveToDisk();
        }
    }

    public void ClearSipDetails()
    {
        lock (_lock)
        {
            _inMemorySettings.Remove(KeySipDomain);
            _inMemorySettings.Remove(KeySipUsername);
            _inMemorySettings.Remove(KeySipPassword);
            SaveToDisk();
        }
    }

    public void ClearAll()
    {
        lock (_lock)
        {
            _inMemorySettings.Clear();
            if (File.Exists(_storageFilePath))
            {
                File.Delete(_storageFilePath);
            }
        }
    }

    private string GetValue(string key)
    {
        lock (_lock)
        {
            return _inMemorySettings.TryGetValue(key, out var val) ? val : string.Empty;
        }
    }

    private void SetValue(string key, string value)
    {
        lock (_lock)
        {
            _inMemorySettings[key] = value;
            SaveToDisk();
        }
    }

    private void SaveToDisk()
    {
        try
        {
            var json = JsonSerializer.Serialize(_inMemorySettings);
            var bytes = Encoding.UTF8.GetBytes(json);

            byte[] protectedBytes;
            if (OperatingSystem.IsWindows())
            {
                // Encrypt via Windows DPAPI (Current User)
                protectedBytes = ProtectedData.Protect(bytes, null, DataProtectionScope.CurrentUser);
            }
            else
            {
                // Fallback for non-Windows dev environments
                protectedBytes = bytes;
            }

            File.WriteAllBytes(_storageFilePath, protectedBytes);
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Secure storage save error: {ex.Message}");
        }
    }

    private void LoadFromDisk()
    {
        lock (_lock)
        {
            if (!File.Exists(_storageFilePath)) return;

            try
            {
                var protectedBytes = File.ReadAllBytes(_storageFilePath);
                byte[] rawBytes;

                if (OperatingSystem.IsWindows())
                {
                    rawBytes = ProtectedData.Unprotect(protectedBytes, null, DataProtectionScope.CurrentUser);
                }
                else
                {
                    rawBytes = protectedBytes;
                }

                var json = Encoding.UTF8.GetString(rawBytes);
                var dict = JsonSerializer.Deserialize<Dictionary<string, string>>(json);
                if (dict != null)
                {
                    _inMemorySettings.Clear();
                    foreach (var kvp in dict)
                    {
                        _inMemorySettings[kvp.Key] = kvp.Value;
                    }
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Secure storage load error: {ex.Message}");
            }
        }
    }
}

