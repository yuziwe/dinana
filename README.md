# Dinana

Dinana is an Android app for writing and publishing a [Hexo](https://hexo.io/) blog from your phone. It connects directly to a GitHub-hosted blog repository, so you can create posts, edit drafts, add images, and push changes without opening a laptop.

## What It Does

- Browse posts stored in `source/_posts/`
- Create, edit, and delete Markdown posts
- Preview Markdown before publishing
- Insert images from your device gallery
- Commit posts and images directly to GitHub
- Sort posts by publish date or filename
- Check for app updates from GitHub Releases
- Use a clean light or dark theme

## Requirements

- Android 8.0 or later
- A GitHub personal access token with `repo` access
- A Hexo blog repository on GitHub
- Posts stored in the standard Hexo path: `source/_posts/`

## Getting Started

1. Download and install the latest APK from GitHub Releases.
2. Open Dinana.
3. Go to Settings.
4. Enter your GitHub token, repository owner, repository name, and branch.
5. Return to the post list and start writing.

Dinana saves your GitHub token and repository settings in encrypted Android storage.

## How Publishing Works

Dinana writes Markdown files directly into your blog repository. New posts are created with Hexo front matter, and images are uploaded beside the post in `source/_posts/{slug}/`.

Simple post edits are committed as a single file update. Posts with images are committed together with their image files so the content and assets stay in sync.

## Updating

Dinana can check GitHub Releases for newer versions. When an APK release is available, the app can download it and open the Android installer.

## For Contributors

This is a Kotlin Android app built with Jetpack Compose and Material 3.

Useful commands:

```bash
./gradlew assembleDebug       # build a debug APK
./gradlew installDebug        # install on a connected device
./gradlew compileDebugKotlin  # quick compile check
./gradlew lint                # run Android lint
```

On Windows PowerShell, use `.\gradlew.bat`.

See [AGENTS.md](AGENTS.md) for repository structure, coding conventions, and contribution notes.

## License

[MIT](LICENSE)
