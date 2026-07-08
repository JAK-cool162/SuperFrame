package com.jak.superframe;

import com.jak.superframe.config.SuperFrameConfig;
import com.jak.superframe.framegen.FrameGenerator;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

/**
 * SuperFrame - Upscaling + Frame Generation
 * Based in part on RenderScale by Zelo101 (MIT)
 * https://github.com/Zolo101/RenderScale
 */
public class SuperFrame {
    private static Minecraft client = Minecraft.getInstance();

    public static final ConfigHolder<SuperFrameConfig> CONFIG = SuperFrameConfig.init();

    // Scaled render target (game world, no UI)
    @Nullable public RenderTarget scaledTarget;
    // Vanilla main target (native resolution)
    @Nullable public RenderTarget vanillaTarget;

    private boolean shouldScale = false;
    private static SuperFrame instance;

    private final FrameGenerator frameGenerator = new FrameGenerator();

    public static void init() {
        instance = new SuperFrame();
        SuperFrameClient.LOGGER.info("SuperFrame core initialized");
    }

    public static SuperFrame getInstance() {
        return instance;
    }

    public static SuperFrameConfig getConfig() {
        return CONFIG.getConfig();
    }

    public FrameGenerator getFrameGenerator() {
        return frameGenerator;
    }

    public void onResolutionChanged() {
        if (getWindow() == null) return;
        ProfilerFiller profiler = client.getProfiler();
        profiler.push("superframe_resize");
        resizeTargets();
        profiler.pop();
    }

    public void setShouldScale(boolean shouldScale) {
        ProfilerFiller profiler = client.getProfiler();
        profiler.push("superframe_scale_toggle");

        if (this.shouldScale == shouldScale && scaledTarget != null) {
            profiler.pop();
            return;
        }

        Window window = client.getWindow();
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();

        // compute scaled size
        float scale = getCurrentScaleFactor();
        int scaledWidth = Math.max(1, (int)(windowWidth * scale));
        int scaledHeight = Math.max(1, (int)(windowHeight * scale));

        if (scaledTarget == null) {
            scaledTarget = new MainTarget(scaledWidth, scaledHeight);
        }

        if (vanillaTarget == null) {
            vanillaTarget = client.getMainRenderTarget();
        }

        this.shouldScale = shouldScale;

        if (shouldScale) {
            client.mainRenderTarget = scaledTarget;
            scaledTarget.bindWrite(true);
        } else {
            client.mainRenderTarget = vanillaTarget;
            vanillaTarget.bindWrite(true);

            // Upscale back to native
            if (scaledTarget != null) {
                try {
                    upscaleToTarget(scaledTarget, vanillaTarget, windowWidth, windowHeight);
                } catch (Exception e) {
                    SuperFrameClient.LOGGER.error("Upscale blit failed", e);
                }
            }
        }
        profiler.pop();
    }

    private void upscaleToTarget(RenderTarget src, RenderTarget dst, int dstW, int dstH) {
        SuperFrameConfig cfg = getConfig();
        // For MC 1.21.1: simple blit
        // For MC 1.21.5+: use FSR pipeline (see upscaler)
        boolean linear = cfg.getUpscaleFilterLinear();
        src.blitToScreen(dstW, dstH);
        // NOTE: On 1.21.5+ you can replace this with:
        // SuperFrameUpscaler.blit(src, dst, cfg.upscaleMode, linear);
        // Included FSR shaders are in assets/superframe/shaders/
        // See Upscaler.java for 1.21.5+ pipeline code (commented).

        // FrameGen capture
        if (cfg.frameGenMode != SuperFrameConfig.FrameGenMode.OFF) {
            frameGenerator.capture(dst);
        }
    }

    public double getCurrentScaleFactor() {
        SuperFrameConfig cfg = getConfig();
        if (!cfg.enabled) return 1.0;
        return Math.max(0.01f, cfg.renderScale);
    }

    public boolean isScalingActive() {
        return shouldScale && getConfig().enabled;
    }

    @Nullable
    private Window getWindow() {
        return client.getWindow();
    }

    private void resizeTargets() {
        if (scaledTarget != null) {
            Window window = client.getWindow();
            float scale = (float)getCurrentScaleFactor();
            int w = Math.max(1, (int)(window.getWidth() * scale));
            int h = Math.max(1, (int)(window.getHeight() * scale));
            scaledTarget.resize(w, h, Minecraft.ON_OSX);
        }
        frameGenerator.resize();
    }

    public void setVanillaTarget(RenderTarget target) {
        this.vanillaTarget = target;
    }
}
