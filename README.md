# J.A.R.V.I.S — Personal AI Assistant

A local-first personal assistant project with a futuristic interface, being developed for Android and Windows.

## Current project pieces

- Responsive holographic dashboard and animated reactor
- Text commands and local demo responses
- Android tap-to-speak input through the installed speech-recognition service (offline preference is best-effort and not guaranteed)
- Browser speech recognition where supported
- Android opt-in, on-device **“Hey JARVIS”** wake-word detection using ONNX
- Persistent microphone notification with a Stop listening action
- Notification tap opens JARVIS and starts a voice-command session after the wake phrase
- Opt-in clap listener
- Installable PWA shell with basic offline caching
- Automated Android debug APK build through GitHub Actions
- No hosted AI API key and no external AI calls for wake-word detection

## Run the web preview

1. Open this repository in Codespaces.
2. Run:
   ```bash
   python3 -m http.server 8000
   ```
3. Open forwarded port 8000 from the Codespaces Ports panel.

Microphone access in the browser generally requires HTTPS or localhost. The web preview cannot run the Android background wake-word service.

## Android app

Open the `android/` folder in Android Studio with JDK 17 and Android SDK 35. Build the `app` debug variant.

Tap **HEY JARVIS** and grant microphone permission. The app starts an Android microphone foreground service and shows an ongoing notification with a Stop listening action. Say **“Hey JARVIS”**; the notification changes to indicate detection. Tap it to reopen the app and begin the follow-up voice-command session.

Android restricts apps from automatically bringing an activity to the foreground in many background situations. For that reason, JARVIS uses a notification tap after detecting the wake phrase instead of trying to force the screen open. Force-stop, permission revocation, battery restrictions, and OS policies can stop the listener. Start it again from the app when needed.

The wake-word engine runs locally. The follow-up speech-to-text uses the device's installed Android recognition service; the app requests offline recognition, but that preference is not guaranteed by every device.

To get a debug APK from GitHub, open the repository's **Actions** tab and download the `jarvis-debug-apk` artifact from a successful **Build JARVIS Android APK** run.

## GitHub Pages

To publish the web app, open repository Settings → Pages and select **GitHub Actions** as the deployment source. Then open Actions → **Deploy JARVIS to GitHub Pages** → Run workflow.

## Not implemented yet

- Local conversational language model
- Windows tray application
- Persistent local memory
- Permission-gated computer/device actions

A wake phrase is only a convenience trigger. It does not authorize sensitive actions.

## Third-party notices

The Android wake-word integration uses [openwakeword-android](https://github.com/msnilsen/openwakeword-android), whose library code is Apache-2.0. Its bundled pretrained wake-word model is from [openWakeWord](https://github.com/dscripka/openWakeWord) and is licensed CC BY-NC-SA 4.0. That model license is non-commercial and share-alike; do not use the bundled model in a commercial distribution without obtaining appropriate rights. The speech-embedding model is attributed to Google and licensed Apache-2.0. See the upstream project's license notices for full terms.

## Privacy

Microphone access is off by default and starts only after the user chooses HEY JARVIS or CLAP and grants permission. Android displays a persistent notification with a Stop control while a background listener runs. Wake-word inference is performed on-device; the wake-word audio is not uploaded by this project. The subsequent Android speech-recognition service may have its own network behavior and privacy terms.

See [Android notes](android/README.md) and [platform roadmap](docs/PLATFORM-ROADMAP.md).
