# Public release checklist

## Prepared in this change

- [x] App name, launcher artwork, About screen, and repository links use the ReSpotifuck branding, and the app opens with an animated launch splash.
- [x] In-app update checks query this fork's GitHub Releases API and fall back to signed APKs in `release-assets/` when no GitHub Release is published; Settings offers the discovered APK link.
- [x] App activities use Android `FLAG_SECURE` to block system screenshots and screen recording.
- [x] First launch displays a service, copyright, and local proxy certificate disclosure.
- [x] Release APK includes the Bouncy Castle license notice; duplicate copies are merged only after confirming identical contents.
- [x] Replaced deprecated AndroidX Security Crypto storage with Android Keystore backed AES-GCM encryption.
- [x] Updated the stable Android, Kotlin, Compose, WebKit, Media, and Bouncy Castle build dependencies.
- [x] Android application ID and Kotlin package use `com.respotfck.spotifck`.
- [x] README points to this repository and its issue, release, and build pages.
- [x] About screen and README link to current maintainer N3K0 (MarshMeadow).

## Before publishing an APK

- [ ] Add the license terms supplied by the project owner. Distribution rights were confirmed, but the terms have not yet been provided.
- [x] A new Android release signing key has been generated for the first public package ID. Back up its private key and configure it as GitHub Actions secrets before CI releases.
- [ ] Publish a tagged GitHub Release with the signed APK after adding the license terms.
- [ ] Exercise the release APK on supported Android versions, including certificate setup, playback, notifications, update checks, and uninstall.
- [ ] Add official social links here and in the app only after those accounts exist. No social profiles are configured in this checkout.
