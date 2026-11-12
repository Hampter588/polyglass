package dev.polyglass.api;

/**
 * @param size        size in bytes
 * @param usage       bitmask of the {@code USAGE_*} constants
 * @param hostVisible memory can be mapped and written from the CPU
 */
public record BufferDesc(long size, int usage, boolean hostVisible) {

    public static final int USAGE_VERTEX = 1;
    public static final int USAGE_INDEX = 1 << 1;
    public static final int USAGE_UNIFORM = 1 << 2;
    public static final int USAGE_STORAGE = 1 << 3;
    public static final int USAGE_TRANSFER_SRC = 1 << 4;
    public static final int USAGE_TRANSFER_DST = 1 << 5;

    public BufferDesc {
        if (size <= 0) {
            throw new IllegalArgumentException("buffer size must be positive: " + size);
        }
    }
}
