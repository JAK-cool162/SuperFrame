package com.jak.glshader.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * SuperFrame integration – optional runtime dependency
 *
 * - Static world layer → SuperFrame render target
 * - Hook into SuperFrame scale change event to invalidate shadow cache
 *
 * Uses reflection so GLShader compiles without SuperFrame present.
 */
public class SuperFrameCompat {
    private static final List<Runnable> scaleChangeListeners = new ArrayList<>();

    public static boolean isSuperFrameLoaded() {
        return FabricLoader.getInstance().isModLoaded("superframe");
    }

    public static void registerScaleChangeListener(Runnable r) {
        scaleChangeListeners.add(r);
        // If SuperFrame is present, try to hook its config save listener via reflection
        if (isSuperFrameLoaded()) {
            try {
                // com.jak.superframe.SuperFrame.CONFIG.registerSaveListener(...)
                Class<?> sfConfigClass = Class.forName("com.jak.superframe.config.SuperFrameConfig");
                Class<?> sfClass = Class.forName("com.jak.superframe.SuperFrame");
                Object configHolder = sfClass.getField("CONFIG").get(null);
                // ConfigHolder.registerSaveListener(...)
                // We already added our own listener in GlShader.init via direct call if compile-time dep exists
                // For reflection-only mode, user can manually call fireScaleChange() from SuperFrame
            } catch (Throwable ignored) {}
        }
    }

    /** Called by SuperFrame (if present) or manually */
    public static void fireScaleChange() {
        for (Runnable r : scaleChangeListeners) r.run();
    }

    /** Get current SuperFrame render scale, or 1.0 if not loaded */
    public static float getSuperFrameScale() {
        if (!isSuperFrameLoaded()) return 1.0f;
        try {
            Class<?> sf = Class.forName("com.jak.superframe.SuperFrame");
            Object inst = sf.getMethod("getInstance").invoke(null);
            if (inst == null) return 1.0f;
            double scale = (double) sf.getMethod("getCurrentScaleFactor").invoke(inst);
            return (float) scale;
        } catch (Throwable t) {
            return 1.0f;
        }
    }
}
