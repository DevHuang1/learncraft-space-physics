# LearnCraft Space Physics — Native Android

This module is the native Android direction for the orbital physics prototype.

## Architecture

The app uses **Kotlin**, **Jetpack Compose**, and a custom Compose `Canvas` viewport. `PhysicsEngine.kt` is renderer-independent and owns fixed-step integration, gravity wells, per-object mass/radius/elasticity, event emission, and a uniform spatial-hash collision broad phase. `FixedStepRunner` bounds substeps. `SpatialAudioController.kt` maps collision and gravity-well events to pooled Android sounds with stereo pan, volume, and collision-gap limiting. `MainActivity.kt` hosts the Compose surface, drag selection, animation loop, and per-object controls.

The next native milestones are to connect `SpatialAudioController` to bundled `res/raw` sound assets, add particle pooling, persistent settings, and an OpenGL renderer option for scenes beyond the Canvas budget. The cross-platform sharing plan is documented in `../KMP_ARCHITECTURE.md`.

## Build prerequisites

Use Android Studio with JDK 17, Android SDK Platform 35, Build Tools 35.x, and Gradle 8.9 or newer. From this directory:

```bash
./gradlew :app:assembleDebug
```

The current sandbox does not have the Android SDK or Gradle command installed, so APK compilation must be completed on an Android development machine or after installing those toolchains.
