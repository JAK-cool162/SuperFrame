package com.jak.glshader;

import com.jak.glshader.compat.IrisCompat;
import com.jak.glshader.compat.SuperFrameCompat;
import com.jak.glshader.config.GlShaderConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;

public class GlShader {
    public static final ConfigHolder<GlShaderConfig> CONFIG = GlShaderConfig.init();

    public static void init() {
        GlShaderClient.LOGGER.info("GLShader core init – SuperFrame integration: {}", SuperFrameCompat.isSuperFrameLoaded());
        // Hook SuperFrame scale change to invalidate shadow cache
        SuperFrameCompat.registerScaleChangeListener(() -> {
            com.jak.glshader.light.LightCacheManager.invalidateAll();
            GlShaderClient.LOGGER.info("SuperFrame scale changed – light cache invalidated");
        });
    }

    public static GlShaderConfig getConfig() {
        return CONFIG.getConfig();
    }

    /** Returns true if GLShader lighting should run (Iris shaders OFF) */
    public static boolean shouldRunShaders() {
        GlShaderConfig cfg = getConfig();
        if (!cfg.enabled) return false;
        if (cfg.disableWhenIrisActive && IrisCompat.isShaderPackActive()) {
            return false;
        }
        return true;
    }
}
