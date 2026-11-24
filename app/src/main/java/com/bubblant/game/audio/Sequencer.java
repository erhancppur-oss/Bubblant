package com.bubblant.game.audio;

import java.util.ArrayList;
import java.util.Random;

/** Step sequencer + generative melody for one {@link MusicStyle}. */
final class Sequencer {
    static final int[] MAJOR = {0, 2, 4, 5, 7, 9, 11};
    static final int[] MINOR = {0, 2, 3, 5, 7, 8, 10};
    static final int[] MIXO = {0, 2, 4, 5, 7, 9, 10};
    static final int[] PHRYG = {0, 1, 3, 5, 7, 8, 10};

    static final int SINE = 0, TRI = 1, SQUARE = 2, SAW = 3, NOISE = 4, KICK = 5, SNARE = 6, HAT = 7;

    private static final int STEPS = 16 * 8; // 8 bars of 16ths

    private final MusicStyle st;
    private final Random rnd;
    private final double samplesPerStep;
    private double counter;
    private int step;
    private int loops;
    private final ArrayList<Voice> voices = new ArrayList<>();
    private int[] melody;
    volatile float tension;
    private int noise = 0x1234567;

    Sequencer(MusicStyle st) {
        this.st = st;
        this.rnd = new Random(st.ordinal() * 977L + 31);
        samplesPerStep = AudioEngine.SR * 60.0 / st.bpm / 4.0;
        genMelody();
    }

    private int degToMidi(int deg) {
        int oct = Math.floorDiv(deg, 7);
        int idx = Math.floorMod(deg, 7);
        return st.root + 12 * oct + st.scale[idx];
    }

    private void genMelody() {
        melody = new int[STEPS];
        java.util.Arrays.fill(melody, -2);
        int deg = 7 + rnd.nextInt(3);
        boolean sixteenths = st.density > 0.65f;
        for (int bar = 0; bar < 8; bar++) {
            int chord = st.prog[bar % 4];
            for (int s = 0; s < 16; s += (sixteenths && rnd.nextFloat() < 0.3f) ? 1 : 2) {
                boolean strong = s % 4 == 0;
                boolean play = s == 0 ? rnd.nextFloat() < 0.9f : rnd.nextFloat() < st.density * (strong ? 1f : 0.7f);
                int i = bar * 16 + s;
                if (!play) {
                    if (rnd.nextFloat() < 0.35f) melody[i] = -1;
                    continue;
                }
                if (strong) {
                    int[] tones = {chord, chord + 2, chord + 4};
                    int best = deg, bestD = 99;
                    for (int t : tones) {
                        for (int o = 0; o <= 2; o++) {
                            int cand = t + 7 * o;
                            int d = Math.abs(cand - deg) + rnd.nextInt(2);
                            if (d < bestD) { bestD = d; best = cand; }
                        }
                    }
                    deg = best;
                } else {
                    int[] moves = {-2, -1, -1, 1, 1, 2};
                    deg += moves[rnd.nextInt(moves.length)];
                }
                if (deg < 4) deg = 5;
                if (deg > 14) deg = 12;
                melody[i] = degToMidi(deg);
            }
        }
        // repeat first phrase for catchiness, last bar stays different
        System.arraycopy(melody, 0, melody, 64, 48);
    }

    private static double freq(int midi) {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }

    private Voice add(int wave, double f, float amp, float attackSec, float decaySec, float sus, float lenSec, float relSec, float cut) {
        if (voices.size() > 28) voices.remove(0);
        Voice v = new Voice();
        v.wave = wave;
        v.inc = f / AudioEngine.SR;
        v.amp = amp;
        v.a = Math.max(1, (int) (attackSec * AudioEngine.SR));
        v.decay = (float) Math.exp(-1.0 / (decaySec * AudioEngine.SR));
        v.sus = sus;
        v.len = (int) (lenSec * AudioEngine.SR);
        v.rel = (float) Math.exp(-1.0 / (relSec * AudioEngine.SR));
        v.cut = cut;
        voices.add(v);
        return v;
    }

    private float stepSec() { return (float) (samplesPerStep / AudioEngine.SR); }

