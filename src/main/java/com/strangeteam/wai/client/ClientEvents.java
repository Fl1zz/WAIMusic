/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client;

import com.strangeteam.wai.client.gui.GuiMusicPlayer;
import com.strangeteam.wai.client.gui.GuiOverlay;
import com.strangeteam.wai.client.managers.MusicManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ClientEvents {

    public static KeyBinding openPlayer;
    public static KeyBinding toggleHud;
    public static KeyBinding playPause;
    public static KeyBinding nextTrack;

    public ClientEvents() {
        openPlayer = new KeyBinding("key.waimusic.open",      Keyboard.KEY_P, "key.categories.waimusic");
        toggleHud  = new KeyBinding("key.waimusic.togglehud",       Keyboard.KEY_H, "key.categories.waimusic");
        playPause  = new KeyBinding("key.waimusic.playpause", Keyboard.KEY_J, "key.categories.waimusic");
        nextTrack  = new KeyBinding("key.waimusic.next",      Keyboard.KEY_N, "key.categories.waimusic");

        ClientRegistry.registerKeyBinding(openPlayer);
        ClientRegistry.registerKeyBinding(toggleHud);
        ClientRegistry.registerKeyBinding(playPause);
        ClientRegistry.registerKeyBinding(nextTrack);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (openPlayer.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiMusicPlayer());
        }
        if (toggleHud.isPressed()) {
            GuiOverlay.hidden = !GuiOverlay.hidden;
        }
        if (playPause.isPressed()) {
            MusicManager.INSTANCE.togglePause();
        }
        if (nextTrack.isPressed()) {
            MusicManager.INSTANCE.next();
        }
    }
}