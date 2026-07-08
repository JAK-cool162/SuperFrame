package com.jak.superframe.mixin;

import com.jak.superframe.SuperFrame;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "resize", at = @At("TAIL"))
    private void superframe$onResize(int width, int height, CallbackInfo ci) {
        if (SuperFrame.getInstance() != null) {
            SuperFrame.getInstance().onResolutionChanged();
        }
    }
}
