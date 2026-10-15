# Android Guardian

Android Guardian is an Android-first, privacy-minded wellbeing companion. Its first UI increment uses a clear dashboard, quick routine buttons, spoken feedback, and optional speech input.

## MVP 0.2
- Spanish text-to-speech feedback when the Android speech engine is available.
- Optional speech input through Android's installed recognition service. Voice commands can start water, study, pause, going-out, battery, and weather actions; other phrases are placed into the reminder field for confirmation.
- Local reminder notifications scheduled through Android AlarmManager.
- Quick actions for water, a rest break, studying, and getting ready to go out.
- Battery percentage display and threshold notifications while the app is open and receiving battery broadcasts, with 30% and 20% thresholds re-armed after charging above 33% and 23%.
- User-triggered weather lookup using GPS permission and Open-Meteo.
- Balanced, non-intrusive copy and explicit permission requests.

## Build
Requires JDK 17, Android SDK platform 35, and Gradle 8.11.1:
`gradle testDebugUnitTest assembleDebug`

## Important limitations
- Android can delay inexact alarms due to Doze, OEM battery optimization, or revoked notification permission.
- Reminder list is session-only and reminders are not re-registered after reboot yet.
- Battery threshold monitoring is not a guaranteed always-on background service. Android/OEM restrictions can stop background delivery; current monitoring is active while the app is open.
- Speech input depends on an installed Android recognition service and may use its own network/service policies. Text-to-speech depends on an installed voice.
- Weather uses the device's last known location only after the user taps the weather action and grants location permission. Coordinates are sent to Open-Meteo as part of the request.
- No diagnosis, medical advice, or treatment changes.
- The character is currently represented by lightweight Compose UI; the modular 2D rig and animated waifu are a later increment.

## Privacy
Reminders are scheduled locally. Weather requests require network access and use Open-Meteo. The app does not request contacts, accessibility, or notification-listener access. Speech uses Android's system recognition flow rather than an app-hosted audio recorder.
