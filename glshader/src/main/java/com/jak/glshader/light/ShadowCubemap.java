package com.jak.glshader.light;

import com.jak.glshader.GlShader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * PER LIGHT SOURCE SHADOW CUBEMAP – Step 2
 *
 * Each BlockPos light source owns a 360° shadow cubemap.
 * Stored as a depth map per light (6 faces).
 * Cached, only recalculates when block changes within light range.
 * NOT recalculated every frame.
 *
 * Initial implementation: CPU raymarch, low-res (configurable).
 * Future: GPU render-to-cubemap depth pass.
 */
public class ShadowCubemap {
    public static final int FACE_COUNT = 6;
    // Face order: +X, -X, +Y, -Y, +Z, -Z
    public static final int POS_X = 0, NEG_X = 1, POS_Y = 2, NEG_Y = 3, POS_Z = 4, NEG_Z = 5;

    private final BlockPos lightPos;
    private int resolution;
    private byte[][] faces; // [face][y*res + x] – occlusion distance 0-15
    private boolean dirty = true;
    private long lastBuildTime = 0;
    private int buildCount = 0;

    public ShadowCubemap(BlockPos lightPos, int resolution) {
        this.lightPos = lightPos.immutable();
        this.resolution = Math.max(8, Math.min(128, resolution));
        this.faces = new byte[FACE_COUNT][this.resolution * this.resolution];
        markDirty();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public BlockPos getLightPos() {
        return lightPos;
    }

    /**
     * Rebuild cubemap – only called when dirty.
     * CPU raymarch version – cheap, works everywhere (ARM64/x86_64).
     */
    public void rebuild(Level level) {
        if (!dirty) return;
        if (level == null) return;

        long start = System.nanoTime();

        int res = this.resolution;
        // For each cubemap face, cast rays
        for (int face = 0; face < FACE_COUNT; face++) {
            byte[] depth = faces[face];
            for (int y = 0; y < res; y++) {
                for (int x = 0; x < res; x++) {
                    // Map pixel to direction vector on cube face
                    float[] dir = ShadowUtils.cubemapPixelToDirection(face, x, y, res);
                    int occlusion = raymarchOcclusion(level, lightPos, dir[0], dir[1], dir[2], 15);
                    depth[y * res + x] = (byte) occlusion;
                }
            }
        }

        dirty = false;
        lastBuildTime = System.currentTimeMillis();
        buildCount++;
        long elapsedUs = (System.nanoTime() - start) / 1000;
        // Debug: uncomment to log slow builds
        // com.jak.glshader.GlShaderClient.LOGGER.debug("ShadowCubemap rebuilt {} in {} µs", lightPos, elapsedUs);
    }

    /**
     * Raymarch from lightPos outward, return first solid hit distance (0-15)
     * 15 = no occlusion within range
     */
    private int raymarchOcclusion(Level level, BlockPos origin, float dx, float dy, float dz, int maxDist) {
        double ox = origin.getX() + 0.5;
        double oy = origin.getY() + 0.5;
        double oz = origin.getZ() + 0.5;

        // Normalize direction
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-4f) return maxDist;
        dx /= len; dy /= len; dz /= len;

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        // Step in 0.5 block increments for decent accuracy / low cost
        for (int step = 1; step <= maxDist * 2; step++) {
            double dist = step * 0.5;
            int bx = (int) Math.floor(ox + dx * dist);
            int by = (int) Math.floor(oy + dy * dist);
            int bz = (int) Math.floor(oz + dz * dist);
            mpos.set(bx, by, bz);
            if (mpos.equals(origin)) continue;

            BlockState state;
            try {
                state = level.getBlockState(mpos);
            } catch (Exception e) {
                // Chunk not loaded – treat as no occlusion
                continue;
            }
            // Solid render blocks occlude light
            // Use isSolidRender / isOpaque – Yarn 1.21.11: isSolid()
            boolean occludes = false;
            try {
                occludes = state.isSolidRender(level, mpos);
            } catch (Throwable t) {
                // Fallback for mappings difference
                occludes = state.isSolid();
            }
            if (occludes) {
                return Math.min(15, (int) Math.ceil(dist));
            }
        }
        return maxDist;
    }

    /**
     * Sample shadow factor for a world position relative to this light.
     * Returns 0.0 = fully shadowed, 1.0 = fully lit
     */
    public float sampleShadow(net.minecraft.world.phys.Vec3 worldPos) {
        if (dirty) return 1.0f; // if not built yet, assume lit

        double lx = lightPos.getX() + 0.5;
        double ly = lightPos.getY() + 0.5;
        double lz = lightPos.getZ() + 0.5;

        double dx = worldPos.x - lx;
        double dy = worldPos.y - ly;
        double dz = worldPos.z - lz;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (dist < 0.1) return 1.0f;

        // Find cubemap face
        double adx = Math.abs(dx), ady = Math.abs(dy), adz = Math.abs(dz);
        int face;
        float u, v;
        if (adx >= ady && adx >= adz) {
            face = dx > 0 ? POS_X : NEG_X;
            float sc = (float)(1.0 / adx);
            u = (float)((dz * sc + 1.0) * 0.5);
            v = (float)((-dy * sc + 1.0) * 0.5);
            if (face == NEG_X) u = 1.0f - u;
        } else if (ady >= adx && ady >= adz) {
            face = dy > 0 ? POS_Y : NEG_Y;
            float sc = (float)(1.0 / ady);
            u = (float)((dx * sc + 1.0) * 0.5);
            v = (float)((dz * sc + 1.0) * 0.5);
            if (face == NEG_Y) v = 1.0f - v;
        } else {
            face = dz > 0 ? POS_Z : NEG_Z;
            float sc = (float)(1.0 / adz);
            u = (float)((dx * sc + 1.0) * 0.5);
            v = (float)((-dy * sc + 1.0) * 0.5);
            if (face == NEG_Z) u = 1.0f - u;
        }

        int res = this.resolution;
        int ix = Math.max(0, Math.min(res - 1, (int)(u * res)));
        int iy = Math.max(0, Math.min(res - 1, (int)(v * res)));
        byte[] depthMap = faces[face];
        int occlusionDist = depthMap[iy * res + ix] & 0xFF;

        // If stored occlusion distance < actual distance → in shadow
        if (occlusionDist < dist - 0.5) {
            return 0.2f; // shadowed – keep some ambient
        }
        return 1.0f;
    }

    public int getResolution() {
        return resolution;
    }

    public void setResolution(int resolution) {
        if (this.resolution != resolution) {
            this.resolution = resolution;
            this.faces = new byte[FACE_COUNT][resolution * resolution];
            markDirty();
        }
    }

    public long getLastBuildTime() {
        return lastBuildTime;
    }

    public int getBuildCount() {
        return buildCount;
    }
}
