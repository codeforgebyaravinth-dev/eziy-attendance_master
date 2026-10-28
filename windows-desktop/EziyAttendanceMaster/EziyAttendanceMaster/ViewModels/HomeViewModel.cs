using System;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.Input;
using EziyAttendanceMaster.Services;
using EziyAttendanceMaster.Utils;

namespace EziyAttendanceMaster.ViewModels;

public class HomeViewModel : ViewModelBase
{
    private readonly IAttendanceApiService _apiService;
    private readonly ISecureStorageService _prefs;

    private string _employeeName = "Employee";
    private string _companyName = "VSPL Enterprise";
    private string _employeeId = "";
    private bool _isCheckedIn;
    private string _timeText = "Loading status...";
    private string _lastCheckInTime = "—";
    private string _lastCheckOutTime = "—";
    private double _todayWorkedHours;
    private double _shiftProgress;
    private bool _isLoadingStatus;
    private bool _isSubmittingAttendance;
    private string? _toastMessage;
    private bool _isCameraDialogOpen;

    public string EmployeeName
    {
        get => _employeeName;
        set => SetProperty(ref _employeeName, value);
    }

    public string CompanyName
    {
        get => _companyName;
        set => SetProperty(ref _companyName, value);
    }

    public string EmployeeId
    {
        get => _employeeId;
        set => SetProperty(ref _employeeId, value);
    }

    public bool IsCheckedIn
    {
        get => _isCheckedIn;
        set
        {
            if (SetProperty(ref _isCheckedIn, value))
            {
                OnPropertyChanged(nameof(ClockButtonText));
                OnPropertyChanged(nameof(StatusBadgeText));
            }
        }
    }

    public string ClockButtonText => IsCheckedIn ? "CLOCK OUT (FACE VERIFIED)" : "CLOCK IN (FACE VERIFIED)";
    public string StatusBadgeText => IsCheckedIn ? "CHECKED IN" : "CHECKED OUT";

    public string TimeText
    {
        get => _timeText;
        set => SetProperty(ref _timeText, value);
    }

    public string LastCheckInTime
    {
        get => _lastCheckInTime;
        set => SetProperty(ref _lastCheckInTime, value);
    }

    public string LastCheckOutTime
    {
        get => _lastCheckOutTime;
        set => SetProperty(ref _lastCheckOutTime, value);
    }

    public double TodayWorkedHours
    {
        get => _todayWorkedHours;
        set
        {
            if (SetProperty(ref _todayWorkedHours, value))
            {
                OnPropertyChanged(nameof(WorkedHoursFormatted));
            }
        }
    }

    public string WorkedHoursFormatted => $"{TodayWorkedHours:F1}h / 8.0h";

    public double ShiftProgress
    {
        get => _shiftProgress;
        set => SetProperty(ref _shiftProgress, value);
    }

    public bool IsLoadingStatus
    {
        get => _isLoadingStatus;
        set => SetProperty(ref _isLoadingStatus, value);
    }

    public bool IsSubmittingAttendance
    {
        get => _isSubmittingAttendance;
        set => SetProperty(ref _isSubmittingAttendance, value);
    }

    public string? ToastMessage
    {
        get => _toastMessage;
        set => SetProperty(ref _toastMessage, value);
    }

    public bool IsCameraDialogOpen
    {
        get => _isCameraDialogOpen;
        set => SetProperty(ref _isCameraDialogOpen, value);
    }

    public IAsyncRelayCommand RefreshStatusCommand { get; }
    public IRelayCommand OpenCameraCommand { get; }

    public HomeViewModel(IAttendanceApiService apiService, ISecureStorageService prefs)
    {
        _apiService = apiService;
        _prefs = prefs;

        RefreshStatusCommand = new AsyncRelayCommand(RefreshStatusAsync);
        OpenCameraCommand = new RelayCommand(() => IsCameraDialogOpen = true);

        LoadUserInfo();
        _ = RefreshStatusAsync();
    }

    public void LoadUserInfo()
    {
        EmployeeName = string.IsNullOrWhiteSpace(_prefs.EmployeeName) ? "Employee" : _prefs.EmployeeName;
        CompanyName = string.IsNullOrWhiteSpace(_prefs.CompanyName) ? "VSPL Enterprise" : _prefs.CompanyName;
        EmployeeId = _prefs.EmployeeId;
    }

    public async Task RefreshStatusAsync()
    {
        IsLoadingStatus = true;
        var (success, msg, status) = await _apiService.GetStatusAsync();
        IsLoadingStatus = false;

        if (success && status != null)
        {
            var checkedIn = string.Equals(status.State, "checked_in", StringComparison.OrdinalIgnoreCase);
            var rawCheckIn = status.LastCheckIn;
            var rawCheckOut = status.LastCheckOut;

            var checkInLocal = EziyDateTimeUtils.FormatUtcToLocal(rawCheckIn);
            var checkOutLocal = checkedIn ? "In Progress (Active Shift)" : EziyDateTimeUtils.FormatUtcToLocal(rawCheckOut);

            TimeText = checkedIn ? $"Since: {checkInLocal}" : $"Last: {checkOutLocal}";
            LastCheckInTime = checkInLocal;
            LastCheckOutTime = checkOutLocal;

            var worked = checkedIn ? EziyDateTimeUtils.CalculateElapsedHours(rawCheckIn) : 0.0;
            TodayWorkedHours = worked;
            ShiftProgress = Math.Clamp(worked / 8.0, 0.0, 1.0);
            IsCheckedIn = checkedIn;
        }
        else
        {
            ToastMessage = msg ?? "Could not sync status";
        }
    }

    public async Task<bool> SubmitAttendanceWithFaceAsync(string base64Image)
    {
        IsSubmittingAttendance = true;
        ToastMessage = "Verifying face and marking attendance...";

        // Standard Windows station coordinates (or mock 13.0827, 80.2707)
        const double latitude = 13.0827;
        const double longitude = 80.2707;
        const float accuracy = 5.0f;

        var (success, msg, markResp) = await _apiService.MarkAttendanceAsync(base64Image, latitude, longitude, accuracy);
        IsSubmittingAttendance = false;

        if (success && markResp != null)
        {
            ToastMessage = $"Success! {markResp.Status}: {markResp.EmployeeName}";
            await RefreshStatusAsync();
            return true;
        }

        ToastMessage = msg ?? "Failed to mark attendance";
        return false;
    }
}

