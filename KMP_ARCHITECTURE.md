# Kotlin Multiplatform Blueprint: Shared Orbital Physics

## Goal

Keep simulation rules, fixed-step timing, spatial-hash broad phase, gravity wells, collisions, and event generation in Kotlin Multiplatform `commonMain`. Keep rendering, pointer input, and audio platform-specific.

| Layer | `commonMain` | Android | Web/Wasm or JS |
|---|---|---|---|
| Physics state | `Body`, `GravityWell`, `PhysicsSettings` | Uses shared state | Uses shared state |
| Simulation | Fixed-step runner, gravity, spatial hash, elastic collisions | Calls engine from a frame coroutine | Calls engine from `requestAnimationFrame` |
| Rendering | No platform APIs | Compose `Canvas` or OpenGL renderer | Canvas 2D or WebGL renderer |
| Input | Commands such as `SelectBody`, `DragBody`, `DropWell` | Compose pointer input | Pointer and touch events |
| Audio | `PhysicsEvent` output only | `SoundPoolSpatialAudio` | Web Audio spatial mixer |
| Persistence | Serializable settings/progress DTOs | DataStore or Room adapter | IndexedDB/local storage adapter |

## Recommended source set

```text
shared/
  src/commonMain/kotlin/physics/
    Body.kt
    GravityWell.kt
    PhysicsEngine.kt
    FixedStepRunner.kt
    PhysicsEvent.kt
  src/androidMain/kotlin/audio/
    SpatialAudioController.kt
  src/jsMain/kotlin/audio/
    WebSpatialAudioController.kt
androidApp/
  src/main/kotlin/.../MainActivity.kt
webApp/
  src/jsMain/kotlin/.../CanvasRenderer.kt
```

## Shared engine contract

```kotlin
interface PhysicsWorld {
    val bodies: List<Body>
    val wells: List<GravityWell>
    fun step(dt: Float, width: Float, height: Float, settings: PhysicsSettings): List<PhysicsEvent>
}
```

The engine should receive a fixed `dt`, never read wall-clock time for integration, and return immutable event data for rendering and audio adapters. A platform loop accumulates real frame time and calls the engine at 60 Hz, with a bounded substep count to prevent a slow frame from causing a spiral of death.

## Android adapter

Compose owns layout and pointer gestures. A `Canvas` or OpenGL renderer reads the shared body snapshot. `SoundPoolSpatialAudio` consumes collision and gravity-well events, computes stereo pan from the event x-position, and applies a global volume plus collision event rate limit.

## Web adapter

The repository now contains a real `shared/src/jsMain` `CanvasPhysicsAdapter`. It owns browser Canvas rendering, pointer coordinates, drag selection, and tap-created gravity wells while calling the common `FixedStepRunner`. The Android adapter loads bundled `res/raw/collision.wav` and `res/raw/gravity_well.wav` through `SpatialAudioController`; the browser audio layer can consume the same `PhysicsEvent` types through a Web Audio `StereoPannerNode`. Browser audio must initialize only from a user gesture.

## Migration sequence

First move the current Kotlin physics core into `shared/commonMain` and add deterministic tests for collision response, gravity wells, and broad-phase candidate generation. Then build platform renderers without changing the engine. Finally add platform input and audio adapters. This preserves behavioral parity while allowing Android to move from Compose Canvas to OpenGL later without rewriting simulation rules.

## Performance rules

Use a uniform spatial hash with a cell size at least the maximum body diameter, avoid allocations inside the fixed-step loop, reuse event and candidate buffers, keep rendering snapshots separate from mutable physics state, and degrade background rendering before lowering simulation correctness. For 100–500 elements, measure frame time, simulation time, collision candidates, resolved collisions, and audio events per second on both platforms. The `BENCHMARKS.md` guide and `scripts/run-benchmarks.sh` describe JVM, shared-test, JavaScript compilation, and Android build commands.
