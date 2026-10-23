/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.gui;

import com.strangeteam.wai.client.managers.MusicManager;
import com.strangeteam.wai.client.info.PlaylistInfo;
import com.strangeteam.wai.client.managers.PlaylistsManager;
import com.strangeteam.wai.client.info.TrackInfo;
import com.strangeteam.wai.client.gui.cfg.OverlayConfig;
import com.strangeteam.wai.client.gui.create.GuiCreatePlaylist;
import com.strangeteam.wai.client.gui.create.GuiImportPlaylistWizard;
import com.strangeteam.wai.client.gui.create.GuiRenamePlaylist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Панель плейлистов (слева) */

public class GuiPlaylistPanel {

    private final GuiMusicPlayer parent;

    private int x, y, w, h;
    private int scroll = 0;

    private static final int BTN_H = 18;
    private static final int HEADER_H = 14;

    // Drag плейлистов
    private boolean draggingPlaylist     = false;
    private int     dragPlaylistFrom     = -1;
    private int     dragPlaylistTo       = -1;
    private int     dragPlaylistOffsetY  = 0;

    private static final int EDIT_BTN_W = 12;   // кнопка редакции

    public GuiPlaylistPanel(GuiMusicPlayer parent) {
        this.parent = parent;
    }

    public void updateLayout(int guiX, int guiY, int guiH) {
        this.w = OverlayConfig.playlistWidth;
        this.h = guiH;
        this.x = guiX - w - 8;
        this.y = guiY;
    }

    public int getWidth() { return w; }
    public int getX() { return x; }

    public boolean handleKey(int keyCode) {
        if (keyCode == org.lwjgl.input.Keyboard.KEY_BACK
                || keyCode == org.lwjgl.input.Keyboard.KEY_DELETE) {
            PlaylistInfo active = MusicManager.INSTANCE.getActivePlaylist();
            if (active != null && !active.builtin) {
                PlaylistsManager.INSTANCE.removePlaylist(active.id);
                MusicManager.INSTANCE.setActivePlaylist(null);
                return true;
            }
        }
        return false;
    }

