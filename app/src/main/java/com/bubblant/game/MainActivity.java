package com.bubblant.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bubblant.game.audio.AudioEngine;
import com.bubblant.game.audio.MusicStyle;
import com.bubblant.game.engine.LevelSpec;
import com.bubblant.game.view.MenuBubblesView;

public class MainActivity extends AppCompatActivity {
    private TextView starsView;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Prefs.init(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(new MenuBubblesView(this));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(0, Ui.dp(this, 40), 0, 0);

        android.view.View spacer = new android.view.View(this);
        col.addView(spacer, new LinearLayout.LayoutParams(1, (int) (getResources().getDisplayMetrics().heightPixels * 0.36f)));

        TextView title = Ui.title(this, getString(R.string.app_name), 56);
        col.addView(title);
        TextView sub = Ui.title(this, getString(R.string.tagline), 18);
        col.addView(sub);
        starsView = Ui.title(this, "", 18);
        starsView.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 10));
        col.addView(starsView);

        col.addView(Ui.button(this, getString(R.string.play), v -> {
            int lvl = Math.min(Prefs.unlocked(), LevelSpec.COUNT);
            startActivity(new Intent(this, GameActivity.class).putExtra(GameActivity.EXTRA_LEVEL, lvl));
        }));
        col.addView(Ui.button(this, getString(R.string.levels), v -> startActivity(new Intent(this, LevelSelectActivity.class))));
        col.addView(Ui.button(this, getString(R.string.settings), v -> startActivity(new Intent(this, SettingsActivity.class))));

        root.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout banner = new FrameLayout(this);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        root.addView(banner, blp);
        setContentView(root);

        AdManager.gatherConsent(this);
        AdManager.loadBanner(this, banner);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AudioEngine.get().playMusic(MusicStyle.MENU);
        starsView.setText("⭐ " + Prefs.totalStars() + " / " + (LevelSpec.COUNT * 3));
    }
}
