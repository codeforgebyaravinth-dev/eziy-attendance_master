using System;
using CommunityToolkit.Mvvm.Input;
using EziyAttendanceMaster.Services;

namespace EziyAttendanceMaster.ViewModels;

public class SettingsViewModel : ViewModelBase
{
    private readonly ISecureStorageService _prefs;

    private string _serverUrl = "";
    private string _companyName = "";
    private string _employeeName = "";
    private string _employeeId = "";
    private string _sipDomain = "sip.linphone.org";
    private string _sipUsername = "";
    private string _sipPassword = "";
    private bool _isSaved;

    public event Action? OnLoggedOut;

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

    public string SipDomain
    {
        get => _sipDomain;
        set => SetProperty(ref _sipDomain, value);
    }

    public string SipUsername
    {
        get => _sipUsername;
        set => SetProperty(ref _sipUsername, value);
    }

    public string SipPassword
    {
        get => _sipPassword;
        set => SetProperty(ref _sipPassword, value);
    }

    public bool IsSaved
    {
        get => _isSaved;
        set => SetProperty(ref _isSaved, value);
    }

    public IRelayCommand SaveSettingsCommand { get; }
    public IRelayCommand LogoutCommand { get; }

    public SettingsViewModel(ISecureStorageService prefs)
    {
        _prefs = prefs;

        SaveSettingsCommand = new RelayCommand(SaveSettings);
        LogoutCommand = new RelayCommand(Logout);

        LoadSettings();
    }

    public void LoadSettings()
    {
        ServerUrl = _prefs.ServerUrl;
        CompanyName = _prefs.CompanyName;
        EmployeeName = _prefs.EmployeeName;
        EmployeeId = _prefs.EmployeeId;
        SipDomain = string.IsNullOrWhiteSpace(_prefs.SipDomain) ? "sip.linphone.org" : _prefs.SipDomain;
        SipUsername = _prefs.SipUsername;
        SipPassword = _prefs.SipPassword;
        IsSaved = false;
    }

    public void SaveSettings()
    {
        _prefs.ServerUrl = ServerUrl;
        _prefs.CompanyName = CompanyName;
        _prefs.EmployeeName = EmployeeName;
        _prefs.SaveSipDetails(SipDomain, SipUsername, SipPassword);
        IsSaved = true;
    }

    public void Logout()
    {
        _prefs.ClearAll();
        OnLoggedOut?.Invoke();
    }
}

