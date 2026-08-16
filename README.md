# Orbital Physics Engine

A visual-first orbital simulation for LearnCraft, built around a shared Kotlin Multiplatform physics core and platform-specific rendering adapters. The project combines a browser Canvas prototype, Jetpack Compose Android renderer, spatial audio, gravity wells, elastic collisions, drag interaction, and high-density spatial-hash simulation.

## What is included

| Layer | Location | Purpose |
|---|---|---|
| Browser prototype | `index.html` | Interactive Canvas scene with gravity wells, drag gestures, collisions, particles, adaptive quality, and Web Audio |
| Shared physics | `android/shared/src/commonMain` | Fixed-step integration, softened gravity, nearby attraction, uniform spatial hash, collision response, and physics events |
| Browser adapter | `android/shared/src/jsMain` | Connects the shared engine to an HTML Canvas and pointer events |
| Android renderer | `android/app` | Jetpack Compose Canvas rendering, touch-created wells, object settings, adaptive detail, and SoundPool audio |
| Benchmark | `android/benchmark` | JVM benchmark matrix from 100 to 5,000 bodies with median/p95 timing and broad-phase candidate counts |

## Core capabilities

The engine supports a central gravity field, finite gravity wells, per-object mass/radius/elasticity values, elastic collision response, mass-weighted positional correction, collision-event budgeting, spatial audio cues, and a capped fixed-step runner. Nearby attraction is restricted to spatial-hash neighbors so the active simulation avoids a global all-pairs force pass.

The visual system intentionally favors an exploratory space rather than a dashboard. Objects orbit a central learning core, can be dragged, and respond to wells with glow, burst, and audio feedback. The Android renderer uses level-of-detail limits for smaller viewports and larger scenes so touch responsiveness remains more important than rendering every background particle at maximum detail.

## Run the browser prototype

Serve the repository with any static HTTP server and open the resulting URL:

```bash
npx serve -l 4173 .
```

Audio begins only after the user enables it because browsers require a user gesture before an audio context can start.

## Build the Kotlin Multiplatform targets

The Android project expects JDK 17, Android SDK Platform 35, and Gradle 8.9 or newer. From the `android` directory, use the Gradle tasks below:

```bash
./gradlew :shared:allTests
./gradlew :shared:jsBrowserProductionWebpack
./gradlew :app:assembleDebug
./gradlew :benchmark:run
```

The current sandbox does not include the Gradle wrapper or Android SDK, so Android and Kotlin compilation should be run on a development machine with the stated toolchain.

## Run benchmarks

The benchmark suite performs 120 warm-up steps and 600 measured fixed steps for each body count. It prints CSV columns for body count, median step time, p95 step time, spatial-hash candidate pairs, and resolved collisions.

```bash
./scripts/run-benchmarks.sh
```

See [`BENCHMARKS.md`](BENCHMARKS.md) for measurement guidance and interpretation. See [`KMP_ARCHITECTURE.md`](KMP_ARCHITECTURE.md) for the shared-module design and platform boundaries.

## Repository status

This is an active engineering prototype. The browser prototype is directly runnable; Android/KMP source is organized for compilation on a machine with the required Kotlin, Gradle, and Android toolchain. Benchmark numbers should always be collected on the target device or a documented reference machine rather than inferred from source inspection.
