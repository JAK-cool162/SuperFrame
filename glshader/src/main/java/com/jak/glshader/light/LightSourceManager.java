package com.jak.glshader.light;

import com.jak.glshader.GlShader;
import com.jak.glshader.GlShaderClient;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PER LIGHT SOURCE SHADOW CUBEMAP MANAGER
 *
 * - Each BlockPos light source owns a 360° shadow cubemap
 * - Store as a depth map per light (6 faces)
 * - Cache it — only recalculate when a block changes within that light's range
 * - Register a block change listener via Mixin on LevelChunk.setBlockState()
 * - On change: find all light sources within range, mark their cubemap dirty
 * - Only dirty cubemaps get recalculated next frame
 * - Do NOT recalculate every frame
 */
public class LightSourceManager {
    private static final ConcurrentHashMap<BlockPos, LightSource> LIGHTS = new ConcurrentHashMap<>();
    // Chunk -> light sources index for fast spatial query
    private static final Long2ObjectOpenHashMap<List<LightSource>> CHUNK_INDEX = new Long2ObjectOpenHashMap<>();
    private static final Object INDEX_LOCK = new Object();

    private static int cubemapResolution = 32; // default, configurable
    private static int maxUpdatesPerFrame = 2;

    public static void init() {
        GlShaderClient.LOGGER.info("LightSourceManager init – shadow cubemaps enabled");
    }

    /** Register or update a light source */
    public static LightSource registerLight(BlockPos pos, int lightLevel) {
        BlockPos immutable = pos.immutable();
        return LIGHTS.compute(immutable, (p, existing) -> {
            if (existing != null) {
                existing.setLightLevel(lightLevel);
                existing.touch();
                return existing;
            }
            LightSource ls = new LightSource(immutable, lightLevel, getCubemapResolution());
            indexLightSource(ls);
            GlShaderClient.LOGGER.debug("Registered light source {} level {}", pos, lightLevel);
            return ls;
        });
    }

    public static void unregisterLight(BlockPos pos) {
        LightSource removed = LIGHTS.remove(pos);
        if (removed != null) {
            removed.setRemoved(true);
            unindexLightSource(removed);
            GlShaderClient.LOGGER.debug("Unregistered light source {}", pos);
        }
    }

    public static LightSource getLightSource(BlockPos pos) {
        return LIGHTS.get(pos);
    }

    /** Scan a chunk for light-emitting blocks and register them */
    public static void scanChunk(LevelChunk chunk) {
        if (!GlShader.shouldRunShaders()) return;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();

        int found = 0;
        // Simple scan – in production you'd use chunk light engine data or block entity list
        // For now, sparse scan every 2 blocks to keep cost low (full scan is expensive)
        for (int y = minY; y < maxY; y += 1) {
            for (int z = 0; z < 16; z += 2) {
                for (int x = 0; x < 16; x += 2) {
                    mpos.set(baseX + x, y, baseZ + z);
                    BlockState state;
                    try {
                        state = chunk.getBlockState(mpos);
                    } catch (Exception e) {
                        continue;
                    }
                    int emission = state.getLightEmission();
                    if (emission > 0) {
                        // Found light – refine to exact pos by checking neighbors
                        // For now just register the sampled pos
                        registerLight(mpos.immutable(), emission);
                        found++;
                    }
                }
            }
        }
        if (found > 0) {
            GlShaderClient.LOGGER.debug("Chunk {} scan found {} light sources (sparse)", chunk.getPos(), found);
        }
    }

    /** Called from LevelChunkMixin on block change */
    public static void onBlockChanged(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        int oldEmit = oldState.getLightEmission();
        int newEmit = newState.getLightEmission();

        // Light source added / removed / changed
        if (oldEmit != newEmit) {
            if (newEmit > 0) {
                registerLight(pos, newEmit);
            } else {
                unregisterLight(pos);
            }
            // Light level changed – mark nearby lights dirty (they may be occluded differently now)
            markDirtyInRange(pos, 16);
            return;
        }

        // Block opacity / collision changed – may affect shadows
        boolean wasSolid;
        boolean isSolid;
        try {
            if (level != null) {
                wasSolid = oldState.isSolidRender(level, pos);
                isSolid = newState.isSolidRender(level, pos);
            } else {
                // Fallback when Level is not available (mixin context)
                wasSolid = oldState.isSolid();
                isSolid = newState.isSolid();
            }
        } catch (Throwable t) {
            // Mapping differences between 1.20 / 1.21 – fall back to simple solid check
            wasSolid = oldState.isSolid();
            isSolid = newState.isSolid();
        }
        if (wasSolid != isSolid) {
            markDirtyInRange(pos, 16);
        } else {
            // Even if opacity didn't change, be conservative – a block change near a light can affect shadows
            // Only mark dirty if the block is within light range of any source – markDirtyInRange already does that
            // For now, do nothing extra – LightCacheManager already invalidated chunk light cache
        }
    }

