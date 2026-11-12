# Polyglass

A graphics backend selector for Minecraft 26.2: pick Vulkan, OpenGL or Direct3D
12 from one menu, with an automatic fallback chain.

Pinned to **26.2** on purpose: DirectXMod, the only working D3D12 backend,
targets it. 26.3 moved the GPU API from `com.mojang.blaze3d.systems` to
`com.mojang.renderpearl.api` and made the backend own window creation, which is
source-breaking for that mod. See [ARCHITECTURE.md](ARCHITECTURE.md).

**poly** (many backends) + **glass** (what you look at the game through).

## How it works

Minecraft 26.x already owns the hard part. `PreferredGraphicsApi.getBackendsToTry()`
returns the candidate backends, and the boot loop tries each one, catching
`BackendCreationException` and moving to the next. Vanilla ships
`GlBackend` and `VulkanBackend`.

Polyglass is a single mixin on that method. It reorders the array according to
`config/polyglass.json` and reuses any backend another mod already contributed.
It does not implement a renderer, a device lifecycle, or a shader compiler — the
game already has all of that.

## Backends

| Id | Source | Notes |
|---|---|---|
| `vulkan` | vanilla `VulkanBackend` | First choice on Windows and Linux |
| `opengl` | vanilla `GlBackend` | Always available; the safety net |
| `dx12` | [DirectXMod](https://github.com/366862732/DirectXmod) | Windows only, 26.2 only; requires that mod installed. Apache-2.0, not bundled |

`auto` (the default) means "walk the configured order and take the first backend
that can create a device". A backend that refuses is skipped, not fatal.

DirectXMod is an optional, soft dependency: `fabric.mod.json` declares it under
`recommends`, so installing it is never required and never blocks the game from
launching. When it is absent, the `dx12` entry simply never appears in the
candidate list.

### No live switching

The candidate list is consumed once, before the GLFW window exists, and the
`GpuDevice` owns every buffer, texture and pipeline created afterwards. There is
no teardown-and-recreate path in the game. The menu therefore writes the config
and asks for a restart — the same model as every game with a DirectX/Vulkan
toggle.

## In-game menu

**F9** (rebindable under Controls → Polyglass) opens the picker:

```
Polyglass — graphics backend
This session: OpenGL 4.6
Order: vulkan → dx12 → opengl

▶ Auto — follow order
   Vulkan (vanilla)
   Direct3D 12 (DirectXMod) — not installed
   OpenGL (vanilla)

[Get DirectXMod (optional)]
Changes apply on the next launch.
[Reset]                              [Done]
```

- `This session:` reports the device the game actually created, which is the
  only reliable answer to "did my choice take effect?".
- `Get DirectXMod` opens the upstream repository behind a confirmation screen.
- `Reset` restores `auto` and the platform default order.

## Configuration

`config/polyglass.json`, written on first launch:

```json
{
  "backend": "auto",
  "order": ["vulkan", "dx12", "opengl"],
  "debug": false
}
```

Selection: explicit `backend` first (if usable), then `order`, then anything
still available. Unknown or unavailable ids are dropped silently; `debug: true`
logs the resolved plan and the running backend.

## Building

```powershell
.\gradlew build
```

Targets Minecraft **26.2**, Fabric Loader 0.19.5, Java 25. Versions live in
`gradle.properties`. Minecraft 26.1+ is unobfuscated, so there is no `mappings`
dependency and the Loom plugin id is `net.fabricmc.fabric-loom`.

Run with `.\gradlew runClient`, or drop `build/libs/polyglass-*.jar` into
`mods/`. Tests (`src/test/java`, JUnit 5) need no GPU, no window and no
Minecraft classes.

## Layout

```
src/main/java/dev/polyglass/
  Polyglass.java                  entrypoint: boot log + F9 picker
  Probe.java                      OS / classpath probes
  backend/BackendPlan.java        ordering policy (pure, unit tested)
  backend/BackendCatalog.java     builds the GpuBackend[] from config
  config/PolyglassConfig.java     polyglass.json
  menu/PolyglassScreen.java       backend picker
  mixin/PreferredGraphicsApiMixin.java   the entire integration
```

## What is not included

- **No software/CPU renderer.** A CPU backend is not a menu entry, it is a
  renderer: 26.2 requires a full `GpuDeviceBackend`, a command encoder and
  render passes, a pipeline compiler, and per-OS window blitting. That is a
  separate project, so it is out of scope rather than shipped half-working.
- **No live API switching.** The array is consumed once, before the window
  exists; no teardown path exists.
- **No Metal.** MetalRender ships no source and its licence forbids bundling.
- **No VulkanMod.** It targets 1.21.11 and replaces `LevelRenderer` itself; it
  does not implement the `GpuBackend` seam.

Read [ARCHITECTURE.md](ARCHITECTURE.md) for why it is built this way.
