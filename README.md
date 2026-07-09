# GpsApp

A native Android app (Kotlin + Jetpack Compose) that turns your phone's motion sensors into four independent utilities:

1. **Gesture Shortcuts** – trigger actions (flashlight, camera, mute call) with physical gestures (shake, flip, twist).
2. **Anti-Theft Guard** – arm your phone like a desk alarm; any movement or rotation sets off a loud, full-screen alarm.
3. **Dead Reckoning (PDR)** – estimate your position and heading using step counting and the compass, without relying on GPS (useful in tunnels, basements, and underground parking).
4. **Bubble Level & Clinometer** – check surface level with a 2-axis spirit bubble, or read pitch, roll, and slope in degrees.

> **Note:** Despite the project name, the app does not integrate live GPS/location APIs. Positioning is done entirely with onboard motion sensors (accelerometer, gyroscope, rotation vector) via **Pedestrian Dead Reckoning**.

---

## Table of Contents

- [Features](#features)
  - [1. Gesture Shortcuts](#1-gesture-shortcuts)
  - [2. Anti-Theft Guard](#2-anti-theft-guard)
  - [3. Dead Reckoning (PDR)](#3-dead-reckoning-pdr)
  - [4. Bubble Level & Clinometer](#4-bubble-level--clinometer)
- [Architecture](#architecture)
  - [High-Level Diagram](#high-level-diagram)
  - [Package Structure](#package-structure)
  - [Data Persistence](#data-persistence)
  - [Background Services](#background-services)
  - [Boot Persistence](#boot-persistence)
- [Tech Stack](#tech-stack)
- [Permissions](#permissions)
- [Testing](#testing)
- [Project Setup](#project-setup)
- [Building & Running](#building--running)

---

## Features

### 1. Gesture Shortcuts

Enable a background service that listens to the accelerometer and gyroscope and fires an action whenever a recognized gesture occurs.

| Gesture | Sensor(s) used | Detection logic | Triggered action |
|---|---|---|---|
| **Shake** | Accelerometer | Computes acceleration magnitude and its rate of change ("jerk"). Counts peaks above a configurable sensitivity threshold (default `20`, range `8–45`); requires **3 peaks within 700 ms**, with a **1.5 s cooldown** between triggers. | Toggle the **camera flashlight** (torch) via `CameraManager`. |
| **Flip (face-down)** | Accelerometer | Detects the Z-axis crossing into face-down (`z ≤ -6.5`) and staying there for **220 ms**, then re-arms once face-up (`z ≥ 6.5`) again. **1.5 s cooldown**. | **Mute an incoming call** (only while the phone is ringing) by muting the ring audio stream. Requires Notification Policy (Do Not Disturb) access. |
| **Twist** | Gyroscope | Counts peaks where the angular velocity around the Z-axis exceeds a sensitivity threshold (default `3.2 rad/s`, range `1.2–7`); requires **2 peaks within 1.2 s**, with a **1.6 s cooldown**. | **Launch the camera app** for a quick photo. |

**How it works end-to-end:**

```
SensorHub (accelerometer / gyroscope)
      │
      ▼
GestureEngine  (per-gesture detectors merged into one Flow<GestureType>)
      │
      ▼
GestureShortcutService  (foreground service, listens while enabled)
      │
      ▼
GestureAction.execute()  →  Success / Ignored / RequiresPermission / Failure
```

- Each gesture can be individually enabled/disabled, and shake/twist sensitivity is adjustable via sliders in **Settings → Gestures**.
- Settings are persisted with **DataStore Preferences** (`gesture_settings` store) so they survive app restarts and device reboots.
- The service is a `LifecycleService` running with `START_STICKY`, backed by a foreground notification ("Gesture shortcuts active").

### 2. Anti-Theft Guard

Turns your phone into a motion-triggered alarm — useful when you leave it on a desk or table.

**State machine:**

```
DISARMED → ARMING → CALIBRATING → MONITORING → TRIGGERED
```

| Stage | Behavior |
|---|---|
| **Arming** | Grace period after arming (default **8 s**, range 3–30 s) so you can put the phone down. |
| **Calibrating** | Establishes a stillness baseline over **1.2 s** using an exponential low-pass filter (`α = 0.85`); restarts calibration if motion is detected. |
| **Monitoring** | Watches for two kinds of triggers relative to the calibrated baseline: |
| | • **Movement** – sustained acceleration deviation ≥ sensitivity (default `1.9`, range `0.8–4.5`) for **250 ms**. |
| | • **Rotation** – integrated gyroscope angle exceeds a threshold (default `35°`, range `12–120°`). |
| **Triggered** | Alarm fires and stays active until the phone is unlocked (disarmed). |

**When triggered:**
- `TheftAlarmService` plays the device's default alarm sound at max volume on the alarm stream (with optional vibration pattern), and posts a high-priority notification with a **full-screen intent**.
- `TheftAlarmActivity` is launched over the lock screen (`showWhenLocked`, `turnScreenOn`) displaying a large "ANTI-THEFT ALARM" warning.
- Unlocking the device (`ACTION_USER_PRESENT`) automatically disarms the guard and stops the alarm.
- A **Test Alarm** button in settings lets you preview the sound/vibration for 4 seconds without arming the guard.

Settings (armed state, sensitivities, arm delay, vibration) are persisted via DataStore (`anti_theft_settings` store).

### 3. Dead Reckoning (PDR)

Estimates a 2D position trail without GPS, by combining step detection with compass heading — designed for GPS-denied environments like tunnels or parking garages.

- **Step detection** (`StepDetector`): removes gravity using a low-pass filter (`α = 0.9`) to get linear acceleration, then flags a step when the acceleration magnitude peaks above a threshold (`1.1`) with a minimum **250 ms** between steps (mimicking heel-strike impulses).
- **Heading**: derived from the device's fused `TYPE_ROTATION_VECTOR` sensor (azimuth in radians).
- **Position update**, on every detected step:

  ```
  x += stepLength × sin(heading)
  y += stepLength × cos(heading)
  ```

  Default step length is **0.75 m**, adjustable between 0.4–1.1 m for personal calibration.
- **UI**: a live canvas draws the walked path on a grid with a heading arrow, plus running stats for step count, heading (degrees), and X/Y coordinates. Start / Stop / Reset controls are provided, and tracking runs only while the screen is open (no background service).

### 4. Bubble Level & Clinometer

Uses the accelerometer to show whether a surface is level and to read tilt angles — no background service; sensor listening runs only while the **Level** tab is visible (lifecycle-aware via `repeatOnLifecycle`).

**Sensor processing:**

- **Low-pass filter** on each accelerometer axis (`α = 0.85` default, adjustable 0.7–0.95):

  ```
  filtered = α × filtered + (1 − α) × sample
  ```

- **Pitch** (forward/back tilt):

  ```
  pitch = atan2(−x, √(y² + z²))
  ```

- **Roll** (left/right tilt):

  ```
  roll = atan2(y, z)
  ```

- **Slope** (overall tilt from horizontal):

  ```
  slope = atan2(√(x² + y²), |z|)
  ```

  All angles are converted to degrees. **Calibration** stores the current pitch/roll as offsets so the resting orientation reads as zero. A surface is **level** when both `|pitch|` and `|roll|` are within the tolerance (default **1.0°**, range **0.5–5.0°**). Bubble position maps pitch/roll to normalized offsets in `[−1, 1]` using a 15° full deflection.

**How it works end-to-end:**

```
SensorHub (accelerometer)
      │
      ▼
LevelEngine  →  TiltCalculator  (low-pass filter, angle math, calibration, freeze)
      │
      ▼
LevelViewModel  (screen-active-only listening)
      │
      ▼
LevelScreen  →  BubbleLevelCanvas / ClinometerPanel
```

**UI:**

- **Display modes** (toggle via segmented control): **Bubble** (2-axis spirit level with crosshairs and center target), **Clinometer** (pitch, roll, slope in degrees with LEVEL badge), or **Combined** (both).
- **Controls**: **Calibrate** (zero current orientation), **Hold** / **Resume** (freeze live readings), **Reset** (clear calibration offsets).
- **Settings**: level-tolerance slider and filter-smoothing slider; persisted via DataStore (`level_settings` store).

---

## Architecture

The app follows an **MVVM** pattern: Compose screens observe `StateFlow`s exposed by `ViewModel`s, which in turn read/write settings through repository classes backed by DataStore, and coordinate sensor-driven "engine" classes.

### High-Level Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          MainActivity (Compose)                               │
│   Bottom Navigation: Gestures | Anti-Theft | Dead Reckoning | Level          │
└───────────┬─────────────────┬─────────────────┬─────────────────┬────────────┘
            │                 │                 │                 │
   GestureSettingsViewModel  AntiTheftViewModel  DeadReckoningViewModel  LevelViewModel
            │                 │                 │                 │
  GestureSettingsRepository  AntiTheftSettingsRepository   (in-memory)  LevelSettingsRepository
            │  (DataStore)    │  (DataStore)                          │  (DataStore)
            │                 │                 │                 │
  GestureShortcutService     TheftAlarmService          PdrEngine    LevelEngine
    (foreground service)     (foreground service)     (ViewModel-scoped) (ViewModel-scoped)
            │                 │                 │                 │
     GestureEngine          TheftGuardEngine        StepDetector + PdrTracker  TiltCalculator
            │                 │                 │                 │
            └─────────────────┴─────────────────┴─────────────────┘
                                        ▼
                         SensorHub (Accelerometer, Gyroscope, Rotation Vector)
```

Cross-cutting piece: `CallStateMonitor` feeds live call state (`IDLE`/`RINGING`/`OFFHOOK`) into `GestureShortcutService` to gate the mute-on-flip action.

### Package Structure

```
com.rama.gpsapp
├── MainActivity.kt                 # Single-activity host, bottom nav (4 routes)
├── actions/                        # Gesture-triggered actions
│   ├── GestureAction.kt            #   interface + ActionResult contract
│   ├── ActionResult.kt             #   Success / Ignored / RequiresPermission / Failure
│   ├── FlashlightToggleAction.kt
│   ├── LaunchCameraAction.kt
│   └── MuteIncomingCallAction.kt
├── gesture/                        # Sensor → gesture detection
│   ├── GestureEngine.kt
│   ├── GestureType.kt
│   ├── ShakeDetector.kt
│   ├── FlipDetector.kt
│   └── TwistDetector.kt
├── theft/                          # Anti-theft alarm subsystem
│   ├── TheftGuardEngine.kt
│   ├── TheftGuardDetector.kt       #   arm/calibrate/monitor state machine
│   ├── TheftTrigger.kt
│   ├── TheftAlarmService.kt
│   ├── TheftAlarmActivity.kt
│   ├── TheftAlarmState.kt
│   └── AlarmSoundPlayer.kt
├── pdr/                             # Pedestrian dead reckoning
│   ├── PdrEngine.kt
│   ├── PdrTracker.kt
│   ├── StepDetector.kt
│   └── PdrPosition.kt
├── level/                           # Bubble level & clinometer
│   ├── LevelEngine.kt
│   ├── TiltCalculator.kt            #   low-pass filter, pitch/roll/slope math
│   ├── LevelReading.kt
│   └── LevelDisplayMode.kt
├── sensor/                          # Sensor abstraction
│   ├── SensorHub.kt
│   └── SensorSample.kt
├── call/                            # Telephony state for mute-on-flip
│   ├── CallStateMonitor.kt
│   └── CallState.kt
├── data/                            # Settings models, DataStore, repositories
│   ├── GestureSettings.kt / GesturePreferences.kt / GestureSettingsRepository.kt
│   ├── AntiTheftSettings.kt / AntiTheftPreferences.kt / AntiTheftSettingsRepository.kt
│   └── LevelSettings.kt / LevelPreferences.kt / LevelSettingsRepository.kt
├── service/
│   ├── GestureShortcutService.kt
│   └── BootCompletedReceiver.kt
└── ui/                              # Compose screens + ViewModels + theme
    ├── gestures/  (GestureSettingsScreen, GestureSettingsViewModel)
    ├── theft/     (AntiTheftSettingsScreen, AntiTheftViewModel)
    ├── deadreckoning/ (DeadReckoningScreen, DeadReckoningViewModel)
    ├── level/       (LevelScreen, LevelViewModel, BubbleLevelCanvas, ClinometerPanel)
    └── theme/     (Color, Type, Theme)
```

### Data Persistence

Three independent **Jetpack DataStore (Preferences)** stores back all user-configurable settings, so they survive process death and reboot:

| Store name | Model | Fields |
|---|---|---|
| `gesture_settings` | `GestureSettings` | `serviceEnabled`, per-gesture toggles (shake/flip/twist), `shakeSensitivity`, `twistSensitivity` |
| `anti_theft_settings` | `AntiTheftSettings` | `armed`, `movementSensitivity`, `rotationSensitivityDegrees`, `armDelaySeconds`, `vibrateEnabled` |
| `level_settings` | `LevelSettings` | `displayMode`, `levelToleranceDegrees`, `filterAlpha`, `calibrationPitchOffsetDegrees`, `calibrationRollOffsetDegrees` |

Repositories (`GestureSettingsRepository`, `AntiTheftSettingsRepository`, `LevelSettingsRepository`) expose the settings as `Flow`s and provide coerced setters used by the ViewModels (and the background services where applicable).

### Background Services

Both long-running features run as **foreground `LifecycleService`s** with `foregroundServiceType="specialUse"`, each with its own notification channel:

- **`GestureShortcutService`** (subtype `gesture-shortcuts`) — collects sensor + settings + call-state flows and dispatches gesture actions.
- **`TheftAlarmService`** (subtype `anti-theft-alarm`) — runs the arm/calibrate/monitor state machine and raises the full-screen alarm when triggered.

### Boot Persistence

`BootCompletedReceiver` listens for `ACTION_BOOT_COMPLETED` and, using `goAsync()`, reads both settings repositories to restart `GestureShortcutService` (if it was enabled) and `TheftAlarmService` (if the guard was armed) — so protection survives a device restart.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | **Kotlin 2.2** |
| UI Toolkit | **Jetpack Compose** + **Material 3** |
| Architecture Pattern | **MVVM** (`ViewModel` + `StateFlow`/`Flow`) |
| Navigation | **Navigation Compose** (bottom navigation bar, 4 routes) |
| Concurrency | **Kotlin Coroutines & Flow** |
| Local Persistence | **Jetpack DataStore (Preferences)** |
| Background Execution | **Foreground `LifecycleService`s** |
| Sensors | Android **`SensorManager`** (Accelerometer, Gyroscope, Rotation Vector) via a custom `SensorHub` |
| Telephony | `TelephonyManager` / `TelephonyCallback` (API 31+) with legacy `PhoneStateListener` fallback |
| Build System | Gradle (Kotlin DSL) with version catalogs (`libs.versions.toml`) |
| Min / Target / Compile SDK | 24 / 37 / 37 |
| Testing | JUnit4 (unit), Espresso + Compose UI Test (instrumented) |

---

## Permissions

| Permission | Purpose |
|---|---|
| `READ_PHONE_STATE` | Detect incoming calls, to support the flip-to-mute gesture |
| `ACCESS_NOTIFICATION_POLICY` | Access Do Not Disturb policy to mute the ringer |
| `POST_NOTIFICATIONS` | Show foreground-service and alarm notifications (Android 13+) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Run the gesture-shortcut and anti-theft services in the background |
| `RECEIVE_BOOT_COMPLETED` | Automatically restart enabled services after device reboot |
| `VIBRATE` | Vibration pattern for the anti-theft alarm |
| `USE_FULL_SCREEN_INTENT` | Show the alarm screen over the lock screen |

The flashlight and camera-launch actions don't require the `CAMERA` runtime permission on most devices (torch control and launching the system camera app via intent).

---

## Testing

**Unit tests** (`app/src/test/java/com/rama/gpsapp/`):

| File | Covers |
|---|---|
| `gesture/ShakeDetectorTest.kt` | Peak-based shake triggering vs. gentle motion |
| `gesture/FlipDetectorTest.kt` | Face-down hold triggering vs. brief tilts |
| `gesture/TwistDetectorTest.kt` | Gyro-peak-based twist triggering vs. low angular velocity |
| `theft/TheftGuardDetectorTest.kt` | Arm delay, calibration restart, movement/rotation triggers, disarm reset |
| `pdr/StepDetectorTest.kt` | Step-peak detection, threshold guard, minimum interval debounce |
| `pdr/PdrTrackerTest.kt` | Position accumulation for north/east headings, reset behavior |
| `level/TiltCalculatorTest.kt` | Low-pass filtering, pitch/roll/slope angles, calibration offsets, level tolerance, freeze |
| `ExampleUnitTest.kt` | Placeholder sanity test |

**Instrumented tests** (`app/src/androidTest/java/com/rama/gpsapp/`):

| File | Covers |
|---|---|
| `ExampleInstrumentedTest.kt` | Verifies the app's package name/context |

Run tests with:

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires a connected device/emulator)
./gradlew connectedAndroidTest
```

---

## Project Setup

**Prerequisites:**
- Android Studio (latest stable) or a JDK 17+ toolchain with the Android SDK installed.
- Android SDK Platform 37 (compile/target SDK) and a device/emulator running Android 7.0 (API 24) or newer.

**Clone & open:**

```bash
git clone <repository-url>
cd GpsApp
```

Open the project folder in Android Studio and let Gradle sync, or build from the command line (see below).

---

## Building & Running

```bash
# Build a debug APK
./gradlew assembleDebug

# Install & run on a connected device/emulator
./gradlew installDebug
```

On Windows PowerShell, use `gradlew.bat` instead of `./gradlew`:

```powershell
.\gradlew.bat assembleDebug
```

After installing, grant the runtime permissions requested in-app (phone state, notifications, Do Not Disturb access) to unlock the flip-to-mute and full alarm-notification behavior.
