# Public release checklist

## Prepared in this change

- [x] App name, launcher artwork, About screen, and repository links use the Spotif*ck branding.
- [x] In-app update checks query this fork's GitHub Releases API.
- [x] Existing Android application ID and local preference names are retained for upgrade compatibility.
- [x] README points to this repository and its issue, release, and build pages.

## Before publishing an APK

- [ ] Resolve the project's distribution license. This checkout has no LICENSE file; verify the upstream license and permission for every inherited component before publishing source or binaries.
- [ ] Configure and retain the Android signing keystore secrets in GitHub Actions. Keep the same signing key for future updates.
- [ ] Create a tagged GitHub Release and attach the signed APK.
- [ ] Install and exercise the release APK on supported Android versions, including certificate setup, playback, notifications, update checks, and uninstall.
- [ ] Add official social links here and in the app only after those accounts exist. No social profiles are configured in this checkout.
