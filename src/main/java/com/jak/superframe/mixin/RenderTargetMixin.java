package com.jak.superframe.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;

/**
 * RenderTarget hooks.
 * For MC 1.21.1 + Sodium: you can force texture filter mode here if needed.
 * Left empty for now – SuperFrame handles scaling at GameRenderer level.
 */
@Mixin(RenderTarget.class)
public class RenderTargetMixin {
    // intentionally empty – placeholder for Sodium/Iris compat
}

