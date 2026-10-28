using System;
using System.Threading.Tasks;
using System.Windows.Media.Imaging;

namespace EziyAttendanceMaster.Services;

public class FaceValidationResult
{
    public bool IsFaceDetected { get; set; }
    public bool IsFaceCentered { get; set; }
    public bool IsFaceWellSized { get; set; }
    public bool IsValidForCapture => IsFaceDetected && IsFaceCentered && IsFaceWellSized;
    public string FeedbackMessage { get; set; } = "Position face in oval";
}

public interface IWebcamFaceService : IDisposable
{
    event Action<BitmapSource, FaceValidationResult>? OnFrameProcessed;
    bool IsRunning { get; }
    Task StartCameraAsync(int cameraIndex = 0);
    Task StopCameraAsync();
    (string Base64Image, BitmapSource? CapturedBmp)? CaptureSnapshot();
}

