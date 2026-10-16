# Android Guardian

Android Guardian is an Android-first, privacy-minded wellbeing companion. Its first UI increment uses a clear dashboard, quick routine buttons, spoken feedback, and optional speech input.

## MVP 0.2
- Spanish text-to-speech feedback when the Android speech engine is available.
- Optional speech input through Android's installed recognition service. Voice commands can start water, study, pause, going-out, battery, and weather actions; other phrases are placed into the reminder field for confirmation.
- Local reminder notifications scheduled through Android AlarmManager and persisted on-device.
- Pending reminders are restored after app launch, device reboot, and system time/time-zone changes; expired reminders are discarded and delivered reminders are removed from the local schedule.
- Quick actions for water, a rest break, studying, and getting ready to go out.
- Reminder manager showing pending local reminders and allowing cancellation with confirmation.
- Device health overview: internal storage used/free/total with a shortcut to Android's storage settings.
- Careful storage cleanup: user-selected folder scan for old temporary/partial downloads, old logs, old APK installers, old zero-byte files, screenshots older than 30 days (when their names identify them), and files of 100 MB or more. Large files, APKs, and screenshots are review candidates, not automatically classified as trash. Every deletion requires selection and a confirmation dialog; no automatic cleanup.
- Battery percentage display and threshold notifications while the app is open and receiving battery broadcasts, with 30% and 20% thresholds re-armed after charging above 33% and 23%.
- User-triggered weather lookup using GPS permission and Open-Meteo.
- Balanced, non-intrusive copy and explicit permission requests.

## Build
Requires JDK 17, Android SDK platform 35, and Gradle 8.11.1:
`gradle testDebugUnitTest assembleDebug`

## Important limitations
- Android can delay inexact alarms due to Doze, OEM battery optimization, or revoked notification permission.
- Reminder notifications use inexact alarms, so delivery can be delayed by Android. Exact alarms require special access and should only be added if a clear user-facing need justifies the permission.
- The cleaner can only inspect folders the user explicitly grants through Android's Storage Access Framework. Android restricts access to other apps' private data and some protected folders. Files may be permanently deleted by the selected document provider, so each deletion requires explicit confirmation.
- Battery threshold monitoring is not a guaranteed always-on background service. Android/OEM restrictions can stop background delivery; current monitoring is active while the app is open.
- Speech input depends on an installed Android recognition service and may use its own network/service policies. Text-to-speech depends on an installed voice.
- Weather uses the device's last known location only after the user taps the weather action and grants location permission. Coordinates are sent to Open-Meteo as part of the request.
- No diagnosis, medical advice, or treatment changes.
- The character is currently represented by lightweight Compose UI; the modular 2D rig and animated waifu are a later increment.

## Research notes and safe next steps
- Android's Storage Access Framework supports user-selected directory access: https://developer.android.com/training/data-storage/shared/documents-files
- Android's app-specific storage guidance: https://developer.android.com/training/data-storage/app-specific
- Alarm precision and special-access requirements: https://developer.android.com/develop/background-work/services/alarms
- Next safe increments: visual gallery review using Android's photo picker/MediaStore permissions, duplicate-image matching based on actual content (not filename alone), recurring routines, a focus/Pomodoro timer, and a local weekly wellbeing summary. These should be opt-in, run off the main UI thread, and always offer a review step before deletion. Avoid claims of magical RAM boosting and never uninstall or delete another app's private data silently.

## Paimon híbrida: primera base

La siguiente evolución está definida en [docs/PAIMON_HYBRID_ARCHITECTURE.md](docs/PAIMON_HYBRID_ARCHITECTURE.md). El objetivo es una compañera cálida con avatar anime, organización personal y chat remoto opcional, manteniendo las funciones esenciales en el teléfono.

- Router local determinista para batería, clima, agua, estudio, pausas, salida, ayuda y preparación de recordatorios.
- Los comandos desconocidos devuelven una respuesta de aclaración y no ejecutan acciones automáticamente.
- Pruebas unitarias para comandos conocidos, acentos, prioridad de recordatorios y texto desconocido.
- Esta fase no incorpora API keys, motor LLM local, permiso de accesibilidad ni micrófono de escucha permanente.
- La integración remota futura debe tener cancelación, timeout, presupuesto/límites y consentimiento; el modelo no recibe acceso directo a APIs Android ni puede ejecutar herramientas fuera de una lista permitida.

**TIMER AG-PAIMON-001:** 1–2 días efectivos estimados para completar y validar esta primera base. La ejecución de CI y el uso en un Xiaomi físico se registran por separado; el éxito de compilación no equivale a validar el comportamiento en el dispositivo.

## Privacy
Reminders are scheduled locally. Weather requests require network access and use Open-Meteo. The app does not request contacts, accessibility, or notification-listener access. Speech uses Android's system recognition flow rather than an app-hosted audio recorder.
