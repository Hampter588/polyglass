package dev.polyglass.backend;

import dev.polyglass.api.BackendException;
import dev.polyglass.api.Device;
import dev.polyglass.api.DeviceConfig;
import dev.polyglass.api.GraphicsApi;
import dev.polyglass.api.GraphicsBackend;

/** Shared plumbing for the four backends. */
public abstract class AbstractBackend implements GraphicsBackend {

    private final String id;
    private final GraphicsApi api;
    private final int priority;

    protected AbstractBackend(String id, GraphicsApi api, int priority) {
        this.id = id;
        this.api = api;
        this.priority = priority;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public GraphicsApi api() {
        return api;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public Device createDevice(DeviceConfig config) {
        throw new BackendException(id + ": device creation is not implemented yet (ROADMAP.md, milestone M1+)");
    }
}
