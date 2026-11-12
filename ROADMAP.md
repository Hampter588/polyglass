# Roadmap

Polyglass is a selector, not a renderer. Vulkan and OpenGL come from the game;
DX12 comes from DirectXMod when that mod runs on the targeted version; the CPU
fallback is the only renderer Polyglass owns.

## M0 — Thin selector ✅

- [x] Gradle + Fabric for 26.3 / Java 25
- [x] Verified the seam against the 26.3 jar: `PreferredGraphicsApi#getBackendsToTry()`
- [x] Single mixin that reorders the candidate array and merges other mods' entries
- [x] `BackendPlan` ordering policy, pure Java, 8 unit tests
- [x] `polyglass.json` (backend, order, software toggle, debug)
- [x] F9 picker screen with restart notice
- [x] Graceful degradation for unavailable backends

## S1 — CPU raster core ✅

- [x] ARGB colour buffer + float depth buffer
- [x] Clipped, flat-shaded triangle fill with depth test
- [x] Headless tests (coverage, ordering, clipping, degenerate)

## S2 — Software backend: device binding

- [ ] `SoftDevice implements GpuDevice`: buffers, textures, samplers, texture views
- [ ] `CommandEncoder` → `SoftRaster` (clear, draw, viewport/scissor)
- [ ] `compilePipeline`: consume the frontend's SPIR-V and run a minimal
      vertex/fragment interpreter, starting with `core/screenquad` and
      `position_tex_color`
- [ ] Offscreen acceptance: render a frame into `SoftRaster` and dump a PNG from
      a JUnit test — no window, no game
- [ ] Keep `createDevice` throwing until the offscreen frame is correct

## S3 — Software backend: presentation

This is the decision that makes or breaks the tier, and it is per-OS native work.

- [ ] Choose the blit path: Win32 `StretchDIBits`, X11 SHM, or Cocoa `CALayer`
- [ ] Own the window (the 26.3 `GpuBackend.createWindow` contract now allows it)
- [ ] Wire input so the game still receives keyboard/mouse
- [ ] Acceptance: title screen and a flat world at 320×240 on a machine with no
      working Vulkan *and* no working OpenGL driver

If S3 turns out to cost more than it returns, stop at S2: the raster core is
still useful for CI screenshots and golden-image tests.

## D1 — Direct3D 12

Blocked upstream, not on Polyglass: DirectXMod is on Minecraft 26.2 and the
26.2 → 26.3 GPU API change (`com.mojang.blaze3d.systems.GpuBackend` →
`com.mojang.renderpearl.api.device.GpuBackend`, plus backend-owned window
creation) is source-breaking.

- [ ] Track DirectXMod for a 26.3 branch
- [ ] When it lands: verify `BackendCatalog` picks it up with no code change
- [ ] Alternative: pin Polyglass to 26.2, where it works today

## X1 — Optional integration probe

- [ ] Show, in the picker, which backends could actually create a device
- [ ] Warn instead of silently reordering when another mod owns the same mixin
- [ ] `polyglass doctor` log block: OS, GPU, available backends, missing natives

## Explicitly out of scope

- **Live API switching.** The array is consumed once, before the window exists,
  and a `GpuDevice` owns everything created after it. No teardown path exists.
- **Metal (MetalRender).** No source in its repository, macOS/Apple Silicon
  only, and its licence forbids bundling without written permission.
- **VulkanMod.** Targets 1.21.11 and replaces `LevelRenderer` itself; it does not
  implement the `GpuBackend` seam and cannot coexist with 26.x.
- **A Polyglass renderer for Vulkan/OpenGL.** Those already exist in the game.
