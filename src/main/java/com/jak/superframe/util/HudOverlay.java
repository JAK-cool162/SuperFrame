package com.jak.superframe.util;

import com.jak.superframe.SuperFrame;
import com.jak.superframe.config.SuperFrameConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class HudOverlay implements HudRenderCallback {
    @Override
    public void onHudRender(GuiGraphics guiGraphics, float tickDelta) {
        SuperFrameConfig cfg = SuperFrame.getConfig();
        if (!cfg.frameGenOptions.hudOverlay) return;
        if (!cfg.enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        // Only show in-game, not in menus
        if (mc.screen != null) return;

        String text = String.format("SF %.2fx | FG: %s | %s",
                cfg.getScale(),
                cfg.frameGenMode.getDisplayName(),
                cfg.upscaleMode.name()
        );
        int x = 4;
        int y = 4;
        guiGraphics.drawString(mc.font, text, x, y, 0xFFA0E0FF, true);
    }

    public static void register() {
        HudRenderCallback.EVENT.register(new HudOverlay());
    }
}
