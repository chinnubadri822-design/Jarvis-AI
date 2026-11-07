# JARVIS Android companion

The Android shell hosts the dashboard in a WebView, supports tap-to-speak through Android's installed speech-recognition service, and includes two user-controlled microphone foreground services.

## Build

Open the repository's `android/` folder in Android Studio. Use JDK 17 and Android SDK 35, then build the `app` debug variant. The build downloads the wake-word runtime dependency from JitPack.

## Offline “Hey JARVIS” wake word

1. Open JARVIS and tap **HEY JARVIS**.
2. Grant microphone permission if Android asks.
3. Keep the ongoing **JARVIS is listening** notification visible.
4. Say **“Hey JARVIS”**.
5. When the notification says the wake phrase was detected, tap it. JARVIS opens and starts the follow-up voice-command session.
6. To stop background listening, use **Stop listening** in the notification or tap **WAKE ON** in the app.

Wake-word detection uses the bundled openWakeWord ONNX pipeline locally. No hosted API key or cloud wake-word call is used. The pretrained model is licensed CC BY-NC-SA 4.0 (non-commercial, share-alike); see the root README's Third-party notices before redistributing or using this model commercially.

The follow-up speech-to-text is provided by the Android device's installed recognition service. JARVIS requests its offline mode, but some devices may still require network access for speech recognition.

## Clap listener

- Clap detection is off by default.
- The user must tap **CLAP** and grant microphone permission.
- Android displays an ongoing notification while microphone capture is active.
- The notification includes a Stop listening action.
- A detected clap updates the notification; tapping it opens JARVIS.
- Clap detection can mistake other sharp sounds for a clap.

The clap and wake-word listeners share microphone access, so the app stops one before starting the other.

## Android limitations and privacy

- Android may block an app from opening its screen automatically while in the background. JARVIS uses a notification tap instead.
- Force-stop, permission revocation, battery restrictions, and OS policies can stop the service.
- Microphone capture is opt-in and is shown by Android's foreground-service notification.
- Wake-word inference is local; the wake-word audio is not uploaded by this project.
- Do not treat a wake phrase or clap as authorization for sensitive device actions.
