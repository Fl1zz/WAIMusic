/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.gui.create;

import com.strangeteam.wai.client.managers.MusicManager;
import com.strangeteam.wai.client.gui.GuiFitScreen;
import com.strangeteam.wai.client.info.TrackInfo;
import com.strangeteam.wai.client.managers.UserTracksManager;
import com.strangeteam.wai.client.info.AudioMetadataUtil;
import com.strangeteam.wai.client.info.TranslitUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Canvas;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Меню добавки треков */

public class GuiAddTrack extends GuiFitScreen {
    @Override protected int designWidth()  { return FIELD_W + 4 + 60 + 16; }
    @Override protected int designHeight() { return 370; }

    private final GuiScreen parent;

    private GuiTextField fieldId;
    private GuiTextField fieldTitle;
    private GuiTextField fieldArtist;
    private GuiTextField fieldAudioPath;
    private GuiTextField fieldIconPath;

    private String statusMsg = "";
    private int statusColor = 0xFFFFFF;

    private int scroll = 0;

    // Куда пишем путь при drag-n-drop: 0 = auto, 1 = audio, 2 = icon
    private int dropTarget = 0;

    private DropTarget awtDropTarget;

    private final List<String> userTrackIds = new ArrayList<>();

    // Геометрия
    private int cx;
    private static final int FIELD_W = 300;
    private static final int FIELD_H = 18;

    private static final int Y_ID       = 60;
    private static final int Y_TITLE    = 95;
    private static final int Y_ARTIST   = 130;
    private static final int Y_AUDIO    = 165;
    private static final int Y_ICON     = 200;

    private static final int Y_ADD_BTN  = 240;
    private static final int ADD_BTN_H  = 20;

    private static final int Y_HINT     = 264;
    private static final int Y_LIST_HDR = 282;
    private static final int Y_LIST     = 296;
    private static final int ROW_H      = 18;

    public GuiAddTrack(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        int groupW = FIELD_W + 4 + 60;

        cx = width / 2 - FIELD_W / 2;
        int maxCx = width - groupW - 8;
        if (cx > maxCx) cx = maxCx;

        fieldId        = new GuiTextField(0, fontRenderer, cx, Y_ID,     FIELD_W, FIELD_H);
        fieldTitle     = new GuiTextField(1, fontRenderer, cx, Y_TITLE,  FIELD_W, FIELD_H);
        fieldArtist    = new GuiTextField(2, fontRenderer, cx, Y_ARTIST, FIELD_W, FIELD_H);
        fieldAudioPath = new GuiTextField(3, fontRenderer, cx, Y_AUDIO,  FIELD_W, FIELD_H);
        fieldIconPath  = new GuiTextField(4, fontRenderer, cx, Y_ICON,   FIELD_W, FIELD_H);

        fieldId.setMaxStringLength(64);
        fieldTitle.setMaxStringLength(128);
        fieldArtist.setMaxStringLength(128);
        fieldAudioPath.setMaxStringLength(512);
        fieldIconPath.setMaxStringLength(512);

        fieldAudioPath.setText(UserTracksManager.INSTANCE.getMusicDir().getAbsolutePath() + File.separator);
        fieldIconPath.setText(UserTracksManager.INSTANCE.getIconsDir().getAbsolutePath() + File.separator);

        refreshUserList();

        installDropTarget();
    }

