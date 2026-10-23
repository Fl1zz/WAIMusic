/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai;

import com.strangeteam.wai.client.ClientEvents;
import com.strangeteam.wai.client.managers.MusicManager;
import com.strangeteam.wai.client.managers.PlaylistsManager;
import com.strangeteam.wai.client.managers.UserTracksManager;
import com.strangeteam.wai.client.managers.AudioMixerManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;


import java.io.File;
import com.strangeteam.wai.client.gui.cfg.OverlayConfigIO;

// Привет, Дима

@Mod(
        modid = WAIMusic.MODID,
        name = WAIMusic.NAME,
        version = WAIMusic.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        clientSideOnly = true
)
public class WAIMusic {

    public static final String MODID   = "waimusic";
    public static final String NAME    = "WAI Music Player";
    public static final String VERSION = "30.0";

    @Mod.Instance(MODID)
    public static WAIMusic instance;

    public static Logger logger;

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void preInit(FMLPreInitializationEvent e) {
        logger = e.getModLog();
        MusicManager.INSTANCE.reloadTracks();
        File configDir = e.getModConfigurationDirectory();
        UserTracksManager.INSTANCE.init(configDir);
        PlaylistsManager.INSTANCE.init(configDir);
        MusicManager.INSTANCE.reloadTracks();
        AudioMixerManager.INSTANCE.refresh();
        OverlayConfigIO.init(new File(configDir, "wai-overlay.cfg"));
    }

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        MinecraftForge.EVENT_BUS.register(new com.strangeteam.wai.client.gui.GuiOverlay());
    }
}