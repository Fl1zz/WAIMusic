/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.gui;

import com.strangeteam.wai.client.managers.MusicManager;
import com.strangeteam.wai.client.info.TrackInfo;
import com.strangeteam.wai.client.gui.cfg.OverlayConfig;
import com.strangeteam.wai.client.gui.create.TextureBinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/** Оверлей */ // бл зачем я пишу эти комментарии, тут и по названию понятно

@SideOnly(Side.CLIENT)
public class GuiOverlay {

    // Скрытие HUD
    public static boolean hidden = false;

    public static boolean showRemaining = false;

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (hidden) return;

        MusicManager mm = MusicManager.INSTANCE;
        TrackInfo cur = mm.getCurrent();
        if (cur == null || !mm.isPlaying()) return;

        Minecraft mc = Minecraft.getMinecraft();

        int w = OverlayConfig.width;
        int h = OverlayConfig.height;
        int x = OverlayConfig.overlayX;
        int y = OverlayConfig.overlayY;

        int alpha = OverlayConfig.overlayAlpha & 0xFF;
        float a = alpha / 255.0f;

        float scale = Math.max(0.5f, Math.min(2.0f, OverlayConfig.textScale));

        // ФОН ПАНЕЛИ
        int bgAlphaTotal = (OverlayConfig.bgAlpha & 0xFF) * alpha / 255;

        int bg = withAlpha(OverlayConfig.bgColor, bgAlphaTotal);

        Gui.drawRect(x, y, x + w, y + h, bg);

        // Верхняя линия
        Gui.drawRect(x, y, x + w, y + 1,
                withAlpha(OverlayConfig.lineColor, alpha));

        // ИКОНКА
        int textX = 4;

        if (OverlayConfig.showIcon) {

            int iconSize = Math.max(8, h - 8);

            GlStateManager.pushMatrix();

            GlStateManager.color(
                    1.0f,
                    1.0f,
                    1.0f,
                    a
            );

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(
                    GL11.GL_SRC_ALPHA,
                    GL11.GL_ONE_MINUS_SRC_ALPHA
            );
            GlStateManager.enableTexture2D();

            boolean hasIcon = cur.isUserTrack() ? cur.iconFile != null : cur.icon != null;
            if (hasIcon) {
                TextureBinder.bindTrackIcon(cur);
                Gui.drawModalRectWithCustomSizedTexture(
                        x + 4, y + 4, 0, 0,
                        iconSize, iconSize, iconSize, iconSize
                );
            }

            Gui.drawModalRectWithCustomSizedTexture(x + 4, y + 4, 0, 0, iconSize, iconSize, iconSize, iconSize);

            GlStateManager.color(
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f
            );

            GlStateManager.disableBlend();

            GlStateManager.popMatrix();

            textX = 4 + iconSize + 4;
        }

        // РАЗМЕР ОБЛАСТИ ТЕКСТА
        int textW = w - textX - 4;

        if (textW < 10) {
            textW = 10;
        }

        // НАЗВАНИЕ
        int ty = 6;

        if (OverlayConfig.showTitle) {

            String title = mc.fontRenderer.trimStringToWidth(cur.title, (int)(textW / scale));

            GlStateManager.pushMatrix();

            GlStateManager.translate(x + textX, y + ty, 0);

            GlStateManager.scale(scale, scale, 1.0f);

            GlStateManager.color(
                    1.0f,
                    1.0f,
                    1.0f,
                    a
            );

            mc.fontRenderer.drawString(title, 0, 0, OverlayConfig.textColor & 0xFFFFFF);

            GlStateManager.popMatrix();

            ty += (int)(12 * scale);
        }

        // АВТОР
        if (OverlayConfig.showArtist) {

            String artist = mc.fontRenderer.trimStringToWidth(cur.artist, (int)(textW / scale));

            GlStateManager.pushMatrix();

            GlStateManager.translate(x + textX, y + ty, 0);

            GlStateManager.scale(scale, scale, 1.0f);

            GlStateManager.color(
                    1.0f,
                    1.0f,
                    1.0f,
                    a
            );

            mc.fontRenderer.drawString(artist, 0, 0, OverlayConfig.textColorSub & 0xFFFFFF);

            GlStateManager.popMatrix();

            ty += (int)(12 * scale);
        }

        // ВРЕМЯ / ПРОГРЕСС
        long pos = mm.getPositionMs();
        long dur = mm.getDurationMs();

        int barY = h - 8;
        int barX = textX;
        int barW = textW;

        // ПРОГРЕСС-БАР
        if (OverlayConfig.showProgress && dur > 0) {

            Gui.drawRect(
                    x + barX,
                    y + barY,
                    x + barX + barW,
                    y + barY + 3,
                    withAlpha(
                            0x333333,
                            alpha
                    )
            );

            int fill = (int)Math.max(0, Math.min(
                            barW,
                            (double)pos / dur * barW));

            Gui.drawRect(
                    x + barX,
                    y + barY,
                    x + barX + fill,
                    y + barY + 3,
                    withAlpha(
                            OverlayConfig.lineColor,
                            alpha
                    )
            );
        }

        // ВРЕМЯ
        if (OverlayConfig.showTime) {

            String t = showRemaining
                    ? "-" + fmt(Math.max(0, dur - pos))
                    : fmt(pos) + " / " + fmt(dur);

            int color = mm.isPaused()
                    ? OverlayConfig.timeColorPaused
                    : OverlayConfig.timeColor;

            // Чтобы время тоже корректно вписывалось
            String timeText =
                    mc.fontRenderer.trimStringToWidth(t, (int)(textW / scale));

            // Выравнивание вправо (в конец полосы)
            int timeOffsetX = 0;
            if (OverlayConfig.timeAlignRight) {
                int timeW = mc.fontRenderer.getStringWidth(timeText);
                timeOffsetX = textW - (int)(timeW * scale);
                if (timeOffsetX < 0) timeOffsetX = 0;
            }

            GlStateManager.pushMatrix();

            GlStateManager.translate(
                    x + textX + timeOffsetX,
                    y + barY - (int)(12 * scale),
                    0
            );

            GlStateManager.scale(
                    scale,
                    scale,
                    1.0f
            );

            GlStateManager.color(
                    1.0f,
                    1.0f,
                    1.0f,
                    a
            );

            mc.fontRenderer.drawString(
                    timeText,
                    0,
                    0,
                    color & 0xFFFFFF
            );

            GlStateManager.popMatrix();
        }

        GlStateManager.color(
                1.0f,
                1.0f,
                1.0f,
                1.0f
        );
    }

    private static int withAlpha(int rgb, int a) {
        return ((a & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static String fmt(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}