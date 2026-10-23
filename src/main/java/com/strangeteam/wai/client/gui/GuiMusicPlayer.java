/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.gui;

import com.strangeteam.wai.client.managers.MusicManager;
import com.strangeteam.wai.client.managers.PlaylistsManager;
import com.strangeteam.wai.client.gui.GuiFitScreen;
import com.strangeteam.wai.client.info.PlaylistInfo;
import com.strangeteam.wai.client.info.TrackInfo;
import com.strangeteam.wai.client.gui.cfg.OverlayConfig;
import com.strangeteam.wai.client.gui.create.GuiAddTrack;
import com.strangeteam.wai.client.gui.create.TextureBinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.resources.I18n;

import com.strangeteam.wai.client.managers.UserTracksManager;

import java.io.IOException;
import java.util.List;

/** Плеер */

public class GuiMusicPlayer extends GuiFitScreen {
    private int panelW() { return OverlayConfig.playlistWidth + 8; }
    @Override protected int designWidth()  { return panelW() + guiW + 8 + listW + 16; }
    @Override protected int designHeight() { return guiH + 24; }
    private int guiX, guiY;
    private final int guiW = 260, guiH = 220;

    private boolean draggingVolume = false;
    private int volBarX, volBarY;
    private final int volBarW = 100, volBarH = 10;

    private static final net.minecraft.util.ResourceLocation LOGO =
            new net.minecraft.util.ResourceLocation("waimusic", "textures/gui/waimusic.png");

    // кнопка mute
    private static final int MUTE_BTN_W = 16;
    private static final int MUTE_BTN_H = 10;
    private int muteBtnX, muteBtnY;

    private boolean draggingSeek = false;
    private int seekBarX, seekBarY;
    private final int seekBarW = 220, seekBarH = 8;

    private int listX, listY, listW = 160, listH;
    private int listScroll = 0;

    // скроллбар очереди
    private static final int SCROLLBAR_W = 4;
    private int  scrollBarX, scrollBarY, scrollBarH;
    private int  scrollThumbY, scrollThumbH;
    private boolean draggingScroll = false;
    private int  dragScrollOffsetY = 0;

    private boolean draggingTrack      = false;
    private int     dragTrackFromIndex = -1;
    private int     dragTrackToIndex   = -1;
    private int     dragMouseOffsetY   = 0;

    private int listRowH() { return 14; }

    private int visibleRows() {
        return Math.max(1, (listH - 4) / listRowH());
    }

    private int maxScroll() {
        int total = currentList().size();
        return Math.max(0, total - visibleRows());
    }

    // скролл
    private void clampScroll() {
        int max = maxScroll();
        if (listScroll < 0) listScroll = 0;
        if (listScroll > max) listScroll = max;
    }

    private void updateScrollThumb() {
        int total = currentList().size();
        int vis   = visibleRows();

        if (total <= vis) {
            scrollThumbY = scrollBarY;
            scrollThumbH = scrollBarH;
            return;
        }

        scrollThumbH = Math.max(16, scrollBarH * vis / total);
        int travel = scrollBarH - scrollThumbH;
        int max    = maxScroll();
        scrollThumbY = scrollBarY + (max == 0 ? 0 : travel * listScroll / max);
    }

    private GuiPlaylistPanel playlistPanel;

    @Override
    public void initGui() {
        int totalW = panelW() + guiW + 8 + listW;
        guiX = (width - totalW) / 2 + panelW();
        guiY = (height - guiH) / 2;

        playlistPanel = new GuiPlaylistPanel(this);
        playlistPanel.updateLayout(guiX, guiY, guiH);

        volBarX = guiX + 10;
        volBarY = guiY + guiH - 22;

        muteBtnX = volBarX + volBarW + 4;
        muteBtnY = volBarY;

        seekBarX = guiX + 20;
        seekBarY = guiY + 118;

        listX = guiX + guiW + 8;
        listY = guiY;
        listH = guiH;

        scrollBarX = listX + listW - SCROLLBAR_W;
        scrollBarY = listY;
        scrollBarH = listH;
    }

