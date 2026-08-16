# LearnCraft Space Physics

A standalone Canvas prototype for a visual orbital learning interface.

## Features

- 120, 250, or 500 orbiting elements.
- Elastic circle-to-circle collision response with configurable per-object elasticity.
- Uniform spatial-hash broad phase for high-density neighbor collision queries.
- Central and user-dropped gravity wells.
- Opt-in spatial Web Audio feedback for collisions, close passes, and well capture.
- Physics settings panel for selected-object mass, size, and elasticity overrides.
- Audio volume slider and collision sound gap limiter for dense scenes.
- Drag-to-reposition foreground objects, particle bursts, pause, reset, and adaptive quality.

## Run locally

Serve the directory with any static HTTP server, then open the served URL in a browser:

```bash
npx serve -l 4173 .
```

Audio begins only after the user enables it because browsers require a user gesture before starting an audio context.

## Performance note

The prototype uses a shared fixed-step loop, adaptive quality, and a uniform spatial hash that only checks each element against nearby cells. In the browser validation run, the 500-element scene reported approximately 60 FPS while collision counters continued increasing. The spatial-hash cell size is 56 CSS pixels; production integrations should tune it to their maximum element diameter and use a typed-array store when scaling further.
