# Dinana

An Android app for managing a [Hexo](https://hexo.io/) blog hosted on GitHub. Write, edit, and publish markdown posts directly from your phone.

## Features

- **Post management** — list, create, edit, and delete Hexo blog posts
- **Markdown editor** — write in markdown with a live preview powered by Markwon
- **Image upload** — insert images from your gallery; they're uploaded alongside the post
- **GitHub integration** — uses the GitHub Contents API and Git Data API to commit directly to your blog repo
- **Pull-to-refresh** — swipe to reload the post list
- **Sorting** — sort posts by name or publish date (parsed from front matter)
- **Auto-update** — checks for new versions on startup; downloads and installs APK with one tap
- **Theme** — GitHub-inspired light and dark mode, adaptive status bar

## Requirements

- Android 8.0 (API 26) or later
- A GitHub Personal Access Token with `repo` scope
- A Hexo blog repository on GitHub with posts in `source/_posts/`

## Setup

1. Install the APK on your device
2. Open the app and tap the Settings icon
3. Enter your GitHub Personal Access Token, repository owner, repository name, and branch
4. Go back — your posts will load automatically

## Build

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device
./gradlew installDebug

# Build signed release APK (requires keystore.properties)
./gradlew assembleRelease

# Check for compilation errors
./gradlew compileDebugKotlin
```

### Release signing

For local release builds, create `keystore.properties` at the project root:

```properties
storeFile=release.jks
storePassword=your-keystore-password
keyAlias=dinana
keyPassword=your-key-password
```

Generate a keystore if you don't have one:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias dinana
```

## Release

Pushing a tag matching `v*` (e.g. `v1.1.0`) triggers CI to build and sign a release APK and create a GitHub Release with the APK attached. The app version is automatically derived from the tag at build time.

```bash
git tag v1.1.0
git push origin v1.1.0
```

Manual workflow dispatch is also available for testing (builds APK without creating a release).

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Jetpack Navigation Compose |
| HTTP | OkHttp (no Retrofit) |
| Serialization | kotlinx.serialization |
| Auth storage | EncryptedSharedPreferences (AES-256) |
| Markdown rendering | Markwon + Glide |
| Architecture | Manual DI via `AppContainer` + `ViewModelFactory` |

## Architecture

The app follows a simple MVVM pattern with manual dependency injection:

```
AppContainer (Application)
  ├── TokenManager         — Encrypted storage for GitHub credentials
  ├── GitHubApi            — OkHttp client for GitHub REST API
  └── BlogRepository       — Maps API responses to BlogPost domain model

ViewModels (StateFlow<UiState>)
  ├── PostListViewModel    — Post listing, deletion, sort
  ├── EditorViewModel      — Post editing, saving, image upload
  └── SettingsViewModel    — GitHub configuration

Compose Screens
  ├── PostListScreen       — Post list with pull-to-refresh
  ├── EditorScreen         — Markdown editor with preview toggle
  └── SettingsScreen       — GitHub credentials form, update check

Utilities
  └── UpdateUtils          — APK download and package installer
```

Blog posts live in `source/_posts/` in your GitHub repo. The app uses the GitHub Contents API for single-file operations and the Git Data API for batch commits (save with images, delete with assets).

## License

[MIT](LICENSE)
