using System.ComponentModel;
using System.Windows;
using EziyAttendanceMaster.ViewModels;
using EziyAttendanceMaster.Views;

namespace EziyAttendanceMaster;

public partial class MainWindow : Window
{
    private CallOverlayDialog? _activeCallDialog;

    public MainWindow()
    {
        InitializeComponent();
        DataContextChanged += OnDataContextChanged;
    }

    private void OnDataContextChanged(object sender, DependencyPropertyChangedEventArgs e)
    {
        if (e.NewValue is MainViewModel mainVm)
        {
            mainVm.IntercomVm.PropertyChanged += OnIntercomPropertyChanged;
        }
    }

    private void OnIntercomPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(SipIntercomViewModel.IsCallActive) && sender is SipIntercomViewModel intercomVm)
        {
            Dispatcher.Invoke(() =>
            {
                if (intercomVm.IsCallActive)
                {
                    if (_activeCallDialog == null || !_activeCallDialog.IsLoaded)
                    {
                        _activeCallDialog = new CallOverlayDialog(intercomVm)
                        {
                            Owner = this
                        };
                        _activeCallDialog.Closed += (_, _) => _activeCallDialog = null;
                        _activeCallDialog.ShowDialog();
                    }
                }
                else
                {
                    _activeCallDialog?.Close();
                    _activeCallDialog = null;
                }
            });
        }
    }
}

