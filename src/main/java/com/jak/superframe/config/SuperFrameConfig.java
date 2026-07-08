package com.jak.superframe.config;

import com.jak.superframe.SuperFrame;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.network.chat.Component;

@Config(name = "superframe")
public class SuperFrameConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 25, max = 400)
    public int renderScalePercent = 100; // 100 = 1.0x

    public float getScale() {
        float s = renderScalePercent / 100.0f;
        return Math.max(0.25f, Math.min(4.0f, s));
    }

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public UpscaleMode upscaleMode = UpscaleMode.BILINEAR;

    @ConfigEntry.Gui.Tooltip
    public boolean forceLinearFiltering = true;

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public FrameGenMode frameGenMode = FrameGenMode.OFF;

    @ConfigEntry.Gui.CollapsibleObject
    public PerformanceTweaks performance = new PerformanceTweaks();

    @ConfigEntry.Gui.CollapsibleObject
    public FrameGenOptions frameGenOptions = new FrameGenOptions();

    // Iris / Sodium compat
    @ConfigEntry.Category("compat")
    @ConfigEntry.Gui.Tooltip
    public float irisShaderScaleOverride = -1.0f;

    public static class PerformanceTweaks {
        @ConfigEntry.Gui.Tooltip
        public boolean dynamicResolution = true;
        @ConfigEntry.Gui.Tooltip
        public int targetFps = 60;
        @ConfigEntry.BoundedDiscrete(min = 50, max = 100)
        public int dynamicResolutionMin = 50; // percent
        public boolean reduceParticles = false;
        public boolean fastFog = false;
        public boolean noEntityShadows = false;
    }

    public static class FrameGenOptions {
        @ConfigEntry.Gui.Tooltip
        public float sharpness = 0.5f;
        @ConfigEntry.Gui.Tooltip
        public boolean reduceGhosting = true;
        @ConfigEntry.Gui.Tooltip
        public boolean hudOverlay = true;
    }

    public enum UpscaleMode implements ConfigEntry.Gui.EnumHandler.EnumDisplayOptionProvider {
        NEAREST("Nearest"),
        BILINEAR("Bilinear"),
        FSR1("FSR 1.0 EASU+RCAS"),
        CAS("CAS Sharpen");

        private final String name;
        UpscaleMode(String name) { this.name = name; }
        public String getDisplayName() { return name; }
        @Override public Component getEnumDisplayName() { return Component.literal(name); }
    }

    public enum FrameGenMode implements ConfigEntry.Gui.EnumHandler.EnumDisplayOptionProvider {
        OFF("Off"),
        BLEND_2X("Blend 2x"),
        FLOW_LITE("Flow Lite");

        private final String name;
        FrameGenMode(String name) { this.name = name; }
        public String getDisplayName() { return name; }
        @Override public Component getEnumDisplayName() { return Component.literal(name); }
    }

    public boolean getUpscaleFilterLinear() {
        if (upscaleMode == UpscaleMode.NEAREST) return false;
        return forceLinearFiltering || getScale() > 1.0f;
    }

    public static ConfigHolder<SuperFrameConfig> init() {
        ConfigHolder<SuperFrameConfig> holder = AutoConfig.register(SuperFrameConfig.class, JanksonConfigSerializer::new);
        holder.registerSaveListener((manager, data) -> {
            if (SuperFrame.getInstance() != null) {
                SuperFrame.getInstance().onResolutionChanged();
                com.jak.superframe.compat.iris.IrisCompat.reloadShaders();
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        return holder;
    }
}
