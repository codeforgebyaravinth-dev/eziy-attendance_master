using System;
using CommunityToolkit.Mvvm.Input;
using EziyAttendanceMaster.Linphone;
using EziyAttendanceMaster.Services;

namespace EziyAttendanceMaster.ViewModels;

public class MainViewModel : ViewModelBase
{
    private readonly ISecureStorageService _prefs;
    private readonly IAttendanceApiService _apiService;
    private readonly IEziyMqttService _mqttService;
    private readonly EziyLinphoneManager _linphoneManager;

    private object _currentView;
    private string _selectedTab = "home";
    private string? _globalAlertNotification;
    private bool _isMqttActive;

    public SetupViewModel SetupVm { get; }
    public HomeViewModel HomeVm { get; }
    public HistoryViewModel HistoryVm { get; }
    public SipIntercomViewModel IntercomVm { get; }
    public SettingsViewModel SettingsVm { get; }

    public object CurrentView
    {
        get => _currentView;
        set => SetProperty(ref _currentView, value);
    }

    public string SelectedTab
    {
        get => _selectedTab;
        set => SetProperty(ref _selectedTab, value);
    }

    public string? GlobalAlertNotification
    {
        get => _globalAlertNotification;
        set => SetProperty(ref _globalAlertNotification, value);
    }

    public bool IsMqttActive
    {
        get => _isMqttActive;
        set => SetProperty(ref _isMqttActive, value);
    }

    public bool IsNavigationRailVisible => CurrentView != SetupVm;

    public IRelayCommand<string> NavigateTabCommand { get; }
    public IRelayCommand DismissAlertCommand { get; }

    public MainViewModel(
        ISecureStorageService prefs,
        IAttendanceApiService apiService,
        IEziyMqttService mqttService,
        EziyLinphoneManager linphoneManager)
    {
        _prefs = prefs;
        _apiService = apiService;
        _mqttService = mqttService;
        _linphoneManager = linphoneManager;

        SetupVm = new SetupViewModel(_apiService, _prefs);
        HomeVm = new HomeViewModel(_apiService, _prefs);
        HistoryVm = new HistoryViewModel(_apiService);
        IntercomVm = new SipIntercomViewModel(_linphoneManager, _prefs);
        SettingsVm = new SettingsViewModel(_prefs);

        NavigateTabCommand = new RelayCommand<string>(NavigateToTab);
        DismissAlertCommand = new RelayCommand(() => GlobalAlertNotification = null);

        SetupVm.OnSetupSuccessful += OnSetupSuccess;
        SettingsVm.OnLoggedOut += OnUserLoggedOut;

        _mqttService.OnAlertReceived += alert =>
        {
            App.Current.Dispatcher.Invoke(() =>
            {
                GlobalAlertNotification = $"🔔 {alert.EmployeeName}: {alert.Status} ({alert.Timestamp})";
                _ = HomeVm.RefreshStatusAsync();
            });
        };

        if (_prefs.IsLoggedIn)
        {
            _currentView = HomeVm;
            _selectedTab = "home";
            _ = _mqttService.StartAsync();
            IsMqttActive = true;
        }
        else
        {
            _currentView = SetupVm;
            _selectedTab = "setup";
        }
    }

    private void NavigateToTab(string? tab)
    {
        if (string.IsNullOrWhiteSpace(tab)) return;

        SelectedTab = tab;
        switch (tab.ToLowerInvariant())
        {
            case "home":
                HomeVm.LoadUserInfo();
                _ = HomeVm.RefreshStatusAsync();
                CurrentView = HomeVm;
                break;
            case "history":
                _ = HistoryVm.LoadHistoryAsync();
                CurrentView = HistoryVm;
                break;
            case "intercom":
                CurrentView = IntercomVm;
                break;
            case "settings":
                SettingsVm.LoadSettings();
                CurrentView = SettingsVm;
                break;
        }

        OnPropertyChanged(nameof(IsNavigationRailVisible));
    }

    private void OnSetupSuccess()
    {
        HomeVm.LoadUserInfo();
        _ = HomeVm.RefreshStatusAsync();
        _ = _mqttService.StartAsync();
        IsMqttActive = true;
        NavigateToTab("home");
    }

    private void OnUserLoggedOut()
    {
        _ = _mqttService.StopAsync();
        IsMqttActive = false;
        CurrentView = SetupVm;
        SelectedTab = "setup";
        OnPropertyChanged(nameof(IsNavigationRailVisible));
    }
}

