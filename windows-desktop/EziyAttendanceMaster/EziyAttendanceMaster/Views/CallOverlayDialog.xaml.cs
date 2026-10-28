using System;
using System.ComponentModel;
using System.Windows;
using EziyAttendanceMaster.ViewModels;

namespace EziyAttendanceMaster.Views;

public partial class CallOverlayDialog : Window
{
    private readonly SipIntercomViewModel _intercomVm;

    public CallOverlayDialog(SipIntercomViewModel intercomVm)
    {
        InitializeComponent();
        _intercomVm = intercomVm;
        DataContext = _intercomVm;

        Loaded += OnWindowLoaded;
        _intercomVm.PropertyChanged += OnViewModelPropertyChanged;
    }

    private void OnWindowLoaded(object sender, RoutedEventArgs e)
    {
        // Bind native Linphone Direct3D / GDI video display surfaces to Win32 HWND handles
        App.LinphoneManager.SetVideoSurfaces(RemoteVideoHost.Hwnd, PreviewVideoHost.Hwnd);
    }

    private void OnViewModelPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(SipIntercomViewModel.IsCallActive) && !_intercomVm.IsCallActive)
        {
            Dispatcher.Invoke(Close);
        }
    }

    private void OnHangUpClicked(object sender, RoutedEventArgs e)
    {
        _intercomVm.HangUp();
        Close();
    }

    protected override void OnClosed(EventArgs e)
    {
        _intercomVm.PropertyChanged -= OnViewModelPropertyChanged;
        base.OnClosed(e);
    }
}

