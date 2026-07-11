package com.jak.glshader.mixin;

import com.jak.glshader.light.LightCacheManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CHUNK LIGHT CACHE SYSTEM – Mixin access to LevelChunk
 *
 * - Pre-calculate light zones per chunk on load
 * - Invalidate cache only on block change event
 */
@Mixin(LevelChunk.class)
public class LevelChunkMixin {

    /**
     * Hook block state changes – invalidate light cache for this chunk
     * LevelChunk.setBlockState is the central point for block updates
     */
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void glshader$onBlockChanged(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        // Only invalidate if block light emission or opacity changed
        BlockState old = cir.getReturnValue();
        if (old != null) {
            // Simple heuristic – always invalidate for now (cheap)
            // TODO: compare old.getLightEmission() != state.getLightEmission()
            //       || old.isSolidRender() != state.isSolidRender()
            LightCacheManager.onBlockChanged(pos);
        }
    }

    // Optional: build cache when chunk is loaded
    // LevelChunk has no clear onLoad callback in 1.21 – use postProcess or just lazy-build on first get
}
