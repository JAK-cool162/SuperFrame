package com.jak.glshader.shadow;

import com.jak.glshader.GlShader;
import com.jak.glshader.GlShaderClient;
import com.jak.glshader.light.LightSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * CASCADED SHADOW RINGS – Update frequency scheduler
 *
 * Rings move with player position.
 * Farther rings update LESS frequently than closer rings.
 */
public class CascadedShadowManager {
    private static Vec3 playerPos = Vec3.ZERO;
    private static long clientTick = 0;

    // Configurable ring boundaries – defaults match ShadowRing enum, can be overridden via GlShaderConfig
    public static int getRing1Radius() {
        try { return GlShader.getConfig().shadows.cascaded.ring1Radius; } catch (Exception e) { return 16; }
    }
    public static int getRing2Radius() {
        try { return GlShader.getConfig().shadows.cascaded.ring2Radius; } catch (Exception e) { return 48; }
    }
    public static int getRing3Radius() {
        try { return GlShader.getConfig().shadows.cascaded.ring3Radius; } catch (Exception e) { return 96; }
    }

    private static boolean isCascadedEnabled() {
        try { return GlShader.getConfig().shadows.cascaded.cascadedRings; } catch (Exception e) { return true; }
    }

    private static int getRingUpdateInterval(ShadowRing ring) {
        try {
            var c = GlShader.getConfig().shadows.cascaded;
            return switch (ring) {
                case RING_1 -> c.ring1UpdateInterval;
                case RING_2 -> c.ring2UpdateInterval;
                case RING_3 -> c.ring3UpdateInterval;
                case RING_4 -> c.ring4UpdateInterval;
            };
        } catch (Exception e) {
            return ring.getUpdateIntervalTicks();
        }
    }

    private static int getRingBlockChangeThreshold(ShadowRing ring) {
        try {
            var c = GlShader.getConfig().shadows.cascaded;
            return switch (ring) {
                case RING_1 -> c.ring1BlockChangeThreshold;
                case RING_2 -> c.ring2BlockChangeThreshold;
                case RING_3 -> c.ring3BlockChangeThreshold;
                case RING_4 -> c.ring4BlockChangeThreshold;
            };
        } catch (Exception e) {
            return ring.getBlockChangeThreshold();
        }
    }

    private static float getRingResolutionScale(ShadowRing ring) {
        try {
            var c = GlShader.getConfig().shadows.cascaded;
            return switch (ring) {
                case RING_1 -> 1.0f;
                case RING_2 -> c.ring2ResolutionScale;
                case RING_3 -> c.ring3ResolutionScale;
                case RING_4 -> c.ring4ResolutionScale;
            };
        } catch (Exception e) {
            return ring.getResolutionScale();
        }
    }

    public static void init() {
        GlShaderClient.LOGGER.info("CascadedShadowManager init – 4 rings");
    }

    /** Call each client tick – updates player position */
    public static void tick() {
        clientTick++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            playerPos = mc.player.position();
        } else if (mc.getCameraEntity() != null) {
            playerPos = mc.getCameraEntity().position();
        }
    }

    public static Vec3 getPlayerPos() {
        return playerPos;
    }

    public static long getClientTick() {
        return clientTick;
    }

    /** Determine which ring a light source is in, based on current player position */
    public static ShadowRing getRingForPos(BlockPos lightPos) {
        double dx = lightPos.getX() + 0.5 - playerPos.x;
        double dy = lightPos.getY() + 0.5 - playerPos.y;
        double dz = lightPos.getZ() + 0.5 - playerPos.z;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Use config-overridable radii if cascaded rings are enabled
        if (isCascadedEnabled()) {
            int r1 = getRing1Radius();
            int r2 = getRing2Radius();
            int r3 = getRing3Radius();
            if (dist < r1) return ShadowRing.RING_1;
            if (dist < r2) return ShadowRing.RING_2;
            if (dist < r3) return ShadowRing.RING_3;
            return ShadowRing.RING_4;
        }
        return ShadowRing.fromDistance(dist);
    }

    /** Should this light source be updated this tick? Based on ring frequency */
    public static boolean shouldUpdateThisTick(LightSource light) {
        ShadowRing ring = light.getCurrentRing();
        if (ring == null) ring = getRingForPos(light.getPos());

        long lastUpdate = light.getLastShadowUpdateTick();
        long interval = getRingUpdateInterval(ring);

        return (clientTick - lastUpdate) >= interval;
    }

    /** Should a block change mark this light dirty? Ring-aware threshold */
    public static boolean shouldMarkDirtyOnBlockChange(LightSource light, int accumulatedBlockChanges) {
        ShadowRing ring = light.getCurrentRing();
        if (ring == null) ring = getRingForPos(light.getPos());

        int threshold = getRingBlockChangeThreshold(ring);
        return accumulatedBlockChanges >= threshold;
    }

    /** Get scaled cubemap resolution for a light based on its ring */
    public static int getResolutionForLight(LightSource light, int baseResolution) {
        ShadowRing ring = light.getCurrentRing();
        if (ring == null) ring = getRingForPos(light.getPos());
        float scale = getRingResolutionScale(ring);
        // Compute scaled resolution, respecting minimum 8
        int scaled = Math.max(8, Math.round(baseResolution * scale));
        // Ring 4 = chunk blob only – force 8
        if (ring == ShadowRing.RING_4) scaled = 8;
        return scaled;
    }

    /** Priority score for update scheduler – lower = update sooner */
    public static int getUpdatePriority(LightSource light, double distanceToPlayerSq) {
        ShadowRing ring = light.getCurrentRing();
        if (ring == null) ring = ShadowRing.RING_4;
        // Priority = ringOrdinal * 10000 + distance
        // Ensures Ring1 always beats Ring2, etc., then closest first within ring
        return ring.getUpdatePriority() * 10000 + (int)Math.min(9999, Math.sqrt(distanceToPlayerSq));
    }
}
