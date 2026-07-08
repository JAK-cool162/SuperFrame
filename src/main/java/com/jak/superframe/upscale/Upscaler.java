package com.jak.superframe.upscale;

import com.jak.superframe.SuperFrameClient;
import com.jak.superframe.config.SuperFrameConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.resources.ResourceLocation;

/**
 * Upscaling dispatcher.
 *
 * MC 1.21.1 path: uses vanilla blitToScreen (nearest/bilinear).
 * MC 1.21.5+ path: use RenderPipelines + FSR EASU/RCAS shaders.
 *
 * FSR shaders included in assets/superframe/shaders/core/
 * Original FSR by AMD, port by RenderScale (MIT).
 */
public class Upscaler {

    public static void blit(RenderTarget src, RenderTarget dst, SuperFrameConfig.UpscaleMode mode, boolean linear) {
        // 1.21.1 simple path
        src.blitToScreen(dst.width, dst.height);
        // For 1.21.5+, replace with:
        // if (mode == UpscaleMode.FSR1) {
        //   FsrUpscaler.easuRcas(src, dst);
        // } else {
        //   BlitHelper.blit(src, dst, linear ? FilterMode.LINEAR : FilterMode.NEAREST);
        // }
    }

    // Placeholder for 1.21.5+ RenderPipeline objects
    // public static final ResourceLocation FSR_EASU_ID = ResourceLocation.fromNamespaceAndPath(SuperFrameClient.MOD_ID, "pipeline/fsr_easu");
    // public static final ResourceLocation FSR_RCAS_ID = ResourceLocation.fromNamespaceAndPath(SuperFrameClient.MOD_ID, "pipeline/fsr_rcas");
}
