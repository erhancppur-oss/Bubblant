package com.bubblant.game.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.bubblant.game.engine.AntBrain;

/** Draws the cartoon ant with all its moods, gestures and hats. */
public final class AntPainter {
    public static final int HAT_COUNT = 6;
    /** stars needed to unlock each hat */
    public static final int[] HAT_STARS = {0, 12, 35, 65, 100, 150};

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private static final int[] BODY = {0xFF6D3B1F, 0xFF3E2723, 0xFFB71C1C, 0xFF5D4037, 0xFF1B5E20};

    public AntPainter() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * @param s      overall size (≈ head-to-tail length / 2)
     * @param brain  may be null for decorative ants
     * @param t      animation clock
     * @param look   color variant
     */
    public void draw(Canvas c, float x, float y, float s, AntBrain brain, float t, int hat, int look, int alpha, boolean flail) {
        c.save();
        c.translate(x, y);
        int body = BODY[look % BODY.length];
        AntBrain.Mood mood = brain == null ? AntBrain.Mood.HAPPY : brain.mood;
        float jump = brain != null && brain.jumpTimer > 0 ? (float) Math.abs(Math.sin(t * 14)) * s * 0.25f : 0;
        c.translate(0, -jump);

        stroke.setAlpha(alpha);
        fill.setAlpha(alpha);

        // legs (3 per side)
        stroke.setColor(darken(body));
        stroke.setAlpha(alpha);
        stroke.setStrokeWidth(s * 0.07f);
        float legAnim = flail ? (float) Math.sin(t * 25) * 0.5f : (float) Math.sin(t * 6) * 0.12f;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                float by = s * (0.0f + i * 0.12f);
                float a = (i - 1) * 0.55f + (i % 2 == 0 ? legAnim : -legAnim);
                float kx = side * s * 0.38f, ky = by + (float) Math.sin(a) * s * 0.2f;
                float fx = side * s * 0.55f, fy = by + s * 0.25f + (float) Math.sin(a) * s * 0.25f;
                path.reset();
                path.moveTo(0, by);
                path.lineTo(kx, ky - s * 0.08f);
                path.lineTo(fx, fy);
                c.drawPath(path, stroke);
            }
        }

        // abdomen + thorax
        fill.setColor(body);
        fill.setAlpha(alpha);
        rect.set(-s * 0.34f, s * 0.18f, s * 0.34f, s * 0.78f);
        c.drawOval(rect, fill);
        rect.set(-s * 0.2f, -s * 0.1f, s * 0.2f, s * 0.26f);
        c.drawOval(rect, fill);
        fill.setColor(Color.WHITE);
        fill.setAlpha(alpha * 70 / 255);
        rect.set(-s * 0.2f, s * 0.26f, -s * 0.02f, s * 0.45f);
        c.drawOval(rect, fill);

        // pointing arm
        if (brain != null && brain.pointTimer > 0 && !flail) {
            float a = brain.pointAngle;
            float ax = (float) Math.cos(a), ay = (float) Math.sin(a);
            stroke.setColor(darken(body));
            stroke.setAlpha(alpha);
            stroke.setStrokeWidth(s * 0.09f);
            float reach = s * (0.75f + 0.08f * (float) Math.sin(t * 10));
            c.drawLine(0, s * 0.05f, ax * reach, s * 0.05f + ay * reach, stroke);
            fill.setColor(0xFFFFE0B2);
            fill.setAlpha(alpha);
            c.drawCircle(ax * reach, s * 0.05f + ay * reach, s * 0.09f, fill);
        }

        // head
        float shake = brain != null && brain.shakeTimer > 0 ? (float) Math.sin(t * 28) * s * 0.12f * Math.min(1f, brain.shakeTimer) : 0;
        float tremble = mood == AntBrain.Mood.SCARED ? (float) Math.sin(t * 60) * s * 0.02f : 0;
        c.save();
        c.translate(shake + tremble, -s * 0.38f);
        c.rotate(shake / s * 40f);
        float hr = s * 0.36f;

        // antennae
        stroke.setColor(darken(body));
        stroke.setAlpha(alpha);
        stroke.setStrokeWidth(s * 0.05f);
        float wig = (float) Math.sin(t * 5) * s * 0.08f;
        if (mood == AntBrain.Mood.SAD) wig = -s * 0.1f;
        for (int side = -1; side <= 1; side += 2) {
            path.reset();
            path.moveTo(side * hr * 0.35f, -hr * 0.8f);
            path.quadTo(side * hr * 0.6f, -hr * 1.6f, side * hr * 1.0f + wig * side, -hr * 1.65f + (mood == AntBrain.Mood.SAD ? hr * 0.6f : 0));
            c.drawPath(path, stroke);
            fill.setColor(mood == AntBrain.Mood.ANGRY ? 0xFFFF1744 : body);
            fill.setAlpha(alpha);
            c.drawCircle(side * hr * 1.0f + wig * side, -hr * 1.65f + (mood == AntBrain.Mood.SAD ? hr * 0.6f : 0), s * 0.07f, fill);
        }

        int headColor = mood == AntBrain.Mood.ANGRY ? blend(body, 0xFFE53935, 0.55f + 0.2f * (float) Math.sin(t * 12)) : body;
        fill.setColor(headColor);
        fill.setAlpha(alpha);
        c.drawCircle(0, 0, hr, fill);
        fill.setColor(Color.WHITE);
        fill.setAlpha(alpha * 60 / 255);
        c.drawCircle(-hr * 0.4f, -hr * 0.45f, hr * 0.25f, fill);

        // eyes
        float ex = hr * 0.42f, ey = -hr * 0.12f;
        float eyeR = mood == AntBrain.Mood.SCARED ? hr * 0.34f : hr * 0.28f;
        float lookX = 0, lookY = 0;
        if (brain != null) { lookX = brain.desiredX * eyeR * 0.35f; lookY = brain.desiredY * eyeR * 0.35f; }
        if (mood == AntBrain.Mood.HAPPY && (brain == null || brain.thumbTimer > 0 || brain.jumpTimer > 0)) {
            stroke.setColor(Color.BLACK);
            stroke.setAlpha(alpha);
            stroke.setStrokeWidth(s * 0.06f);
            for (int side = -1; side <= 1; side += 2) {
                rect.set(side * ex - eyeR * 0.8f, ey - eyeR * 0.6f, side * ex + eyeR * 0.8f, ey + eyeR * 0.8f);
                c.drawArc(rect, 200, 140, false, stroke);
            }
        } else {
            for (int side = -1; side <= 1; side += 2) {
                fill.setColor(Color.WHITE);
                fill.setAlpha(alpha);
                c.drawCircle(side * ex, ey, eyeR, fill);
                fill.setColor(Color.BLACK);
                fill.setAlpha(alpha);
                float pr = mood == AntBrain.Mood.SCARED ? eyeR * 0.3f : eyeR * 0.5f;
                if (flail) {
                    // X eyes while drowning
                    stroke.setColor(Color.BLACK);
                    stroke.setStrokeWidth(s * 0.05f);
                    c.drawLine(side * ex - pr, ey - pr, side * ex + pr, ey + pr, stroke);
                    c.drawLine(side * ex - pr, ey + pr, side * ex + pr, ey - pr, stroke);
                } else {
                    c.drawCircle(side * ex + lookX, ey + lookY, pr, fill);
                    fill.setColor(Color.WHITE);
                    fill.setAlpha(alpha);
                    c.drawCircle(side * ex + lookX - pr * 0.3f, ey + lookY - pr * 0.3f, pr * 0.3f, fill);
                }
            }
            // eyebrows
            stroke.setColor(Color.BLACK);
            stroke.setAlpha(alpha);
            stroke.setStrokeWidth(s * 0.06f);
            if (mood == AntBrain.Mood.ANGRY) {
                for (int side = -1; side <= 1; side += 2)
                    c.drawLine(side * (ex + eyeR), ey - eyeR * 1.5f, side * (ex - eyeR * 0.6f), ey - eyeR * 0.9f, stroke);
            } else if (mood == AntBrain.Mood.SAD) {
                for (int side = -1; side <= 1; side += 2)
                    c.drawLine(side * (ex + eyeR), ey - eyeR * 0.95f, side * (ex - eyeR * 0.6f), ey - eyeR * 1.5f, stroke);
            } else if (mood == AntBrain.Mood.SCARED) {
                for (int side = -1; side <= 1; side += 2)
                    c.drawLine(side * (ex + eyeR * 0.8f), ey - eyeR * 1.6f, side * (ex - eyeR * 0.6f), ey - eyeR * 1.7f, stroke);
            }
        }

        // mouth
        stroke.setColor(0xFF1A0E0A);
        stroke.setAlpha(alpha);
        stroke.setStrokeWidth(s * 0.05f);
        float my = hr * 0.45f;
        boolean talking = brain != null && brain.speechTimer > 0 && Math.sin(t * 22) > 0;
        if (flail || mood == AntBrain.Mood.SCARED || (talking && mood != AntBrain.Mood.ANGRY)) {
            fill.setColor(0xFF3E0B0B);
            fill.setAlpha(alpha);
            rect.set(-hr * 0.18f, my - hr * 0.14f, hr * 0.18f, my + hr * 0.18f);
            c.drawOval(rect, fill);
        } else if (mood == AntBrain.Mood.ANGRY) {
            path.reset();
            path.moveTo(-hr * 0.3f, my + hr * 0.05f);
            path.lineTo(-hr * 0.15f, my - hr * 0.05f);
            path.lineTo(0, my + hr * 0.05f);
            path.lineTo(hr * 0.15f, my - hr * 0.05f);
            path.lineTo(hr * 0.3f, my + hr * 0.05f);
            c.drawPath(path, stroke);
        } else if (mood == AntBrain.Mood.SAD) {
            rect.set(-hr * 0.3f, my, hr * 0.3f, my + hr * 0.4f);
            c.drawArc(rect, 200, 140, false, stroke);
        } else {
            rect.set(-hr * 0.32f, my - hr * 0.3f, hr * 0.32f, my + hr * 0.15f);
            c.drawArc(rect, 20, 140, false, stroke);
        }

        drawHat(c, hat, hr, alpha);
        c.restore();

        // angry steam
        if (mood == AntBrain.Mood.ANGRY) {
            fill.setColor(Color.WHITE);
            for (int i = 0; i < 3; i++) {
                float ph = (t * 1.5f + i * 0.33f) % 1f;
                fill.setAlpha((int) (alpha * (1 - ph) * 0.8f));
                float side = i % 2 == 0 ? -1 : 1;
                c.drawCircle(side * s * (0.45f + ph * 0.2f), -s * (0.85f + ph * 0.6f), s * (0.06f + ph * 0.08f), fill);
            }
        }

        // thumbs up
        if (brain != null && brain.thumbTimer > 0) {
            text.setTextSize(s * 0.75f);
            text.setAlpha(alpha);
            float pop = Math.min(1f, (1.6f - brain.thumbTimer) * 6f + 0.3f);
            c.save();
            c.translate(s * 0.75f, -s * 0.55f);
            c.scale(pop, pop);
            c.drawText("👍", 0, s * 0.25f, text);
            c.restore();
        }
        c.restore();
    }

    private void drawHat(Canvas c, int hat, float hr, int alpha) {
        if (hat <= 0) return;
        fill.setAlpha(alpha);
        switch (hat) {
            case 1: // red cap
                fill.setColor(0xFFE53935);
                fill.setAlpha(alpha);
                rect.set(-hr * 0.85f, -hr * 1.25f, hr * 0.85f, -hr * 0.2f);
                c.drawArc(rect, 180, 180, true, fill);
                rect.set(-hr * 0.2f, -hr * 0.8f, hr * 1.25f, -hr * 0.6f);
                c.drawRoundRect(rect, hr * 0.1f, hr * 0.1f, fill);
                fill.setColor(Color.WHITE);
                fill.setAlpha(alpha);
                c.drawCircle(0, -hr * 1.2f, hr * 0.1f, fill);
                break;
            case 2: // pink bow
                fill.setColor(0xFFFF4081);
                fill.setAlpha(alpha);
                path.reset();
                path.moveTo(0, -hr * 0.9f);
                path.lineTo(-hr * 0.6f, -hr * 1.25f);
                path.lineTo(-hr * 0.6f, -hr * 0.55f);
                path.close();
                path.moveTo(0, -hr * 0.9f);
                path.lineTo(hr * 0.6f, -hr * 1.25f);
                path.lineTo(hr * 0.6f, -hr * 0.55f);
                path.close();
                c.drawPath(path, fill);
                c.drawCircle(0, -hr * 0.9f, hr * 0.15f, fill);
                break;
            case 3: // pirate hat
                fill.setColor(0xFF212121);
                fill.setAlpha(alpha);
                path.reset();
                path.moveTo(-hr * 1.15f, -hr * 0.6f);
                path.quadTo(0, -hr * 2.0f, hr * 1.15f, -hr * 0.6f);
                path.quadTo(0, -hr * 0.9f, -hr * 1.15f, -hr * 0.6f);
                c.drawPath(path, fill);
                fill.setColor(Color.WHITE);
                fill.setAlpha(alpha);
                c.drawCircle(0, -hr * 1.05f, hr * 0.14f, fill);
                break;
            case 4: // crown
                fill.setColor(0xFFFFC107);
                fill.setAlpha(alpha);
                path.reset();
                path.moveTo(-hr * 0.7f, -hr * 0.7f);
                path.lineTo(-hr * 0.75f, -hr * 1.45f);
                path.lineTo(-hr * 0.35f, -hr * 1.05f);
                path.lineTo(0, -hr * 1.6f);
                path.lineTo(hr * 0.35f, -hr * 1.05f);
                path.lineTo(hr * 0.75f, -hr * 1.45f);
                path.lineTo(hr * 0.7f, -hr * 0.7f);
                path.close();
                c.drawPath(path, fill);
                fill.setColor(0xFFE91E63);
                fill.setAlpha(alpha);
                c.drawCircle(0, -hr * 0.95f, hr * 0.12f, fill);
                break;
            default: // wizard hat
                fill.setColor(0xFF3949AB);
                fill.setAlpha(alpha);
                path.reset();
                path.moveTo(-hr * 0.95f, -hr * 0.65f);
                path.lineTo(hr * 0.2f, -hr * 2.3f);
                path.lineTo(hr * 0.95f, -hr * 0.65f);
                path.close();
                c.drawPath(path, fill);
                fill.setColor(0xFFFFEB3B);
                fill.setAlpha(alpha);
                c.drawCircle(hr * 0.05f, -hr * 1.3f, hr * 0.13f, fill);
                break;
        }
    }

    private static int darken(int c) {
        return Color.argb(255, (int) (Color.red(c) * 0.6f), (int) (Color.green(c) * 0.6f), (int) (Color.blue(c) * 0.6f));
    }

    private static int blend(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        return Color.argb(255,
                (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }
}
