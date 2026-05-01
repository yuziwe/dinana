# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install debug
./gradlew installDebug

# Clean build
./gradlew clean assembleDebug

# Run lint checks
./gradlew lint

# Check for build issues without full APK build
./gradlew compileDebugKotlin
```

No test framework is configured. There are no test files.

## Architecture Overview

**Dinana** is an Android app that manages a Hexo blog hosted on GitHub. Blog posts live in `source/_posts/` in a GitHub repo, and the app uses the GitHub Contents API to list, create, edit, and delete markdown posts.

### Stack

- **Language**: Kotlin, no coroutines Flow—uses `StateFlow` with `collectAsStateWithLifecycle`
- **UI**: Jetpack Compose + Material3, GitHub Primer-inspired color scheme (light/dark)
- **Navigation**: Jetpack Navigation Compose (3 routes: PostList, Editor, Settings)
- **DI**: Manual via `AppContainer` in the `Application` class + a `ViewModelFactory` with a `when` block
- **API layer**: OkHttp directly (no Retrofit), kotlinx.serialization for JSON
- **Auth storage**: EncryptedSharedPreferences (AES-256)
- **Markdown rendering**: Markwon library with Glide image plugin

### Package Structure (`com.dinana.blog`)

```
data/api/          — GitHubApi (OkHttp), GitHubModels (serializable DTOs)
data/repository/   — BlogRepository (maps API to BlogPost domain model)
data/local/        — TokenManager (encrypted prefs for GitHub token + repo config)
viewmodel/         — PostListViewModel, EditorViewModel, SettingsViewModel
ui/screens/        — PostListScreen, EditorScreen, SettingsScreen
ui/components/     — MarkdownPreview (wraps Markwon)
ui/theme/          — Color, Theme, Typography (monochrome palette)
util/              — MarkdownUtils (slugify, front matter, image URL resolution), UpdateUtils (APK download + install)
navigation/        — Routes object (route constants + helper)
```

### Data Flow

1. `TokenManager` stores GitHub PAT, owner, repo, branch in encrypted prefs
2. `BlogRepository` wraps `GitHubApi` with blog-specific logic (posts dir, front matter, base64 encoding)
3. ViewModels hold `MutableStateFlow<UiState>` and are the sole source of UI state
4. Compose screens observe via `collectAsStateWithLifecycle()` and pass lambdas for events
5. `AppContainer` creates `GitHubApi` with the current token — repo is recreated fresh on each call from `provideRepository()`

### Save Strategy (Two-Tier)

- **Simple save** (no images): `BlogRepository.pushPost()` → Contents API `PUT` (single commit)
- **Save with images**: `EditorViewModel` stages images in-memory; on save, calls `BlogRepository.pushPostWithImages()` → Git Data API (create blobs → create tree → create commit → update ref) so the post + all images land in a single commit
- Image picker uses `ActivityResultContracts.GetContent()` — the byte array is read on `Dispatchers.IO`

### Delete Strategy

- `PostListViewModel.deletePost()` calls `BlogRepository.deletePostWithAssets()` which uses the Git Data API to traverse the repo tree, find the post `.md` + all files in `source/_posts/{slug}/`, and create a single commit that removes them all
- Falls back to Contents API `DELETE` when no asset directory exists

### UI Patterns

- Pull-to-refresh via `pullRefresh` modifier (Material, not Material3)
- Toast-based transient messages surfaced via `LaunchedEffect` observing `message` in UI state
- Sort cycling: `NAME` → `DATE_DESC` → `DATE_ASC` (date values parsed from YAML front matter via `BlogRepository.enrichWithDates()`)
- Default sort order is `DATE_DESC`
- Every network error is shown inline; retry buttons appear on failure
- `EditorScreen` uses Edit/Preview toggle with Markwon-based `MarkdownPreview` component
- `AlertDialog` with download progress shown from NavHost level for startup update check
- Status bar color syncs with theme via `SideEffect` in `DinanaTheme`

### Key Design Decisions

- No Retrofit — raw OkHttp calls wrapped with `withContext(Dispatchers.IO)` (30s connect/read/write timeouts)
- No Hilt/Dagger — manual `ViewModelFactory` in `MainActivity.kt` with a `when` block
- GitHub Contents API (not GraphQL) — operates on `source/_posts/` directory
- Image uploads go into `source/_posts/{slug}/` alongside the markdown file
- Blog posts are Hexo-style markdown with YAML front matter
- `MarkdownUtils.resolveImageUrls()` rewrites relative image refs to raw.githubusercontent.com URLs
- Posts are identified by filename — the slug (URL-safe title) becomes `{slug}.md`
- App version derived from Git tag at build time (`git tag --points-at HEAD`), fallback to `1.0.0`
- Update check calls GitHub Releases API for hardcoded repo (`strings.xml`), compares via semver
- APK download uses OkHttp with file verification; install uses `ACTION_INSTALL_PACKAGE` + FileProvider

### CRUD Operations (Contents API)

- **List**: `GET /repos/{owner}/{repo}/contents/source/_posts/` → filter `.md` files
- **Read**: `GET /repos/{owner}/{repo}/contents/source/_posts/{file}` → decode base64 content
- **Create/Update**: `PUT /repos/{owner}/{repo}/contents/source/_posts/{file}` with base64-encoded body
- **Delete**: `DELETE /repos/{owner}/{repo}/contents/source/_posts/{file}` with SHA
- **Image upload**: Same `PUT` endpoint, writing binary file to `source/_posts/{slug}/{image}`

### Git Data API (Batch Operations)

Used for multi-file commits. Methods on `GitHubApi`:

- `getRef()` / `getCommit()` / `getTree()` — read current branch state
- `createBlob()` — upload binary files (images) to the object store
- `createTree()` — build a new tree referencing existing blobs/content
- `createCommit()` / `updateRef()` — commit the tree and advance the branch
- `deletePostWithAssets()` in `BlogRepository` uses this API to delete the post `.md` and its asset directory files in a single commit (tree items with `sha = null` act as deletions)
