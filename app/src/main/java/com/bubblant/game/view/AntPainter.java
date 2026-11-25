package com.bubblant.game.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import com.bubblant.game.engine.AntBrain;

/**
 * A realistic-looking ant seen from the side, standing ON the bubble:
 * head with compound eye and mandibles, elbowed antennae, thorax, petiole, banded gaster
 * and six jointed legs whose feet touch the surface it stands on.
 * The ant keeps its personality: it points, shakes its head, gets angry, gives a thumbs up.
 */
public final class AntPainter {
    public static final int HAT_COUNT = 7;
    public static final int HAT_HERO = 6;
    /** stars needed to unlock each hat (the hero costume is unlocked early, it is the ant's signature look) */
    public static final int[] HAT_STARS = {0, 20, 60, 120, 200, 300, 10};

    /** When drawing without a brain (icons, menus): raise a front leg towards this angle (radians). */
    public float heroPointAngle = Float.NaN;

    /** body colors: [dark, mid, highlight] for each variant */
    private static final int[][] SKIN = {
            {0xFF2A120A, 0xFF6B2E14, 0xFFC0703A}, // red-brown wood ant (hero)
            {0xFF0E0E10, 0xFF2B2B30, 0xFF8A8A95}, // black ant
            {0xFF3A0A06, 0xFF9A2312, 0xFFFF7A4A}, // fire ant
            {0xFF2E3A08, 0xFF7C8F1C, 0xFFD8E870}, // green weaver ant
            {0xFF3E2A06, 0xFFA87A18, 0xFFFFD980}, // golden ant
    };

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    public AntPainter() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * @param x,y     point where the feet touch the surface
     * @param s       size (ant is about 1.7*s long)
     * @param facing  -1 (left) .. 1 (right); values near 0 look like the ant is turning
     */
    public void draw(Canvas c, float x, float y, float s, AntBrain brain, float t, int hat, int look,
                     int alpha, boolean flail, float facing) {
        int[] sk = SKIN[look % SKIN.length];
        AntBrain.Mood mood = brain == null ? AntBrain.Mood.HAPPY : brain.mood;
        boolean angry = mood == AntBrain.Mood.ANGRY;
        boolean scared = mood == AntBrain.Mood.SCARED;
        boolean sad = mood == AntBrain.Mood.SAD;

        c.save();
        c.translate(x, y);
        float jump = brain != null && brain.jumpTimer > 0 ? (float) Math.abs(Math.sin(t * 13)) * s * 0.28f : 0;
        float tremble = scared ? (float) Math.sin(t * 70) * s * 0.012f : 0;
        float f = Math.abs(facing) < 0.18f ? Math.copySign(0.18f, facing == 0 ? 1 : facing) : facing;
        c.scale(f, 1);
        c.translate(tremble, -jump);

        float walk = brain == null ? t * 4 : t * 9;
        float legAmp = flail ? 0.16f : 0.035f;
        float legSpeed = flail ? 3.2f : 1f;

        // ---- far-side legs (behind the body)
        drawLegs(c, s, sk, alpha, walk * legSpeed, legAmp, true, brain, flail, facing);

        if (hat == HAT_HERO) drawCape(c, s, alpha, t);

        // ---- gaster (abdomen)
        c.save();
        c.rotate(-16, -0.62f * s, -0.52f * s);
        rect.set(-1.03f * s, -0.82f * s, -0.21f * s, -0.22f * s);
        fill.setShader(new RadialGradient(-0.5f * s, -0.68f * s, 0.55f * s,
                new int[]{sk[2], sk[1], sk[0]}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        fill.setAlpha(alpha);
        c.drawOval(rect, fill);
        fill.setShader(null);
        // segment bands
        stroke.setShader(null);
        stroke.setColor(sk[0]);
        stroke.setAlpha(alpha * 140 / 255);
        stroke.setStrokeWidth(s * 0.025f);
        for (int i = 1; i <= 3; i++) {
            float bx = -0.32f * s - i * 0.16f * s;
            rect.set(bx - 0.1f * s, -0.8f * s, bx + 0.1f * s, -0.24f * s);
            c.drawArc(rect, 280, 160, false, stroke);
        }
        // specular highlight
        fill.setColor(Color.WHITE);
        fill.setAlpha(alpha * 90 / 255);
        rect.set(-0.72f * s, -0.76f * s, -0.42f * s, -0.66f * s);
        c.drawOval(rect, fill);
        // tiny hairs at the tip
        stroke.setColor(sk[0]);
        stroke.setAlpha(alpha);
        stroke.setStrokeWidth(s * 0.012f);
        c.drawLine(-1.02f * s, -0.5f * s, -1.1f * s, -0.47f * s, stroke);
        c.drawLine(-0.98f * s, -0.38f * s, -1.05f * s, -0.33f * s, stroke);
        c.restore();

        // ---- petiole (waist)
        fill.setColor(sk[1]);
        fill.setAlpha(alpha);
        c.drawCircle(-0.2f * s, -0.52f * s, 0.075f * s, fill);
        stroke.setColor(sk[1]);
        stroke.setAlpha(alpha);
        stroke.setStrokeWidth(s * 0.07f);
        c.drawLine(-0.3f * s, -0.52f * s, -0.12f * s, -0.53f * s, stroke);

        // ---- mesosoma (thorax)
        c.save();
        c.rotate(-12, 0.06f * s, -0.56f * s);
        rect.set(-0.18f * s, -0.68f * s, 0.32f * s, -0.44f * s);
        fill.setShader(new RadialGradient(0.1f * s, -0.66f * s, 0.32f * s,
                new int[]{sk[2], sk[1], sk[0]}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        fill.setAlpha(alpha);
        c.drawOval(rect, fill);
        fill.setShader(null);
        fill.setColor(sk[1]);
        fill.setAlpha(alpha);
        c.drawCircle(0.2f * s, -0.6f * s, 0.095f * s, fill); // pronotum hump
        fill.setColor(Color.WHITE);
        fill.setAlpha(alpha * 70 / 255);
        rect.set(-0.02f * s, -0.67f * s, 0.18f * s, -0.62f * s);
        c.drawOval(rect, fill);
        c.restore();

        // ---- near-side legs (in front of the body)
        drawLegs(c, s, sk, alpha, walk * legSpeed, legAmp, false, brain, flail, facing);

        // ---- head group
        float shake = brain != null && brain.shakeTimer > 0 ? (float) Math.sin(t * 26) * 14f * Math.min(1f, brain.shakeTimer) : 0;
        float nod = brain != null && brain.speechTimer > 0 && !angry ? (float) Math.sin(t * 9) * 3f : 0;
        c.save();
        c.rotate(shake + nod + (sad ? 10 : 0), 0.3f * s, -0.64f * s);

        // antennae (far one first)
        drawAntenna(c, s, sk, alpha, t, sad, angry, true);

        int headMid = angry ? blend(sk[1], 0xFFD32F2F, 0.45f + 0.2f * (float) Math.sin(t * 10)) : sk[1];
        int headHi = angry ? blend(sk[2], 0xFFFF8A80, 0.5f) : sk[2];
        c.save();
        c.rotate(8, 0.47f * s, -0.7f * s);
        rect.set(0.25f * s, -0.9f * s, 0.69f * s, -0.52f * s);
        fill.setShader(new RadialGradient(0.42f * s, -0.82f * s, 0.32f * s,
                new int[]{headHi, headMid, sk[0]}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        fill.setAlpha(alpha);
        c.drawOval(rect, fill);
        fill.setShader(null);
        c.restore();

        if (hat == HAT_HERO) drawMask(c, s, alpha, t);

        // mandibles (open when talking or angry)
        boolean talking = brain != null && brain.speechTimer > 0 && Math.sin(t * 20) > 0;
        float open = (talking || angry || flail) ? 14f : 3f;
        stroke.setColor(sk[0]);
        stroke.setAlpha(alpha);
        stroke.setStrokeWidth(s * 0.045f);
        for (int side = -1; side <= 1; side += 2) {
            c.save();
            c.rotate(side * open, 0.64f * s, -0.62f * s);
            path.reset();
            path.moveTo(0.62f * s, -0.62f * s);
            path.quadTo(0.78f * s, -0.62f * s + side * 0.02f * s, 0.8f * s, -0.56f * s);
            c.drawPath(path, stroke);
            c.restore();
        }

        // eye
        float ex = 0.52f * s, ey = -0.76f * s;
        float er = (scared ? 0.085f : 0.07f) * s;
        if (mood == AntBrain.Mood.HAPPY && brain != null && (brain.thumbTimer > 0 || brain.jumpTimer > 0)) {
            stroke.setColor(Color.BLACK);
            stroke.setAlpha(alpha);
            stroke.setStrokeWidth(s * 0.03f);
            rect.set(ex - er, ey - er * 0.6f, ex + er, ey + er * 1.2f);
            c.drawArc(rect, 200, 140, false, stroke);
        } else if (flail) {
            stroke.setColor(Color.BLACK);
            stroke.setAlpha(alpha);
            stroke.setStrokeWidth(s * 0.025f);
            c.drawLine(ex - er, ey - er, ex + er, ey + er, stroke);
            c.drawLine(ex - er, ey + er, ex + er, ey - er, stroke);
        } else {
            if (scared || hat == HAT_HERO) {
                fill.setColor(Color.WHITE);
                fill.setAlpha(alpha);
                c.drawCircle(ex, ey, er * (scared ? 1.35f : 1.3f), fill);
            }
            fill.setShader(new RadialGradient(ex - er * 0.3f, ey - er * 0.3f, er * 1.3f,
                    new int[]{0xFF4A4A55, 0xFF050508}, null, Shader.TileMode.CLAMP));
            fill.setAlpha(alpha);
            c.drawCircle(ex, ey, er, fill);
            fill.setShader(null);
            float lx = 0, ly = 0;
            if (brain != null) { lx = brain.desiredX * Math.signum(facing == 0 ? 1 : facing) * er * 0.25f; ly = brain.desiredY * er * 0.25f; }
            fill.setColor(Color.WHITE);
            fill.setAlpha(alpha * 230 / 255);
            c.drawCircle(ex - er * 0.3f + lx, ey - er * 0.35f + ly, er * 0.32f, fill);
            fill.setAlpha(alpha * 120 / 255);
            c.drawCircle(ex + er * 0.35f + lx, ey + er * 0.3f + ly, er * 0.14f, fill);
        }
        // brow
        stroke.setColor(sk[0]);
        stroke.setAlpha(alpha);
        stroke.setStrokeWidth(s * 0.035f);
        if (angry) c.drawLine(ex - er * 1.3f, ey - er * 1.7f, ex + er * 1.1f, ey - er * 0.9f, stroke);
        else if (sad) c.drawLine(ex - er * 1.2f, ey - er * 1.0f, ex + er * 1.1f, ey - er * 1.6f, stroke);
        else if (scared) c.drawLine(ex - er * 1.2f, ey - er * 2.0f, ex + er * 1.2f, ey - er * 2.1f, stroke);

        // near antenna
        drawAntenna(c, s, sk, alpha, t, sad, angry, false);

        drawHat(c, hat, s, alpha);
        c.restore(); // head group

        // sweat drop when scared
        if (scared) {
            fill.setColor(0xFF81D4FA);
            fill.setAlpha(alpha);
            float dy = ((t * 1.5f) % 1f) * 0.2f * s;
            c.drawCircle(0.28f * s, -0.95f * s + dy, 0.035f * s, fill);
        }
        c.restore();

        // ---- effects drawn without the facing flip
        if (angry) {
            fill.setColor(Color.WHITE);
            for (int i = 0; i < 3; i++) {
                float ph = (t * 1.5f + i * 0.33f) % 1f;
                fill.setAlpha((int) (alpha * (1 - ph) * 0.8f));
                c.drawCircle(x + facing * 0.45f * s + (i - 1) * 0.12f * s, y - 1.1f * s - ph * 0.5f * s, s * (0.05f + ph * 0.07f), fill);
            }
        }
        if (brain != null && brain.thumbTimer > 0) {
            text.setTextSize(s * 0.6f);
            text.setAlpha(alpha);
            float pop = Math.min(1f, (1.6f - brain.thumbTimer) * 6f + 0.3f);
            c.save();
            c.translate(x - facing * 0.55f * s, y - 1.25f * s);
            c.scale(pop, pop);
            c.drawText("👍", 0, s * 0.2f, text);
            c.restore();
        }
    }

    private void drawLegs(Canvas c, float s, int[] sk, int alpha, float walk, float amp, boolean far,
                          AntBrain brain, boolean flail, float facing) {
        float[] ax = {0.2f, 0.06f, -0.07f};       // attach x on thorax
        float[] fx = far ? new float[]{0.46f, -0.06f, -0.72f} : new float[]{0.64f, 0.1f, -0.56f}; // feet x
        int col = far ? sk[0] : sk[1];
        int a = far ? alpha * 200 / 255 : alpha;
        for (int i = 0; i < 3; i++) {
            float phase = walk + i * 2.1f + (far ? 1.05f : 0);
            float footX = fx[i] + (float) Math.sin(phase) * amp;
            float lift = Math.max(0, (float) Math.cos(phase)) * amp * (flail ? 2.5f : 1.2f);
            float footY = -lift;
            float attX = ax[i], attY = -0.5f;
            boolean pointing = !far && i == 0 && (brain != null ? brain.pointTimer > 0 : !Float.isNaN(heroPointAngle)) && !flail;
            if (pointing) {
                float pa = brain != null ? brain.pointAngle : heroPointAngle;
                float dirX = (float) Math.cos(pa) * Math.signum(facing == 0 ? 1 : facing);
                float dirY = (float) Math.sin(pa);
                float kx = attX + 0.22f, ky = -0.78f;
                float tx = kx + dirX * 0.45f, ty = ky + dirY * 0.45f - 0.08f;
                drawLeg(c, s, col, a, attX, attY, kx, ky, tx, ty, true);
                continue;
            }
            float kneeX = attX + (footX - attX) * 0.45f + (i == 2 ? -0.06f : 0.06f);
            float kneeY = -0.82f + (far ? 0.04f : 0);
            drawLeg(c, s, col, a, attX, attY, kneeX, kneeY, footX, footY, false);
        }
    }

    private void drawLeg(Canvas c, float s, int col, int alpha, float ax, float ay, float kx, float ky,
                         float fx, float fy, boolean pointing) {
        stroke.setShader(null);
        stroke.setColor(col);
        stroke.setAlpha(alpha);
        // femur
        stroke.setStrokeWidth(s * 0.05f);
        c.drawLine(ax * s, ay * s, kx * s, ky * s, stroke);
        // tibia
        stroke.setStrokeWidth(s * 0.034f);
        c.drawLine(kx * s, ky * s, fx * s, fy * s, stroke);
        // tarsus (foot) lying on the surface
        stroke.setStrokeWidth(s * 0.02f);
        if (pointing) {
            fill.setColor(col);
            fill.setAlpha(alpha);
            c.drawCircle(fx * s, fy * s, s * 0.035f, fill);
        } else {
            float dir = fx >= kx ? 1 : -1;
            c.drawLine(fx * s, fy * s, (fx + dir * 0.1f) * s, (fy + 0.01f) * s, stroke);
        }
        // knee joint
        fill.setColor(col);
        fill.setAlpha(alpha);
        c.drawCircle(kx * s, ky * s, s * 0.03f, fill);
    }

    private void drawAntenna(Canvas c, float s, int[] sk, int alpha, float t, boolean sad, boolean angry, boolean far) {
        float wig = (float) Math.sin(t * (angry ? 12 : 4) + (far ? 1.3f : 0)) * 0.05f;
        float bx = far ? 0.44f : 0.5f, by = -0.86f;
        float exx = (far ? 0.5f : 0.58f), eyy = sad ? -1.02f : -1.2f;
        float tx = (far ? 0.8f : 0.9f) + wig, ty = sad ? -0.86f : -1.1f + wig;
        stroke.setColor(far ? sk[0] : sk[1]);
        stroke.setAlpha(far ? alpha * 200 / 255 : alpha);
        stroke.setStrokeWidth(s * 0.028f);
        c.drawLine(bx * s, by * s, exx * s, eyy * s, stroke);   // scape
        stroke.setStrokeWidth(s * 0.022f);
        c.drawLine(exx * s, eyy * s, tx * s, ty * s, stroke);   // funiculus
        fill.setColor(far ? sk[0] : sk[1]);
        fill.setAlpha(alpha);
        c.drawCircle(tx * s, ty * s, s * 0.035f, fill);         // club
    }

    /** Superhero cape flowing back from the shoulders. */
    private void drawCape(Canvas c, float s, int alpha, float t) {
        float w1 = (float) Math.sin(t * 5) * 0.06f, w2 = (float) Math.sin(t * 5 + 1.4f) * 0.08f;
        path.reset();
        path.moveTo(0.24f * s, -0.7f * s);
        path.cubicTo(-0.1f * s, -0.95f * s, -0.6f * s, (-1.05f + w1) * s, -1.25f * s, (-0.98f + w2) * s);
        path.quadTo(-1.18f * s, (-0.82f + w1) * s, -1.32f * s, (-0.7f + w2) * s);
        path.quadTo(-1.05f * s, (-0.72f + w1) * s, -0.95f * s, (-0.6f + w2) * s);
        path.cubicTo(-0.5f * s, -0.72f * s, -0.1f * s, -0.62f * s, 0.2f * s, -0.58f * s);
        path.close();
        fill.setShader(new LinearGradient(0, -1.0f * s, 0, -0.6f * s, 0xFFFF5252, 0xFFB71C1C, Shader.TileMode.CLAMP));
        fill.setAlpha(alpha);
        c.drawPath(path, fill);
        fill.setShader(null);
        stroke.setColor(0xFF7F0000);
        stroke.setAlpha(alpha);
        stroke.setStrokeWidth(s * 0.02f);
        c.drawPath(path, stroke);
        // collar clasp
        fill.setColor(0xFFFFD54F);
        fill.setAlpha(alpha);
        c.drawCircle(0.22f * s, -0.66f * s, 0.05f * s, fill);
    }

    /** Hero mask over the eye, with two little ribbons at the back of the head. */
    private void drawMask(Canvas c, float s, int alpha, float t) {
        float wob = (float) Math.sin(t * 7) * 0.03f;
        fill.setColor(0xFF1A237E);
        fill.setAlpha(alpha);
        rect.set(0.33f * s, -0.86f * s, 0.67f * s, -0.68f * s);
        c.drawRoundRect(rect, 0.09f * s, 0.09f * s, fill);
        path.reset();
        path.moveTo(0.34f * s, -0.8f * s);
        path.lineTo(0.14f * s, (-0.9f + wob) * s);
        path.lineTo(0.17f * s, (-0.8f + wob) * s);
        path.close();
        path.moveTo(0.34f * s, -0.76f * s);
        path.lineTo(0.12f * s, (-0.74f - wob) * s);
        path.lineTo(0.18f * s, (-0.68f - wob) * s);
        path.close();
        c.drawPath(path, fill);
    }

    private void drawHat(Canvas c, int hat, float s, int alpha) {
        if (hat <= 0 || hat == HAT_HERO) return;
        float hx = 0.44f * s, hy = -0.88f * s, hr = 0.2f * s;
        c.save();
        c.translate(hx, hy);
        c.rotate(-8);
        fill.setShader(null);
        switch (hat) {
            case 1: // red cap with visor pointing forward
                fill.setColor(0xFFE53935);
                fill.setAlpha(alpha);
                rect.set(-hr, -hr * 0.9f, hr, hr * 0.5f);
                c.drawArc(rect, 180, 180, true, fill);
                rect.set(0, -hr * 0.25f, hr * 1.7f, hr * 0.05f);
                c.drawRoundRect(rect, hr * 0.12f, hr * 0.12f, fill);
                fill.setColor(Color.WHITE);
                fill.setAlpha(alpha);
                c.drawCircle(0, -hr * 0.9f, hr * 0.13f, fill);
                break;
            case 2: // bow
                fill.setColor(0xFFFF4081);
                fill.setAlpha(alpha);
                path.reset();
                path.moveTo(0, -hr * 0.3f);
                path.lineTo(-hr * 0.8f, -hr * 0.8f);
                path.lineTo(-hr * 0.8f, hr * 0.15f);
                path.close();
                path.moveTo(0, -hr * 0.3f);
                path.lineTo(hr * 0.8f, -hr * 0.8f);
                path.lineTo(hr * 0.8f, hr * 0.15f);
                path.close();
                c.drawPath(path, fill);
                c.drawCircle(0, -hr * 0.3f, hr * 0.2f, fill);
                break;
            case 3: // pirate hat
                fill.setColor(0xFF1C1C1C);
                fill.setAlpha(alpha);
                path.reset();
                path.moveTo(-hr * 1.3f, 0);
                path.quadTo(0, -hr * 1.9f, hr * 1.3f, 0);
                path.quadTo(0, -hr * 0.4f, -hr * 1.3f, 0);
                c.drawPath(path, fill);
                fill.setColor(Color.WHITE);
                fill.setAlpha(alpha);
                c.drawCircle(0, -hr * 0.55f, hr * 0.16f, fill);
                break;
            case 4: // crown
                fill.setShader(new LinearGradient(0, -hr * 1.2f, 0, 0, 0xFFFFF176, 0xFFFFA000, Shader.TileMode.CLAMP));
                fill.setAlpha(alpha);
                path.reset();
                path.moveTo(-hr * 0.8f, 0);
                path.lineTo(-hr * 0.9f, -hr * 0.9f);
                path.lineTo(-hr * 0.4f, -hr * 0.45f);
                path.lineTo(0, -hr * 1.15f);
                path.lineTo(hr * 0.4f, -hr * 0.45f);
                path.lineTo(hr * 0.9f, -hr * 0.9f);
                path.lineTo(hr * 0.8f, 0);
                path.close();
                c.drawPath(path, fill);
                fill.setShader(null);
                fill.setColor(0xFFE91E63);
                fill.setAlpha(alpha);
                c.drawCircle(0, -hr * 0.35f, hr * 0.14f, fill);
                break;
            default: // wizard hat
                fill.setShader(new LinearGradient(0, -hr * 2.4f, 0, 0, 0xFF5C6BC0, 0xFF283593, Shader.TileMode.CLAMP));
                fill.setAlpha(alpha);
                path.reset();
                path.moveTo(-hr * 1.1f, 0);
                path.lineTo(-hr * 0.2f, -hr * 2.4f);
                path.lineTo(hr * 1.1f, 0);
                path.close();
                c.drawPath(path, fill);
                fill.setShader(null);
                fill.setColor(0xFFFFEB3B);
                fill.setAlpha(alpha);
                c.drawCircle(0, -hr * 0.9f, hr * 0.15f, fill);
                break;
        }
        fill.setShader(null);
        c.restore();
    }

    private static int blend(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        return Color.argb(255,
                (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }
}
