package com.jak.glshader.light;

import com.jak.glshader.GlShader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Lightweight per-chunk lightmap.
 *
 * Pre-calculates light zones to avoid per-frame light lookups.
 * Zone size configurable (default 4 blocks).
 *
 * Stored as packed bytes: skyLight (4 bits) | blockLight (4 bits)
 */
public class ChunkLightCache {
    private final ChunkPos chunkPos;
    private final LevelChunk chunk;
    private volatile boolean invalid = false;
    private long lastBuildTime = 0;

    // Light zone grid – coarse lightmap
    // For a 16x16 chunk, zoneSize=4 => 4x4 zones per Y-section
    // We store per-section to support full world height
    private byte[][][] zoneLight; // [section][z][x] packed light

    private int zoneSize;
    private int zonesPerAxis;
    private int sectionCount;

    public ChunkLightCache(ChunkPos pos, LevelChunk chunk) {
        this.chunkPos = pos;
        this.chunk = chunk;
    }

    public void rebuild() {
        if (!GlShader.getConfig().lightCache.enabled) return;

        this.zoneSize = Math.max(1, GlShader.getConfig().lightCache.zoneSize);
        this.zonesPerAxis = 16 / zoneSize;
        if (16 % zoneSize != 0) zonesPerAxis = 16; // fallback

        int minSection = chunk.getMinSection();
        int maxSection = chunk.getMaxSection();
        this.sectionCount = maxSection - minSection;

        zoneLight = new byte[sectionCount][zonesPerAxis][zonesPerAxis];

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();

        // Pre-calculate light zones per chunk
        for (int s = 0; s < sectionCount; s++) {
            int sectionY = (minSection + s) * 16;
            for (int zx = 0; zx < zonesPerAxis; zx++) {
                for (int zz = 0; zz < zonesPerAxis; zz++) {
                    // Sample center of zone
                    int sampleX = baseX + zx * zoneSize + zoneSize / 2;
                    int sampleZ = baseZ + zz * zoneSize + zoneSize / 2;
                    int sampleY = sectionY + 8;

                    mpos.set(sampleX, sampleY, sampleZ);

                    int blockLight = chunk.getLightEmission(mpos);
                    // Alternative: chunk.getLightLayer(LightLayer.BLOCK).get(mpos) etc.
                    // For 1.21+: use chunk.getLightLevel(mpos)
                    int skyLight = 15; // simplified – real impl would query LightLayer.SKY

                    try {
                        skyLight = chunk.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(mpos);
                        blockLight = Math.max(blockLight, chunk.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(mpos));
                    } catch (Exception ignored) {}

                    byte packed = (byte) ((skyLight & 0xF) << 4 | (blockLight & 0xF));
                    zoneLight[s][zz][zx] = packed;
                }
            }
        }

        lastBuildTime = System.currentTimeMillis();
        invalid = false;
    }

    /** Get cached light for a world position – fast zone lookup */
    public int getLight(BlockPos pos) {
        if (zoneLight == null || invalid) return 15;
        int lx = pos.getX() & 15;
        int lz = pos.getZ() & 15;
        int ly = pos.getY();
        int section = chunk.getSectionIndex(ly);
        if (section < 0 || section >= sectionCount) return 15;
        int zx = Math.min(zonesPerAxis - 1, lx / zoneSize);
        int zz = Math.min(zonesPerAxis - 1, lz / zoneSize);
        byte packed = zoneLight[section][zz][zx];
        int sky = (packed >> 4) & 0xF;
        int block = packed & 0xF;
        return Math.max(sky, block);
    }

    public void markInvalid() {
        this.invalid = true;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    public long getLastBuildTime() {
        return lastBuildTime;
    }
}
