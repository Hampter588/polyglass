# Architecture

## The seam (verified against the 26.3 jar)

Minecraft 26.3 chooses its graphics backend through one method:

```java
// net.minecraft.client.PreferredGraphicsApi
public GpuBackend[] getBackendsToTry();
```

`PreferredGraphicsApi` is an enum: `DEFAULT`, `OPENGL`, `VULKAN`. The game calls
`getBackendsToTry()` once during boot, then walks the array:

```
for (GpuBackend backend : preferred.getBackendsToTry()) {
    backend.loadLibrary();
    long window = backend.createWindow(title, width, height, shareWith);
    GpuDevice device = backend.createDevice(debugOptions);   // throws BackendCreationException
    // success -> keep it; failure -> unloadLibrary(), next candidate
}
```

That is the whole integration surface. Polyglass is one `@Inject` on that
method's `RETURN`. It does not implement a renderer, a device lifecycle, a
shader compiler, or a window.

```
┌──────────────────────────────────────────────────────────┐
│ Minecraft 26.3                                           │
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
      plus, if the user installed it: com.xgdt.dx12.dx12.Dx12Backend
```

## 26.3 moved the GPU API

In 26.2 the interface lived at `com.mojang.blaze3d.systems.GpuBackend`. In 26.3
it moved to a separate library inside the game jar:

```
com.mojang.renderpearl.api.device.GpuBackend
com.mojang.renderpearl.api.device.GpuDevice
com.mojang.renderpearl.api.device.BackendCreationException   (checked)
com.mojang.renderpearl.backend.opengl.GlBackend
com.mojang.renderpearl.backend.vulkan.VulkanBackend
```

The interface also changed shape:

```java
public interface GpuBackend {
    String getName();
    void loadLibrary() throws BackendCreationException;
    void unloadLibrary();
    long createWindow(String title, int width, int height, long shareWith);
    GpuDevice createDevice(GpuDebugOptions options) throws BackendCreationException;
}
```

The backend now owns window creation. The 26.2 methods
`setWindowHints()` / `handleWindowCreationErrors(...)` / `createDevice(long window, …)`
are gone.

**Consequence:** DirectXMod, which targets 26.2, cannot run on 26.3 — its
`Dx12Backend` implements an interface that no longer exists, and its mixin
returns an array of a type that no longer exists. The `dx12` id is therefore
inert until that mod is ported (or Polyglass pins to 26.2).

## Shaders

`GpuDevice.compilePipeline(RenderPipeline, ShaderSource, Executor)` is async and
returns `CompletableFuture<CompiledRenderPipeline.Pending>`. `ShaderSource`
hands out GLSL text via `getShader(Identifier, ShaderType)`. The frontend
(`com.mojang.renderpearl.frontend.shaders.GlslCompiler`, `SPIRVModule`,
`PipelineBuilder`) does GLSL → SPIR-V, and backends consume SPIR-V
(`com.mojang.renderpearl.backend.api.SpvModule`).

This is the reason a software backend is not a weekend job: it has to either
reuse the SPIR-V front half and interpret/execute SPIR-V, or run the GLSL path
and interpret that. Either way it needs to understand every pipeline the game
compiles.

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

Two independent problems:

1. **Rasterisation.** `SoftRaster` is done: ARGB colour buffer, float depth
   buffer, clipped filled triangles, tested headlessly.
2. **Presentation.** Not designed yet, and it is the part that decides whether
   this is useful. A CPU framebuffer still has to reach a window. Options are a
   platform-native blit (Win32 `StretchDIBits`, X11 SHM, Cocoa `CALayer`) with
   its own window and input plumbing, or borrowing a GL context purely as a
   blit target. The first is real native work per OS; the second reuses the
   driver you were trying to avoid.

Until both exist, `SoftBackend.createDevice` throws `BackendCreationException`
on purpose, so the boot loop treats it as unavailable and falls through. That
is safer than a device that binds and then crashes on the first draw.

## Non-goals

Live API switching (impossible: the array is consumed before the window exists),
Metal (no source, restrictive licence), and VulkanMod (1.21.11, pre-26.x
architecture).
