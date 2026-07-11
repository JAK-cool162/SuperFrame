package com.jak.glshader.light;

import com.jak.glshader.shadow.CascadedShadowManager;
import com.jak.glshader.shadow.ShadowRing;
import net.minecraft.core.BlockPos;

/**
 * Represents a block light source with an owned shadow cubemap
 *
 * Step 3: Cascaded Shadow Rings – tracks ring, update frequency, block change counter
 */
public class LightSource {
    private final BlockPos pos;
    private int lightLevel;
    private final ShadowCubemap shadowCubemap;
    private long lastSeenTime;
    private boolean removed = false;

    // --- Cascaded Shadow Rings state ---
    private ShadowRing currentRing = ShadowRing.RING_1;
    private int blockChangeCounter = 0;
    private long lastShadowUpdateTick = 0;
    private int baseCubemapResolution;

    public LightSource(BlockPos pos, int lightLevel, int cubemapResolution) {
        this.pos = pos.immutable();
        this.lightLevel = lightLevel;
        this.baseCubemapResolution = cubemapResolution;
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

    // ===== Cascaded Shadow Rings API =====

    public ShadowRing getCurrentRing() {
        return currentRing;
    }

    /** Update ring assignment based on player position – returns true if ring changed */
    public boolean updateRing() {
        ShadowRing newRing = CascadedShadowManager.getRingForPos(pos);
        if (newRing != currentRing) {
            currentRing = newRing;
            // Resolution changed – update cubemap
            int newRes = CascadedShadowManager.getResolutionForLight(this, baseCubemapResolution);
            if (newRes != shadowCubemap.getResolution()) {
                shadowCubemap.setResolution(newRes); // marks dirty automatically
            }
            return true;
        }
        return false;
    }

    /** Called when a nearby block changes – increments counter, returns true if should mark dirty per ring threshold */
    public boolean onNearbyBlockChange() {
        blockChangeCounter++;
        boolean shouldMark = CascadedShadowManager.shouldMarkDirtyOnBlockChange(this, blockChangeCounter);
        if (shouldMark) {
            blockChangeCounter = 0; // reset after marking
            markDirty();
            return true;
        }
        return false;
    }

    public void resetBlockChangeCounter() {
        blockChangeCounter = 0;
    }

    public int getBlockChangeCounter() {
        return blockChangeCounter;
    }

    public long getLastShadowUpdateTick() {
        return lastShadowUpdateTick;
    }

    public void setLastShadowUpdateTick(long tick) {
        this.lastShadowUpdateTick = tick;
    }

    /** Should this light be updated this tick? Ring frequency check */
    public boolean shouldUpdateThisTick() {
        if (!isDirty()) return false;
        return CascadedShadowManager.shouldUpdateThisTick(this);
    }

    public int getBaseCubemapResolution() {
        return baseCubemapResolution;
    }

    public void setBaseCubemapResolution(int baseRes) {
        this.baseCubemapResolution = baseRes;
        // Re-evaluate actual resolution for current ring
        updateRing();
    }
}
