using System.Collections.ObjectModel;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.Input;
using EziyAttendanceMaster.Models;
using EziyAttendanceMaster.Services;
using EziyAttendanceMaster.Utils;

namespace EziyAttendanceMaster.ViewModels;

public class HistoryViewModel : ViewModelBase
{
    private readonly IAttendanceApiService _apiService;
    private bool _isLoading;
    private string? _errorMessage;

    public ObservableCollection<AttendanceRecordDto> Records { get; } = new();

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

    public IAsyncRelayCommand LoadHistoryCommand { get; }

    public HistoryViewModel(IAttendanceApiService apiService)
    {
        _apiService = apiService;
        LoadHistoryCommand = new AsyncRelayCommand(LoadHistoryAsync);
        _ = LoadHistoryAsync();
    }

    public async Task LoadHistoryAsync()
    {
        IsLoading = true;
        ErrorMessage = null;

        var (success, msg, response) = await _apiService.GetHistoryAsync();
        IsLoading = false;

        Records.Clear();
        if (success && response != null)
        {
            foreach (var rec in response.Attendance)
            {
                Records.Add(new AttendanceRecordDto
                {
                    CheckIn = EziyDateTimeUtils.FormatUtcToLocal(rec.CheckIn),
                    CheckOut = EziyDateTimeUtils.FormatUtcToLocal(rec.CheckOut),
                    WorkedHours = rec.WorkedHours
                });
            }
        }
        else
        {
            ErrorMessage = msg ?? "Failed to load attendance history";
        }
    }
}

