package com.bubblant.game;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.bubblant.game.audio.AudioEngine;

public class BubblantApp extends Application {
    private int started;

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.init(this);
        AudioEngine.get().setMusicOn(Prefs.music());
        AudioEngine.get().setSfxOn(Prefs.sfx());
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity a) {
                started++;
                AudioEngine.get().setPaused(false);
            }
            @Override public void onActivityStopped(Activity a) {
                started--;
                if (started <= 0) {
                    started = 0;
                    AudioEngine.get().setPaused(true);
                }
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityResumed(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
        AudioEngine.get().start();
    }
}
