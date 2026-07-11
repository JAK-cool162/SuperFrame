package com.jak.glshader.compat;

import net.fabricmc.loader.api.FabricLoader;

public class IrisCompat {
    public static boolean isIrisLoaded() {
        return FabricLoader.getInstance().isModLoaded("iris") || FabricLoader.getInstance().isModLoaded("oculus");
    }

    public static boolean isShaderPackActive() {
        if (!isIrisLoaded()) return false;
        try {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApi.getMethod("getInstance").invoke(null);
            return (boolean) irisApi.getMethod("isShaderPackInUse").invoke(instance);
        } catch (Throwable t) {
            return false;
        }
    }
}
