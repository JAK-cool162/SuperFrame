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
            // Light level changed – mark nearby lights dirty with ring-aware threshold
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
                wasSolid = oldState.isSolid();
                isSolid = newState.isSolid();
            }
        } catch (Throwable t) {
            wasSolid = oldState.isSolid();
            isSolid = newState.isSolid();
        }
        if (wasSolid != isSolid) {
            // Ring-aware dirty marking
            markDirtyInRange(pos, 16);
        }
        // else: no opacity change – still allow LightCacheManager to handle it, shadow cubemaps stay clean
    }

    /** Find all light sources within range of a block change, mark their cubemap dirty – RING AWARE */
    public static void markDirtyInRange(BlockPos changedPos, int range) {
        int count = 0;
        int rangeSq = range * range;
        int chunkRadius = (range >> 4) + 1;
        int cx = changedPos.getX() >> 4;
        int cz = changedPos.getZ() >> 4;

        synchronized (INDEX_LOCK) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                    long chunkKey = net.minecraft.world.level.ChunkPos.asLong(cx + dx, cz + dz);
                    List<LightSource> list = CHUNK_INDEX.get(chunkKey);
                    if (list == null) continue;
                    List<LightSource> copy;
                    synchronized (list) {
                        copy = new ArrayList<>(list);
                    }
                    for (LightSource ls : copy) {
                        if (ls.getPos().distSqr(changedPos) <= rangeSq) {
                            // Ring-aware: onNearbyBlockChange() increments counter and marks dirty only if threshold reached
                            // Ring 1: 1 change = dirty
                            // Ring 2: 2-3 changes
                            // Ring 3: 5 changes, low priority
                            // Ring 4: 20 changes, barely ever
                            if (ls.onNearbyBlockChange()) {
                                count++;
                            }
                        }
                    }
                }
            }
        }
        if (count > 0) {
            GlShaderClient.LOGGER.debug("Marked {} light cubemaps dirty near {} (ring-aware)", count, changedPos);
        }
    }

    /** Process dirty cubemaps – call once per client tick, max N per frame – CASCADED RINGS */
    public static void tick(Level level) {
        if (!GlShader.shouldRunShaders()) return;
        if (level == null) return;

        // Update ring assignments based on current player position
        // Do this lazily – only for lights we're about to consider
        net.minecraft.world.phys.Vec3 playerPos = com.jak.glshader.shadow.CascadedShadowManager.getPlayerPos();
        long currentTick = com.jak.glshader.shadow.CascadedShadowManager.getClientTick();

        // Collect dirty lights, update their ring, filter by update frequency
        List<LightSource> candidates = new ArrayList<>();
        for (LightSource ls : LIGHTS.values()) {
            if (ls.isRemoved()) continue;
            if (!ls.isDirty()) continue;

            // Update ring assignment (moves with player)
            ls.updateRing();

            // Ring frequency check – should we update this tick?
            if (!ls.shouldUpdateThisTick()) continue;

            candidates.add(ls);
        }

        // Sort by ring priority, then distance to player (closest first)
        candidates.sort((a, b) -> {
            int pa = com.jak.glshader.shadow.CascadedShadowManager.getUpdatePriority(a, a.getPos().distSqr(new net.minecraft.core.BlockPos((int)playerPos.x, (int)playerPos.y, (int)playerPos.z)));
            int pb = com.jak.glshader.shadow.CascadedShadowManager.getUpdatePriority(b, b.getPos().distSqr(new net.minecraft.core.BlockPos((int)playerPos.x, (int)playerPos.y, (int)playerPos.z)));
            return Integer.compare(pa, pb);
        });

        int budget = getMaxUpdatesPerFrame();
        int updated = 0;
        int[] ringCounts = new int[4];

        for (LightSource ls : candidates) {
            if (updated >= budget) break;
            try {
                // Ensure cubemap resolution matches current ring
                int baseRes = ls.getBaseCubemapResolution();
                int targetRes = com.jak.glshader.shadow.CascadedShadowManager.getResolutionForLight(ls, baseRes);
                if (ls.getShadowCubemap().getResolution() != targetRes) {
                    ls.getShadowCubemap().setResolution(targetRes);
                }

                ls.getShadowCubemap().rebuild(level);
                ls.setLastShadowUpdateTick(currentTick);
                ls.resetBlockChangeCounter();
                updated++;

                // Track ring stats
                var ring = ls.getCurrentRing();
                if (ring != null) ringCounts[ring.ordinal()]++;

            } catch (Exception e) {
                GlShaderClient.LOGGER.warn("Failed to rebuild shadow cubemap at {}: {}", ls.getPos(), e.toString());
            }
        }

        if (updated > 0) {
            GlShaderClient.LOGGER.debug("Rebuilt {} shadow cubemaps [R1={}, R2={}, R3={}, R4={}] / {} total lights",
                    updated, ringCounts[0], ringCounts[1], ringCounts[2], ringCounts[3], LIGHTS.size());
        }

        // Periodic cleanup of stale lights
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
