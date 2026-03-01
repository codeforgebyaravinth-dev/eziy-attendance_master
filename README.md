# Eziy Attendance Master — Android Enterprise Application

![Android Version](https://img.shields.io/badge/Android-SDK%2035%20(Min%2026)-2E7D32?style=flat-square&logo=android)
![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0.21-0B3D91?style=flat-square&logo=kotlin)
![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-00AEEF?style=flat-square&logo=jetpackcompose)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Coroutines-062B66?style=flat-square)

**Eziy Attendance Master** is a modern, enterprise-grade Android application designed for automated employee workforce tracking. It integrates **Facial Recognition**, **High-Precision GPS Location Validation**, and **Real-Time Push Notifications** directly with the **Odoo ERP Human Resources (`face_attendance`) Backend**.

---

## 📋 Executive Overview

The application empowers enterprise field workers and office employees to clock in/out within seconds while eliminating buddy-punching and location fraud through dual verification:
1. **Facial Verification:** Camera selfie capture compressed off-thread and transmitted securely to Odoo.
2. **GPS Geofencing:** Precise location verification via Google Play Services `FusedLocationProviderClient`.

---

## ✨ Key Features

- 🎨 **Modern Jetpack Compose Material 3 UI:** Pure white brand background, clean floating Material 3 navigation bar, and responsive layouts.
- 👑 **Hero Shift Dashboard:** Live check-in/out status card, daily shift progress bar (tracking an 8.0-hour shift target), user avatar greeting, and real-time status badges.
- 📸 **Dynamic Clock In / Clock Out:** Prominent action button automatically toggling between `CLOCK IN (FACE + GPS)` and `CLOCK OUT (FACE + GPS)`.
- 📍 **GPS Readiness Indicator:** Visual `📍 GPS High-Precision Location Ready` chip ensuring GPS locks before camera trigger.
- 🔒 **Encrypted Credential Storage:** AES-256 Android Keystore encryption via `EncryptedSharedPreferences` to secure authentication tokens, 8-digit Badge IDs, and PINs.
- 📜 **Attendance History & Timesheets:** Complete log of historical check-ins, check-outs, and total hours worked.
- 🧪 **Offline Demo Mode:** Built-in demo environment (`https://demo`) allowing complete UI, camera, GPS, and preference testing without requiring an active Odoo backend.
- 📡 **Real-Time MQTT Push Alerts:** Background service (`EziyMqttService`) receiving real-time attendance update alerts.

---

## 👨‍💻 Developer & Lead Engineer

**Aravinth V**  
*Android & Web Application Developer | Cryptography Specialist*

- **Domain Expertise:** Native Android Architecture (Jetpack Compose, MVVM, Coroutines), Web Application Development, Applied Cryptography & Secure Storage (Android Keystore, AES-256 Encryption, SSL/TLS Communications).
- **Project Role:** Lead Application Architect & Developer — Responsible for full codebase refactoring, security hardening, Jetpack Compose UI design, and Odoo REST API integration.

---

## 🛠️ Tech Stack & Libraries

| Domain | Technology / Library | Version / Details |
| :--- | :--- | :--- |
| **Language** | Kotlin | `2.0.21` (JVM 17 Compatibility Target) |
| **UI Framework** | Jetpack Compose | Material 3 Design System (`compose-bom:2024.10.01`) |
| **Navigation** | Navigation Compose | `2.8.4` |
| **Architecture** | MVVM + Clean Repository | Unidirectional Data Flow (UDF) |
| **Asynchronous Engine** | Kotlin Coroutines & `StateFlow` | `1.9.0` |
| **Networking** | Retrofit 2 + OkHttp + Gson | `2.11.0` (with logging interceptors) |
| **Security & Crypto** | Jetpack Security Crypto | `1.1.0-alpha06` (`EncryptedSharedPreferences` AES-256) |
| **Location Engine** | Google Play Services Location | `21.3.0` (`FusedLocationProviderClient`) |
| **Messaging Protocol** | Eclipse Paho MQTT v3 Client | `1.2.5` |
| **Unit Testing** | JUnit 4 + Coroutines Test | `4.13.2` / `1.9.0` |

---

## 🏗️ Architecture & Package Structure

The project follows official Android Clean Architecture guidelines, decoupling the UI, Domain/Logic, and Data layers:

```text
in.eziy.attendancemaster/
├── data/
│   ├── local/
│   │   └── SecurePreferencesManager.kt       # EncryptedSharedPreferences (AES-256)
│   ├── remote/
│   │   ├── ApiService.kt                      # Retrofit REST Interface
│   │   └── dto/                               # Data Transfer Objects (Login, Status, History, Mark)
│   └── repository/
│       ├── AttendanceRepository.kt            # Repository Interface
│       └── AttendanceRepositoryImpl.kt        # Implementation + Demo Mode Engine
├── location/
│   └── EziyLocationClient.kt                  # FusedLocationProviderClient Manager
├── service/
│   └── EziyMqttService.kt                     # Background Foreground Service for MQTT Alerts
├── ui/
│   ├── components/                            # Reusable Material 3 Eziy Components
│   ├── history/                            # Attendance History Screen & ViewModel
│   ├── home/                               # Hero Dashboard Screen & ViewModel
│   ├── navigation/                         # Bottom Navigation Definitions
│   ├── settings/                           # Settings Screen & ViewModel
│   ├── setup/                              # Login/Setup Screen & ViewModel
│   └── theme/                              # Eziy Material 3 Theme (Color, Type, Theme)
└── MainActivity.kt                            # Single-Activity Entry Point + Compose NavHost
```

---

## 🔑 Production Release Signing & Keystore Credentials

The application is configured with a production 2048-bit RSA release signing key and ProGuard code shrinking:

* **Keystore File:** `app/release-key.jks`
* **Keystore Config:** `keystore.properties`
* **Key Alias:** `eziy_release_key`
* **Key Passwords:** `eziy_release_password_2025`
* **Signed Release APK Location:** `app/build/outputs/apk/release/app-release.apk`

---

## 🚀 Building & Running Unit Tests

### System Requirements
* **IDE:** Android Studio Ladybug (2024.2.1) or higher
* **JDK:** Java 17
* **Android SDK:** Compile SDK `35`, Target SDK `35`, Min SDK `26` (Android 8.0 Oreo+)

### Build Commands

To run unit tests:
```bash
./gradlew :app:testDebugUnitTest
```

To build the signed production release APK:
```bash
./gradlew :app:assembleRelease
```

To build the debug APK:
```bash
./gradlew :app:assembleDebug
```

---

## 🧪 Testing with Demo Mode

To test the application without connecting to a live Odoo server:
1. Launch the app on any Android device or emulator.
2. On the **First-time Setup** screen, enter **`https://demo`** as the **Server URL**.
3. Fill in any company name, employee name, **8-digit Badge ID** (e.g. `12345678`), and PIN (e.g. `1234`).
4. Tap **Authenticate & Continue**.
5. Test the **Hero Dashboard**, **Clock In / Out**, **Location Fetching**, **Selfie Camera Capture**, **Attendance History**, and **Settings**.

---

## 📄 License & Client Delivery Notice

Developed by **Aravinth V** for **Eziy Enterprise Solutions**. All code, branding, and assets belong to the client.
