package com.bubblant.game.view;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;

import com.bubblant.game.engine.AntBrain;
import com.bubblant.game.engine.GameWorld;
import com.bubblant.game.engine.Level;
import com.bubblant.game.engine.WorldTheme;

import java.util.Random;

/** Draws a {@link GameWorld}: textured real-life scenes, a glossy 3D bubble and the ant riding on top. */
public final class GameRenderer {
    public static final class Labels {
        public String level = "", world = "", tip = "", story = "", ready = "", friends = "", air = "AIR";
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final Matrix mtx = new Matrix();
    private final AntPainter antPainter = new AntPainter();

    private WorldTheme theme = WorldTheme.of(0);
    private BitmapShader floorShader, wallShader, caustic1, caustic2;
    private Shader waterShader, vignette, bubbleShader, rimShader, hiliteShader, glowShader, sugarGlow;
    private int cachedW, cachedH;
    private float camX = Float.NaN, camY;
    private float facing = 1f;
    public float scale = 1;
    public float bubbleSX, bubbleSY;
    public final RectF pauseRect = new RectF();
    public int insetTop;
    public boolean showTalk = true;
    public int hat;
    public Labels labels = new Labels();
    public float touchX = -1, touchY = -1;
    private float t;
    private final float[] motes = new float[90];

