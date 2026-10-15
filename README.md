# Android Guardian

Android Guardian is an Android-first, privacy-minded wellbeing companion with a modular anime-waifu interface planned for later increments.

## MVP 0.1
- Local reminder notifications scheduled through Android AlarmManager.
- Battery percentage display and a foreground warning when opened at 30% or below.
- User-triggered weather lookup using GPS permission and Open-Meteo.
- Balanced, non-intrusive copy and explicit permission requests.
- Unit tests for reminder timing bounds and battery threshold policy.

## Build
Requires JDK 17, Android SDK platform 35, and Gradle 8.11 or compatible:
`gradle testDebugUnitTest assembleDebug`

## Important limitations
- Android can delay inexact alarms due to Doze, OEM battery optimization, or revoked notification permission.
- This increment does not yet persist a reminder history or re-register reminders after reboot.
- Battery status is refreshed when the app opens; continuous background monitoring and one-shot 30%/20% crossing notifications are not yet implemented.
- Weather uses the device's last known location only after the user taps the weather action and grants location permission. Coordinates are not stored by the app, but are sent to Open-Meteo as part of the request.
- No diagnosis, medical advice, or treatment changes.
- The character is currently represented by lightweight Compose UI; the modular 2D rig and animated waifu are a later increment.

## Privacy
Reminders are scheduled locally. Weather requests require network access and use Open-Meteo. The app does not request microphone, contacts, accessibility, or notification-listener access.
