<p align="center">
  <img src=".github/icon.png" width="120" height="120" style="border-radius: 24px;" />
</p>

<h1 align="center">OpenEljur Android</h1>

<p align="center">
  Jetpack Compose Android client for <code>OpenEljur</code>
</p>

## Features

- Android 8.0+ (API 26+) support
- Material You (dynamic colors on Android 12+)
- Languages: `ru`, `en`, `uk`, `ro`
- Screens: Diary, Marks, Messages, Settings
- Diary with week navigation, homework and file downloads
- Marks with quarter tabs, mark chips and comment dialog
- Messages with inbox/sent folders, pagination and file download to Downloads
- Message compose with recipient picker and file attachments
- Theme: `light` / `dark` / `system`
- Language switching with instant app restart
- Offline cache — shows data without network

## Installation

### Direct APK

The easiest way — no account required.

1. Download the latest `.apk` from [Releases](../../releases)
2. Open the file on your device
3. Allow installation from unknown sources if prompted
4. Tap **Install** — done

### Build from source

1. Clone the repo
2. Edit `local.properties` and add:
   ```
   API_BASE_URL=https://your-backend-url
   DEFAULT_SCHOOL_ID=your-school-id
   ```
3. Open in Android Studio and run, or build via CLI:
   ```bash
   ./gradlew assembleDebug
   ```

Minimum Android Studio version: **Hedgehog (2023.1.1)**

> `local.properties` is not committed to git.

## CI / Release

GitHub Actions builds an APK and publishes it to Releases on every `v*` tag push:

```bash
git tag v1.0.0
git push --tags
```

Or trigger manually: Actions → Build & Release APK → Run workflow.

Required secrets are described in [`.github/SECRETS.md`](.github/SECRETS.md).

## License

[MIT](LICENSE)
