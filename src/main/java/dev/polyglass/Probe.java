package dev.polyglass;

import java.util.Locale;

/** Cheap platform and dependency probes used during backend selection. */
public final class Probe {

    private Probe() {
    }

    public static boolean isWindows() {
        return osName().contains("win");
    }

    public static boolean isMacOs() {
        String os = osName();
        return os.contains("mac") || os.contains("darwin");
    }

    public static boolean isLinux() {
        return osName().contains("linux");
    }

    /** {@code true} if the class is present on the classpath (bindings are loaded). */
    public static boolean hasClass(String className) {
        try {
            Class.forName(className, false, Probe.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** {@code true} if a JNI/FFM native library with this name can be loaded. */
    public static boolean hasNativeLibrary(String libraryName) {
        try {
            System.loadLibrary(libraryName);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Looks for a library file on {@code java.library.path} without loading it.
     * Accepts both the platform-mapped name ({@code EGL.dll}, {@code libEGL.so},
     * {@code libEGL.dylib}) and the common {@code lib}-prefixed Windows form
     * ({@code libEGL.dll}).
     */
    public static boolean hasLibraryOnPath(String baseName) {
        String mapped = System.mapLibraryName(baseName);
        String[] candidates = {mapped, "lib" + mapped};
        String path = System.getProperty("java.library.path", "");
        for (String dir : path.split(java.io.File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            for (String candidate : candidates) {
                try {
                    if (java.nio.file.Files.isRegularFile(java.nio.file.Path.of(dir, candidate))) {
                        return true;
                    }
                } catch (RuntimeException ignored) {
                    // Invalid path entry; keep looking.
                }
            }
        }
        return false;
    }

    private static String osName() {
        return System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
    }
}
