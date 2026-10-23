/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.gui.cfg;

/** Дефолтный конфиг */

public class OverlayConfig {

    /** ОВЕРЛЕЙ */
    // Цвет текста (ARGB)
    public static int textColor       = 0xFFFFFFFF;
    // Цвет второстепенного текста (артист)
    public static int textColorSub    = 0xFFAAAAAA;
    // Цвет времени
    public static int timeColor       = 0xFF00FF88;
    // Цвет времени на паузе
    public static int timeColorPaused = 0xFFFFAA00;

    // Цвет фона панели (RGB, без альфы — альфа берётся из bgAlpha)
    public static int bgColor         = 0x000000;
    // Прозрачность фона 0..255.
    public static int bgAlpha         = 0xAA;
    // Прозрачность оверлея
    public static int bgLogoAlpha      = 255;
    // Цвет лого
    public static int LogoColor         = 0xFF00AAFF;

    // Общая прозрачность всего оверлея 0..255
    public static int overlayAlpha    = 0xFF;

    // Цвет линий (полоска сверху)
    public static int lineColor       = 0xFF00AAFF;

    // Размеры оверлея
    public static int width  = 170;
    public static int height = 52;

    // Позиция оверлея на экране
    public static int overlayX = 8;
    public static int overlayY = 8;

    public static boolean timeAlignRight = false;

    // Отображаемые
    public static boolean showTime      = true;
    public static boolean showProgress  = true;
    public static boolean showIcon      = true;
    public static boolean showTitle     = true;
    public static boolean showArtist    = true;

    /** Окно настроек (GuiMusicPlayer) */
    public static int  guiBgColor      = 0x101010;
    public static int  guiBgAlpha      = 0xCC;
    public static int  guiTextColor    = 0xFFFFFF;
    public static int  guiButtonColor  = 0x1A1A1A;
    public static int  guiButtonHover  = 0x2266AA;
    public static int  guiButtonText   = 0xFFFFFF;
    public static int  settingsBtnColor      = 0x1A1A1A;
    public static int  settingsBtnHover      = 0x2266AA;
    public static int  settingsBtnTextColor  = 0xFFFFFF;
    public static int  settingsSectionColor  = 0x00AAFF;
    public static int  settingsSliderColor   = 0x00AAFF;

    // VOLUME
    public static int  volumeBarColor = 0x2E2E2E;
    public static int volumeBarFillColor  = 0x00FF88;


    public static int guiDimAlpha = 120;
    public static float textScale = 1.0f;


    /** Окно плейлистов (левая панель в GuiMusicPlayer) */
    public static int  playlistWidth     = 160;
    public static int  playlistBgColor   = 0x000000;
    public static int  playlistBgAlpha   = 0xCC;
    public static int  playlistTextColor = 0xFFFFFF;
    public static int  playlistSubColor  = 0xAAAAAA;
    public static int  playlistSelColor  = 0x2266AA;
    public static int  playlistBtnColor  = 0x1A1A1A;
    public static int  playlistBtnHover  = 0x2266AA;
    public static int  playlistBtnText   = 0xFFFFFF;
    public static int  playlistPanelBgAlpha = 0xCC;
    public static boolean playlistShowDesc = true;
    public static int playlistHeaderBgColor      = 0x1A1A1A;
    public static int playlistHeaderTextColor    = 0xFFFFFF;
    public static int playlistBuiltinTextColor   = 0xFFDD66;
    public static int playlistRowColor           = 0x000000;
    public static int playlistRowHoverColor      = 0xFFFFFF;
    public static int playlistBtnBorderColor     = 0x444444;
    public static int playlistEditBtnColor       = 0x000000;
    public static int playlistEditBtnHoverColor  = 0x2266AA;
    public static int playlistEditBtnBorderColor = 0xFFFFFF;
    public static int playlistEditBtnTextColor   = 0xFFFFFF;
    public static int playlistHintColor          = 0x888888;
    public static int playlistDragGhostBg        = 0x000000;
    public static int playlistDragGhostText      = 0xFFAA00;
    public static int playlistDragMarkerColor    = 0xFFAA00;

    /** Правая панель */
    public static int  queuePanelBgColor   = 0x000000;
    public static int  queuePanelBgAlpha   = 0xAA;
    public static int  queueTextColor      = 0xFFFFFF;
    public static int  queueTextColorHover = 0xAAAAAA;
    public static int  queueTextColorBuiltin = 0x777777;
    public static int  queueTextColorPlaying = 0x00FF88;
    public static int  queueTextColorDragging = 0xFFAA00;

    // МИКШЕР
    public static String audioMixerName = "";
}