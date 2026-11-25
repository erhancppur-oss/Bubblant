package com.bubblant.game.audio;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Gives the ant a voice using the phone's text-to-speech engine.
 * The language, regional accent, voice and pitch can all be chosen in Settings.
 */
public final class AntVoice {
    public interface ReadyListener { void onReady(AntVoice v); }

    /** Regional accents offered for each language. */
    public static String[] accentsFor(String lang) {
        switch (lang) {
            case "en": return new String[]{"en-US", "en-GB", "en-AU", "en-IN", "en-IE"};
            case "es": return new String[]{"es-ES", "es-MX", "es-US", "es-AR"};
            case "de": return new String[]{"de-DE", "de-AT", "de-CH"};
            case "fr": return new String[]{"fr-FR", "fr-CA", "fr-BE", "fr-CH"};
            case "pt": return new String[]{"pt-BR", "pt-PT"};
            case "tr": return new String[]{"tr-TR"};
            default: return new String[]{lang};
        }
    }

    private static final float[] PITCH = {1.75f, 1.15f, 0.75f};
    private static final float[] RATE = {1.12f, 1.0f, 0.92f};

    private TextToSpeech tts;
    private volatile boolean ready;
    private Locale locale;
    private final int pitchPreset;
    private String voiceName;
    private boolean langMissing;

    public AntVoice(Context ctx, Locale locale, String voiceName, int pitchPreset) {
        this(ctx, locale, voiceName, pitchPreset, null);
    }

    public AntVoice(Context ctx, Locale locale, String voiceName, int pitchPreset, ReadyListener listener) {
        this.locale = locale;
        this.voiceName = voiceName;
        this.pitchPreset = Math.max(0, Math.min(2, pitchPreset));
        try {
            tts = new TextToSpeech(ctx.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    applyLocale();
                    ready = true;
                    if (listener != null) listener.onReady(this);
                }
            });
        } catch (Exception e) {
            tts = null;
        }
    }

    private void applyLocale() {
        int r = tts.setLanguage(locale);
        langMissing = r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED;
        if (langMissing) {
            // try the base language (e.g. "es" when "es-AR" is not installed)
            int r2 = tts.setLanguage(new Locale(locale.getLanguage()));
            langMissing = r2 == TextToSpeech.LANG_MISSING_DATA || r2 == TextToSpeech.LANG_NOT_SUPPORTED;
        }
        if (voiceName != null && !voiceName.isEmpty()) {
            try {
                for (Voice v : tts.getVoices()) {
                    if (v.getName().equals(voiceName)) { tts.setVoice(v); break; }
                }
            } catch (Exception ignored) {}
        }
    }

    /** true when the phone has no voice data for this language */
    public boolean isLanguageMissing() { return langMissing; }

    public boolean isReady() { return ready; }

    /** Voices installed for the current accent, best quality first. */
    public List<Voice> voices() {
        ArrayList<Voice> out = new ArrayList<>();
        if (!ready || tts == null) return out;
        try {
            for (Voice v : tts.getVoices()) {
                Locale l = v.getLocale();
                if (l == null || !l.getLanguage().equals(locale.getLanguage())) continue;
                boolean countryOk = locale.getCountry().isEmpty() || locale.getCountry().equalsIgnoreCase(l.getCountry());
                if (!countryOk) continue;
                if (v.getFeatures() != null && v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
                out.add(v);
            }
        } catch (Exception ignored) {}
        Collections.sort(out, (a, b) -> b.getQuality() - a.getQuality());
        return out;
    }

    public void speak(String text, boolean angry) {
        if (!ready || tts == null) return;
        // strip emoji / symbols so the engine doesn't read them out
        String clean = text.replaceAll("[^\\p{L}\\p{N}\\s,.!?'’¡¿-]", "").trim();
        if (clean.isEmpty()) return;
        float pitch = PITCH[pitchPreset] * (angry ? 0.8f : 1f);
        float rate = RATE[pitchPreset] * (angry ? 1.15f : 1f);
        tts.setPitch(pitch);
        tts.setSpeechRate(rate);
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "ant");
    }

    public void stop() {
        if (tts != null) tts.stop();
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
