package com.jak.glshader;

import com.jak.glshader.light.LightCacheManager;
import com.jak.glshader.light.LightSourceManager;
import com.jak.glshader.shadow.CascadedShadowManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlShaderClient implements ClientModInitializer {
    public static final String MOD_ID = "glshader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static GlShaderClient INSTANCE;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("GLShader initializing – Chunk Light Cache + Shadow Cubemaps + Cascaded Rings");
        GlShader.init();
        LightCacheManager.init();
        LightSourceManager.init();
        CascadedShadowManager.init();

        // Tick cascaded shadow rings – update player position, ring assignments
        // Then process dirty shadow cubemaps – N per frame, prioritized by ring
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CascadedShadowManager.tick();
            if (client.level == null) return;
            if (!GlShader.shouldRunShaders()) return;
            LightSourceManager.tick(client.level);
        });
    }

    public static GlShaderClient getInstance() {
        return INSTANCE;
    }
}
