# Learnings — durable project conventions

- **Toolchain lives under `$HOME/.local`** (survives the /tmp wipe, dies with the
  container): JDK 17 `~/.local/jdk17`, Android SDK `~/.local/android-sdk`
  (platform-36, build-tools 36.0.0), Gradle 8.14.3 `~/.local/gradle-dist`.
  `$DIGBY_ENV` already exports JAVA_HOME/ANDROID_HOME/PATH. After a container
  replacement, re-provision with the commands in `README.md` (Build section).
- **Container memory cap is 2 GiB (cgroup memory.max)**, not the 30 GB `free -h`
  suggests. Gradle default `-Xmx4096m` gets the daemon OOM-killed mid-dex
  (`mergeExtDexDebug`, exit = "daemon disappeared"). Working profile:
  `org.gradle.jvmargs=-Xmx1400m -XX:MaxMetaspaceSize=384m`,
  `org.gradle.parallel=false`, `org.gradle.workers.max=2`,
  `kotlin.compiler.execution.strategy=in-process` (avoids a second compiler JVM).
  With this profile `assembleDebug` completes in ~70 s.
- **Android XML resources reject `--` inside comments** (SAXParseException in
  `mergeDebugResources`). The brand string itself (`--force`) is fine in values and
  paths, but never put it in an XML comment.
- **Versions are pinned in `gradle/libs.versions.toml`**: AGP 8.13.2 ↔ Gradle 8.14.3
  ↔ Kotlin 2.0.21 ↔ Compose BOM 2024.10.01 ↔ compileSdk 36. Verify a bump against
  google maven metadata (`dl.google.com/dl/android/maven2/.../maven-metadata.xml`)
  before changing it — newer AGP 9.x requires built-in Kotlin and different plugins.
- **Gradle wrapper was generated once via the local dist** (`gradle wrapper --gradle-version 8.14.3`);
  `./gradlew` then works standalone. Use `--no-daemon` always in this container.
- `ACTION_UNINSTALL_PACKAGE` is deprecated (warning only) — kept deliberately for the
  long-press Uninstall action; `ACTION_DELETE` is the modern spelling if we ever care.
