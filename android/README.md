# LearnCraft Space Physics — Native Android

This module is the native Android direction for the orbital physics prototype. The project now has a real `:shared` Kotlin Multiplatform module with `commonMain` physics and a `:benchmark` Kotlin/JVM module.

## Architecture

The app uses **Kotlin**, **Jetpack Compose**, and a custom Compose `Canvas` viewport. `PhysicsEngine.kt` is renderer-independent and owns fixed-step integration, gravity wells, per-object mass/radius/elasticity, event emission, and a uniform spatial-hash collision broad phase. `FixedStepRunner` bounds substeps. `SpatialAudioController.kt` maps collision and gravity-well events to pooled Android sounds with stereo pan, volume, and collision-gap limiting. `MainActivity.kt` hosts the Compose surface, drag selection, animation loop, and per-object controls.

The Compose Canvas accepts a tap to create a gravity well and a drag gesture to select and move a body. The benchmark can be run with `./gradlew :benchmark:run` and prints CSV results for 1,000, 2,000, and 5,000 particles. The next native milestones are to connect `SpatialAudioController` to bundled `res/raw` sound assets, add particle pooling, persistent settings, and an OpenGL renderer option for scenes beyond the Canvas budget. The cross-platform sharing plan is documented in `../KMP_ARCHITECTURE.md`.

## Build prerequisites

Use Android Studio with JDK 17, Android SDK Platform 35, Build Tools 35.x, and Gradle 8.9 or newer. From this directory:

```bash
./gradlew :app:assembleDebug
```

The current sandbox does not have the Android SDK or Gradle command installed, so APK compilation must be completed on an Android development machine or after installing those toolchains.

## Download an APK for a Samsung phone

The repository includes a GitHub Actions workflow at `.github/workflows/android-apk.yml`. Every push that changes `android/**` builds the native debug APK, and the workflow can also be started manually from the **Actions** tab using **Android APK → Run workflow**.

After the workflow completes, open the successful run, scroll to **Artifacts**, and download `learncraft-space-physics-debug-apk`. Extract the ZIP on the Samsung phone or computer; the installable file is `app-debug.apk`. On the phone, open the APK with My Files and allow installation from that source when Android asks. The APK is unsigned debug output intended for personal testing, not Play Store distribution.

| Goal | Command or action |
|---|---|
| Build locally | `gradle :app:assembleDebug` from `android/` |
| APK output | `android/app/build/outputs/apk/debug/app-debug.apk` |
| Run shared tests | `gradle :shared:test` |
| Run the benchmark | `gradle :benchmark:run` |
| Build from GitHub | Actions → Android APK → Run workflow |

For a local build, install Android Studio with JDK 17, Android SDK Platform 35, Build Tools 35.0.0, and Gradle 8.9. The sandbox used to edit this repository does not contain the Android SDK or Gradle executable, so the GitHub Actions workflow is the supported no-local-toolchain route for obtaining the APK.
