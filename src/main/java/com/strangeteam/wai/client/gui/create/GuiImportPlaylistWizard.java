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
import com.strangeteam.wai.client.managers.UserTracksManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import com.strangeteam.wai.client.info.PlaylistInfo;
import com.strangeteam.wai.client.info.AudioMetadataUtil;
import com.strangeteam.wai.client.info.TranslitUtil;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Импорт плейлиста */

public class GuiImportPlaylistWizard extends GuiScreen {

    private final GuiScreen parent;
    private final List<File> files;
    private final String playlistId;
    private final String playlistName;

    private int index = 0;
    private final List<String> addedTrackIds = new ArrayList<>();

    private GuiTextField fieldId, fieldTitle, fieldArtist, fieldIcon;

    private String status = "";
    private int statusColor = 0xFFFFFF;

    private int cx;
    private static final int FIELD_W = 300;
    private static final int FIELD_H = 18;

    private static final int Y_ID     = 90;
    private static final int Y_TITLE  = 130;
    private static final int Y_ARTIST = 170;
    private static final int Y_ICON   = 210;
    private static final int Y_BTN    = 250;

    public GuiImportPlaylistWizard(GuiScreen parent, String playlistId,
                                   String playlistName, List<File> files) {
        this.parent = parent;
        this.playlistId = playlistId;
        this.playlistName = playlistName;
        this.files = files;
    }

    @Override
    public void initGui() {
        cx = width / 2 - FIELD_W / 2;
        fieldId     = new GuiTextField(0, fontRenderer, cx, Y_ID,     FIELD_W, FIELD_H);
        fieldTitle  = new GuiTextField(1, fontRenderer, cx, Y_TITLE,  FIELD_W, FIELD_H);
        fieldArtist = new GuiTextField(2, fontRenderer, cx, Y_ARTIST, FIELD_W, FIELD_H);
        fieldIcon   = new GuiTextField(3, fontRenderer, cx, Y_ICON,   FIELD_W, FIELD_H);

        fieldId.setMaxStringLength(64);
        fieldTitle.setMaxStringLength(128);
        fieldArtist.setMaxStringLength(128);
        fieldIcon.setMaxStringLength(512);

        prefillCurrent();
    }

    private void prefillCurrent() {
        if (index >= files.size()) return;
        File f = files.get(index);

        // Базовое имя файла без расширения — фолбэк для title и ID
        String base = f.getName();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);

        String title  = base;
        String artist = "";
        File   cover  = null;

        // Пытаемся прочитать метаданные (title, artist, обложка)
        AudioMetadataUtil.Meta meta = AudioMetadataUtil.read(f);
        if (meta != null) {
            if (meta.title  != null && !meta.title.trim().isEmpty())  title  = meta.title;
            if (meta.artist != null && !meta.artist.trim().isEmpty()) artist = meta.artist;
            cover = meta.coverFile;
        }

        // ID — транслитерация от title (кириллица → латиница, спецсимволы → _)
        fieldId.setText(TranslitUtil.makeId(title, 64));

        // Название и исполнитель — из тегов, с фолбэком
        fieldTitle.setText(title);
        fieldArtist.setText(artist);

        // Иконка — встроенная обложка, если есть
        fieldIcon.setText(cover != null ? cover.getAbsolutePath() : "");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawCenteredString(fontRenderer,
                "§l" + I18n.format("gui.wai.import_playlist.title", playlistName),
                width / 2, 15, 0xFFFFFF);
        drawCenteredString(fontRenderer,
                "§7" + I18n.format("gui.wai.import_playlist.progress", index + 1, files.size()),
                width / 2, 30, 0xAAAAAA);

        if (index < files.size()) {
            File f = files.get(index);
            drawString(fontRenderer, "§7" + I18n.format("gui.wai.import_playlist.file", f.getName()), cx, 55, 0xFFFFFF);
        }

        drawString(fontRenderer, I18n.format("gui.wai.import_playlist.id_label"), cx, Y_ID - 12, 0xFFFFFF);
        fieldId.drawTextBox();

        drawString(fontRenderer, I18n.format("gui.wai.import_playlist.name_label"), cx, Y_TITLE - 12, 0xFFFFFF);
        fieldTitle.drawTextBox();

        drawString(fontRenderer, I18n.format("gui.wai.import_playlist.artist_label"), cx, Y_ARTIST - 12, 0xFFFFFF);
        fieldArtist.drawTextBox();

        drawString(fontRenderer, I18n.format("gui.wai.import_playlist.icon_label"), cx, Y_ICON - 12, 0xFFFFFF);
        fieldIcon.drawTextBox();

        drawSmallButton(mouseX, mouseY, cx + FIELD_W + 4, Y_ICON, 60, FIELD_H, I18n.format("gui.wai.import_playlist.browse"));

        int bw = 140;
        int bx = width / 2 - bw - 10;
        drawButton(bx, Y_BTN, bw, 20, "§a" + I18n.format("gui.wai.import_playlist.add_next"), mouseX, mouseY);
        int bx2 = width / 2 + 10;
        drawButton(bx2, Y_BTN, bw, 20, I18n.format("gui.wai.import_playlist.skip"), mouseX, mouseY);

