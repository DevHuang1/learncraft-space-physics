# LearnCraft Space Physics — Native Android

This module is the native Android direction for the orbital physics prototype. The project now has a real `:shared` Kotlin Multiplatform module with `commonMain` physics and a `:benchmark` Kotlin/JVM module.

## Architecture

The app uses **Kotlin**, **Jetpack Compose**, and a custom Compose `Canvas` viewport. `PhysicsEngine.kt` is renderer-independent and owns fixed-step integration, gravity wells, per-object mass/radius/elasticity, event emission, and a uniform spatial-hash collision broad phase. `FixedStepRunner` bounds substeps. `SpatialAudioController.kt` maps collision and gravity-well events to pooled Android sounds with stereo pan, volume, and collision-gap limiting. `MainActivity.kt` hosts the Compose surface, drag selection, animation loop, and per-object controls.

The Compose Canvas accepts a tap to create a gravity well and a drag gesture to select and move a body. The benchmark can be run with `./gradlew :benchmark:run` and prints CSV results for 1,000, 2,000, and 5,000 particles. The next native milestones are to connect `SpatialAudioController` to bundled `res/raw` sound assets, add particle pooling, persistent settings, and an OpenGL renderer option for scenes beyond the Canvas budget. The cross-platform sharing plan is documented in `../KMP_ARCHITECTURE.md`.

## Build prerequisites

Use Android Studio with JDK 17, Android SDK Platform 35, Build Tools 35.x, and Gradle 8.9 or newer. From this directory:

```bash
gradle :app:assembleRelease
```

The current sandbox does not have the Android SDK or Gradle command installed, so APK compilation must be completed on an Android development machine or after installing those toolchains.

## Download an APK for a Samsung phone

The repository includes a GitHub Actions workflow at `.github/workflows/android-apk.yml`. A manual run from **Actions → Android APK → Run workflow** builds a signed release APK and publishes it as both a workflow artifact and a public GitHub release asset.

Before the first manual run, configure these repository secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. The base64 secret must contain the complete release keystore file encoded without line wrapping. Keep the keystore and passwords private; never commit them to the repository.

After the workflow completes, download `app-release.apk` from the release page or the `learncraft-space-physics-release-apk` artifact. Open it with My Files on the Samsung phone and allow installation from that source if Android asks. The installed control panel displays the app version and version code.

| Goal | Command or action |
|---|---|
| Build locally | `gradle :app:assembleRelease` from `android/` |
| APK output | `android/app/build/outputs/apk/release/app-release.apk` |
| Run shared tests | `gradle :shared:test` |
| Run the benchmark | `gradle :benchmark:run` |
| Build from GitHub | Actions → Android APK → Run workflow |

For a local signed build, export `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` before running `gradle :app:assembleRelease`. The sandbox used to edit this repository does not contain the Android SDK or Gradle executable, so the GitHub Actions workflow is the supported no-local-toolchain route for obtaining the signed APK.
