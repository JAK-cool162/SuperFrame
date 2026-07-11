package com.jak.glshader.mixin;

import com.jak.glshader.light.LightCacheManager;
import com.jak.glshader.light.LightSourceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CHUNK LIGHT CACHE + SHADOW CUBEMAP – Mixin access to LevelChunk
 *
 * - Pre-calculate light zones per chunk on load
 * - Invalidate cache only on block change event
 * - Track light sources, mark shadow cubemaps dirty on block change
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    // Level field – Yarn 1.21.11: private final Level level;
    @Shadow @Final private Level level;

    // Scan chunk for light sources after construction (runs on client chunk load)
    @Inject(method = "<init>", at = @At("TAIL"))
    private void glshader$onChunkInit(CallbackInfo ci) {
        LevelChunk self = (LevelChunk) (Object) this;
        try {
            LightSourceManager.scanChunk(self);
        } catch (Exception ignored) {}
    }

    /**
     * Hook block state changes – invalidate light cache + shadow cubemaps
     * LevelChunk.setBlockState is the central point for block updates
     */
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void glshader$onBlockChanged(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        BlockState old = cir.getReturnValue();
        if (old == null) return;

        // 1. Light cache invalidation
        LightCacheManager.onBlockChanged(pos);

        // 2. Shadow cubemap invalidation + light source tracking
        LightSourceManager.onBlockChanged(this.level, pos, old, state);
    }
}

