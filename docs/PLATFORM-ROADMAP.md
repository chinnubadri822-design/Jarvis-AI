# JARVIS Platform Roadmap

## Current milestone

The repository contains a responsive browser interface with text commands, browser speech input when available, speech synthesis, and opt-in clap detection while the page is open. The PWA shell can be installed and cached for offline access.

## Android native companion

The native Android WebView shell supports tap-to-speak through the installed Android speech-recognition service, an opt-in clap listener, and an opt-in local “Hey JARVIS” wake-word listener.

The wake-word path uses the openWakeWord Android ONNX pipeline and its bundled “Hey JARVIS” model. Inference runs on the device without a hosted API key. A user-started microphone foreground service displays an ongoing notification with a Stop action. When the phrase is detected, the notification changes; tapping it opens JARVIS and starts the follow-up voice session.

Android may block automatic activity launches from the background. The notification-tap flow is intentional. Force-stop, permission revocation, battery limits, and OS policies can stop listening. The speech-to-text service used after wake detection is provided by Android; its offline preference is best-effort and device-dependent.

The bundled wake-word model is CC BY-NC-SA 4.0. Review the root README's Third-party notices before redistributing or considering commercial use.

## Windows companion

A native Windows tray app can run while the user is signed in, with explicit microphone permission and visible pause/stop controls. Exiting the tray app stops listening.

## Local AI

Use an optional local model runtime rather than a hosted API. Model selection depends on device RAM, storage, and processor capability.

## Security

- Ask before sending messages, deleting files, changing settings, or controlling devices.
- Never execute arbitrary shell commands from model output.
- Use allow-listed actions and confirmations for consequential operations.
- Keep credentials and private data out of Git history.
