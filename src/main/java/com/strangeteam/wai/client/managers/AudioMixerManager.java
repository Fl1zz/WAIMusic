/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.managers;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.List;

/** МИКШЕРЫ */

@SideOnly(Side.CLIENT)
public final class AudioMixerManager {

    public static final AudioMixerManager INSTANCE = new AudioMixerManager();

    //Список всех доступных микшеров вывода.
    private final List<Mixer.Info> mixers = new ArrayList<>();

    //Выбранный вручную микшер. null = системный по умолчанию.
    private volatile Mixer.Info selected = null;

    private AudioMixerManager() {}

    //Обновить список микшеров (вызывать при старте и по кнопке "Обновить").
    public void refresh() {
        mixers.clear();
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer m = AudioSystem.getMixer(info);
            // Нас интересуют только те, что умеют SourceDataLine (вывод звука)
            if (m.getSourceLineInfo().length > 0) {
                mixers.add(info);
            }
        }
    }

    public List<Mixer.Info> getMixers() {
        return mixers;
    }

    public Mixer.Info getSelected() {
        return selected;
    }

    public void setSelected(Mixer.Info info) {
        this.selected = info;
    }

    public String getSelectedName() {
        if (selected == null) return "Системный по умолчанию";
        return selected.getName();
    }
    public Mixer.Info findByName(String name) {
        if (name == null || name.isEmpty()) return null;
        for (Mixer.Info info : mixers) {
            if (info.getName().equals(name)) return info;
        }
        return null;
    }

    public SourceDataLine openLine(AudioFormat format, int bufferSize) throws Exception {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        if (selected == null) {
            if (!AudioSystem.isLineSupported(info)) {
                throw new IllegalStateException(
                        "SourceDataLine не поддерживает формат: " + format);
            }
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, bufferSize);
            return line;
        }

        Mixer mixer = AudioSystem.getMixer(selected);
        if (!mixer.isLineSupported(info)) {
            // Микшер не умеет этот формат — падаем на системный дефолт
            if (!AudioSystem.isLineSupported(info)) {
                throw new IllegalStateException(
                        "Микшер " + selected.getName()
                                + " и системный дефолт не поддерживают формат: " + format);
            }
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, bufferSize);
            return line;
        }

        SourceDataLine line = (SourceDataLine) mixer.getLine(info);
        line.open(format, bufferSize);
        return line;
    }
}