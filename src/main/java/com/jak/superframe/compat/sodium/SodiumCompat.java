package com.jak.superframe.compat.sodium;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Sodium compatibility helpers.
 * SuperFrame is fully compatible with Sodium – both manipulate the main render target.
 * This class provides optional integration into Sodium's video settings screen.
 */
public class SodiumCompat {
    public static boolean isSodiumLoaded() {
        return FabricLoader.getInstance().isModLoaded("sodium");
    }

    // If you want a Sodium options page, implement me.shedaniel.clothconfig2.api.AbstractConfigListEntry
    // and register via Sodium's options API (if available).
    // For now, use ModMenu / Cloth Config standalone screen – works fine alongside Sodium.
}
