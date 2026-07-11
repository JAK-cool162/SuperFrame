package com.jak.glshader.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.world.InteractionResult;

@Config(name = "glshader")
public class GlShaderConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip
    public boolean disableWhenIrisActive = true;

    @ConfigEntry.Gui.CollapsibleObject
    public LightCache lightCache = new LightCache();

    @ConfigEntry.Gui.CollapsibleObject
    public Shadows shadows = new Shadows();

    @ConfigEntry.Gui.CollapsibleObject
    public Gi gi = new Gi();

    public static class LightCache {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;
        @ConfigEntry.Gui.Tooltip
        public boolean precalculateLightZones = true;
        @ConfigEntry.BoundedDiscrete(min = 1, max = 16)
        public int zoneSize = 4; // blocks per light zone
    }

    public static class Shadows {
        public boolean perLightShadowCubemap = true;
        public boolean cascadedRings = true;
        public boolean heightmapSunOcclusion = true;
    }

    public static class Gi {
        public boolean fakeGlobalIllumination = true;
        public boolean colorBleeding = true;
        public boolean ambientOcclusionFromHeightmap = true;
    }

    public static ConfigHolder<GlShaderConfig> init() {
        ConfigHolder<GlShaderConfig> holder = AutoConfig.register(GlShaderConfig.class, JanksonConfigSerializer::new);
        holder.registerSaveListener((manager, data) -> {
            com.jak.glshader.light.LightCacheManager.invalidateAll();
            return InteractionResult.PASS;
        });
        return holder;
    }
}
