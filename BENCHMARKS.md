# Cross-Target KMP Benchmark Guide

The benchmark module exercises the same `commonMain` physics engine used by Android and the browser adapter.

| Target | Command | What it verifies |
|---|---|---|
| JVM | `./gradlew :benchmark:run` | 1,000, 2,000, and 5,000 particle step timing and collision counts |
| Shared tests | `./gradlew :shared:allTests` | Common physics behavior across configured test targets |
| JavaScript | `./gradlew :shared:jsBrowserProductionWebpack` | Kotlin/JS compilation of the Canvas adapter |
| Android | `./gradlew :app:assembleDebug` | Compose app and Android SoundPool adapter compilation |

## Recommended run procedure

Run the JVM benchmark three times after a warm-up and compare the median average step time. Record particle count, average step milliseconds, collision count, JVM version, CPU model, and whether the run is throttled. Then run the shared tests and JavaScript compilation. Finally build the Android debug APK and measure frame time on a physical device; desktop JVM timings are not a substitute for Android profiling.

The repository includes `scripts/run-benchmarks.sh`, which runs the shared suite in sequence and fails early when the Gradle wrapper is unavailable. Install JDK 17, Android SDK Platform 35, and Gradle 8.9 or newer before running it. The current sandbox has Java but does not have Gradle or the Android SDK, so runtime benchmark execution remains a machine-side step.

## Metrics to capture

Capture average fixed-step time, p95 step time, broad-phase candidate pairs, resolved collisions, allocations per frame, and audio events per second. A spatial-hash regression is indicated by candidate pairs growing close to the all-pairs baseline as density rises.
