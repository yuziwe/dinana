# Repository Guidelines

## Project Structure & Module Organization

Dinana is a single-module Android app. The Gradle root contains `settings.gradle.kts`, shared plugin versions in `build.gradle.kts`, and the Android app in `app/`. Kotlin source lives under `app/src/main/java/com/dinana/blog/`:

- `data/api`, `data/repository`, and `data/local` handle GitHub API access, blog operations, and encrypted settings.
- `viewmodel` contains screen state and business logic.
- `ui/screens`, `ui/components`, and `ui/theme` contain Jetpack Compose UI.
- `navigation` defines routes, and `util` contains markdown and update helpers.

Android resources are in `app/src/main/res/`. There are currently no `test` or `androidTest` source sets.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root:

```bash
./gradlew assembleDebug       # build a debug APK
./gradlew installDebug        # install debug build on a connected device
./gradlew compileDebugKotlin  # fast Kotlin compile check
./gradlew lint                # run Android lint
./gradlew assembleRelease     # build signed release APK when signing config exists
```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`.

## Coding Style & Naming Conventions

Write Kotlin using 4-space indentation and idiomatic Compose patterns. Keep package names under `com.dinana.blog`. Name composables with `PascalCase` and a clear UI suffix, for example `PostListScreen` or `MarkdownPreview`. Name ViewModels as `{Feature}ViewModel`, state holders as `{Feature}UiState`, and repository/API classes by responsibility. Prefer existing manual dependency injection through `DinanaApp`, `AppContainer`, and the `ViewModelFactory`; do not introduce Hilt, Retrofit, or a new architecture without a strong reason.

## Testing Guidelines

No automated tests are configured yet. For now, validate changes with `./gradlew compileDebugKotlin` and `./gradlew lint`, then manually exercise affected flows on a device or emulator. When adding tests, place JVM tests in `app/src/test/` and instrumentation or Compose UI tests in `app/src/androidTest/`, using names like `MarkdownUtilsTest` or `PostListScreenTest`.

## Commit & Pull Request Guidelines

Recent commits use short imperative summaries such as `Preserve case in post title slug` and `Update README with auto-update feature and theme docs`. Follow that style: one concise subject line, capitalized, describing the behavior changed.

Pull requests should include a brief summary, validation steps or command output, linked issues when applicable, and screenshots or screen recordings for UI changes. Note any manual GitHub token, repository, branch, signing, or update-check configuration needed to reproduce the change.

## Security & Configuration Tips

Do not commit real GitHub personal access tokens, local SDK paths, or production signing secrets. `keystore.properties`, `local.properties`, `release.jks`, and generated APKs should remain local unless explicitly required for release automation.
