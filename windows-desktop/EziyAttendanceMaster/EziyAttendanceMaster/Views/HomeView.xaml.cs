using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using EziyAttendanceMaster.ViewModels;

namespace EziyAttendanceMaster.Views;

public partial class HomeView : UserControl
{
    public HomeView()
    {
        InitializeComponent();
        DataContextChanged += OnDataContextChanged;
    }

    private void OnDataContextChanged(object sender, DependencyPropertyChangedEventArgs e)
    {
        if (e.OldValue is HomeViewModel oldVm)
        {
            oldVm.PropertyChanged -= OnViewModelPropertyChanged;
        }
        if (e.NewValue is HomeViewModel newVm)
        {
            newVm.PropertyChanged += OnViewModelPropertyChanged;
        }
    }

    private void OnViewModelPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(HomeViewModel.IsCameraDialogOpen) && sender is HomeViewModel vm && vm.IsCameraDialogOpen)
        {
            var dialog = new FaceCameraDialog(vm)
            {
                Owner = Window.GetWindow(this)
            };
            dialog.ShowDialog();
            vm.IsCameraDialogOpen = false;
        }
    }
}