        if (!status.isEmpty()) {
            drawCenteredString(fontRenderer, status, width / 2, Y_BTN + 26, statusColor);
        }

        int backX = width / 2 - 50;
        int backY = height - 30;
        boolean hb = mouseX >= backX && mouseX <= backX + 100 && mouseY >= backY && mouseY <= backY + 20;
        Gui.drawRect(backX, backY, backX + 100, backY + 20, hb ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, I18n.format("gui.wai.import_playlist.cancel"), width / 2, backY + 6, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawButton(int x, int y, int w, int h, String label, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        Gui.drawRect(x, y, x + w, y + h, hover ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, label, x + w / 2, y + 6, 0xFFFFFF);
    }

    private void drawSmallButton(int mx, int my, int x, int y, int w, int h, String label) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        Gui.drawRect(x, y, x + w, y + h, hover ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFF);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        fieldId.mouseClicked(mouseX, mouseY, mouseButton);
        fieldTitle.mouseClicked(mouseX, mouseY, mouseButton);
        fieldArtist.mouseClicked(mouseX, mouseY, mouseButton);
        fieldIcon.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        int bw = 140;
        int bx = width / 2 - bw - 10;
        int bx2 = width / 2 + 10;

        if (isIn(mouseX, mouseY, bx, Y_BTN, bw, 20)) { doAddCurrent(); return; }
        if (isIn(mouseX, mouseY, bx2, Y_BTN, bw, 20)) { skipCurrent(); return; }

        if (isIn(mouseX, mouseY, cx + FIELD_W + 4, Y_ICON, 60, FIELD_H)) {
            openIconChooser();
            return;
        }

        int backX = width / 2 - 50;
        int backY = height - 30;
        if (isIn(mouseX, mouseY, backX, backY, 100, 20)) {
            finish(false);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void doAddCurrent() {
        if (index >= files.size()) return;
        File audio = files.get(index);

        String id = fieldId.getText().trim();
        String title = fieldTitle.getText().trim();
        String artist = fieldArtist.getText().trim();
        String iconPath = fieldIcon.getText().trim();

        if (id.isEmpty() || title.isEmpty()) {
            status = "§c" + I18n.format("gui.wai.import_playlist.status.fill_required");
            statusColor = 0xFF5555;
            return;
        }

        File icon = iconPath.isEmpty() ? null : new File(iconPath);
        if (icon != null && !icon.exists()) icon = null;

        String err = UserTracksManager.INSTANCE.addTrack(id, title, artist.isEmpty() ? I18n.format("gui.wai.import_playlist.unknown_artist") : artist, audio, icon);

        if (err != null) {
            status = "§c" + err;
            statusColor = 0xFF5555;
            return;
        }

        String normId = id.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        addedTrackIds.add(normId);
        index++;
        status = "";
        if (index < files.size()) {
            prefillCurrent();
        } else {
            finish(true);
        }
    }

    private void skipCurrent() {
        index++;
        if (index < files.size()) {
            prefillCurrent();
        } else {
            finish(!addedTrackIds.isEmpty());
        }
    }

    private void finish(boolean apply) {
        if (!apply) {
            // Откат: удаляем плейлист и все добавленные треки
            PlaylistsManager.INSTANCE.removePlaylist(playlistId);
            for (String tid : addedTrackIds) {
                UserTracksManager.INSTANCE.removeTrack(tid);
            }
            MusicManager.INSTANCE.reloadTracks();
            MusicManager.INSTANCE.setActivePlaylist(null);
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }

        if (addedTrackIds.isEmpty()) {
            // Нечего применять — тоже откакт
            PlaylistsManager.INSTANCE.removePlaylist(playlistId);
            MusicManager.INSTANCE.reloadTracks();
            MusicManager.INSTANCE.setActivePlaylist(null);
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }

        PlaylistInfo pl = PlaylistsManager.INSTANCE.getById(playlistId);
        if (pl != null && !pl.builtin) {
            pl.trackIds.clear();
            pl.trackIds.addAll(addedTrackIds);
            PlaylistsManager.INSTANCE.saveAll();
        }

        MusicManager.INSTANCE.reloadTracks();
        MusicManager.INSTANCE.setActivePlaylist(PlaylistsManager.INSTANCE.getById(playlistId));

        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    private void openIconChooser() {
        new Thread(() -> {
            try {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle(I18n.format("gui.wai.import_playlist.dialog.icon_title"));
                fc.setFileFilter(new FileNameExtensionFilter(
                        I18n.format("gui.wai.import_playlist.dialog.images"),
                        "png", "jpg", "jpeg", "bmp", "gif"));

                int r = fc.showOpenDialog(null);
                if (r == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    if (f != null) {
                        Minecraft.getMinecraft().addScheduledTask(() -> fieldIcon.setText(f.getAbsolutePath()));
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, "WAIMusic-IconChooser").start();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        fieldId.textboxKeyTyped(typedChar, keyCode);
        fieldTitle.textboxKeyTyped(typedChar, keyCode);
        fieldArtist.textboxKeyTyped(typedChar, keyCode);
        fieldIcon.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        fieldId.updateCursorCounter();
        fieldTitle.updateCursorCounter();
        fieldArtist.updateCursorCounter();
        fieldIcon.updateCursorCounter();
        super.updateScreen();
    }

    private static boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}