# VSPL / Eziy Attendance Master — Native Windows Desktop Application

Enterprise Employee Attendance & VoIP Intercom Desktop Station built with **Microsoft .NET 8 / C# (WPF)**, powered by the **Native Belledonne Linphone C# SDK (`liblinphone`)**, **OpenCV Face Verification**, **Odoo ERP REST API**, and **MQTT Real-Time Push Alerts**.

---

## 🚀 Key Feature Parity with Android App

| Android Feature (`/app`) | Native Windows Desktop (`/windows-desktop`) | Tech Implementation |
| :--- | :--- | :--- |
| **Linphone VoIP Intercom** | Native Windows SIP Intercom with 3x3 Speed Dial & Dialer | `LinphoneSDK.Windows` (`liblinphone.dll` + `LinphoneWrapper.cs`) |
| **Video Calls & Self View** | Hardware-accelerated Win32 `HWND` direct rendering | `LinphoneVideoHost` (`HwndHost`) bound to `NativeVideoWindowId` |
| **Hero Shift Dashboard** | 8.0-Hour Daily Shift Progress tracker & Live Status | MVVM `HomeViewModel` + Live Status computation |
| **Face Selfie Capture** | Webcam feed with Oval guide and centering validation | `OpenCvSharp4` + `WebcamFaceService` |
| **Setup & Authentication** | Setup screen with 8-digit Badge ID, PIN, & Demo Mode | `SetupViewModel` + `AttendanceApiService` |
| **Attendance History** | Check-in / Check-out timesheet logs | `HistoryViewModel` with formatting |
| **Credential Encryption** | AES-256 Storage | **Windows DPAPI** (`System.Security.Cryptography.ProtectedData`) |
| **Real-time Push Alerts** | Real-time attendance notifications | `MQTTnet` Client (`tcp://mqtt.eziy.in:1883`) |

---

## 🛠️ System Requirements

- **Operating System:** Windows 10 (Version 19041+) or Windows 11 (x64 / x86)
- **SDK:** .NET 8.0 SDK or .NET 10.0 SDK
- **IDE:** Visual Studio 2022 (with *.NET Desktop Development* workload) or VS Code

---

## 📦 Solution Architecture

```text
windows-desktop/EziyAttendanceMaster/
├── EziyAttendanceMaster.sln
└── EziyAttendanceMaster/
    ├── App.xaml / App.xaml.cs                         # Application startup, DI & theme styles
    ├── MainWindow.xaml / MainWindow.xaml.cs           # VSPL Navy Navigation Rail & Tab viewport
    ├── Linphone/
    │   ├── EziyLinphoneManager.cs                     # Native Linphone Core C# wrapper controller
    │   └── LinphoneVideoHost.cs                       # Win32 HWND host for video streams
    ├── Models/
    │   ├── Dtos.cs                                    # Odoo Login, Status, History, Mark models
    │   └── SipModels.cs                               # Speed dial, contact directory, call logs
    ├── Services/
    │   ├── AttendanceApiService.cs                    # REST client + offline Demo Mode engine
    │   ├── SecureStorageService.cs                    # Windows DPAPI AES credential protection
    │   ├── EziyMqttService.cs                         # MQTTnet background listener
    │   └── WebcamFaceService.cs                       # OpenCV webcam capture & face centering
    ├── Utils/
    │   ├── Converters.cs                              # WPF UI Value Converters
    │   └── EziyDateTimeUtils.cs                       # UTC to local & shift hour calculator
    ├── ViewModels/
    │   ├── MainViewModel.cs                           # App state & tab navigation coordinator
    │   ├── SetupViewModel.cs                          # First-time setup & login
    │   ├── HomeViewModel.cs                           # Shift dashboard & Clock in/out
    │   ├── HistoryViewModel.cs                        # Attendance logs
    │   ├── SipIntercomViewModel.cs                    # 3x3 Speed Dial, dialer & call state
    │   └── SettingsViewModel.cs                       # Server & SIP credential configuration
    └── Views/
        ├── SetupView.xaml                             # Setup screen
        ├── HomeView.xaml                              # Hero Dashboard
        ├── HistoryView.xaml                           # Logs table
        ├── SipIntercomView.xaml                       # Intercom station
        ├── SettingsView.xaml                          # System settings
        ├── FaceCameraDialog.xaml                      # Live webcam capture with oval guide
        └── CallOverlayDialog.xaml                     # Active in-call video dialog (remote + PiP)
```

---

## 🔨 How to Build & Run

### 1. Build via .NET CLI (on Windows):

```powershell
cd windows-desktop/EziyAttendanceMaster
dotnet restore
dotnet build -c Release
```

### 2. Publish as a Single-File Standalone `.exe`:

To ship a portable executable that runs on any Windows machine without requiring .NET pre-installed:

```powershell
dotnet publish EziyAttendanceMaster/EziyAttendanceMaster.csproj `
  -c Release `
  -r win-x64 `
  --self-contained true `
  -p:PublishSingleFile=true `
  -p:IncludeNativeLibrariesForSelfExtract=true `
  -o ./dist/publish
```

The output standalone executable will be located at:
`./dist/publish/EziyAttendanceMaster.exe`

---

## 🧪 Testing with Offline Demo Mode

If you don't have an active Odoo ERP backend or Linphone PBX server reachable during testing:
1. Launch the app.
2. In the **First-Time Setup** screen, enter `https://demo` as the **Server URL**.
3. Enter any company name, employee name, **8-digit Badge ID** (e.g. `12345678`), and PIN (e.g. `1234`).
4. Click **Authenticate & Continue**.
5. Test the **Hero Shift Dashboard**, **Webcam Face Capture**, **Attendance History**, and **SIP Intercom Station**.

