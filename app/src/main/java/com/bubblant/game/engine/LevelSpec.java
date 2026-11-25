package com.bubblant.game.engine;

/**
 * Difficulty recipe for each of the 120 levels (12 real-life worlds x 10 levels).
 * Levels are generated deterministically from this recipe so every player gets the same maze.
 */
public final class LevelSpec {
    public static final int PER_WORLD = 10;
    public static final int WORLDS = 12;
    public static final int COUNT = PER_WORLD * WORLDS;

    public int number, world, cols, rows;
    public long seed;
    public float braid;
    public int urchins, fish, currents, whirlpools, rescue, eels;
    public float wind, gustiness, currentStrength, fishSpeed, popSpeed, chaserSpeed;
    public boolean rain, lightning, shrink, timed;
    /** light radius in cells, 0 = no darkness */
    public float darkness;
    /** key of a new-mechanic tip shown at level start, or null */
    public String tip;
    /** first level of a world: show the world's story */
    public boolean story;

    public static LevelSpec of(int n) {
        LevelSpec s = build(n);
        s.story = (n - 1) % PER_WORLD == 0;
        // tip = first mechanic that has never appeared in an earlier level
        boolean[] seen = new boolean[TIP_KEYS.length];
        for (int i = 1; i < n; i++) {
            boolean[] m = mechanics(build(i));
            for (int j = 0; j < m.length; j++) seen[j] |= m[j];
        }
        boolean[] mine = mechanics(s);
        for (int j = 0; j < mine.length; j++) {
            if (mine[j] && !seen[j]) { s.tip = TIP_KEYS[j]; break; }
        }
        return s;
    }

    private static final String[] TIP_KEYS = {"tilt", "urchin", "rescue", "shrink", "fish", "current", "timed", "wind", "dark", "whirl", "storm", "eel"};

    private static boolean[] mechanics(LevelSpec s) {
        return new boolean[]{true, s.urchins > 0, s.rescue > 0, s.shrink, s.fish > 0, s.currents > 0, s.timed,
                s.wind > 0, s.darkness > 0, s.whirlpools > 0, s.rain, s.eels > 0};
    }

    private static LevelSpec build(int n) {
        LevelSpec s = new LevelSpec();
        s.number = n;
        int w = (n - 1) / PER_WORLD;
        int k = (n - 1) % PER_WORLD;
        float d = (n - 1) / (float) (COUNT - 1); // 0..1 overall difficulty
        s.world = w;
        s.seed = n * 7919L + 12345L;
        s.cols = 3 + Math.min(4, (n + 6) / 24);
        s.rows = 4 + (n * 11) / COUNT + (k % 3);
        if (n <= 2) { s.cols = 3; s.rows = 4 + n; }
        s.braid = clamp(0.62f - d * 0.56f, 0.06f, 1f);
        if (n <= 3) s.braid = 0.9f;
        s.popSpeed = 410f - d * 150f;
        s.fishSpeed = 70f + d * 110f;
        s.currentStrength = 150f + d * 170f;
        s.chaserSpeed = 82f + d * 45f;

        switch (w) {
            case 0: // Garden puddle - learn to steer
                s.urchins = k >= 3 ? 1 + k / 3 : 0;
                s.fish = k >= 8 ? 1 : 0;
                s.rescue = (k == 5 || k == 9) ? 1 : 0;
                s.shrink = k == 7;
                break;
            case 1: // Kitchen sink - faucet stream and the drain
                s.currents = k >= 1 ? 1 + k / 3 : 0;
                s.urchins = 1 + k / 3;
                s.fish = k >= 4 ? 1 : 0;
                s.whirlpools = k >= 6 ? 1 : 0;
                s.rescue = (k == 3 || k == 8) ? 1 : 0;
                s.timed = k == 5 || k == 9;
                break;
            case 2: // Bathtub - rubber ducks
                s.fish = 1 + k / 3;
                s.whirlpools = k >= 4 ? 1 : 0;
                s.currents = k / 3;
                s.urchins = 1 + k / 4;
                s.shrink = k == 4 || k == 8;
                s.rescue = k == 9 ? 2 : (k == 2 || k == 6) ? 1 : 0;
                break;
            case 3: // Aquarium - goldfish, pump bubbles, filter current
                s.fish = 2 + k / 3;
                s.currents = 1 + k / 3;
                s.urchins = 1 + k / 3;
                s.shrink = k % 2 == 1;
                s.eels = k >= 7 ? 1 : 0; // a curious big fish
                s.rescue = (k == 3 || k == 7) ? 1 : 0;
                break;
            case 4: // Garden pond - frogs and wind
                s.wind = 50 + k * 6;
                s.gustiness = 0.5f + k * 0.04f;
                s.fish = 1 + k / 4;
                s.eels = k >= 3 ? 1 + k / 6 : 0;
                s.urchins = 2 + k / 3;
                s.rescue = k == 9 ? 2 : (k == 2 || k == 6) ? 1 : 0;
                s.timed = k == 5;
                break;
            case 5: // Swimming pool - jets, pool robot
                s.currents = 2 + k / 3;
                s.currentStrength *= 1.15f;
                s.eels = k >= 2 ? 1 : 0;
                s.fish = 1 + k / 4;
                s.wind = 60 + k * 5;
                s.gustiness = 0.5f;
                s.whirlpools = k >= 6 ? 1 : 0;
                s.timed = k == 3 || k == 7;
                s.rescue = (k == 5 || k == 9) ? 1 : 0;
                break;
            case 6: // Park fountain - jets and gusts
                s.currents = 3 + k / 3;
                s.whirlpools = 1 + k / 5;
                s.wind = 90 + k * 6;
                s.gustiness = 0.8f;
                s.fish = 1 + k / 4;
                s.urchins = 2 + k / 3;
                s.shrink = k == 4 || k == 8;
                s.rescue = (k == 2 || k == 7) ? 1 : 0;
                break;
            case 7: // Rice paddy stream - wind, frogs, light rain
                s.wind = 110 + k * 6;
                s.gustiness = 0.8f;
                s.currents = 3 + k / 3;
                s.fish = 2 + k / 3;
                s.eels = 1 + k / 5;
                s.rain = k >= 5;
                s.urchins = 2 + k / 3;
                s.timed = k == 3 || k == 8;
                s.rescue = k == 9 ? 2 : k == 5 ? 1 : 0;
                break;
            case 8: // Tide pool - crabs and waves
                s.eels = 1 + k / 4;
                s.urchins = 4 + k / 3;
                s.wind = 100 + k * 8;
                s.gustiness = 1.0f;
                s.fish = 2 + k / 3;
                s.whirlpools = k >= 5 ? 1 : 0;
                s.shrink = k % 2 == 1;
                s.timed = k == 6;
                s.rescue = (k == 4 || k == 9) ? 1 : 0;
                break;
            case 9: // Thunderstorm lake
                s.rain = true;
                s.lightning = true;
                s.wind = 130 + k * 8;
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
            case 10: // Storm drain - dark, rats, strong currents
                s.darkness = 2.6f - k * 0.08f;
                s.eels = 1 + k / 4;
                s.currents = 3 + k / 3;
                s.currentStrength *= 1.1f;
                s.whirlpools = 1 + k / 4;
                s.urchins = 3 + k / 2;
                s.fish = 1 + k / 4;
                s.shrink = k % 2 == 1;
                s.rescue = k == 8 ? 2 : k == 3 ? 1 : 0;
                break;
            default: // Flooded basement at night - horror
                s.darkness = 2.1f - k * 0.07f;
                s.eels = 1 + k / 3;
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
        return s;
    }

    private static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
}
