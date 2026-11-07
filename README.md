# J.A.R.V.I.S — Personal AI Assistant

A local-first personal assistant project with a futuristic interface, being developed for Android and Windows.

## Current features

- Responsive holographic dashboard and animated reactor
- Text commands and local demo responses
- Browser speech recognition where supported
- Browser text-to-speech
- Opt-in clap detection while the page is open
- Installable PWA shell with basic offline caching
- No hosted AI API keys and no external AI calls in this prototype

## Run in GitHub Codespaces

1. Open this repository in Codespaces.
2. Open a terminal and run:
   ```bash
   python3 -m http.server 8000
   ```
3. Open forwarded port 8000 from the Codespaces Ports panel.

Microphone access generally requires HTTPS or localhost. Speech recognition availability varies by browser and OS.

## Install on a phone

After HTTPS deployment, open the site in a supported browser and choose **Install app** or **Add to Home screen**. This is an installable web interface, not yet a native Android app; it cannot listen after the browser/app is closed.

## GitHub Pages

The workflow in `.github/workflows/pages.yml` deploys the static app on pushes to `main`. In repository Settings → Pages, select **GitHub Actions** as the deployment source if needed. Check the Actions tab for deployment status.

## Limitations

- The web prototype cannot listen after the browser tab closes, after force-stop, or when the device is powered off.
- Clap detection is experimental and may react to other sharp sounds.
- Background microphone access requires separate native Android and Windows apps with OS permissions and visible controls.
- Android may prevent opening an app window from the background; a notification can be shown instead.
- A local AI model is not connected yet; current replies are simple local demo responses.

## Roadmap

1. [x] Responsive dashboard
2. [x] Browser voice input and speech output
3. [x] Foreground clap detection
4. [x] Installable PWA shell and offline cache
5. [ ] Native Android foreground microphone service
6. [ ] Windows desktop companion
7. [ ] Local wake-word model
8. [ ] Local language model
9. [ ] Persistent local memory
10. [ ] Permission-gated device actions

## Privacy

Microphone access is requested only after the user activates a voice or clap feature. The web prototype does not transmit microphone audio. Native background listening must remain visible, user-controlled, and easy to stop. Never store passwords or tokens in this repository.

See [Platform Roadmap](docs/PLATFORM-ROADMAP.md).
