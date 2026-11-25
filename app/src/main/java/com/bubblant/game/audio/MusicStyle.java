package com.bubblant.game.audio;

/** Musical "moods". Each world of the game gets its own; they get darker as the game gets harder. */
public enum MusicStyle {
    //        bpm  root  scale        progression         lead              bass          kick                 snare                hat                  dens  arp   pad   horror
    MENU(108, 60, Sequencer.MAJOR, new int[]{0, 5, 3, 4}, Sequencer.TRI, 0.20f, 0.30f, "x...o...x...o.x.", "x.......x.......", "....x.......x...", "..x...x...x...x.", 0.50f, 0.05f, 0.00f, false),
    POND(116, 60, Sequencer.MAJOR, new int[]{0, 5, 3, 4}, Sequencer.TRI, 0.22f, 0.25f, "x...o...x...o.x.", "x.......x.......", "....x.......x...", "..x...x...x...x.", 0.58f, 0.00f, 0.00f, false),
    RIVER(124, 62, Sequencer.MIXO, new int[]{0, 6, 3, 0}, Sequencer.SQUARE, 0.11f, 0.18f, "x.x.o.x.x.x.o.xo", "x...x...x...x...", "....x.......x..x", "x.x.x.x.x.x.x.x.", 0.62f, 0.00f, 0.00f, false),
    LAKE(132, 65, Sequencer.MAJOR, new int[]{0, 4, 5, 3}, Sequencer.TRI, 0.19f, 0.30f, "x..x..x.o..x..x.", "x..x..x...x..x..", "....x.......x...", ".xx..xx..xx..xx.", 0.60f, 0.06f, 0.00f, false),
    CAVE(96, 57, Sequencer.MINOR, new int[]{0, 5, 3, 4}, Sequencer.SINE, 0.14f, 0.55f, "x.......x.......", "x.........x.....", "............x...", "....x.......x...", 0.30f, 0.09f, 0.06f, false),
    STORM(150, 52, Sequencer.MINOR, new int[]{0, 5, 6, 4}, Sequencer.SAW, 0.09f, 0.15f, "xxxoxxxoxxxoxxfo", "x...x...x...x.x.", "....x.......x...", "xxxxxxxxxxxxxxxx", 0.70f, 0.00f, 0.04f, false),
    ABYSS(72, 45, Sequencer.PHRYG, new int[]{0, 0, 1, 0}, Sequencer.SINE, 0.10f, 1.20f, "................", "x.x.............", "................", "................", 0.12f, 0.00f, 0.05f, true);

    public final int bpm, root;
    public final int[] scale, prog;
    public final int leadWave;
    public final float leadVol, leadDecay;
    public final String bass, kick, snare, hat;
    public final float density, arpVol, padVol;
    public final boolean horror;

    MusicStyle(int bpm, int root, int[] scale, int[] prog, int leadWave, float leadVol, float leadDecay,
               String bass, String kick, String snare, String hat, float density, float arpVol, float padVol, boolean horror) {
        this.bpm = bpm;
        this.root = root;
        this.scale = scale;
        this.prog = prog;
        this.leadWave = leadWave;
        this.leadVol = leadVol;
        this.leadDecay = leadDecay;
        this.bass = bass;
        this.kick = kick;
        this.snare = snare;
        this.hat = hat;
        this.density = density;
        this.arpVol = arpVol;
        this.padVol = padVol;
        this.horror = horror;
    }

    public static MusicStyle forWorld(int world) {
        switch (com.bubblant.game.engine.WorldTheme.of(world).music) {
            case 0: return POND;
            case 1: return LAKE;
            case 2: return RIVER;
            case 3: return CAVE;
            case 4: return STORM;
            default: return ABYSS;
        }
    }
}