    private void installDropTarget() {
        try {
            if (Display.getParent() instanceof Canvas) {
                Canvas canvas = (Canvas) Display.getParent();

                awtDropTarget = new DropTarget(canvas, DnDConstants.ACTION_COPY,
                        new DropTargetAdapter() {
                            @Override
                            public void drop(DropTargetDropEvent dtde) {
                                handleAwtDrop(dtde);
                            }
                        }, true);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void handleAwtDrop(DropTargetDropEvent dtde) {
        try {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                List<File> droppedFiles = (List<File>) dtde.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);

                if (droppedFiles != null && !droppedFiles.isEmpty()) {
                    final File f = droppedFiles.get(0);

                    Minecraft.getMinecraft().addScheduledTask(() -> processDroppedFile(f));
                }
                dtde.dropComplete(true);
            } else {
                dtde.rejectDrop();
            }
        } catch (Exception e) {
            e.printStackTrace();
            dtde.rejectDrop();
        }
    }

    // Интересный факт: я пытался сделать перетаскивание файлов в область пути, но не вышло. Будет иначе
    protected void processDroppedFile(File file) {
        if (file == null || !file.isFile()) return;

        String name = file.getName().toLowerCase();
        boolean isImage = name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".bmp")
                || name.endsWith(".gif");
        boolean isAudio = name.endsWith(".ogg") || name.endsWith(".mp3")
                || name.endsWith(".wav") || name.endsWith(".aiff")
                || name.endsWith(".aif");

        int target = dropTarget;
        if (target == 0) {
            if (isImage)      target = 2;
            else if (isAudio) target = 1;
            else              target = 1;
        }

        String path = file.getAbsolutePath();

        if (target == 2) {
            fieldIconPath.setText(path);
            setStatus("§a" + I18n.format("gui.wai.add_track.status.icon", file.getName()), 0x55FF55);
            return;
        }

        // target == 1 — аудио
        fieldAudioPath.setText(path);

        // Базовое имя файла без расширения — фолбэк
        String base = file.getName();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);

        String title  = base;
        String artist = "";
        File   cover  = null;

        // Пытаемся прочитать теги
        AudioMetadataUtil.Meta meta = AudioMetadataUtil.read(file);
        if (meta != null) {
            if (meta.title  != null && !meta.title.trim().isEmpty())  title  = meta.title;
            if (meta.artist != null && !meta.artist.trim().isEmpty()) artist = meta.artist;
            cover = meta.coverFile;
        }

        // Заполняем только пустые поля
        if (fieldId.getText().trim().isEmpty()) {
            fieldId.setText(TranslitUtil.makeId(title, 64));
        }
        if (fieldTitle.getText().trim().isEmpty()) {
            fieldTitle.setText(title);
        }
        if (fieldArtist.getText().trim().isEmpty() && !artist.isEmpty()) {
            fieldArtist.setText(artist);
        }

        // Обложка — только если поле иконки пустое и обложка есть
        boolean coverFromTags = false;
        if (cover != null && fieldIconPath.getText().trim().isEmpty()) {
            fieldIconPath.setText(cover.getAbsolutePath());
            coverFromTags = true;
        }

