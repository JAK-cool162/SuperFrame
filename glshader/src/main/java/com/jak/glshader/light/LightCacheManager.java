package com.jak.glshader.light;

import com.jak.glshader.GlShaderClient;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.ChunkPos;

/**
 * CHUNK LIGHT CACHE SYSTEM – Step 1
 *
 * - Access LevelChunk data via Mixin
 * - Pre-calculate light zones per chunk
 * - Store as lightweight lightmap per chunk
 * - Invalidate cache only on block change event
 */
public class LightCacheManager {
    private static final Long2ObjectOpenHashMap<ChunkLightCache> CACHE = new Long2ObjectOpenHashMap<>();
    private static final Object LOCK = new Object();

    public static void init() {
        GlShaderClient.LOGGER.info("LightCacheManager init");
        // Fabric chunk load/unload events could be hooked here
        // ClientChunkEvents.CHUNK_LOAD / CHUNK_UNLOAD
    }

    public static ChunkLightCache getOrCreate(ChunkPos pos, net.minecraft.world.level.chunk.LevelChunk chunk) {
        long key = pos.toLong();
        synchronized (LOCK) {
            ChunkLightCache cache = CACHE.get(key);
            if (cache == null || cache.isInvalid()) {
                cache = new ChunkLightCache(pos, chunk);
                CACHE.put(key, cache);
                cache.rebuild();
            }
            return cache;
        }
    }

    public static ChunkLightCache get(ChunkPos pos) {
        synchronized (LOCK) {
            return CACHE.get(pos.toLong());
        }
    }

    public static void invalidate(ChunkPos pos) {
        synchronized (LOCK) {
            ChunkLightCache c = CACHE.remove(pos.toLong());
            if (c != null) c.markInvalid();
        }
        GlShaderClient.LOGGER.debug("Invalidated light cache {}", pos);
    }

    public static void invalidateAll() {
        synchronized (LOCK) {
            CACHE.values().forEach(ChunkLightCache::markInvalid);
            CACHE.clear();
        }
        GlShaderClient.LOGGER.info("Light cache cleared ({} chunks)", CACHE.size());
    }

    /** Called from LevelChunkMixin on block change */
    public static void onBlockChanged(net.minecraft.core.BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        invalidate(cp);
        // Also invalidate neighbors if light crosses chunk borders
        invalidate(new ChunkPos(cp.x - 1, cp.z));
        invalidate(new ChunkPos(cp.x + 1, cp.z));
        invalidate(new ChunkPos(cp.x, cp.z - 1));
        invalidate(new ChunkPos(cp.x, cp.z + 1));
    }
}
