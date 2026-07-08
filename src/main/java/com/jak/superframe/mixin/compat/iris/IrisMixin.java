package com.jak.superframe.mixin.compat.iris;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * Iris / Oculus compatibility mixin.
 * Pseudo = true so the mixin only applies if Iris is present.
 *
 * Here you can adjust Iris shadow map resolution to match SuperFrame scale,
 * preventing mismatched framebuffer sizes.
 *
 * Left as a no-op stub – SuperFrame works with Iris out of the box.
 * Add targeted injections here if you hit shader pack issues.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.WorldRenderingPipeline", remap = false)
public class IrisMixin {
    // no-op
}
