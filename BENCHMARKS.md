# Cross-Target KMP Benchmark Guide

The benchmark module exercises the same `commonMain` physics engine used by Android and the browser adapter. It reports fixed-step timing and broad-phase efficiency rather than relying on a single average that can hide frame spikes.

| Target | Command | What it verifies |
|---|---|---|
| JVM | `./gradlew :benchmark:run` | 100, 250, 500, 1,000, 2,000, and 5,000 body timing, candidate pairs, and collision counts |
| Shared tests | `./gradlew :shared:allTests` | Common physics behavior across configured test targets |
| JavaScript | `./gradlew :shared:jsBrowserProductionWebpack` | Kotlin/JS compilation of the Canvas adapter |
| Android | `./gradlew :app:assembleDebug` | Compose app and Android SoundPool adapter compilation |

## Recommended run procedure

Run the JVM benchmark three times after a warm-up and compare the median and p95 step times. Record particle count, step count, JVM version, CPU model, and whether the machine is throttled. Then run shared tests and JavaScript compilation. Finally build the Android debug APK and measure frame time on a physical device; desktop JVM timings are not a substitute for Android profiling.

The benchmark performs 120 warm-up steps followed by 600 measured fixed steps for each body count. Its CSV output is:

```text
count,steps,median_step_ms,p95_step_ms,candidate_pairs,collisions
```

A spatial-hash regression is indicated when candidate pairs grow toward the all-pairs baseline. The engine now exposes `broadPhaseCandidateCount` so this can be tracked directly. Candidate counts accumulate during the run and should be divided by the measured step count when comparing machines.

The repository includes `scripts/run-benchmarks.sh`, which runs the shared suite in sequence and fails early when the Gradle wrapper is unavailable. Install JDK 17, Android SDK Platform 35, and Gradle 8.9 or newer before running it. The current sandbox has Java but does not have Gradle or the Android SDK, so runtime benchmark execution remains a machine-side step.

## Target interpretation

For interactive Android use, prioritize stable p95 fixed-step time over peak throughput. A useful first target is keeping the 500-body active field below roughly 8 ms per fixed update on the test device, with the 1,000-body benchmark used to reveal degradation rather than as a promise of a specific frame rate. If p95 rises sharply, disable pairwise attraction, reduce collision event emission, and render distant bodies at lower detail before reducing touch responsiveness.
