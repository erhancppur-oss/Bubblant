package com.bubblant.game;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.os.LocaleListCompat;

import com.bubblant.game.audio.AudioEngine;
import com.bubblant.game.view.AntPainter;
import com.bubblant.game.view.MenuBubblesView;

public class SettingsActivity extends AppCompatActivity {
    private static final String[] LANG_TAGS = {"", "en", "tr", "es", "de", "fr", "pt"};
    private static final String[] LANG_NAMES = {"System", "English", "Türkçe", "Español", "Deutsch", "Français", "Português"};
    public static final String PRIVACY_URL = "https://erhancppur-oss.github.io/Bubblant/privacy.html";

    private LinearLayout hats;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        MenuBubblesView bg = new MenuBubblesView(this);
        bg.setShowAnt(false);
        root.addView(bg);
        ScrollView scroll = new ScrollView(this);
        LinearLayout col = Ui.panel(this);
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = Ui.dp(this, 16);
        plp.setMargins(m, Ui.dp(this, 30), m, Ui.dp(this, 30));
        scroll.addView(col, plp);
        root.addView(scroll);
        setContentView(root);

        col.addView(Ui.title(this, getString(R.string.settings), 32));

        col.addView(toggle(getString(R.string.set_music), Prefs.music(), v -> { Prefs.setMusic(v); AudioEngine.get().setMusicOn(v); }));
        col.addView(toggle(getString(R.string.set_sfx), Prefs.sfx(), v -> { Prefs.setSfx(v); AudioEngine.get().setSfxOn(v); }));
        col.addView(toggle(getString(R.string.set_voice), Prefs.voice(), Prefs::setVoice));
        col.addView(toggle(getString(R.string.set_talk), Prefs.talk(), Prefs::setTalk));
        col.addView(toggle(getString(R.string.set_vibration), Prefs.vibration(), Prefs::setVibration));
        col.addView(toggle(getString(R.string.set_touch), Prefs.control() == 1, v -> Prefs.setControl(v ? 1 : 0)));

        // sensitivity
        TextView sensLabel = label(getString(R.string.set_sensitivity) + ": " + Prefs.sensitivity() + "%");
        col.addView(sensLabel);
        SeekBar sb = new SeekBar(this);
        sb.setMax(110);
        sb.setProgress(Prefs.sensitivity() - 50);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                Prefs.setSensitivity(p + 50);
                sensLabel.setText(getString(R.string.set_sensitivity) + ": " + (p + 50) + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        col.addView(sb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // language
        col.addView(label(getString(R.string.set_language)));
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, LANG_NAMES);
        sp.setAdapter(ad);
        String cur = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        int sel = 0;
        for (int i = 1; i < LANG_TAGS.length; i++) if (cur.startsWith(LANG_TAGS[i])) sel = i;
        sp.setSelection(sel, false);
        sp.setBackgroundColor(0xFFFFFFFF);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                AppCompatDelegate.setApplicationLocales(pos == 0 ? LocaleListCompat.getEmptyLocaleList()
                        : LocaleListCompat.forLanguageTags(LANG_TAGS[pos]));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        col.addView(sp, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));

        // wardrobe
        col.addView(label(getString(R.string.set_hats)));
        hats = new LinearLayout(this);
        hats.setOrientation(LinearLayout.HORIZONTAL);
        hats.setGravity(Gravity.CENTER);
        col.addView(hats);
        buildHats();

        if (AdManager.privacyOptionsRequired()) {
            col.addView(Ui.button(this, getString(R.string.set_privacy_options), v -> AdManager.showPrivacyOptions(this)));
        }
        col.addView(Ui.button(this, getString(R.string.set_privacy_policy), v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))); } catch (Exception ignored) {}
        }));
        col.addView(Ui.button(this, getString(R.string.back), v -> finish()));
    }

    private void buildHats() {
        hats.removeAllViews();
        String[] names = getResources().getStringArray(R.array.hat_names);
        int stars = Prefs.totalStars();
        for (int i = 0; i < AntPainter.HAT_COUNT; i++) {
            final int idx = i;
            boolean unlocked = stars >= AntPainter.HAT_STARS[i];
            TextView t = new TextView(this);
            t.setGravity(Gravity.CENTER);
            t.setTextColor(Color.WHITE);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            t.setText(unlocked ? names[i] : "🔒\n⭐" + AntPainter.HAT_STARS[i]);
            t.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8));
            t.setBackgroundColor(Prefs.hat() == i ? 0xFFFFA000 : 0x33FFFFFF);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
            if (unlocked) t.setOnClickListener(v -> { Prefs.setHat(idx); buildHats(); });
            hats.addView(t, lp);
        }
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        return t;
    }

    interface BoolSetter { void set(boolean v); }

    private View toggle(String text, boolean value, BoolSetter setter) {
        SwitchCompat sw = new SwitchCompat(this);
        sw.setText(text);
        sw.setTextColor(Color.WHITE);
        sw.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        sw.setChecked(value);
        sw.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        sw.setOnCheckedChangeListener((b, v) -> setter.set(v));
        sw.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return sw;
    }
}
