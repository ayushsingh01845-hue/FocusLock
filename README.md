# FocusLock

A real Android focus / app-blocker app: Kotlin + Jetpack Compose + Material 3.

## ⚠️ About the APK

This environment has **no Android SDK, no Gradle, and no internet access**, so I could not
run an actual Gradle build here — there's no way for me to produce a signed, installable
`.apk` file in this sandbox. What you're getting instead is the **complete, real Android
Studio project source**, ready to open and build into an APK on your own machine in a few
minutes (steps below). Everything the spec asked for is implemented as working Kotlin code,
not mockups.

## How to build the APK yourself

1. Install **Android Studio** (Koala or newer) if you don't have it.
2. Unzip this project, then `File → Open` the `FocusLock/` folder.
3. Let Gradle sync (it will download the SDK/Gradle bits automatically).
4. `Build → Generate Signed Bundle / APK → APK` (or just `Build → Build APK(s)` for a debug
   build you can sideload immediately).
5. Install the resulting `app-debug.apk` / `app-release.apk` on your phone (`adb install
   app-release.apk`, or copy it over and tap it with "install unknown apps" allowed).

Command line instead of Android Studio, once you have the SDK installed:
```
cd FocusLock
./gradlew assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
```
(You'll need to add a `gradle-wrapper.jar`; Android Studio generates it automatically on
first sync, or run `gradle wrapper` once if you have Gradle installed globally.)

## What's implemented

- **Home** — status card, today's focus time, streak, blocked-app count, Quick Start.
- **Create session** — task name, mode presets (Study / Work / Deep Focus / Custom /
  Pomodoro), duration presets + custom minutes, app picker.
- **App picker** — real installed-app list via `PackageManager`, search, select all/clear all.
- **Real blocking** — `FocusAccessibilityService` watches foreground-app switches
  (`TYPE_WINDOW_STATE_CHANGED` only — it never reads screen content) and, when a blocked
  package comes to the front, sends the user home and shows `BlockActivity` full-screen.
  This is a legitimate, Play-Store-compatible mechanism — no root, no exploits.
- **Reliable timer** — `FocusForegroundService` runs a `CountDownTimer` backed by a
  persisted end-timestamp (DataStore), so it survives the app being minimized, the screen
  locking, or even a reboot (`BootReceiver` resumes it from the stored timestamp, not from
  zero).
- **Permissions screen** — Usage Access, Accessibility, Notifications, each with a plain-
  language "why", a live granted/not-granted check, and a button straight to the right
  system settings screen.
- **Pomodoro mode** — auto-alternating focus/break segments; blocked apps are freed during
  breaks.
- **Stats** — today/week/month totals, streak, completed count, longest session, most-
  blocked apps.
- **History** — every session logged locally with date, task, duration, completed/ended
  status.
- **Settings** — theme (system/light/dark), Pomodoro lengths, sound/vibration toggles,
  reset-all-data.
- **Anti-bypass design** — no prominent "stop blocking" button on the active-session screen;
  ending early is a small, separately-labelled "Emergency end session" link behind a
  confirmation dialog, so the user is never truly locked out of their own phone.
- **Privacy** — everything is stored with Jetpack DataStore, on-device only. There is no
  networking code anywhere in this project.

## Known platform limitations (explained honestly, not faked)

- Android does not let any non-device-owner app *literally* prevent another app's process
  from running — FocusLock uses the strongest legitimate approach available (Accessibility
  service intercepting the foreground-switch event and covering it instantly with the block
  screen), which is the same technique used by real published focus-blocker apps.
- On some OEM skins (Xiaomi/MIUI, Oppo, etc.) you may also need to disable battery
  optimization for FocusLock so the accessibility/foreground services aren't killed — worth
  adding a one-line note in the Permissions screen for your target devices.
- App icons in the picker currently show a colored initial instead of the real app icon
  (kept dependency-free); swapping in `PackageManager#getApplicationIcon` + an `AndroidView`
  or Coil is a small, isolated change in `AppSelectionScreen.kt`.

## Project layout

```
app/src/main/java/com/focuslock/app/
  MainActivity.kt, BlockActivity.kt, FocusLockApplication.kt
  data/            Models.kt, PreferencesManager.kt (DataStore), AppRepository.kt
  service/         FocusAccessibilityService.kt, FocusForegroundService.kt, BootReceiver.kt
  ui/              FocusViewModel.kt
  ui/navigation/   Screen.kt, NavGraph.kt
  ui/screens/      HomeScreen, CreateSessionScreen, AppSelectionScreen, FocusTabScreen,
                   PermissionsScreen, StatsScreen, HistoryScreen, SettingsScreen
  ui/theme/        Color.kt, Type.kt, Theme.kt
  util/            TimeUtils.kt
```
