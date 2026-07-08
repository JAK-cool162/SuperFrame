package com.jak.superframe.compat.iris;

import com.jak.superframe.SuperFrameClient;
import net.fabricmc.loader.api.FabricLoader;

public class IrisCompat {
    public static boolean isIrisLoaded() {
        return FabricLoader.getInstance().isModLoaded("iris") || FabricLoader.getInstance().isModLoaded("oculus");
    }

    public static void reloadShaders() {
        if (!isIrisLoaded()) return;
        try {
            // Use reflection to avoid hard dependency
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApi.getMethod("getInstance").invoke(null);
            boolean inUse = (boolean) irisApi.getMethod("isShaderPackInUse").invoke(instance);
            if (inUse) {
                SuperFrameClient.LOGGER.info("Iris shader pack detected – reloading for SuperFrame scale change");
                // Iris 1.7+: IrisApi.getInstance().reload();
                try {
                    irisApi.getMethod("reload").invoke(instance);
                } catch (NoSuchMethodException ignored) {
                    // older iris – ignore
                }
            }
        } catch (Throwable t) {
            // Iris not present or API changed – silently ignore
        }
    }

    public static float getIrisScaleOverride(float fallback) {
        // If user set irisShaderScaleOverride > 0 in config, use that when shaders are active
        try {
            if (isIrisLoaded()) {
                Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance = irisApi.getMethod("getInstance").invoke(null);
                boolean inUse = (boolean) irisApi.getMethod("isShaderPackInUse").invoke(instance);
                if (inUse) {
                    float override = com.jak.superframe.SuperFrame.getConfig().irisShaderScaleOverride;
                    if (override > 0) return override;
                }
            }
        } catch (Throwable ignored) {}
        return fallback;
    }
}
