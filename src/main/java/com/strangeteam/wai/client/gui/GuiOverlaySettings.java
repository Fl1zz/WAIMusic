/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.gui;

import com.strangeteam.wai.client.gui.cfg.OverlayConfig;
import com.strangeteam.wai.client.gui.cfg.OverlayConfigIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import net.minecraft.client.resources.I18n;


import com.strangeteam.wai.client.managers.AudioMixerManager;
import com.strangeteam.wai.client.managers.MusicManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/** Настройки оверлея/плеера */

public class GuiOverlaySettings extends GuiScreen {

    private final GuiScreen parent;
    private int scroll = 0;

    private final List<Widget> widgets = new ArrayList<>();
    private Widget dragging = null;

    private ColorWidget focusedColor = null;

    public GuiOverlaySettings(GuiScreen parent) {
        this.parent = parent;
    }

    // Виджеты
    private abstract static class Widget {
        int x, y;
        int baseY;
        int w, h;

        abstract void draw(GuiOverlaySettings gui, int mouseX, int mouseY);
        void onClick(int mx, int my) {}
        void onDrag(int mx, int my) {}

        boolean isIn(int mx, int my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        static boolean isIn(int mx, int my, int rx, int ry, int rw, int rh) {
            return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
        }

        void updateScreenY(int scroll) { this.y = baseY - scroll; }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (focusedColor != null) {
            if (focusedColor.handleKey(typedChar, keyCode)) {
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    private static class ToggleWidget extends Widget {
        String label;
        java.util.function.BooleanSupplier getter;
        Runnable action;

        ToggleWidget(String label, java.util.function.BooleanSupplier getter, Runnable action) {
            this.label = label; this.getter = getter; this.action = action;
            this.w = 300; this.h = 16;
        }

        @Override
        void draw(GuiOverlaySettings gui, int mouseX, int mouseY) {
            boolean hover = isIn(mouseX, mouseY);
            Gui.drawRect(x, y, x + w, y + h, hover ? 0x44FFFFFF : 0x22000000);
            gui.drawString(gui.fontRenderer, label, x + 4, y + 4, 0xFFFFFF);
            String state = getter.getAsBoolean() ? "§aON" : "§cOFF";
            gui.drawString(gui.fontRenderer, state, x + 260, y + 4, 0xFFFFFF);
        }

        @Override
        void onClick(int mx, int my) {
            if (isIn(mx, my)) action.run();
        }
    }

    private static class SliderWidget extends Widget {
        String label;
        int min, max;
        java.util.function.IntSupplier getter;
        java.util.function.IntConsumer setter;

        SliderWidget(String label, int min, int max,
                     java.util.function.IntSupplier getter,
                     java.util.function.IntConsumer setter) {
            this.label = label; this.min = min; this.max = max;
            this.getter = getter; this.setter = setter;
            this.w = 300; this.h = 22;
        }

        @Override
        void draw(GuiOverlaySettings gui, int mouseX, int mouseY) {
            int val = getter.getAsInt();
            gui.drawString(gui.fontRenderer, label + ": " + val, x + 4, y, 0xFFFFFF);

            Gui.drawRect(x, y + 10, x + w, y + 16, 0xFF333333);
            int fill = (int) ((val - min) / (float) (max - min) * w);
            Gui.drawRect(x, y + 10, x + fill, y + 16,
                    0xFF000000 | OverlayConfig.settingsSliderColor);
            Gui.drawRect(x + fill - 1, y + 8, x + fill + 1, y + 18, 0xFFFFFFFF);
        }

        @Override
        void onClick(int mx, int my) {
            if (isIn(mx, my)) apply(mx);
        }

        @Override
        void onDrag(int mx, int my) {
            apply(mx);
        }

        void apply(int mx) {
            float f = (mx - x) / (float) w;
            f = Math.max(0f, Math.min(1f, f));
            int v = (int) (min + f * (max - min));
            setter.accept(v);
        }
    }

    private static class DropdownWidget extends Widget {
        String label;
        java.util.function.Supplier<List<String>> optionsSupplier;
        java.util.function.Supplier<String> getter;
        java.util.function.Consumer<String> setter;

        boolean open = false;
        int scroll = 0;

        DropdownWidget(String label,
                       java.util.function.Supplier<List<String>> optionsSupplier,
                       java.util.function.Supplier<String> getter,
                       java.util.function.Consumer<String> setter) {
            this.label = label;
            this.optionsSupplier = optionsSupplier;
            this.getter = getter;
            this.setter = setter;
            this.w = 300;
            this.h = 20;
        }

        private static final int ITEM_H = 12;
        private static final int MAX_VISIBLE = 8;

        private int listHeight(List<String> options) {
            return Math.min(options.size(), MAX_VISIBLE) * ITEM_H;
        }

        // Рисуем только заголовок (фон + текст + стрелка)
        @Override
        void draw(GuiOverlaySettings gui, int mouseX, int mouseY) {
            boolean hover = isIn(mouseX, mouseY);
            Gui.drawRect(x, y, x + w, y + h, hover ? 0xFF2A2A2A : 0xFF1A1A1A);
            Gui.drawRect(x, y, x + w, y + 1, 0xFF444444);
            gui.drawString(gui.fontRenderer,
                    label + ": " + trim(gui, getter.get(), w - 60),
                    x + 4, y + 6, 0xFFFFFF);
            gui.drawString(gui.fontRenderer, open ? "▲" : "▼", x + w - 12, y + 6, 0xAAAAAA);
        }

        void drawOpenList(GuiOverlaySettings gui, int mouseX, int mouseY) {
            if (!open) return;

            List<String> options = optionsSupplier.get();
            if (options.isEmpty()) return;

            int listH = listHeight(options);
            int ly = y + h;

            // Непрозрачный фон + жирная рамка
            Gui.drawRect(x - 1, ly - 1, x + w + 1, ly + listH + 1, 0xFF000000);
            Gui.drawRect(x, ly, x + w, ly + listH, 0xFF101010);
            // Рамка
            Gui.drawRect(x - 1, ly - 1, x + w + 1, ly,      0xFF00AAFF);
            Gui.drawRect(x - 1, ly + listH, x + w + 1, ly + listH + 1, 0xFF00AAFF);
            Gui.drawRect(x - 1, ly - 1, x, ly + listH + 1,  0xFF00AAFF);
            Gui.drawRect(x + w, ly - 1, x + w + 1, ly + listH + 1, 0xFF00AAFF);

            int visible = Math.min(options.size(), MAX_VISIBLE);

            for (int i = 0; i < visible; i++) {
                int idx = i + scroll;
                if (idx >= options.size()) break;

                int iy = ly + i * ITEM_H;
                String opt = options.get(idx);
                boolean hoverItem = mouseX >= x && mouseX <= x + w
                        && mouseY >= iy && mouseY <= iy + ITEM_H;
                boolean isSelected = opt.equals(getter.get());

                if (hoverItem)     Gui.drawRect(x, iy, x + w, iy + ITEM_H, 0xFF2266AA);
                else if (isSelected) Gui.drawRect(x, iy, x + w, iy + ITEM_H, 0xFF1A3A5A);

                gui.drawString(gui.fontRenderer, trim(gui, opt, w - 8),
                        x + 4, iy + 2, isSelected ? 0x00FF88 : 0xFFFFFF);
            }

            // Индикатор прокрутки (если список длиннее видимой части)
            if (options.size() > MAX_VISIBLE) {
                int trackX = x + w - 3;
                Gui.drawRect(trackX, ly, trackX + 2, ly + listH, 0xFF333333);
                int thumbH = Math.max(6, listH * MAX_VISIBLE / options.size());
                int maxScroll = options.size() - MAX_VISIBLE;
                int thumbY = ly + (maxScroll == 0 ? 0 : (listH - thumbH) * scroll / maxScroll);
                Gui.drawRect(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xFF00AAFF);
            }
        }

        private static String trim(GuiOverlaySettings gui, String s, int maxW) {
            return gui.fontRenderer.trimStringToWidth(s, maxW);
        }

        @Override
        void onClick(int mx, int my) {
            if (isIn(mx, my)) {
                open = !open;
                scroll = 0;
                return;
            }
            if (!open) return;

            List<String> options = optionsSupplier.get();
            int listH = listHeight(options);
            int ly = y + h;

            if (mx >= x && mx <= x + w && my >= ly && my <= ly + listH) {
                int i = (my - ly) / ITEM_H;
                int idx = i + scroll;
                if (idx >= 0 && idx < options.size()) {
                    setter.accept(options.get(idx));
                }
                open = false;
            } else {
                open = false;
            }
        }

        boolean handleClick(int mx, int my) {
            // клик по заголовку
            if (isIn(mx, my)) {
                open = !open;
                scroll = 0;
                return true;
            }

            // клик по открытому списку
            if (open) {
                List<String> options = optionsSupplier.get();
                int listH = listHeight(options);
                int ly = y + h;

                if (mx >= x && mx <= x + w && my >= ly && my <= ly + listH) {
                    int i = (my - ly) / ITEM_H;
                    int idx = i + scroll;
                    if (idx >= 0 && idx < options.size()) {
                        setter.accept(options.get(idx));
                    }
                    open = false;
                    return true;
                }
            }

            return false;
        }

        boolean isMouseOverAll(int mx, int my) {
            if (isIn(mx, my)) return true;
            if (!open) return false;
            List<String> options = optionsSupplier.get();
            int listH = listHeight(options);
            int ly = y + h;
            return mx >= x && mx <= x + w && my >= ly && my <= ly + listH;
        }

        boolean handleWheel(int wheel) {
            if (!open) return false;
            List<String> options = optionsSupplier.get();
            int maxScroll = Math.max(0, options.size() - MAX_VISIBLE);
            if (maxScroll == 0) return true;

            scroll -= Integer.signum(wheel);
            if (scroll < 0) scroll = 0;
            if (scroll > maxScroll) scroll = maxScroll;
            return true;
        }
    }

    private static class ColorWidget extends Widget {
        String label;
        java.util.function.IntSupplier getter;
        java.util.function.IntConsumer setter;

        // Фокус/редактирование
        boolean editing = false;
        String editBuffer = "";
        int caret = 0;              // позиция каретки в буфере (0..6)
        int selStart = 0;           // начало выделения
        int selEnd = 0;             // конец выделения (selStart == selEnd — ничего не выделено)

        private static final int HEX_W   = 70;
        private static final int SMALL_H = 12;
        private static final int MINI_W  = 80;

        ColorWidget(String label,
                    java.util.function.IntSupplier getter,
                    java.util.function.IntConsumer setter) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.w = 300;
            this.h = 44;
        }

        private int hexX() { return x + w - HEX_W - 2; }
        private int hexY() { return y + 4; }

        // draw

        @Override
        void draw(GuiOverlaySettings gui, int mouseX, int mouseY) {
            int rgb = getter.getAsInt();

            Gui.drawRect(x, y + 14, x + 80, y + 22, 0xFF000000 | rgb);

            gui.drawString(gui.fontRenderer,
                    label + ": " + String.format("#%06X", rgb),
                    x, y + 2, 0xFFFFFF);

            // HEX-поле
            int hx = hexX(), hy = hexY();
            boolean hoverHex = isIn(mouseX, mouseY, hx, hy, HEX_W, SMALL_H);

            Gui.drawRect(hx, hy, hx + HEX_W, hy + SMALL_H,
                    editing ? 0xFF000000 : (hoverHex ? 0xFF2A2A2A : 0xFF1A1A1A));
            Gui.drawRect(hx, hy, hx + HEX_W, hy + 1, 0xFF444444);

            String hexText = editing ? editBuffer : String.format("%06X", rgb);
            int textX = hx + 2;
            int textY = hy + 2;

            if (editing && selStart != selEnd) {
                int a = Math.min(selStart, selEnd);
                int b = Math.max(selStart, selEnd);
                String pre = hexText.substring(0, a);
                String sel = hexText.substring(a, b);
                int preW  = gui.fontRenderer.getStringWidth(pre);
                int selW  = gui.fontRenderer.getStringWidth(sel);
                Gui.drawRect(textX + preW, textY - 1,
                        textX + preW + selW, textY + 9,
                        0xFF2266AA);
            }

            gui.drawString(gui.fontRenderer, hexText, textX, textY, 0xFFFFFF);

            // Каретка
            if (editing && selStart == selEnd
                    && ((System.currentTimeMillis() / 500) % 2 == 0)) {
                String pre = hexText.substring(0, Math.min(caret, hexText.length()));
                int cx = textX + gui.fontRenderer.getStringWidth(pre);
                Gui.drawRect(cx, textY - 1, cx + 1, textY + 9, 0xFFFFFFFF);
            }

            // R/G/B
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8)  & 0xFF;
            int b =  rgb        & 0xFF;
            drawMini(gui, "R", r, x,       y + 26);
            drawMini(gui, "G", g, x + 105, y + 26);
            drawMini(gui, "B", b, x + 210, y + 26);
        }

        private void drawMini(GuiOverlaySettings gui, String lbl, int val,
                              int mx0, int my0) {
            gui.drawString(gui.fontRenderer, lbl, mx0, my0, 0xFFFFFF);
            Gui.drawRect(mx0 + 12, my0 + 4, mx0 + 12 + MINI_W, my0 + 8, 0xFF333333);
            int fill = val * MINI_W / 255;
            Gui.drawRect(mx0 + 12, my0 + 4, mx0 + 12 + fill, my0 + 8,
                    0xFF000000 | OverlayConfig.settingsSliderColor);
            gui.drawString(gui.fontRenderer, String.valueOf(val),
                    mx0 + 12 + MINI_W + 4, my0, 0xCCCCCC);
        }

        // input
        boolean handleClick(int mx, int my, GuiOverlaySettings gui) {
            if (isIn(mx, my, hexX(), hexY(), HEX_W, SMALL_H)) {
                if (!editing) {
                    beginEditing();
                } else {
                    int rel = mx - (hexX() + 2);
                    int idx = charIndexAt(gui, editBuffer, rel);
                    caret = idx;
                    selStart = selEnd = idx;
                }
                return true;
            }

            if (isOverMiniSlider(mx, my)) {
                commitIfEditing();
                onDrag(mx, my);
                return true;
            }

            if (isIn(mx, my)) {
                commitIfEditing();
                return true;
            }

            return false;
        }

        @Override
        void onClick(int mx, int my) {
            handleClick(mx, my, null);
        }

        @Override
        void onDrag(int mx, int my) {
            if (editing) return;

            int rgb = getter.getAsInt();
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8)  & 0xFF;
            int b =  rgb        & 0xFF;

            if (my >= y + 26 && my <= y + 26 + 12) {
                if (mx >= x + 12 && mx <= x + 12 + MINI_W) {
                    r = clamp((mx - (x + 12)) * 255 / MINI_W);
                } else if (mx >= x + 105 + 12 && mx <= x + 105 + 12 + MINI_W) {
                    g = clamp((mx - (x + 105 + 12)) * 255 / MINI_W);
                } else if (mx >= x + 210 + 12 && mx <= x + 210 + 12 + MINI_W) {
                    b = clamp((mx - (x + 210 + 12)) * 255 / MINI_W);
                }
            }
            setter.accept((r << 16) | (g << 8) | b);
        }

        // фокус

        void beginEditing() {
            editing = true;
            editBuffer = String.format("%06X", getter.getAsInt());
            caret = editBuffer.length();
            selStart = selEnd = caret;
        }

        void endEditing() {
            commitIfEditing();
        }

        private void commitIfEditing() {
            if (!editing) return;
            editing = false;
            String s = editBuffer.trim().replace("#", "");
            if (s.matches("[0-9a-fA-F]{6}")) {
                setter.accept((int) Long.parseLong(s, 16) & 0xFFFFFF);
            }
            editBuffer = "";
            caret = selStart = selEnd = 0;
        }

        /** true — событие съедено. */
        boolean handleKey(char typedChar, int keyCode) {
            if (!editing) return false;

            boolean ctrl = org.lwjgl.input.Keyboard.isKeyDown(
                    org.lwjgl.input.Keyboard.KEY_LCONTROL)
                    || org.lwjgl.input.Keyboard.isKeyDown(
                    org.lwjgl.input.Keyboard.KEY_RCONTROL);
            boolean shift = org.lwjgl.input.Keyboard.isKeyDown(
                    org.lwjgl.input.Keyboard.KEY_LSHIFT)
                    || org.lwjgl.input.Keyboard.isKeyDown(
                    org.lwjgl.input.Keyboard.KEY_RSHIFT);

            switch (keyCode) {
                case org.lwjgl.input.Keyboard.KEY_ESCAPE:
                    editing = false;
                    editBuffer = "";
                    caret = selStart = selEnd = 0;
                    return true;

                case org.lwjgl.input.Keyboard.KEY_RETURN:
                case org.lwjgl.input.Keyboard.KEY_NUMPADENTER:
                    commitIfEditing();
                    return true;

                case org.lwjgl.input.Keyboard.KEY_BACK: {
                    if (selStart != selEnd) {
                        deleteSelection();
                    } else if (caret > 0) {
                        editBuffer = editBuffer.substring(0, caret - 1)
                                + editBuffer.substring(caret);
                        caret--;
                    }
                    selStart = selEnd = caret;
                    return true;
                }

                case org.lwjgl.input.Keyboard.KEY_DELETE: {
                    if (selStart != selEnd) {
                        deleteSelection();
                    } else if (caret < editBuffer.length()) {
                        editBuffer = editBuffer.substring(0, caret)
                                + editBuffer.substring(caret + 1);
                    }
                    selStart = selEnd = caret;
                    return true;
                }

                case org.lwjgl.input.Keyboard.KEY_LEFT: {
                    if (caret > 0) caret--;
                    if (!shift) selStart = selEnd = caret;
                    else selEnd = caret;
                    return true;
                }

                case org.lwjgl.input.Keyboard.KEY_RIGHT: {
                    if (caret < editBuffer.length()) caret++;
                    if (!shift) selStart = selEnd = caret;
                    else selEnd = caret;
                    return true;
                }

                case org.lwjgl.input.Keyboard.KEY_HOME:
                    caret = 0;
                    if (!shift) selStart = selEnd = caret;
                    else selEnd = caret;
                    return true;

                case org.lwjgl.input.Keyboard.KEY_END:
                    caret = editBuffer.length();
                    if (!shift) selStart = selEnd = caret;
                    else selEnd = caret;
                    return true;

                case org.lwjgl.input.Keyboard.KEY_A:
                    if (ctrl) {
                        selStart = 0;
                        selEnd = editBuffer.length();
                        caret = selEnd;
                        return true;
                    }
                    break;

                case org.lwjgl.input.Keyboard.KEY_C:
                    if (ctrl) {
                        String s = selectedOrAll();
                        if (!s.isEmpty()) setClipboard(s);
                        return true;
                    }
                    break;

                case org.lwjgl.input.Keyboard.KEY_X:
                    if (ctrl) {
                        String s = selectedOrAll();
                        if (!s.isEmpty()) setClipboard(s);
                        deleteSelectionOrAll();
                        return true;
                    }
                    break;

                case org.lwjgl.input.Keyboard.KEY_V:
                    if (ctrl) {
                        String cb = getClipboard();
                        if (cb != null) {
                            cb = cb.trim().replace("#", "");
                            if (cb.matches("[0-9a-fA-F]{1,6}")) {
                                deleteSelection();
                                if (editBuffer.length() + cb.length() <= 6) {
                                    editBuffer = editBuffer.substring(0, caret)
                                            + cb.toUpperCase()
                                            + editBuffer.substring(caret);
                                    caret += cb.length();
                                }
                                selStart = selEnd = caret;
                            }
                        }
                        return true;
                    }
                    break;
            }

            char c = Character.toUpperCase(typedChar);
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
                deleteSelection();
                if (editBuffer.length() < 6) {
                    editBuffer = editBuffer.substring(0, caret)
                            + c
                            + editBuffer.substring(caret);
                    caret++;
                }
                selStart = selEnd = caret;
            }
            return true;
        }

