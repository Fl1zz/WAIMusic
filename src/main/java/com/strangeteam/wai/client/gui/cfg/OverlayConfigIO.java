/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.gui.cfg;

import net.minecraftforge.common.config.Configuration;

import com.strangeteam.wai.client.managers.AudioMixerManager;

import java.io.File;

/** Конфиг */

public final class OverlayConfigIO {

    private static Configuration cfg;

    public static void init(File configFile) {
        cfg = new Configuration(configFile);
        load();
        AudioMixerManager.INSTANCE.refresh();
        AudioMixerManager.INSTANCE.setSelected(
                AudioMixerManager.INSTANCE.findByName(OverlayConfig.audioMixerName));
    }

    public static void load() {
        if (cfg == null) return;
        try {
            cfg.load();

            // ОВЕРЛЕЙ
            OverlayConfig.textColor       = hex(cfg.get("overlay", "textColor",       hex(OverlayConfig.textColor),       "").getString());
            OverlayConfig.textColorSub    = hex(cfg.get("overlay", "textColorSub",    hex(OverlayConfig.textColorSub),    "").getString());
            OverlayConfig.timeColor       = hex(cfg.get("overlay", "timeColor",       hex(OverlayConfig.timeColor),       "").getString());
            OverlayConfig.timeColorPaused = hex(cfg.get("overlay", "timeColorPaused", hex(OverlayConfig.timeColorPaused), "").getString());
            OverlayConfig.bgColor         = hex(cfg.get("overlay", "bgColor",         hex(OverlayConfig.bgColor),         "").getString());
            OverlayConfig.lineColor       = hex(cfg.get("overlay", "lineColor",       hex(OverlayConfig.lineColor),       "").getString());

            OverlayConfig.bgAlpha      = cfg.getInt("bgAlpha",      "overlay", OverlayConfig.bgAlpha,      0, 255, "");
            OverlayConfig.overlayAlpha = cfg.getInt("overlayAlpha", "overlay", OverlayConfig.overlayAlpha, 0, 255, "");

            OverlayConfig.width  = cfg.getInt("width",  "overlay", OverlayConfig.width,  100, 400, "");
            OverlayConfig.height = cfg.getInt("height", "overlay", OverlayConfig.height,  30, 120, "");

            OverlayConfig.showTime     = cfg.getBoolean("showTime",     "overlay", OverlayConfig.showTime,     "");
            OverlayConfig.showProgress = cfg.getBoolean("showProgress", "overlay", OverlayConfig.showProgress, "");
            OverlayConfig.showIcon     = cfg.getBoolean("showIcon",     "overlay", OverlayConfig.showIcon,     "");
            OverlayConfig.showTitle    = cfg.getBoolean("showTitle",    "overlay", OverlayConfig.showTitle,    "");
            OverlayConfig.showArtist   = cfg.getBoolean("showArtist",   "overlay", OverlayConfig.showArtist,   "");
            OverlayConfig.textScale = cfg.getFloat("textScale", "overlay",
                    OverlayConfig.textScale, 0.5f, 2.0f, "");

            OverlayConfig.overlayX = cfg.getInt("overlayX", "overlay", OverlayConfig.overlayX, 0, 10000, "");
            OverlayConfig.overlayY = cfg.getInt("overlayY", "overlay", OverlayConfig.overlayY, 0, 10000, "");
            OverlayConfig.timeAlignRight = cfg.getBoolean("timeAlignRight", "overlay",
                    OverlayConfig.timeAlignRight, "");

            OverlayConfig.overlayX = cfg.getInt("overlayX", "overlay", OverlayConfig.overlayX, 0, 10000, "");
            OverlayConfig.overlayY = cfg.getInt("overlayY", "overlay", OverlayConfig.overlayY, 0, 10000, "");
            OverlayConfig.timeAlignRight = cfg.getBoolean("timeAlignRight", "overlay",
                    OverlayConfig.timeAlignRight, "");

            // ОКНО ПЛЕЕРА
            OverlayConfig.guiBgColor     = hex(cfg.get("gui", "guiBgColor",    hex(OverlayConfig.guiBgColor),    "").getString());
            OverlayConfig.guiTextColor   = hex(cfg.get("gui", "guiTextColor",  hex(OverlayConfig.guiTextColor),  "").getString());
            OverlayConfig.guiButtonColor = hex(cfg.get("gui", "guiButtonColor",hex(OverlayConfig.guiButtonColor),"") .getString());
            OverlayConfig.guiButtonHover = hex(cfg.get("gui", "guiButtonHover",hex(OverlayConfig.guiButtonHover),"") .getString());
            OverlayConfig.guiButtonText  = hex(cfg.get("gui", "guiButtonText", hex(OverlayConfig.guiButtonText), "").getString());

            OverlayConfig.guiBgAlpha = cfg.getInt("guiBgAlpha", "gui", OverlayConfig.guiBgAlpha, 0, 255, "");
            OverlayConfig.guiDimAlpha = cfg.getInt("guiDimAlpha","gui", OverlayConfig.guiDimAlpha, 0, 255, "");
            OverlayConfig.bgLogoAlpha = cfg.getInt("bgLogoAlpha","gui", OverlayConfig.bgLogoAlpha, 0, 255, "");
            OverlayConfig.LogoColor       = hex(cfg.get("overlay", "LogoColor",       hex(OverlayConfig.LogoColor),       "").getString());

            // ОЧЕРЕДЬ (правая панель)
            OverlayConfig.queuePanelBgColor    = hex(cfg.get("queue", "queuePanelBgColor",
                    hex(OverlayConfig.queuePanelBgColor), "").getString());
            OverlayConfig.queuePanelBgAlpha    = cfg.getInt("queue", "queuePanelBgAlpha",
                    OverlayConfig.queuePanelBgAlpha, 0, 255, "");
            OverlayConfig.queueTextColor       = hex(cfg.get("queue", "queueTextColor",
                    hex(OverlayConfig.queueTextColor), "").getString());
            OverlayConfig.queueTextColorHover  = hex(cfg.get("queue", "queueTextColorHover",
                    hex(OverlayConfig.queueTextColorHover), "").getString());
            OverlayConfig.queueTextColorBuiltin= hex(cfg.get("queue", "queueTextColorBuiltin",
                    hex(OverlayConfig.queueTextColorBuiltin), "").getString());
            OverlayConfig.queueTextColorPlaying= hex(cfg.get("queue", "queueTextColorPlaying",
                    hex(OverlayConfig.queueTextColorPlaying), "").getString());
            OverlayConfig.queueTextColorDragging=hex(cfg.get("queue", "queueTextColorDragging",
                    hex(OverlayConfig.queueTextColorDragging), "").getString());

            // НАСТРОЙКИ (кнопки в GuiOverlaySettings)
            OverlayConfig.settingsBtnColor     = hex(cfg.get("settings", "settingsBtnColor",
                    hex(OverlayConfig.settingsBtnColor), "").getString());
            OverlayConfig.settingsBtnHover     = hex(cfg.get("settings", "settingsBtnHover",
                    hex(OverlayConfig.settingsBtnHover), "").getString());
            OverlayConfig.settingsBtnTextColor = hex(cfg.get("settings", "settingsBtnTextColor",
                    hex(OverlayConfig.settingsBtnTextColor), "").getString());
            OverlayConfig.settingsSectionColor = hex(cfg.get("settings", "settingsSectionColor",
                    hex(OverlayConfig.settingsSectionColor), "").getString());
            OverlayConfig.settingsSliderColor  = hex(cfg.get("settings", "settingsSliderColor",
                    hex(OverlayConfig.settingsSliderColor), "").getString());

            // ГРОМКОСТЬ
            OverlayConfig.volumeBarColor       = hex(cfg.get("gui", "volumeBarColor",
                    hex(OverlayConfig.volumeBarColor), "").getString());
            OverlayConfig.volumeBarFillColor  = hex(cfg.get("gui", "volumeBarFillColor",
                    hex(OverlayConfig.volumeBarFillColor), "").getString());

            // ПЛЕЙЛИСТЫ
            OverlayConfig.playlistWidth     = cfg.getInt("playlistWidth", "playlist", OverlayConfig.playlistWidth, 80, 300, "");
            OverlayConfig.playlistBgColor   = hex(cfg.get("playlist", "playlistBgColor",   hex(OverlayConfig.playlistBgColor),   "").getString());
            OverlayConfig.playlistBgAlpha   = cfg.getInt("playlistBgAlpha", "playlist", OverlayConfig.playlistBgAlpha, 0, 255, "");
            OverlayConfig.playlistTextColor = hex(cfg.get("playlist", "playlistTextColor", hex(OverlayConfig.playlistTextColor), "").getString());
            OverlayConfig.playlistSubColor  = hex(cfg.get("playlist", "playlistSubColor",  hex(OverlayConfig.playlistSubColor),  "").getString());
            OverlayConfig.playlistSelColor  = hex(cfg.get("playlist", "playlistSelColor",  hex(OverlayConfig.playlistSelColor),  "").getString());
            OverlayConfig.playlistBtnColor  = hex(cfg.get("playlist", "playlistBtnColor",  hex(OverlayConfig.playlistBtnColor),  "").getString());
            OverlayConfig.playlistBtnHover  = hex(cfg.get("playlist", "playlistBtnHover",  hex(OverlayConfig.playlistBtnHover),  "").getString());
            OverlayConfig.playlistShowDesc  = cfg.getBoolean("playlistShowDesc", "playlist", OverlayConfig.playlistShowDesc, "");
            OverlayConfig.playlistBtnText   = hex(cfg.get("playlist", "playlistBtnText",
                    hex(OverlayConfig.playlistBtnText), "").getString());
            OverlayConfig.playlistPanelBgAlpha = cfg.getInt("playlist", "playlistPanelBgAlpha",
                    OverlayConfig.playlistPanelBgAlpha, 0, 255, "");
            OverlayConfig.playlistPanelBgAlpha     = cfg.getInt("playlist", "playlistPanelBgAlpha",
                    OverlayConfig.playlistPanelBgAlpha, 0, 255, "");
            OverlayConfig.playlistHeaderBgColor    = hex(cfg.get("playlist", "playlistHeaderBgColor",
                    hex(OverlayConfig.playlistHeaderBgColor), "").getString());
            OverlayConfig.playlistHeaderTextColor  = hex(cfg.get("playlist", "playlistHeaderTextColor",
                    hex(OverlayConfig.playlistHeaderTextColor), "").getString());
            OverlayConfig.playlistBuiltinTextColor = hex(cfg.get("playlist", "playlistBuiltinTextColor",
                    hex(OverlayConfig.playlistBuiltinTextColor), "").getString());
            OverlayConfig.playlistRowColor         = hex(cfg.get("playlist", "playlistRowColor",
                    hex(OverlayConfig.playlistRowColor), "").getString());
            OverlayConfig.playlistRowHoverColor    = hex(cfg.get("playlist", "playlistRowHoverColor",
                    hex(OverlayConfig.playlistRowHoverColor), "").getString());
            OverlayConfig.playlistBtnBorderColor   = hex(cfg.get("playlist", "playlistBtnBorderColor",
                    hex(OverlayConfig.playlistBtnBorderColor), "").getString());
            OverlayConfig.playlistBtnText          = hex(cfg.get("playlist", "playlistBtnText",
                    hex(OverlayConfig.playlistBtnText), "").getString());
            OverlayConfig.playlistEditBtnColor     = hex(cfg.get("playlist", "playlistEditBtnColor",
                    hex(OverlayConfig.playlistEditBtnColor), "").getString());
            OverlayConfig.playlistEditBtnHoverColor= hex(cfg.get("playlist", "playlistEditBtnHoverColor",
                    hex(OverlayConfig.playlistEditBtnHoverColor), "").getString());
            OverlayConfig.playlistEditBtnBorderColor=hex(cfg.get("playlist", "playlistEditBtnBorderColor",
                    hex(OverlayConfig.playlistEditBtnBorderColor), "").getString());
            OverlayConfig.playlistEditBtnTextColor = hex(cfg.get("playlist", "playlistEditBtnTextColor",
                    hex(OverlayConfig.playlistEditBtnTextColor), "").getString());
            OverlayConfig.playlistHintColor        = hex(cfg.get("playlist", "playlistHintColor",
                    hex(OverlayConfig.playlistHintColor), "").getString());
            OverlayConfig.playlistDragGhostBg      = hex(cfg.get("playlist", "playlistDragGhostBg",
                    hex(OverlayConfig.playlistDragGhostBg), "").getString());
            OverlayConfig.playlistDragGhostText    = hex(cfg.get("playlist", "playlistDragGhostText",
                    hex(OverlayConfig.playlistDragGhostText), "").getString());
            OverlayConfig.playlistDragMarkerColor  = hex(cfg.get("playlist", "playlistDragMarkerColor",
                    hex(OverlayConfig.playlistDragMarkerColor), "").getString());

            // МИКШЕР
            OverlayConfig.audioMixerName = cfg.getString("audioMixerName", "audio",
                    OverlayConfig.audioMixerName, "");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cfg.hasChanged()) cfg.save();
        }
    }

    public static void save() {
        if (cfg == null) return;
        try {
            // ОВЕРЛЕЙ
            cfg.get("overlay", "textColor",       hex(OverlayConfig.textColor),       "").set(hex(OverlayConfig.textColor));
            cfg.get("overlay", "textColorSub",    hex(OverlayConfig.textColorSub),    "").set(hex(OverlayConfig.textColorSub));
            cfg.get("overlay", "timeColor",       hex(OverlayConfig.timeColor),       "").set(hex(OverlayConfig.timeColor));
            cfg.get("overlay", "timeColorPaused", hex(OverlayConfig.timeColorPaused), "").set(hex(OverlayConfig.timeColorPaused));
            cfg.get("overlay", "bgColor",         hex(OverlayConfig.bgColor),         "").set(hex(OverlayConfig.bgColor));
            cfg.get("overlay", "lineColor",       hex(OverlayConfig.lineColor),       "").set(hex(OverlayConfig.lineColor));

            cfg.get("overlay", "bgAlpha",      OverlayConfig.bgAlpha,      "").set(OverlayConfig.bgAlpha);
            cfg.get("overlay", "overlayAlpha", OverlayConfig.overlayAlpha, "").set(OverlayConfig.overlayAlpha);

            cfg.get("overlay", "width",  OverlayConfig.width,  "").set(OverlayConfig.width);
            cfg.get("overlay", "height", OverlayConfig.height, "").set(OverlayConfig.height);

            cfg.get("overlay", "showTime",     OverlayConfig.showTime,     "").set(OverlayConfig.showTime);
            cfg.get("overlay", "showProgress", OverlayConfig.showProgress, "").set(OverlayConfig.showProgress);
            cfg.get("overlay", "showIcon",     OverlayConfig.showIcon,     "").set(OverlayConfig.showIcon);
            cfg.get("overlay", "showTitle",    OverlayConfig.showTitle,    "").set(OverlayConfig.showTitle);
            cfg.get("overlay", "showArtist",   OverlayConfig.showArtist,   "").set(OverlayConfig.showArtist);
            cfg.get("overlay", "textScale", OverlayConfig.textScale, "").set(OverlayConfig.textScale);

            cfg.get("overlay", "overlayX", OverlayConfig.overlayX, "").set(OverlayConfig.overlayX);
            cfg.get("overlay", "overlayY", OverlayConfig.overlayY, "").set(OverlayConfig.overlayY);
            cfg.get("overlay", "timeAlignRight", OverlayConfig.timeAlignRight, "")
                    .set(OverlayConfig.timeAlignRight);

            cfg.get("overlay", "overlayX", OverlayConfig.overlayX, "").set(OverlayConfig.overlayX);
            cfg.get("overlay", "overlayY", OverlayConfig.overlayY, "").set(OverlayConfig.overlayY);
            cfg.get("overlay", "timeAlignRight", OverlayConfig.timeAlignRight, "")
                    .set(OverlayConfig.timeAlignRight);

            // ОКНО ПЛЕЕРА
            cfg.get("gui", "guiBgColor",     hex(OverlayConfig.guiBgColor),     "").set(hex(OverlayConfig.guiBgColor));
            cfg.get("gui", "guiTextColor",   hex(OverlayConfig.guiTextColor),   "").set(hex(OverlayConfig.guiTextColor));
            cfg.get("gui", "guiButtonColor", hex(OverlayConfig.guiButtonColor), "").set(hex(OverlayConfig.guiButtonColor));
            cfg.get("gui", "guiButtonHover", hex(OverlayConfig.guiButtonHover), "").set(hex(OverlayConfig.guiButtonHover));
            cfg.get("gui", "guiButtonText",  hex(OverlayConfig.guiButtonText),  "").set(hex(OverlayConfig.guiButtonText));

            cfg.get("gui", "guiBgAlpha",  OverlayConfig.guiBgAlpha,  "").set(OverlayConfig.guiBgAlpha);
            cfg.get("gui", "guiDimAlpha", OverlayConfig.guiDimAlpha, "").set(OverlayConfig.guiDimAlpha);
            cfg.get("gui", "bgLogoAlpha", OverlayConfig.bgLogoAlpha, "").set(OverlayConfig.bgLogoAlpha);
            cfg.get("overlay", "LogoColor",       hex(OverlayConfig.LogoColor),       "").set(hex(OverlayConfig.LogoColor));

            // ПРАВАЯ ПАНЕЛЬ
            cfg.get("queue", "queuePanelBgColor", hex(OverlayConfig.queuePanelBgColor), "")
                    .set(hex(OverlayConfig.queuePanelBgColor));
            cfg.get("queue", "queuePanelBgAlpha", OverlayConfig.queuePanelBgAlpha, "")
                    .set(OverlayConfig.queuePanelBgAlpha);
            cfg.get("queue", "queueTextColor", hex(OverlayConfig.queueTextColor), "")
                    .set(hex(OverlayConfig.queueTextColor));
            cfg.get("queue", "queueTextColorHover", hex(OverlayConfig.queueTextColorHover), "")
                    .set(hex(OverlayConfig.queueTextColorHover));
            cfg.get("queue", "queueTextColorBuiltin", hex(OverlayConfig.queueTextColorBuiltin), "")
                    .set(hex(OverlayConfig.queueTextColorBuiltin));
            cfg.get("queue", "queueTextColorPlaying", hex(OverlayConfig.queueTextColorPlaying), "")
                    .set(hex(OverlayConfig.queueTextColorPlaying));
            cfg.get("queue", "queueTextColorDragging", hex(OverlayConfig.queueTextColorDragging), "")
                    .set(hex(OverlayConfig.queueTextColorDragging));

            // НАСТРОЙКИ (кнопки в GuiOverlaySettings)
            cfg.get("settings", "settingsBtnColor", hex(OverlayConfig.settingsBtnColor), "")
                    .set(hex(OverlayConfig.settingsBtnColor));
            cfg.get("settings", "settingsBtnHover", hex(OverlayConfig.settingsBtnHover), "")
                    .set(hex(OverlayConfig.settingsBtnHover));
            cfg.get("settings", "settingsBtnTextColor", hex(OverlayConfig.settingsBtnTextColor), "")
                    .set(hex(OverlayConfig.settingsBtnTextColor));
            cfg.get("settings", "settingsSectionColor", hex(OverlayConfig.settingsSectionColor), "")
                    .set(hex(OverlayConfig.settingsSectionColor));
            cfg.get("settings", "settingsSliderColor", hex(OverlayConfig.settingsSliderColor), "")
                    .set(hex(OverlayConfig.settingsSliderColor));

            // ГРОМКОСТЬ
            cfg.get("gui", "volumeBarColor", hex(OverlayConfig.volumeBarColor), "")
                    .set(hex(OverlayConfig.volumeBarColor));
            cfg.get("gui", "volumeBarFillColor", hex(OverlayConfig.volumeBarFillColor), "")
                    .set(hex(OverlayConfig.volumeBarFillColor));

            // ПЛЕЙЛИСТЫ
            cfg.get("playlist", "playlistWidth", OverlayConfig.playlistWidth, "").set(OverlayConfig.playlistWidth);
            cfg.get("playlist", "playlistBgColor",   hex(OverlayConfig.playlistBgColor),   "").set(hex(OverlayConfig.playlistBgColor));
            cfg.get("playlist", "playlistBgAlpha",   OverlayConfig.playlistBgAlpha,   "").set(OverlayConfig.playlistBgAlpha);
            cfg.get("playlist", "playlistTextColor", hex(OverlayConfig.playlistTextColor), "").set(hex(OverlayConfig.playlistTextColor));
            cfg.get("playlist", "playlistSubColor",  hex(OverlayConfig.playlistSubColor),  "").set(hex(OverlayConfig.playlistSubColor));
            cfg.get("playlist", "playlistSelColor",  hex(OverlayConfig.playlistSelColor),  "").set(hex(OverlayConfig.playlistSelColor));
            cfg.get("playlist", "playlistBtnColor",  hex(OverlayConfig.playlistBtnColor),  "").set(hex(OverlayConfig.playlistBtnColor));
            cfg.get("playlist", "playlistBtnHover",  hex(OverlayConfig.playlistBtnHover),  "").set(hex(OverlayConfig.playlistBtnHover));
            cfg.get("playlist", "playlistShowDesc",  OverlayConfig.playlistShowDesc,  "").set(OverlayConfig.playlistShowDesc);
            cfg.get("playlist", "playlistBtnText", hex(OverlayConfig.playlistBtnText), "")
                    .set(hex(OverlayConfig.playlistBtnText));
            cfg.get("playlist", "playlistPanelBgAlpha", OverlayConfig.playlistPanelBgAlpha, "")
                    .set(OverlayConfig.playlistPanelBgAlpha);
            cfg.get("playlist", "playlistPanelBgAlpha",     OverlayConfig.playlistPanelBgAlpha,     "").set(OverlayConfig.playlistPanelBgAlpha);
            cfg.get("playlist", "playlistHeaderBgColor",    hex(OverlayConfig.playlistHeaderBgColor),    "").set(hex(OverlayConfig.playlistHeaderBgColor));
            cfg.get("playlist", "playlistHeaderTextColor",  hex(OverlayConfig.playlistHeaderTextColor),  "").set(hex(OverlayConfig.playlistHeaderTextColor));
            cfg.get("playlist", "playlistBuiltinTextColor", hex(OverlayConfig.playlistBuiltinTextColor), "").set(hex(OverlayConfig.playlistBuiltinTextColor));
            cfg.get("playlist", "playlistRowColor",         hex(OverlayConfig.playlistRowColor),         "").set(hex(OverlayConfig.playlistRowColor));
            cfg.get("playlist", "playlistRowHoverColor",    hex(OverlayConfig.playlistRowHoverColor),    "").set(hex(OverlayConfig.playlistRowHoverColor));
            cfg.get("playlist", "playlistBtnBorderColor",   hex(OverlayConfig.playlistBtnBorderColor),   "").set(hex(OverlayConfig.playlistBtnBorderColor));
            cfg.get("playlist", "playlistBtnText",          hex(OverlayConfig.playlistBtnText),          "").set(hex(OverlayConfig.playlistBtnText));
            cfg.get("playlist", "playlistEditBtnColor",     hex(OverlayConfig.playlistEditBtnColor),     "").set(hex(OverlayConfig.playlistEditBtnColor));
            cfg.get("playlist", "playlistEditBtnHoverColor",hex(OverlayConfig.playlistEditBtnHoverColor),"").set(hex(OverlayConfig.playlistEditBtnHoverColor));
            cfg.get("playlist", "playlistEditBtnBorderColor",hex(OverlayConfig.playlistEditBtnBorderColor),"").set(hex(OverlayConfig.playlistEditBtnBorderColor));
            cfg.get("playlist", "playlistEditBtnTextColor", hex(OverlayConfig.playlistEditBtnTextColor), "").set(hex(OverlayConfig.playlistEditBtnTextColor));
            cfg.get("playlist", "playlistHintColor",        hex(OverlayConfig.playlistHintColor),        "").set(hex(OverlayConfig.playlistHintColor));
            cfg.get("playlist", "playlistDragGhostBg",      hex(OverlayConfig.playlistDragGhostBg),      "").set(hex(OverlayConfig.playlistDragGhostBg));
            cfg.get("playlist", "playlistDragGhostText",    hex(OverlayConfig.playlistDragGhostText),    "").set(hex(OverlayConfig.playlistDragGhostText));
            cfg.get("playlist", "playlistDragMarkerColor",  hex(OverlayConfig.playlistDragMarkerColor),  "").set(hex(OverlayConfig.playlistDragMarkerColor));

            // МИКШЕР
            cfg.get("audio", "audioMixerName", OverlayConfig.audioMixerName, "")
                    .set(OverlayConfig.audioMixerName);
            if (cfg.hasChanged()) cfg.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String hex(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }

    private static int hex(String s) {
        try { return (int) Long.parseLong(s.replace("#", "").trim(), 16) & 0xFFFFFF; }
        catch (Exception e) { return 0; }
    }

    private OverlayConfigIO() {}
}