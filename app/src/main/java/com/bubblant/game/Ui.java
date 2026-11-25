package com.bubblant.game;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bubblant.game.audio.AudioEngine;
import com.bubblant.game.audio.Sfx;

/** Helpers to build the cartoon UI in code. */
public final class Ui {
    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static Button button(Context c, String text, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(0xFF4E342E);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackgroundResource(R.drawable.btn_bubble);
        b.setPadding(dp(c, 24), dp(c, 10), dp(c, 24), dp(c, 10));
        b.setOnClickListener(v -> {
            AudioEngine.get().play(Sfx.CLICK);
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(60)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
            l.onClick(v);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 260), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 8);
        lp.bottomMargin = dp(c, 8);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        b.setLayoutParams(lp);
        return b;
    }

    public static TextView title(Context c, String text, float sp) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setShadowLayer(8, 0, 4, 0xAA000000);
        return t;
    }

    public static LinearLayout panel(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setBackgroundResource(R.drawable.panel);
        int p = dp(c, 24);
        l.setPadding(p, p, p, p);
        return l;
    }

    /** Keep content clear of the status bar, camera cut-out and navigation bar (edge-to-edge on Android 15+). */
    public static void padForSystemBars(View v) {
        final int l = v.getPaddingLeft(), t = v.getPaddingTop(), r = v.getPaddingRight(), b = v.getPaddingBottom();
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            androidx.core.graphics.Insets i = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            view.setPadding(l + i.left, t + i.top, r + i.right, b + i.bottom);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(v);
    }

    /** Spinner adapter with dark text on the white field (the dark theme would make it white-on-white). */
    public static android.widget.ArrayAdapter<String> spinnerAdapter(Context c, String[] items) {
        return new android.widget.ArrayAdapter<String>(c, android.R.layout.simple_spinner_dropdown_item, items) {
            @Override public View getView(int pos, View convert, android.view.ViewGroup parent) {
                View v = super.getView(pos, convert, parent);
                ((TextView) v).setTextColor(0xFF0D3B66);
                return v;
            }
            @Override public View getDropDownView(int pos, View convert, android.view.ViewGroup parent) {
                View v = super.getDropDownView(pos, convert, parent);
                ((TextView) v).setTextColor(0xFF0D3B66);
                v.setBackgroundColor(Color.WHITE);
                return v;
            }
        };
    }

    public static String worldName(Context c, int world) {
        String[] names = c.getResources().getStringArray(R.array.world_names);
        return names[Math.min(world, names.length - 1)];
    }

    private Ui() {}
}
