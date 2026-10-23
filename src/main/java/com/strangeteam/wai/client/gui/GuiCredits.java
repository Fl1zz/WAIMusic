/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.gui;

import com.strangeteam.wai.client.gui.cfg.OverlayConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Автор */
public class GuiCredits extends GuiScreen {

    private final GuiScreen parent;

    private final List<LinkEntry> links = new ArrayList<>();

    private int panelX, panelY;
    private final int panelW = 320;
    private final int panelH = 250;

    private static final net.minecraft.util.ResourceLocation LOGO =
            new net.minecraft.util.ResourceLocation("waimusic", "textures/gui/strangeteam_white.png");

    private static class LinkEntry {
        String label;
        String url;
        LinkEntry(String label, String url) {
            this.label = label;
            this.url = url;
        }
    }

    public GuiCredits(GuiScreen parent) {
        this.parent = parent;
        links.add(new LinkEntry((I18n.format("waimusic.credits.strpage")), "https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z"));
        links.add(new LinkEntry("Github Fl1z",           "https://github.com/Fl1zz"));
        links.add(new LinkEntry("Telegram",               "https://t.me/Fl1zStrangeHill"));
        links.add(new LinkEntry("Youtube",                "https://youtube.com/@fl1z?si=HGILAJ3cKScyConw"));
        links.add(new LinkEntry("Discord",                "https://discord.com/invite/Dvc2N6bEyM"));
    }

    private static int withAlpha(int rgb, int a) {
        return ((a & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private int lineColor() {
        return withAlpha(OverlayConfig.lineColor, 0xFF);
    }

    private int textColor() {
        return OverlayConfig.guiTextColor;
    }

    @Override
    public void initGui() {
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Панель
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, withAlpha(OverlayConfig.guiBgColor, OverlayConfig.guiBgAlpha));
        // Верхняя и нижняя линии
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + 1, lineColor());
        Gui.drawRect(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, lineColor());

        drawCenteredString(fontRenderer, I18n.format("waimusic.credits.author"), width / 2, panelY + 18, textColor());

        // Хочу пиццу заказать, денег нет бл
        drawCenteredString(fontRenderer, "Fl1z", width / 2, panelY + 40, lineColor());
        drawCenteredString(fontRenderer, I18n.format("waimusic.credits.offcli"),
                width / 2, panelY + 54, withAlpha(OverlayConfig.guiButtonText, OverlayConfig.guiBgAlpha));

        // Разделитель
        Gui.drawRect(panelX + 20, panelY + 76,
                panelX + panelW - 20, panelY + 77, lineColor());

        // Ссылки
        int linkStartY = panelY + 92;
        int rowH = 26;

        for (int i = 0; i < links.size(); i++) {
            LinkEntry le = links.get(i);
            int y = linkStartY + i * rowH;

            if (y + rowH < panelY + 80 || y > panelY + panelH - 40) continue;

            int x = panelX + 20;
            int w = panelW - 40;
            int h = 20;

            boolean hover = mouseX >= x && mouseX <= x + w
                    && mouseY >= y && mouseY <= y + h
                    && mouseY >= panelY + 80 && mouseY <= panelY + panelH - 40;

            int baseColor = withAlpha(OverlayConfig.guiButtonColor, 0xFF);
            int hoverColor = withAlpha(OverlayConfig.guiButtonHover, 0xFF);
            Gui.drawRect(x, y, x + w, y + h, hover ? hoverColor : baseColor);
            Gui.drawRect(x, y, x + w, y + 1, lineColor());

            drawString(fontRenderer, le.label, x + 8, y + 6, textColor());

            // Мой любимый символ кста - треугольник
            drawString(fontRenderer, "▶", x + w - 14, y + 6, lineColor());
        }

        int bx = width / 2 - 50, by = panelY + panelH - 30;
        boolean backHover = mouseX >= bx && mouseX <= bx + 100
                && mouseY >= by && mouseY <= by + 20;
        Gui.drawRect(bx, by, bx + 100, by + 20,
                backHover ? withAlpha(OverlayConfig.guiButtonHover, 0xFF)
                        : withAlpha(OverlayConfig.guiButtonColor, 0xFF));
        Gui.drawRect(bx, by, bx + 100, by + 1, lineColor());
        drawCenteredString(fontRenderer, I18n.format("waimusic.credits.back"), width / 2, by + 6, textColor());

        // STRANGETEAM ЛЮТЫЕ
        int logoSize = 20; // в пикселях
        int logoMargin = 4;
        int logoY = panelY + 6;

        int lc = OverlayConfig.lineColor;
        float r = ((lc >> 16) & 0xFF) / 255f;
        float g = ((lc >> 8)  & 0xFF) / 255f;
        float b = ( lc        & 0xFF) / 255f;

        net.minecraft.client.renderer.GlStateManager.color(r, g, b, 1f);
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.blendFunc(
                org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();

        mc.getTextureManager().bindTexture(LOGO);

        Gui.drawModalRectWithCustomSizedTexture(
                panelX + logoMargin, logoY, 0, 0,
                logoSize, logoSize, logoSize, logoSize);
        Gui.drawModalRectWithCustomSizedTexture(
                panelX + panelW - logoMargin - logoSize, logoY, 0, 0,
                logoSize, logoSize, logoSize, logoSize);

        net.minecraft.client.renderer.GlStateManager.color(1f, 1f, 1f, 1f);
        net.minecraft.client.renderer.GlStateManager.disableBlend();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;

        int bx = width / 2 - 50, by = panelY + panelH - 30;
        if (mouseX >= bx && mouseX <= bx + 100 && mouseY >= by && mouseY <= by + 20) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }

        int linkStartY = panelY + 92;
        int rowH = 26;
        int x = panelX + 20;
        int w = panelW - 40;
        int h = 20;

        for (int i = 0; i < links.size(); i++) {
            int y = linkStartY + i * rowH;
            if (mouseX >= x && mouseX <= x + w
                    && mouseY >= y && mouseY <= y + h
                    && mouseY >= panelY + 80 && mouseY <= panelY + panelH - 40) {
                openLink(links.get(i).url);
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void openLink(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}