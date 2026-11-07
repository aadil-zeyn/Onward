# Onward

A calm, beautiful place for your tasks and habits, built with Flutter.

<p align="center">
  <img src="docs/screenshot-tasks-light.png" alt="Onward tasks, light theme" width="32%">
  <img src="docs/screenshot-tasks-dark.png" alt="Onward tasks, dark theme" width="32%">
  <img src="docs/screenshot-edit.png" alt="Editing a task" width="32%">
</p>

## Install on Android with Obtainium

Every push to `main` builds a signed APK and publishes it on the [Releases page](https://github.com/aadil-zeyn/Onward/releases).

1. Install [Obtainium](https://github.com/ImranR98/Obtainium).
2. Tap **Add App** and paste `https://github.com/aadil-zeyn/Onward`.
3. Tap **Add**, then **Install**. Obtainium will notify you when a new build is released.

You can also download the `.apk` straight from the latest release.

## Features

**Tasks**
- Quick capture with a priority (None, Low, Medium, High) and a due date: Today, Tomorrow, Next week, or any date you pick.
- A checkbox that animates, a progress ring for the day, and a burst of confetti when everything's done.
- Tap a task to edit it. Swipe right to complete it, or swipe left to delete it (with undo).
- Hold the dotted handle and drag to reorder.
- All, Active and Done filters with live counts, plus "Clear done" with undo.
- Due dates read as *Today*, *Tomorrow* or *Thursday*, and overdue tasks are highlighted.

**Habits**
- Track daily habits with a seven-day history and streaks.

**Everywhere**
- Warm light and dark themes. The app follows your system by default, and the sun/moon button switches.
- Everything is stored on your device.

## Develop

```sh
flutter pub get
flutter run           # on a device or emulator
flutter test
flutter build apk     # build/app/outputs/flutter-apk/app-release.apk
```

## Release signing

Android only installs an update when it is signed with the same key as the installed app. Out of the box, builds are signed with `android/app/onward-public.jks`. That key is committed to this public repo, so every CI build has a stable signature and updates install smoothly. Because anyone can read it, it doesn't prove who built an APK. Only install Onward from this repo's releases.

To sign with a private key instead, add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 your-key.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | the keystore password |
| `ANDROID_KEY_ALIAS` | the key alias |
| `ANDROID_KEY_PASSWORD` | the key password (if different) |

Switching keys changes the signature, so you'll need to uninstall and reinstall the app once.

## Fonts

[DM Sans](https://fonts.google.com/specimen/DM+Sans) and [Fraunces](https://fonts.google.com/specimen/Fraunces) are bundled under the SIL Open Font License (`assets/fonts/`).
