package com.bubblant.game;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bubblant.game.audio.AudioEngine;
import com.bubblant.game.audio.MusicStyle;
import com.bubblant.game.audio.Sfx;
import com.bubblant.game.engine.LevelSpec;
import com.bubblant.game.view.MenuBubblesView;

public class LevelSelectActivity extends AppCompatActivity {
    private LinearLayout list;
    private FrameLayout banner;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        MenuBubblesView bg = new MenuBubblesView(this);
        bg.setShowAnt(false);
        root.addView(bg);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 12);
        list.setPadding(pad, Ui.dp(this, 30), pad, Ui.dp(this, 90));
        scroll.addView(list);
        root.addView(scroll);
        banner = new FrameLayout(this);
        root.addView(banner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        setContentView(root);
        Ui.padForSystemBars(scroll);
        Ui.padForSystemBars(banner);
        AdManager.loadBanner(this, banner);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AudioEngine.get().playMusic(MusicStyle.MENU);
        build();
    }

    private void build() {
        list.removeAllViews();
        list.addView(Ui.title(this, getString(R.string.levels), 36));
        int unlocked = Prefs.unlocked();
        int screenW = getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 24);
        int cell = screenW / 5;
        for (int w = 0; w < LevelSpec.WORLDS; w++) {
            TextView header = Ui.title(this, (w + 1) + ". " + Ui.worldName(this, w), 22);
            header.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 6));
            list.addView(header);
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(5);
            for (int k = 0; k < LevelSpec.PER_WORLD; k++) {
                int n = w * LevelSpec.PER_WORLD + k + 1;
                boolean open = n <= unlocked;
                TextView t = new TextView(this);
                int st = Prefs.stars(n);
                String stars = st == 0 ? "" : "\n" + "★★★".substring(0, st) + "☆☆☆".substring(0, 3 - st);
                t.setText(open ? n + stars : "🔒");
                t.setGravity(Gravity.CENTER);
                t.setTextColor(Color.WHITE);
                t.setTypeface(Typeface.DEFAULT_BOLD);
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, open ? 18 : 20);
                GradientDrawable bgd = new GradientDrawable();
                bgd.setShape(GradientDrawable.OVAL);
                bgd.setColor(open ? com.bubblant.game.engine.WorldTheme.of(w).waterBottom : 0x66000000);
                bgd.setStroke(Ui.dp(this, 3), n == unlocked ? 0xFFFFC107 : 0xAAFFFFFF);
                t.setBackground(bgd);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = cell - Ui.dp(this, 8);
                lp.height = cell - Ui.dp(this, 8);
                lp.setMargins(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
                t.setLayoutParams(lp);
                if (open) {
                    t.setOnClickListener(v -> {
                        AudioEngine.get().play(Sfx.CLICK);
                        startActivity(new Intent(this, GameActivity.class).putExtra(GameActivity.EXTRA_LEVEL, n));
                    });
                }
                grid.addView(t);
            }
            list.addView(grid);
        }
    }
}
