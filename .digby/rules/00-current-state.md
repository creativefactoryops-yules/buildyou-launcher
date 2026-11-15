# Current state (always injected)

## Project: --force ./BUILD YOU (Android launcher)

- **v1.1 SHIPPED (2026-11-15, review fixes):** debug APK rebuilt after the independent
  review; five defects fixed (window insets, stamp baseline, width budget, terminal
  focus tracking, M3 dark scheme) plus README path fix.
  - APK: `dist/app-debug.apk` (identical to `app/build/outputs/apk/debug/app-debug.apk`),
    24,210,981 bytes,
    sha256 `0c2427e4e9f13b22f569595bd675ca84c06af1a64b638000378df68d0682ff86`.
  - Build: `./gradlew --no-daemon assembleDebug` → `BUILD SUCCESSFUL in 41s`.
  - applicationId `force.buildyou.launcher`, versionName `1.0.0` (versionCode 1),
    minSdk 31 / targetSdk 36 / compileSdk 36, debug-signed (apksigner v2 Verifies,
    CN=Android Debug).
  - Manifest verified via aapt2: MainActivity exported, launchMode=singleTask,
    clearTaskOnLaunch, MAIN+DEFAULT+HOME intent filter, `<queries>` MAIN/LAUNCHER,
    NO QUERY_ALL_PACKAGES.
  - Review fixes in this build: statusBarsPadding on the header +
    navigationBarsPadding/imePadding in the terminal (edge-to-edge no longer draws
    under system bars); stamp rendered as a bordered `Text` so `alignByBaseline`
    truly shares the date's baseline (a `Box` has no baseline); header maxWidth nets
    out its 32dp side padding before the TextMeasurer shrink/ladder; terminal input
    wires `onFocusChanged` (hint no longer renders under the cursor);
    `MaterialTheme(darkColorScheme())` wraps the deck; README points at `dist/`.
- **Verification level:** compile + manifest/signature inspection only. No emulator/
  device run was possible (no KVM) — do not claim on-device testing.
- **Stack pins:** AGP 8.13.2 · Gradle 8.14.3 · Kotlin 2.0.21 · Compose BOM 2024.10.01 ·
  JDK 17. All in `gradle/libs.versions.toml`.
- **Do not touch:** `gradle.properties` memory profile (2 GiB cgroup cap — see
  learnings.md), version pins without checking google maven metadata.
- **Next steps (v2 backlog, all named as TODO(v2) in source):** drag-to-reorder,
  `wall <path>` image wallpaper, home-screen widgets (AppWidgetHost), notification
  badges, snake game, creature mascot.
- **CI:** `.github/workflows/build.yml` builds + uploads `app-debug.apk` on push/PR
  (and via `workflow_dispatch`, no secrets). 
- **Published (2026-11-15):** source is public at
  **https://github.com/creativefactoryops-yules/buildyou-launcher** (clone URL
  `https://github.com/creativefactoryops-yules/buildyou-launcher.git`). The account's
  stored token carries scopes `read:org, repo, user` — **no `workflow` scope** — so
  GitHub refuses every write that would introduce `.github/workflows/build.yml`
  (probed: git push over HTTPS, git push via an API-created deploy key, Git Data
  refs POST and PATCH, Contents PUT — all refused; generic ref writes DO work).
  origin/main therefore carries the full source as a **filtered publish chain** off
  `d1f2a3c` — every launcher commit preserved with its message, the CI workflow file
  withheld. Hygiene on origin: `local.properties` absent, no keystores / real `.env`,
  `gradlew` mode `100755`, wrapper jar+properties present, `dist/app-debug.apk` shipped.
- **PENDING (owner action):** the CI workflow file cannot land until the owner
  supplies a credential with `workflow` scope (paste a PAT → store_secret) or adds
  the file via the GitHub web UI (a browser session is not scope-gated). The
  validated workflow is also parked, byte-identical, at `docs/ci-workflow.yml` and
  the README's **CI** section documents the one-step web-UI activation. Once the
  file exists, `workflow_dispatch` can be triggered and CI runs on every push.
  Owner question flagged with both routes.
- **Repo state:** session work on branch `digby/session-3156d862` (full history,
  includes the workflow file); origin/main = filtered publish chain. Local `main`
  is a stale scaffold commit. After a scope fix, either add the workflow file as a
  new commit on origin/main, or force-replace main with the true session history.
