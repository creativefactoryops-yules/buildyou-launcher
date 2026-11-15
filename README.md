# --force ./BUILD YOU

A real, installable Android home-screen launcher. Not a mock: it declares
`CATEGORY_HOME`, appears in the system "Use as Home app" chooser, lists your actual
installed apps from `PackageManager`, launches them, and exposes a working terminal
bound to real actions.

**Brand**: `--force ./BUILD YOU` · dark-only · monospace stamp with a fixed 1dp border ·
live date row · auto-shrinking `BUILD YOU` wordmark (18–40sp, measured with
`TextMeasurer` so it never overflows).

The launcher icon is `app/src/main/res/drawable/ic_launcher_foreground.xml`.

## Stack

| Piece | Version |
| --- | --- |
| Kotlin | 2.0.21 |
| Jetpack Compose BOM | 2024.10.01 (Material 3 1.3.1) |
| Android Gradle Plugin | 8.13.2 |
| Gradle | 8.14.3 |
| compileSdk / targetSdk | 36 |
| minSdk | 31 (Android 12) |
| Java | 17 |

Single activity (`MainActivity`), edge-to-edge, Compose-only UI (no appcompat).

## What it does

- **Home replacement.** The manifest declares `<activity android:name=".MainActivity"
  android:exported="true" android:launchMode="singleTask">` with
  `MAIN + HOME + DEFAULT`. Android therefore offers it in the **Use as Home app**
  chooser, and pressing Home returns to the deck instead of stacking activities
  (`singleTask` + `clearTaskOnLaunch`, plus `onNewIntent` re-scan).
- **Real app inventory.** `PackageManager.queryIntentActivities(ACTION_MAIN,
  CATEGORY_LAUNCHER)`, excluding the launcher's own package, sorted by label,
  icons converted to `ImageBitmap` for Compose. Package visibility is declared with a
  `<queries>` block for the LAUNCHER intent — **no `QUERY_ALL_PACKAGES`**.
- **Tap to launch.** `getLaunchIntentForPackage(pkg)` + `startActivity` with
  `FLAG_ACTIVITY_NEW_TASK`; a null launch intent falls back to the App Info page.
- **Long-press sheet.** App info (`ACTION_APPLICATION_DETAILS_SETTINGS`),
  Uninstall (`ACTION_UNINSTALL_PACKAGE`), Hide (persisted set in
  `SharedPreferences`; unhide via the terminal's `unhide <pkg>`).
- **Terminal pane.** `help`, `apps`, `open <name>`, `wall <color>` (solid colours via
  `WallpaperManager`; image paths honestly report *not implemented in v1*), `brand`,
  `icon`, `--force` (reload inventory), plus `hidden` / `unhide` / `date` / `clear`.
  Unsupported things print an honest "not implemented", never a fake success.
- **Icon ladder.** The stamp text degrades by available width — `[ --force ./BUILD YOU ]`
  → `[ -f ]` → `[ · ]` → nothing — and the chosen rung is written to logcat
  (`stamp ladder → …`).

### Explicitly out of scope for v1 (named, not silently stubbed)

Home-screen widgets (`AppWidgetHost`), notification badges, the snake game, the
creature mascot. Each is a `TODO(v2)` comment in the source at the relevant spot.

## Build

```bash
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk   (debug-signed)
```

Debug builds sign with AGP's auto-generated debug keystore — no signing config needed.
A prebuilt copy of the same artifact is committed at **`dist/app-debug.apk`** (the
`app/build/` directory is gitignored, so use `dist/` when cloning).

## Clone & build from GitHub

```bash
git clone https://github.com/creativefactoryops-yules/buildyou-launcher.git
cd buildyou-launcher
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk   (debug-signed)
```

Everything needed to build is in the repo: the Gradle wrapper (`gradlew` + `gradle/wrapper/*`)
and the version pins (`gradle/libs.versions.toml`). You only need a JDK 17
and an Android SDK (platform 36 + build-tools 36.0.0) — point `local.properties`'s
`sdk.dir` at your SDK, or set `ANDROID_HOME`. `local.properties` is gitignored and
machine-specific on purpose. The CI workflow definition ships as
`docs/ci-workflow.yml`; after the one-time activation step (see **CI** below) it
builds the APK on `ubuntu-latest` on every push and via *workflow_dispatch* — grab
the `app-debug-apk` artifact from a run page if you don't want to build locally.

### Building on the phone (Termux)

```bash
pkg install -y git openjdk-17
git clone https://github.com/creativefactoryops-yules/buildyou-launcher.git
cd buildyou-launcher
./gradlew assembleDebug
```

Notes for on-device builds: Gradle needs ~2 GB of free memory, so close other apps.
The Android SDK's `aapt2` build tool ships an x86_64 binary — on an ARM phone a native
build may fail in resource linking; if so, use **proot-distro** (a Debian/Ubuntu chroot
with an `aarch64` toolchain) inside Termux, or just download the APK from the CI
artifacts / `dist/app-debug.apk` and sideload it (below).

## Sideload walkthrough

1. **Transfer the APK** — `dist/app-debug.apk` from a fresh clone, or
   `app/build/outputs/apk/debug/app-debug.apk` right after building — to the phone
   (USB copy, cloud drive, or direct download).
2. **Allow installs from the source you used.** Open the APK from your file manager
   or browser; Android will prompt *"Install unknown apps"* — allow it **for that one
   app** (e.g. Files or Chrome). Settings path: *Settings → Apps → Special app access
   → Install unknown apps → [your file manager] → Allow from this source*.
3. **Install.** Confirm the install prompt. Because the app declares
   `CATEGORY_HOME`, Android may warn that it is a home app — expected.
4. **Make it the Home app.** Either take the chooser that pops up the next time you
   press **Home**, or set it manually: *Settings → Apps → Default apps → Home app →
   --force ./BUILD YOU*.
5. Press Home. The deck appears with the live date, the wordmark, and your apps.
   Long-press an icon for App info / Uninstall / Hide; type `help` in the terminal.

> Note on the app icon on the deck: the launcher deliberately **excludes its own
> package** from the inventory, so you will not find itself among the apps.

## CI

`.github/workflows/build.yml` builds the debug APK on every push/PR with
`actions/checkout@v4` + `actions/setup-java@v4` (temurin 17) + the Android SDK
preinstalled on GitHub's `ubuntu-latest` runners, and uploads `app-debug.apk` via
`actions/upload-artifact@v4`. It uses **no repository secrets**.

> **One-time activation.** This repo was first published by an automation whose
> GitHub credential is not allowed to create `.github/workflows/*` files (GitHub
> requires a `workflow`-scoped token for that). The validated workflow is therefore
> also parked, byte-identical, at **`docs/ci-workflow.yml`**. To turn CI on, create
> `.github/workflows/build.yml` in the GitHub web UI (which is not scope-gated):
> *Add file → Create new file*, name it `.github/workflows/build.yml`, paste the
> content of `docs/ci-workflow.yml`, commit. CI then builds the APK on every push
> and on demand (*Actions → build-apk → Run workflow*).

## Verification honesty

The APK produced here is verified at **compile level plus manifest inspection**
(`aapt2 dump badging`, `aapt2 dump xmltree` for the HOME category, `apksigner verify`
for the debug signature). No emulator or device was available, so behaviour on a
handset is **not** claimed as tested.