    /** Find all light sources within range of a block change, mark their cubemap dirty */
    public static void markDirtyInRange(BlockPos changedPos, int range) {
        int count = 0;
        int rangeSq = range * range;
        // Iterate over nearby chunks for efficiency
        int chunkRadius = (range >> 4) + 1;
        int cx = changedPos.getX() >> 4;
        int cz = changedPos.getZ() >> 4;

        synchronized (INDEX_LOCK) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                    long chunkKey = net.minecraft.world.level.ChunkPos.asLong(cx + dx, cz + dz);
                    List<LightSource> list = CHUNK_INDEX.get(chunkKey);
                    if (list == null) continue;
                    // Copy to avoid CME
                    List<LightSource> copy;
                    synchronized (list) {
                        copy = new ArrayList<>(list);
                    }
                    for (LightSource ls : copy) {
                        if (ls.getPos().distSqr(changedPos) <= rangeSq) {
                            ls.markDirty();
                            count++;
                        }
                    }
                }
            }
        }
        if (count > 0) {
            GlShaderClient.LOGGER.debug("Marked {} light cubemaps dirty near {}", count, changedPos);
        }
    }

    /** Process dirty cubemaps – call once per client tick, max N per frame */
    public static void tick(Level level) {
        if (!GlShader.shouldRunShaders()) return;
        if (level == null) return;

        int budget = getMaxUpdatesPerFrame();
        int updated = 0;
        for (LightSource ls : LIGHTS.values()) {
            if (updated >= budget) break;
            if (ls.isDirty() && !ls.isRemoved()) {
                try {
                    ls.getShadowCubemap().rebuild(level);
                    updated++;
                } catch (Exception e) {
                    GlShaderClient.LOGGER.warn("Failed to rebuild shadow cubemap at {}: {}", ls.getPos(), e.toString());
                    // Mark clean to avoid spamming – will get dirty again on next block change
                    // Actually leave dirty = false already set in rebuild
                }
            }
        }
        if (updated > 0) {
            GlShaderClient.LOGGER.debug("Rebuilt {} shadow cubemaps this tick, {} total lights", updated, LIGHTS.size());
        }

        // Periodic cleanup of stale lights (not seen in 60s)
        long now = System.currentTimeMillis();
        LIGHTS.entrySet().removeIf(e -> {
            LightSource ls = e.getValue();
            if (now - ls.getLastSeenTime() > 60000 && !ls.isDirty()) {
                unindexLightSource(ls);
                return true;
            }
            return false;
        });
    }

    public static void markAllDirty() {
        for (LightSource ls : LIGHTS.values()) {
            ls.markDirty();
        }
        GlShaderClient.LOGGER.info("Marked all {} light cubemaps dirty", LIGHTS.size());
    }

    public static int getLightCount() {
        return LIGHTS.size();
    }

    public static List<LightSource> getAllLights() {
        return new ArrayList<>(LIGHTS.values());
    }

    // --- chunk spatial index ---

    private static void indexLightSource(LightSource ls) {
        BlockPos p = ls.getPos();
        long chunkKey = net.minecraft.world.level.ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4);
        synchronized (INDEX_LOCK) {
            List<LightSource> list = CHUNK_INDEX.computeIfAbsent(chunkKey, k -> new ArrayList<>());
            synchronized (list) {
                if (!list.contains(ls)) list.add(ls);
            }
        }
    }

    private static void unindexLightSource(LightSource ls) {
        BlockPos p = ls.getPos();
        long chunkKey = net.minecraft.world.level.ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4);
        synchronized (INDEX_LOCK) {
            List<LightSource> list = CHUNK_INDEX.get(chunkKey);
            if (list != null) {
                synchronized (list) {
                    list.remove(ls);
                    if (list.isEmpty()) CHUNK_INDEX.remove(chunkKey);
                }
            }
        }
    }

    // Config getters
    private static int getCubemapResolution() {
        try {
            return Math.max(8, Math.min(128, GlShader.getConfig().shadows.cubemapResolution));
        } catch (Exception e) {
            return cubemapResolution;
        }
    }

    private static int getMaxUpdatesPerFrame() {
        try {
            return Math.max(1, GlShader.getConfig().shadows.maxUpdatesPerFrame);
        } catch (Exception e) {
            return maxUpdatesPerFrame;
        }
    }
}
