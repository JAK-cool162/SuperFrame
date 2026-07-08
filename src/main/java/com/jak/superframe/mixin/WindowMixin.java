package com.jak.superframe.mixin;

import com.jak.superframe.SuperFrame;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "onFramebufferResize", at = @At("TAIL"), require = 0)
    private void superframe$onFramebufferResize(long window, int framebufferWidth, int framebufferHeight, CallbackInfo ci) {
        if (SuperFrame.getInstance() != null) {
            SuperFrame.getInstance().onResolutionChanged();
        }
    }

    // MC 1.21.1: onResize, 1.21.5+: refreshFramebufferSize – try both, non-required
    @Inject(method = "onResize", at = @At("TAIL"), require = 0, remap = false)
    private void superframe$onResize(long window, int width, int height, CallbackInfo ci) {
        if (SuperFrame.getInstance() != null) {
            SuperFrame.getInstance().onResolutionChanged();
        }
    }

    @Inject(method = "refreshFramebufferSize", at = @At("TAIL"), require = 0)
    private void superframe$refreshFramebufferSize(CallbackInfo ci) {
        if (SuperFrame.getInstance() != null) {
            SuperFrame.getInstance().onResolutionChanged();
        }
    }
}


