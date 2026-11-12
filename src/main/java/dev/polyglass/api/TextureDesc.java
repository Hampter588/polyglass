package dev.polyglass.api;

/**
 * @param width     width in texels
 * @param height    height in texels
 * @param mipLevels mip chain length, at least 1
 * @param format    backend-neutral format id, resolved by the backend
 * @param usage     bitmask of the {@code USAGE_*} constants
 */
public record TextureDesc(int width, int height, int mipLevels, int format, int usage) {

    public static final int USAGE_SAMPLED = 1;
    public static final int USAGE_RENDER_TARGET = 1 << 1;
    public static final int USAGE_DEPTH_STENCIL = 1 << 2;
    public static final int USAGE_TRANSFER_SRC = 1 << 3;
    public static final int USAGE_TRANSFER_DST = 1 << 4;

    public TextureDesc {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("texture size must be positive: " + width + "x" + height);
        }
        if (mipLevels < 1) {
            throw new IllegalArgumentException("mipLevels must be >= 1: " + mipLevels);
        }
    }
}
