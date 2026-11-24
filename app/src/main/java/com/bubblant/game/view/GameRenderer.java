package com.bubblant.game.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import com.bubblant.game.engine.AntBrain;
import com.bubblant.game.engine.GameWorld;
import com.bubblant.game.engine.Level;

import java.util.Random;

/** Draws a {@link GameWorld}. Everything is vector-drawn with Canvas, no bitmaps needed. */
public final class GameRenderer {
    public static final class Theme {
        final int waterTop, waterBottom, wall, wallEdge, wallDark, caustic, fishColor;
        Theme(int waterTop, int waterBottom, int wall, int wallEdge, int wallDark, int caustic, int fishColor) {
            this.waterTop = waterTop; this.waterBottom = waterBottom; this.wall = wall; this.wallEdge = wallEdge;
            this.wallDark = wallDark; this.caustic = caustic; this.fishColor = fishColor;
        }
    }

    public static final Theme[] THEMES = {
            new Theme(0xFF4FC3F7, 0xFF0277BD, 0xFF5D7A2E, 0xFF9CCC65, 0xFF33691E, 0x40FFFFFF, 0xFFFF9800), // pond
            new Theme(0xFF26C6DA, 0xFF00695C, 0xFF6D4C41, 0xFFA1887F, 0xFF3E2723, 0x38FFFFFF, 0xFFFFB300), // river
            new Theme(0xFF81D4FA, 0xFF1565C0, 0xFF546E7A, 0xFFB0BEC5, 0xFF263238, 0x38FFFFFF, 0xFFFF7043), // lake
            new Theme(0xFF37474F, 0xFF0B1418, 0xFF4E342E, 0xFF8D6E63, 0xFF1B0F0B, 0x18FFFFFF, 0xFFFFCA28), // cave
            new Theme(0xFF455A64, 0xFF1A237E, 0xFF263238, 0xFF607D8B, 0xFF0D1214, 0x20FFFFFF, 0xFFE53935), // storm
            new Theme(0xFF1A0033, 0xFF000000, 0xFF311B92, 0xFF7C4DFF, 0xFF12005E, 0x10B388FF, 0xFF76FF03), // abyss
    };

    public static final class Labels {
        public String level = "", world = "", tip = "", ready = "", friends = "", air = "AIR";
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final AntPainter antPainter = new AntPainter();
    private final Random rnd = new Random();

    private Shader bgShader;
    private int bgH;
    private float camX = Float.NaN, camY;
    public float scale = 1;
    public float bubbleSX, bubbleSY;
    public final RectF pauseRect = new RectF();
    public int insetTop;
    public boolean showTalk = true;
    public int hat;
    public Labels labels = new Labels();
    public float touchX = -1, touchY = -1;
    private float t;
    private final float[] plankton = new float[60];

    public GameRenderer() {
        s.setStyle(Paint.Style.STROKE);
        s.setStrokeCap(Paint.Cap.ROUND);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        Random r = new Random(3);
        for (int i = 0; i < plankton.length; i++) plankton[i] = r.nextFloat();
    }

    public void draw(Canvas c, GameWorld w, float frameDt) {
        t += frameDt;
        int W = c.getWidth(), H = c.getHeight();
        Level lv = w.level;
        Theme th = THEMES[Math.min(THEMES.length - 1, w.spec.world)];
        float u = W / 1080f; // ui unit

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

        // background water
        if (bgShader == null || bgH != H) {
            bgShader = new LinearGradient(0, 0, 0, H, th.waterTop, th.waterBottom, Shader.TileMode.CLAMP);
            bgH = H;
        }
        p.setShader(bgShader);
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);

        // caustic light lines
        s.setColor(th.caustic);
        s.setStrokeWidth(6 * u);
        for (int i = 0; i < 7; i++) {
            path.reset();
            float base = (i / 7f) * H + (float) Math.sin(t * 0.3f + i) * 30 * u;
            for (int x = 0; x <= W; x += W / 12) {
                float yy = base + (float) Math.sin(x * 0.006f / u + t * (0.8f + i * 0.1f) + i * 2) * 40 * u;
                if (x == 0) path.moveTo(x, yy); else path.lineTo(x, yy);
            }
            c.drawPath(path, s);
        }
        // plankton with parallax
        p.setColor(0x55FFFFFF);
        for (int i = 0; i < plankton.length; i += 2) {
            float px = ((plankton[i] * 3000 - camX * scale * 0.5f) % W + W) % W;
            float py = ((plankton[i + 1] * 4000 - camY * scale * 0.5f + t * 8 * u) % H + H) % H;
            c.drawCircle(px, py, (2 + (i % 3)) * u, p);
        }