        // Статус
        String status;
        if (!artist.isEmpty() && coverFromTags) {
            status = I18n.format("gui.wai.add_track.status.audio_meta_cover", file.getName(), title, artist);
        } else if (!artist.isEmpty()) {
            status = I18n.format("gui.wai.add_track.status.audio_meta", file.getName(), title, artist);
        } else if (coverFromTags) {
            status = I18n.format("gui.wai.add_track.status.audio_cover", file.getName());
        } else {
            status = I18n.format("gui.wai.add_track.status.audio", file.getName());
        }
        setStatus("§a" + status, 0x55FF55);
    }

    // Отрисовка
    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawCenteredString(fontRenderer, "§l" + I18n.format("gui.wai.add_track.title"), width / 2, 15, 0xFFFFFF);
        drawCenteredString(fontRenderer,
                "§7" + I18n.format("gui.wai.add_track.formats"),
                width / 2, 30, 0xAAAAAA);

        drawString(fontRenderer, I18n.format("gui.wai.add_track.id_label"), cx, Y_ID - 12, 0xFFFFFF);
        fieldId.drawTextBox();

        drawString(fontRenderer, I18n.format("gui.wai.add_track.name_label"), cx, Y_TITLE - 12, 0xFFFFFF);
        fieldTitle.drawTextBox();

        drawString(fontRenderer, I18n.format("gui.wai.add_track.artist_label"), cx, Y_ARTIST - 12, 0xFFFFFF);
        fieldArtist.drawTextBox();

        drawString(fontRenderer, I18n.format("gui.wai.add_track.audio_label"), cx, Y_AUDIO - 12, 0xFFFFFF);
        fieldAudioPath.drawTextBox();

        drawString(fontRenderer, I18n.format("gui.wai.add_track.icon_label"), cx, Y_ICON - 12, 0xFFFFFF);
        fieldIconPath.drawTextBox();

        // Кнопки "Обзор..."
        drawSmallButton(mouseX, mouseY, cx + FIELD_W + 4, Y_AUDIO, 60, FIELD_H, I18n.format("gui.wai.add_track.browse"));
        drawSmallButton(mouseX, mouseY, cx + FIELD_W + 4, Y_ICON,  60, FIELD_H, I18n.format("gui.wai.add_track.browse"));

        // Кнопка "Добавить"
        boolean hoverAdd = isIn(mouseX, mouseY, cx, Y_ADD_BTN, FIELD_W, ADD_BTN_H);
        Gui.drawRect(cx, Y_ADD_BTN, cx + FIELD_W, Y_ADD_BTN + ADD_BTN_H,
                hoverAdd ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, I18n.format("gui.wai.add_track.add_button"),
                width / 2, Y_ADD_BTN + (ADD_BTN_H - 8) / 2, 0xFFFFFF);

        if (!statusMsg.isEmpty()) {
            drawCenteredString(fontRenderer, statusMsg, width / 2, Y_ADD_BTN + 24, statusColor);
        }

        drawCenteredString(fontRenderer, "§l" + I18n.format("gui.wai.add_track.user_tracks"), width / 2, Y_LIST_HDR, 0xFFFFFF);

        int visibleBottom = height - 40;
        for (int i = 0; i < userTrackIds.size(); i++) {
            int y = Y_LIST + i * ROW_H - scroll;
            if (y + ROW_H < Y_LIST || y > visibleBottom) continue;

            String id = userTrackIds.get(i);
            int rx = cx;
            int rw = FIELD_W - 40;

            boolean hoverRow = isIn(mouseX, mouseY, rx, y, rw, ROW_H - 2);
            Gui.drawRect(rx, y, rx + rw, y + ROW_H - 2, hoverRow ? 0x44FFFFFF : 0x22000000);
            drawString(fontRenderer, id, rx + 4, y + 4, 0xFFFFFF);

            int dx = cx + rw + 4;
            boolean hoverDel = isIn(mouseX, mouseY, dx, y, 30, ROW_H - 2);
            Gui.drawRect(dx, y, dx + 30, y + ROW_H - 2, hoverDel ? 0xFFAA2222 : 0xFF661111);
            drawCenteredString(fontRenderer, "X", dx + 15, y + 4, 0xFFFFFF);
        }

        int backX = width / 2 - 50;
        int backY = height - 30;
        boolean hoverBack = isIn(mouseX, mouseY, backX, backY, 100, 20);
        Gui.drawRect(backX, backY, backX + 100, backY + 20, hoverBack ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, I18n.format("gui.wai.add_track.back"), width / 2, backY + 6, 0xFFFFFF);

        super.drawContent(mouseX, mouseY, partialTicks);
    }

    private void drawSmallButton(int mouseX, int mouseY, int x, int y, int w, int h, String label) {
        boolean hover = isIn(mouseX, mouseY, x, y, w, h);
        Gui.drawRect(x, y, x + w, y + h, hover ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFF);
    }

    // Ввод
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        fieldId.mouseClicked(mouseX, mouseY, mouseButton);
        fieldTitle.mouseClicked(mouseX, mouseY, mouseButton);
        fieldArtist.mouseClicked(mouseX, mouseY, mouseButton);
        fieldAudioPath.mouseClicked(mouseX, mouseY, mouseButton);
        fieldIconPath.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) return;

        if (isIn(mouseX, mouseY, cx + FIELD_W + 70, Y_AUDIO, 45, FIELD_H)) {
            dropTarget = 1; return;
        }
        if (isIn(mouseX, mouseY, cx + FIELD_W + 70, Y_ICON, 45, FIELD_H)) {
            dropTarget = 2; return;
        }

        if (isIn(mouseX, mouseY, cx + FIELD_W + 4, Y_AUDIO, 60, FIELD_H)) {
            openFileChooser(false); return;
        }
        if (isIn(mouseX, mouseY, cx + FIELD_W + 4, Y_ICON, 60, FIELD_H)) {
            openFileChooser(true); return;
        }

        if (isIn(mouseX, mouseY, cx, Y_ADD_BTN, FIELD_W, ADD_BTN_H)) {
            doAdd();
            return;
        }

        int rw = FIELD_W - 40;
        for (int i = 0; i < userTrackIds.size(); i++) {
            int y = Y_LIST + i * ROW_H - scroll;
            int dx = cx + rw + 4;
            if (isIn(mouseX, mouseY, dx, y, 30, ROW_H - 2)) {
                doRemove(userTrackIds.get(i));
                return;
            }
        }

        int backX = width / 2 - 50;
        int backY = height - 30;
        if (isIn(mouseX, mouseY, backX, backY, 100, 20)) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    // Swing FileChooser
    private void openFileChooser(boolean forIcon) {
        new Thread(() -> {
            try {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle(I18n.format(forIcon ? "gui.wai.add_track.dialog.icon_title" : "gui.wai.add_track.dialog.audio_title"));
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

                if (forIcon) {
                    fc.setFileFilter(new FileNameExtensionFilter(
                            I18n.format("gui.wai.add_track.dialog.images"),
                            "png", "jpg", "jpeg", "bmp", "gif"));
                } else {
                    fc.setFileFilter(new FileNameExtensionFilter(
                            I18n.format("gui.wai.add_track.dialog.audio"),
                            "ogg", "mp3", "wav", "aiff", "aif"));
                }

                int result = fc.showOpenDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) return;

                File selected = fc.getSelectedFile();
                if (selected == null) return;

                final String path  = selected.getAbsolutePath();
                final String fname = selected.getName();

                final String base;
                {
                    String b = fname;
                    int d = b.lastIndexOf('.');
                    if (d > 0) b = b.substring(0, d);
                    base = b;
                }

                String title  = base;
                String artist = "";
                File   cover  = null;

                if (!forIcon) {
                    AudioMetadataUtil.Meta meta = AudioMetadataUtil.read(selected);
                    if (meta != null) {
                        if (meta.title  != null && !meta.title.trim().isEmpty())  title  = meta.title;
                        if (meta.artist != null && !meta.artist.trim().isEmpty()) artist = meta.artist;
                        cover = meta.coverFile;
                    }
                }

                final String fTitle  = title;
                final String fArtist = artist;
                final File   fCover  = cover;
                final boolean fIsIcon = forIcon;

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (fIsIcon) {
                        fieldIconPath.setText(path);
                        setStatus("§a" + I18n.format("gui.wai.add_track.status.icon", fname), 0x55FF55);
                        return;
                    }

                    fieldAudioPath.setText(path);

                    // Заполняем только пустые поля
                    if (fieldId.getText().trim().isEmpty()) {
                        fieldId.setText(TranslitUtil.makeId(fTitle, 64));
                    }
                    if (fieldTitle.getText().trim().isEmpty()) {
                        fieldTitle.setText(fTitle);
                    }
                    if (fieldArtist.getText().trim().isEmpty() && !fArtist.isEmpty()) {
                        fieldArtist.setText(fArtist);
                    }

                    // Обложка из тегов — только если поле иконки пустое
                    boolean coverFromTags = false;
                    if (fCover != null && fieldIconPath.getText().trim().isEmpty()) {
                        fieldIconPath.setText(fCover.getAbsolutePath());
                        coverFromTags = true;
                    }

                    // Статус
                    String status;
                    if (!fArtist.isEmpty() && coverFromTags) {
                        status = I18n.format("gui.wai.add_track.status.audio_meta_cover", fname, fTitle, fArtist);
                    } else if (!fArtist.isEmpty()) {
                        status = I18n.format("gui.wai.add_track.status.audio_meta", fname, fTitle, fArtist);
                    } else if (coverFromTags) {
                        status = I18n.format("gui.wai.add_track.status.audio_cover", fname);
                    } else {
                        status = I18n.format("gui.wai.add_track.status.audio", fname);
                    }
                    setStatus("§a" + status, 0x55FF55);
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, "WAIMusic-FileChooser").start();
    }

    // Добавление / удаление
    private void doAdd() {
        String id     = fieldId.getText().trim();
        String title  = fieldTitle.getText().trim();
        String artist = fieldArtist.getText().trim();
        String audio  = fieldAudioPath.getText().trim();
        String icon   = fieldIconPath.getText().trim();

        if (id.isEmpty() || title.isEmpty() || audio.isEmpty()) {
            setStatus("§c" + I18n.format("gui.wai.add_track.status.fill_required"), 0xFF5555);
            return;
        }

        File audioFile = new File(audio);
        if (!audioFile.exists() || !audioFile.isFile()) {
            setStatus("§c" + I18n.format("gui.wai.add_track.status.audio_not_found", audio), 0xFF5555);
            return;
        }

        String lower = audioFile.getName().toLowerCase();
        if (!(lower.endsWith(".ogg") || lower.endsWith(".mp3")
                || lower.endsWith(".wav") || lower.endsWith(".aiff")
                || lower.endsWith(".aif"))) {
            setStatus("§c" + I18n.format("gui.wai.add_track.status.format_unsupported"), 0xFF5555);
            return;
        }

        File iconFile = icon.isEmpty() ? null : new File(icon);
        if (iconFile != null && !iconFile.exists()) iconFile = null;

        String err = UserTracksManager.INSTANCE.addTrack(id, title, artist, audioFile, iconFile);
        if (err == null) {
            MusicManager.INSTANCE.reloadTracks();
            refreshUserList();
            setStatus("§a" + I18n.format("gui.wai.add_track.status.added"), 0x55FF55);
            fieldId.setText("");
            fieldTitle.setText("");
            fieldArtist.setText("");
        } else {
            setStatus("§c" + err, 0xFF5555);
        }
    }

    private void doRemove(String id) {
        boolean ok = UserTracksManager.INSTANCE.removeTrack(id);
        if (ok) {
            MusicManager.INSTANCE.reloadTracks();
            refreshUserList();
            setStatus("§a" + I18n.format("gui.wai.add_track.status.removed"), 0x55FF55);
        } else {
            setStatus("§c" + I18n.format("gui.wai.add_track.status.remove_error"), 0xFF5555);
        }
    }

    private void setStatus(String msg, int color) {
        this.statusMsg = msg;
        this.statusColor = color;
    }

    private void refreshUserList() {
        userTrackIds.clear();
        for (TrackInfo t : MusicManager.INSTANCE.tracks) {
            if (t.isUserTrack()) userTrackIds.add(t.id);
        }
    }

    // Клавиатура / тик / скролл
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        fieldId.textboxKeyTyped(typedChar, keyCode);
        fieldTitle.textboxKeyTyped(typedChar, keyCode);
        fieldArtist.textboxKeyTyped(typedChar, keyCode);
        fieldAudioPath.textboxKeyTyped(typedChar, keyCode);
        fieldIconPath.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        fieldId.updateCursorCounter();
        fieldTitle.updateCursorCounter();
        fieldArtist.updateCursorCounter();
        fieldAudioPath.updateCursorCounter();
        fieldIconPath.updateCursorCounter();
        super.updateScreen();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll -= Integer.signum(wheel) * ROW_H;
            int visibleRows = Math.max(1, (height - 40 - Y_LIST) / ROW_H);
            int maxScroll = Math.max(0, (userTrackIds.size() - visibleRows) * ROW_H);
            if (scroll < 0) scroll = 0;
            if (scroll > maxScroll) scroll = maxScroll;
        }
    }

    @Override
    public void onGuiClosed() {
        try {
            if (awtDropTarget != null) {
                awtDropTarget.setComponent(null);
                awtDropTarget = null;
            }
        } catch (Throwable ignored) {}

        super.onGuiClosed();
    }

    private boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}