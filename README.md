# Polyglass

A graphics backend selector for Minecraft 26.2: pick Vulkan, OpenGL, Direct3D 12
or a CPU rasteriser from one button, with an automatic fallback chain.

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
| `software` | Polyglass | CPU rasteriser. **Incomplete** — see below |

`auto` (the default) means "walk the configured order and take the first backend
that can create a device". A backend that refuses is skipped, not fatal.

### No live switching

The candidate list is consumed once, before the GLFW window exists, and the
`GpuDevice` owns every buffer, texture and pipeline created afterwards. There is
no teardown-and-recreate path in the game. The menu therefore writes the config
and asks for a restart — the same model as every game with a DirectX/Vulkan
toggle.

### Software backend status

The CPU core (`dev.polyglass.backend.software.SoftRaster`) works: colour buffer,
depth buffer, clipped filled triangles, and headless tests. The `GpuDevice`
binding does not exist yet, so `SoftBackend.createDevice` throws
`BackendCreationException` on purpose. That keeps the entry safe to enable: the
boot loop treats it as "unavailable" and falls through to Vulkan/OpenGL instead
of booting into a half-bound device.

It is off by default (`enableSoftware`). Turn it on to see the fallback in the
log, not to get frames.

### About VulkanMod and MetalRender

Not integrated, deliberately:

- **VulkanMod** targets Minecraft 1.21.11 and predates the 26.x backend SPI. It
  replaces `LevelRenderer` itself, so it cannot plug into `PreferredGraphicsApi`
  at all. On 26.x its job is done by the game's own `VulkanBackend`.
- **MetalRender** is macOS/Apple-Silicon only, ships binaries without source in
  the repository, and its licence requires prior written permission to
  redistribute — including bundling. It cannot be wrapped or shipped.

## Configuration

`config/polyglass.json`, written on first launch:

```json
{
  "backend": "auto",
  "order": ["vulkan", "dx12", "opengl", "software"],
  "enableSoftware": false,
  "debug": false
}
```

Selection: explicit `backend` first (if usable), then `order`, then anything
still available. Unknown or unavailable ids are dropped silently; `debug: true`
logs the resolved plan.

In game, **F9** opens the picker. It is also reachable from the key bindings
menu.

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
  Polyglass.java                  entrypoint: config log + F9 picker
  Probe.java                      OS / classpath probes
  backend/BackendPlan.java        ordering policy (pure, unit tested)
  backend/BackendCatalog.java     builds the GpuBackend[] from config
  backend/software/SoftBackend.java   CPU backend (device binding pending)
  backend/software/SoftRaster.java    framebuffer + triangle rasteriser
  config/PolyglassConfig.java     polyglass.json
  menu/PolyglassScreen.java       backend picker
  mixin/PreferredGraphicsApiMixin.java   the entire integration
```

Read [ARCHITECTURE.md](ARCHITECTURE.md) for why it is built this way, and
[ROADMAP.md](ROADMAP.md) for what is left.
