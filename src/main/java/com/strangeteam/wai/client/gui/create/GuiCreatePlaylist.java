/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.gui.create;

import com.strangeteam.wai.client.managers.PlaylistsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import java.io.IOException;

/** Меню создания плейлиста */

public class GuiCreatePlaylist extends GuiScreen {

    private final GuiScreen parent;

    private GuiTextField fieldId;
    private GuiTextField fieldName;
    private GuiTextField fieldDesc;

    private String status = "";
    private int statusColor = 0xFFFFFF;

    private int cx;
    private static final int FIELD_W = 300;
    private static final int FIELD_H = 18;

    public GuiCreatePlaylist(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        cx = width / 2 - FIELD_W / 2;
        fieldId   = new GuiTextField(0, fontRenderer, cx, 70,  FIELD_W, FIELD_H);
        fieldName = new GuiTextField(1, fontRenderer, cx, 110, FIELD_W, FIELD_H);
        fieldDesc = new GuiTextField(2, fontRenderer, cx, 150, FIELD_W, FIELD_H);

        fieldId.setMaxStringLength(64);
        fieldName.setMaxStringLength(128);
        fieldDesc.setMaxStringLength(256);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "§l" + I18n.format("gui.wai.create_playlist.title"), width / 2, 20, 0xFFFFFF);
        drawCenteredString(fontRenderer, "§7" + I18n.format("gui.wai.create_playlist.hint"), width / 2, 34, 0xAAAAAA);

        drawString(fontRenderer, I18n.format("gui.wai.create_playlist.id_label"),          cx, 58,  0xFFFFFF); fieldId.drawTextBox();
        drawString(fontRenderer, I18n.format("gui.wai.create_playlist.name_label"),    cx, 98,  0xFFFFFF); fieldName.drawTextBox();
        drawString(fontRenderer, I18n.format("gui.wai.create_playlist.desc_label"),    cx, 138, 0xFFFFFF); fieldDesc.drawTextBox();

        int btnY = 190;
        boolean hover = isIn(mouseX, mouseY, cx, btnY, FIELD_W, 20);
        Gui.drawRect(cx, btnY, cx + FIELD_W, btnY + 20, hover ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, I18n.format("gui.wai.create_playlist.create_button"), width / 2, btnY + 6, 0xFFFFFF);

        if (!status.isEmpty()) {
            drawCenteredString(fontRenderer, status, width / 2, btnY + 26, statusColor);
        }

        int backX = width / 2 - 50;
        int backY = height - 30;
        boolean hb = isIn(mouseX, mouseY, backX, backY, 100, 20);
        Gui.drawRect(backX, backY, backX + 100, backY + 20, hb ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, I18n.format("gui.wai.create_playlist.back"), width / 2, backY + 6, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        fieldId.mouseClicked(mouseX, mouseY, mouseButton);
        fieldName.mouseClicked(mouseX, mouseY, mouseButton);
        fieldDesc.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) { super.mouseClicked(mouseX, mouseY, mouseButton); return; }

        int btnY = 190;
        if (isIn(mouseX, mouseY, cx, btnY, FIELD_W, 20)) {
            doCreate();
            return;
        }

        int backX = width / 2 - 50;
        int backY = height - 30;
        if (isIn(mouseX, mouseY, backX, backY, 100, 20)) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void doCreate() {
        String id = fieldId.getText().trim();
        String name = fieldName.getText().trim();
        String desc = fieldDesc.getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            status = "§c" + I18n.format("gui.wai.create_playlist.status.fill_required");
            statusColor = 0xFF5555;
            return;
        }

        String err = PlaylistsManager.INSTANCE.createPlaylist(id, name, desc);
        if (err == null) {
            String normId = id.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
            Minecraft.getMinecraft().displayGuiScreen(new GuiPlaylistTrackPicker(parent, normId));
        } else {
            status = "§c" + err;
            statusColor = 0xFF5555;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        fieldId.textboxKeyTyped(typedChar, keyCode);
        fieldName.textboxKeyTyped(typedChar, keyCode);
        fieldDesc.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        fieldId.updateCursorCounter();
        fieldName.updateCursorCounter();
        fieldDesc.updateCursorCounter();
        super.updateScreen();
    }

    private static boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}