    // Отрисовка
    public void draw(int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();

        // Фон панели — цвет + отдельная прозрачность панели плейлистов
        int bg = argb(OverlayConfig.playlistBgColor, OverlayConfig.playlistPanelBgAlpha);
        Gui.drawRect(x, y, x + w, y + h, bg);

        // Верхняя линия
        Gui.drawRect(x, y, x + w, y + 1,
                argb(OverlayConfig.lineColor, 0xFF));

        // Заголовок — фон и текст из конфига
        int headerBg = argb(OverlayConfig.playlistHeaderBgColor, 0xFF);
        Gui.drawRect(x, y, x + w, y + HEADER_H, headerBg);
        mc.fontRenderer.drawStringWithShadow(I18n.format("waimusic.guipp.header"),
                x + 4, y + 3,
                OverlayConfig.playlistHeaderTextColor);

        int listTop = y + HEADER_H;
        int listBottom = y + h - 2 * (BTN_H + 2) - 4;

        List<PlaylistInfo> list = PlaylistsManager.INSTANCE.playlists;
        PlaylistInfo active = MusicManager.INSTANCE.getActivePlaylist();

        int rowH = OverlayConfig.playlistShowDesc ? 24 : 14;

        // Библиотека
        int libY = listTop + 2 - scroll * rowH;
        if (libY + rowH >= listTop && libY <= listBottom) {
            boolean hover = mouseX >= x && mouseX <= x + w
                    && mouseY >= libY && mouseY <= libY + rowH;
            boolean isActive = (active == null);

            int rowBg;
            if (isActive) rowBg = argb(OverlayConfig.playlistSelColor, 0xFF);
            else if (hover) rowBg = argb(OverlayConfig.playlistRowHoverColor, 0x44);
            else rowBg = argb(OverlayConfig.playlistRowColor, 0x22);

            Gui.drawRect(x + 2, libY, x + w - 2, libY + rowH - 1, rowBg);

            mc.fontRenderer.drawStringWithShadow(
                    mc.fontRenderer.trimStringToWidth(
                            I18n.format("waimusic.guipp.library"), w - 8),
                    x + 5, libY + 3, OverlayConfig.playlistTextColor);

            if (OverlayConfig.playlistShowDesc) {
                int cnt = MusicManager.INSTANCE.tracks.size();
                mc.fontRenderer.drawStringWithShadow(
                        I18n.format("waimusic.guipp.track_count", cnt),
                        x + 5, libY + 13, OverlayConfig.playlistSubColor);
            }
        }

        // Плейлисты
        for (int i = 0; i < list.size(); i++) {
            PlaylistInfo p = list.get(i);
            int ry = listTop + 2 + (i + 1) * rowH - scroll * rowH;
            if (ry + rowH < listTop || ry > listBottom) continue;

            boolean hover = mouseX >= x && mouseX <= x + w
                    && mouseY >= ry && mouseY <= ry + rowH;
            boolean isActive = p == active;

            int rowBg;
            if (isActive) rowBg = argb(OverlayConfig.playlistSelColor, 0xFF);
            else if (hover) rowBg = argb(OverlayConfig.playlistRowHoverColor, 0x44);
            else rowBg = argb(OverlayConfig.playlistRowColor, 0x22);

            Gui.drawRect(x + 2, ry, x + w - 2, ry + rowH - 1, rowBg);

            int nameColor = p.builtin
                    ? OverlayConfig.playlistBuiltinTextColor
                    : OverlayConfig.playlistTextColor;
            mc.fontRenderer.drawStringWithShadow(
                    mc.fontRenderer.trimStringToWidth(p.name, w - 8),
                    x + 5, ry + 3, nameColor);

            if (OverlayConfig.playlistShowDesc) {
                String sub = p.builtin
                        ? I18n.format("waimusic.guipp.builtin")
                        : I18n.format("waimusic.guipp.track_count", p.size());
                mc.fontRenderer.drawStringWithShadow(
                        mc.fontRenderer.trimStringToWidth(sub, w - 8),
                        x + 5, ry + 13, OverlayConfig.playlistSubColor);
            }

            // Кнопка редакции (только для пользовательских)
            if (!p.builtin) {
                int editX = x + w - EDIT_BTN_W - 4;
                int editY = ry + (rowH - EDIT_BTN_W) / 2;
                boolean hoverEdit = mouseX >= editX && mouseX <= editX + EDIT_BTN_W
                        && mouseY >= editY && mouseY <= editY + EDIT_BTN_W;

                int editBg = hoverEdit
                        ? argb(OverlayConfig.playlistEditBtnHoverColor, 0xAA)
                        : argb(OverlayConfig.playlistEditBtnColor, 0x66);

                Gui.drawRect(editX, editY, editX + EDIT_BTN_W, editY + EDIT_BTN_W, editBg);
                Gui.drawRect(editX, editY, editX + EDIT_BTN_W, editY + 1,
                        argb(OverlayConfig.playlistEditBtnBorderColor, 0x66));

                mc.fontRenderer.drawStringWithShadow("...",
                        editX + (EDIT_BTN_W - mc.fontRenderer.getStringWidth("✎")) / 2,
                        editY + 2,
                        OverlayConfig.playlistEditBtnTextColor);
            }
        }

        if (draggingPlaylist) {
            int userCount = 0;
            for (PlaylistInfo p : list) if (!p.builtin) userCount++;

            if (dragPlaylistFrom >= 0 && dragPlaylistFrom < userCount) {
                List<PlaylistInfo> userList = new ArrayList<>();
                for (PlaylistInfo p : list) if (!p.builtin) userList.add(p);

                PlaylistInfo dragged = userList.get(dragPlaylistFrom);
                int ghostY = mouseY - dragPlaylistOffsetY;

                Gui.drawRect(x, ghostY, x + w, ghostY + rowH - 2,
                        argb(OverlayConfig.playlistDragGhostBg, 0x88));
                Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(
                        dragged.name, x + 5, ghostY + 3,
                        OverlayConfig.playlistDragGhostText);
            }
        }

        // Подсказка
        int by = y + h - 2 * (BTN_H + 2);
        mc.fontRenderer.drawStringWithShadow(
                I18n.format("waimusic.guipp.hint"),
                x + 4, by - 10, OverlayConfig.playlistSubColor);

        // Маркер вставки при drag плейлиста
        if (draggingPlaylist) {
            int builtinCount = 0;
            for (PlaylistInfo p : list) if (p.builtin) builtinCount++;

            int visualRow = builtinCount + dragPlaylistTo;
            int markerY = listTop + 2 + (visualRow + 1) * rowH - scroll * rowH;

            if (markerY >= listTop && markerY <= listBottom) {
                Gui.drawRect(x, markerY - 1, x + w, markerY + 1,
                        OverlayConfig.playlistDragMarkerColor);
            }
        }

        // Кнопки
        drawBtn(I18n.format("waimusic.guipp.create"), x + 2, by, w - 4, BTN_H, mouseX, mouseY);
        drawBtn(I18n.format("waimusic.guipp.import"), x + 2, by + BTN_H + 2, w - 4, BTN_H, mouseX, mouseY);
    }

