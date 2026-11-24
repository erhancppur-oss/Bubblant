package com.bubblant.game.audio;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** Gives the ant a squeaky little voice using the phone's text-to-speech engine. */
public final class AntVoice {
    private TextToSpeech tts;
    private boolean ready;
    private final Locale locale;

    public AntVoice(Context ctx, Locale locale) {
        this.locale = locale;
        try {
            tts = new TextToSpeech(ctx.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    int r = tts.setLanguage(locale);
                    if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                        r = tts.setLanguage(Locale.ENGLISH);
                    }
                    ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
                    tts.setPitch(1.8f);
                    tts.setSpeechRate(1.15f);
                }
            });
        } catch (Exception e) {
            tts = null;
        }
    }

    public void speak(String text, boolean angry) {
        if (!ready || tts == null) return;
        // strip emoji / symbols so the engine doesn't read them out
        String clean = text.replaceAll("[^\\p{L}\\p{N}\\s,.!?'’-]", "").trim();
        if (clean.isEmpty()) return;
        tts.setPitch(angry ? 1.35f : 1.8f);
        tts.setSpeechRate(angry ? 1.3f : 1.15f);
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
