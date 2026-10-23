/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.managers;

import com.google.gson.*;
import com.strangeteam.wai.client.info.TrackInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.resources.I18n;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@SideOnly(Side.CLIENT)
public final class UserTracksManager {

    public static final UserTracksManager INSTANCE = new UserTracksManager();

    private File baseDir;
    private File musicDir;
    private File iconsDir;
    private File indexFile;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private UserTracksManager() {}

    public void init(File configDir) {
        baseDir  = new File(configDir, "waimusic");
        musicDir = new File(baseDir, "music");
        iconsDir = new File(baseDir, "icons");
        indexFile = new File(baseDir, "tracks.json");

        if (!musicDir.exists()) musicDir.mkdirs();
        if (!iconsDir.exists()) iconsDir.mkdirs();
        if (!indexFile.exists()) {
            try { indexFile.createNewFile(); } catch (IOException ignored) {}
            writeIndex(new JsonArray());
        }
    }

    public File getMusicDir() { return musicDir; }
    public File getIconsDir() { return iconsDir; }
    public File getBaseDir()  { return baseDir; }

    // Индекс
    public JsonArray readIndex() {
        try {
            if (!indexFile.exists()) return new JsonArray();
            String content = new String(Files.readAllBytes(indexFile.toPath()), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) return new JsonArray();
            JsonElement el = new JsonParser().parse(content);
            if (el.isJsonArray()) return el.getAsJsonArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JsonArray();
    }

    public void writeIndex(JsonArray arr) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(indexFile), StandardCharsets.UTF_8)) {
            gson.toJson(arr, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Загрузка
    public List<TrackInfo> loadAll() {
        List<TrackInfo> list = new ArrayList<>();
        JsonArray arr = readIndex();

        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            try {
                String id     = o.get("id").getAsString();
                String title  = o.has("title")  ? o.get("title").getAsString()  : id;
                String artist = o.has("artist") ? o.get("artist").getAsString() : "Unknown";
                String file   = o.get("file").getAsString();
                String icon   = o.has("icon")   ? o.get("icon").getAsString()   : null;
                long   dur    = o.has("duration") ? o.get("duration").getAsLong() : 0L;

                File audio = new File(musicDir, file);
                if (!audio.exists()) continue;

                File iconF = (icon != null) ? new File(iconsDir, icon) : null;
                if (iconF != null && !iconF.exists()) iconF = null;

                list.add(new TrackInfo(id, audio, iconF, title, artist, dur));
            } catch (Exception ignored) {}
        }
        return list;
    }

    // Добавление
    public String addTrack(String id, String title, String artist,
                           File audioSource, File iconSource) {
        try {
            if (id == null || id.trim().isEmpty()) return I18n.format("gui.wai.user_tracks.error.id_empty");
            id = id.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");

            // Проверка на дубликат
            JsonArray arr = readIndex();
            for (JsonElement el : arr) {
                if (el.isJsonObject() && el.getAsJsonObject().get("id").getAsString().equals(id)) {
                    return I18n.format("gui.wai.user_tracks.error.id_exists");
                }
            }

            // Копируем аудио
            String ext = getExtension(audioSource.getName());
            String audioName = id + "." + ext;
            File audioDst = new File(musicDir, audioName);
            Files.copy(audioSource.toPath(), audioDst.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Копируем иконку
            String iconName = null;
            if (iconSource != null && iconSource.exists()) {
                String iext = getExtension(iconSource.getName());
                iconName = id + "." + iext;
                File iconDst = new File(iconsDir, iconName);
                Files.copy(iconSource.toPath(), iconDst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Длительность
            long dur = MusicManager.INSTANCE.probeDuration(audioDst);

            JsonObject o = new JsonObject();
            o.addProperty("id", id);
            o.addProperty("title", title);
            o.addProperty("artist", artist);
            o.addProperty("file", audioName);
            if (iconName != null) o.addProperty("icon", iconName);
            o.addProperty("duration", dur);
            arr.add(o);
            writeIndex(arr);

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка: " + e.getMessage();
        }
    }

    // Удаление
    public boolean removeTrack(String id) {
        try {
            JsonArray arr = readIndex();
            JsonArray newArr = new JsonArray();
            boolean removed = false;

            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                if (o.get("id").getAsString().equals(id)) {
                    String file = o.get("file").getAsString();
                    new File(musicDir, file).delete();
                    if (o.has("icon")) {
                        new File(iconsDir, o.get("icon").getAsString()).delete();
                    }
                    removed = true;
                } else {
                    newArr.add(o);
                }
            }

            if (removed) writeIndex(newArr);
            return removed;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Сохранение порядка пользовательских треков
    public void saveUserOrder(List<TrackInfo> allTracks) {
        // Существующие записи по ID, чтобы не потерять title/artist/file/icon/duration
        JsonArray oldArr = readIndex();
        Map<String, JsonObject> byId = new HashMap<>();
        for (JsonElement el : oldArr) {
            if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                byId.put(o.get("id").getAsString(), o);
            }
        }

        JsonArray newArr = new JsonArray();
        for (TrackInfo t : allTracks) {
            if (!t.isUserTrack()) continue;
            JsonObject o = byId.get(t.id);
            if (o != null) newArr.add(o);
        }

        writeIndex(newArr);
    }

    private static String getExtension(String name) {
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(i + 1).toLowerCase() : "ogg";
    }
}