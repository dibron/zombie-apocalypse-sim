# zombie-sim

A large-scale, emergent-behavior zombie apocalypse city simulation. Every
entity makes its own decisions each simulation tick based on continuous
stats rather than scripted behavior trees — stories emerge from entity
interactions instead of being hand-authored.

Built to eventually scale to thousands of individually-simulated entities
running for in-game months to years, with the simulation running headless
and fully decoupled from rendering.

## Tech stack

- **Language:** Kotlin
- **Engine:** [libGDX](https://libgdx.com/) (desktop only)
- **ECS:** [Fleks](https://github.com/Quillraven/Fleks)
- **Build:** Gradle (Kotlin DSL)
- **Serialization:** kotlinx-serialization (JSON world-config presets)

## Project layout

```
core/                       Shared logic (no platform-specific code)
  src/main/kotlin/com/zombiesim/
    core/                    Simulation: clock, ECS world, world config
    render/                  libGDX screens (setup screen, sim screen)
    ui/                      scene2d.ui styling and widgets
desktop/                    LWJGL3 desktop launcher
  src/main/kotlin/com/zombiesim/desktop/
```

The simulation (`core` package) has no dependency on rendering — it runs
on its own clock thread and can be driven independently of frame rate.

## Building and running

Requires JDK 17+.

```bash
# build everything
./gradlew build

# launch the app
./gradlew desktop:run
```

On Windows (PowerShell/cmd), use `gradlew.bat` instead of `./gradlew`.

The app opens on a setup screen for configuring the world (population,
infection rules, economy, military presence, weather, etc.), with
options to randomize, reset to defaults, and save/load presets. Hitting
"Generate & Start" moves to the simulation screen, which currently shows
a headless clock ticking with play/pause and speed controls (1x–10000x).

## Status

Early-stage foundation work: simulation clock, ECS wiring, and
world-configuration UI are in place. World generation, entities, and
simulation behavior have not been built yet.