        private String selectedOrAll() {
            if (selStart == selEnd) return editBuffer;
            int a = Math.min(selStart, selEnd);
            int b = Math.max(selStart, selEnd);
            return editBuffer.substring(a, b);
        }

        private void deleteSelection() {
            if (selStart == selEnd) return;
            int a = Math.min(selStart, selEnd);
            int b = Math.max(selStart, selEnd);
            editBuffer = editBuffer.substring(0, a) + editBuffer.substring(b);
            caret = a;
            selStart = selEnd = a;
        }

        private void deleteSelectionOrAll() {
            if (selStart == selEnd) {
                editBuffer = "";
                caret = 0;
            } else {
                deleteSelection();
            }
            selStart = selEnd = caret;
        }

        private int charIndexAt(GuiOverlaySettings gui, String s, int relX) {
            if (relX <= 0) return 0;
            for (int i = 0; i <= s.length(); i++) {
                int w = gui.fontRenderer.getStringWidth(s.substring(0, i));
                if (w >= relX) return i;
            }
            return s.length();
        }

        private static int clamp(int v) {
            return Math.max(0, Math.min(255, v));
        }

        private static void setClipboard(String s) {
            try {
                java.awt.Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(s), null);
            } catch (Throwable ignored) {}
        }

        private static String getClipboard() {
            try {
                java.awt.datatransfer.Transferable t =
                        java.awt.Toolkit.getDefaultToolkit()
                                .getSystemClipboard()
                                .getContents(null);
                if (t != null && t.isDataFlavorSupported(
                        java.awt.datatransfer.DataFlavor.stringFlavor)) {
                    return (String) t.getTransferData(
                            java.awt.datatransfer.DataFlavor.stringFlavor);
                }
            } catch (Throwable ignored) {}
            return null;
        }

