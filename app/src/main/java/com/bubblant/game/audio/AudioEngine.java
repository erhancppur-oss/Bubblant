package com.bubblant.game.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.ArrayList;

/**
 * Tiny real-time synthesizer: procedurally generated music (no audio files needed,
 * no licensing issues) plus synthesized sound effects, mixed into one AudioTrack.
 */
public final class AudioEngine implements Runnable {
    public static final int SR = 22050;
    private static final int BLOCK = 512;

    private static AudioEngine instance;

    public static synchronized AudioEngine get() {
        if (instance == null) instance = new AudioEngine();
        return instance;
    }

    private Thread thread;
    private volatile boolean running;
    private volatile boolean paused;
    private volatile boolean musicOn = true;
    private volatile boolean sfxOn = true;
    private volatile MusicStyle pendingStyle;
    private volatile MusicStyle currentStyle;
    private volatile float tension;
    private Sequencer seq;
    private final Object lock = new Object();
    private final ArrayList<float[]> sfxData = new ArrayList<>();
    private final ArrayList<int[]> sfxPlaying = new ArrayList<>(); // [sfxIndex, position, volume*1000]
    private float musicFade = 1f;

    private AudioEngine() {
        Sfx.build(sfxData);
    }

    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "bubblant-audio");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void setMusicOn(boolean on) { musicOn = on; }
    public void setSfxOn(boolean on) { sfxOn = on; }
    public void setTension(float t) { tension = t; }

    public void setPaused(boolean p) {
        paused = p;
        if (!p) synchronized (lock) { lock.notifyAll(); }
    }

    public void playMusic(MusicStyle style) {
        if (style != currentStyle) pendingStyle = style;
    }

    public void play(int sfx) { play(sfx, 1f); }

    public void play(int sfx, float vol) {
        if (!sfxOn || sfx < 0 || sfx >= sfxData.size()) return;
        synchronized (sfxPlaying) {
            if (sfxPlaying.size() > 10) sfxPlaying.remove(0);
            sfxPlaying.add(new int[]{sfx, 0, (int) (vol * 1000)});
        }
    }

    @Override
    public void run() {
        int minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufBytes = Math.max(minBuf, BLOCK * 2 * 4);
        AudioTrack track;
        try {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(SR)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(bufBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            track.play();
        } catch (Exception e) {
            running = false;
            return;
        }

        float[] mix = new float[BLOCK];
        short[] out = new short[BLOCK];
        while (running) {
            if (paused) {
                track.pause();
                synchronized (lock) {
                    while (paused && running) {
                        try { lock.wait(500); } catch (InterruptedException ignored) {}
                    }
                }
                track.play();
            }
            MusicStyle ps = pendingStyle;
            if (ps != null) {
                // quick fade-out before switching
                musicFade -= 0.08f;
                if (musicFade <= 0f || seq == null) {
                    pendingStyle = null;
                    currentStyle = ps;
                    seq = new Sequencer(ps);
                    musicFade = 0f;
                }
            } else if (musicFade < 1f) {
                musicFade = Math.min(1f, musicFade + 0.02f);
            }

            java.util.Arrays.fill(mix, 0f);
            if (seq != null) {
                seq.tension = tension;
                // keep sequencer running even when muted so it resumes in time
                seq.render(mix, BLOCK, musicOn ? 0.5f * musicFade : 0f);
            }
            synchronized (sfxPlaying) {
                for (int i = sfxPlaying.size() - 1; i >= 0; i--) {
                    int[] p = sfxPlaying.get(i);
                    float[] d = sfxData.get(p[0]);
                    float v = p[2] / 1000f * 0.8f;
                    int pos = p[1];
                    int n = Math.min(BLOCK, d.length - pos);
                    for (int j = 0; j < n; j++) mix[j] += d[pos + j] * v;
                    p[1] += n;
                    if (p[1] >= d.length) sfxPlaying.remove(i);
                }
            }
            for (int i = 0; i < BLOCK; i++) {
                float x = mix[i];
                // soft clip
                if (x > 3f) x = 1f;
                else if (x < -3f) x = -1f;
                else x = x * (27f + x * x) / (27f + 9f * x * x);
                out[i] = (short) (x * 30000f);
            }
            track.write(out, 0, BLOCK);
        }
        track.stop();
        track.release();
    }
}
