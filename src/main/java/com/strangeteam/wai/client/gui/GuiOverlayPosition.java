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
import com.strangeteam.wai.client.gui.cfg.OverlayConfigIO;
import com.strangeteam.wai.client.gui.create.TextureBinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/** Настройка позиции оверлея */

@SideOnly(Side.CLIENT)
public class GuiOverlayPosition extends GuiScreen {

    private final GuiScreen parent;

    private final int startX, startY;

    private boolean dragging = false;
    private int grabDX = 0, grabDY = 0;

    public GuiOverlayPosition(GuiScreen parent) {
        this.parent = parent;
        this.startX = OverlayConfig.overlayX;
        this.startY = OverlayConfig.overlayY;
    }

    @Override
    public void initGui() {
        // кш
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xAA000000);

        drawCenteredString(fontRenderer,
                I18n.format("waimusic.guiop.podsk1"),
                width / 2, 12, 0xFFFFFF);
        drawCenteredString(fontRenderer,
                I18n.format("waimusic.guiop.podsk2"),
                width / 2, 26, 0xAAAAAA);

        drawGhostOverlay();

        int bx = width / 2 - 105;
        int by = height - 30;

        boolean hoverSave = isIn(mouseX, mouseY, bx, by, 100, 20);
        Gui.drawRect(bx, by, bx + 100, by + 20, hoverSave ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, I18n.format("waimusic.guiop.save"), bx + 50, by + 6, 0xFFFFFF);

        boolean hoverCancel = isIn(mouseX, mouseY, bx + 110, by, 100, 20);
        Gui.drawRect(bx + 110, by, bx + 210, by + 20,
                hoverCancel ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, I18n.format("waimusic.guiop.cancel"), bx + 160, by + 6, 0xFFFFFF);

        boolean hoverReset = isIn(mouseX, mouseY, bx + 220, by, 100, 20);
        Gui.drawRect(bx + 220, by, bx + 320, by + 20,
                hoverReset ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, I18n.format("waimusic.guiop.reset"), bx + 270, by + 6, 0xFFFFFF);

