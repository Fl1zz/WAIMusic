package com.strangeteam.wai.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

/** Экран, который сам уменьшается, если не помещается в окно */
public abstract class GuiFitScreen extends GuiScreen {

    protected float uiScale = 1f;

    /** Минимальная ширина/высота, которой хватает для вёрстки (в GUI-пикселях) */
    protected abstract int designWidth();
    protected abstract int designHeight();

    @Override
    public void setWorldAndResolution(Minecraft mc, int w, int h) {
        uiScale = Math.min(1f, Math.min(w / (float) designWidth(), h / (float) designHeight()));
        int vw = (int) Math.ceil(w / uiScale);
        int vh = (int) Math.ceil(h / uiScale);
        super.setWorldAndResolution(mc, vw, vh);   // initGui() увидит виртуальные width/height
    }

    @Override
    public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(uiScale, uiScale, 1f);
        drawContent((int) (mouseX / uiScale), (int) (mouseY / uiScale), partialTicks);
        GlStateManager.popMatrix();
    }

    /** Вместо drawScreen. Координаты мыши уже в виртуальных пикселях */
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}