    private int argb(int rgb, int a) {
        return ((a & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    @Override
    public void drawContent(int mouseX, int mouseY, float partialTicks) {

        // LOGO
        int logoSize = 200; // в пикселях
        int logoX = guiX + (guiW - logoSize) / 2;
        int logoY = guiY + (guiH - logoSize) / 2;

        int lc = OverlayConfig.LogoColor;
        float r = ((lc >> 16) & 0xFF) / 255f;
        float g = ((lc >> 8)  & 0xFF) / 255f;
        float b = ( lc        & 0xFF) / 255f;

        float logoalpha = OverlayConfig.bgLogoAlpha / 255f;

        net.minecraft.client.renderer.GlStateManager.color(r, g, b,
                logoalpha);
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.blendFunc(
                org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();

        mc.getTextureManager().bindTexture(LOGO);

        Gui.drawModalRectWithCustomSizedTexture(
                logoX, logoY, 0, 0,
                logoSize, logoSize, logoSize, logoSize);

        net.minecraft.client.renderer.GlStateManager.color(1f, 1f, 1f, 1f);
        net.minecraft.client.renderer.GlStateManager.disableBlend();

        // фон
        int bg = argb(OverlayConfig.guiBgColor, OverlayConfig.guiBgAlpha);
        Gui.drawRect(guiX, guiY, guiX + guiW, guiY + guiH, bg);
        Gui.drawRect(guiX, guiY, guiX + guiW, guiY + 1,
                withAlpha(OverlayConfig.lineColor, 0xFF));
        Gui.drawRect(guiX, guiY + guiH - 1, guiX + guiW, guiY + guiH,
                withAlpha(OverlayConfig.lineColor, 0xFF));

        MusicManager mm = MusicManager.INSTANCE;
        TrackInfo cur = mm.getCurrent();

        if (cur != null) {
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableTexture2D();

            if (cur.iconFile != null || cur.icon != null) {
                TextureBinder.bindTrackIcon(cur);
                Gui.drawModalRectWithCustomSizedTexture(guiX + 12, guiY + 12, 0, 0, 64, 64, 64, 64);
            }
            Gui.drawModalRectWithCustomSizedTexture(guiX + 12, guiY + 12, 0, 0, 64, 64, 64, 64);

            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.disableBlend();

            drawString(fontRenderer, trim(cur.title, 180),  guiX + 84, guiY + 20,
                    OverlayConfig.guiTextColor);
            drawString(fontRenderer, "by " + trim(cur.artist, 170), guiX + 84, guiY + 34,
                    OverlayConfig.textColorSub);
        } else {
            drawString(fontRenderer, I18n.format("waimusic.guimp.no_track"), guiX + 84, guiY + 30,
                    OverlayConfig.guiTextColor);
        }

        long pos = mm.getPositionMs();
        long dur = mm.getDurationMs();
        drawString(fontRenderer, fmt(pos) + " / " + fmt(dur),
                guiX + 20, guiY + 100, OverlayConfig.guiTextColor);

        // Seek
        Gui.drawRect(seekBarX, seekBarY, seekBarX + seekBarW, seekBarY + seekBarH, 0xFF333333);
        if (dur > 0) {
            int fill = (int) ((double) pos / dur * seekBarW);
            Gui.drawRect(seekBarX, seekBarY, seekBarX + fill, seekBarY + seekBarH,
                    withAlpha(OverlayConfig.lineColor, 0xFF));
            Gui.drawRect(seekBarX + fill - 1, seekBarY - 2,
                    seekBarX + fill + 1, seekBarY + seekBarH + 2, 0xFFFFFFFF);
        }

        drawButton(guiX + 10,  guiY + 138, 40, 20, "◀◀", mouseX, mouseY);
        drawButton(guiX + 55,  guiY + 138, 40, 20,
                (mm.isPlaying() && !mm.isPaused()) ? "❚❚" : "▶", mouseX, mouseY);
        drawButton(guiX + 100, guiY + 138, 40, 20, "■",  mouseX, mouseY);
        drawButton(guiX + 145, guiY + 138, 40, 20, "▶▶", mouseX, mouseY);
        drawButton(guiX + 190, guiY + 138, 60, 20,
                mm.isLoop() ? "Loop: ON" : "Loop: OFF", mouseX, mouseY);

        drawButton(guiX + 5,   guiY + 162, 80, 20, I18n.format("waimusic.guimp.settings"), mouseX, mouseY);
        drawButton(guiX + 90,  guiY + 162, 80, 20, I18n.format("waimusic.guimp.author"),     mouseX, mouseY);
        drawButton(guiX + 175, guiY + 162, 80, 20, I18n.format("waimusic.guimp.add"),  mouseX, mouseY);

        drawString(fontRenderer, I18n.format("waimusic.guimp.volume"), volBarX, volBarY - 12, OverlayConfig.guiTextColor);
        Gui.drawRect(volBarX, volBarY, volBarX + volBarW, volBarY + volBarH,
                withAlpha(OverlayConfig.volumeBarColor, 0xFF));
        int volFill = (int) (mm.getVolume() * volBarW);
        Gui.drawRect(volBarX, volBarY, volBarX + volFill, volBarY + volBarH,
                withAlpha(OverlayConfig.volumeBarFillColor, 0xFF));
        Gui.drawRect(volBarX + volFill - 1, volBarY - 2,
                volBarX + volFill + 1, volBarY + volBarH + 2, 0xFFFFFFFF);

        drawMuteButton(muteBtnX, muteBtnY, MUTE_BTN_W, MUTE_BTN_H,
                mm.isMuted(), mouseX, mouseY);

        drawTrackList(mouseX, mouseY);
        drawString(fontRenderer,
                I18n.format("waimusic.guimp.podskazka1"),
                listX, listY + listH + 4, 0xAAAAAA);

        super.drawContent(mouseX, mouseY, partialTicks);
        if (playlistPanel != null) playlistPanel.draw(mouseX, mouseY);
    }

    private static int withAlpha(int rgb, int a) {
        return ((a & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private int firstUserIndex() {
        List<TrackInfo> list = MusicManager.INSTANCE.tracks;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isUserTrack()) return i;
        }
        return -1;
    }

    private int lastUserIndex() {
        List<TrackInfo> list = MusicManager.INSTANCE.tracks;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).isUserTrack()) return i;
        }
        return -1;
    }

    private void drawTrackList(int mouseX, int mouseY) {
        List<TrackInfo> list = MusicManager.INSTANCE.getActivePlaylist() != null
                ? PlaylistsManager.INSTANCE.resolveTracks(MusicManager.INSTANCE.getActivePlaylist())
                : MusicManager.INSTANCE.tracks;
        Gui.drawRect(listX, listY, listX + listW, listY + listH, 0xAA000000);
        Gui.drawRect(listX, listY, listX + listW, listY + 1,
                withAlpha(OverlayConfig.lineColor, 0xFF));

        int rowH = 14;
        int y = listY + 4 - listScroll * rowH;

        for (int i = 0; i < list.size(); i++) {
            // Пропускаем строку, которую тащат
            if (draggingTrack && i == dragTrackFromIndex) {
                y += rowH;
                continue;
            }

            if (y + rowH < listY || y > listY + listH) { y += rowH; continue; }

            TrackInfo t = list.get(i);
            boolean hover = mouseX >= listX && mouseX <= listX + listW
                    && mouseY >= y && mouseY <= y + rowH;
            boolean active = t == MusicManager.INSTANCE.getCurrent();

            // Встроенные — приглушённые, пользовательские — обычные
            int color;
            if (active) {
                color = OverlayConfig.queueTextColorPlaying;
            } else if (t.isUserTrack()) {
                color = hover ? OverlayConfig.queueTextColorHover : OverlayConfig.queueTextColor;
            } else {
                color = OverlayConfig.queueTextColorBuiltin;
            }

            drawString(fontRenderer, trim(t.title, listW - 8), listX + 4, y + 3, color);
            y += rowH;
        }

        if (draggingTrack) {
            int markerY = listY + 4 + dragTrackToIndex * rowH - listScroll * rowH;
            if (markerY >= listY && markerY <= listY + listH) {
                int queueBg = withAlpha(OverlayConfig.queuePanelBgColor, OverlayConfig.queuePanelBgAlpha);
                Gui.drawRect(listX, listY, listX + listW, listY + listH, queueBg);
            }
            if (dragTrackFromIndex >= 0 && dragTrackFromIndex < list.size()) {
                TrackInfo dragged = list.get(dragTrackFromIndex);
                int ghostY = mouseY - dragMouseOffsetY;
                Gui.drawRect(listX, ghostY, listX + listW, ghostY + rowH - 2, 0x88000000);
                drawString(fontRenderer, trim(dragged.title, listW - 8), listX + 4, ghostY + 3, 0xFFFFAA00);
            }
        }

        // СКРОЛЛБАР
        updateScrollThumb();

        int trackColor = withAlpha(OverlayConfig.lineColor, 0x55);
        int thumbColor = withAlpha(OverlayConfig.lineColor, 0xFF);

        Gui.drawRect(scrollBarX, scrollBarY, scrollBarX + SCROLLBAR_W, scrollBarY + scrollBarH, trackColor);
        Gui.drawRect(scrollBarX, scrollThumbY, scrollBarX + SCROLLBAR_W, scrollThumbY + scrollThumbH, thumbColor);
    }

    private List<TrackInfo> currentList() {
        MusicManager mm = MusicManager.INSTANCE;
        if (mm.getActivePlaylist() != null) {
            List<TrackInfo> l = PlaylistsManager.INSTANCE.resolveTracks(mm.getActivePlaylist());
            if (!l.isEmpty()) return l;
        }
        return mm.tracks;
    }

    private String trim(String s, int maxWidth) {
        return fontRenderer.trimStringToWidth(s, maxWidth);
    }

    private void drawMuteButton(int x, int y, int w, int h,
                                boolean muted, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;

        int base;
        int border;
        int icon;

        if (muted) {
            base   = withAlpha(OverlayConfig.guiButtonColor, 0xFF);
            border = withAlpha(OverlayConfig.lineColor, 0xFF);
            icon   = withAlpha(OverlayConfig.lineColor, 0xFF);
        } else {
            base   = hover
                    ? withAlpha(OverlayConfig.guiButtonHover, 0xFF)
                    : withAlpha(OverlayConfig.guiButtonColor, 0xFF);
            border = 0xFF444444;
            icon   = OverlayConfig.guiButtonText;
        }

        Gui.drawRect(x, y, x + w, y + h, base);
        Gui.drawRect(x, y, x + w, y + 1, border);

        String glyph = muted ? "✖" : "♬";
        drawCenteredString(fontRenderer, glyph, x + w / 2, y + (h - 8) / 2, icon);
    }

    private void drawButton(int x, int y, int w, int h, String label, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int base  = withAlpha(OverlayConfig.guiButtonColor, 0xFF);
        int hoverC = withAlpha(OverlayConfig.guiButtonHover, 0xFF);
        Gui.drawRect(x, y, x + w, y + h, hover ? hoverC : base);
        Gui.drawRect(x, y, x + w, y + 1, 0xFF444444);
        drawCenteredString(fontRenderer, label, x + w / 2, y + (h - 8) / 2, OverlayConfig.guiButtonText);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Удаление активного плейлиста
        if (keyCode == org.lwjgl.input.Keyboard.KEY_BACK
                || keyCode == org.lwjgl.input.Keyboard.KEY_DELETE) {
            if (playlistPanel != null && playlistPanel.handleKey(keyCode)) {
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0 && mouseButton != 1) return;
        MusicManager mm = MusicManager.INSTANCE;

        // клик по скроллбару очереди
        if (mouseButton == 0
                && mouseX >= scrollBarX && mouseX <= scrollBarX + SCROLLBAR_W
                && mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarH) {

            updateScrollThumb();

            if (mouseY >= scrollThumbY && mouseY <= scrollThumbY + scrollThumbH) {
                draggingScroll = true;
                dragScrollOffsetY = mouseY - scrollThumbY;
            } else {
                int total = currentList().size();
                int vis   = visibleRows();
                int max   = maxScroll();
                if (total > vis && max > 0) {
                    int travel = scrollBarH - scrollThumbH;
                    int rel    = mouseY - scrollBarY - scrollThumbH / 2;
                    if (travel > 0) {
                        listScroll = Math.round((float) rel / travel * max);
                        clampScroll();
                    }
                }
            }
            return;
        }

        if (playlistPanel != null && playlistPanel.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }

        if (isIn(mouseX, mouseY, muteBtnX, muteBtnY, MUTE_BTN_W, MUTE_BTN_H)) {
            mm.toggleMute();
            return;
        }

        if (isIn(mouseX, mouseY, volBarX, volBarY, volBarW, volBarH)) {
            draggingVolume = true; updateVolumeFromMouse(mouseX); return;
        }
        if (isIn(mouseX, mouseY, seekBarX, seekBarY, seekBarW, seekBarH)) {
            draggingSeek = true; updateSeekFromMouse(mouseX); return;
        }
        if (isIn(mouseX, mouseY, guiX + 10,  guiY + 138, 40, 20)) { mm.prev();        return; }
        if (isIn(mouseX, mouseY, guiX + 55,  guiY + 138, 40, 20)) { mm.togglePause(); return; }
        if (isIn(mouseX, mouseY, guiX + 100, guiY + 138, 40, 20)) { mm.stop();        return; }
        if (isIn(mouseX, mouseY, guiX + 145, guiY + 138, 40, 20)) { mm.next();        return; }
        if (isIn(mouseX, mouseY, guiX + 190, guiY + 138, 60, 20)) { mm.setLoop(!mm.isLoop()); return; }

        // Настройки
        if (isIn(mouseX, mouseY, guiX + 5, guiY + 162, 80, 20)) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiOverlaySettings(this));
            return;
        }
        // Автор
        if (isIn(mouseX, mouseY, guiX + 90, guiY + 162, 80, 20)) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiCredits(this));
            return;
        }
        // Добавка треков
        if (isIn(mouseX, mouseY, guiX + 175, guiY + 162, 80, 20)) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiAddTrack(this));
            return;
        }

        if (mouseX >= listX && mouseX <= listX + listW
                && mouseY >= listY && mouseY <= listY + listH) {
            int rowH = 14;
            int idx = (mouseY - (listY + 4) + listScroll * rowH) / rowH;

            List<TrackInfo> list = currentList();
            if (idx >= 0 && idx < list.size()) {
                TrackInfo t = list.get(idx);
                PlaylistInfo active = mm.getActivePlaylist();

                // Drag: ПКМ или Shift+ЛКМ
                boolean wantDrag = (mouseButton == 1) || isShiftKeyDown();

                if (wantDrag && t.isUserTrack()) {
                    // Во встроенном плейлисте drag запрещён
                    if (active != null && active.builtin) return;

                    draggingTrack = true;
                    dragTrackFromIndex = idx;
                    dragTrackToIndex = idx;
                    int rowTop = listY + 4 + idx * rowH - listScroll * rowH;
                    dragMouseOffsetY = mouseY - rowTop;
                    return;
                }

                mm.play(t);
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void updateScrollFromMouse(int mouseY) {
        int max = maxScroll();
        if (max <= 0) return;

        int travel = scrollBarH - scrollThumbH;
        if (travel <= 0) return;

        int rel = mouseY - dragScrollOffsetY - scrollBarY;
        if (rel < 0) rel = 0;
        if (rel > travel) rel = travel;

        listScroll = Math.round((float) rel / travel * max);
        clampScroll();
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (playlistPanel != null) playlistPanel.mouseDragged(mouseX, mouseY);

        if (draggingTrack)  updateDragTarget(mouseY);
        if (draggingScroll) updateScrollFromMouse(mouseY);
        if (draggingVolume) updateVolumeFromMouse(mouseX);
        if (draggingSeek)   updateSeekFromMouse(mouseX);
    }

    private void updateDragTarget(int mouseY) {
        int rowH = 14;
        int relativeY = mouseY - (listY + 4) + listScroll * rowH;
        int idx = relativeY / rowH;

        PlaylistInfo active = MusicManager.INSTANCE.getActivePlaylist();

        if (active != null) {
            if (active.builtin) { dragTrackToIndex = dragTrackFromIndex; return; }
            int size = active.trackIds.size();
            if (idx < 0) idx = 0;
            if (idx > size) idx = size;
            dragTrackToIndex = idx;
            return;
        }

        int first = firstUserIndex();
        int last  = lastUserIndex();
        if (first < 0 || last < 0) { dragTrackToIndex = dragTrackFromIndex; return; }
        if (idx < first)    idx = first;
        if (idx > last + 1) idx = last + 1;
        dragTrackToIndex = idx;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (playlistPanel != null) playlistPanel.mouseReleasedAny(mouseX, mouseY, state);

        if (draggingTrack) {
            applyTrackReorder();
            draggingTrack = false;
            dragTrackFromIndex = -1;
            dragTrackToIndex = -1;
        }
        draggingVolume = false;
        draggingScroll = false;
        draggingSeek   = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    private void applyTrackReorder() {
        PlaylistsManager pm = PlaylistsManager.INSTANCE;
        MusicManager mm = MusicManager.INSTANCE;

        PlaylistInfo active = mm.getActivePlaylist();

        if (active != null) {
            if (active.builtin) return;
            if (dragTrackFromIndex < 0 || dragTrackFromIndex >= active.trackIds.size()) return;

            int to = dragTrackToIndex;
            if (to < 0) to = 0;
            if (to > active.trackIds.size()) to = active.trackIds.size();
            if (to == dragTrackFromIndex || to == dragTrackFromIndex + 1) return;

            pm.moveTrackInPlaylist(active.id, dragTrackFromIndex, to);
            return;
        }

        List<TrackInfo> list = mm.tracks;
        if (dragTrackFromIndex < 0 || dragTrackFromIndex >= list.size()) return;

        int to = dragTrackToIndex;
        if (to < 0) to = 0;
        if (to > list.size()) to = list.size();
        if (to == dragTrackFromIndex || to == dragTrackFromIndex + 1) return;

        TrackInfo moved = list.remove(dragTrackFromIndex);
        if (to > dragTrackFromIndex) to--;
        list.add(to, moved);

        UserTracksManager.INSTANCE.saveUserOrder(list);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mx = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
            int my = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

            if (playlistPanel != null && playlistPanel.isMouseOver(mx, my)) {
                playlistPanel.handleScroll(wheel);
                return;
            }

            listScroll -= Integer.signum(wheel);
            clampScroll();
        }
    }

    private void updateVolumeFromMouse(int mx) {
        float v = (float) (mx - volBarX) / volBarW;
        MusicManager.INSTANCE.setVolume(v);
    }

    private void updateSeekFromMouse(int mx) {
        long dur = MusicManager.INSTANCE.getDurationMs();
        if (dur <= 0) return;
        float f = (float) (mx - seekBarX) / seekBarW;
        f = Math.max(0f, Math.min(1f, f));
        MusicManager.INSTANCE.seek((long) (f * dur));
    }

    private boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String fmt(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}