# Dinana

An Android app for managing a [Hexo](https://hexo.io/) blog hosted on GitHub. Write, edit, and publish markdown posts directly from your phone.

## Features

- **Post management** — list, create, edit, and delete Hexo blog posts
- **Markdown editor** — write in markdown with a live preview powered by Markwon
- **Image upload** — insert images from your gallery; they're uploaded alongside the post
- **GitHub integration** — uses the GitHub Contents API and Git Data API to commit directly to your blog repo
- **Pull-to-refresh** — swipe to reload the post list
- **Sorting** — sort posts by name or publish date (parsed from front matter)

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

# Check for compilation errors
./gradlew compileDebugKotlin
```

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
  └── SettingsScreen       — GitHub credentials form
```

Blog posts live in `source/_posts/` in your GitHub repo. The app uses the GitHub Contents API for single-file operations and the Git Data API for batch commits (save with images, delete with assets).

## License

[MIT](LICENSE)
