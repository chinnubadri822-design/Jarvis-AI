# JARVIS Android companion

This is the first native Android shell. It hosts the existing dashboard in a WebView and provides an opt-in microphone foreground service for clap detection.

## Build

Open the repository in Android Studio and open the `android/` folder. Use JDK 17 and Android SDK 35, then build the `app` debug variant.

## Microphone behavior

- Clap detection is off by default.
- The user must tap the CLAP control and grant microphone permission.
- Android displays an ongoing notification while microphone capture is active.
- The notification includes a Stop listening action.
- A clap updates the notification; tapping it opens JARVIS.
- Android may block an app from opening its screen automatically while in the background. This implementation intentionally uses a notification instead of trying to bypass that restriction.
- Force-stop, permission revocation, battery restrictions, and OS policies can stop the service.

This is clap detection only. Local "Hey JARVIS" wake-word detection and the local AI model are later milestones.
