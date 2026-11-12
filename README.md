# Polyglass

A full renderer replacement for Minecraft: Java Edition that presents several
graphics backends behind one API.

The name: **poly** (many backends) + **glass** (what you look at the game
through).

## Status

**Scaffold.** What works today: backend registry, platform probing, backend
selection with fallback chains, render-path selection, and a config file. What
does not: every backend's `createDevice` throws. No pixels are drawn by
Polyglass yet. See [ROADMAP.md](ROADMAP.md).

## Backends

| Id       | API          | Platforms            | Bindings | Notes |
|----------|--------------|----------------------|----------|-------|
| `vulkan` | Vulkan 1.3+  | Windows, Linux, macOS* | LWJGL | Primary target |
| `d3d12`  | Direct3D 12  | Windows              | FFM → native shim | No LWJGL bindings exist |
| `metal`  | Metal 3      | macOS                | FFM → native shim | Or route through MoltenVK |
| `angle`  | OpenGL ES 3.1 | Windows, macOS, Linux | LWJGL EGL/GLES + bundled ANGLE | Compatibility tier: D3D11 / Metal / Vulkan |
| `opengl` | OpenGL 4.6   | Everything           | LWJGL (bundled) | Reference backend, always available |
| `null`   | —            | Everything           | — | Headless. Never auto-selected |
| `mesh`   | *not an API* | GPU-dependent        | — | Alias: OpenGL + mesh-shader render path |

\* macOS has no native Vulkan driver; MoltenVK is required. LWJGL's Vulkan
module bundles it, but device enumeration on macOS needs
`VK_KHR_portability_enumeration` enabled explicitly.

### `angle` and `null`

`angle` is the compatibility tier: GL ES 3.1 translated by ANGLE to D3D11 on
Windows, Metal on macOS, or Vulkan/GL on Linux. It covers Windows VMs, RDP
sessions, old Intel iGPUs without Vulkan drivers, and macOS — at the cost of a
translation layer. It is **GL ES**, not desktop GL, so Sodium/Iris will not run
on it.

`null` is a working headless device: every call succeeds and nothing is drawn.
Use it for CI, screenshot harnesses, dedicated servers, and for proving the
submit path before blaming a driver. It is only selected when asked for by name.

Selection order is: explicit choice → `fallback` chain → priority order. `null`
is never in the default chain.

### About `mesh`

You asked for "the one Nvidium uses". Nvidium does not use a different graphics
API — it renders chunks with **mesh shaders** (`GL_NV_mesh_shader`) on top of
OpenGL. So in Polyglass `mesh` is a *render path*, not a fifth API:

```json
{ "backend": "mesh" }
```

means "OpenGL device, mesh-shader chunk pipeline". When the device reports no
mesh-shader support (i.e. not an NVIDIA card), Polyglass logs a warning and
drops to the standard chunk path instead of failing to start. The same path is
planned for Vulkan via `VK_EXT_mesh_shader`.

## Configuration

`config/polyglass.json`, written on first launch:

```json
{
  "backend": "auto",
  "fallback": ["vulkan", "d3d12", "angle", "opengl"],
  "meshPath": true,
  "debug": false,
  "framesInFlight": 2
}
```

`backend` accepts `auto`, `opengl`, `vulkan`, `d3d12`, `metal`, `angle`,
`null`, `mesh`.
Selection order is: explicit choice → `fallback` chain → priority order. A
backend that cannot run is skipped with a logged reason.

## Building

You need **JDK 25** and **Gradle 9.7.1** (or generate a wrapper). Nothing is
installed on this machine yet, so the build has not been run.

```powershell
.\gradlew build
```

Targets: Minecraft **26.3**, Fabric Loader 0.19.5, Java 25. Versions live in
`gradle.properties`. Minecraft 26.1+ is unobfuscated, so there is no `mappings`
dependency and the Loom plugin id is `net.fabricmc.fabric-loom` (not `-remap`).

Tests run without a GPU (`src/test/java`, JUnit 5) and drive the null backend
through a full frame.

Then run with `.\gradlew runClient`, or drop the jar from
`build/libs/` into `mods/`.

## Layout

```
src/main/java/dev/polyglass/
  Polyglass.java          entrypoint: select backend + render path
  Probe.java              OS / classpath / native-library probes
  api/                    backend-neutral device API (no Minecraft imports)
  backend/                one package per API implementation
  config/                 polyglass.json
  renderpath/             standard vs mesh-shader chunk pipeline
native/                   C ABI contract for the D3D12 and Metal shims
```

Read [ARCHITECTURE.md](ARCHITECTURE.md) before writing a backend.
