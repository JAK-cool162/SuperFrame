package com.jak.glshader;

import com.jak.glshader.config.GlShaderConfig;
import com.jak.glshader.light.LightCacheManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlShaderClient implements ClientModInitializer {
    public static final String MOD_ID = "glshader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static GlShaderClient INSTANCE;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("GLShader initializing – Chunk Light Cache System");
        GlShader.init();
        LightCacheManager.init();
    }

    public static GlShaderClient getInstance() {
        return INSTANCE;
    }
}