    private void drawBtn(String label, int bx, int by, int bw, int bh, int mx, int my) {
        boolean hover = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        Gui.drawRect(bx, by, bx + bw, by + bh,
                argb(hover ? OverlayConfig.playlistBtnHover : OverlayConfig.playlistBtnColor, 0xFF));
        Gui.drawRect(bx, by, bx + bw, by + 1,
                argb(OverlayConfig.playlistBtnBorderColor, 0xFF));

        net.minecraft.client.gui.FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        String t = fr.trimStringToWidth(label, bw - 4);
        fr.drawStringWithShadow(t,
                bx + (bw - fr.getStringWidth(t)) / 2,
                by + (bh - 8) / 2,
                OverlayConfig.playlistBtnText);
    }

    // Ввод
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 && button != 1) return false;

        // Кнопки (только ЛКМ)
        int by = y + h - 2 * (BTN_H + 2);
        if (button == 0) {
            if (isIn(mouseX, mouseY, x + 2, by, w - 4, BTN_H)) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiCreatePlaylist(parent));
                return true;
            }
            if (isIn(mouseX, mouseY, x + 2, by + BTN_H + 2, w - 4, BTN_H)) {
                openImportDialog();
                return true;
            }
        }

        // Список
        int listTop = y + HEADER_H;
        int listBottom = y + h - 2 * (BTN_H + 2) - 4;
        if (mouseY < listTop || mouseY > listBottom) return false;
        if (mouseX < x || mouseX > x + w) return false;

        int rowH = OverlayConfig.playlistShowDesc ? 24 : 14;
        int visualIdx = (mouseY - (listTop + 2) + scroll * rowH) / rowH;
        int idx = visualIdx - 1; // -1 = библиотека

        MusicManager mm = MusicManager.INSTANCE;

        // Библиотека — только ЛКМ
        if (idx == -1) {
            if (button == 0) mm.setActivePlaylist(null);
            return true;
        }

        List<PlaylistInfo> list = PlaylistsManager.INSTANCE.playlists;
        if (idx < 0 || idx >= list.size()) return false;

        PlaylistInfo p = list.get(idx);

        // Кнопка редакции (ЛКМ, только для пользовательских)
        if (button == 0 && !p.builtin) {
            int ry = listTop + 2 + (idx + 1) * rowH - scroll * rowH;
            int editX = x + w - EDIT_BTN_W - 4;
            int editY = ry + (rowH - EDIT_BTN_W) / 2;

            if (mouseX >= editX && mouseX <= editX + EDIT_BTN_W
                    && mouseY >= editY && mouseY <= editY + EDIT_BTN_W) {
                Minecraft.getMinecraft().displayGuiScreen(
                        new GuiRenamePlaylist(parent, p.id));
                return true;
            }
        }

        // ПКМ по пользовательскому плейлисту — начать drag
        if (button == 1) {
            if (p.builtin) return true;
            draggingPlaylist = true;

            int userFrom = 0;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == p) break;
                if (!list.get(i).builtin) userFrom++;
            }
            dragPlaylistFrom = userFrom;
            dragPlaylistTo   = userFrom;
            dragPlaylistOffsetY = mouseY
                    - (listTop + 2 + (idx + 1) * rowH - scroll * rowH);
            return true;
        }

        // ЛКМ — выбрать
        mm.setActivePlaylist(p);

        if (mm.getCurrent() != null) {
            List<TrackInfo> queue = PlaylistsManager.INSTANCE.resolveTracks(p);
            if (!queue.contains(mm.getCurrent()) && !queue.isEmpty()) {
                mm.play(queue.get(0));
            }
        }
        return true;
    }

    public boolean handleScroll(int wheel) {
        if (wheel == 0) return false;
        List<PlaylistInfo> list = PlaylistsManager.INSTANCE.playlists;
        int rowH = OverlayConfig.playlistShowDesc ? 24 : 14;
        int visible = (h - HEADER_H - 2 * (BTN_H + 2) - 4) / rowH;
        int max = Math.max(0, (list.size() + 1) - visible);
        scroll -= Integer.signum(wheel);
        if (scroll < 0) scroll = 0;
        if (scroll > max) scroll = max;
        return true;
    }

    public boolean isMouseOver(int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // Импорт
    private void openImportDialog() {
        new Thread(() -> {
            try {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle(I18n.format("waimusic.guipp.import_title"));
                fc.setMultiSelectionEnabled(true);
                fc.setFileFilter(new FileNameExtensionFilter(
                        I18n.format("waimusic.guipp.import_filter"),
                        "json", "ogg", "mp3", "wav", "aiff", "aif"));

                int r = fc.showOpenDialog(null);
                if (r != JFileChooser.APPROVE_OPTION) return;

                File[] selected = fc.getSelectedFiles();
                if (selected == null || selected.length == 0) return;

                if (selected.length == 1
                        && selected[0].getName().toLowerCase().endsWith(".json")) {
                    Minecraft.getMinecraft().addScheduledTask(() ->
                            PlaylistsManager.INSTANCE.importPlaylist(selected[0]));
                    return;
                }

                final List<File> audio = new ArrayList<>();
                for (File f : selected) {
                    String n = f.getName().toLowerCase();
                    if (n.endsWith(".ogg") || n.endsWith(".mp3")
                            || n.endsWith(".wav") || n.endsWith(".aiff")
                            || n.endsWith(".aif")) {
                        audio.add(f);
                    }
                }

                if (audio.isEmpty()) return;

                String baseName = audio.get(0).getName();
                int dot = baseName.lastIndexOf('.');
                if (dot > 0) baseName = baseName.substring(0, dot);

                final String plName = baseName;
                String plId = plName.replaceAll("[^a-zA-Z0-9_\\-]", "_");

                String base = plId;
                int k = 1;
                while (PlaylistsManager.INSTANCE.getById(plId) != null) {
                    plId = base + "_" + (k++);
                }
                final String finalPlId = plId;

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    PlaylistsManager.INSTANCE.createPlaylist(finalPlId, plName,
                            I18n.format("waimusic.guipp.import_desc"));
                    Minecraft.getMinecraft().displayGuiScreen(
                            new GuiImportPlaylistWizard(
                                    GuiPlaylistPanel.this.parent,
                                    finalPlId, plName, audio));
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, "WAIMusic-PlaylistImport").start();
    }

    public void mouseDragged(int mouseX, int mouseY) {
        if (!draggingPlaylist) return;

        int listTop = y + HEADER_H;
        int rowH = OverlayConfig.playlistShowDesc ? 24 : 14;
        int relativeY = mouseY - (listTop + 2) + scroll * rowH;
        int visualIdx = relativeY / rowH;

        List<PlaylistInfo> list = PlaylistsManager.INSTANCE.playlists;
        int builtinCount = 0;
        for (PlaylistInfo p : list) if (p.builtin) builtinCount++;

        int userIdx = visualIdx - 1 - builtinCount;
        int userCount = list.size() - builtinCount;
        if (userIdx < 0) userIdx = 0;
        if (userIdx > userCount) userIdx = userCount;

        dragPlaylistTo = userIdx;
    }

    public void mouseReleasedAny(int mouseX, int mouseY, int button) {
        if (draggingPlaylist) {
            if (dragPlaylistFrom != dragPlaylistTo) {
                PlaylistsManager.INSTANCE.movePlaylist(dragPlaylistFrom, dragPlaylistTo);
            }
            draggingPlaylist = false;
            dragPlaylistFrom = -1;
            dragPlaylistTo   = -1;
        }
    }

    // Утилиты
    private static int argb(int rgb, int a) {
        return ((a & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}