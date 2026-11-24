package com.bubblant.game.audio;

import java.util.List;
import java.util.Random;

/** Synthesized sound effects. */
public final class Sfx {
    public static final int POP = 0, SUGAR = 1, AIR = 2, BUMP = 3, WIN = 4, SINK = 5, THUNDER = 6,
            FRIEND = 7, CLICK = 8, SPLASH = 9, TICK = 10, WHOOSH = 11, STAR = 12, HUH = 13;

    private static final int SR = AudioEngine.SR;
    private static final Random R = new Random(7);

    static void build(List<float[]> out) {
        out.add(pop());
        out.add(sweep(520, 1250, 0.13f, 0.5f, 0, true));     // SUGAR
        out.add(air());
        out.add(sweep(140, 55, 0.12f, 0.8f, 0, false));      // BUMP
        out.add(arp(new int[]{72, 76, 79, 84, 88}, 0.09f, 0.45f)); // WIN
        out.add(sink());
        out.add(thunder());
        out.add(arp(new int[]{84, 91}, 0.06f, 0.3f));        // FRIEND
        out.add(sweep(1800, 1200, 0.03f, 0.35f, 0, false));  // CLICK
        out.add(noiseBurst(0.25f, 0.4f, 0.25f));             // SPLASH
        out.add(sweep(1500, 1500, 0.05f, 0.3f, 0, false));   // TICK
        out.add(whoosh());
        out.add(arp(new int[]{88, 93, 100}, 0.05f, 0.3f));   // STAR
        out.add(sweep(300, 520, 0.18f, 0.4f, 1, false));     // HUH
    }

    private static float[] sweep(float f0, float f1, float dur, float vol, int wave, boolean twoTone) {
        int n = (int) (dur * SR * (twoTone ? 2 : 1));
        float[] d = new float[n];
        double ph = 0;
        int half = (int) (dur * SR);
        for (int i = 0; i < n; i++) {
            int li = twoTone ? i % half : i;
            float t = (float) li / half;
            float f = f0 + (f1 - f0) * t;
            if (twoTone && i >= half) f *= 1.5f;
            ph += f / SR;
            float x = wave == 0 ? (float) Math.sin(ph * 2 * Math.PI) : (float) (1 - 4 * Math.abs((ph % 1) - 0.5));
            float env = (1 - t) * Math.min(1f, li / 60f);
            d[i] = x * env * vol;
        }
        return d;
    }

    private static float[] pop() {
        int n = (int) (0.22f * SR);
        float[] d = new float[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float f = 900 - 700 * t;
            ph += f / SR;
            float env = (float) Math.exp(-t * 9);
            d[i] = ((R.nextFloat() * 2 - 1) * 0.6f * (float) Math.exp(-t * 25) + (float) Math.sin(ph * 2 * Math.PI) * 0.6f) * env;
        }
        return d;
    }

    private static float[] air() {
        int n = (int) (0.3f * SR);
        float[] d = new float[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float local = (t * 3) % 1f;
            float f = 400 + 900 * local;
            ph += f / SR;
            d[i] = (float) Math.sin(ph * 2 * Math.PI) * (1 - local) * 0.45f;
        }
        return d;
    }

    private static float[] arp(int[] notes, float each, float vol) {
        int per = (int) (each * SR);
        int n = per * notes.length + (int) (0.3f * SR);
        float[] d = new float[n];
        for (int k = 0; k < notes.length; k++) {
            double f = 440.0 * Math.pow(2, (notes[k] - 69) / 12.0);
            int start = k * per;
            int len = n - start;
            for (int i = 0; i < len; i++) {
                float env = (float) Math.exp(-i / (SR * 0.15f)) * Math.min(1f, i / 40f);
                double ph = f * i / SR;
                float x = (float) (1 - 4 * Math.abs((ph % 1) - 0.5)) * 0.7f + (float) Math.sin(ph * 4 * Math.PI) * 0.3f;
                d[start + i] += x * env * vol;
            }
        }
        return d;
    }

    private static float[] sink() {
        int n = (int) (1.1f * SR);
        float[] d = new float[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float f = 420 - 330 * t + (float) Math.sin(t * 60) * 40;
            ph += f / SR;
            float glub = 0.5f + 0.5f * (float) Math.sin(t * 38);
            d[i] = (float) Math.sin(ph * 2 * Math.PI) * (1 - t) * glub * 0.5f;
        }
        return d;
    }

    private static float[] noiseBurst(float dur, float vol, float cut) {
        int n = (int) (dur * SR);
        float[] d = new float[n];
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            lp += cut * ((R.nextFloat() * 2 - 1) - lp);
            d[i] = lp * (1 - t) * (1 - t) * vol;
        }
        return d;
    }

    private static float[] thunder() {
        int n = (int) (1.8f * SR);
        float[] d = new float[n];
        float lp = 0, lp2 = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            lp += 0.05f * ((R.nextFloat() * 2 - 1) - lp);
            lp2 += 0.3f * (lp - lp2);
            float env = (float) Math.exp(-t * 3) * (0.7f + 0.3f * (float) Math.sin(t * 25));
            d[i] = lp2 * env * 3.2f;
        }
        return d;
    }

    private static float[] whoosh() {
        int n = (int) (0.9f * SR);
        float[] d = new float[n];
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float cut = 0.03f + 0.15f * (float) Math.sin(t * Math.PI);
            lp += cut * ((R.nextFloat() * 2 - 1) - lp);
            d[i] = lp * (float) Math.sin(t * Math.PI) * 0.9f;
        }
        return d;
    }

    private Sfx() {}
}
