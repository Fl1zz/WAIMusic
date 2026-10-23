/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.info;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;

@SideOnly(Side.CLIENT)
public class TrackInfo {

    public final String id;
    public final ResourceLocation ogg;
    public final File file;
    public final ResourceLocation icon;
    public final File iconFile;
    public final ResourceLocation iconLocation;
    public final String title;
    public final String artist;
    public final long durationMs;

    // Встроенный
    public TrackInfo(String id, String title, String artist, long durationMs) {
        this.id = id;
        this.ogg = new ResourceLocation("waimusic", "sounds/music/" + id + ".ogg");
        this.file = null;
        this.icon = new ResourceLocation("waimusic", "textures/tracks/" + id + ".png");
        this.iconFile = null;
        this.iconLocation = this.icon;
        this.title = title;
        this.artist = artist;
        this.durationMs = durationMs;
    }

    // Пользовательский
    public TrackInfo(String id, File file, File iconFile, String title, String artist, long durationMs) {
        this.id = id;
        this.ogg = null;
        this.file = file;
        this.icon = null;
        this.iconFile = iconFile;
        this.iconLocation = new ResourceLocation(
                "waimusic_user",
                iconFile != null ? id : "missing"
        );
        this.title = title;
        this.artist = artist;
        this.durationMs = durationMs;
    }

    public boolean isUserTrack() {
        return file != null;
    }

    @Override
    public String toString() {
        return title + " — " + artist;
    }
}