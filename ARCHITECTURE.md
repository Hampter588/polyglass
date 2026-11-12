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
┌───────▼────────┐ ┌─────────────▼──────┐
│ GlBackend      │ │ VulkanBackend      │
│ (vanilla)      │ │ (vanilla)          │
└────────────────┘ └────────────────────┘
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
version-agnostic, and moving to 26.3 later is an import sweep in
`BackendCatalog` and the mixin.

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

DirectXMod injects at the same `RETURN` with `cancellable = true` and
**replaces** the array with a hard-coded `{dx12, gl, vulkan}`. Two injectors that
both call `setReturnValue` fight, and the one applied last wins.

Two things stop that from mattering:

1. `@Mixin(..., priority = 500)` on our handler. Lower priority is applied
   earlier, and the first `RETURN` handler to call `setReturnValue` wins —
   Mixin guards every later handler with an `isCancelled()` check, so the rest
   never run. Applying first therefore makes Polyglass the decider. Both mods
   sit at the default 1000 without the explicit value, and DirectXMod was
   observed winning that race: Polyglass's log line never appeared and the
   configured order was ignored.
2. `BackendCatalog` **merges**: it reads the array it is given, reuses the
   instances already in it, adds the ones it can construct (including
   DirectXMod's `Dx12Backend`, by reflection), and only then reorders. Unknown
   backends from other mods are preserved at the end.

Because of (1) DirectXMod's handler is the one that gets skipped, but nothing is
lost: its backend class is still on the classpath, so `dx12` is constructed and
offered exactly as if its mixin had run.



## Selection policy

`BackendPlan.resolve(requested, order, available)` is pure `java.util` and unit
tested:

1. explicit `backend` (if usable),
2. each id in `order` (if usable),
3. any remaining usable id, so a newly added backend is never dead on arrival.

Unavailable ids are dropped, never fatal. If the plan comes out empty, the
original vanilla array is returned untouched.

## No software backend

A CPU backend is not a menu entry — it is a renderer. On 26.2 it would need a
`GpuDeviceBackend` (16 methods: surfaces, command encoders, samplers, textures,
texture views, buffers, pipeline compilation, queries, `DeviceInfo`),
`CommandEncoderBackend`, `RenderPassBackend`, a GLSL/SPIR-V interpreter for every
pipeline the game compiles, and a per-OS blit into the window
(`StretchDIBits` / X11 SHM / `CALayer`) with input forwarding.

None of that is a selector's job, so it is not in this project. Shipping a
`SoftBackend` that throws in `createDevice` was tested and removed: an entry
that can never be chosen is dead weight in the menu, and the fallback chain
already works without it.

## Optional dependency

DirectXMod (`gl4dx12`) is reached only by reflection on
`com.xgdt.dx12.dx12.Dx12Backend`. `fabric.mod.json` declares it under
`recommends`, so Fabric suggests it without ever gating launch. Nothing of
DirectXMod is bundled: it is Apache-2.0, but it carries Intel XESS and NVIDIA
DLSS binaries that are not, so redistribution is deliberately out of scope.
Polyglass links to upstream instead.

## Shaders

Not Polyglass's business: `ShaderSource.getShader(Identifier, ShaderType)` hands
out GLSL text and whichever backend the game picked consumes it. The selector
never touches shaders.

## Non-goals

Live API switching (impossible: the array is consumed before the window exists),
Metal (no source, restrictive licence), VulkanMod (1.21.11, pre-26.x
architecture), and any CPU/software renderer (a separate project; see above).
