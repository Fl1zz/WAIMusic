/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.managers;

import com.google.gson.*;
import com.strangeteam.wai.client.info.PlaylistInfo;
import com.strangeteam.wai.client.info.TrackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@SideOnly(Side.CLIENT)
public final class PlaylistsManager {

    public static final PlaylistsManager INSTANCE = new PlaylistsManager();

    /** Все плейлисты (встроенные + пользовательские) */
    public final List<PlaylistInfo> playlists = new CopyOnWriteArrayList<>();

    private File baseDir;
    private File playlistsFile;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private PlaylistsManager() {}

    public void init(File configDir) {
        baseDir = new File(configDir, "waimusic");
        if (!baseDir.exists()) baseDir.mkdirs();
        playlistsFile = new File(baseDir, "playlists.json");
        if (!playlistsFile.exists()) {
            try { playlistsFile.createNewFile(); } catch (IOException ignored) {}
            writeUserPlaylists(new JsonArray());
        }
        reload();
    }

    // Загрузка

    public void reload() {
        List<PlaylistInfo> result = new ArrayList<>();

        // Встроенные
        result.addAll(loadBuiltinPlaylists());

        // Пользовательские
        result.addAll(loadUserPlaylists());

        playlists.clear();
        playlists.addAll(result);
    }

    private List<PlaylistInfo> loadBuiltinPlaylists() {
        List<PlaylistInfo> list = new ArrayList<>();
        try {
            IResource res = Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation("waimusic", "playlists.json"));
            try (InputStream in = res.getInputStream()) {
                JsonElement el = new JsonParser().parse(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                if (el.isJsonArray()) {
                    for (JsonElement e : el.getAsJsonArray()) {
                        PlaylistInfo p = parsePlaylist(e, true);
                        if (p != null) list.add(p);
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    private List<PlaylistInfo> loadUserPlaylists() {
        List<PlaylistInfo> list = new ArrayList<>();
        JsonArray arr = readUserPlaylists();
        for (JsonElement e : arr) {
            PlaylistInfo p = parsePlaylist(e, false);
            if (p != null) list.add(p);
        }
        return list;
    }

    private PlaylistInfo parsePlaylist(JsonElement el, boolean builtin) {
        if (!el.isJsonObject()) return null;
        try {
            JsonObject o = el.getAsJsonObject();
            String id    = o.get("id").getAsString();
            String name  = o.has("name") ? o.get("name").getAsString() : id;
            String desc  = o.has("description") ? o.get("description").getAsString() : "";

            List<String> trackIds = new ArrayList<>();
            if (o.has("tracks") && o.get("tracks").isJsonArray()) {
                for (JsonElement t : o.getAsJsonArray("tracks")) {
                    if (t.isJsonPrimitive()) trackIds.add(t.getAsString());
                }
            }
            return new PlaylistInfo(id, name, desc, trackIds, builtin);
        } catch (Exception e) {
            return null;
        }
    }

    // Сохранение

    public JsonArray readUserPlaylists() {
        try {
            if (!playlistsFile.exists()) return new JsonArray();
            String content = new String(Files.readAllBytes(playlistsFile.toPath()), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) return new JsonArray();
            JsonElement el = new JsonParser().parse(content);
            if (el.isJsonArray()) return el.getAsJsonArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JsonArray();
    }

    public void writeUserPlaylists(JsonArray arr) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(playlistsFile), StandardCharsets.UTF_8)) {
            gson.toJson(arr, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAll() {
        JsonArray arr = new JsonArray();
        for (PlaylistInfo p : playlists) {
            if (p.builtin) continue;
            JsonObject o = new JsonObject();
            o.addProperty("id", p.id);
            o.addProperty("name", p.name);
            o.addProperty("description", p.description);
            JsonArray tracks = new JsonArray();
            for (String tid : p.trackIds) tracks.add(tid);
            o.add("tracks", tracks);
            arr.add(o);
        }
        writeUserPlaylists(arr);
    }

    // API

    public PlaylistInfo getById(String id) {
        if (id == null) return null;
        for (PlaylistInfo p : playlists) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    public PlaylistInfo getFirst() {
        return playlists.isEmpty() ? null : playlists.get(0);
    }

    /** Все треки плейлиста (в порядке из trackIds) */
    public List<TrackInfo> resolveTracks(PlaylistInfo pl) {
        List<TrackInfo> out = new ArrayList<>();
        if (pl == null) return out;
        for (String tid : pl.trackIds) {
            for (TrackInfo t : MusicManager.INSTANCE.tracks) {
                if (t.id.equals(tid)) {
                    out.add(t);
                    break;
                }
            }
        }
        return out;
    }

    // Создание / удаление / редактирование

    public String createPlaylist(String id, String name, String description) {
        if (id == null || id.trim().isEmpty()) return "ID не может быть пустым";
        id = id.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");

        if (getById(id) != null) return "Плейлист с таким ID уже существует";

        PlaylistInfo p = new PlaylistInfo(id, name, description, null, false);
        playlists.add(p);
        saveAll();
        return null;
    }

    public boolean movePlaylist(int from, int to) {
        List<PlaylistInfo> user = new ArrayList<>();
        for (PlaylistInfo p : playlists) if (!p.builtin) user.add(p);
        if (from < 0 || from >= user.size()) return false;
        if (to < 0) to = 0;
        if (to > user.size()) to = user.size();
        if (to == from || to == from + 1) return false;

        PlaylistInfo moved = user.remove(from);
        if (to > from) to--;
        user.add(to, moved);

        List<PlaylistInfo> rebuilt = new ArrayList<>();
        for (PlaylistInfo p : playlists) if (p.builtin) rebuilt.add(p);
        rebuilt.addAll(user);

        playlists.clear();
        playlists.addAll(rebuilt);
        saveAll();
        return true;
    }

    public boolean renamePlaylist(String id, String newName) {
        PlaylistInfo p = getById(id);
        if (p == null || p.builtin) return false;
        if (newName == null || newName.trim().isEmpty()) return false;
        p.name = newName.trim();
        saveAll();
        return true;
    }

    public boolean removePlaylist(String id) {
        PlaylistInfo p = getById(id);
        if (p == null || p.builtin) return false;
        playlists.remove(p);
        saveAll();
        return true;
    }

    public boolean addTrackToPlaylist(String playlistId, String trackId) {
        PlaylistInfo p = getById(playlistId);
        if (p == null || p.builtin) return false;
        if (p.trackIds.contains(trackId)) return false;
        p.trackIds.add(trackId);
        saveAll();
        return true;
    }

    public boolean removeTrackFromPlaylist(String playlistId, String trackId) {
        PlaylistInfo p = getById(playlistId);
        if (p == null || p.builtin) return false;
        boolean ok = p.trackIds.remove(trackId);
        if (ok) saveAll();
        return ok;
    }

    public boolean moveTrackInPlaylist(String playlistId, int from, int to) {
        PlaylistInfo p = getById(playlistId);
        if (p == null || p.builtin) return false;
        if (from < 0 || from >= p.trackIds.size()) return false;
        if (to < 0) to = 0;
        if (to > p.trackIds.size()) to = p.trackIds.size();

        String moved = p.trackIds.remove(from);
        if (to > from) to--;
        p.trackIds.add(to, moved);
        saveAll();
        return true;
    }

    /** Импорт плейлиста из внешнего JSON-файла */
    public String importPlaylist(File jsonFile) {
        try {
            String content = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
            JsonElement el = new JsonParser().parse(content);

            JsonObject o;
            if (el.isJsonObject()) {
                o = el.getAsJsonObject();
            } else if (el.isJsonArray()) {
                o = new JsonObject();
                o.addProperty("id", stripExt(jsonFile.getName()));
                o.addProperty("name", stripExt(jsonFile.getName()));
                o.addProperty("description", "Imported");
                o.add("tracks", el.getAsJsonArray());
            } else {
                return "Неверный формат JSON";
            }

            String id = o.has("id") ? o.get("id").getAsString()
                    : stripExt(jsonFile.getName());
            id = id.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");

            // Если такой id уже есть — добавляем суффикс
            String baseId = id;
            int n = 1;
            while (getById(id) != null) {
                id = baseId + "_" + (n++);
            }

            String name = o.has("name") ? o.get("name").getAsString() : id;
            String desc = o.has("description") ? o.get("description").getAsString() : "Imported";

            List<String> trackIds = new ArrayList<>();
            if (o.has("tracks") && o.get("tracks").isJsonArray()) {
                for (JsonElement t : o.getAsJsonArray("tracks")) {
                    if (t.isJsonPrimitive()) trackIds.add(t.getAsString());
                }
            }

            PlaylistInfo p = new PlaylistInfo(id, name, desc, trackIds, false);
            playlists.add(p);
            saveAll();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка: " + e.getMessage();
        }
    }

    /** Экспорт плейлиста в файл */
    public boolean exportPlaylist(PlaylistInfo p, File out) {
        if (p == null) return false;
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
            JsonObject o = new JsonObject();
            o.addProperty("id", p.id);
            o.addProperty("name", p.name);
            o.addProperty("description", p.description);
            JsonArray arr = new JsonArray();
            for (String tid : p.trackIds) arr.add(tid);
            o.add("tracks", arr);
            gson.toJson(o, w);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String stripExt(String n) {
        int i = n.lastIndexOf('.');
        return i >= 0 ? n.substring(0, i) : n;
    }
}