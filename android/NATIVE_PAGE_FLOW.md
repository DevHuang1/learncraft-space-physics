# Native Android Orbital Page Flow

The Android application is a self-contained, portrait-first **LearnCraft Space Physics** experience. It uses a tactile orbital home screen rather than a conventional tab bar. Every destination remains explicitly labelled so motion is decorative, not required for navigation.

| Screen | Primary content | Main action |
|---|---|---|
| Home | Central physics core and four orbiting destinations | Open a laboratory, saved snapshot, or preferences |
| Experiments | Four named physics scenarios with clear body-count labels | Launch a real shared-engine simulation |
| Saved Simulations | Locally stored full body/well/settings snapshots | Restore or delete a saved state |
| Settings | Sound, reduced-motion, attraction, and velocity controls | Persist device-local preferences |
| Simulator | High-density Compose canvas, gravity wells, drag controls, and object overrides | Save a full local snapshot or return to orbit |

The visual system uses deep-space navy (`#050814`), violet for the learning core (`#8B5CF6`), mint for experiments (`#34D399`), blue for saved signals (`#60A5FA`), and gold for settings (`#FBBF24`). All major targets have text labels, high-contrast captions, and large touch areas suitable for one-handed portrait use.

## Primary flows

1. **Home → Experiments → Simulator:** The user taps the mint experiment world, selects a scenario, and enters the existing high-performance physics canvas.
2. **Simulator → Save → Saved Simulations → Restore:** The user saves the current bodies, wells, and physics settings locally, then restores that exact snapshot later from the blue saved-signal page.
3. **Home → Settings → Home:** The user adjusts sound, motion, attraction, or velocity settings. Values persist locally and are applied to future simulation launches.
