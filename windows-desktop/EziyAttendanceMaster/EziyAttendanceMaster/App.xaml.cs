using System.Windows;
using EziyAttendanceMaster.Linphone;
using EziyAttendanceMaster.Services;
using EziyAttendanceMaster.ViewModels;

namespace EziyAttendanceMaster;

public partial class App : Application
{
    public static ISecureStorageService StorageService { get; private set; } = null!;
    public static IAttendanceApiService ApiService { get; private set; } = null!;
    public static IEziyMqttService MqttService { get; private set; } = null!;
    public static EziyLinphoneManager LinphoneManager { get; private set; } = null!;
    public static MainViewModel MainVm { get; private set; } = null!;

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        StorageService = new SecureStorageService();
        ApiService = new AttendanceApiService(StorageService);
        MqttService = new EziyMqttService(StorageService);
        LinphoneManager = new EziyLinphoneManager();

        MainVm = new MainViewModel(StorageService, ApiService, MqttService, LinphoneManager);

        var mainWindow = new MainWindow
        {
            DataContext = MainVm
        };

        mainWindow.Show();
    }

    protected override void OnExit(ExitEventArgs e)
    {
        LinphoneManager.Dispose();
        (MqttService as IDisposable)?.Dispose();
        base.OnExit(e);
    }
}

