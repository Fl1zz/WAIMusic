/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.info;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class PlaylistInfo {

    public String id;
    public String name;
    public String description;
    public final List<String> trackIds = new ArrayList<>();
    public final boolean builtin;

    /** Встроенный плейлист */
    public PlaylistInfo(String id, String name, String description, List<String> trackIds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.builtin = true;
        if (trackIds != null) this.trackIds.addAll(trackIds);
    }

    /** Пользовательский плейлист */
    public PlaylistInfo(String id, String name, String description, List<String> trackIds, boolean builtin) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.builtin = builtin;
        if (trackIds != null) this.trackIds.addAll(trackIds);
    }

    public int size() { return trackIds.size(); }

    @Override
    public String toString() { return name; }
}