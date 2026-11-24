package com.bubblant.game.engine;

/**
 * Difficulty recipe for each of the 60 levels (6 worlds x 10 levels).
 * Levels are generated deterministically from this recipe so every player gets the same maze.
 */
public final class LevelSpec {
    public static final int COUNT = 60;
    public static final int PER_WORLD = 10;
    public static final int WORLDS = 6;

    public int number, world, cols, rows;
    public long seed;
    public float braid;
    public int urchins, fish, currents, whirlpools, rescue, eels;
    public float wind, gustiness, currentStrength, fishSpeed, popSpeed;
    public boolean rain, lightning, shrink, timed;
    /** light radius in cells, 0 = no darkness */
    public float darkness;
    /** key of a new-mechanic tip shown at level start, or null */
    public String tip;

    public static LevelSpec of(int n) {
        LevelSpec s = new LevelSpec();
        s.number = n;
        int w = (n - 1) / PER_WORLD;
        int k = (n - 1) % PER_WORLD;
        s.world = w;
        s.seed = n * 7919L + 12345L;
        s.cols = 3 + Math.min(4, (n + 4) / 12);
        s.rows = 4 + (n * 11) / 60 + (k % 3);
        if (n <= 2) { s.cols = 3; s.rows = 4 + n; }
        s.braid = clamp(0.6f - n * 0.009f, 0.06f, 1f);
        if (n <= 3) s.braid = 0.9f;
        // early levels are forgiving, later ones pop easily
        s.popSpeed = 410f - n * 2.4f;
        s.fishSpeed = 70f + n * 1.5f;
        s.currentStrength = 150f + n * 3f;

        switch (w) {
            case 0: // Sunny Pond - learn to steer
                s.urchins = k >= 3 ? 1 + k / 3 : 0;
                s.fish = k >= 8 ? 1 : 0;
                s.rescue = (k == 5 || k == 9) ? 1 : 0;
                s.shrink = k == 7;
                break;
            case 1: // Lazy River - currents
                s.currents = 2 + k / 2;
                s.urchins = 2 + k / 3;
                s.fish = 1 + k / 4;
                s.rescue = k == 9 ? 2 : (k == 3 || k == 7) ? 1 : 0;
                s.shrink = k == 5 || k == 8;
                s.timed = k == 4 || k == 9;
                break;
            case 2: // Windy Lake - wind gusts
                s.wind = 70 + k * 10;
                s.gustiness = 0.5f + k * 0.05f;
                s.currents = 1 + k / 3;
                s.urchins = 2 + k / 3;
                s.fish = 1 + k / 3;
                s.rescue = k == 9 ? 2 : (k == 2 || k == 6) ? 1 : 0;
                s.shrink = k == 4 || k == 8;
                s.timed = k == 3 || k == 7;
                break;
            case 3: // Dark Cave - darkness + whirlpools
                s.darkness = 2.8f - k * 0.06f;
                s.urchins = 3 + k / 2;
                s.fish = 1 + k / 3;
                s.whirlpools = k >= 5 ? 1 + (k - 5) / 3 : 0;
                s.currents = k / 3;
                s.shrink = k % 2 == 1;
                s.rescue = k == 8 ? 2 : k == 3 ? 1 : 0;
                break;
            case 4: // Thunder Storm - rain, lightning, piranhas
                s.rain = true;
                s.lightning = true;
                s.wind = 110 + k * 8;
                s.gustiness = 0.9f;
                s.whirlpools = 1 + k / 4;
                s.fish = 2 + k / 3;
                s.fishSpeed += 30;
                s.currents = 2 + k / 3;
                s.urchins = 3 + k / 3;
                s.timed = k == 2 || k == 5 || k == 8;
                s.shrink = k == 4 || k == 7 || k == 9;
                s.rescue = k == 9 ? 2 : (k == 1 || k == 6) ? 1 : 0;
                break;
            default: // The Abyss - horror: eels hunt you in the dark
                s.darkness = 2.2f - k * 0.07f;
                s.eels = 1 + k / 4;
                s.whirlpools = 1 + k / 4;
                s.currents = 2 + k / 3;
                s.urchins = 3 + k / 3;
                s.fish = 1 + k / 4;
                s.shrink = k % 2 == 0;
                s.timed = k == 3 || k == 7 || k == 9;
                s.rescue = k == 9 ? 3 : (k == 2 || k == 5) ? 1 : 0;
                s.wind = k >= 5 ? 60 : 0;
                s.gustiness = 0.6f;
                break;
        }

        switch (n) {
            case 1: s.tip = "tilt"; break;
            case 4: s.tip = "urchin"; break;
            case 6: s.tip = "rescue"; break;
            case 8: s.tip = "shrink"; break;
            case 9: s.tip = "fish"; break;
            case 11: s.tip = "current"; break;
            case 15: s.tip = "timed"; break;
            case 21: s.tip = "wind"; break;
            case 31: s.tip = "dark"; break;
            case 36: s.tip = "whirl"; break;
            case 41: s.tip = "storm"; break;
            case 51: s.tip = "eel"; break;
        }
        return s;
    }

    private static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
}
