package com.strangeteam.wai.client.gui.create;

import com.strangeteam.wai.client.info.TrackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Установщик иконок и защита от сбоев */

@SideOnly(Side.CLIENT)
public final class TextureBinder {

    public static void bindTrackIcon(TrackInfo track) {
        if (track == null) return;

        TextureManager tm = Minecraft.getMinecraft().getTextureManager();

        if (track.isUserTrack()) {
            if (track.iconFile == null) return;

            ResourceLocation loc = track.iconLocation;

            // Проверяем — вдруг уже загружена
            ITextureObject existing = tm.getTexture(loc);
            if (existing == null) {
                tm.loadTexture(loc, new UserIconTexture(loc, track.iconFile));
            }
            tm.bindTexture(loc);
        } else {
            if (track.icon == null) return;
            tm.bindTexture(track.icon);
        }
    }

    private TextureBinder() {}
}