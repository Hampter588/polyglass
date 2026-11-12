package dev.polyglass.api;

/**
 * @param vertexModule   compiled vertex module, backend-resolved
 * @param fragmentModule compiled fragment module, backend-resolved
 * @param meshModule     compiled mesh module, or {@code null} for the standard path
 * @param depthTest      depth testing enabled
 * @param depthWrite     depth writes enabled
 */
public record PipelineDesc(
        ShaderModule vertexModule,
        ShaderModule fragmentModule,
        ShaderModule meshModule,
        boolean depthTest,
        boolean depthWrite) {

    /** A shader module in whatever form the backend consumes (SPIR-V, DXIL, MSL, GLSL source). */
    public record ShaderModule(String name, byte[] bytecode) {
        public ShaderModule {
            if (bytecode == null || bytecode.length == 0) {
                throw new IllegalArgumentException("shader module '" + name + "' has no bytecode");
            }
        }
    }

    public static PipelineDesc standard(ShaderModule vertex, ShaderModule fragment) {
        return new PipelineDesc(vertex, fragment, null, true, true);
    }
}
