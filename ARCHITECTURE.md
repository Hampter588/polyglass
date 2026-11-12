# Architecture

## The seam (verified against the 26.2 jar)

Minecraft chooses its graphics backend through one method:

```java
// net.minecraft.client.PreferredGraphicsApi  (enum: DEFAULT, OPENGL, VULKAN)
public GpuBackend[] getBackendsToTry();
```

The boot loop walks that array and tries each candidate:

```
for (GpuBackend backend : preferred.getBackendsToTry()) {
    backend.setWindowHints();                       // inside GLFW's error scope
    // window is created by the game with those hints
    try {
        GpuDevice device = backend.createDevice(window, shaderSource, debugOptions, criticalShaderLoader);
        // success -> keep it
    } catch (BackendCreationException e) {
        // failure -> next candidate
    }
}
```

That is the whole integration surface. Polyglass is one `@Inject` on that
method's `RETURN`. It does not implement a renderer, a device lifecycle, a
shader compiler, or a window.

```
┌──────────────────────────────────────────────────────────┐
│ Minecraft 26.2                                           │
│  PreferredGraphicsApi.getBackendsToTry()                 │
└───────────────────┬──────────────────────────────────────┘
                    │ mixin, @At("RETURN")
┌───────────────────▼──────────────────────────────────────┐
│ dev.polyglass.backend                                    │
│  BackendPlan     pure ordering policy (unit tested)      │
│  BackendCatalog  GpuBackend[] assembly + merge           │
└───────────────────┬──────────────────────────────────────┘
        ┌───────────┴────────────┬─────────────────┐
┌───────▼────────┐ ┌─────────────▼──────┐ ┌────────▼─────────┐
│ GlBackend      │ │ VulkanBackend      │ │ SoftBackend      │
│ (vanilla)      │ │ (vanilla)          │ │ (Polyglass)      │
└────────────────┘ └────────────────────┘ └──────────────────┘
      plus, if installed: com.xgdt.dx12.dx12.Dx12Backend
```

The 26.2 `GpuBackend` contract:

```java
public interface GpuBackend {
    String getName();
    void setWindowHints();
    void handleWindowCreationErrors(GLFWErrorCapture.Error error) throws BackendCreationException;
    GpuDevice createDevice(long window, ShaderSource defaultShaderSource,
                           GpuDebugOptions debugOptions, Runnable criticalShaderLoader)
            throws BackendCreationException;
}
```

## Why 26.2 and not 26.3

26.3 moved the GPU API into a separate library and changed the contract:

| | 26.2 (target) | 26.3 |
|---|---|---|
| Package | `com.mojang.blaze3d.systems` | `com.mojang.renderpearl.api.device` |
| Window | created by the game | **created by the backend** (`createWindow(String,int,int,long)`) |
| Hooks | `setWindowHints`, `handleWindowCreationErrors` | removed |
| Device | `createDevice(long, ShaderSource, GpuDebugOptions, Runnable)` | `createDevice(GpuDebugOptions)` |

DirectXMod — the only real D3D12 backend in the ecosystem — targets 26.2. Its
`Dx12Backend` implements the 26.2 interface, so on 26.3 the class cannot even
load. Pinning to 26.2 buys a working DX12 today; the selector itself is
version-agnostic, and moving to 26.3 later is an import sweep plus a different
`SoftBackend` signature.

## 26.3, for the record

In 26.3 the interface and implementations live at:

```
com.mojang.renderpearl.api.device.GpuBackend
com.mojang.renderpearl.api.device.GpuDevice
com.mojang.renderpearl.backend.opengl.GlBackend
com.mojang.renderpearl.backend.vulkan.VulkanBackend
```

`PreferredGraphicsApi.getBackendsToTry()` still exists, so the mixin target is
stable across both versions — only the imported types change.

## Competing mixins

DirectXMod injects at the same `RETURN` with `cancellable = true`. Two injectors
that both call `setReturnValue` would fight, and the last one applied would win.
`BackendCatalog` avoids that by **merging**: it reads the array it is given,
reuses the instances already in it, adds the ones it can construct, and only
then reorders. Unknown backends from other mods are preserved at the end. The
result converges regardless of application order.

## Selection policy

`BackendPlan.resolve(requested, order, available)` is pure `java.util` and unit
tested:

1. explicit `backend` (if usable),
2. each id in `order` (if usable),
3. any remaining usable id, so a newly added backend is never dead on arrival.

Unavailable ids are dropped, never fatal. If the plan comes out empty, the
original vanilla array is returned untouched.

## Software backend

Three independent problems:

1. **Rasterisation.** Done. `SoftRaster` is an ARGB colour buffer, a float depth
   buffer, and clipped filled triangles. `SoftFrameDump` composes a complete
   320×240 scene (sky gradient, ground, three depth-tested triangles, one clipped
   at the frame edge) and writes it as a PNG. Both are tested headlessly.
2. **Device binding.** In 26.2 a backend returns a `GpuDevice` built from a
   `GpuDeviceBackend` (16 methods: surfaces, command encoders, samplers,
   textures, texture views, buffers, pipelines, queries, `DeviceInfo`). Plus
   `CommandEncoderBackend` and `RenderPassBackend`. Until those exist the
   frontend cannot accept the device, so this is the real milestone, not a
   formality.
3. **Presentation.** Not designed yet, and it is the part that decides whether
   this is useful. In 26.2 the game creates the window and the device exposes
   `createSurface(windowHandle)`; a CPU framebuffer still has to reach that
   window. Options are a platform-native blit (Win32 `StretchDIBits`, X11 SHM,
   Cocoa `CALayer`) using hints from `setWindowHints()`, or borrowing a GL
   context purely as a blit target. The first is real native work per OS; the
   second reuses the driver you were trying to avoid.

Until (2) and (3) exist, `SoftBackend.createDevice` throws
`BackendCreationException` on purpose, so the boot loop treats it as unavailable
and falls through. That is safer than a device that binds and then crashes on
the first draw.

## Optional dependency

DirectXMod (`gl4dx12`) is reached only by reflection on
`com.xgdt.dx12.dx12.Dx12Backend`. `fabric.mod.json` declares it under
`recommends`, so Fabric suggests it without ever gating launch. Nothing of
DirectXMod is bundled: it is Apache-2.0, but it carries Intel XESS and NVIDIA
DLSS binaries that are not, so redistribution is deliberately out of scope.
Polyglass links to upstream instead.

## Shaders

`ShaderSource.getShader(Identifier, ShaderType)` hands out GLSL text. The GL
backend compiles it directly; the Vulkan backend compiles to SPIR-V and its
pipelines consume SPIR-V modules. A software backend reading `ShaderSource`
therefore sees GLSL, which has to be interpreted (or compiled to SPIR-V first
and then interpreted). Either way it needs to understand every pipeline the
game compiles — start with `core/screenquad` and `position_tex_color`.

## Non-goals

Live API switching (impossible: the array is consumed before the window exists),
Metal (no source, restrictive licence), and VulkanMod (1.21.11, pre-26.x
architecture).
