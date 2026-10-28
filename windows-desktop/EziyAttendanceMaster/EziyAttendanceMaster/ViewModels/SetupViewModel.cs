using System;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.Input;
using EziyAttendanceMaster.Services;

namespace EziyAttendanceMaster.ViewModels;

public partial class SetupViewModel : ViewModelBase
{
    private readonly IAttendanceApiService _apiService;
    private readonly ISecureStorageService _prefs;

    private string _serverUrl = "https://demo";
    private string _companyName = "VSPL";
    private string _employeeName = "Demo Employee";
    private string _employeeId = "12345678";
    private string _pin = "1234";
    private bool _isLoading;
    private string? _errorMessage;

    public event Action? OnSetupSuccessful;

    public string ServerUrl
    {
        get => _serverUrl;
        set => SetProperty(ref _serverUrl, value);
    }

    public string CompanyName
    {
        get => _companyName;
        set => SetProperty(ref _companyName, value);
    }

    public string EmployeeName
    {
        get => _employeeName;
        set => SetProperty(ref _employeeName, value);
    }

    public string EmployeeId
    {
        get => _employeeId;
        set => SetProperty(ref _employeeId, value);
    }

    public string Pin
    {
        get => _pin;
        set => SetProperty(ref _pin, value);
    }

    public bool IsLoading
    {
        get => _isLoading;
        set => SetProperty(ref _isLoading, value);
    }

    public string? ErrorMessage
    {
        get => _errorMessage;
        set => SetProperty(ref _errorMessage, value);
    }

    public IAsyncRelayCommand AuthenticateCommand { get; }

    public SetupViewModel(IAttendanceApiService apiService, ISecureStorageService prefs)
    {
        _apiService = apiService;
        _prefs = prefs;

        if (!string.IsNullOrWhiteSpace(_prefs.ServerUrl))
            _serverUrl = _prefs.ServerUrl;
        if (!string.IsNullOrWhiteSpace(_prefs.EmployeeId))
            _employeeId = _prefs.EmployeeId;
        if (!string.IsNullOrWhiteSpace(_prefs.CompanyName))
            _companyName = _prefs.CompanyName;
        if (!string.IsNullOrWhiteSpace(_prefs.EmployeeName))
            _employeeName = _prefs.EmployeeName;

        AuthenticateCommand = new AsyncRelayCommand(AuthenticateAsync);
    }

    public async Task AuthenticateAsync()
    {
        var server = ServerUrl.Trim();
        var id = EmployeeId.Trim();
        var pinCode = Pin.Trim();

        if (!server.StartsWith("https://", StringComparison.OrdinalIgnoreCase) && !server.Contains("demo", StringComparison.OrdinalIgnoreCase))
        {
            ErrorMessage = "Please enter a valid HTTPS server URL (or https://demo)";
            return;
        }

        if (id.Length != 8)
        {
            ErrorMessage = "Employee ID must be an 8-digit number";
            return;
        }

        if (string.IsNullOrWhiteSpace(pinCode))
        {
            ErrorMessage = "Please enter your Attendance PIN";
            return;
        }

        IsLoading = true;
        ErrorMessage = null;

        var (success, msg, _) = await _apiService.LoginAsync(server, id, pinCode);

        IsLoading = false;

        if (success)
        {
            OnSetupSuccessful?.Invoke();
        }
        else
        {
            ErrorMessage = msg ?? "Authentication failed";
        }
    }
}