        // Рисуем поверх всего тонкую рамку вокруг всего экрана
        Gui.drawRect(0, 0, width, 1, 0xFF00AAFF);
        Gui.drawRect(0, height - 1, width, height, 0xFF00AAFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawGhostOverlay() {
        Minecraft mc = Minecraft.getMinecraft();
        MusicManager mm = MusicManager.INSTANCE;
        TrackInfo cur = mm.getCurrent();

        int w = OverlayConfig.width;
        int h = OverlayConfig.height;
        int x = OverlayConfig.overlayX;
        int y = OverlayConfig.overlayY;

        int alpha = OverlayConfig.overlayAlpha & 0xFF;
        float a = alpha / 255.0f;

        float scale = Math.max(0.5f, Math.min(2.0f, OverlayConfig.textScale));

        // Фон
        int bgAlphaTotal = (OverlayConfig.bgAlpha & 0xFF) * alpha / 255;
        Gui.drawRect(x, y, x + w, y + h,
                withAlpha(OverlayConfig.bgColor, bgAlphaTotal));
        Gui.drawRect(x, y, x + w, y + 1,
                withAlpha(OverlayConfig.lineColor, alpha));

        // Рамка, чтобы было видно границы
        drawBorder(x, y, w, h, 0xFFFFAA00);

        // Иконка
        int textX = 4;
        if (OverlayConfig.showIcon && cur != null) {
            int iconSize = Math.max(8, h - 8);

            GlStateManager.pushMatrix();
            GlStateManager.color(1f, 1f, 1f, a);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableTexture2D();

            boolean hasIcon = cur.isUserTrack() ? cur.iconFile != null : cur.icon != null;
            if (hasIcon) {
                TextureBinder.bindTrackIcon(cur);
                Gui.drawModalRectWithCustomSizedTexture(
                        x + 4, y + 4, 0, 0,
                        iconSize, iconSize, iconSize, iconSize
                );
            }
            Gui.drawModalRectWithCustomSizedTexture(
                    x + 4, y + 4, 0, 0,
                    iconSize, iconSize, iconSize, iconSize
            );

            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();

            textX = 4 + iconSize + 4;
        }

        int textW = w - textX - 4;
        if (textW < 10) textW = 10;

        // Лютый рофл. Я запрещаю Диду делать дисс на меня
        String title  = cur != null ? cur.title  : I18n.format("waimusic.guiop.dissforfl1z");
        String artist = cur != null ? cur.artist : "Didsaife";

        int ty = 6;

        if (OverlayConfig.showTitle) {
            drawScaledText(mc.fontRenderer.trimStringToWidth(title, (int)(textW / scale)),
                    x + textX, y + ty, scale, a, OverlayConfig.textColor & 0xFFFFFF);
            ty += (int)(12 * scale);
        }

        if (OverlayConfig.showArtist) {
            drawScaledText(mc.fontRenderer.trimStringToWidth(artist, (int)(textW / scale)),
                    x + textX, y + ty, scale, a, OverlayConfig.textColorSub & 0xFFFFFF);
            ty += (int)(12 * scale);
        }

        // Полоса
        int barY = h - 8;
        int barX = textX;
        int barW = textW;

        long pos = 0, dur = 0;
        if (cur != null) {
            pos = mm.getPositionMs();
            dur = mm.getDurationMs();
        }
        if (dur <= 0) {
            pos = 30_000;
            dur = 100_000;
        }

        if (OverlayConfig.showProgress) {
            Gui.drawRect(x + barX, y + barY, x + barX + barW, y + barY + 3,
                    withAlpha(0x333333, alpha));

            int fill = (int)Math.max(0, Math.min(barW, (double)pos / dur * barW));
            Gui.drawRect(x + barX, y + barY, x + barX + fill, y + barY + 3,
                    withAlpha(OverlayConfig.lineColor, alpha));
        }

        if (OverlayConfig.showTime) {
            String t = "0:67 / 3:00";
            if (cur != null) {
                t = fmt(pos) + " / " + fmt(dur);
            }

            int color = (mm.isPaused() ? OverlayConfig.timeColorPaused : OverlayConfig.timeColor) & 0xFFFFFF;
            String timeText = mc.fontRenderer.trimStringToWidth(t, (int)(textW / scale));

            int timeOffsetX = 0;
            if (OverlayConfig.timeAlignRight) {
                int timeW = mc.fontRenderer.getStringWidth(timeText);
                timeOffsetX = textW - (int)(timeW * scale);
                if (timeOffsetX < 0) timeOffsetX = 0;
            }

            drawScaledText(timeText,
                    x + textX + timeOffsetX,
                    y + barY - (int)(12 * scale),
                    scale, a, color);
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawScaledText(String text, int sx, int sy, float scale, float a, int rgb) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(sx, sy, 0);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.color(1f, 1f, 1f, a);
        Minecraft.getMinecraft().fontRenderer.drawString(text, 0, 0, rgb);
        GlStateManager.popMatrix();
    }

    private void drawBorder(int x, int y, int w, int h, int color) {
        Gui.drawRect(x, y, x + w, y + 1, color);
        Gui.drawRect(x, y + h - 1, x + w, y + h, color);
        Gui.drawRect(x, y, x + 1, y + h, color);
        Gui.drawRect(x + w - 1, y, x + w, y + h, color);
    }

    private boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int withAlpha(int rgb, int a) {
        return ((a & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static String fmt(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    // Ввод

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            int bx = width / 2 - 105;
            int by = height - 30;

            if (isIn(mouseX, mouseY, bx, by, 100, 20)) {
                // Сохранить
                OverlayConfigIO.save();
                Minecraft.getMinecraft().displayGuiScreen(parent);
                return;
            }
            if (isIn(mouseX, mouseY, bx + 110, by, 100, 20)) {
                // Отмена — вернуть как было
                OverlayConfig.overlayX = startX;
                OverlayConfig.overlayY = startY;
                Minecraft.getMinecraft().displayGuiScreen(parent);
                return;
            }
            if (isIn(mouseX, mouseY, bx + 220, by, 100, 20)) {
                // Сброс — в левый верхний угол
                OverlayConfig.overlayX = 8;
                OverlayConfig.overlayY = 8;
                return;
            }

            // Начало перетаскивания
            int ox = OverlayConfig.overlayX;
            int oy = OverlayConfig.overlayY;
            int ow = OverlayConfig.width;
            int oh = OverlayConfig.height;

            if (mouseX >= ox && mouseX <= ox + ow
                    && mouseY >= oy && mouseY <= oy + oh) {
                dragging = true;
                grabDX = mouseX - ox;
                grabDY = mouseY - oy;
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragging) {
            int nx = mouseX - grabDX;
            int ny = mouseY - grabDY;

            // Ограничиваем экраном
            nx = Math.max(0, Math.min(width  - OverlayConfig.width,  nx));
            ny = Math.max(0, Math.min(height - OverlayConfig.height, ny));

            OverlayConfig.overlayX = nx;
            OverlayConfig.overlayY = ny;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        // Колесо можно использовать для точной подстройки позиции
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && !dragging) {
            // ничего.
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}