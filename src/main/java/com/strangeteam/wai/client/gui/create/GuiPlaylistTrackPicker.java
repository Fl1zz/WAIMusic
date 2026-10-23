/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.gui.create;

import com.strangeteam.wai.client.managers.MusicManager;
import com.strangeteam.wai.client.managers.PlaylistsManager;
import com.strangeteam.wai.client.info.PlaylistInfo;
import com.strangeteam.wai.client.info.TrackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Меню выбора треков из библиотеки для нового плейлиста */

public class GuiPlaylistTrackPicker extends GuiScreen {

    private final GuiScreen parent;
    private final String playlistId;

    private final Set<String> selected = new HashSet<>();
    private int scroll = 0;
    private static final int ROW_H = 14;

    public GuiPlaylistTrackPicker(GuiScreen parent, String playlistId) {
        this.parent = parent;
        this.playlistId = playlistId;
    }

    @Override
    public void initGui() {
        PlaylistInfo pl = PlaylistsManager.INSTANCE.getById(playlistId);
        if (pl != null) selected.addAll(pl.trackIds);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "§l" + I18n.format("gui.wai.playlist_picker.title"),
                width / 2, 12, 0xFFFFFF);

        int x = width / 2 - 200;
        int y = 40;
        int w = 400;
        int h = height - 100;

        Gui.drawRect(x, y, x + w, y + h, 0xAA000000);

        List<TrackInfo> list = MusicManager.INSTANCE.tracks;
        for (int i = 0; i < list.size(); i++) {
            int ry = y + 2 + i * ROW_H - scroll * ROW_H;
            if (ry + ROW_H < y || ry > y + h) continue;

            TrackInfo t = list.get(i);
            boolean sel = selected.contains(t.id);
            boolean hover = mouseX >= x && mouseX <= x + w
                    && mouseY >= ry && mouseY <= ry + ROW_H;

            Gui.drawRect(x + 1, ry, x + w - 1, ry + ROW_H - 1,
                    hover ? 0x44FFFFFF : 0x22000000);

            String mark = sel ? "§a[X]" : "§7[ ]";
            String name = (t.isUserTrack() ? "§f" : "§7") + t.title
                    + " §8— " + t.artist;
            drawString(fontRenderer, mark + " " + trim(name, w - 20),
                    x + 4, ry + 3, 0xFFFFFF);
        }

        int by = height - 40;
        drawCenteredString(fontRenderer,
                "§7" + I18n.format("gui.wai.playlist_picker.selected", selected.size(), list.size()),
                width / 2, by - 12, 0xAAAAAA);

        // Кнопки
        int bx = width / 2 - 160;
        drawButton(bx, by, 150, 20, "§a" + I18n.format("gui.wai.playlist_picker.save"), mouseX, mouseY);
        drawButton(bx + 170, by, 150, 20, I18n.format("gui.wai.playlist_picker.cancel"), mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawButton(int x, int y, int w, int h, String label, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        Gui.drawRect(x, y, x + w, y + h, hover ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, label, x + w / 2, y + 6, 0xFFFFFF);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        int by = height - 40;
        int bx = width / 2 - 160;
        if (isIn(mouseX, mouseY, bx, by, 150, 20)) {
            applyAndClose();
            return;
        }
        if (isIn(mouseX, mouseY, bx + 170, by, 150, 20)) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }

        int x = width / 2 - 200;
        int y = 40;
        int w = 400;
        int h = height - 100;
        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            int idx = (mouseY - (y + 2) + scroll * ROW_H) / ROW_H;
            List<TrackInfo> list = MusicManager.INSTANCE.tracks;
            if (idx >= 0 && idx < list.size()) {
                String id = list.get(idx).id;
                if (selected.contains(id)) selected.remove(id);
                else selected.add(id);
            }
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void applyAndClose() {
        PlaylistsManager pm = PlaylistsManager.INSTANCE;
        PlaylistInfo pl = pm.getById(playlistId);
        if (pl != null && !pl.builtin) {
            pl.trackIds.clear();
            for (TrackInfo t : MusicManager.INSTANCE.tracks) {
                if (selected.contains(t.id)) pl.trackIds.add(t.id);
            }
            pm.saveAll();
        }
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll -= Integer.signum(wheel);
            int visible = (height - 100) / ROW_H;
            int max = Math.max(0, MusicManager.INSTANCE.tracks.size() - visible);
            if (scroll < 0) scroll = 0;
            if (scroll > max) scroll = max;
        }
    }

    private String trim(String s, int maxWidth) {
        return fontRenderer.trimStringToWidth(s, maxWidth);
    }

    private static boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}