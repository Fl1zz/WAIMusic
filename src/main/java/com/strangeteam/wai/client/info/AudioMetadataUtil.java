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
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** Чтение title/artist/обложки из тегов аудиофайла (OGG/MP3/FLAC/WAV) */
@SideOnly(Side.CLIENT)
public final class AudioMetadataUtil {

    public static class Meta {
        public String title;
        public String artist;
        public File   coverFile;
    }

    public static Meta read(File audio) {
        if (audio == null || !audio.isFile()) return null;
        try {
            AudioFile af = AudioFileIO.read(audio);
            Tag tag = af.getTag();

            Meta m = new Meta();

            if (tag != null) {
                m.title  = safe(tag.getFirst(FieldKey.TITLE));
                m.artist = safe(tag.getFirst(FieldKey.ARTIST));
                if (m.artist == null) {
                    m.artist = safe(tag.getFirst(FieldKey.ALBUM_ARTIST));
                }
            }

            try {
                if (tag != null) {
                    Artwork art = tag.getFirstArtwork();
                    if (art != null) {
                        m.coverFile = saveArtworkToTemp(art);
                    }
                }
            } catch (Throwable ignored) {
            }

            boolean hasTitle  = m.title  != null && !m.title.isEmpty();
            boolean hasArtist = m.artist != null && !m.artist.isEmpty();
            if (!hasTitle && !hasArtist && m.coverFile == null) {
                return null;
            }
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    public static long readDurationMs(File audio) {
        try {
            AudioFile af = AudioFileIO.read(audio);
            return af.getAudioHeader().getTrackLength() * 1000L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static File saveArtworkToTemp(Artwork art) {
        try {
            byte[] data = art.getBinaryData();
            if (data == null || data.length == 0) return null;

            String ext = guessExtension(art.getMimeType(), art.getBinaryData());
            File tmp = File.createTempFile("waimusic_cover_", "." + ext);
            tmp.deleteOnExit();

            try (OutputStream os = new FileOutputStream(tmp)) {
                os.write(data);
            }
            return tmp;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String guessExtension(String mime, byte[] data) {
        if (mime != null) {
            String m = mime.toLowerCase();
            if (m.contains("jpeg") || m.contains("jpg")) return "jpg";
            if (m.contains("png"))  return "png";
            if (m.contains("bmp"))  return "bmp";
            if (m.contains("gif"))  return "gif";
        }
        if (data != null && data.length >= 4) {
            int b0 = data[0] & 0xFF, b1 = data[1] & 0xFF;
            if (b0 == 0x89 && b1 == 0x50) return "png";
            if (b0 == 0xFF && b1 == 0xD8) return "jpg";
            if (b0 == 0x47 && b1 == 0x49) return "gif";
            if (b0 == 0x42 && b1 == 0x4D) return "bmp";
        }
        return "jpg";
    }

    private static String safe(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private AudioMetadataUtil() {}
}