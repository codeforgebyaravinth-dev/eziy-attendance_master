using System;
using System.Windows;
using System.Windows.Media;
using EziyAttendanceMaster.Services;
using EziyAttendanceMaster.ViewModels;

namespace EziyAttendanceMaster.Views;

public partial class FaceCameraDialog : Window
{
    private readonly HomeViewModel _homeVm;
    private readonly IWebcamFaceService _webcamService;

    public FaceCameraDialog(HomeViewModel homeVm)
    {
        InitializeComponent();
        _homeVm = homeVm;
        _webcamService = new WebcamFaceService();

        Loaded += OnWindowLoaded;
        Closed += OnWindowClosed;
    }

    private async void OnWindowLoaded(object sender, RoutedEventArgs e)
    {
        _webcamService.OnFrameProcessed += (bmp, validation) =>
        {
            Dispatcher.Invoke(() =>
            {
                WebcamImage.Source = bmp;
                TxtStatus.Text = validation.FeedbackMessage;

                if (validation.IsValidForCapture)
                {
                    FaceOvalGuide.Stroke = new SolidColorBrush(Color.FromRgb(16, 185, 129)); // Green #10B981
                    BtnCapture.IsEnabled = true;
                }
                else
                {
                    FaceOvalGuide.Stroke = new SolidColorBrush(Color.FromRgb(2, 132, 199)); // Cyan #0284C7
                }
            });
        };

        await _webcamService.StartCameraAsync();
    }

    private async void OnCaptureClicked(object sender, RoutedEventArgs e)
    {
        BtnCapture.IsEnabled = false;
        TxtStatus.Text = "Capturing photo & verifying...";

        var snapshot = _webcamService.CaptureSnapshot();
        if (snapshot.HasValue && !string.IsNullOrWhiteSpace(snapshot.Value.Base64Image))
        {
            await _webcamService.StopCameraAsync();
            await _homeVm.SubmitAttendanceWithFaceAsync(snapshot.Value.Base64Image);
            Close();
        }
        else
        {
            TxtStatus.Text = "Failed to capture image. Please try again.";
            BtnCapture.IsEnabled = true;
        }
    }

    private void OnCloseClicked(object sender, RoutedEventArgs e)
    {
        Close();
    }

    private async void OnWindowClosed(object? sender, EventArgs e)
    {
        await _webcamService.StopCameraAsync();
        _webcamService.Dispose();
    }
}

