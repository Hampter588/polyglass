# Architecture

## Layers

```
┌──────────────────────────────────────────────┐
│ Minecraft (26.3)                             │
│  GameRenderer · LevelRenderer · GUI          │
└───────────────────┬──────────────────────────┘
                    │  game-side hook (mixin / replaced renderer)
┌───────────────────▼──────────────────────────┐
│ dev.polyglass.api                            │
│  Device · Swapchain · CommandList ·          │
│  Buffer · Texture · Pipeline · Capabilities  │
└───────────────────┬──────────────────────────┘
       ┌────────────┴─────────────┬──────────────┐
┌──────▼──────┐ ┌──────▼──────┐ ┌──▼───────┐ ┌──▼──────┐
│ opengl      │ │ vulkan      │ │ d3d12    │ │ metal   │
│ (LWJGL)     │ │ (LWJGL)     │ │ (FFM)    │ │ (FFM)   │
└─────────────┘ └─────────────┘ └──────────┘ └─────────┘
        ▲ render paths: STANDARD · MESH (mesh shaders)
```

Rules that keep this sane:

- **`api/` never imports Minecraft and never imports a backend.** It is plain
  Java records and interfaces. It can be unit-tested without a game.
- **A backend never imports another backend.** Shared code goes in `backend/`.
- **Render paths never touch an API directly.** They call `Device`.
- **Capabilities, not `if (api == VULKAN)`.** If a feature is missing, gate it
  through `Capabilities` so a new backend gets it for free.

## Integration seam

Two options, and this choice is the single most important decision in the
project:

1. **Implement Blaze3D's device layer.** Since 1.21.5 the game routes GPU work
   through `com.mojang.blaze3d.systems.GpuDevice` / `GpuBuffer` /
   `RenderPipeline`. If 26.3 still exposes that seam, Polyglass can supply a
   non-GL `GpuDevice` and keep vanilla's renderer intact. Small blast radius,
   huge payoff.
2. **Replace the renderer.** Own `GameRenderer` + `LevelRenderer` and draw the
   world ourselves, the way VulkanMod does. Much more work, but it is the only
   way to get the mesh-shader path and real wins on chunk geometry.

**Verify option 1 first** against a decompiled 26.3 jar (`gradlew genSources`).
If the seam is usable, take it for the standard path and use option 2 only for
the mesh path. If it is not, this becomes a VulkanMod-scale project.

## Backend contract

- `isAvailable()` is a cheap probe and must never throw. It answers "could this
  start here?", not "is the GPU good?".
- `createDevice()` does the expensive work: instance/device creation, adapter
  pick, extension query, native library load.
- Failure at `createDevice()` is recoverable: `Backends.select` already picked a
  chain, and the caller may move to the next entry.
- `close()` is idempotent.

## Frames and threading

- One render thread. `Device` is render-thread-confined.
- `framesInFlight` (default 2) swapchain images; resources are referenced by
  frame index and recycled on fence signal.
- `waitIdle()` only on resize, screenshot, resource reload and shutdown.
- Uploads go through a transfer queue where the backend has one; the OpenGL
  backend emulates it with orphaning + `glFenceSync`.

## Capability gating

`Capabilities` is produced once at device creation and is the only source of
truth. A backend that cannot fill in a field must report the conservative value
rather than guess. `RenderPaths.resolve` uses it to downgrade MESH → STANDARD.

## The hard parts (in order of pain)

1. **Shaders.** Vanilla ships GLSL. Vulkan needs SPIR-V, D3D12 needs DXIL,
   Metal needs MSL. Plan: GLSL → SPIR-V (glslang) as the canonical form, then
   SPIR-V → DXIL/MSL at build time, cached on disk. This is most of the work.
   Exception: the MoltenVK route on macOS does the SPIR-V → MSL step itself,
   so that path only needs the GLSL → SPIR-V half.
2. **Resource lifetime.** GL lets you delete anything at any time. Vulkan/D3D12
   require deferred destruction tied to frames in flight.
3. **State emulation.** `GlStateManager`, VAOs, client-side arrays, and
   `glBlitFramebuffer` have no direct equivalent. Each needs a translation
   layer, and each is a correctness bug waiting to happen.
4. **Present/format differences.** sRGB handling, depth precision, MSAA resolve,
   and window resize all behave differently per API.
5. **Mod compatibility.** Sodium and Iris hook deep into the GL renderer.
   Assume they are incompatible until proven otherwise, and detect + warn
   instead of crashing.

## Non-goals for M0

Compute, ray tracing, HDR output, multi-GPU, and a settings UI. The config file
is the UI until the standard path renders a correct frame.
