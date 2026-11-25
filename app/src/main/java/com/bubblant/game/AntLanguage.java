package com.bubblant.game;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.bubblant.game.audio.AntVoice;

import java.util.Locale;

/** Works out which language and accent the ant speaks with. */
public final class AntLanguage {
    public static final String[] LANGS = {"en", "tr", "es", "de", "fr", "pt"};

    /** Language code the ant talks in. */
    public static String lang(Context c) {
        String l = Prefs.antLang();
        if (!l.isEmpty()) return l;
        String game = c.getResources().getConfiguration().getLocales().get(0).getLanguage();
        for (String s : LANGS) if (s.equals(game)) return s;
        return "en";
    }

    /** Locale (with accent/country) used for text-to-speech. */
    public static Locale voiceLocale(Context c) {
        String accent = Prefs.antAccent();
        String lang = lang(c);
        if (!accent.isEmpty() && accent.startsWith(lang)) return Locale.forLanguageTag(accent);
        if (Prefs.antLang().isEmpty()) {
            Locale game = c.getResources().getConfiguration().getLocales().get(0);
            if (game.getLanguage().equals(lang) && !game.getCountry().isEmpty()) return game;
        }
        return Locale.forLanguageTag(AntVoice.accentsFor(lang)[0]);
    }

    /** Resources in the ant's language, so its phrases match its voice. */
    public static Resources resources(Context c) {
        Configuration cfg = new Configuration(c.getResources().getConfiguration());
        cfg.setLocale(new Locale(lang(c)));
        return c.createConfigurationContext(cfg).getResources();
    }

    private AntLanguage() {}
}
