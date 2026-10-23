/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.managers;

import com.strangeteam.wai.client.info.PlaylistInfo;
import com.strangeteam.wai.client.info.TrackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Ядро (загрузка, декодирование и др.) */

@SideOnly(Side.CLIENT)
public class MusicManager {

    public static final MusicManager INSTANCE = new MusicManager();

    public final List<TrackInfo> tracks = new CopyOnWriteArrayList<>();

    // ПОТОКИ

    /**
     * Worker используется только для тяжёлых операций:
     *  - загрузка ресурса
     *  - чтение метаданных
     *  - декодирование OGG -> PCM
     *  - запуск следующего трека
     *
     * Он НЕ занимается непосредственным воспроизведением.
     * Приоритет понижен, чтобы декодер не конкурировал с рендером.
     */
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WAIMusic-Worker");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 2);
        return t;
    });

    private final ExecutorService lineCloser = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WAIMusic-LineCloser");
        t.setDaemon(true);
        return t;
    });

    private volatile Thread playbackThread;

    // СОСТОЯНИЕ
    private volatile TrackInfo current;

    private volatile boolean playing = false;
    private volatile boolean paused = false;
    private volatile boolean loop = false;

    private volatile float volume = 1.0f;
    private volatile boolean muted = false;

    private volatile long positionMs = 0;
    private volatile long durationMs = 0;
    private volatile long lastTickMs = 0;

    // АУДИО

    private volatile SourceDataLine line;
    private volatile AudioFormat format;
    private volatile byte[] pcmBuffer;
    private volatile int pcmPos = 0;
    private final AtomicInteger generation = new AtomicInteger();
    private final Object stateLock = new Object();
    private MusicManager() {
    }

    public AudioInputStream getMp3Stream(File mp3File) throws Exception {
        return AudioSystem.getAudioInputStream(mp3File);
    }

    // СКАНИРОВАНИЕНЕ

    public void reloadTracks() {
        TrackInfo oldCurrent = current;
        List<TrackInfo> newTracks = new ArrayList<>();

        // Встроенные
        try {
            IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
            List<IResource> all = rm.getAllResources(new ResourceLocation("waimusic", ""));
            Set<String> ids = new TreeSet<>();

            for (IResource res : all) {
                String path = res.getResourceLocation().getResourcePath();
                if (!path.startsWith("sounds/music/")) continue;
                if (!path.endsWith(".ogg")) continue;
                String file = path.substring(path.lastIndexOf('/') + 1);
                ids.add(file.substring(0, file.length() - 4));
            }

            if (ids.isEmpty()) readIndexJson(ids);

            for (String id : ids) {
                TrackInfo info = loadMeta(id);
                if (info != null) newTracks.add(info);
            }
        } catch (Exception ignored) {}

        // Пользовательские
        newTracks.addAll(UserTracksManager.INSTANCE.loadAll());

        tracks.clear();
        tracks.addAll(newTracks);

        // Восстанавливаем текущий
        if (oldCurrent != null) {
            boolean found = false;
            for (TrackInfo t : tracks) {
                if (t.id.equals(oldCurrent.id)) {
                    current = t;
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Трек удалён — останавливаем
                stopInternal();
                current = null;
            }
        }
    }

    public long probeDuration(File audioFile) {
        try {
            String name = audioFile.getName().toLowerCase();

            if (name.endsWith(".ogg")) {
                AudioInputStream ais = new VorbisAudioFileReader().getAudioInputStream(audioFile);
                AudioFormat fmt = ais.getFormat();
                long frames = ais.getFrameLength();
                ais.close();
                if (frames > 0 && fmt.getFrameRate() > 0) {
                    return (long)(frames * 1000.0 / fmt.getFrameRate());
                }
            } else {
                // MP3 / WAV / AIFF
                AudioFileFormat aff = AudioSystem.getAudioFileFormat(audioFile);
                AudioFormat fmt = aff.getFormat();

                // Для MP3 mp3spi отдаёт duration в свойствах
                Object dur = aff.properties().get("duration");
                if (dur instanceof Long) {
                    long micros = (Long) dur;
                    if (micros > 0) return micros / 1000L;
                }

                // Иначе считаем по фреймам (для WAV/AIFF работает)
                long frames = aff.getFrameLength();
                if (frames > 0 && fmt.getFrameRate() > 0) {
                    return (long)(frames * 1000.0 / fmt.getFrameRate());
                }
            }
        } catch (Throwable ignored) {}
        return 0L;
    }

    private void readIndexJson(Set<String> ids) {
        try {
            IResource res =
                    Minecraft.getMinecraft()
                            .getResourceManager()
                            .getResource(
                                    new ResourceLocation(
                                            "waimusic",
                                            "sounds/music/tracks.json"
                                    )
                            );

            try (InputStream in = res.getInputStream()) {

                com.google.gson.JsonArray arr =
                        new com.google.gson.JsonParser()
                                .parse(
                                        new InputStreamReader(
                                                in,
                                                "UTF-8"
                                        )
                                )
                                .getAsJsonArray();

                for (int i = 0; i < arr.size(); i++) {
                    ids.add(arr.get(i).getAsString());
                }
            }

        } catch (Exception ignored) {
        }
    }

    /**
     * Метаданные:
     1. .json рядом с OGG
     2. Vorbis/Ogg теги через Jaudiotagger
     3. fallback
     */
    private TrackInfo loadMeta(String id) {

        // JSON
        try {
            IResource meta =
                    Minecraft.getMinecraft()
                            .getResourceManager()
                            .getResource(
                                    new ResourceLocation(
                                            "waimusic",
                                            "sounds/music/" +
                                                    id +
                                                    ".json"
                                    )
                            );

            try (InputStream in = meta.getInputStream()) {

                com.google.gson.JsonObject o =
                        new com.google.gson.JsonParser()
                                .parse(
                                        new InputStreamReader(
                                                in,
                                                "UTF-8"
                                        )
                                )
                                .getAsJsonObject();

                String title =
                        o.has("title")
                                ? o.get("title").getAsString()
                                : id;

                String artist =
                        o.has("artist")
                                ? o.get("artist").getAsString()
                                : "Unknown";

                long dur =
                        o.has("duration")
                                ? o.get("duration").getAsLong()
                                : 0L;

                return new TrackInfo(
                        id,
                        title,
                        artist,
                        dur
                );
            }

        } catch (Exception ignored) {
        }

        // Jaudiotagger
        try {
            IResource res =
                    Minecraft.getMinecraft()
                            .getResourceManager()
                            .getResource(
                                    new ResourceLocation(
                                            "waimusic",
                                            "sounds/music/" +
                                                    id +
                                                    ".ogg"
                                    )
                            );

            File tmp =
                    File.createTempFile(
                            "waimusic_meta_",
                            ".ogg"
                    );

            tmp.deleteOnExit();

            try (InputStream in = res.getInputStream();
                 FileOutputStream out =
                         new FileOutputStream(tmp)) {

                byte[] buf = new byte[8192];

                int n;

                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }

            AudioFile af = AudioFileIO.read(tmp);

            Tag tag = af.getTag();

            String title = id;
            String artist = "Unknown";

            if (tag != null) {

                String t = tag.getFirst(FieldKey.TITLE);

                String a = tag.getFirst(FieldKey.ARTIST);

                if (t != null && !t.isEmpty()) {title = t;}

                if (a != null && !a.isEmpty()) {artist = a;}
            }

            long dur =
                    af.getAudioHeader() != null
                            ? af.getAudioHeader()
                            .getTrackLength() * 1000L
                            : 0L;
            tmp.delete();

            return new TrackInfo(
                    id,
                    title,
                    artist,
                    dur
            );

        } catch (Throwable t) {}

        // Fallback
        return new TrackInfo(
                id,
                id,
                "Unknown",
                0L
        );
    }

    // Публичный API
    public TrackInfo getCurrent() {
        return current;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean v) {
        loop = v;
    }

    public float getVolume() {
        return volume;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean m) {
        muted = m;
        applyVolume();
    }

    public void toggleMute() {
        setMuted(!muted);
    }

    public long getPositionMs() {
        return positionMs;
    }

    public long getDurationMs() {
        return durationMs > 0
                ? durationMs
                : estimateDuration();
    }

    // PLAYLISTS
    private volatile PlaylistInfo activePlaylist;

    public PlaylistInfo getActivePlaylist() { return activePlaylist; }

    public void setActivePlaylist(PlaylistInfo pl) {
        this.activePlaylist = pl;
    }

    public List<TrackInfo> getPlaybackQueue() {
        if (activePlaylist != null) {
            List<TrackInfo> list = PlaylistsManager.INSTANCE.resolveTracks(activePlaylist);
            if (!list.isEmpty()) return list;
        }
        return tracks;
    }

    // PLAY
    public void play(TrackInfo track) {
        if (track == null) return;

        stopInternal();
        final int gen = generation.get();
        current = track;

        worker.submit(() -> {
            if (gen != generation.get()) return; // за время ожидания выбрали другой трек

            try {
                DecodedPcm pcm = decode(track, gen);
                if (pcm == null) return;          // отменено во время декодирования
                if (gen != generation.get()) return;

                SourceDataLine newLine = AudioMixerManager.INSTANCE.openLine(pcm.format, 8192 * 4);
                if (newLine == null) {
                    if (gen == generation.get()) { playing = false; paused = false; }
                    return;
                }
                try {
                    newLine.start();
                } catch (Throwable t) {
                    closeLineAsync(newLine);
                    if (gen == generation.get()) { playing = false; paused = false; }
                    return;
                }

                synchronized (stateLock) {
                    if (gen != generation.get()) {   // успели переключить, пока открывали линию
                        closeLineAsync(newLine);
                        return;
                    }

                    format = pcm.format;
                    pcmBuffer = pcm.data;
                    pcmPos = 0;
                    positionMs = 0;
                    durationMs = pcm.data.length * 1000L
                            / (pcm.format.getFrameSize() * (long) pcm.format.getFrameRate());

                    line = newLine;
                    applyVolume();

                    paused = false;
                    playing = true;
                    lastTickMs = System.currentTimeMillis();

                    Thread t = new Thread(() -> pumpLoop(newLine, gen), "WAIMusic-Playback");
                    t.setDaemon(true);
                    playbackThread = t;
                    t.start();
                }
            } catch (Throwable e) {
                if (gen == generation.get()) {
                    playing = false;
                    paused = false;
                }
            }
        });
    }

    // PAUSE
    public void togglePause() {
        if (!playing) {return;}
        paused = !paused;
        if (!paused) {lastTickMs = System.currentTimeMillis();
        }
    }

    // STOP
    public void stop() {
        stopInternal();
    }

    // NEXT
    public void next() {
        List<TrackInfo> queue = getPlaybackQueue();
        if (queue.isEmpty()) return;
        int i = current == null ? -1 : queue.indexOf(current);
        play(queue.get((i + 1) % queue.size()));
    }

    // PREV
    public void prev() {
        List<TrackInfo> queue = getPlaybackQueue();
        if (queue.isEmpty()) return;
        int i = current == null ? 0 : queue.indexOf(current);
        play(queue.get((i - 1 + queue.size()) % queue.size()));
    }

    // VOLUME
    public void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        if (volume > 0f) muted = false;
        applyVolume();
    }

    // SEEK
    public void seek(long ms) {
        if (!playing || pcmBuffer == null || format == null) {
            return;
        }

        if (ms < 0) {
            ms = 0;
        }

        long duration = getDurationMs();

        if (duration > 0 && ms > duration) {
            ms = duration;
        }

        int frameSize = format.getFrameSize();

        if (frameSize <= 0) {
            return;
        }

        int bytePos = (int) (ms * frameSize * format.getFrameRate() / 1000.0);

        bytePos -= bytePos % frameSize;

        if (bytePos < 0) {
            bytePos = 0;
        }

        if (bytePos > pcmBuffer.length) {
            bytePos = pcmBuffer.length - (pcmBuffer.length % frameSize);
        }

        pcmPos = bytePos;

        positionMs = currentPosMs();

        lastTickMs = System.currentTimeMillis();
    }

    private static final class DecodedPcm {
        final byte[] data;
        final AudioFormat format;
        DecodedPcm(byte[] d, AudioFormat f) { data = d; format = f; }
    }

    private static final class PcmOut extends ByteArrayOutputStream {
        PcmOut(int size) { super(size); }
        byte[] toExactArray() {
            return count == buf.length ? buf : Arrays.copyOf(buf, count);
        }
    }

    private DecodedPcm decode(TrackInfo track, int gen) throws Exception {
        File tmp;
        boolean deleteTmp = false;

        if (track.isUserTrack()) {
            tmp = track.file;
        } else {
            IResource res = Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(track.ogg);

            tmp = File.createTempFile("waimusic_", ".ogg");
            tmp.deleteOnExit();
            deleteTmp = true;

            try (InputStream in = res.getInputStream();
                 FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (gen != generation.get()) return null;
                    out.write(buf, 0, n);
                }
            }
        }

        AudioInputStream sourceStream = null;
        AudioInputStream pcmStream = null;

        try {
            String name = tmp.getName().toLowerCase();
            if (name.endsWith(".ogg")) {
                sourceStream = new VorbisAudioFileReader().getAudioInputStream(tmp);
            } else {
                sourceStream = AudioSystem.getAudioInputStream(tmp);
            }

            if (sourceStream == null) {
                throw new IOException("Не удалось открыть аудиопоток: " + tmp.getName());
            }

            AudioFormat srcFormat = sourceStream.getFormat();

            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    srcFormat.getSampleRate(),
                    16,
                    srcFormat.getChannels(),
                    srcFormat.getChannels() * 2,
                    srcFormat.getSampleRate(),
                    false
            );

            pcmStream = AudioSystem.getAudioInputStream(pcmFormat, sourceStream);

            long frames = sourceStream.getFrameLength();
            long guess = frames > 0
                    ? frames * pcmFormat.getFrameSize()
                    : Math.max(tmp.length() * 12L, 1L << 20);

            int initial = (int) Math.min(
                    Math.max(guess, 1L << 20) + 65536,
                    Integer.MAX_VALUE - 16
            );

            PcmOut out = new PcmOut(initial);
            byte[] chunk = new byte[65536];
            int read;
            while ((read = pcmStream.read(chunk, 0, chunk.length)) != -1) {
                if (gen != generation.get()) return null;
                if (read > 0) out.write(chunk, 0, read);
            }

            byte[] data = out.toExactArray();
            if (data.length == 0) throw new IOException("Пустой PCM stream");

            AudioFormat fmt = pcmStream.getFormat();
            if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
                throw new IllegalStateException("Не PCM_SIGNED: " + fmt);
            if (fmt.getSampleSizeInBits() != 16)
                throw new IllegalStateException("Не 16-bit: " + fmt);
            if (fmt.isBigEndian())
                throw new IllegalStateException("Big Endian PCM не поддерживается");

            return new DecodedPcm(data, fmt);
        } finally {
            if (pcmStream != null) try { pcmStream.close(); } catch (Exception ignored) {}
            if (sourceStream != null) try { sourceStream.close(); } catch (Exception ignored) {}
            if (deleteTmp) tmp.delete();
        }
    }

    // OGG -> PCM
    private void startTrack(TrackInfo track) throws Exception {

        File tmp;
        boolean deleteTmp = false;

        if (track.isUserTrack()) {
            // Пользовательский — используем напрямую
            tmp = track.file;
        } else {
            // Встроенный — копируем во временный
            IResource res = Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(track.ogg);

            tmp = File.createTempFile("waimusic_", ".ogg");
            tmp.deleteOnExit();
            deleteTmp = true;

            try (InputStream in = res.getInputStream();
                 FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
        }

        AudioInputStream sourceStream = null;
        AudioInputStream pcmStream = null;

        try {
            String name = tmp.getName().toLowerCase();

            if (name.endsWith(".ogg")) {
                sourceStream = new VorbisAudioFileReader().getAudioInputStream(tmp);
            } else {
                // mp3, wav, aiff — mp3spi/vorbisspi разрулят по SPI
                sourceStream = AudioSystem.getAudioInputStream(tmp);
            }

            if (sourceStream == null) {
                throw new IOException("Не удалось открыть аудиопоток: " + tmp.getName());
            }

            AudioFormat srcFormat = sourceStream.getFormat();

            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    srcFormat.getSampleRate(),
                    16,
                    srcFormat.getChannels(),
                    srcFormat.getChannels() * 2,
                    srcFormat.getSampleRate(),
                    false
            );

            pcmStream = AudioSystem.getAudioInputStream(pcmFormat, sourceStream);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 1024);
            byte[] buffer = new byte[8192];
            int read;

            while ((read = pcmStream.read(buffer, 0, buffer.length)) != -1) {
                if (read > 0) baos.write(buffer, 0, read);
            }

            pcmBuffer = baos.toByteArray();

            if (pcmBuffer.length == 0) {
                throw new IOException("Пустой PCM stream");
            }

            format = pcmStream.getFormat();

        } finally {
            if (pcmStream != null) try { pcmStream.close(); } catch (Exception ignored) {}
            if (sourceStream != null) try { sourceStream.close(); } catch (Exception ignored) {}
            if (deleteTmp) tmp.delete();
        }

        pcmPos = 0;
        positionMs = 0;

        if (format == null) throw new IllegalStateException("PCM format == null");

        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
            throw new IllegalStateException("Не PCM_SIGNED: " + format);

        if (format.getSampleSizeInBits() != 16)
            throw new IllegalStateException("Не 16-bit: " + format);

        if (format.isBigEndian())
            throw new IllegalStateException("Big Endian PCM не поддерживается");

        SourceDataLine newLine = AudioMixerManager.INSTANCE.openLine(format, 8192 * 4);

        line = newLine;
        applyVolume();
        newLine.start();

        durationMs = pcmBuffer.length * 1000L / (format.getFrameSize() * (long) format.getFrameRate());
    }

    // PLAYBACK LOOP
    private void pumpLoop(final SourceDataLine myLine, final int gen) {
        boolean naturalEnd = false;

        try {
            while (playing && gen == generation.get()) {
                if (!myLine.isOpen()) break;

                if (paused) {
                    Thread.sleep(20);
                    continue;
                }

                byte[] buffer = pcmBuffer;
                if (buffer == null) break;

                if (pcmPos >= buffer.length) {
                    if (loop) {
                        pcmPos = 0;
                        positionMs = 0;
                        continue;
                    } else {
                        naturalEnd = true;
                        playing = false;
                        break;
                    }
                }

                int chunk = Math.min(4096, buffer.length - pcmPos);

                int written;
                try {
                    written = myLine.write(buffer, pcmPos, chunk);
                } catch (IllegalStateException e) {
                    break;
                }

                if (written <= 0) {
                    Thread.sleep(5);
                    continue;
                }

                pcmPos += written;
                positionMs = currentPosMs();
                lastTickMs = System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable ignored) {
        } finally {
            // Дать буферу линии доиграть хвост, чтобы не обрезать конец трека.
            if (naturalEnd) {
                try { myLine.drain(); } catch (Exception ignored) {}
            }

            try { myLine.stop(); } catch (Exception ignored) {}
            try { myLine.close(); } catch (Exception ignored) {}

            synchronized (stateLock) {
                if (line == myLine) line = null;
                if (playbackThread == Thread.currentThread()) playbackThread = null;
            }

            if (naturalEnd && gen == generation.get()) {
                playing = false;
                paused = false;

                TrackInfo finishedTrack = current;
                List<TrackInfo> queue = getPlaybackQueue();
                if (!queue.isEmpty()) {
                    int index = finishedTrack == null ? -1 : queue.indexOf(finishedTrack);
                    int nextIndex = (index + 1) % queue.size();
                    play(queue.get(nextIndex));
                } else {
                    playing = false;
                    paused = false;
                }
            }
        }
    }


    // ТЕКУЩАЯ ПОЗИЦИЯ
    private long currentPosMs() {

        AudioFormat f = format;

        if (f == null) {
            return positionMs;
        }

        int frameSize = f.getFrameSize();

        float frameRate = f.getFrameRate();

        if (frameSize <= 0 || frameRate <= 0) {
            return positionMs;
        }

        return pcmPos * 1000L / (frameSize * (long) frameRate);
    }

    // ДЛИТЕЛЬНОСТЬ
    private long estimateDuration() {

        AudioFormat f = format;

        byte[] buffer = pcmBuffer;

        if (f == null || buffer == null) {
            return 0;
        }

        int frameSize = f.getFrameSize();

        float frameRate = f.getFrameRate();

        if (frameSize <= 0 || frameRate <= 0) {
            return 0;
        }

        return buffer.length * 1000L / (frameSize * (long) frameRate);
    }

    // VOLUME
    private void applyVolume() {
        SourceDataLine currentLine = line;
        if (currentLine == null) return;
        try {
            FloatControl gain = (FloatControl)
                    currentLine.getControl(FloatControl.Type.MASTER_GAIN);

            float minDb = gain.getMinimum();
            float maxDb = gain.getMaximum();

            float db;
            if (muted) {
                db = Math.max(minDb, -80f);
            } else {
                float floorDb = Math.min(0f, Math.max(minDb, -60f));
                float v = Math.max(0f, Math.min(1f, volume));
                db = floorDb + (0f - floorDb) * v;
            }

            db = Math.max(minDb, Math.min(maxDb, db));
            gain.setValue(db);
        } catch (Exception ignored) {
        }
    }

    // STOP INTERNAL
    private void closeLineAsync(SourceDataLine l) {
        if (l == null) return;
        lineCloser.execute(() -> {
            try { l.stop(); } catch (Exception ignored) {}
            try { l.flush(); } catch (Exception ignored) {}
            try { l.close(); } catch (Exception ignored) {}
        });
    }

    private void stopInternal() {
        SourceDataLine old;
        synchronized (stateLock) {
            generation.incrementAndGet();  // отменяет всё, что ещё декодируется/ждёт в очереди
            playing = false;
            paused = false;

            Thread thread = playbackThread;
            if (thread != null && thread != Thread.currentThread()) {
                thread.interrupt();
            }

            old = line;
            line = null;

            pcmBuffer = null;
            pcmPos = 0;
            positionMs = 0;
            durationMs = 0;
        }
        closeLineAsync(old);
    }

    // ПЕРЕЗАПУСК

    public void restartCurrent() {
        TrackInfo cur = current;
        if (cur == null) return;

        long pos = positionMs;
        boolean wasPlaying = playing;
        boolean wasPaused = paused;

        play(cur);

        if (pos > 0) {
            new Thread(() -> {
                for (int i = 0; i < 200; i++) {
                    if (pcmBuffer != null && format != null) break;
                    try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                }
                seek(pos);
                if (wasPaused) paused = true;
                else if (!wasPlaying) {}
            }, "WAIMusic-RestartSeek").start();
        }
    }
}