        boolean isOverMiniSlider(int mx, int my) {
            if (my < y + 26 || my > y + 26 + 12) return false;

            // R
            if (mx >= x + 12 && mx <= x + 12 + MINI_W) return true;
            // G
            if (mx >= x + 105 + 12 && mx <= x + 105 + 12 + MINI_W) return true;
            // B
            if (mx >= x + 210 + 12 && mx <= x + 210 + 12 + MINI_W) return true;

            return false;
        }
    }

    private static class ButtonWidget extends Widget {
        String label;
        Runnable action;

        ButtonWidget(String label, Runnable action) {
            this.label = label;
            this.action = action;
            this.w = 300;
            this.h = 20;
        }

        @Override
        void draw(GuiOverlaySettings gui, int mouseX, int mouseY) {
            boolean hover = isIn(mouseX, mouseY);
            int base  = 0xFF000000 | OverlayConfig.settingsBtnColor;
            int hoverC = 0xFF000000 | OverlayConfig.settingsBtnHover;
            Gui.drawRect(x, y, x + w, y + h, hover ? hoverC : base);
            gui.drawCenteredString(gui.fontRenderer, label,
                    x + w / 2, y + 6, OverlayConfig.settingsBtnTextColor);
        }

        @Override
        void onClick(int mx, int my) {
            if (isIn(mx, my)) action.run();
        }
    }

    private static class SectionWidget extends Widget {
        String title;
        SectionWidget(String title) { this.title = title; this.w = 300; this.h = 18; }
        @Override
        void draw(GuiOverlaySettings gui, int mouseX, int mouseY) {
            int color = 0xFF000000 | OverlayConfig.settingsSectionColor;
            gui.drawString(gui.fontRenderer, "§l" + title, x, y + 4, color);
        }
    }

    // Стройка
    private void buildWidgets() {
        widgets.clear();
        int x = width / 2 - 150;
        int cy = 40;

        widgets.add(new SectionWidget(I18n.format("waimusic.guios.ovtitle")));

        widgets.add(new ToggleWidget(I18n.format("waimusic.guios.showtime"),
                () -> OverlayConfig.showTime, () -> OverlayConfig.showTime = !OverlayConfig.showTime));
        widgets.add(new ToggleWidget(I18n.format("waimusic.guios.polzshow"),
                () -> OverlayConfig.showProgress, () -> OverlayConfig.showProgress = !OverlayConfig.showProgress));
        widgets.add(new ToggleWidget(I18n.format("waimusic.guios.showicon"),
                () -> OverlayConfig.showIcon, () -> OverlayConfig.showIcon = !OverlayConfig.showIcon));
        widgets.add(new ToggleWidget(I18n.format("waimusic.guios.showtitle"),
                () -> OverlayConfig.showTitle, () -> OverlayConfig.showTitle = !OverlayConfig.showTitle));
        widgets.add(new ToggleWidget(I18n.format("waimusic.guios.showartist"),
                () -> OverlayConfig.showArtist, () -> OverlayConfig.showArtist = !OverlayConfig.showArtist));
        widgets.add(new ToggleWidget(I18n.format("waimusic.guios.timeright"),
                () -> OverlayConfig.timeAlignRight,
                () -> OverlayConfig.timeAlignRight = !OverlayConfig.timeAlignRight));

        widgets.add(new ButtonWidget(I18n.format("waimusic.guios.posbtn"), () ->
                Minecraft.getMinecraft().displayGuiScreen(
                        new GuiOverlayPosition(GuiOverlaySettings.this)
                )
        ));

        widgets.add(new ButtonWidget(I18n.format("waimusic.guios.refreshmixers"), () -> {
            AudioMixerManager.INSTANCE.refresh();
            if (AudioMixerManager.INSTANCE.getSelected() != null
                    && AudioMixerManager.INSTANCE.findByName(
                    AudioMixerManager.INSTANCE.getSelected().getName()) == null) {
                AudioMixerManager.INSTANCE.setSelected(null);
                OverlayConfig.audioMixerName = "";
            }
        }));

        widgets.add(new DropdownWidget(
                I18n.format("waimusic.guios.mixer"),
                () -> {
                    List<String> names = new ArrayList<>();
                    names.add(I18n.format("waimusic.guios.mixer_default"));
                    for (javax.sound.sampled.Mixer.Info mi : AudioMixerManager.INSTANCE.getMixers()) {
                        names.add(mi.getName());
                    }
                    return names;
                },
                () -> {
                    String sel = AudioMixerManager.INSTANCE.getSelectedName();
                    return sel == null || sel.isEmpty()
                            ? I18n.format("waimusic.guios.mixer_default") : sel;
                },
                name -> {
                    javax.sound.sampled.Mixer.Info info =
                            name.equals(I18n.format("waimusic.guios.mixer_default"))
                                    ? null
                                    : AudioMixerManager.INSTANCE.findByName(name);
                    AudioMixerManager.INSTANCE.setSelected(info);
                    OverlayConfig.audioMixerName = (info == null) ? "" : info.getName();
                    MusicManager.INSTANCE.restartCurrent();
                }
        ));

        widgets.add(new SliderWidget(I18n.format("waimusic.guios.width"), 100, 400,
                () -> OverlayConfig.width, v -> OverlayConfig.width = v));
        widgets.add(new SliderWidget(I18n.format("waimusic.guios.height"), 30, 120,
                () -> OverlayConfig.height, v -> OverlayConfig.height = v));
        widgets.add(new SliderWidget(I18n.format("waimusic.guios.bgalpha"), 0, 255,
                () -> OverlayConfig.bgAlpha, v -> OverlayConfig.bgAlpha = v));
        widgets.add(new SliderWidget(I18n.format("waimusic.guios.overlayalpha"), 0, 255,
                () -> OverlayConfig.overlayAlpha, v -> OverlayConfig.overlayAlpha = v));
        widgets.add(new SliderWidget(I18n.format("waimusic.guios.textscale"), 50, 200,
                () -> (int) (OverlayConfig.textScale * 100f),
                v -> OverlayConfig.textScale = v / 100f));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.color.bg"),
                () -> OverlayConfig.bgColor, v -> OverlayConfig.bgColor = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.color.line"),
                () -> OverlayConfig.lineColor, v -> OverlayConfig.lineColor = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.color.text"),
                () -> OverlayConfig.textColor, v -> OverlayConfig.textColor = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.color.artist"),
                () -> OverlayConfig.textColorSub, v -> OverlayConfig.textColorSub = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.color.time"),
                () -> OverlayConfig.timeColor, v -> OverlayConfig.timeColor = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.color.paused"),
                () -> OverlayConfig.timeColorPaused, v -> OverlayConfig.timeColorPaused = v));

        widgets.add(new SectionWidget(I18n.format("waimusic.guios.player.title")));
        widgets.add(new SliderWidget(I18n.format("waimusic.guios.player.bgalpha"), 0, 255,
                () -> OverlayConfig.guiBgAlpha, v -> OverlayConfig.guiBgAlpha = v));
        widgets.add(new SliderWidget(I18n.format("waimusic.guios.player.logoalpha"), 0, 255,
                () -> OverlayConfig.bgLogoAlpha, v -> OverlayConfig.bgLogoAlpha = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.player.logocolor"),
                () -> OverlayConfig.LogoColor, v -> OverlayConfig.LogoColor = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.player.bgcolor"),
                () -> OverlayConfig.guiBgColor, v -> OverlayConfig.guiBgColor = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.player.textcolor"),
                () -> OverlayConfig.guiTextColor, v -> OverlayConfig.guiTextColor = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.player.btncolor"),
                () -> OverlayConfig.guiButtonColor, v -> OverlayConfig.guiButtonColor = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.player.btnhover"),
                () -> OverlayConfig.guiButtonHover, v -> OverlayConfig.guiButtonHover = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.player.btntext"),
                () -> OverlayConfig.guiButtonText, v -> OverlayConfig.guiButtonText = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.player.volbar"),
                () -> OverlayConfig.volumeBarColor,
                v -> OverlayConfig.volumeBarColor = v));
        widgets.add(new ColorWidget(I18n.format("waimusic.guios.player.volbarfill"),
                () -> OverlayConfig.volumeBarFillColor,
                v -> OverlayConfig.volumeBarFillColor = v));

        widgets.add(new SectionWidget(I18n.format("waimusic.guios.queue.title")));

        widgets.add(new SliderWidget(I18n.format("waimusic.guios.queue.bgalpha"), 0, 255,
                () -> OverlayConfig.queuePanelBgAlpha,
                v -> OverlayConfig.queuePanelBgAlpha = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.queue.bgcolor"),
                () -> OverlayConfig.queuePanelBgColor,
                v -> OverlayConfig.queuePanelBgColor = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.queue.textcolor"),
                () -> OverlayConfig.queueTextColor,
                v -> OverlayConfig.queueTextColor = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.queue.texthover"),
                () -> OverlayConfig.queueTextColorHover,
                v -> OverlayConfig.queueTextColorHover = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.queue.textbuiltin"),
                () -> OverlayConfig.queueTextColorBuiltin,
                v -> OverlayConfig.queueTextColorBuiltin = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.queue.textplaying"),
                () -> OverlayConfig.queueTextColorPlaying,
                v -> OverlayConfig.queueTextColorPlaying = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.queue.textdragging"),
                () -> OverlayConfig.queueTextColorDragging,
                v -> OverlayConfig.queueTextColorDragging = v));


        widgets.add(new SectionWidget(I18n.format("waimusic.guios.playlist.title")));

        widgets.add(new SliderWidget(I18n.format("waimusic.guios.playlist.bgalpha"), 0, 255,
                () -> OverlayConfig.playlistPanelBgAlpha,
                v -> OverlayConfig.playlistPanelBgAlpha = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.playlist.bgcolor"),
                () -> OverlayConfig.playlistBgColor,
                v -> OverlayConfig.playlistBgColor = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.playlist.textcolor"),
                () -> OverlayConfig.playlistTextColor,
                v -> OverlayConfig.playlistTextColor = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.playlist.subcolor"),
                () -> OverlayConfig.playlistSubColor,
                v -> OverlayConfig.playlistSubColor = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.playlist.selcolor"),
                () -> OverlayConfig.playlistSelColor,
                v -> OverlayConfig.playlistSelColor = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.playlist.btncolor"),
                () -> OverlayConfig.playlistBtnColor,
                v -> OverlayConfig.playlistBtnColor = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.playlist.btnhover"),
                () -> OverlayConfig.playlistBtnHover,
                v -> OverlayConfig.playlistBtnHover = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.playlist.btntext"),
                () -> OverlayConfig.playlistBtnText,
                v -> OverlayConfig.playlistBtnText = v));

        widgets.add(new SectionWidget(I18n.format("waimusic.guios.settings.title")));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.settings.btncolor"),
                () -> OverlayConfig.settingsBtnColor,
                v -> OverlayConfig.settingsBtnColor = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.settings.btnhover"),
                () -> OverlayConfig.settingsBtnHover,
                v -> OverlayConfig.settingsBtnHover = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.settings.btntext"),
                () -> OverlayConfig.settingsBtnTextColor,
                v -> OverlayConfig.settingsBtnTextColor = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.settings.section"),
                () -> OverlayConfig.settingsSectionColor,
                v -> OverlayConfig.settingsSectionColor = v));

        widgets.add(new ColorWidget(I18n.format("waimusic.guios.settings.slider"),
                () -> OverlayConfig.settingsSliderColor,
                v -> OverlayConfig.settingsSliderColor = v));

        for (Widget wgt : widgets) {
            wgt.x = x;
            wgt.baseY = cy;
            wgt.y = cy - scroll;
            cy += wgt.h + 2;
        }
    }

    @Override
    public void initGui() {
        buildWidgets();
    }

    // ОтриСовка
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format("waimusic.guios.setwaimus"), width / 2, 12, 0xFFFFFF);

        // Обычные виджеты (в т.ч. заголовки дропдаунов)
        for (Widget wgt : widgets) {
            wgt.updateScreenY(scroll);
            if (wgt.y + wgt.h >= 30 && wgt.y <= height - 40) {
                wgt.draw(this, mouseX, mouseY);
            }
        }

        // Открытый список дропдауна — ПОВЕРХ всего
        for (Widget wgt : widgets) {
            if (wgt instanceof DropdownWidget) {
                DropdownWidget dd = (DropdownWidget) wgt;
                if (dd.y + dd.h >= 30 && dd.y <= height - 40) {
                    dd.drawOpenList(this, mouseX, mouseY);
                }
            }
        }

        int bx = width / 2 - 50, by = height - 30;
        boolean hover = mouseX >= bx && mouseX <= bx + 100 && mouseY >= by && mouseY <= by + 20;
        Gui.drawRect(bx, by, bx + 100, by + 20, hover ? 0xFF2266AA : 0xFF1A1A1A);
        drawCenteredString(fontRenderer, I18n.format("waimusic.guios.nazad"), width / 2, by + 6, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // Ввод
    private Widget findWidget(int mx, int my) {
        for (Widget wgt : widgets) {
            if (mx >= wgt.x && mx <= wgt.x + wgt.w
                    && my >= wgt.y && my <= wgt.y + wgt.h) {
                return wgt;
            }
        }
        return null;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0 && mouseButton != 1) return;

        for (Widget wgt : widgets) wgt.updateScreenY(scroll);

        // Кнопка «Назад»
        int bx = width / 2 - 50, by = height - 30;
        if (mouseButton == 0
                && mouseX >= bx && mouseX <= bx + 100
                && mouseY >= by && mouseY <= by + 20) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }

        boolean anyDropdownOpen = false;
        for (Widget wgt : widgets) {
            if (wgt instanceof DropdownWidget) {
                DropdownWidget dd = (DropdownWidget) wgt;
                if (dd.open) {
                    anyDropdownOpen = true;
                    if (dd.handleClick(mouseX, mouseY)) {
                        return; // клик съеден дропдауном
                    }
                }
            }
        }

        if (anyDropdownOpen) {
            for (Widget wgt : widgets) {
                if (wgt instanceof DropdownWidget) {
                    ((DropdownWidget) wgt).open = false;
                }
            }
            if (focusedColor != null) {
                focusedColor.endEditing();
                focusedColor = null;
            }
            return;
        }

        // 3. Активный ColorWidget (фокус)
        if (focusedColor != null) {
            boolean stillInFocus = focusedColor.handleClick(mouseX, mouseY, this);
            if (!stillInFocus) {
                focusedColor.endEditing();
                focusedColor = null;
            } else {
                if (focusedColor.isOverMiniSlider(mouseX, mouseY)) {
                    dragging = focusedColor;
                }
                return;
            }
        }

        // 4. Обычные виджеты
        Widget wgt = findWidget(mouseX, mouseY);
        if (wgt != null) {
            if (wgt instanceof ColorWidget) {
                ColorWidget cw = (ColorWidget) wgt;
                cw.handleClick(mouseX, mouseY, this);
                focusedColor = cw;

                if (cw.isOverMiniSlider(mouseX, mouseY)) {
                    dragging = cw;
                }
            } else {
                wgt.onClick(mouseX, mouseY);
                if (wgt instanceof SliderWidget) {
                    dragging = wgt;
                }
            }
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for (Widget wgt : widgets) wgt.updateScreenY(scroll);
        if (dragging != null) {
            dragging.onDrag(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        int mx = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
        int my = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

        for (Widget wgt : widgets) wgt.updateScreenY(scroll);

        // 1. Если мышь над открытым дропдауном
        for (Widget wgt : widgets) {
            if (wgt instanceof DropdownWidget) {
                DropdownWidget dd = (DropdownWidget) wgt;
                if (dd.isMouseOverAll(mx, my) && dd.handleWheel(wheel)) {
                    return;
                }
            }
        }

        // 2. Обычная прокрутка страницы
        scroll -= Integer.signum(wheel) * 20;
        if (scroll < 0) scroll = 0;
        int totalH = 0;
        for (Widget wgt : widgets) totalH += wgt.h + 2;
        int maxScroll = Math.max(0, totalH - (height - 80));
        if (scroll > maxScroll) scroll = maxScroll;
    }

    @Override
    public void onGuiClosed() {
        OverlayConfigIO.save();
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}