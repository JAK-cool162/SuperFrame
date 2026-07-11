package com.jak.glshader.shadow;

/**
 * CASCADED SHADOW RINGS – Step 3
 *
 * Divide world around player into 4 rings with decreasing resolution / update frequency
 */
public enum ShadowRing {
    RING_1(0, 16, 1.0f, 1, 1, "Close"),
    RING_2(16, 48, 0.5f, 3, 2, "Mid"),
    RING_3(48, 96, 0.25f, 10, 5, "Far"),
    RING_4(96, Integer.MAX_VALUE, 0.1f, 60, 20, "Distant");

    private final int minDistance;
    private final int maxDistance;
    private final float resolutionScale;
    private final int updateIntervalTicks;
    private final int blockChangeThreshold;
    private final String displayName;

    ShadowRing(int minDistance, int maxDistance, float resolutionScale, int updateIntervalTicks, int blockChangeThreshold, String displayName) {
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.resolutionScale = resolutionScale;
        this.updateIntervalTicks = updateIntervalTicks;
        this.blockChangeThreshold = blockChangeThreshold;
        this.displayName = displayName;
    }

    public int getMinDistance() { return minDistance; }
    public int getMaxDistance() { return maxDistance; }
    public float getResolutionScale() { return resolutionScale; }
    public int getUpdateIntervalTicks() { return updateIntervalTicks; }
    public int getBlockChangeThreshold() { return blockChangeThreshold; }
    public String getDisplayName() { return displayName; }

    public boolean contains(double distance) {
        return distance >= minDistance && distance < maxDistance;
    }

    public static ShadowRing fromDistance(double distance) {
        for (ShadowRing ring : values()) {
            if (ring.contains(distance)) return ring;
        }
        return RING_4;
    }

    /** Get cubemap resolution for this ring, given base resolution */
    public int getScaledResolution(int baseResolution) {
        if (this == RING_4) {
            // Chunk blob only – minimal 8x8
            return 8;
        }
        int scaled = Math.max(8, Math.round(baseResolution * resolutionScale));
        // Round down to power-of-two friendly size
        return scaled;
    }

    /** Priority for update scheduler – lower = higher priority */
    public int getUpdatePriority() {
        return this.ordinal();
    }
}
