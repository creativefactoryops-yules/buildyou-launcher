# Current state (always injected)

## Project: --force ./BUILD YOU (Android launcher)

- **v1 SHIPPED (2026-11-15):** debug APK built and verified in-container.
  - APK: `dist/app-debug.apk` (identical to `app/build/outputs/apk/debug/app-debug.apk`),
    24,194,597 bytes, sha256 `131b9a7e6ba3018405773e27a676f8234764b2586a2aaa864e67d2b49ef7324a`.
  - Build: `./gradlew --no-daemon assembleDebug` → `BUILD SUCCESSFUL in 1m 9s`.
  - applicationId `force.buildyou.launcher`, versionName `1.0.0` (versionCode 1),
    minSdk 31 / targetSdk 36 / compileSdk 36, debug-signed (apksigner v2 Verifies,
    CN=Android Debug).
  - Manifest verified via aapt2: MainActivity exported, launchMode=singleTask,
    clearTaskOnLaunch, MAIN+DEFAULT+HOME intent filter, `<queries>` MAIN/LAUNCHER,
    NO QUERY_ALL_PACKAGES.
- **Verification level:** compile + manifest/signature inspection only. No emulator/
  device run was possible (no KVM) — do not claim on-device testing.
- **Stack pins:** AGP 8.13.2 · Gradle 8.14.3 · Kotlin 2.0.21 · Compose BOM 2024.10.01 ·
  JDK 17. All in `gradle/libs.versions.toml`.
- **Do not touch:** `gradle.properties` memory profile (2 GiB cgroup cap — see
  learnings.md), version pins without checking google maven metadata.
- **Next steps (v2 backlog, all named as TODO(v2) in source):** drag-to-reorder,
  `wall <path>` image wallpaper, home-screen widgets (AppWidgetHost), notification
  badges, snake game, creature mascot.
- **CI:** `.github/workflows/build.yml` builds + uploads `app-debug.apk` on push/PR.
  No git remote is configured on this repo, so it has not run here.
- **Repo state:** all work on branch `digby/session-3156d862`; no remote to push to.
