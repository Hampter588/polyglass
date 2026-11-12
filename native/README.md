# Native shims

Direct3D 12 and Metal have no maintained Java bindings, so those backends call a
small native library through the **Foreign Function & Memory API** (final in
Java 22, so plain JDK 25 — no JNI, no `javac -h`).

An alternative for Metal is MoltenVK, which lets the Vulkan backend run on
macOS. Keep that as the comparison path; the shim is the primary one.

## Contract

Each shim is a flat C ABI. No C++ objects, no exceptions across the boundary,
no callbacks into Java except where explicitly stated. Handles are opaque
`uint64_t`.

```c
// polyglass_d3d12.h / polyglass_metal.h

uint64_t pg_create_device(uint64_t native_window, int width, int height);
void     pg_destroy_device(uint64_t device);

// Capabilities are filled into a caller-owned struct, not returned by pointer.
typedef struct {
    char     device_name[128];
    char     driver_info[128];
    uint32_t max_texture_size;
    uint32_t max_frames_in_flight;
    uint8_t  compute;
    uint8_t  mesh_shaders;
    uint8_t  bindless;
    uint8_t  timestamp_queries;
} pg_capabilities;

int  pg_get_capabilities(uint64_t device, pg_capabilities* out);

uint64_t pg_create_buffer(uint64_t device, uint64_t size, uint32_t usage);
uint64_t pg_map_buffer(uint64_t device, uint64_t buffer);
void     pg_unmap_buffer(uint64_t device, uint64_t buffer);
void     pg_destroy_buffer(uint64_t device, uint64_t buffer);

uint64_t pg_create_texture(uint64_t device, uint32_t w, uint32_t h,
                           uint32_t mips, uint32_t format, uint32_t usage);
void     pg_destroy_texture(uint64_t device, uint64_t texture);

uint64_t pg_create_pipeline(uint64_t device, const uint8_t* vs, size_t vs_len,
                            const uint8_t* fs, size_t fs_len);
void     pg_destroy_pipeline(uint64_t device, uint64_t pipeline);

uint64_t pg_begin_frame(uint64_t device);
void     pg_draw(uint64_t device, uint64_t cmd, uint64_t pipeline,
                 uint32_t vertex_count, uint32_t instance_count,
                 uint32_t first_vertex, uint32_t first_instance);
void     pg_end_frame(uint64_t device);   // submit + present

void     pg_wait_idle(uint64_t device);
const char* pg_last_error(void);          // thread-local, valid until next call
```

Rules:

- Every `pg_*` function returns `0` / non-zero on failure and never throws.
- `pg_last_error()` is thread-local and copied into a Java string immediately.
- Buffers are caller-owned; the shim never frees what it did not create.
- Structs are `#pragma pack(1)` with fixed-width types. `memoryLayout` on the
  Java side mirrors the C header exactly; a mismatch is a silent corruption bug,
  so the two are generated from one source of truth (a `.h` parsed by jextract or
  hand-checked in a test).

## Java side

```java
Linker linker = Linker.nativeLinker();
SymbolLookup lib = SymbolLookup.libraryLookup("polyglass_d3d12", Arena.global());

MethodHandle createDevice = linker.downcallHandle(
        lib.find("pg_create_device").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
```

Handles live in `Arena.ofShared()`; every `pg_destroy_*` is paired in the Java
`AutoCloseable` wrapper.

FFM has been final since Java 22 (JEP 454), and LWJGL 3.4.0+ itself runs in FFM
mode on JDK 25, so the shim approach does not add a second native-access stack
to the project.

## Building

Not wired into Gradle yet. Plan is a `native/CMakeLists.txt` per shim, with the
resulting library dropped into `src/main/resources/natives/<os>-<arch>/` and
unpacked to a temp directory at runtime (same pattern as LWJGL natives).

- Windows: MSVC or `clang-cl`, `d3d12.lib`, `dxgi.lib`
- macOS: `clang -framework Metal -framework QuartzCore -framework Cocoa`

Both need a GPU-agnostic fallback: if the shim fails to load, the backend
reports unavailable and selection falls through (already implemented).
