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
        @ConfigEntry.Gui.Tooltip
        public boolean perLightShadowCubemap = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 8, max = 128)
        public int cubemapResolution = 32;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        public int maxUpdatesPerFrame = 2;

        @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
        public CascadedRings cascaded = new CascadedRings();

        public boolean heightmapSunOcclusion = true;
    }

    public static class CascadedRings {
        @ConfigEntry.Gui.Tooltip
        public boolean cascadedRings = true;

        @ConfigEntry.Gui.Tooltip
        public int ring1Radius = 16;
        @ConfigEntry.Gui.Tooltip
        public int ring2Radius = 48;
        @ConfigEntry.Gui.Tooltip
        public int ring3Radius = 96;
        // Ring 4 = 96+ to infinity

        @ConfigEntry.Gui.Tooltip
        public float ring2ResolutionScale = 0.5f;
        @ConfigEntry.Gui.Tooltip
        public float ring3ResolutionScale = 0.25f;
        @ConfigEntry.Gui.Tooltip
        public float ring4ResolutionScale = 0.1f;

        @ConfigEntry.Gui.Tooltip
        public int ring1UpdateInterval = 1;
        @ConfigEntry.Gui.Tooltip
        public int ring2UpdateInterval = 3;
        @ConfigEntry.Gui.Tooltip
        public int ring3UpdateInterval = 10;
        @ConfigEntry.Gui.Tooltip
        public int ring4UpdateInterval = 60;

        @ConfigEntry.Gui.Tooltip
        public int ring1BlockChangeThreshold = 1;
        @ConfigEntry.Gui.Tooltip
        public int ring2BlockChangeThreshold = 2;
        @ConfigEntry.Gui.Tooltip
        public int ring3BlockChangeThreshold = 5;
        @ConfigEntry.Gui.Tooltip
        public int ring4BlockChangeThreshold = 20;
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
            com.jak.glshader.light.LightSourceManager.markAllDirty();
            return InteractionResult.PASS;
        });
        return holder;
    }
}
