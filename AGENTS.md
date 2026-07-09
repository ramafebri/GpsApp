# AGENTS.md

Guidance for AI coding agents (and humans) working in this repository.

## App Overview

**GpsApp** is a native Android app (Kotlin + Jetpack Compose) that turns the phone's motion
sensors into four independent utilities, presented as four bottom-navigation tabs in a
single `MainActivity`:

1. **Gesture Shortcuts** — a background service listens to the accelerometer/gyroscope and
   fires an action when it recognizes a **shake** (toggle flashlight), **flip face-down**
   (mute an incoming call), or **twist** (launch the camera).
2. **Anti-Theft Guard** — arms the phone like a desk alarm (`DISARMED → ARMING → CALIBRATING →
   MONITORING → TRIGGERED`); any movement or rotation beyond a calibrated baseline sets off a
   full-screen, lock-screen alarm until the device is unlocked.
3. **Dead Reckoning (PDR)** — estimates a 2D position trail using step detection (accelerometer)
   plus compass heading (rotation vector sensor), with no GPS/location APIs involved. Useful for
   GPS-denied environments (tunnels, basements, parking garages).
4. **Bubble Level & Clinometer** — uses the accelerometer to show a 2-axis spirit bubble level
   and read pitch, roll, and slope in degrees; supports calibration, hold/freeze, and tolerance
   settings. Runs only while the Level tab is visible (no background service).

> Despite the project name, there is **no live GPS/location integration**. All positioning is
> done with onboard motion sensors via Pedestrian Dead Reckoning (PDR).

See `README.md` for the full feature breakdown, architecture diagrams, detection thresholds,
permissions table, and package structure — read it before making non-trivial changes.

## Architecture at a Glance

MVVM: Compose screens observe `StateFlow`s from `ViewModel`s, which read/write settings through
repository classes backed by Jetpack DataStore, and coordinate sensor-driven "engine" classes.

```
com.rama.gpsapp
├── MainActivity.kt        # Single-activity host, 4-route bottom nav
├── actions/                # Gesture-triggered actions (flashlight, camera, mute call)
├── gesture/                 # Sensor -> gesture detection (Shake/Flip/Twist + GestureEngine)
├── theft/                   # Anti-theft state machine, alarm service/activity
├── pdr/                      # Pedestrian dead reckoning (StepDetector, PdrTracker, PdrEngine)
├── level/                     # Bubble level & clinometer (TiltCalculator, LevelEngine)
├── sensor/                    # SensorHub abstraction over Android SensorManager
├── call/                       # Telephony state monitor (for flip-to-mute)
├── data/                        # Settings models + DataStore repositories
├── service/                      # Foreground services + boot receiver
└── ui/                             # Compose screens, ViewModels, theme (per feature package)
```

Three independent DataStore (Preferences) stores persist settings: `gesture_settings`,
`anti_theft_settings`, and `level_settings`. Both `GestureShortcutService` and `TheftAlarmService`
are foreground `LifecycleService`s (`foregroundServiceType="specialUse"`) restarted on boot by
`BootCompletedReceiver` if they were previously enabled/armed. Level and PDR run only while their
tab is visible (ViewModel-scoped engines, no background service).

## Tech Stack

- Kotlin 2.2, Jetpack Compose + Material 3, Navigation Compose
- MVVM with Kotlin Coroutines/Flow
- Jetpack DataStore (Preferences) for persistence
- Gradle Kotlin DSL with version catalogs (`gradle/libs.versions.toml`)
- Min/Target/Compile SDK: 24 / 37 / 37
- Tests: JUnit4 (unit), Espresso + Compose UI Test (instrumented)

## Build & Test Commands

This is a Windows/PowerShell environment; use `gradlew.bat`.

```powershell
# Build a debug APK
.\gradlew.bat assembleDebug

# Run unit tests
.\gradlew.bat test

# Run instrumented tests (requires a connected device/emulator)
.\gradlew.bat connectedAndroidTest

# Install & run on a connected device/emulator
.\gradlew.bat installDebug
```

Always run `.\gradlew.bat test` after modifying detector/engine logic in `gesture/`, `theft/`,
`pdr/`, or `level/` — these packages have corresponding unit tests under `app/src/test/java/com/rama/gpsapp/`.

## Conventions

- Package-by-feature: new sensor-driven features get their own top-level package (mirroring
  `gesture/`, `theft/`, `pdr/`, `level/`) with detector/engine classes, a `data/` settings model +
  repository, and a `ui/<feature>/` Compose screen + ViewModel.
- Detector classes (e.g. `ShakeDetector`, `FlipDetector`, `TwistDetector`, `StepDetector`,
  `TiltCalculator`) are plain Kotlin classes that consume `SensorSample`s and are unit-testable
  in isolation — keep new detection logic in this style rather than embedding it directly in
  services.
- Settings are modeled as immutable data classes, persisted via DataStore, and exposed as
  `Flow`s from repositories; ViewModels expose `StateFlow`s to Compose screens.
- Background/long-running behavior belongs in a foreground `LifecycleService` with its own
  notification channel and `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`, matching the existing
  `GestureShortcutService` / `TheftAlarmService` pattern.
- Keep `README.md` in sync when adding features, changing detection thresholds/permissions, or
  altering the architecture.

## Notes for Agents

- Do not add real GPS/location API integrations without explicit user request — the app is
  intentionally sensor-only (PDR), per the README's note.
- After changing sensor thresholds or state-machine logic, check the corresponding test file
  in `app/src/test/java/com/rama/gpsapp/` and update/add tests as needed.
- Required runtime permissions are declared in `app/src/main/AndroidManifest.xml` and documented
  in the README's Permissions table — update both if permissions change.