    public GameRenderer() {
        s.setStyle(Paint.Style.STROKE);
        s.setStrokeCap(Paint.Cap.ROUND);
        s.setStrokeJoin(Paint.Join.ROUND);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        Random r = new Random(3);
        for (int i = 0; i < motes.length; i++) motes[i] = r.nextFloat();
        bubbleShader = new RadialGradient(-28, -32, 130,
                new int[]{0x14FFFFFF, 0x1AB3E5FC, 0x33E1F5FE, 0x88FFFFFF}, new float[]{0f, 0.55f, 0.82f, 1f}, Shader.TileMode.CLAMP);
        rimShader = new SweepGradient(0, 0, new int[]{0x66FF80AB, 0x6680D8FF, 0x66B9F6CA, 0x66FFE57F, 0x66EA80FC, 0x66FF80AB}, null);
        hiliteShader = new RadialGradient(-38, -42, 34, 0xEEFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        glowShader = new RadialGradient(0, 0, 100, 0x88FFF59D, 0x00FFF59D, Shader.TileMode.CLAMP);
        sugarGlow = new RadialGradient(0, 0, 100, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
    }

    /** Prepare textures for the level's world. Call before the first frame of a level. */
    public void setWorld(int world) {
        theme = WorldTheme.of(world);
        floorShader = Textures.floorShader(theme.floor, 300f);
        wallShader = Textures.wallShader(theme.wall, 110f);
        caustic1 = new BitmapShader(Textures.caustic(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        caustic2 = new BitmapShader(Textures.caustic(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        cachedW = cachedH = 0;
        camX = Float.NaN;
    }

    // =====================================================================================
    public void draw(Canvas c, GameWorld w, float frameDt) {
        t += frameDt;
        int W = c.getWidth(), H = c.getHeight();
        Level lv = w.level;
        float u = W / 1080f;
        if (floorShader == null) setWorld(w.spec.world);
        if (cachedW != W || cachedH != H) {
            cachedW = W;
            cachedH = H;
            waterShader = new LinearGradient(0, 0, 0, H,
                    (theme.waterTop & 0x00FFFFFF) | (theme.waterAlpha << 24),
                    (theme.waterBottom & 0x00FFFFFF) | (Math.min(255, theme.waterAlpha + 50) << 24), Shader.TileMode.CLAMP);
            vignette = new RadialGradient(W / 2f, H / 2f, (float) Math.hypot(W, H) * 0.6f,
                    new int[]{0x00000000, 0x00000000, 0x66000000}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        }

        // camera
        scale = W / (Level.CS * 3.6f);
        float viewW = W / scale, viewH = H / scale;
        float tx = w.x + w.vx * 0.25f, ty = w.y + w.vy * 0.25f;
        if (Float.isNaN(camX)) { camX = tx; camY = ty; }
        float k = Math.min(1f, frameDt * 4f);
        camX += (tx - camX) * k;
        camY += (ty - camY) * k;
        float margin = Level.CS * 0.35f;
        camX = clampCam(camX, viewW, lv.worldW, margin);
        camY = clampCam(camY, viewH, lv.worldH, margin + insetTop / scale);
        w.viewL = camX - viewW / 2; w.viewR = camX + viewW / 2;
        w.viewT = camY - viewH / 2; w.viewB = camY + viewH / 2;

        // ant faces where it wants to go
        float want = Math.abs(w.vx) > 25 ? Math.signum(w.vx) : (Math.abs(w.ant.desiredX) > 0.3f ? Math.signum(w.ant.desiredX) : facing >= 0 ? 1 : -1);
        facing += (want - facing) * Math.min(1f, frameDt * 6f);

        c.save();
        c.translate(W / 2f, H / 2f);
        c.scale(scale, scale);
        c.translate(-camX, -camY);

        // floor (texture fixed to the world)
        p.setShader(floorShader);
        c.drawRect(w.viewL - 20, w.viewT - 20, w.viewR + 20, w.viewB + 20, p);
        p.setShader(null);
        // floor outside the basin is darker
        c.restore();

        // water tint
        p.setShader(waterShader);
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);

        c.save();
        c.translate(W / 2f, H / 2f);
        c.scale(scale, scale);
        c.translate(-camX, -camY);

        drawCaustics(c, w);
        drawCurrents(c, w);
        drawVortices(c, w);
        drawRipples(c, w);
        drawGoal(c, w);
        drawPickups(c, w);
        drawFriends(c, w);
        drawWalls(c, w);
        drawSpikes(c, w);
        drawPatrols(c, w);
        drawChasers(c, w, false);
        drawWind(c, w);
        drawBubbleAndAnt(c, w);
        drawParticles(c, w);
        c.restore();

        bubbleSX = (w.x - camX) * scale + W / 2f;
        bubbleSY = (w.y - camY) * scale + H / 2f;

        if (theme.outdoor && w.spec.darkness <= 0) drawSunRays(c, W, H);
        p.setShader(vignette);
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);
        if (w.spec.darkness > 0) drawDarkness(c, w, W, H);
        if (w.spec.rain) drawRain(c, W, H, u);
        if (w.flash > 0) {
            p.setColor(Color.WHITE);
            p.setAlpha((int) (Math.min(1f, w.flash) * 200));
            c.drawRect(0, 0, W, H, p);
            p.setAlpha(255);
        }

        if (showTalk) drawSpeech(c, w, W, u);
        drawTouchStick(c, u);
        drawHud(c, w, W, H, u);
        if (w.state == GameWorld.State.READY) drawReady(c, w, W, H, u);
    }

    private static float clampCam(float v, float view, float world, float margin) {
        if (world + margin * 2 <= view) return world / 2f;
        float min = view / 2 - margin, max = world + margin - view / 2;
        return Math.max(min, Math.min(max, v));
    }

    // ===================================================================== environment
    private void drawCaustics(Canvas c, GameWorld w) {
        int a = w.spec.darkness > 0 ? 18 : theme.outdoor ? 52 : 40;
        mtx.setScale(3.2f, 3.2f);
        mtx.postTranslate(t * 9f, t * 5f);
        caustic1.setLocalMatrix(mtx);
        p.setShader(caustic1);
        p.setAlpha(a);
        c.drawRect(w.viewL - 20, w.viewT - 20, w.viewR + 20, w.viewB + 20, p);
        mtx.setScale(4.4f, 4.4f);
        mtx.postRotate(30);
        mtx.postTranslate(-t * 7f, t * 11f);
        caustic2.setLocalMatrix(mtx);
        p.setShader(caustic2);
        p.setAlpha(a * 2 / 3);
        c.drawRect(w.viewL - 20, w.viewT - 20, w.viewR + 20, w.viewB + 20, p);
        p.setShader(null);
        p.setAlpha(255);
        // floating motes (dust, plankton) give depth
        p.setColor(theme.outdoor ? 0x55FFFFF0 : 0x40FFFFFF);
        float vw = w.viewR - w.viewL, vh = w.viewB - w.viewT;
        for (int i = 0; i < motes.length; i += 3) {
            float x = w.viewL + ((motes[i] * 2000 + t * 6 * (motes[i + 2] - 0.5f)) % vw + vw) % vw;
            float y = w.viewT + ((motes[i + 1] * 3000 - t * 4) % vh + vh) % vh;
            c.drawCircle(x, y, 1.2f + motes[i + 2] * 2f, p);
        }
    }

    private void drawSunRays(Canvas c, int W, int H) {
        p.setColor(Color.WHITE);
        for (int i = 0; i < 4; i++) {
            float x = ((i * 0.31f + t * 0.012f) % 1.3f - 0.15f) * W;
            int a = (int) (14 + 8 * Math.sin(t * 0.7f + i * 2));
            p.setAlpha(a);
            path.reset();
            path.moveTo(x, 0);
            path.lineTo(x + W * 0.12f, 0);
            path.lineTo(x + W * 0.12f - H * 0.35f, H);
            path.lineTo(x - H * 0.35f - W * 0.05f, H);
            path.close();
            c.drawPath(path, p);
        }
        p.setAlpha(255);
    }

    private void drawCurrents(Canvas c, GameWorld w) {
        for (Level.Current cu : w.level.currents) {
            if (cu.r < w.viewL || cu.l > w.viewR || cu.b < w.viewT || cu.t > w.viewB) continue;
            boolean horiz = cu.fx != 0;
            float dir = Math.signum(horiz ? cu.fx : cu.fy);
            float speed = Math.abs(horiz ? cu.fx : cu.fy) * 0.45f;
            float len = horiz ? cu.r - cu.l : cu.b - cu.t;
            float across = horiz ? cu.b - cu.t : cu.r - cu.l;
            p.setColor(0x1AFFFFFF);
            c.drawRect(cu.l, cu.t, cu.r, cu.b, p);
            c.save();
            c.clipRect(cu.l, cu.t, cu.r, cu.b);
            s.setStrokeWidth(3f);
            for (int i = 0; i < 14; i++) {
                float lane = (motes[i] * 0.9f + 0.05f) * across;
                float off = ((t * speed * (0.7f + motes[i + 20] * 0.6f) + motes[i + 40] * len) % (len + 60)) - 30;
                if (dir < 0) off = len - off;
                float segLen = 26 + motes[i + 30] * 30;
                s.setColor(Color.WHITE);
                s.setAlpha(90 + (int) (motes[i + 50] * 80));
                path.reset();
                for (int j = 0; j <= 6; j++) {
                    float a = off - dir * segLen * j / 6f;
                    float wob = (float) Math.sin(a * 0.05f + i) * 4;
                    float px = horiz ? cu.l + a : cu.l + lane + wob;
                    float py = horiz ? cu.t + lane + wob : cu.t + a;
                    if (j == 0) path.moveTo(px, py); else path.lineTo(px, py);
                }
                c.drawPath(path, s);
            }
            // tiny bubbles carried along
            p.setColor(0x99FFFFFF);
            for (int i = 0; i < 6; i++) {
                float lane = motes[i + 60] * across;
                float off = ((t * speed * 0.8f + motes[i + 70] * len) % len);
                if (dir < 0) off = len - off;
                float px = horiz ? cu.l + off : cu.l + lane, py = horiz ? cu.t + lane : cu.t + off;
                c.drawCircle(px, py, 2.5f, p);
            }
            c.restore();
        }
    }

    private void drawVortices(Canvas c, GameWorld w) {
        for (Level.Whirl wh : w.level.whirls) {
            c.save();
            c.translate(wh.x, wh.y);
            float rr = wh.radius;
            c.scale(rr / 100f, rr / 100f);
            p.setColor(Color.WHITE); p.setShader(new RadialGradient(0, 0, 100, 0x88001018, 0x00001018, Shader.TileMode.CLAMP));
            c.drawCircle(0, 0, 100, p);
            p.setShader(null);
            s.setStrokeWidth(5f);
            for (int i = 0; i < 6; i++) {
                float r = 18 + i * 14;
                float start = (t * (300 - i * 35) + i * 60) % 360;
                s.setColor(Color.WHITE);
                s.setAlpha(150 - i * 18);
                rect.set(-r, -r, r, r);
                c.drawArc(rect, start, 220, false, s);
            }
            if (theme.vortex == WorldTheme.VORTEX_DRAIN) {
                // metal drain grate
                p.setColor(Color.WHITE); p.setShader(new RadialGradient(-6, -6, 30, 0xFFE0E6EA, 0xFF7B8790, Shader.TileMode.CLAMP));
                c.drawCircle(0, 0, 24, p);
                p.setShader(null);
                p.setColor(0xFF1E2428);
                for (int i = 0; i < 8; i++) {
                    c.save();
                    c.rotate(i * 45 + t * 20);
                    c.drawRoundRect(-2.5f, -19, 2.5f, -7, 2, 2, p);
                    c.restore();
                }
                c.drawCircle(0, 0, 4, p);
            } else {
                p.setColor(0xFF000810);
                c.drawCircle(0, 0, 16, p);
            }
            c.restore();
        }
    }

    private void drawRipples(Canvas c, GameWorld w) {
        s.setStrokeWidth(2.5f);
        for (GameWorld.Ripple r : w.ripples) {
            s.setColor(Color.WHITE);
            s.setAlpha((int) (140 * (1 - r.age / 1.2f)));
            c.drawCircle(r.x, r.y, 6 + r.age * 60, s);
            if (r.age > 0.25f) c.drawCircle(r.x, r.y, (r.age - 0.25f) * 50, s);
        }
        s.setAlpha(255);
    }

    // ===================================================================== goal
    private void drawGoal(Canvas c, GameWorld w) {
        Level lv = w.level;
        float gx = lv.goalX, gy = lv.goalY;
        boolean open = w.riders >= lv.friends.size();
        float pulse = 1 + 0.08f * (float) Math.sin(t * 4);
        c.save();
        c.translate(gx, gy);
        c.save();
        c.scale(pulse, pulse);
        p.setShader(glowShader);
        p.setAlpha(open ? 255 : 110);
        c.drawCircle(0, 0, 100, p);
        p.setShader(null);
        p.setAlpha(255);
        c.restore();
        // shadow
        p.setColor(0x44000000);
        c.drawOval(-60, -40, 68, 58, p);
        switch (theme.goal) {
            case WorldTheme.GOAL_SPONGE: {
                rect.set(-58, -38, 58, 38);
                p.setColor(Color.WHITE); p.setShader(new LinearGradient(0, -38, 0, 38, 0xFFFFE082, 0xFFE0A800, Shader.TileMode.CLAMP));
                c.drawRoundRect(rect, 14, 14, p);
                p.setShader(null);
                rect.set(-58, -38, 58, -14);
                p.setColor(0xFF2E7D32);
                c.drawRoundRect(rect, 12, 12, p);
                p.setColor(0x66B8922A);
                for (int i = 0; i < 14; i++) c.drawCircle(-48 + (i * 37 % 96), -4 + (i * 13 % 36), 3 + i % 3, p);
                break;
            }
            case WorldTheme.GOAL_SOAP: {
                p.setColor(Color.WHITE); p.setShader(new RadialGradient(-10, -10, 80, 0xFFFFFFFF, 0xFFB0C4CE, Shader.TileMode.CLAMP));
                c.drawOval(-64, -44, 64, 44, p);
                p.setShader(null);
                rect.set(-36, -22, 36, 22);
                p.setColor(Color.WHITE); p.setShader(new LinearGradient(0, -22, 0, 22, 0xFFF8BBD0, 0xFFEC407A, Shader.TileMode.CLAMP));
                c.drawRoundRect(rect, 14, 14, p);
                p.setShader(null);
                p.setColor(0x88FFFFFF);
                for (int i = 0; i < 5; i++) c.drawCircle(-20 + i * 12, -26 - (i % 2) * 8, 6 + (i % 3) * 2, p);
                break;
            }
            case WorldTheme.GOAL_SHELL: {
                p.setColor(Color.WHITE); p.setShader(new RadialGradient(0, 30, 90, 0xFFFFE0D6, 0xFFE57373, Shader.TileMode.CLAMP));
                path.reset();
                path.moveTo(0, 40);
                path.cubicTo(-90, -10, -40, -60, 0, -55);
                path.cubicTo(40, -60, 90, -10, 0, 40);
                c.drawPath(path, p);
                p.setShader(null);
                s.setColor(0x66B71C1C);
                s.setStrokeWidth(3);
                for (int i = -3; i <= 3; i++) c.drawLine(0, 38, i * 17, -48 + Math.abs(i) * 6, s);
                break;
            }
            case WorldTheme.GOAL_RING: {
                p.setColor(Color.WHITE); p.setShader(new RadialGradient(-12, -12, 90, 0xFFFF8A80, 0xFFC62828, Shader.TileMode.CLAMP));
                c.drawCircle(0, 0, 60, p);
                p.setShader(null);
                p.setColor(Color.WHITE);
                for (int i = 0; i < 4; i++) {
                    rect.set(-60, -60, 60, 60);
                    c.drawArc(rect, i * 90 + 20, 30, true, p);
                }
                p.setShader(floorShader);
                c.drawCircle(0, 0, 28, p);
                p.setShader(null);
                p.setColor((theme.waterTop & 0x00FFFFFF) | 0x99000000);
                c.drawCircle(0, 0, 28, p);
                break;
            }
            case WorldTheme.GOAL_COIN_ROCK:
            case WorldTheme.GOAL_ROCK: {
                p.setColor(Color.WHITE); p.setShader(new RadialGradient(-14, -18, 80, 0xFFA8A29A, 0xFF4E4844, Shader.TileMode.CLAMP));
                path.reset();
                path.moveTo(-58, 10);
                path.cubicTo(-62, -30, -20, -52, 10, -46);
                path.cubicTo(50, -40, 64, -6, 56, 20);
                path.cubicTo(44, 46, -40, 48, -58, 10);
                c.drawPath(path, p);
                p.setShader(null);
                if (theme.goal == WorldTheme.GOAL_COIN_ROCK) {
                    for (int i = 0; i < 4; i++) {
                        float cx2 = -28 + i * 18, cy2 = -10 + (i % 2) * 14;
                        p.setColor(Color.WHITE); p.setShader(new RadialGradient(cx2 - 3, cy2 - 3, 12, 0xFFFFF59D, 0xFFC79100, Shader.TileMode.CLAMP));
                        c.drawCircle(cx2, cy2, 9, p);
                    }
                    p.setShader(null);
                } else {
                    p.setColor(0xFFEFEBE9);
                    for (int i = 0; i < 7; i++) c.drawCircle(-34 + i * 11, 14 - (i % 3) * 9, 4, p);
                }
                break;
            }
            case WorldTheme.GOAL_STRAW:
            case WorldTheme.GOAL_LOG: {
                boolean log = theme.goal == WorldTheme.GOAL_LOG;
                rect.set(-72, -26, 72, 26);
                p.setColor(Color.WHITE); p.setShader(new LinearGradient(0, -26, 0, 26, log ? 0xFFA1887F : 0xFFF0D78C, log ? 0xFF4E342E : 0xFFB8963E, Shader.TileMode.CLAMP));
                c.drawRoundRect(rect, 26, 26, p);
                p.setShader(null);
                s.setColor(log ? 0x664E342E : 0x88A07C2C);
                s.setStrokeWidth(2.5f);
                for (int i = -2; i <= 2; i++) c.drawLine(-62, i * 9, 62, i * 9 + (log ? 3 : 0), s);
                if (log) {
                    p.setColor(0xFFD7B899);
                    c.drawOval(56, -22, 76, 22, p);
                }
                break;
            }
            case WorldTheme.GOAL_PIPE:
            case WorldTheme.GOAL_STAIRS: {
                p.setColor(Color.WHITE); p.setShader(new RadialGradient(0, 0, 70, 0xFFFFF8E1, 0x00FFF8E1, Shader.TileMode.CLAMP));
                c.drawCircle(0, 0, 70, p);
                p.setShader(null);
                if (theme.goal == WorldTheme.GOAL_PIPE) {
                    s.setColor(0xFF90A4AE);
                    s.setStrokeWidth(12);
                    c.drawCircle(0, 0, 46, s);
                    s.setColor(0xFFB0BEC5);
                    s.setStrokeWidth(5);
                    c.drawLine(-18, -40, -18, 40, s);
                    c.drawLine(18, -40, 18, 40, s);
                    for (int i = -3; i <= 3; i++) c.drawLine(-18, i * 11, 18, i * 11, s);
                } else {
                    for (int i = 0; i < 5; i++) {
                        p.setColor(Color.rgb(120 + i * 20, 110 + i * 20, 100 + i * 20));
                        c.drawRect(-50, 30 - i * 16, 50, 46 - i * 16, p);
                    }
                }
                break;
            }
            case WorldTheme.GOAL_LILY: {
                drawLilyPad(c, 62);
                p.setColor(0xFFF8BBD0);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3 + t * 0.3;
                    c.drawCircle(-20 + (float) Math.cos(a) * 12, -18 + (float) Math.sin(a) * 12, 10, p);
                }
                p.setColor(0xFFFFEB3B);
                c.drawCircle(-20, -18, 8, p);
                break;
            }
            default: // big leaf
                drawLeaf(c, 78, 0xFF7CB342, 0xFF33691E);
                break;
        }
        // little flag on top so the goal is easy to recognise
        s.setColor(0xFF5D4037);
        s.setStrokeWidth(4);
        c.drawLine(30, 10, 30, -70, s);
        p.setColor(open ? 0xFFFF5252 : 0xFF9E9E9E);
        path.reset();
        float wave = (float) Math.sin(t * 6) * 5;
        path.moveTo(30, -70);
        path.quadTo(48, -64 + wave, 64, -60);
        path.lineTo(30, -46);
        path.close();
        c.drawPath(path, p);
        c.restore();
        if (!open) {
            tp.setColor(Color.WHITE);
            tp.setTextSize(30);
            c.drawText(w.riders + "/" + lv.friends.size(), gx, gy + 92, tp);
        }
    }

    private void drawLilyPad(Canvas c, float r) {
        p.setColor(Color.WHITE); p.setShader(new RadialGradient(-r * 0.2f, -r * 0.2f, r * 1.2f, 0xFF7CC576, 0xFF2E7D32, Shader.TileMode.CLAMP));
        rect.set(-r, -r, r, r);
        c.drawArc(rect, 20, 320, true, p);
        p.setShader(null);
        s.setColor(0x6633691E);
        s.setStrokeWidth(2.5f);
        for (int i = 0; i < 7; i++) {
            double a = Math.toRadians(40 + i * 45);
            c.drawLine(0, 0, (float) Math.cos(a) * r * 0.9f, (float) Math.sin(a) * r * 0.9f, s);
        }
    }

    private void drawLeaf(Canvas c, float len, int light, int dark) {
        path.reset();
        path.moveTo(-len, 0);
        path.cubicTo(-len * 0.4f, -len * 0.55f, len * 0.5f, -len * 0.5f, len, 0);
        path.cubicTo(len * 0.5f, len * 0.5f, -len * 0.4f, len * 0.55f, -len, 0);
        p.setColor(Color.WHITE); p.setShader(new LinearGradient(0, -len * 0.5f, 0, len * 0.5f, light, dark, Shader.TileMode.CLAMP));
        c.drawPath(path, p);
        p.setShader(null);
        s.setColor(0x88FFFFFF & dark | 0x55000000);
        s.setStrokeWidth(3);
        c.drawLine(-len * 0.95f, 0, len * 0.95f, 0, s);
        s.setStrokeWidth(1.8f);
        for (int i = -3; i <= 3; i++) {
            float x = i * len * 0.22f;
            c.drawLine(x, 0, x + len * 0.18f, -len * 0.3f, s);
            c.drawLine(x, 0, x + len * 0.18f, len * 0.3f, s);
        }
    }

    // ===================================================================== pickups & friends
    private void drawPickups(Canvas c, GameWorld w) {
        for (Level.Pickup pk : w.level.pickups) {
            if (pk.taken) continue;
            if (pk.x < w.viewL - 50 || pk.x > w.viewR + 50 || pk.y < w.viewT - 50 || pk.y > w.viewB + 50) continue;
            float bob = (float) Math.sin(t * 3 + pk.phase) * 5;
            c.save();
            c.translate(pk.x, pk.y + bob);
            if (pk.type == Level.SUGAR) {
                c.save();
                c.scale(0.45f, 0.45f);
                p.setShader(sugarGlow);
                c.drawCircle(0, 0, 100, p);
                p.setShader(null);
                c.restore();
                c.rotate((float) Math.sin(t * 2 + pk.phase) * 10);
                p.setColor(0x33000000);
                c.drawRect(-13, -7, 19, 25, p);
                // isometric sugar cube
                path.reset(); path.moveTo(0, -18); path.lineTo(17, -9); path.lineTo(0, 0); path.lineTo(-17, -9); path.close();
                p.setColor(0xFFFFFFFF); c.drawPath(path, p);
                path.reset(); path.moveTo(-17, -9); path.lineTo(0, 0); path.lineTo(0, 19); path.lineTo(-17, 10); path.close();
                p.setColor(0xFFE3E8EC); c.drawPath(path, p);
                path.reset(); path.moveTo(17, -9); path.lineTo(0, 0); path.lineTo(0, 19); path.lineTo(17, 10); path.close();
                p.setColor(0xFFC9D1D8); c.drawPath(path, p);
                p.setColor(0x99B0BEC5);
                c.drawCircle(-8, 4, 1.6f, p); c.drawCircle(7, 8, 1.6f, p); c.drawCircle(2, -9, 1.4f, p);
                float sp = (float) Math.max(0, Math.sin(t * 3 + pk.phase * 2));
                p.setColor(Color.WHITE);
                p.setAlpha((int) (sp * 255));
                c.drawCircle(10, -16, 2.5f, p);
                p.setAlpha(255);
            } else {
                float rr = 19 + (float) Math.sin(t * 5 + pk.phase) * 2;
                drawSphere(c, rr, 0.9f);
                tp.setColor(0xCC01579B);
                tp.setTextSize(15);
                c.drawText("O₂", 0, 6, tp);
            }
            c.restore();
        }
    }

    private void drawFriends(Canvas c, GameWorld w) {
        for (Level.Friend f : w.level.friends) {
            if (f.rescued) continue;
            float bob = (float) Math.sin(t * 2 + f.look) * 4;
            c.save();
            c.translate(f.x, f.y + bob);
            p.setColor(0x33000000);
            c.drawOval(-46, -18, 54, 32, p);
            c.save();
            c.rotate(-12 + f.look * 25);
            drawLeaf(c, 48, 0xFF9CCC65, 0xFF558B2F);
            c.restore();
            boolean waving = Math.sin(t * 3 + f.look) > 0.5;
            antPainter.draw(c, 0, 4, 24, null, t + f.look, 0, f.look + 1, 255, waving, f.look % 2 == 0 ? 1 : -1);
            c.restore();
            if (Math.sin(t * 1.3 + f.look) > 0.2) {
                tp.setTextSize(22);
                tp.setColor(Color.WHITE);
                p.setColor(0xCC1A237E);
                float tw = tp.measureText(labels.friends) + 24;
                rect.set(f.x - tw / 2, f.y - 92, f.x + tw / 2, f.y - 62);
                c.drawRoundRect(rect, 14, 14, p);
                c.drawText(labels.friends, f.x, f.y - 70, tp);
            }
        }
    }

    // ===================================================================== walls
    private void drawWalls(Canvas c, GameWorld w) {
        float vl = w.viewL - 40, vr = w.viewR + 40, vt = w.viewT - 40, vb = w.viewB + 40;
        boolean log = theme.wall == WorldTheme.WALL_WOOD;
        float rad = log ? 11 : theme.wall == WorldTheme.WALL_TILE || theme.wall == WorldTheme.WALL_CONCRETE ? 4 : 9;
        // shadows first (so they never cover a neighbouring wall)
        p.setColor(0x55000000);
        for (Level.Wall wl : w.level.walls) {
            if (wl.r < vl || wl.l > vr || wl.b < vt || wl.t > vb) continue;
            rect.set(wl.l + 5, wl.t + 9, wl.r + 7, wl.b + 11);
            c.drawRoundRect(rect, rad + 2, rad + 2, p);
        }
        for (Level.Wall wl : w.level.walls) {
            if (wl.r < vl || wl.l > vr || wl.b < vt || wl.t > vb) continue;
            rect.set(wl.l, wl.t, wl.r, wl.b);
            p.setShader(wallShader);
            c.drawRoundRect(rect, rad, rad, p);
            p.setShader(null);
            boolean horiz = wl.r - wl.l > wl.b - wl.t;
            // bevel: lit top/left edge, shaded bottom/right edge
            p.setColor(theme.wallLight);
            p.setAlpha(120);
            if (horiz) c.drawRoundRect(wl.l + 3, wl.t + 2, wl.r - 3, wl.t + 6, 3, 3, p);
            else c.drawRoundRect(wl.l + 2, wl.t + 3, wl.l + 6, wl.b - 3, 3, 3, p);
            p.setColor(theme.wallDark);
            p.setAlpha(140);
            if (horiz) c.drawRoundRect(wl.l + 3, wl.b - 6, wl.r - 3, wl.b - 2, 3, 3, p);
            else c.drawRoundRect(wl.r - 6, wl.t + 3, wl.r - 2, wl.b - 3, 3, 3, p);
            p.setAlpha(255);
            // water line foam where the wall meets the water
            s.setColor(0x55FFFFFF);
            s.setStrokeWidth(2);
            float wav = (float) Math.sin(t * 2 + wl.seed) * 1.5f;
            if (horiz) c.drawLine(wl.l + 6, wl.b + 3 + wav, wl.r - 6, wl.b + 3 - wav, s);
            else c.drawLine(wl.r + 3 + wav, wl.t + 6, wl.r + 3 - wav, wl.b - 6, s);
        }
    }

    // ===================================================================== hazards
    private void drawSpikes(Canvas c, GameWorld w) {
        for (Level.Urchin un : w.level.urchins) {
            if (un.x < w.viewL - 60 || un.x > w.viewR + 60 || un.y < w.viewT - 60 || un.y > w.viewB + 60) continue;
            c.save();
            c.translate(un.x, un.y);
            float r = un.r;
            p.setColor(0x44000000);
            c.drawCircle(4, 6, r + 4, p);
            switch (theme.spike) {
                case WorldTheme.SPIKE_PIN: { // thumbtack seen from above
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-r * 0.35f, -r * 0.35f, r * 1.4f, 0xFFFF8A80, 0xFFC62828, Shader.TileMode.CLAMP));
                    c.drawCircle(0, 0, r, p);
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-3, -3, r * 0.7f, 0xFFFFFFFF, 0xFF9E9E9E, Shader.TileMode.CLAMP));
                    c.drawCircle(0, 0, r * 0.45f, p);
                    p.setShader(null);
                    s.setColor(0xFFCFD8DC);
                    s.setStrokeWidth(3);
                    c.drawLine(r * 0.5f, r * 0.5f, r * 1.35f, r * 1.35f, s);
                    break;
                }
                case WorldTheme.SPIKE_NAIL: {
                    for (int i = 0; i < 3; i++) {
                        c.save();
                        c.rotate(i * 120 + 20);
                        s.setColor(0xFF8D6E63);
                        s.setStrokeWidth(5);
                        c.drawLine(0, 0, 0, -r * 1.4f, s);
                        p.setColor(0xFFBCAAA4);
                        c.drawCircle(0, -r * 1.4f, 5, p);
                        c.restore();
                    }
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-3, -3, r, 0xFFB0BEC5, 0xFF546E7A, Shader.TileMode.CLAMP));
                    c.drawCircle(0, 0, r * 0.55f, p);
                    p.setShader(null);
                    break;
                }
                case WorldTheme.SPIKE_THORN: {
                    s.setColor(0xFF5D4037);
                    s.setStrokeWidth(6);
                    c.drawLine(-r * 1.3f, r * 0.4f, r * 1.3f, -r * 0.4f, s);
                    p.setColor(0xFF795548);
                    for (int i = -2; i <= 2; i++) {
                        float bx = i * r * 0.5f, by = -i * r * 0.16f;
                        int side = i % 2 == 0 ? 1 : -1;
                        path.reset();
                        path.moveTo(bx - 4, by);
                        path.lineTo(bx + 4, by);
                        path.lineTo(bx + 2, by + side * r * 0.75f);
                        path.close();
                        c.drawPath(path, p);
                    }
                    p.setColor(0xFF7CB342);
                    c.drawOval(-r * 0.4f, -r * 0.9f, r * 0.5f, -r * 0.4f, p);
                    break;
                }
                case WorldTheme.SPIKE_BARNACLE: {
                    for (int i = 0; i < 5; i++) {
                        float a = i * 72 + 15;
                        float bx = (float) Math.cos(Math.toRadians(a)) * r * 0.6f, by = (float) Math.sin(Math.toRadians(a)) * r * 0.6f;
                        p.setColor(Color.WHITE); p.setShader(new RadialGradient(bx - 2, by - 2, 12, 0xFFF5F5F5, 0xFF78909C, Shader.TileMode.CLAMP));
                        c.drawCircle(bx, by, 9, p);
                        p.setShader(null);
                        p.setColor(0xFF263238);
                        c.drawCircle(bx, by, 3, p);
                    }
                    break;
                }
                default: { // sea urchin
                    s.setStrokeWidth(3);
                    float wob = (float) Math.sin(t * 3 + un.x) * 0.08f;
                    for (int i = 0; i < 22; i++) {
                        double a = i * Math.PI * 2 / 22 + wob;
                        float l = r + 10 + (i % 3) * 4;
                        s.setColor(i % 2 == 0 ? 0xFF311B92 : 0xFF4A148C);
                        c.drawLine(0, 0, (float) Math.cos(a) * l, (float) Math.sin(a) * l, s);
                    }
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-r * 0.3f, -r * 0.3f, r * 1.2f, 0xFFB388FF, 0xFF311B92, Shader.TileMode.CLAMP));
                    c.drawCircle(0, 0, r, p);
                    p.setShader(null);
                    break;
                }
            }
            c.restore();
        }
    }

    private void drawPatrols(Canvas c, GameWorld w) {
        for (Level.Fish f : w.level.fish) {
            if (f.x < w.viewL - 60 || f.x > w.viewR + 60 || f.y < w.viewT - 60 || f.y > w.viewB + 60) continue;
            float dx = (f.bx - f.ax) * f.dir, dy = (f.by - f.ay) * f.dir;
            float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
            float r = f.r;
            c.save();
            c.translate(f.x, f.y);
            p.setColor(0x33000000);
            c.drawOval(-r * 1.3f + 5, -r * 0.8f + 8, r * 1.3f + 5, r * 0.8f + 8, p);
            switch (theme.patrol) {
                case WorldTheme.PATROL_DUCK: { // rubber duck, bobbing, seen from above
                    c.rotate(ang + (float) Math.sin(t * 3 + f.phase) * 6);
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-6, -6, r * 1.8f, 0xFFFFF59D, 0xFFFBC02D, Shader.TileMode.CLAMP));
                    c.drawOval(-r * 1.3f, -r * 0.95f, r * 0.9f, r * 0.95f, p);
                    c.drawCircle(r * 0.8f, 0, r * 0.62f, p);
                    p.setShader(null);
                    p.setColor(0xFFFF6F00);
                    c.drawOval(r * 1.2f, -r * 0.22f, r * 1.75f, r * 0.22f, p);
                    p.setColor(Color.BLACK);
                    c.drawCircle(r * 0.95f, -r * 0.3f, 2.5f, p);
                    c.drawCircle(r * 0.95f, r * 0.3f, 2.5f, p);
                    p.setColor(0x55FFFFFF);
                    c.drawOval(-r * 0.8f, -r * 0.7f, 0, -r * 0.3f, p);
                    break;
                }
                case WorldTheme.PATROL_BALL: {
                    c.rotate(t * 90 * f.dir);
                    int[] cols = {0xFFE53935, 0xFFFFFFFF, 0xFF1E88E5, 0xFFFFFFFF, 0xFFFDD835, 0xFFFFFFFF};
                    rect.set(-r * 1.2f, -r * 1.2f, r * 1.2f, r * 1.2f);
                    for (int i = 0; i < 6; i++) { p.setColor(cols[i]); c.drawArc(rect, i * 60, 60, true, p); }
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-r * 0.4f, -r * 0.4f, r * 1.6f, 0x66FFFFFF, 0x33000000, Shader.TileMode.CLAMP));
                    c.drawCircle(0, 0, r * 1.2f, p);
                    p.setShader(null);
                    break;
                }
                case WorldTheme.PATROL_CAN: {
                    c.rotate(ang + t * 20);
                    rect.set(-r * 1.4f, -r * 0.7f, r * 1.4f, r * 0.7f);
                    p.setColor(Color.WHITE); p.setShader(new LinearGradient(0, -r * 0.7f, 0, r * 0.7f, new int[]{0xFF9E9E9E, 0xFFE53935, 0xFF8E1C1A}, null, Shader.TileMode.CLAMP));
                    c.drawRoundRect(rect, 6, 6, p);
                    p.setShader(null);
                    p.setColor(0xFFCFD8DC);
                    c.drawOval(r * 1.15f, -r * 0.7f, r * 1.55f, r * 0.7f, p);
                    break;
                }
                case WorldTheme.PATROL_BEETLE: {
                    c.rotate(ang);
                    s.setColor(0xFF3E2723);
                    s.setStrokeWidth(3);
                    float oar = (float) Math.sin(t * 12 + f.phase) * 8;
                    c.drawLine(-2, -r * 0.6f, -r * 0.6f - oar, -r * 1.5f, s);
                    c.drawLine(-2, r * 0.6f, -r * 0.6f - oar, r * 1.5f, s);
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-4, -6, r * 1.6f, 0xFF6D8B74, 0xFF1B2A20, Shader.TileMode.CLAMP));
                    c.drawOval(-r * 1.2f, -r * 0.75f, r * 1.1f, r * 0.75f, p);
                    p.setShader(null);
                    s.setColor(0x66000000);
                    s.setStrokeWidth(1.5f);
                    c.drawLine(-r * 1.1f, 0, r * 0.6f, 0, s);
                    p.setColor(0xFF1B2A20);
                    c.drawCircle(r * 1.1f, 0, r * 0.35f, p);
                    break;
                }
                case WorldTheme.PATROL_SHRIMP: {
                    c.rotate(ang);
                    p.setColor(0xAAFFAB91);
                    for (int i = 0; i < 5; i++) c.drawCircle(-i * r * 0.38f, (float) Math.sin(t * 6 + i) * 2, r * (0.55f - i * 0.07f), p);
                    s.setColor(0xAAFF7043);
                    s.setStrokeWidth(1.5f);
                    c.drawLine(r * 0.3f, -3, r * 1.6f, -r * 0.7f, s);
                    c.drawLine(r * 0.3f, 3, r * 1.6f, r * 0.7f, s);
                    p.setColor(Color.BLACK);
                    c.drawCircle(r * 0.35f, -r * 0.25f, 2, p);
                    break;
                }
                case WorldTheme.PATROL_TADPOLE: {
                    c.rotate(ang);
                    s.setColor(0xFF263238);
                    s.setStrokeWidth(4);
                    path.reset();
                    path.moveTo(-r * 0.4f, 0);
                    for (int i = 1; i <= 6; i++) path.lineTo(-r * 0.4f - i * r * 0.32f, (float) Math.sin(t * 14 + f.phase - i * 0.9f) * i * 1.4f);
                    c.drawPath(path, s);
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-2, -4, r, 0xFF546E7A, 0xFF1C2529, Shader.TileMode.CLAMP));
                    c.drawOval(-r * 0.7f, -r * 0.6f, r * 0.8f, r * 0.6f, p);
                    p.setShader(null);
                    p.setColor(0xFFFFF59D);
                    c.drawCircle(r * 0.4f, -r * 0.3f, 2.2f, p);
                    c.drawCircle(r * 0.4f, r * 0.3f, 2.2f, p);
                    break;
                }
                default: { // fish: goldfish / koi / piranha
                    c.rotate(ang);
                    int bodyA, bodyB;
                    if (theme.patrol == WorldTheme.PATROL_PIRANHA) { bodyA = 0xFFB0BEC5; bodyB = 0xFFC62828; }
                    else if (theme.patrol == WorldTheme.PATROL_KOI) { bodyA = 0xFFFFFFFF; bodyB = 0xFFFF7043; }
                    else { bodyA = 0xFFFFCC80; bodyB = 0xFFF57C00; }
                    float tail = (float) Math.sin(f.phase) * 8;
                    p.setColor(bodyB);
                    p.setAlpha(200);
                    path.reset();
                    path.moveTo(-r * 0.9f, 0);
                    path.quadTo(-r * 1.6f, -r * 0.2f + tail, -r * 2.2f, -r * 0.9f + tail);
                    path.quadTo(-r * 1.8f, tail * 0.5f, -r * 2.2f, r * 0.9f + tail);
                    path.quadTo(-r * 1.6f, r * 0.2f + tail, -r * 0.9f, 0);
                    c.drawPath(path, p);
                    p.setAlpha(255);
                    p.setColor(Color.WHITE); p.setShader(new LinearGradient(0, -r * 0.85f, 0, r * 0.85f, bodyA, bodyB, Shader.TileMode.CLAMP));
                    c.drawOval(-r * 1.3f, -r * 0.82f, r * 1.35f, r * 0.82f, p);
                    p.setShader(null);
                    if (theme.patrol == WorldTheme.PATROL_KOI) {
                        p.setColor(0xFFE64A19);
                        c.drawCircle(-r * 0.2f, -r * 0.2f, r * 0.35f, p);
                        c.drawCircle(r * 0.5f, r * 0.25f, r * 0.25f, p);
                    }
                    // scales hint
                    s.setColor(0x33000000);
                    s.setStrokeWidth(1.2f);
                    for (int i = 0; i < 3; i++) {
                        rect.set(-r * 0.6f + i * r * 0.4f - 6, -6, -r * 0.6f + i * r * 0.4f + 6, 6);
                        c.drawArc(rect, 270, 180, false, s);
                    }
                    p.setColor(Color.WHITE);
                    c.drawCircle(r * 0.75f, -r * 0.22f, r * 0.24f, p);
                    p.setColor(Color.BLACK);
                    c.drawCircle(r * 0.82f, -r * 0.22f, r * 0.12f, p);
                    if (theme.patrol == WorldTheme.PATROL_PIRANHA) {
                        p.setColor(Color.WHITE);
                        for (int i = 0; i < 3; i++) {
                            float bx = r * (0.85f + i * 0.15f);
                            path.reset(); path.moveTo(bx, r * 0.25f); path.lineTo(bx + 4, r * 0.5f); path.lineTo(bx + 8, r * 0.25f); path.close();
                            c.drawPath(path, p);
                        }
                    }
                    break;
                }
            }
            c.restore();
        }
    }

    private void drawChasers(Canvas c, GameWorld w, boolean eyesOnly) {
        for (Level.Eel e : w.level.eels) {
            float sp = (float) Math.hypot(e.vx, e.vy);
            float dx = sp > 1 ? e.vx / sp : 1, dy = sp > 1 ? e.vy / sp : 0;
            float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
            int eyeColor = e.hunting ? 0xFFFF1744 : 0xFFFFEB3B;
            if (eyesOnly) {
                p.setColor(eyeColor);
                float ex = -dy * 6, ey = dx * 6;
                c.drawCircle(e.x + dx * 8 + ex, e.y + dy * 8 + ey, 4.5f, p);
                c.drawCircle(e.x + dx * 8 - ex, e.y + dy * 8 - ey, 4.5f, p);
                continue;
            }
            if (e.x < w.viewL - 120 || e.x > w.viewR + 120 || e.y < w.viewT - 120 || e.y > w.viewB + 120) continue;
            if (theme.chaser == WorldTheme.CHASE_EEL) {
                for (int i = 10; i >= 0; i--) {
                    float back = i * 10f;
                    float side = (float) Math.sin(e.phase - i * 0.7f) * (3 + i * 1.5f);
                    float bx = e.x - dx * back - dy * side, by = e.y - dy * back + dx * side;
                    p.setColor(i % 2 == 0 ? 0xFF1B3A1B : 0xFF0F240F);
                    c.drawCircle(bx, by, e.r * (1 - i * 0.06f), p);
                }
                p.setColor(eyeColor);
                float ex = -dy * 6, ey = dx * 6;
                c.drawCircle(e.x + dx * 6 + ex, e.y + dy * 6 + ey, 4.5f, p);
                c.drawCircle(e.x + dx * 6 - ex, e.y + dy * 6 - ey, 4.5f, p);
                continue;
            }
            c.save();
            c.translate(e.x, e.y);
            c.rotate(ang);
            float r = e.r * 1.6f;
            p.setColor(0x44000000);
            c.drawOval(-r + 5, -r * 0.8f + 8, r + 5, r * 0.8f + 8, p);
            float kick = (float) Math.sin(e.phase * 1.5f) * 0.25f;
            switch (theme.chaser) {
                case WorldTheme.CHASE_FROG: {
                    p.setColor(0xFF33691E);
                    // legs
                    for (int sd = -1; sd <= 1; sd += 2) {
                        path.reset();
                        path.moveTo(-r * 0.4f, sd * r * 0.4f);
                        path.lineTo(-r * (1.1f + kick), sd * r * (0.9f + kick));
                        path.lineTo(-r * (1.5f + kick), sd * r * (0.5f));
                        path.lineTo(-r * 0.5f, sd * r * 0.2f);
                        path.close();
                        c.drawPath(path, p);
                        c.drawOval(r * 0.3f, sd * r * 0.55f - r * 0.15f, r * 0.75f, sd * r * 0.55f + r * 0.15f, p);
                    }
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-r * 0.2f, -r * 0.3f, r * 1.4f, 0xFF9CCC65, 0xFF33691E, Shader.TileMode.CLAMP));
                    c.drawOval(-r * 0.8f, -r * 0.7f, r * 0.9f, r * 0.7f, p);
                    p.setShader(null);
                    p.setColor(0xFF558B2F);
                    c.drawCircle(-r * 0.2f, r * 0.2f, r * 0.12f, p);
                    c.drawCircle(r * 0.1f, -r * 0.25f, r * 0.1f, p);
                    for (int sd = -1; sd <= 1; sd += 2) {
                        p.setColor(0xFF8BC34A);
                        c.drawCircle(r * 0.55f, sd * r * 0.42f, r * 0.24f, p);
                        p.setColor(eyeColor);
                        c.drawCircle(r * 0.6f, sd * r * 0.42f, r * 0.15f, p);
                        p.setColor(Color.BLACK);
                        c.drawOval(r * 0.56f, sd * r * 0.42f - r * 0.12f, r * 0.66f, sd * r * 0.42f + r * 0.12f, p);
                    }
                    break;
                }
                case WorldTheme.CHASE_CRAB: {
                    s.setColor(0xFFB71C1C);
                    s.setStrokeWidth(4);
                    for (int sd = -1; sd <= 1; sd += 2) {
                        for (int i = 0; i < 3; i++) {
                            float lx = -r * 0.3f + i * r * 0.3f;
                            float walk = (float) Math.sin(e.phase * 2 + i + sd) * 6;
                            c.drawLine(lx, sd * r * 0.5f, lx - 8 + walk, sd * r * 1.15f, s);
                        }
                        // claw
                        c.drawLine(r * 0.4f, sd * r * 0.4f, r * 0.95f, sd * r * 0.7f, s);
                        p.setColor(0xFFE53935);
                        c.drawOval(r * 0.8f, sd * r * 0.7f - r * 0.22f, r * 1.3f, sd * r * 0.7f + r * 0.22f, p);
                    }
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-r * 0.2f, -r * 0.3f, r * 1.3f, 0xFFFF8A65, 0xFFB71C1C, Shader.TileMode.CLAMP));
                    c.drawOval(-r * 0.75f, -r * 0.65f, r * 0.75f, r * 0.65f, p);
                    p.setShader(null);
                    p.setColor(eyeColor);
                    c.drawCircle(r * 0.62f, -r * 0.2f, 4, p);
                    c.drawCircle(r * 0.62f, r * 0.2f, 4, p);
                    break;
                }
                case WorldTheme.CHASE_ROBOT: {
                    rect.set(-r, -r * 0.8f, r, r * 0.8f);
                    p.setColor(Color.WHITE); p.setShader(new LinearGradient(0, -r, 0, r, 0xFF90A4AE, 0xFF37474F, Shader.TileMode.CLAMP));
                    c.drawRoundRect(rect, 10, 10, p);
                    p.setShader(null);
                    p.setColor(0xFF263238);
                    float brush = (t * 400) % 20;
                    for (int i = 0; i < 6; i++) c.drawRect(-r + i * r * 0.35f + brush * 0.1f, r * 0.8f, -r + i * r * 0.35f + 4, r * 0.95f, p);
                    p.setColor(0xFF1565C0);
                    c.drawRoundRect(-r * 0.5f, -r * 0.4f, r * 0.3f, r * 0.4f, 6, 6, p);
                    p.setColor(eyeColor);
                    c.drawCircle(r * 0.8f, -r * 0.45f, 5, p);
                    c.drawCircle(r * 0.8f, r * 0.45f, 5, p);
                    break;
                }
                case WorldTheme.CHASE_RAT: {
                    s.setColor(0xFFE0B2B2);
                    s.setStrokeWidth(3);
                    path.reset();
                    path.moveTo(-r * 0.8f, 0);
                    for (int i = 1; i <= 6; i++) path.lineTo(-r * 0.8f - i * r * 0.3f, (float) Math.sin(e.phase - i * 0.8f) * i * 2);
                    c.drawPath(path, s);
                    p.setColor(Color.WHITE); p.setShader(new RadialGradient(-r * 0.2f, -r * 0.2f, r * 1.3f, 0xFF9E9E9E, 0xFF424242, Shader.TileMode.CLAMP));
                    c.drawOval(-r * 0.9f, -r * 0.55f, r * 0.7f, r * 0.55f, p);
                    p.setShader(null);
                    p.setColor(0xFF616161);
                    path.reset(); path.moveTo(r * 0.5f, -r * 0.35f); path.lineTo(r * 1.2f, 0); path.lineTo(r * 0.5f, r * 0.35f); path.close();
                    c.drawPath(path, p);
                    p.setColor(0xFFF8BBD0);
                    c.drawCircle(r * 0.35f, -r * 0.45f, r * 0.18f, p);
                    c.drawCircle(r * 0.35f, r * 0.45f, r * 0.18f, p);
                    p.setColor(eyeColor);
                    c.drawCircle(r * 0.75f, -r * 0.18f, 3.5f, p);
                    c.drawCircle(r * 0.75f, r * 0.18f, 3.5f, p);
                    break;
                }
                default: { // big fish
                    float tail = (float) Math.sin(e.phase) * 10;
                    p.setColor(0xFF33691E);
                    path.reset();
                    path.moveTo(-r * 0.8f, 0);
                    path.lineTo(-r * 1.6f, -r * 0.7f + tail);
                    path.lineTo(-r * 1.6f, r * 0.7f + tail);
                    path.close();
                    c.drawPath(path, p);
                    p.setColor(Color.WHITE); p.setShader(new LinearGradient(0, -r * 0.7f, 0, r * 0.7f, 0xFF7CB342, 0xFFDCE775, Shader.TileMode.CLAMP));
                    c.drawOval(-r * 1.1f, -r * 0.65f, r * 1.2f, r * 0.65f, p);
                    p.setShader(null);
                    p.setColor(eyeColor);
                    c.drawCircle(r * 0.75f, -r * 0.22f, 5, p);
                    p.setColor(Color.BLACK);
                    c.drawCircle(r * 0.78f, -r * 0.22f, 2.5f, p);
                    break;
                }
            }
            c.restore();
        }
    }

    private void drawWind(Canvas c, GameWorld w) {
        if (w.spec.wind <= 0) return;
        float mag = (float) Math.hypot(w.windX, w.windY);
        if (mag < 1) return;
        float dx = w.windX / mag, dy = w.windY / mag;
        float vw = w.viewR - w.viewL, vh = w.viewB - w.viewT;
        int n = 8 + (int) (w.gust * 12);
        for (int i = 0; i < n; i++) {
            float seedA = motes[i % motes.length], seedB = motes[(i * 7 + 3) % motes.length];
            float travel = (t * mag * 2.2f + seedA * 2000) % (vw + vh);
            float px = w.viewL + seedB * vw + dx * travel - dx * (vw + vh) / 2;
            float py = w.viewT + seedA * vh + dy * travel - dy * (vw + vh) / 2;
            px = w.viewL + ((px - w.viewL) % vw + vw) % vw;
            py = w.viewT + ((py - w.viewT) % vh + vh) % vh;
            s.setColor(Color.WHITE);
            s.setStrokeWidth(2.5f);
            s.setAlpha((int) (50 + 70 * w.gust));
            float l = 30 + mag * 0.25f;
            c.drawLine(px, py, px + dx * l, py + dy * l, s);
            if (i % 3 == 0 && theme.outdoor) { // a blown leaf
                c.save();
                c.translate(px + dx * l * 0.5f, py + dy * l * 0.5f);
                c.rotate(t * 200 + i * 40);
                c.scale(0.18f, 0.18f);
                drawLeaf(c, 50, 0xFFFFB74D, 0xFFE65100);
                c.restore();
            }
        }
        s.setAlpha(255);
    }

    // ===================================================================== bubble + ant
    /** glossy soap-bubble sphere at origin */
    private void drawSphere(Canvas c, float r, float alpha) {
        c.save();
        c.scale(r / 100f, r / 100f);
        p.setShader(bubbleShader);
        p.setAlpha((int) (alpha * 255));
        c.drawCircle(0, 0, 100, p);
        // thin-film rainbow rim
        mtx.setRotate(t * 40);
        rimShader.setLocalMatrix(mtx);
        s.setShader(rimShader);
        s.setStrokeWidth(9);
        s.setAlpha((int) (alpha * 255));
        c.drawCircle(0, 0, 95, s);
        s.setShader(null);
        s.setColor(Color.WHITE);
        s.setAlpha((int) (alpha * 200));
        s.setStrokeWidth(3);
        c.drawCircle(0, 0, 100, s);
        // window reflection + small highlight
        p.setShader(hiliteShader);
        p.setAlpha((int) (alpha * 255));
        c.drawOval(-72, -70, -10, -20, p);
        p.setShader(null);
        p.setColor(Color.WHITE);
        p.setAlpha((int) (alpha * 170));
        c.drawCircle(48, 52, 8, p);
        p.setAlpha(255);
        s.setAlpha(255);
        c.restore();
    }

    private void drawBubbleAndAnt(Canvas c, GameWorld w) {
        AntBrain ant = w.ant;
        if (w.state == GameWorld.State.POPPED) {
            float f = Math.min(1f, w.stateTime / 2f);
            float sc = 1 - 0.55f * f;
            s.setColor(Color.WHITE);
            s.setStrokeWidth(3);
            s.setAlpha((int) (200 * (1 - f)));
            c.drawCircle(w.popX, w.popY, 20 + f * 80, s);
            s.setAlpha(255);
            c.save();
            c.translate(w.popX, w.popY + f * 10);
            c.rotate(f * 120 * Math.signum(facing));
            antPainter.draw(c, 0, 0, 30 * sc, ant, t, hat, 0, (int) (255 * (1 - f * 0.6f)), true, facing);
            c.restore();
            p.setColor((theme.waterBottom & 0x00FFFFFF) | ((int) (f * 160) << 24));
            c.drawCircle(w.popX, w.popY - 15, 45, p);
            return;
        }
        float sp = (float) Math.hypot(w.vx, w.vy);
        float stretch = Math.min(0.16f, sp / 3200f) + (float) Math.sin(w.wobble * 9) * 0.02f + w.lastImpact * 0.12f;
        float ang = (float) Math.toDegrees(Math.atan2(w.vy, w.vx));
        float r = w.r;
        float bx = w.x, by = w.y;
        if (w.state == GameWorld.State.WON) by += (float) Math.sin(t * 8) * 2;

        // shadow and light ring on the floor
        p.setColor(0x33000000);
        c.drawOval(bx - r * 0.9f + r * 0.25f, by - r * 0.55f + r * 0.6f, bx + r * 0.9f + r * 0.25f, by + r * 0.55f + r * 0.6f, p);
        s.setColor(0x44FFFFFF);
        s.setStrokeWidth(2);
        c.drawOval(bx - r * 0.7f + r * 0.25f, by - r * 0.4f + r * 0.6f, bx + r * 0.7f + r * 0.25f, by + r * 0.4f + r * 0.6f, s);

        c.save();
        c.translate(bx, by);
        c.rotate(ang);
        c.scale(1 + stretch, 1 - stretch * 0.8f);
        c.rotate(-ang);
        drawSphere(c, r, 1f);
        c.restore();

        float danger = Math.max(0, sp / w.popSpeed() - 0.75f) * 4;
        if (danger > 0 && w.state == GameWorld.State.PLAYING) {
            s.setColor(0xFFFF5252);
            s.setAlpha((int) (Math.min(1f, danger) * 200));
            s.setStrokeWidth(4);
            c.drawCircle(bx, by, r + 5, s);
            s.setAlpha(255);
        }

        // the ant stands ON TOP of the bubble
        float antS = Math.max(19f, Math.min(36f, r * 0.95f));
        float feetY = by - r * 0.34f;
        // contact shadow on the bubble surface
        p.setColor(0x40000000);
        c.drawOval(bx - antS * 0.9f, feetY - antS * 0.06f, bx + antS * 0.75f, feetY + antS * 0.12f, p);
        // rescued friends ride along next to it
        for (int i = 0; i < w.riders; i++) {
            float ox = (i % 2 == 0 ? -1 : 1) * r * (0.42f + (i / 2) * 0.2f);
            antPainter.draw(c, bx + ox, feetY + r * 0.12f, antS * 0.62f, null, t + i, 0, i + 1, 255, false, ox < 0 ? 1 : -1);
        }
        antPainter.draw(c, bx, feetY, antS, ant, t, hat, 0, 255, false, facing);
    }

    private void drawParticles(Canvas c, GameWorld w) {
        for (GameWorld.Particle pt : w.particles) {
            float a = pt.life / pt.max;
            if (pt.kind == 0) {
                p.setColor(0xFFE1F5FE);
                p.setAlpha((int) (a * 255));
                c.drawCircle(pt.x, pt.y, pt.size, p);
            } else if (pt.kind == 1) {
                s.setColor(Color.WHITE);
                s.setStrokeWidth(2);
                s.setAlpha((int) (a * 220));
                c.drawCircle(pt.x, pt.y, pt.size, s);
            } else {
                p.setColor(0xFFFFF176);
                p.setAlpha((int) (a * 255));
                float sz = pt.size * (0.5f + a);
                c.drawCircle(pt.x, pt.y, sz * 0.6f, p);
                s.setColor(0xFFFFF176);
                s.setAlpha((int) (a * 255));
                s.setStrokeWidth(1.5f);
                c.drawLine(pt.x - sz, pt.y, pt.x + sz, pt.y, s);
                c.drawLine(pt.x, pt.y - sz, pt.x, pt.y + sz, s);
            }
        }
        p.setAlpha(255);
        s.setAlpha(255);
    }

    // ===================================================================== overlays
    private void drawDarkness(Canvas c, GameWorld w, int W, int H) {
        float lightR = w.spec.darkness * Level.CS * scale * 0.5f * (0.95f + 0.05f * (float) Math.sin(t * 3));
        if (w.flash > 0.2f) lightR *= 1 + w.flash * 3;
        float cx = bubbleSX, cy = bubbleSY;
        if (w.state == GameWorld.State.WON) lightR *= 1 + Math.min(3f, w.stateTime * 2);
        path.reset();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.addRect(0, 0, W, H, Path.Direction.CW);
        path.addCircle(cx, cy, lightR, Path.Direction.CW);
        p.setColor(0xF2000000);
        c.drawPath(path, p);
        p.setColor(Color.WHITE); p.setShader(new RadialGradient(cx, cy, lightR, new int[]{0x00000000, 0x00000000, 0xF2000000},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, lightR + 1, p);
        p.setShader(null);
        path.setFillType(Path.FillType.WINDING);

        c.save();
        c.translate(W / 2f, H / 2f);
        c.scale(scale, scale);
        c.translate(-camX, -camY);
        Level lv = w.level;
        float pulse = 0.6f + 0.4f * (float) Math.sin(t * 3);
        c.save();
        c.translate(lv.goalX, lv.goalY);
        c.scale(0.7f, 0.7f);
        p.setShader(glowShader);
        p.setAlpha((int) (255 * pulse));
        c.drawCircle(0, 0, 100, p);
        p.setShader(null);
        p.setAlpha(255);
        c.restore();
        for (int i = 0; i < 5; i++) {
            float fx = lv.goalX + (float) Math.sin(t * 0.7f + i * 2) * 70;
            float fy = lv.goalY + (float) Math.cos(t * 0.9f + i * 1.3f) * 55;
            p.setColor(0xFFFFFF8D);
            p.setAlpha((int) (255 * (0.5f + 0.5f * Math.sin(t * 5 + i))));
            c.drawCircle(fx, fy, 4, p);
        }
        p.setAlpha(255);
        for (Level.Pickup pk : lv.pickups) {
            if (pk.taken) continue;
            p.setColor(pk.type == Level.SUGAR ? 0x66FFFFFF : 0x6681D4FA);
            c.drawCircle(pk.x, pk.y, 8 + 3 * pulse, p);
        }
        drawChasers(c, w, true);
        c.restore();
    }

    private void drawRain(Canvas c, int W, int H, float u) {
        s.setColor(0x88E3F2FD);
        s.setStrokeWidth(2.5f * u);
        for (int i = 0; i < 46; i++) {
            float sx = motes[i % motes.length] * W;
            float sy = ((motes[(i * 3 + 1) % motes.length] * H + t * 1500 * u * (0.8f + (i % 4) * 0.1f)) % (H + 100)) - 50;
            c.drawLine(sx, sy, sx - 10 * u, sy + 42 * u, s);
        }
    }

    private void drawSpeech(Canvas c, GameWorld w, int W, float u) {
        AntBrain a = w.ant;
        if (a.speech == null || a.speechTimer <= 0) return;
        String txt = a.speech;
        tp.setTextSize(42 * u);
        float tw = tp.measureText(txt);
        float maxW = W - 60 * u;
        if (tw > maxW) {
            tp.setTextSize(42 * u * maxW / tw);
            tw = maxW;
        }
        float pad = 26 * u;
        float bx = bubbleSX, headY = bubbleSY - w.r * scale * 0.34f - 34 * scale;
        float bottom = Math.max(insetTop + 250 * u, headY - 40 * u);
        float top = bottom - 78 * u;
        float left = Math.max(20 * u, Math.min(W - 20 * u - tw - pad * 2, bx - tw / 2 - pad));
        float alpha = Math.min(1f, a.speechTimer * 4);
        float popIn = Math.min(1f, (2.5f - Math.min(2.5f, a.speechTimer)) * 8 + 0.6f);
        c.save();
        c.scale(popIn, popIn, bx, bottom);
        p.setColor(0x44000000);
        rect.set(left + 4 * u, top + 6 * u, left + tw + pad * 2 + 4 * u, bottom + 6 * u);
        c.drawRoundRect(rect, 36 * u, 36 * u, p);
        p.setColor(a.speechAngry ? 0xFFFFEBEE : Color.WHITE);
        p.setAlpha((int) (alpha * 248));
        rect.set(left, top, left + tw + pad * 2, bottom);
        c.drawRoundRect(rect, 36 * u, 36 * u, p);
        path.reset();
        float tailX = Math.max(left + 30 * u, Math.min(left + tw + pad * 2 - 30 * u, bx));
        path.moveTo(tailX - 16 * u, bottom - 2);
        path.lineTo(tailX + 16 * u, bottom - 2);
        path.lineTo(bx, Math.max(bottom + 10 * u, headY));
        path.close();
        c.drawPath(path, p);
        tp.setColor(a.speechAngry ? 0xFFC62828 : 0xFF0D3B66);
        tp.setAlpha((int) (alpha * 255));
        c.drawText(txt, left + pad + tw / 2, top + 52 * u, tp);
        c.restore();
        tp.setAlpha(255);
        p.setAlpha(255);
    }

    private void drawTouchStick(Canvas c, float u) {
        if (touchX < 0) return;
        s.setColor(0x66FFFFFF);
        s.setStrokeWidth(6 * u);
        c.drawLine(bubbleSX, bubbleSY, touchX, touchY, s);
        p.setColor(0x44FFFFFF);
        c.drawCircle(touchX, touchY, 50 * u, p);
    }

    private void glass(Canvas c, float l, float tt, float r, float b, float rad) {
        rect.set(l, tt, r, b);
        p.setColor(0x660A1A2A);
        c.drawRoundRect(rect, rad, rad, p);
        s.setColor(0x55FFFFFF);
        s.setStrokeWidth(2);
        c.drawRoundRect(rect, rad, rad, s);
    }

    private void drawHud(Canvas c, GameWorld w, int W, int H, float u) {
        float top = insetTop + 18 * u;
        glass(c, 20 * u, top, W - 20 * u, top + 120 * u, 60 * u);

        tp.setTextAlign(Paint.Align.LEFT);
        tp.setColor(Color.WHITE);
        tp.setTextSize(42 * u);
        c.drawText(labels.level, 60 * u, top + 74 * u, tp);
        tp.setTextAlign(Paint.Align.CENTER);

        // sugar cubes
        for (int i = 0; i < 3; i++) {
            float sx = W / 2f - 70 * u + i * 70 * u, sy = top + 60 * u;
            boolean got = i < w.sugar;
            c.save();
            c.translate(sx, sy);
            c.scale(1.3f * u, 1.3f * u);
            int a = got ? 255 : 70;
            path.reset(); path.moveTo(0, -18); path.lineTo(17, -9); path.lineTo(0, 0); path.lineTo(-17, -9); path.close();
            p.setColor(Color.WHITE); p.setAlpha(a); c.drawPath(path, p);
            path.reset(); path.moveTo(-17, -9); path.lineTo(0, 0); path.lineTo(0, 19); path.lineTo(-17, 10); path.close();
            p.setColor(0xFFDCE3E8); p.setAlpha(a); c.drawPath(path, p);
            path.reset(); path.moveTo(17, -9); path.lineTo(0, 0); path.lineTo(0, 19); path.lineTo(17, 10); path.close();
            p.setColor(0xFFB8C3CB); p.setAlpha(a); c.drawPath(path, p);
            p.setAlpha(255);
            c.restore();
        }

        // pause button
        float pr = 42 * u;
        float px = W - 90 * u, py = top + 60 * u;
        pauseRect.set(px - pr * 1.5f, py - pr * 1.5f, px + pr * 1.5f, py + pr * 1.5f);
        p.setColor(Color.WHITE); p.setShader(new LinearGradient(0, py - pr, 0, py + pr, 0xFFFFD54F, 0xFFFF8F00, Shader.TileMode.CLAMP));
        c.drawCircle(px, py, pr, p);
        p.setShader(null);
        p.setColor(0xFF4E342E);
        c.drawRoundRect(px - 15 * u, py - 17 * u, px - 5 * u, py + 17 * u, 3 * u, 3 * u, p);
        c.drawRoundRect(px + 5 * u, py - 17 * u, px + 15 * u, py + 17 * u, 3 * u, 3 * u, p);

        float y2 = top + 150 * u;
        if (w.spec.timed) {
            boolean low = w.timeLeft < 10;
            float pulse = low ? 1 + 0.1f * (float) Math.abs(Math.sin(t * 8)) : 1;
            glass(c, W / 2f - 120 * u, y2, W / 2f + 120 * u, y2 + 74 * u, 37 * u);
            tp.setTextSize(52 * u * pulse);
            tp.setColor(low ? 0xFFFF5252 : Color.WHITE);
            c.drawText("⏱ " + (int) Math.ceil(w.timeLeft), W / 2f, y2 + 56 * u, tp);
            y2 += 92 * u;
        }
        if (w.spec.shrink) {
            float f = Math.max(0, w.shrinkFrac());
            float bw = W * 0.5f, bx = (W - bw) / 2;
            glass(c, bx - 6 * u, y2 - 6 * u, bx + bw + 6 * u, y2 + 36 * u, 21 * u);
            p.setColor(Color.WHITE); p.setShader(new LinearGradient(bx, 0, bx + bw, 0, f < 0.3f ? 0xFFFF8A80 : 0xFFB3E5FC, f < 0.3f ? 0xFFD32F2F : 0xFF29B6F6, Shader.TileMode.CLAMP));
            rect.set(bx, y2, bx + bw * f, y2 + 30 * u);
            c.drawRoundRect(rect, 15 * u, 15 * u, p);
            p.setShader(null);
            tp.setTextSize(24 * u);
            tp.setColor(Color.WHITE);
            c.drawText(labels.air, W / 2f, y2 + 23 * u, tp);
            y2 += 56 * u;
        }
        if (!w.level.friends.isEmpty()) {
            glass(c, W / 2f - 90 * u, y2, W / 2f + 90 * u, y2 + 64 * u, 32 * u);
            antPainter.draw(c, W / 2f - 40 * u, y2 + 50 * u, 22 * u, null, t, 0, 1, 255, false, 1);
            tp.setTextSize(36 * u);
            tp.setColor(Color.WHITE);
            c.drawText(w.riders + "/" + w.level.friends.size(), W / 2f + 30 * u, y2 + 46 * u, tp);
        }
        if (w.spec.wind > 0) {
            float mag = (float) Math.hypot(w.windX, w.windY);
            float ang = (float) Math.atan2(w.windY, w.windX);
            float cx = 90 * u, cy = top + 200 * u;
            glass(c, cx - 55 * u, cy - 55 * u, cx + 55 * u, cy + 55 * u, 55 * u);
            s.setColor(w.gust > 0.3f ? 0xFFFF8A65 : Color.WHITE);
            s.setStrokeWidth(7 * u);
            float l = 20 * u + Math.min(26 * u, mag * 0.12f * u);
            float ex = cx + (float) Math.cos(ang) * l, ey = cy + (float) Math.sin(ang) * l;
            c.drawLine(cx - (float) Math.cos(ang) * l, cy - (float) Math.sin(ang) * l, ex, ey, s);
            c.drawLine(ex, ey, ex - (float) Math.cos(ang - 0.6f) * 16 * u, ey - (float) Math.sin(ang - 0.6f) * 16 * u, s);
            c.drawLine(ex, ey, ex - (float) Math.cos(ang + 0.6f) * 16 * u, ey - (float) Math.sin(ang + 0.6f) * 16 * u, s);
        }
    }

    private void drawReady(Canvas c, GameWorld w, int W, int H, float u) {
        float a = Math.min(1f, (w.readyTime - w.stateTime) * 3);
        if (a <= 0) return;
        String body = !labels.story.isEmpty() ? labels.story : !labels.tip.isEmpty() ? labels.tip : labels.ready;
        String extra = !labels.story.isEmpty() && !labels.tip.isEmpty() ? labels.tip : null;
        tp.setTextSize(36 * u);
        int lines = countLines(body, W - 140 * u) + (extra != null ? countLines(extra, W - 140 * u) : 0);
        float cardTop = H * 0.52f, cardH = 200 * u + lines * 46 * u;
        p.setColor(0xCC0A1A2A);
        p.setAlpha((int) (a * 210));
        rect.set(36 * u, cardTop, W - 36 * u, cardTop + cardH);
        c.drawRoundRect(rect, 40 * u, 40 * u, p);
        p.setAlpha(255);
        tp.setColor(Color.WHITE);
        tp.setAlpha((int) (a * 255));
        tp.setTextSize(70 * u);
        c.drawText(labels.level, W / 2f, cardTop + 84 * u, tp);
        tp.setTextSize(40 * u);
        tp.setColor(0xFFFFE082);
        tp.setAlpha((int) (a * 255));
        c.drawText(labels.world, W / 2f, cardTop + 140 * u, tp);
        tp.setColor(Color.WHITE);
        tp.setAlpha((int) (a * 255));
        tp.setTextSize(36 * u);
        float yy = drawWrapped(c, body, W / 2f, cardTop + 196 * u, W - 140 * u, 46 * u);
        if (extra != null) {
            tp.setColor(0xFFB3E5FC);
            tp.setAlpha((int) (a * 255));
            drawWrapped(c, extra, W / 2f, yy, W - 140 * u, 46 * u);
        }
        tp.setAlpha(255);
    }

    private int countLines(String text, float maxW) {
        String[] words = text.split(" ");
        int n = 1;
        StringBuilder line = new StringBuilder();
        for (String wd : words) {
            String test = line.length() == 0 ? wd : line + " " + wd;
            if (tp.measureText(test) > maxW && line.length() > 0) { n++; line = new StringBuilder(wd); }
            else line = new StringBuilder(test);
        }
        return n;
    }

    private float drawWrapped(Canvas c, String text, float x, float y, float maxW, float lineH) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String wd : words) {
            String test = line.length() == 0 ? wd : line + " " + wd;
            if (tp.measureText(test) > maxW && line.length() > 0) {
                c.drawText(line.toString(), x, y, tp);
                y += lineH;
                line = new StringBuilder(wd);
            } else line = new StringBuilder(test);
        }
        if (line.length() > 0) { c.drawText(line.toString(), x, y, tp); y += lineH; }
        return y;
    }
}
