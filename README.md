<p align="center">
  <img src="art/spotilolicon.png" alt="Spotilol Logo" width="128" height="128">
</p>

<h1 align="center">spotilol</h1>

<p align="center">
  a lil android app that wraps spotifys web player with builtin adblocking.
</p> its a fork of spotifuck by deviato, ported from smali to
clean kotlin. all free, all open.

it runs a local mitm proxy with a custom CA cert so spotify doesnt clock
you're on a webview. thats basically the magic trick. everything else passes
through untouched.

---

## what it does

- blocks audio ads 
- media notification with play/pause, skip, seek, like/unlike
- works with lock screen, bluetooth, wear os
- autoplay modes: off, once at start, or keep trying
- mobile-friendly CSS/JS layout tweaks (thanks to Spotifuck)
- amoled dark mode (pure black)
- keeps the screen on while you're vibing
- browse your library through spotifys api

---

## you'll need

- android 8.0+ (api 26)
- a spotify account (free or premium)
- google chrome / webview (comes with your phone)

---

## first time setup: the cert thing

spotilol makes a local CA cert so spotify doesnt know you're in a webview.
it lives on your device, stays on your device.

steps:

1. open spotilol -- you'll see the "certificate required" screen.
2. tap "export .pem" to save it to your downloads.
3. go to settings > security > encryption & credentials >
   install a certificate > ca certificate.
4. find `spotilol_ca.pem` in your downloads and tap it.
5. it'll warn you about network monitoring -- just tap "install anyway".
6. come back to spotilol and tap "check". if it worked, you're in.

> NOTE: if you ever clear your devices credential storage (like after
> a factory reset), you'll have to do this again.

---

## build it yourself

```
git clone https://github.com/lyssadev/spotilol
cd spotilol
./gradlew assembleDebug
```

apk lands at `app/build/outputs/apk/debug/app-debug.apk`.

---



---

## wanna help?

contributions are cool. open issues, throw prs, suggest stuff. free for all.

---

## credits

spotilol exists because deviato did the reverse-engineering work on spotifuck.
this ports the core logic from smali to kotlin with extra features and
maintenance.

open-sourced by lyssadev <3 deviato