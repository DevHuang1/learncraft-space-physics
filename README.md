# LearnCraft Space Physics

A standalone Canvas prototype for a visual orbital learning interface.

## Features

- 120, 250, or 500 orbiting elements.
- Elastic circle-to-circle collision response with configurable elasticity.
- Central and user-dropped gravity wells.
- Opt-in spatial Web Audio feedback for collisions, close passes, and well capture.
- Physics settings panel for global object mass and elasticity.
- Drag-to-reposition foreground objects, particle bursts, pause, reset, and adaptive quality.

## Run locally

Serve the directory with any static HTTP server, then open the served URL in a browser:

```bash
npx serve -l 4173 .
```

Audio begins only after the user enables it because browsers require a user gesture before starting an audio context.

## Performance note

The prototype uses a shared fixed-step loop and adaptive quality. At 500 elements, the deliberately simple collision pass can lower the reported frame rate; production integration should use the spatial-hash neighbor strategy documented in the orbital visual learning skill before scaling collision response further.
