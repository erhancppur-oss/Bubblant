package com.bubblant.game;

import android.content.Context;
import android.content.SharedPreferences;

/** Small wrapper around SharedPreferences for settings and progress. */
public final class Prefs {
    private static SharedPreferences sp;

    public static void init(Context c) {
        if (sp == null) sp = c.getApplicationContext().getSharedPreferences("bubblant", Context.MODE_PRIVATE);
    }

    // ---- settings ----
    public static boolean music() { return sp.getBoolean("music", true); }
    public static void setMusic(boolean v) { sp.edit().putBoolean("music", v).apply(); }

    public static boolean sfx() { return sp.getBoolean("sfx", true); }
    public static void setSfx(boolean v) { sp.edit().putBoolean("sfx", v).apply(); }

    /** Spoken voice (text-to-speech). */
    public static boolean voice() { return sp.getBoolean("voice", true); }
    public static void setVoice(boolean v) { sp.edit().putBoolean("voice", v).apply(); }

    /** Speech bubbles on screen. */
    public static boolean talk() { return sp.getBoolean("talk", true); }
    public static void setTalk(boolean v) { sp.edit().putBoolean("talk", v).apply(); }

    public static boolean vibration() { return sp.getBoolean("vibe", true); }
    public static void setVibration(boolean v) { sp.edit().putBoolean("vibe", v).apply(); }

    /** 0 = tilt, 1 = touch. */
    public static int control() { return sp.getInt("control", 0); }
    public static void setControl(int v) { sp.edit().putInt("control", v).apply(); }

    /** Tilt sensitivity 50..160 (%). */
    public static int sensitivity() { return sp.getInt("sens", 100); }
    public static void setSensitivity(int v) { sp.edit().putInt("sens", v).apply(); }

    public static int hat() { return sp.getInt("hat", 0); }
    public static void setHat(int v) { sp.edit().putInt("hat", v).apply(); }

    // ---- progress ----
    public static int unlocked() { return sp.getInt("unlocked", 1); }

    public static int stars(int level) { return sp.getInt("stars_" + level, 0); }

    public static void completeLevel(int level, int stars) {
        SharedPreferences.Editor e = sp.edit();
        if (stars > stars(level)) e.putInt("stars_" + level, stars);
        if (level + 1 > unlocked()) e.putInt("unlocked", level + 1);
        e.apply();
    }

    public static void unlockNext(int level) {
        if (level + 1 > unlocked()) sp.edit().putInt("unlocked", level + 1).apply();
    }

    public static int totalStars() {
        int t = 0;
        for (int i = 1; i <= com.bubblant.game.engine.LevelSpec.COUNT; i++) t += stars(i);
        return t;
    }

    public static int fails(int level) { return sp.getInt("fails_" + level, 0); }
    public static void setFails(int level, int v) { sp.edit().putInt("fails_" + level, v).apply(); }

    public static boolean tipSeen(String key) { return sp.getBoolean("tip_" + key, false); }
    public static void setTipSeen(String key) { sp.edit().putBoolean("tip_" + key, true).apply(); }

    private Prefs() {}
}
