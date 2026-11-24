package com.bubblant.game.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.bubblant.game.Prefs;
import com.bubblant.game.engine.AntBrain;

import java.util.Random;

/** Animated underwater background for the menus with the ant waving on its bubble. */
public final class MenuBubblesView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AntPainter ant = new AntPainter();
    private final float[] bx = new float[30], by = new float[30], br = new float[30], bs = new float[30];
    private long start = System.nanoTime();
    private long last = start;
    private Shader bg;
    private boolean showAnt = true;

    public MenuBubblesView(Context c) { this(c, null); }

    public MenuBubblesView(Context c, AttributeSet a) {
        super(c, a);
        Random r = new Random();
        for (int i = 0; i < bx.length; i++) {
            bx[i] = r.nextFloat();
            by[i] = r.nextFloat();
            br[i] = 4 + r.nextFloat() * 14;
            bs[i] = 0.03f + r.nextFloat() * 0.08f;
        }
        s.setStyle(Paint.Style.STROKE);
    }

    public void setShowAnt(boolean v) { showAnt = v; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        bg = new LinearGradient(0, 0, 0, h, 0xFF4FC3F7, 0xFF01579B, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = System.nanoTime();
        float t = (now - start) / 1e9f;
        float dt = Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        int W = getWidth(), H = getHeight();
        p.setShader(bg);
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);
        s.setColor(0x30FFFFFF);
        s.setStrokeWidth(6);
        for (int i = 0; i < 6; i++) {
            float base = i * H / 6f;
            float px = 0, py = base;
            for (int x = 0; x <= W; x += W / 10) {
                float yy = base + (float) Math.sin(x * 0.008 + t * (0.7 + i * 0.1) + i) * 30;
                if (x > 0) c.drawLine(px, py, x, yy, s);
                px = x; py = yy;
            }
        }
        for (int i = 0; i < bx.length; i++) {
            by[i] -= bs[i] * dt;
            if (by[i] < -0.05f) { by[i] = 1.05f; bx[i] = (float) Math.random(); }
            float x = bx[i] * W + (float) Math.sin(t * 2 + i) * 10, y = by[i] * H;
            s.setColor(0x99FFFFFF);
            s.setStrokeWidth(3);
            c.drawCircle(x, y, br[i], s);
            p.setColor(0x88FFFFFF);
            c.drawCircle(x - br[i] * 0.35f, y - br[i] * 0.35f, br[i] * 0.25f, p);
        }
        if (showAnt) {
            float cx = W / 2f + (float) Math.sin(t * 0.8f) * W * 0.06f;
            float cy = H * 0.24f + (float) Math.sin(t * 1.3f) * 14;
            float r = W * 0.15f;
            p.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
                    new int[]{0x66FFFFFF, 0x22B3E5FC, 0x6681D4FA}, new float[]{0, 0.6f, 1}, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, r, p);
            p.setShader(null);
            s.setColor(0xDDFFFFFF);
            s.setStrokeWidth(5);
            c.drawCircle(cx, cy, r, s);
            p.setColor(0xDDFFFFFF);
            c.drawOval(cx - r * 0.65f, cy - r * 0.7f, cx - r * 0.15f, cy - r * 0.35f, p);
            ant.draw(c, cx, cy, r * 0.8f, null, t, Prefs.hat(), 0, 255, false);
        }
        postInvalidateOnAnimation();
    }
}