        c.save();
        c.translate(W / 2f, H / 2f);
        c.scale(scale, scale);
        c.translate(-camX, -camY);

        drawCurrents(c, w);
        drawWhirls(c, w);
        drawRipples(c, w);
        drawGoal(c, w);
        drawPickups(c, w);
        drawFriends(c, w);
        drawWalls(c, w, th);
        drawUrchins(c, w);
        drawFish(c, w, th);
        drawEels(c, w, false);
        drawWind(c, w);
        drawBubbleAndAnt(c, w);
        drawParticles(c, w);
        c.restore();

        bubbleSX = (w.x - camX) * scale + W / 2f;
        bubbleSY = (w.y - camY) * scale + H / 2f;

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

    private void drawCurrents(Canvas c, GameWorld w) {
        for (Level.Current cu : w.level.currents) {
            p.setColor(0x2200E5FF);
            c.drawRect(cu.l, cu.t, cu.r, cu.b, p);
            c.save();
            c.clipRect(cu.l, cu.t, cu.r, cu.b);
            s.setColor(0x88E0F7FA);
            s.setStrokeWidth(5);
            boolean horiz = cu.fx != 0;
            float dir = Math.signum(horiz ? cu.fx : cu.fy);
            float speed = Math.abs(horiz ? cu.fx : cu.fy) * 0.35f;
            float len = horiz ? cu.r - cu.l : cu.b - cu.t;
            for (int i = 0; i < 4; i++) {
                float off = ((t * speed + i * len / 4f) % len);
                if (dir < 0) off = len - off;
                for (int j = 0; j < 3; j++) {
                    float across = (j + 0.5f) / 3f;
                    float px, py;
                    if (horiz) { px = cu.l + off; py = cu.t + across * (cu.b - cu.t); }
                    else { px = cu.l + across * (cu.r - cu.l); py = cu.t + off; }
                    // chevron
                    path.reset();
                    if (horiz) {
                        path.moveTo(px - dir * 12, py - 12);
                        path.lineTo(px, py);
                        path.lineTo(px - dir * 12, py + 12);
                    } else {
                        path.moveTo(px - 12, py - dir * 12);
                        path.lineTo(px, py);
                        path.lineTo(px + 12, py - dir * 12);
                    }
                    c.drawPath(path, s);
                }
            }
            c.restore();
        }
    }

    private void drawWhirls(Canvas c, GameWorld w) {
        for (Level.Whirl wh : w.level.whirls) {
            p.setShader(new RadialGradient(wh.x, wh.y, wh.radius, 0x88001020, 0x00001020, Shader.TileMode.CLAMP));
            c.drawCircle(wh.x, wh.y, wh.radius, p);
            p.setShader(null);
            s.setColor(0x99E1F5FE);
            s.setStrokeWidth(4);
            for (int i = 0; i < 5; i++) {
                float rr = wh.radius * (0.2f + i * 0.17f);
                float start = (t * (260 - i * 35) + i * 70) % 360;
                rect.set(wh.x - rr, wh.y - rr, wh.x + rr, wh.y + rr);
                c.drawArc(rect, start, 200, false, s);
            }
            p.setColor(0xFF000814);
            c.drawCircle(wh.x, wh.y, 16, p);
        }
    }

    private void drawRipples(Canvas c, GameWorld w) {
        s.setStrokeWidth(3);
        for (GameWorld.Ripple r : w.ripples) {
            s.setColor(Color.WHITE);
            s.setAlpha((int) (150 * (1 - r.age / 1.2f)));
            c.drawCircle(r.x, r.y, 6 + r.age * 60, s);
            if (r.age > 0.25f) c.drawCircle(r.x, r.y, (r.age - 0.25f) * 50, s);
        }
        s.setAlpha(255);
    }

