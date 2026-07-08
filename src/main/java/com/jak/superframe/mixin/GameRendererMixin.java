package com.jak.superframe.mixin;

import com.jak.superframe.SuperFrame;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    // Before world render starts, switch to scaled target
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void superframe$preRenderLevel(float partialTick, long nanoTime, boolean renderBlockOutline, CallbackInfo ci) {
        SuperFrame sf = SuperFrame.getInstance();
        if (sf == null) return;
        if (SuperFrame.getConfig().enabled) {
            sf.setShouldScale(true);
        }
    }

    // After world render, switch back and upscale
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void superframe$postRenderLevel(float partialTick, long nanoTime, boolean renderBlockOutline, CallbackInfo ci) {
        SuperFrame sf = SuperFrame.getInstance();
        if (sf == null) return;
        sf.setShouldScale(false);
    }
}
