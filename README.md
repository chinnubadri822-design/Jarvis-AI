# J.A.R.V.I.S — Personal AI Assistant

A local-first personal assistant project with a futuristic interface, being developed for Android and Windows.

## Current project pieces

- Responsive holographic dashboard and animated reactor
- Text commands and local demo responses
- Browser speech recognition where supported and spoken replies
- Opt-in browser clap detection while the page is open
- Installable PWA shell with basic offline caching
- Native Android WebView shell
- Android opt-in foreground microphone service for clap detection
- Automated Android debug APK build through GitHub Actions
- No hosted AI API keys and no external AI calls in this prototype

## Run the web preview

1. Open this repository in Codespaces.
2. Run:
   ```bash
   python3 -m http.server 8000
   ```
3. Open forwarded port 8000 from the Codespaces Ports panel.

Microphone access in the browser generally requires HTTPS or localhost.

## Android app

Open the `android/` folder in Android Studio with JDK 17 and Android SDK 35. Build the `app` debug variant.

The Android app bundles the dashboard and provides an optional clap listener. Tap **CLAP** and grant microphone permission. Android shows an ongoing notification while the microphone service is active; the notification includes a Stop action. A detected clap updates the notification, which you can tap to reopen JARVIS.

Android restricts automatic activity launches from the background, so this implementation uses a notification rather than forcing the app onto the screen. Force-stop, permission revocation, and OS restrictions can stop the service.

To get a debug APK from GitHub, open the repository's **Actions** tab, run **Build JARVIS Android APK** (or push an Android change), and download the `jarvis-debug-apk` artifact from a successful run.

## GitHub Pages

To publish the web app, open repository Settings → Pages and select **GitHub Actions** as the deployment source. Then open Actions → **Deploy JARVIS to GitHub Pages** → Run workflow.

## Not implemented yet

- Local "Hey JARVIS" wake-word model
- Local language model / conversational AI
- Windows tray application
- Persistent local memory
- Permission-gated computer/device actions

The browser version cannot listen after its tab closes. The Android service currently detects claps only; it does not perform wake-word recognition. A clap is not authorization for sensitive actions.

## Privacy

Microphone access is off by default and starts only after the user chooses CLAP and grants permission. Android displays a persistent notification with a stop control while the native listener runs. Audio is processed locally and is not uploaded by this project. Never store passwords, API tokens, or private data in this repository.

See [Android notes](android/README.md) and [platform roadmap](docs/PLATFORM-ROADMAP.md).
