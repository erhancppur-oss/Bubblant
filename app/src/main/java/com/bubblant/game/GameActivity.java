package com.bubblant.game;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.bubblant.game.audio.AntVoice;
import com.bubblant.game.audio.AudioEngine;
import com.bubblant.game.audio.MusicStyle;
import com.bubblant.game.audio.Sfx;
import com.bubblant.game.engine.GameWorld;
import com.bubblant.game.engine.Level;
import com.bubblant.game.engine.LevelSpec;
import com.bubblant.game.engine.Phrases;
import com.bubblant.game.view.GameView;

public class GameActivity extends AppCompatActivity implements GameWorld.Listener, GameView.Host {
    public static final String EXTRA_LEVEL = "level";

    private GameView view;
    private FrameLayout overlay;
    private Phrases phrases;
    private volatile AntVoice voice;
    private int levelNumber;
    private boolean finished;
    private String voiceKey = "";

    private String voiceKey() {
        return AntLanguage.lang(this) + "|" + Prefs.antAccent() + "|" + Prefs.antVoice() + "|" + Prefs.antPitch();
    }

    private void hideSystemBars() {
        androidx.core.view.WindowInsetsControllerCompat c =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        levelNumber = getIntent().getIntExtra(EXTRA_LEVEL, 1);
        FrameLayout root = new FrameLayout(this);
        view = new GameView(this);
        view.setHost(this);
        root.addView(view);
        overlay = new FrameLayout(this);
        root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = Build.VERSION.SDK_INT >= 30
                    ? insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()).top
                    : insets.getSystemWindowInsetTop();
            view.renderer.insetTop = top;
            return insets;
        });

        phrases = new Phrases(AntLanguage.resources(this));
        voiceKey = voiceKey();
        voice = new AntVoice(this, AntLanguage.voiceLocale(this), Prefs.antVoice(), Prefs.antPitch());
        hideSystemBars();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (overlay.getChildCount() > 0 && !finished) resumeGame();
                else if (overlay.getChildCount() == 0) onPauseRequested();
                else finish();
            }
        });
        startLevel(levelNumber);
    }

    private void startLevel(int n) {
        levelNumber = n;
        finished = false;
        overlay.removeAllViews();
        LevelSpec spec = LevelSpec.of(n);
        Level level = new Level(spec);
        GameWorld world = new GameWorld(level, phrases, this);
        view.renderer.setWorld(spec.world);
        view.renderer.labels.level = getString(R.string.level_n, n);
        view.renderer.labels.world = Ui.worldName(this, spec.world);
        view.renderer.labels.ready = getString(R.string.ready_tilt);
        view.renderer.labels.friends = getString(R.string.help);
        view.renderer.labels.air = getString(R.string.air);
        String tip = "";
        if (spec.tip != null) {
            int id = getResources().getIdentifier("tip_" + spec.tip, "string", getPackageName());
            if (id != 0) tip = getString(id);
        }
        if (Prefs.control() == 1 && "tilt".equals(spec.tip)) tip = getString(R.string.tip_touch);
        view.renderer.labels.tip = tip;
        view.renderer.labels.story = spec.story ? getResources().getStringArray(R.array.world_stories)[spec.world] : "";
        world.readyTime = (spec.story || !tip.isEmpty()) ? 3.8f : 1.6f;
        view.renderer.showTalk = Prefs.talk();
        view.renderer.hat = Prefs.hat();
        view.setWorld(world);
        view.setPaused(false);
        AudioEngine.get().playMusic(MusicStyle.forWorld(spec.world));
        AudioEngine.get().setTension(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.resume();
        view.renderer.showTalk = Prefs.talk();
        hideSystemBars();
        if (!voiceKey().equals(voiceKey)) {
            // ant voice settings changed while paused
            voice.shutdown();
            voiceKey = voiceKey();
            phrases = new Phrases(AntLanguage.resources(this));
            voice = new AntVoice(this, AntLanguage.voiceLocale(this), Prefs.antVoice(), Prefs.antPitch());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.pause();
        if (!finished && overlay.getChildCount() == 0) onPauseRequested();
        voice.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        voice.shutdown();
        AudioEngine.get().setTension(0);
    }

    // ---------- GameWorld.Listener (called on the game thread) ----------
    @Override public void onSfx(int id, float vol) { AudioEngine.get().play(id, vol); }

    @Override public void onSay(String text, boolean angry) {
        if (Prefs.voice()) voice.speak(text, angry);
    }

    @Override public void onVibrate(int ms) {
        if (!Prefs.vibration()) return;
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                v = vm.getDefaultVibrator();
            } else v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(ms);
        } catch (Exception ignored) {}
    }

    @Override public void onFinished(boolean won, int stars, int sugar, float time, int cause) {
        runOnUiThread(() -> {
            if (isFinishing()) return;
            finished = true;
            if (won) showWin(stars, sugar, time);
            else showFail(cause);
        });
    }

    // ---------- panels ----------
    @Override public void onPauseRequested() {
        runOnUiThread(() -> {
            if (finished || overlay.getChildCount() > 0) return;
            view.setPaused(true);
            voice.stop();
            LinearLayout p = Ui.panel(this);
            p.addView(Ui.title(this, getString(R.string.paused), 34));
            p.addView(Ui.button(this, getString(R.string.resume), v -> resumeGame()));
            p.addView(Ui.button(this, getString(R.string.restart), v -> startLevel(levelNumber)));
            p.addView(Ui.button(this, musicLabel(), v -> {
                boolean on = !Prefs.music();
                Prefs.setMusic(on);
                AudioEngine.get().setMusicOn(on);
                ((TextView) v).setText(musicLabel());
            }));
            p.addView(Ui.button(this, voiceLabel(), v -> {
                boolean on = !(Prefs.voice() || Prefs.talk());
                Prefs.setVoice(on);
                Prefs.setTalk(on);
                view.renderer.showTalk = on;
                ((TextView) v).setText(voiceLabel());
            }));
            p.addView(Ui.button(this, getString(R.string.settings), v -> startActivity(new Intent(this, SettingsActivity.class))));
            p.addView(Ui.button(this, getString(R.string.levels), v -> finish()));
            showPanel(p);
        });
    }

    private String musicLabel() { return getString(Prefs.music() ? R.string.music_on : R.string.music_off); }
    private String voiceLabel() { return getString((Prefs.voice() || Prefs.talk()) ? R.string.talk_on : R.string.talk_off); }

    private void resumeGame() {
        overlay.removeAllViews();
        view.setPaused(false);
    }

    private void showWin(int stars, int sugar, float time) {
        int before = Prefs.totalStars();
        Prefs.completeLevel(levelNumber, stars);
        Prefs.setFails(levelNumber, 0);
        LinearLayout p = Ui.panel(this);
        boolean last = levelNumber >= LevelSpec.COUNT;
        p.addView(Ui.title(this, getString(last ? R.string.game_complete : R.string.level_complete), 30));
        TextView st = Ui.title(this, "★★★".substring(0, stars) + "☆☆☆".substring(0, 3 - stars), 54);
        st.setTextColor(0xFFFFD54F);
        p.addView(st);
        p.addView(Ui.title(this, getString(R.string.result_line, sugar, (int) time), 18));
        int after = Prefs.totalStars();
        for (int i = 1; i < com.bubblant.game.view.AntPainter.HAT_COUNT; i++) {
            int need = com.bubblant.game.view.AntPainter.HAT_STARS[i];
            if (before < need && after >= need) {
                TextView h = Ui.title(this, getString(R.string.new_hat), 18);
                h.setTextColor(0xFFFFE082);
                p.addView(h);
                break;
            }
        }
        if (!last) p.addView(Ui.button(this, getString(R.string.next), v ->
                AdManager.onLevelEnd(this, levelNumber, () -> startLevel(levelNumber + 1))));
        p.addView(Ui.button(this, getString(R.string.replay), v ->
                AdManager.onLevelEnd(this, levelNumber, () -> startLevel(levelNumber))));
        p.addView(Ui.button(this, getString(R.string.levels), v -> finish()));
        showPanel(p);
        st.setScaleX(0.2f);
        st.setScaleY(0.2f);
        st.animate().scaleX(1f).scaleY(1f).setDuration(450).setStartDelay(150).start();
        st.postDelayed(() -> AudioEngine.get().play(Sfx.STAR), 300);
    }

    private void showFail(int cause) {
        int fails = Prefs.fails(levelNumber) + 1;
        Prefs.setFails(levelNumber, fails);
        String[] causes = getResources().getStringArray(R.array.fail_causes);
        LinearLayout p = Ui.panel(this);
        p.addView(Ui.title(this, getString(R.string.popped), 32));
        if (cause >= 0 && cause < causes.length) p.addView(Ui.title(this, causes[cause], 18));
        GameWorld w = view.getWorld();
        p.addView(Ui.button(this, getString(R.string.retry), v ->
                AdManager.onLevelEnd(this, levelNumber, () -> startLevel(levelNumber))));
        if (w != null && !w.revived && AdManager.rewardedReady()) {
            p.addView(Ui.button(this, getString(R.string.revive_ad), v ->
                    AdManager.showRewarded(this, () -> {
                        overlay.removeAllViews();
                        finished = false;
                        view.requestRevive();
                        view.setPaused(false);
                    }, () -> {})));
        }
        if (fails >= 3 && levelNumber < LevelSpec.COUNT && AdManager.rewardedReady()) {
            p.addView(Ui.button(this, getString(R.string.skip_ad), v ->
                    AdManager.showRewarded(this, () -> {
                        Prefs.unlockNext(levelNumber);
                        startLevel(levelNumber + 1);
                    }, () -> {})));
        }
        p.addView(Ui.button(this, getString(R.string.levels), v -> finish()));
        showPanel(p);
    }

    private void showPanel(View p) {
        overlay.removeAllViews();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        int m = Ui.dp(this, 28);
        lp.setMargins(m, m, m, m);
        overlay.addView(p, lp);
        p.setAlpha(0f);
        p.setTranslationY(Ui.dp(this, 60));
        p.animate().alpha(1f).translationY(0).setDuration(250).start();
    }
}
