package com.jak.glshader.light;

import net.minecraft.core.BlockPos;

/**
 * Represents a block light source with an owned shadow cubemap
 */
public class LightSource {
    private final BlockPos pos;
    private int lightLevel;
    private final ShadowCubemap shadowCubemap;
    private long lastSeenTime;
    private boolean removed = false;

    public LightSource(BlockPos pos, int lightLevel, int cubemapResolution) {
        this.pos = pos.immutable();
        this.lightLevel = lightLevel;
        this.shadowCubemap = new ShadowCubemap(this.pos, cubemapResolution);
        this.lastSeenTime = System.currentTimeMillis();
        // New light sources start dirty – need first build
        this.shadowCubemap.markDirty();
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public void setLightLevel(int lightLevel) {
        if (this.lightLevel != lightLevel) {
            this.lightLevel = lightLevel;
            markDirty();
        }
    }

    public ShadowCubemap getShadowCubemap() {
        return shadowCubemap;
    }

    public void markDirty() {
        shadowCubemap.markDirty();
    }

    public boolean isDirty() {
        return shadowCubemap.isDirty();
    }

    public void touch() {
        lastSeenTime = System.currentTimeMillis();
    }

    public long getLastSeenTime() {
        return lastSeenTime;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    /** Distance squared to a block pos – for range queries */
    public double distanceSq(BlockPos other) {
        return pos.distSqr(other);
    }

    /** Is the given block pos within this light's effective range? */
    public boolean affects(BlockPos blockPos) {
        // Light level = max distance, simple check
        int r = Math.max(1, lightLevel);
        return Math.abs(pos.getX() - blockPos.getX()) <= r &&
               Math.abs(pos.getY() - blockPos.getY()) <= r &&
               Math.abs(pos.getZ() - blockPos.getZ()) <= r &&
               pos.distSqr(blockPos) <= (double) r * r;
    }
}
