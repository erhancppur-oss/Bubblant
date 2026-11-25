package com.bubblant.game;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.widget.TextView;

import com.bubblant.game.view.AntPainter;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Debug-only tool: renders the heroic app icon with the game's own ant painter and saves PNGs
 * (launcher layers for every density + the 512px Play Store icon) to the app's external files dir.
 */
public class IconExportActivity extends Activity {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AntPainter ant = new AntPainter();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        File dir = new File(getFilesDir(), "icons");
        dir.mkdirs();
        StringBuilder log = new StringBuilder();
        try {
            save(full(512, false), new File(dir, "store-icon-512.png"));
            String[] dens = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
            float[] mul = {1f, 1.5f, 2f, 3f, 4f};
            for (int i = 0; i < dens.length; i++) {
                int fg = Math.round(108 * mul[i]);
                save(background(fg), new File(dir, "bg-" + dens[i] + ".png"));
                save(foreground(fg), new File(dir, "fg-" + dens[i] + ".png"));
                save(full(Math.round(48 * mul[i]), true), new File(dir, "legacy-" + dens[i] + ".png"));
            }
            log.append("OK ").append(dir.getAbsolutePath());
        } catch (Exception e) {
            log.append("FAIL ").append(e);
        }
        TextView tv = new TextView(this);
        tv.setText(log);
        setContentView(tv);
    }

    private void save(Bitmap bmp, File f) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }

    /** Sunburst sky + sea: the heroic backdrop. */
    private void drawBackdrop(Canvas c, float S) {
        p.setColor(Color.WHITE); p.setShader(new RadialGradient(S * 0.5f, S * 0.36f, S * 0.75f, new int[]{0xFFFFF59D, 0xFFFFB300, 0xFFF4511E}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, S, S, p);
        p.setShader(null);
        // rays
        p.setColor(Color.WHITE);
        p.setAlpha(46);
        Path ray = new Path();
        float cx = S * 0.5f, cy = S * 0.36f, R = S * 1.2f;
        for (int i = 0; i < 16; i++) {
            double a0 = Math.toRadians(i * 22.5), a1 = Math.toRadians(i * 22.5 + 10);
            ray.reset();
            ray.moveTo(cx, cy);
            ray.lineTo(cx + (float) Math.cos(a0) * R, cy + (float) Math.sin(a0) * R);
            ray.lineTo(cx + (float) Math.cos(a1) * R, cy + (float) Math.sin(a1) * R);
            ray.close();
            c.drawPath(ray, p);
        }
        p.setAlpha(255);
        // sea with a wave crest
        Path sea = new Path();
        sea.moveTo(0, S * 0.7f);
        sea.cubicTo(S * 0.25f, S * 0.62f, S * 0.4f, S * 0.78f, S * 0.62f, S * 0.68f);
        sea.cubicTo(S * 0.8f, S * 0.6f, S * 0.9f, S * 0.72f, S, S * 0.66f);
        sea.lineTo(S, S);
        sea.lineTo(0, S);
        sea.close();
        p.setColor(Color.WHITE); p.setShader(new LinearGradient(0, S * 0.62f, 0, S, 0xFF4FC3F7, 0xFF01579B, Shader.TileMode.CLAMP));
        c.drawPath(sea, p);
        p.setShader(null);
        Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
        s.setStyle(Paint.Style.STROKE);
        s.setColor(0xCCFFFFFF);
        s.setStrokeWidth(S * 0.018f);
        s.setStrokeCap(Paint.Cap.ROUND);
        Path crest = new Path();
        crest.moveTo(0, S * 0.7f);
        crest.cubicTo(S * 0.25f, S * 0.62f, S * 0.4f, S * 0.78f, S * 0.62f, S * 0.68f);
        crest.cubicTo(S * 0.8f, S * 0.6f, S * 0.9f, S * 0.72f, S, S * 0.66f);
        c.drawPath(crest, s);
        // sparkles
        p.setColor(Color.WHITE);
        float[][] st = {{0.14f, 0.18f, 0.03f}, {0.86f, 0.14f, 0.025f}, {0.82f, 0.44f, 0.018f}, {0.16f, 0.5f, 0.02f}};
        for (float[] q : st) star(c, q[0] * S, q[1] * S, q[2] * S);
    }

    private void star(Canvas c, float x, float y, float r) {
        Path path = new Path();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i - Math.PI / 2;
            float rr = i % 2 == 0 ? r : r * 0.3f;
            float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.close();
        c.drawPath(path, p);
    }

    /** Bubble with the caped ant standing proudly on top. Centered at (cx, cy) with bubble radius r. */
    private void drawHero(Canvas c, float cx, float cy, float r) {
        // shadow on the water
        p.setColor(0x33000000);
        c.drawOval(cx - r * 0.85f, cy + r * 0.75f, cx + r * 0.95f, cy + r * 1.1f, p);
        // bubble
        p.setColor(Color.WHITE); p.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.32f, r * 1.3f,
                new int[]{0x55FFFFFF, 0x44B3E5FC, 0x6681D4FA, 0xCCFFFFFF}, new float[]{0f, 0.55f, 0.85f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);
        Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
        s.setStyle(Paint.Style.STROKE);
        s.setStrokeWidth(r * 0.09f);
        s.setShader(new SweepGradient(cx, cy, new int[]{0x99FF80AB, 0x9980D8FF, 0x99B9F6CA, 0x99FFE57F, 0x99EA80FC, 0x99FF80AB}, null));
        c.drawCircle(cx, cy, r * 0.95f, s);
        s.setShader(null);
        s.setColor(Color.WHITE);
        s.setStrokeWidth(r * 0.035f);
        c.drawCircle(cx, cy, r, s);
        p.setColor(Color.WHITE); p.setShader(new RadialGradient(cx - r * 0.42f, cy - r * 0.45f, r * 0.38f, 0xEEFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawOval(cx - r * 0.75f, cy - r * 0.72f, cx - r * 0.1f, cy - r * 0.2f, p);
        p.setShader(null);
        // hero ant on top, front leg raised to the sky
        float feet = cy - r * 0.36f;
        p.setColor(0x44000000);
        c.drawOval(cx - r * 0.7f, feet - r * 0.05f, cx + r * 0.6f, feet + r * 0.1f, p);
        ant.heroPointAngle = Float.NaN;
        ant.draw(c, cx + r * 0.05f, feet, r * 0.95f, null, 0.35f, AntPainter.HAT_HERO, 0, 255, false, 1f);
    }

    private Bitmap background(int size) {
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        drawBackdrop(new Canvas(b), size);
        return b;
    }

    /** Adaptive icon foreground: keep everything inside the 66dp safe circle of the 108dp layer. */
    private Bitmap foreground(int size) {
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        drawHero(c, size * 0.5f, size * 0.6f, size * 0.19f);
        return b;
    }

    /** Full icon (Play Store / legacy launcher). */
    private Bitmap full(int size, boolean rounded) {
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        drawBackdrop(c, size);
        drawHero(c, size * 0.5f, size * 0.62f, size * 0.27f);
        if (rounded) {
            Bitmap m = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas mc = new Canvas(m);
            Paint mp = new Paint(Paint.ANTI_ALIAS_FLAG);
            mc.drawRoundRect(new RectF(0, 0, size, size), size * 0.22f, size * 0.22f, mp);
            mp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            mc.drawBitmap(b, 0, 0, mp);
            return m;
        }
        return b;
    }
}
