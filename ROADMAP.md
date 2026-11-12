# Roadmap

Polyglass is a selector, not a renderer. Vulkan and OpenGL come from the game;
DX12 comes from DirectXMod when it is installed on 26.2. Polyglass owns no
rendering code of its own.

## M0 — Thin selector ✅

- [x] Gradle + Fabric for 26.2 / Java 25
- [x] Verified the seam against the 26.2 jar: `PreferredGraphicsApi#getBackendsToTry()`
- [x] Single mixin that reorders the candidate array and merges other mods' entries
- [x] `BackendPlan` ordering policy, pure Java, 8 unit tests
- [x] `polyglass.json` (backend, order, debug)
- [x] Graceful degradation for unavailable backends

## M1 — Menu and diagnostics ✅

- [x] F9 picker with restart notice
- [x] Per-backend state (`not installed`, `Windows only`) and a `▶` selection marker
- [x] "This session" line from `RenderSystem.getBackendDescription()`
- [x] `Get DirectXMod` button (confirmation screen, no bundling)
- [x] `Reset` to platform defaults
- [x] Boot log: platform, config, available ids, resolved plan, running backend
- [x] Keybind translation file + mod icon

## D1 — Direct3D 12 via the 26.2 pin

- [x] Pin Polyglass to Minecraft 26.2, the version DirectXMod targets
- [x] Verify against the 26.2 jar: `com.mojang.blaze3d.systems.GpuBackend`,
      `getBackendsToTry()`, `BackendCreationException.Reason`
- [x] Declare DirectXMod (`gl4dx12`) as `recommends`, looked up by reflection
- [ ] Run with DirectXMod installed and confirm `dx12` is offered and selected
- [ ] Play-test: title screen, world load, resize, screenshot

## V1 — Runtime verification (the remaining gate)

Everything below is compile-time proof so far; none of it has been seen running.

- [ ] `.\gradlew runClient` starts and the mixin applies (no mixin error in the log)
- [ ] `[polyglass] backend order: [vulkan, dx12, opengl]` appears
- [ ] `[polyglass] running on: …` reports a real device
- [ ] Title screen renders; picking OpenGL and restarting changes that line

## Later

- **26.3 port.** When DirectXMod moves, swap the imports in `BackendCatalog`
  and the mixin to `com.mojang.renderpearl.*`, adapt `GpuBackend`'s window
  methods, and bump `gradle.properties` + `fabric.mod.json`. `BackendPlan` and
  the menu need no changes.
- **Competing-mixin warning.** DirectXMod injects at the same `RETURN`; the
  merge already copes, but a log line naming the other mod would help.

## Explicitly out of scope

- **A software/CPU renderer.** A CPU backend is a renderer: `GpuDeviceBackend`,
  command encoder, render passes, a pipeline compiler, and per-OS window
  blitting. Separate project; not shipped half-working here.
- **Live API switching.** The array is consumed once, before the window exists,
  and a `GpuDevice` owns everything created after it. No teardown path exists.
- **Metal (MetalRender).** No source in its repository, macOS/Apple Silicon
  only, and its licence forbids bundling without written permission.
- **VulkanMod.** Targets 1.21.11 and replaces `LevelRenderer` itself; it does not
  implement the `GpuBackend` seam and cannot coexist with 26.x.
- **A Polyglass renderer for Vulkan/OpenGL.** Those already exist in the game.
