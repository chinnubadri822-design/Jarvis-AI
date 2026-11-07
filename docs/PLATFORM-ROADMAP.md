# JARVIS Platform Roadmap

## Current milestone

The repository contains a responsive browser interface. It supports text commands, browser speech input when available, speech synthesis, and opt-in clap detection while the page is open. The PWA shell can be installed and cached for offline access.

## Android native companion

A native Android app is required for background microphone features. A web page cannot keep listening after its browser is closed.

The Android implementation must:
- Request microphone permission only after the user enables the feature.
- Run capture only in a user-started foreground service with a persistent notification.
- Declare the microphone foreground-service type and permission for modern Android versions.
- Provide an obvious stop control in the app and notification.
- Process audio locally; do not upload microphone audio.
- Stop capture when disabled, permission is revoked, or the OS stops the service.
- Respect Android background-start restrictions. Do not promise automatic microphone startup after force-stop or at boot.
- Use a notification tap to reopen the interface when background activity launch is restricted.

Clap detection can be local, but is not a reliable identity or authorization signal and must never approve sensitive device actions.

"Hey JARVIS" needs a compatible on-device wake-word model/runtime. Model licensing and download size must be reviewed. No hosted API key is required, but a local model is still needed.

## Windows companion

A native Windows tray app can run while the user is signed in, with explicit microphone permission and visible pause/stop controls. Exiting the tray app stops listening.

## Local AI

Use an optional local model runtime rather than a hosted API. Model selection depends on device RAM, storage, and processor capability.

## Security

- Ask before sending messages, deleting files, changing settings, or controlling devices.
- Never execute arbitrary shell commands from model output.
- Use allow-listed actions and confirmations for consequential operations.
- Keep credentials and private data out of Git history.