    private void trigger() {
        int s16 = step % 16;
        int bar = (step / 16) % 8;
        int chord = st.prog[bar % 4];
        int chordRoot = degToMidi(chord);
        float ss = stepSec();
        boolean tense = tension > 0.5f;

        // drums
        if (st.kick.charAt(s16) == 'x' || (tense && s16 % 4 == 0)) {
            Voice k = add(KICK, st.horror ? 60 : 150, st.horror ? 0.9f : 0.75f, 0.001f, st.horror ? 0.25f : 0.12f, 0f, 0.05f, 0.05f, 1f);
            k.slide = (float) Math.pow(st.horror ? 0.3 : 0.28, 1.0 / (0.12 * AudioEngine.SR));
        }
        if (st.snare.charAt(s16) == 'x') add(SNARE, 190, 0.35f, 0.001f, 0.09f, 0f, 0.05f, 0.04f, 0.7f);
        if (st.hat.charAt(s16) == 'x' || (tense && !st.horror)) add(HAT, 0, s16 % 2 == 0 ? 0.10f : 0.06f, 0.001f, 0.025f, 0f, 0.02f, 0.01f, 1f);

        // bass
        char b = st.bass.charAt(s16);
        if (b != '.') {
            int m = chordRoot - 24;
            if (b == 'o') m += 12;
            if (b == 'f') m += 7;
            int wave = st.leadWave == SAW ? SAW : TRI;
            add(wave, freq(m), wave == SAW ? 0.22f : 0.32f, 0.003f, 0.25f, 0.4f, ss * 1.6f, 0.05f, wave == SAW ? 0.12f : 0.5f);
        }

        // pad at bar start
        if (s16 == 0 && st.padVol > 0) {
            float len = ss * 16;
            add(TRI, freq(chordRoot - 12), st.padVol, len * 0.3f, 4f, 1f, len, 0.4f, 0.12f);
            add(TRI, freq(degToMidi(chord + 2) - 12), st.padVol * 0.8f, len * 0.3f, 4f, 1f, len, 0.4f, 0.12f);
            if (st.horror) add(SINE, freq(chordRoot - 6), st.padVol * 0.9f, len * 0.5f, 4f, 1f, len, 0.6f, 1f); // tritone, creepy
            else add(TRI, freq(degToMidi(chord + 4) - 12), st.padVol * 0.8f, len * 0.3f, 4f, 1f, len, 0.4f, 0.12f);
        }

        // horror drone + eerie notes
        if (st.horror) {
            if (s16 == 0 && bar % 2 == 0) {
                add(SINE, freq(st.root - 12), 0.22f, 1.2f, 6f, 1f, ss * 30, 1f, 1f);
                add(SAW, freq(st.root - 11), 0.05f, 2f, 6f, 1f, ss * 30, 1f, 0.03f);
            }
            if (s16 % 2 == 0 && rnd.nextFloat() < 0.07f + tension * 0.08f) {
                int m = degToMidi(14 + rnd.nextInt(5)) + (rnd.nextBoolean() ? 1 : 0);
                add(SINE, freq(m), 0.07f, 0.4f, 1.5f, 0.5f, 0.9f, 0.8f, 1f);
            }
        }

        // arpeggio
        if (st.arpVol > 0) {
            int[] arp = {chord, chord + 2, chord + 4, chord + 7};
            int m = degToMidi(arp[s16 % 4]);
            add(TRI, freq(m), st.arpVol, 0.003f, 0.12f, 0f, ss, 0.05f, 0.35f);
        }

        // melody
        int mel = melody[step % STEPS];
        if (mel >= 0) {
            int hold = 1;
            while (hold < 8 && melody[(step + hold) % STEPS] == -2) hold++;
            float cut = st.leadWave == SQUARE ? 0.25f : st.leadWave == SAW ? 0.3f : 0.7f;
            add(st.leadWave, freq(mel), st.leadVol * (tense ? 1.2f : 1f), 0.005f, st.leadDecay, 0.25f, ss * hold * 0.9f, 0.06f, cut);
            if (st == MusicStyle.POND || st == MusicStyle.MENU) // sparkly octave
                add(SINE, freq(mel + 12), st.leadVol * 0.25f, 0.002f, 0.12f, 0f, ss, 0.05f, 1f);
        }

        step++;
        if (step % STEPS == 0) {
            loops++;
            if (loops % 3 == 0) genMelody();
        }
    }

    private float noise() {
        noise ^= noise << 13;
        noise ^= noise >>> 17;
        noise ^= noise << 5;
        return (noise & 0xFFFF) / 32768f - 1f;
    }

    void render(float[] buf, int n, float vol) {
        for (int i = 0; i < n; i++) {
            if (counter <= 0) {
                trigger();
                counter += samplesPerStep;
            }
            counter -= 1;
            float sum = 0;
            for (int j = voices.size() - 1; j >= 0; j--) {
                Voice v = voices.get(j);
                float x = v.sample(this);
                if (v.dead) { voices.remove(j); continue; }
                sum += x;
            }
            buf[i] += sum * vol;
        }
    }

    static final class Voice {
        int wave;
        double phase, inc;
        float amp, decay, sus, rel, cut, lp, env, prevNoise, slide = 1f;
        int t, a, len;
        boolean dead;

        float sample(Sequencer s) {
            if (t < a) env = (float) t / a;
            else if (t < len) {
                if (t == a) env = 1f;
                env = sus + (env - sus) * decay;
            } else {
                env *= rel;
                if (env < 0.0005f) { dead = true; return 0; }
            }
            t++;
            float x;
            switch (wave) {
                case SINE: x = (float) Math.sin(phase * 2 * Math.PI); break;
                case TRI: x = (float) (1 - 4 * Math.abs(phase - 0.5)); break;
                case SQUARE: x = phase < 0.5 ? 0.8f : -0.8f; break;
                case SAW: x = (float) (2 * phase - 1); break;
                case KICK:
                    x = (float) Math.sin(phase * 2 * Math.PI);
                    inc *= slide;
                    if (inc < 42.0 / AudioEngine.SR) inc = 42.0 / AudioEngine.SR;
                    break;
                case SNARE: {
                    float nz = s.noise();
                    x = nz * 0.7f + (float) Math.sin(phase * 2 * Math.PI) * 0.3f;
                    break;
                }
                case HAT: {
                    float nz = s.noise();
                    x = nz - prevNoise;
                    prevNoise = nz;
                    break;
                }
                default: x = s.noise();
            }
            phase += inc;
            if (phase >= 1) phase -= 1;
            lp += cut * (x - lp);
            return lp * env * amp;
        }
    }
}