    private void drawGoal(Canvas c, GameWorld w) {
        Level lv = w.level;
        float gx = lv.goalX, gy = lv.goalY;
        boolean open = w.riders >= lv.friends.size();
        float pulse = 1 + 0.08f * (float) Math.sin(t * 4);
        p.setShader(new RadialGradient(gx, gy, 90 * pulse, open ? 0x88FFF59D : 0x55FFFFFF, 0x00FFF59D, Shader.TileMode.CLAMP));
        c.drawCircle(gx, gy, 90 * pulse, p);
        p.setShader(null);
        // lily pad
        p.setColor(0xFF43A047);
        rect.set(gx - 62, gy - 62, gx + 62, gy + 62);
        c.drawArc(rect, 20, 320, true, p);
        p.setColor(0xFF66BB6A);
        rect.set(gx - 50, gy - 50, gx + 50, gy + 50);
        c.drawArc(rect, 25, 310, true, p);
        s.setColor(0xFF2E7D32);
        s.setStrokeWidth(3);
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(40 + i * 50);
            c.drawLine(gx, gy, gx + (float) Math.cos(a) * 55, gy + (float) Math.sin(a) * 55, s);
        }
        // flower
        p.setColor(0xFFF8BBD0);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + t * 0.3;
            c.drawCircle(gx - 20 + (float) Math.cos(a) * 12, gy - 18 + (float) Math.sin(a) * 12, 10, p);
        }
        p.setColor(0xFFFFEB3B);
        c.drawCircle(gx - 20, gy - 18, 8, p);
        // flag
        s.setColor(0xFF5D4037);
        s.setStrokeWidth(5);
        c.drawLine(gx + 22, gy + 20, gx + 22, gy - 55, s);
        p.setColor(open ? 0xFFFF5252 : 0xFF9E9E9E);
        path.reset();
        float wave = (float) Math.sin(t * 6) * 5;
        path.moveTo(gx + 22, gy - 55);
        path.quadTo(gx + 42, gy - 50 + wave, gx + 60, gy - 45);
        path.lineTo(gx + 22, gy - 30);
        path.close();
        c.drawPath(path, p);
        if (!open) {
            tp.setColor(Color.WHITE);
            tp.setTextSize(30);
            c.drawText(w.riders + "/" + lv.friends.size(), gx, gy + 90, tp);
        }
    }

    private void drawPickups(Canvas c, GameWorld w) {
        for (Level.Pickup pk : w.level.pickups) {
            if (pk.taken) continue;
            float bob = (float) Math.sin(t * 3 + pk.phase) * 5;
            if (pk.type == Level.SUGAR) {
                p.setShader(new RadialGradient(pk.x, pk.y + bob, 40, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
                c.drawCircle(pk.x, pk.y + bob, 40, p);
                p.setShader(null);
                c.save();
                c.rotate((float) Math.sin(t * 2 + pk.phase) * 12, pk.x, pk.y + bob);
                p.setColor(0xFFFAFAFA);
                rect.set(pk.x - 16, pk.y + bob - 16, pk.x + 16, pk.y + bob + 16);
                c.drawRoundRect(rect, 5, 5, p);
                p.setColor(0xFFE0E0E0);
                rect.set(pk.x - 16, pk.y + bob + 4, pk.x + 16, pk.y + bob + 16);
                c.drawRoundRect(rect, 5, 5, p);
                p.setColor(0xFFBDBDBD);
                c.drawCircle(pk.x - 6, pk.y + bob - 6, 2, p);
                c.drawCircle(pk.x + 5, pk.y + bob - 2, 2, p);
                c.restore();
            } else {
                float rr = 20 + (float) Math.sin(t * 5 + pk.phase) * 2;
                p.setColor(0x55B3E5FC);
                c.drawCircle(pk.x, pk.y + bob, rr, p);
                s.setColor(0xDDFFFFFF);
                s.setStrokeWidth(3);
                c.drawCircle(pk.x, pk.y + bob, rr, s);
                p.setColor(0xCCFFFFFF);
                c.drawCircle(pk.x - rr * 0.4f, pk.y + bob - rr * 0.4f, rr * 0.25f, p);
                tp.setColor(0xFF01579B);
                tp.setTextSize(16);
                c.drawText("O₂", pk.x, pk.y + bob + 6, tp);
            }
        }
    }

    private void drawFriends(Canvas c, GameWorld w) {
        for (Level.Friend f : w.level.friends) {
            if (f.rescued) continue;
            float bob = (float) Math.sin(t * 2 + f.look) * 4;
            // leaf raft
            p.setColor(0xFF7CB342);
            c.save();
            c.rotate(20 + f.look * 40, f.x, f.y);
            rect.set(f.x - 48, f.y - 28 + bob, f.x + 48, f.y + 28 + bob);
            c.drawOval(rect, p);
            s.setColor(0xFF558B2F);
            s.setStrokeWidth(3);
            c.drawLine(f.x - 44, f.y + bob, f.x + 44, f.y + bob, s);
            c.restore();
            antPainter.draw(c, f.x, f.y + bob - 5, 26, null, t + f.look, 0, f.look + 1, 255, Math.sin(t * 3 + f.look) > 0.6);
            // "help" waving bubble
            if (Math.sin(t * 1.3 + f.look) > 0.2) {
                tp.setTextSize(24);
                tp.setColor(Color.WHITE);
                p.setColor(0xCC000000);
                rect.set(f.x - 40, f.y - 82, f.x + 40, f.y - 50);
                c.drawRoundRect(rect, 12, 12, p);
                c.drawText(labels.friends, f.x, f.y - 58, tp);
            }
        }
    }

    private void drawWalls(Canvas c, GameWorld w, Theme th) {
        float vl = w.viewL - 40, vr = w.viewR + 40, vt = w.viewT - 40, vb = w.viewB + 40;
        for (Level.Wall wl : w.level.walls) {
            if (wl.r < vl || wl.l > vr || wl.b < vt || wl.t > vb) continue;
            p.setColor(th.wallDark);
            rect.set(wl.l + 3, wl.t + 5, wl.r + 3, wl.b + 5);
            c.drawRoundRect(rect, 11, 11, p);
            p.setColor(th.wall);
            rect.set(wl.l, wl.t, wl.r, wl.b);
            c.drawRoundRect(rect, 11, 11, p);
            s.setColor(th.wallEdge);
            s.setStrokeWidth(3);
            boolean horiz = wl.r - wl.l > wl.b - wl.t;
            if (horiz) c.drawLine(wl.l + 8, wl.t + 5, wl.r - 8, wl.t + 5, s);
            else c.drawLine(wl.l + 5, wl.t + 8, wl.l + 5, wl.b - 8, s);
            // texture specks / pebbles / moss
            p.setColor(th.wallEdge);
            p.setAlpha(140);
            int seed = wl.seed;
            float len = horiz ? wl.r - wl.l : wl.b - wl.t;
            int n = (int) (len / 45);
            for (int i = 0; i < n; i++) {
                seed = seed * 1103515245 + 12345;
                float f = ((seed >>> 8) & 0xFFFF) / 65535f;
                float off = 10 + f * (len - 20);
                float ax = horiz ? wl.l + off : (wl.l + wl.r) / 2 + (f - 0.5f) * 8;
                float ay = horiz ? (wl.t + wl.b) / 2 + (f - 0.5f) * 8 : wl.t + off;
                c.drawCircle(ax, ay, 3 + f * 3, p);
            }
            p.setAlpha(255);
        }
    }

    private void drawUrchins(Canvas c, GameWorld w) {
        for (Level.Urchin un : w.level.urchins) {
            s.setColor(0xFF4A148C);
            s.setStrokeWidth(4);
            float wob = (float) Math.sin(t * 3 + un.x) * 0.1f;
            for (int i = 0; i < 14; i++) {
                double a = i * Math.PI * 2 / 14 + wob;
                float l = un.r + 9 + (i % 2) * 4;
                c.drawLine(un.x, un.y, un.x + (float) Math.cos(a) * l, un.y + (float) Math.sin(a) * l, s);
            }
            p.setColor(0xFF6A1B9A);
            c.drawCircle(un.x, un.y, un.r, p);
            p.setColor(0xFFAB47BC);
            c.drawCircle(un.x - 5, un.y - 5, un.r * 0.4f, p);
        }
    }

    private void drawFish(Canvas c, GameWorld w, Theme th) {
        boolean piranha = w.spec.world == 4;
        for (Level.Fish f : w.level.fish) {
            float dx = (f.bx - f.ax) * f.dir, dy = (f.by - f.ay) * f.dir;
            float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
            c.save();
            c.translate(f.x, f.y);
            c.rotate(ang);
            float tail = (float) Math.sin(f.phase) * 8;
            p.setColor(th.fishColor);
            path.reset();
            path.moveTo(-f.r * 0.9f, 0);
            path.lineTo(-f.r * 2.0f, -f.r * 0.8f + tail);
            path.lineTo(-f.r * 2.0f, f.r * 0.8f + tail);
            path.close();
            c.drawPath(path, p);
            rect.set(-f.r * 1.3f, -f.r * 0.85f, f.r * 1.3f, f.r * 0.85f);
            c.drawOval(rect, p);
            p.setColor(0x33000000);
            rect.set(-f.r * 0.6f, -f.r * 0.85f, -f.r * 0.1f, f.r * 0.85f);
            c.drawOval(rect, p);
            p.setColor(Color.WHITE);
            c.drawCircle(f.r * 0.6f, -f.r * 0.25f, f.r * 0.28f, p);
            p.setColor(Color.BLACK);
            c.drawCircle(f.r * 0.68f, -f.r * 0.25f, f.r * 0.14f, p);
            if (piranha) {
                p.setColor(Color.WHITE);
                for (int i = 0; i < 3; i++) {
                    path.reset();
                    float bx = f.r * (0.75f + i * 0.17f);
                    path.moveTo(bx, f.r * 0.25f);
                    path.lineTo(bx + 4, f.r * 0.5f);
                    path.lineTo(bx + 8, f.r * 0.25f);
                    path.close();
                    c.drawPath(path, p);
                }
            }
            c.restore();
        }
    }

    private void drawEels(Canvas c, GameWorld w, boolean eyesOnly) {
        for (Level.Eel e : w.level.eels) {
            float sp = (float) Math.hypot(e.vx, e.vy);
            float dx = sp > 1 ? e.vx / sp : 1, dy = sp > 1 ? e.vy / sp : 0;
            if (!eyesOnly) {
                s.setColor(0xFF1B2E1B);
                for (int i = 8; i >= 0; i--) {
                    float back = i * 11f;
                    float side = (float) Math.sin(e.phase - i * 0.7f) * (3 + i * 1.6f);
                    float bx = e.x - dx * back - dy * side, by = e.y - dy * back + dx * side;
                    p.setColor(i % 2 == 0 ? 0xFF1B3A1B : 0xFF0F240F);
                    c.drawCircle(bx, by, e.r * (1 - i * 0.07f), p);
                }
            }
            p.setColor(e.hunting ? 0xFFFF1744 : 0xFFFFEB3B);
            float ex = -dy * 6, ey = dx * 6;
            c.drawCircle(e.x + dx * 6 + ex, e.y + dy * 6 + ey, 4.5f, p);
            c.drawCircle(e.x + dx * 6 - ex, e.y + dy * 6 - ey, 4.5f, p);
        }
    }

    private void drawWind(Canvas c, GameWorld w) {
        if (w.spec.wind <= 0) return;
        float mag = (float) Math.hypot(w.windX, w.windY);
        if (mag < 1) return;
        float dx = w.windX / mag, dy = w.windY / mag;
        s.setColor(Color.WHITE);
        s.setStrokeWidth(3);
        float vw = w.viewR - w.viewL, vh = w.viewB - w.viewT;
        int n = 10 + (int) (w.gust * 14);
        for (int i = 0; i < n; i++) {
            float seedA = plankton[i % plankton.length], seedB = plankton[(i * 7 + 3) % plankton.length];
            float travel = (t * mag * 2.2f + seedA * 2000) % (vw + vh);
            float px = w.viewL + seedB * vw + dx * travel - dx * (vw + vh) / 2;
            float py = w.viewT + seedA * vh + dy * travel - dy * (vw + vh) / 2;
            px = w.viewL + ((px - w.viewL) % vw + vw) % vw;
            py = w.viewT + ((py - w.viewT) % vh + vh) % vh;
            s.setAlpha((int) (60 + 80 * w.gust));
            float l = 30 + mag * 0.25f;
            c.drawLine(px, py, px + dx * l, py + dy * l, s);
        }
        s.setAlpha(255);
    }

    private void drawBubbleAndAnt(Canvas c, GameWorld w) {
        AntBrain ant = w.ant;
        if (w.state == GameWorld.State.POPPED) {
            float f = Math.min(1f, w.stateTime / 2f);
            float sc = 1 - 0.65f * f;
            c.save();
            c.translate(w.popX, w.popY);
            c.rotate(f * 200);
            antPainter.draw(c, 0, 0, 34 * sc, ant, t, hat, 0, (int) (255 * (1 - f * 0.7f)), true);
            c.restore();
            p.setColor(0x330D47A1);
            c.drawCircle(w.popX, w.popY, 60 * f + 10, p);
            return;
        }
        float sp = (float) Math.hypot(w.vx, w.vy);
        float stretch = Math.min(0.18f, sp / 3000f) + (float) Math.sin(w.wobble * 9) * 0.02f + w.lastImpact * 0.12f;
        float ang = (float) Math.toDegrees(Math.atan2(w.vy, w.vx));
        float r = w.r;
        // shadow
        p.setColor(0x22000000);
        c.drawCircle(w.x + 8, w.y + 12, r, p);

        c.save();
        c.translate(w.x, w.y);
        c.rotate(ang);
        c.scale(1 + stretch, 1 - stretch * 0.8f);
        p.setShader(new RadialGradient(-r * 0.3f, -r * 0.3f, r * 1.3f,
                new int[]{0x55FFFFFF, 0x22B3E5FC, 0x5581D4FA}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(0, 0, r, p);
        p.setShader(null);
        c.restore();
        s.setColor(0xCCFFFFFF);
        s.setStrokeWidth(3);
        c.drawCircle(w.x, w.y, r, s);
        // shine
        p.setColor(0xDDFFFFFF);
        rect.set(w.x - r * 0.65f, w.y - r * 0.7f, w.x - r * 0.15f, w.y - r * 0.35f);
        c.drawOval(rect, p);
        p.setColor(0x88FFFFFF);
        c.drawCircle(w.x + r * 0.45f, w.y + r * 0.5f, r * 0.08f, p);
        // danger tint when going too fast
        float danger = Math.max(0, sp / w.popSpeed() - 0.75f) * 4;
        if (danger > 0 && w.state == GameWorld.State.PLAYING) {
            s.setColor(0xFFFF5252);
            s.setAlpha((int) (Math.min(1f, danger) * 200));
            s.setStrokeWidth(5);
            c.drawCircle(w.x, w.y, r + 4, s);
            s.setAlpha(255);
        }

        // riders around the main ant
        int riders = w.riders;
        for (int i = 0; i < riders; i++) {
            double a = Math.PI * 0.75 + i * 0.9;
            float rx = w.x + (float) Math.cos(a) * r * 0.55f, ry = w.y + (float) Math.sin(a) * r * 0.45f;
            antPainter.draw(c, rx, ry, 16, null, t + i, 0, i + 1, 255, false);
        }
        float antSize = Math.max(22f, r * 0.85f);
        antPainter.draw(c, w.x, w.y - r * 0.05f, antSize, ant, t, hat, 0, 255, false);
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
                c.drawLine(pt.x - sz, pt.y, pt.x + sz, pt.y, p);
                c.drawCircle(pt.x, pt.y, sz * 0.6f, p);
            }
        }
        p.setAlpha(255);
        s.setAlpha(255);
    }

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
        p.setShader(new RadialGradient(cx, cy, lightR, new int[]{0x00000000, 0x00000000, 0xF2000000},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, lightR + 1, p);
        p.setShader(null);
        path.setFillType(Path.FillType.WINDING);

        // glowing things visible in the dark
        c.save();
        c.translate(W / 2f, H / 2f);
        c.scale(scale, scale);
        c.translate(-camX, -camY);
        Level lv = w.level;
        float pulse = 0.6f + 0.4f * (float) Math.sin(t * 3);
        p.setShader(new RadialGradient(lv.goalX, lv.goalY, 70, Color.argb((int) (160 * pulse), 255, 245, 157), 0x00FFF59D, Shader.TileMode.CLAMP));
        c.drawCircle(lv.goalX, lv.goalY, 70, p);
        p.setShader(null);
        // fireflies near the goal
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
        drawEels(c, w, true);
        c.restore();
    }

    private void drawRain(Canvas c, int W, int H, float u) {
        s.setColor(0x88E3F2FD);
        s.setStrokeWidth(3 * u);
        for (int i = 0; i < 40; i++) {
            float sx = plankton[i % plankton.length] * W;
            float sy = ((plankton[(i * 3 + 1) % plankton.length] * H + t * 1400 * u * (0.8f + (i % 4) * 0.1f)) % (H + 100)) - 50;
            c.drawLine(sx, sy, sx - 10 * u, sy + 40 * u, s);
        }
    }

    private void drawSpeech(Canvas c, GameWorld w, int W, float u) {
        AntBrain a = w.ant;
        if (a.speech == null || a.speechTimer <= 0) return;
        String txt = a.speech;
        tp.setTextSize(44 * u);
        float tw = tp.measureText(txt);
        float maxW = W - 60 * u;
        if (tw > maxW) {
            tp.setTextSize(44 * u * maxW / tw);
            tw = maxW;
        }
        float pad = 26 * u;
        float bx = bubbleSX, by = bubbleSY - w.r * scale - 110 * u;
        float left = Math.max(20 * u, Math.min(W - 20 * u - tw - pad * 2, bx - tw / 2 - pad));
        float top = Math.max(insetTop + 170 * u, by - 40 * u);
        float bottom = top + 80 * u;
        float alpha = Math.min(1f, a.speechTimer * 4);
        float popIn = Math.min(1f, (2.5f - Math.min(2.5f, a.speechTimer)) * 8 + 0.6f);
        c.save();
        c.scale(popIn, popIn, bx, bottom);
        p.setColor(a.speechAngry ? 0xFFFFCDD2 : Color.WHITE);
        p.setAlpha((int) (alpha * 245));
        rect.set(left, top, left + tw + pad * 2, bottom);
        c.drawRoundRect(rect, 36 * u, 36 * u, p);
        path.reset();
        float tailX = Math.max(left + 30 * u, Math.min(left + tw + pad * 2 - 30 * u, bx));
        path.moveTo(tailX - 18 * u, bottom - 2);
        path.lineTo(tailX + 18 * u, bottom - 2);
        path.lineTo(bx, Math.max(bottom + 10 * u, bubbleSY - w.r * scale - 20 * u));
        path.close();
        c.drawPath(path, p);
        s.setColor(a.speechAngry ? 0xFFD32F2F : 0xFF1565C0);
        s.setAlpha((int) (alpha * 255));
        s.setStrokeWidth(4 * u);
        c.drawRoundRect(rect, 36 * u, 36 * u, s);
        tp.setColor(a.speechAngry ? 0xFFB71C1C : 0xFF0D47A1);
        tp.setAlpha((int) (alpha * 255));
        c.drawText(txt, left + pad + tw / 2, top + 54 * u, tp);
        c.restore();
        tp.setAlpha(255);
        s.setAlpha(255);
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

    private void drawHud(Canvas c, GameWorld w, int W, int H, float u) {
        float top = insetTop + 20 * u;
        // top bar
        p.setColor(0x66000000);
        rect.set(20 * u, top, W - 20 * u, top + 120 * u);
        c.drawRoundRect(rect, 60 * u, 60 * u, p);

        tp.setTextAlign(Paint.Align.LEFT);
        tp.setColor(Color.WHITE);
        tp.setTextSize(44 * u);
        c.drawText(labels.level, 60 * u, top + 76 * u, tp);
        tp.setTextAlign(Paint.Align.CENTER);

        // sugar cubes
        for (int i = 0; i < 3; i++) {
            float sx = W / 2f - 80 * u + i * 80 * u, sy = top + 60 * u;
            p.setColor(i < w.sugar ? Color.WHITE : 0x55FFFFFF);
            rect.set(sx - 24 * u, sy - 24 * u, sx + 24 * u, sy + 24 * u);
            c.drawRoundRect(rect, 8 * u, 8 * u, p);
        }

        // pause button
        float pr = 44 * u;
        float px = W - 90 * u, py = top + 60 * u;
        pauseRect.set(px - pr * 1.5f, py - pr * 1.5f, px + pr * 1.5f, py + pr * 1.5f);
        p.setColor(0xFFFFC107);
        c.drawCircle(px, py, pr, p);
        p.setColor(0xFF5D4037);
        c.drawRect(px - 16 * u, py - 18 * u, px - 5 * u, py + 18 * u, p);
        c.drawRect(px + 5 * u, py - 18 * u, px + 16 * u, py + 18 * u, p);

        float y2 = top + 160 * u;
        // timer
        if (w.spec.timed) {
            boolean low = w.timeLeft < 10;
            float pulse = low ? 1 + 0.1f * (float) Math.abs(Math.sin(t * 8)) : 1;
            tp.setTextSize(64 * u * pulse);
            tp.setColor(low ? 0xFFFF5252 : Color.WHITE);
            int secs = (int) Math.ceil(w.timeLeft);
            c.drawText("⏱ " + secs, W / 2f, y2 + 40 * u, tp);
            y2 += 90 * u;
        }
        // air meter
        if (w.spec.shrink) {
            float f = Math.max(0, w.shrinkFrac());
            float bw = W * 0.5f, bx = (W - bw) / 2;
            p.setColor(0x66000000);
            rect.set(bx, y2, bx + bw, y2 + 30 * u);
            c.drawRoundRect(rect, 15 * u, 15 * u, p);
            p.setColor(f < 0.3f ? 0xFFFF5252 : 0xFF81D4FA);
            rect.set(bx, y2, bx + bw * f, y2 + 30 * u);
            c.drawRoundRect(rect, 15 * u, 15 * u, p);
            tp.setTextSize(26 * u);
            tp.setColor(Color.WHITE);
            c.drawText(labels.air, W / 2f, y2 + 24 * u, tp);
            y2 += 50 * u;
        }
        // friends counter
        if (!w.level.friends.isEmpty()) {
            tp.setTextSize(40 * u);
            tp.setColor(Color.WHITE);
            c.drawText("🐜 " + w.riders + "/" + w.level.friends.size(), W / 2f, y2 + 40 * u, tp);
        }
        // wind indicator
        if (w.spec.wind > 0) {
            float mag = (float) Math.hypot(w.windX, w.windY);
            float ang = (float) Math.atan2(w.windY, w.windX);
            float cx = 90 * u, cy = top + 200 * u;
            p.setColor(0x55000000);
            c.drawCircle(cx, cy, 55 * u, p);
            s.setColor(w.gust > 0.3f ? 0xFFFF8A65 : Color.WHITE);
            s.setStrokeWidth(8 * u);
            float l = 20 * u + Math.min(28 * u, mag * 0.12f * u);
            float ex = cx + (float) Math.cos(ang) * l, ey = cy + (float) Math.sin(ang) * l;
            c.drawLine(cx - (float) Math.cos(ang) * l, cy - (float) Math.sin(ang) * l, ex, ey, s);
            c.drawLine(ex, ey, ex - (float) Math.cos(ang - 0.6f) * 18 * u, ey - (float) Math.sin(ang - 0.6f) * 18 * u, s);
            c.drawLine(ex, ey, ex - (float) Math.cos(ang + 0.6f) * 18 * u, ey - (float) Math.sin(ang + 0.6f) * 18 * u, s);
        }
    }

    private void drawReady(Canvas c, GameWorld w, int W, int H, float u) {
        float a = Math.min(1f, (1.6f - w.stateTime) * 3);
        if (a <= 0) return;
        p.setColor(0x88000000);
        p.setAlpha((int) (a * 140));
        c.drawRect(0, H * 0.55f, W, H * 0.55f + 330 * u, p);
        p.setAlpha(255);
        tp.setColor(Color.WHITE);
        tp.setAlpha((int) (a * 255));
        tp.setTextSize(80 * u);
        c.drawText(labels.level, W / 2f, H * 0.55f + 100 * u, tp);
        tp.setTextSize(42 * u);
        tp.setColor(0xFFFFE082);
        tp.setAlpha((int) (a * 255));
        c.drawText(labels.world, W / 2f, H * 0.55f + 160 * u, tp);
        tp.setColor(Color.WHITE);
        tp.setAlpha((int) (a * 255));
        tp.setTextSize(38 * u);
        String tip = labels.tip.isEmpty() ? labels.ready : labels.tip;
        drawWrapped(c, tip, W / 2f, H * 0.55f + 230 * u, W - 100 * u, 46 * u);
        tp.setAlpha(255);
    }

    private void drawWrapped(Canvas c, String text, float x, float y, float maxW, float lineH) {
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
        if (line.length() > 0) c.drawText(line.toString(), x, y, tp);
    }
}
