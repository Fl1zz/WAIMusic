package com.strangeteam.wai.client.gui.create;

import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** Пользовательская иконка. Используется в TextureBinder */

@SideOnly(Side.CLIENT)
public class UserIconTexture extends SimpleTexture {

    private final File file;

    public UserIconTexture(ResourceLocation loc, File file) {
        super(loc);
        this.file = file;
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        // Удаляем старую текстуру, если была
        this.deleteGlTexture();

        BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            image = null;
        }

        if (image == null) {
            // fallback — заглушка 16x16
            image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++)
                    image.setRGB(x, y, 0xFF333333);
        }

        net.minecraft.client.renderer.texture.TextureUtil.uploadTextureImageAllocate(
                this.getGlTextureId(), image, false, false);
    }
}