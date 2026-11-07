# J.A.R.V.I.S — Personal AI Assistant

A fresh, local-first personal assistant project with a futuristic Stark-inspired interface. The project is being built for Android and Windows, with a browser-based interface as the first milestone.

## Current milestone: Interface prototype

- Responsive dark holographic dashboard
- Animated reactor-inspired visual core
- Text command input and local demo responses
- Browser speech recognition where supported
- Browser text-to-speech
- Opt-in clap detection while the page is open
- No cloud AI API keys and no external AI calls in this prototype

## Run it

The current prototype is static HTML/CSS/JavaScript.

1. Open the repository in GitHub Codespaces.
2. Open a terminal.
3. Run a local static server, for example:

   ```bash
   python3 -m http.server 8000
   ```

4. Open the forwarded port 8000 using the Codespaces Ports panel.

Microphone access generally requires HTTPS or localhost. Browser speech recognition availability varies by browser and operating system.

## Important limitations

This first milestone is a foreground web prototype. It cannot listen after the browser tab is closed, after the app is force-stopped, or when the device is powered off. Browser APIs do not provide reliable always-on wake-word detection.

The planned native implementations are:
- Android: a user-enabled foreground microphone service, visible persistent notification, and platform-compliant background behavior.
- Windows: a user-enabled tray/background application with explicit microphone permission.
- Local AI: an optional local model runtime, with no hosted API key. Model size and speed depend on device hardware.

Clap detection in this prototype is experimental, requires microphone permission, and can be triggered by other sharp sounds. It runs only while the page is open.

## Roadmap

1. [x] Create responsive JARVIS dashboard
2. [x] Add browser voice input and spoken responses
3. [x] Add opt-in foreground clap detection
4. [ ] Add native Android application and foreground service
5. [ ] Add Windows desktop companion
6. [ ] Integrate a local language model
7. [ ] Add local preferences and persistent conversation memory
8. [ ] Add permission-gated device actions

## Privacy and safety

- Microphone access is requested only after the user activates a voice or clap feature.
- The prototype does not transmit microphone audio to a server.
- Device actions must require clear user permission and confirmation.
- Do not store passwords, authentication tokens, or other secrets in this repository.

## License

Personal learning project. Add a license if you decide to publish or distribute it.
