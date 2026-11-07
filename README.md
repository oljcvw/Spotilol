<p align="center">
  <img src="art/respotifuck-logo.png" alt="ReSpotifuck logo" width="128" height="128">
</p>

<h1 align="center">ReSpotifuck</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Status-Online-2ea44f?style=for-the-badge&labelColor=0d0d0d" alt="status: online">
  &nbsp;
  <a href="https://github.com/ReSpotF-ck/-ReSpotif-ck-App/tree/main/release-assets">
    <img src="https://img.shields.io/badge/Download-APK-E60023?style=for-the-badge&logo=android&logoColor=white&labelColor=0d0d0d" alt="download APK">
  </a>
  &nbsp;
  <a href="https://github.com/ReSpotF-ck/-ReSpotif-ck-App/tree/main/release-assets">
    <img src="https://img.shields.io/github/v/release/ReSpotF-ck/-ReSpotif-ck-App?style=for-the-badge&logo=github&labelColor=0d0d0d&color=E60023" alt="latest release">
  </a>
  &nbsp;
  <a href="https://github.com/ReSpotF-ck/-ReSpotif-ck-App/issues">
    <img src="https://img.shields.io/github/issues/ReSpotF-ck/-ReSpotif-ck-App?style=for-the-badge&labelColor=0d0d0d&color=E60023" alt="open issues">
  </a>
  &nbsp;
  <a href="https://github.com/ReSpotF-ck/-ReSpotif-ck-App/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/ReSpotF-ck/-ReSpotif-ck-App/build-debug.yml?branch=main&style=for-the-badge&labelColor=0d0d0d&color=E60023" alt="build status">
  </a>
</p>

<p align="center">An independent Android wrapper for Spotify's web player, with ad filtering and playback controls.</p>

ReSpotifuck is a community fork of [Spotilol](https://github.com/oljcvw/Spotilol), which builds on reverse-engineering work from [Spotifuck](https://github.com/deviato/Spotifuck). It is not affiliated with or endorsed by Spotify.

## Download

> **Note:** ReSpotifuck downloads are not publicly listed or advertised. You have to search for them; they are not on app stores or download sites.

<p>
  <a href="https://github.com/ReSpotF-ck/-ReSpotif-ck-App/tree/main/release-assets"><img src="https://img.shields.io/badge/Download%20ReSpotifuck-APK-E60023?style=for-the-badge&logo=android&logoColor=white&labelColor=0d0d0d" alt="Download ReSpotifuck APK"></a>
  &nbsp;
  <a href="https://github.com/ReSpotF-ck/-ReSpotif-ck-App/tree/main/archive"><img src="https://img.shields.io/badge/Old%20versions-archive-555555?style=for-the-badge&logo=github&labelColor=0d0d0d" alt="Old versions archive"></a>
</p>

Get the latest APK from the [release-assets folder](https://github.com/ReSpotF-ck/-ReSpotif-ck-App/tree/main/release-assets). Download the APK and install it on your device. Older APKs and retired files are in the [archive folder](https://github.com/ReSpotF-ck/-ReSpotif-ck-App/tree/main/archive). Android may ask you to allow installs from your browser or file manager.

## Features

- Filters audio ads
- Media notification with play/pause, skip, seek, and like controls
- Lock screen, Bluetooth, and Wear OS controls
- Autoplay modes: off, once at start, or permanent
- Mobile layout tweaks, AMOLED mode, and custom CSS
- Keeps the screen on during playback
- Browse your library through Spotify's web app
- Checks GitHub Releases for updates

## Requirements

- Android 9.0 or newer
- A Spotify account
- Android System WebView or Chrome

## First launch

The app creates a local certificate used by its proxy. Android requires you to install it before continuing:

1. Open ReSpotifuck and tap **Export .pem**.
2. Open **Settings > Security > Encryption & credentials > Install a certificate > CA certificate**.
3. Select **ReSpotifuck_CA.pem** in Downloads and follow Android's prompts.
4. Return to ReSpotifuck and tap **Check**.

The certificate and proxy key are stored on your device. If you clear credential storage or reinstall the app, you may need to repeat setup.

## Privacy and legal notice

On first launch, the app explains its third-party service, copyright, ad-filtering, and proxy behavior. The proxy listens on the device only and can decrypt and modify HTTPS traffic routed through the app's WebView. Installing its CA certificate adds a trust anchor on your device; remove it from **Settings > Security > Encryption & credentials > User credentials** if you stop using the app. The proxy is not a VPN, encrypted DNS service, or anonymity tool, and it does not hide activity from Spotify or your network provider. DNS uses the device's configured resolver.

ReSpotifuck is an independent community project, not affiliated with Spotify. Service features can change or conflict with third-party terms. Users are responsible for complying with applicable laws, service terms, and copyright requirements. This notice is not a grant of rights to Spotify content.

## Build from source

~~~bash
git clone https://github.com/ReSpotF-ck/-ReSpotif-ck-App.git
cd -- -ReSpotif-ck-App
./gradlew assembleDebug
~~~

APKs are written to `app/build/outputs/apk/<buildType>/` as `ReSpotifuck-<version>-<buildType>.apk`.

## Contributing

Report bugs and suggest changes through [GitHub Issues](https://github.com/ReSpotF-ck/-ReSpotif-ck-App/issues). Pull requests are welcome.

## Links

- [Source code](https://github.com/ReSpotF-ck/-ReSpotif-ck-App)
- [Releases](https://github.com/ReSpotF-ck/-ReSpotif-ck-App/releases)
- [Issues](https://github.com/ReSpotF-ck/-ReSpotif-ck-App/issues)
- [Download APKs (release-assets)](https://github.com/ReSpotF-ck/-ReSpotif-ck-App/tree/main/release-assets)
- [Old versions and archived files](https://github.com/ReSpotF-ck/-ReSpotif-ck-App/tree/main/archive)
- Official social media accounts: none are configured yet.

## Current maintainer

N3K0 (MarshMeadow) is the current maintainer of the ReSpotF*ck projects.

- [N3K0 on GitHub](https://github.com/MarshMeadow)
- [N3K0's website](https://marshmeadow.netlify.app)

## Credits

- Spotilol, by lyssadev and maintained by its contributors
- Bouncy Castle, used for local certificate creation; its license notice is retained in the APK
- Spotifuck, by Deviato, whose reverse-engineering work informed the upstream project

This fork uses its own Android application ID (`com.respotfck.spotifck`, previously `com.project.lol` in Spotilol), so it installs as a separate app rather than updating Spotilol in place. After switching, log in again and install the new ReSpotifuck CA certificate. Internal preference names inherited from Spotilol (`spotilol_prefs`) are kept unchanged.
