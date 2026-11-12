# Roadmap

A full renderer replacement is a multi-year project measured in person-years,
not weekends. VulkanMod — one API, one Minecraft version at a time — has taken
years. The milestones below are ordered so that every stage ends with a game
that starts and renders something, which is the only way to keep a project like
this alive.

## M0 — Scaffold ✅

- [x] Gradle + Fabric setup for 26.3 / Java 25
- [x] Backend SPI (`Device`, `Swapchain`, `CommandList`, resources)
- [x] Backend registry, probing, fallback selection
- [x] `polyglass.json` config
- [x] Render-path split (STANDARD / MESH) and capability gating
- [x] `null` headless backend plus JUnit coverage of selection and frame submission
- [x] `angle` backend registration + probing (device creation is M3.5)
- [x] Docs: architecture, backend matrix, native shim contract

## M1 — OpenGL backend, real device

- [ ] Decide the integration seam (Blaze3D `GpuDevice` vs. renderer replacement) — see ARCHITECTURE.md
- [ ] `OpenGlDevice` wrapping the game's existing context
- [ ] `OpenGlSwapchain` on the GLFW window
- [ ] Buffers, textures, pipelines, command lists
- [ ] Render one triangle through `Device` in-game
- [ ] Acceptance: vanilla world renders with Polyglass owning the pipeline, no visual diff

## M2 — Vulkan backend

- [ ] `lwjgl-vulkan` dependency pinned to the game's LWJGL version
- [ ] Instance/device/adapter selection, validation in debug builds
- [ ] Swapchain via `VK_KHR_surface` + `VK_KHR_swapchain` (GLFW handles)
- [ ] Memory allocator, deferred deletion, frames in flight
- [ ] GLSL → SPIR-V shader compilation + disk cache
- [ ] Acceptance: standard path at parity with M1 on Windows and Linux

## M3 — Direct3D 12 backend

- [ ] `native/polyglass_d3d12` C ABI shim (see native/README.md)
- [ ] FFM bindings, no JNI
- [ ] Command queues, descriptor heaps, root signatures
- [ ] SPIR-V → DXIL at build time
- [ ] Acceptance: parity with M2 on Windows

## M3.5 — ANGLE compatibility tier

- [ ] Bundle ANGLE natives (`libEGL` + `libGLESv2`) per platform, unpack at runtime
- [ ] EGL context + surface on the GLFW window, GL ES 3.1 bindings
- [ ] Keep this tier's shaders inside ES 3.1 (no desktop-GL-only features)
- [ ] Config knob for the ANGLE platform (`auto` | `d3d11` | `metal` | `vulkan` | `gl`)
- [ ] Acceptance: world renders on a Windows VM / RDP session where Vulkan and
      native D3D12 are unavailable

## M4 — Metal backend

- [ ] `native/polyglass_metal` shim (ObjC + C ABI)
- [ ] FFM bindings, `CAMetalLayer` on the GLFW `NSView`
- [ ] SPIR-V → MSL, pipeline state caching
- [ ] MoltenVK path kept as a comparison/debug fallback
- [ ] Acceptance: parity with M2 on macOS

## M5 — Mesh-shader render path

- [ ] Chunk section → meshlet build (CPU, then GPU)
- [ ] `GL_NV_mesh_shader` implementation (Nvidium's technique)
- [ ] `VK_EXT_mesh_shader` implementation
- [ ] Frustum + occlusion culling in the mesh pipeline
- [ ] Acceptance: measurable win over the standard path on NVIDIA hardware,
      graceful downgrade everywhere else

## M6 — Hardening

- [ ] Shader cache invalidation, first-run compile budget
- [ ] Crash reporting that names the backend and pipeline
- [ ] Screenshot / video / OBS compatibility
- [ ] Sodium/Iris detection with an explicit "unsupported" screen
- [ ] Settings UI (backend picker + fallback ordering)

## Open decisions

- Licensing of vendor shader toolchains (glslang, DXC, SPIRV-Cross) and whether
  they ship as natives or are invoked at build time.
- Whether mods get a public extension point for custom render paths.
- Minimum GPU: Vulkan 1.2 vs 1.3, D3D12 feature level 12_0 vs 12_1, Metal Apple7+.
