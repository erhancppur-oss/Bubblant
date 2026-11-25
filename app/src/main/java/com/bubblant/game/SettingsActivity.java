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

import com.bubblant.game.audio.AntVoice;
import com.bubblant.game.audio.AudioEngine;

import java.util.Locale;
import com.bubblant.game.view.AntPainter;
import com.bubblant.game.view.MenuBubblesView;

public class SettingsActivity extends AppCompatActivity {
    private static final String[] LANG_TAGS = {"", "en", "tr", "es", "de", "fr", "pt"};
    private static final String[] LANG_NAMES = {"System", "English", "Türkçe", "Español", "Deutsch", "Français", "Português"};
    public static final String PRIVACY_URL = "https://erhancppur-oss.github.io/Bubblant/privacy.html";

    private LinearLayout hats;
    private LinearLayout voiceBox;
    private AntVoice testVoice;

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
        Ui.padForSystemBars(scroll);

        col.addView(Ui.title(this, getString(R.string.settings), 32));

        col.addView(toggle(getString(R.string.set_music), Prefs.music(), v -> { Prefs.setMusic(v); AudioEngine.get().setMusicOn(v); }));
        col.addView(toggle(getString(R.string.set_sfx), Prefs.sfx(), v -> { Prefs.setSfx(v); AudioEngine.get().setSfxOn(v); }));
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
        ArrayAdapter<String> ad = Ui.spinnerAdapter(this, LANG_NAMES);
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

        // the ant's voice
        voiceBox = new LinearLayout(this);
        voiceBox.setOrientation(LinearLayout.VERTICAL);
        col.addView(voiceBox);
        buildVoiceSection();

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (testVoice != null) testVoice.shutdown();
    }

    /** Ant voice: on/off, speech bubbles, language, accent, voice, style, test. */
    private void buildVoiceSection() {
        voiceBox.removeAllViews();
        TextView head = label(getString(R.string.set_ant_voice));
        head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        head.setTextColor(0xFFFFE082);
        voiceBox.addView(head);
        voiceBox.addView(toggle(getString(R.string.set_voice), Prefs.voice(), Prefs::setVoice));
        voiceBox.addView(toggle(getString(R.string.set_talk), Prefs.talk(), Prefs::setTalk));

        // language the ant speaks
        voiceBox.addView(label(getString(R.string.set_ant_lang)));
        String[] langNames = new String[AntLanguage.LANGS.length + 1];
        langNames[0] = getString(R.string.same_as_game);
        for (int i = 0; i < AntLanguage.LANGS.length; i++) langNames[i + 1] = LANG_NAMES[i + 1];
        int langSel = 0;
        for (int i = 0; i < AntLanguage.LANGS.length; i++) if (AntLanguage.LANGS[i].equals(Prefs.antLang())) langSel = i + 1;
        voiceBox.addView(spinner(langNames, langSel, pos -> {
            String v = pos == 0 ? "" : AntLanguage.LANGS[pos - 1];
            if (!v.equals(Prefs.antLang())) { Prefs.setAntLang(v); buildVoiceSection(); }
        }));

        // accent
        String lang = AntLanguage.lang(this);
        String[] accents = AntVoice.accentsFor(lang);
        Locale ui = getResources().getConfiguration().getLocales().get(0);
        java.util.Locale voiceLoc = AntLanguage.voiceLocale(this);
        String[] accentNames = new String[accents.length];
        int accSel = 0;
        for (int i = 0; i < accents.length; i++) {
            Locale l = Locale.forLanguageTag(accents[i]);
            accentNames[i] = l.getDisplayCountry(ui).isEmpty() ? l.getDisplayName(ui) : l.getDisplayLanguage(ui) + " (" + l.getDisplayCountry(ui) + ")";
            if (l.getCountry().equalsIgnoreCase(voiceLoc.getCountry())) accSel = i;
        }
        voiceBox.addView(label(getString(R.string.set_accent)));
        voiceBox.addView(spinner(accentNames, accSel, pos -> {
            if (!Locale.forLanguageTag(accents[pos]).equals(AntLanguage.voiceLocale(this))) {
                Prefs.setAntAccent(accents[pos]);
                buildVoiceSection();
            }
        }));

        // voice style (pitch)
        voiceBox.addView(label(getString(R.string.set_voice_style)));
        voiceBox.addView(spinner(getResources().getStringArray(R.array.voice_styles), Prefs.antPitch(), pos -> {
            if (pos != Prefs.antPitch()) { Prefs.setAntPitch(pos); rebuildTestVoice(null); }
        }));

        // voices installed for that accent (filled when TTS is ready)
        TextView voiceLabel = label(getString(R.string.set_voice_pick));
        voiceBox.addView(voiceLabel);
        LinearLayout voiceSlot = new LinearLayout(this);
        voiceSlot.setOrientation(LinearLayout.VERTICAL);
        voiceBox.addView(voiceSlot);

        voiceBox.addView(Ui.button(this, getString(R.string.test_voice), v -> {
            if (testVoice != null) {
                com.bubblant.game.engine.Phrases ph = new com.bubblant.game.engine.Phrases(AntLanguage.resources(this));
                testVoice.speak(ph.pick(ph.start) + " " + ph.pick(ph.left), false);
            }
        }));

        rebuildTestVoice(tv -> runOnUiThread(() -> {
            if (isFinishing()) return;
            voiceSlot.removeAllViews();
            if (tv.isLanguageMissing()) {
                TextView warn = label(getString(R.string.voice_missing));
                warn.setTextColor(0xFFFFAB91);
                warn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                voiceSlot.addView(warn);
                voiceSlot.addView(Ui.button(this, getString(R.string.install_voice), x -> {
                    try { startActivity(new Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)); } catch (Exception ignored) {}
                }));
                return;
            }
            java.util.List<android.speech.tts.Voice> vs = tv.voices();
            String[] names = new String[vs.size() + 1];
            names[0] = getString(R.string.voice_auto);
            int vsel = 0;
            for (int i = 0; i < vs.size(); i++) {
                android.speech.tts.Voice vo = vs.get(i);
                names[i + 1] = getString(R.string.voice_n, i + 1) + (vo.isNetworkConnectionRequired() ? " ☁" : "");
                if (vo.getName().equals(Prefs.antVoice())) vsel = i + 1;
            }
            voiceSlot.addView(spinner(names, vsel, pos -> {
                String name = pos == 0 ? "" : vs.get(pos - 1).getName();
                if (!name.equals(Prefs.antVoice())) { Prefs.setAntVoice(name); rebuildTestVoice(null); }
            }));
        }));
    }

    private void rebuildTestVoice(AntVoice.ReadyListener l) {
        if (testVoice != null) testVoice.shutdown();
        testVoice = new AntVoice(this, AntLanguage.voiceLocale(this), Prefs.antVoice(), Prefs.antPitch(), l);
    }

    interface PosListener { void picked(int pos); }

    private Spinner spinner(String[] items, int sel, PosListener l) {
        Spinner sp = new Spinner(this);
        sp.setAdapter(Ui.spinnerAdapter(this, items));
        sp.setSelection(Math.max(0, Math.min(items.length - 1, sel)), false);
        sp.setBackgroundColor(0xFFFFFFFF);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) { l.picked(pos); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
        sp.setLayoutParams(lp);
        return sp;
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
