package com.bubblant.game.view;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.bubblant.game.engine.WorldTheme;

import java.util.Random;

/** Procedurally generated, seamlessly tiling textures (no image assets needed). */
public final class Textures {
    private static final int FLOOR = 256, WALL = 128, CAUSTIC = 256;

    private static Bitmap caustic;
    private static final Bitmap[] floors = new Bitmap[12];
    private static final Bitmap[] walls = new Bitmap[8];

    private static final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Floor texture shader; one tile covers {@code worldSize} world units. */
    public static BitmapShader floorShader(int type, float worldSize) {
        if (floors[type] == null) floors[type] = makeFloor(type);
        return shader(floors[type], worldSize / FLOOR);
    }

    public static BitmapShader wallShader(int type, float worldSize) {
        if (walls[type] == null) walls[type] = makeWall(type);
        return shader(walls[type], worldSize / WALL);
    }

    public static Bitmap caustic() {
        if (caustic == null) caustic = makeCaustic();
        return caustic;
    }

    private static BitmapShader shader(Bitmap b, float scale) {
        BitmapShader s = new BitmapShader(b, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Matrix m = new Matrix();
        m.setScale(scale, scale);
        s.setLocalMatrix(m);
        return s;
    }

    // ---------------------------------------------------------------- helpers
    /** draw a circle so it wraps around the tile edges */
    private static void blob(Canvas c, int size, float x, float y, float r, Paint paint) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++) {
                float cx = x + dx * size, cy = y + dy * size;
                if (cx + r < 0 || cx - r > size || cy + r < 0 || cy - r > size) continue;
                c.drawCircle(cx, cy, r, paint);
            }
    }

    private static void oval(Canvas c, int size, float x, float y, float rx, float ry, float rot, Paint paint) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++) {
                float cx = x + dx * size, cy = y + dy * size;
                float r = Math.max(rx, ry);
                if (cx + r < 0 || cx - r > size || cy + r < 0 || cy - r > size) continue;
                c.save();
                c.rotate(rot, cx, cy);
                c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, paint);
                c.restore();
            }
    }

    private static void noise(Canvas c, int size, Random r, int n, float minR, float maxR, int color, int alphaMin, int alphaMax) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < n; i++) {
            p.setColor(color);
            p.setAlpha(alphaMin + r.nextInt(Math.max(1, alphaMax - alphaMin)));
            blob(c, size, r.nextFloat() * size, r.nextFloat() * size, minR + r.nextFloat() * (maxR - minR), p);
        }
        p.setAlpha(255);
    }

    private static void pebbles(Canvas c, int size, Random r, int n, float minR, float maxR, int[] colors) {
        for (int i = 0; i < n; i++) {
            float x = r.nextFloat() * size, y = r.nextFloat() * size;
            float rx = minR + r.nextFloat() * (maxR - minR), ry = rx * (0.6f + r.nextFloat() * 0.4f);
            float rot = r.nextFloat() * 180;
            p.setShader(null);
            p.setColor(0x55000000);
            oval(c, size, x + rx * 0.15f, y + ry * 0.25f, rx, ry, rot, p);
            p.setColor(colors[r.nextInt(colors.length)]);
            oval(c, size, x, y, rx, ry, rot, p);
            p.setColor(0x44FFFFFF);
            oval(c, size, x - rx * 0.25f, y - ry * 0.3f, rx * 0.45f, ry * 0.35f, rot, p);
        }
    }

    private static void grid(Canvas c, int size, int n, int grout, float width) {
        p.setShader(null);
        p.setColor(grout);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        float step = size / (float) n;
        for (int i = 0; i <= n; i++) {
            c.drawLine(i * step, 0, i * step, size, p);
            c.drawLine(0, i * step, size, i * step, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    // ---------------------------------------------------------------- floors
    private static Bitmap makeFloor(int type) {
        int S = FLOOR;
        Bitmap b = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Random r = new Random(100 + type);
        switch (type) {
            case WorldTheme.FLOOR_MUD:
                c.drawColor(0xFF5A4A36);
                noise(c, S, r, 260, 6, 26, 0xFF3E3224, 30, 90);
                noise(c, S, r, 200, 4, 18, 0xFF7A6548, 30, 90);
                pebbles(c, S, r, 26, 4, 11, new int[]{0xFF8A8073, 0xFF6E655A, 0xFF9C8F7E});
                for (int i = 0; i < 6; i++) { // fallen leaf bits
                    p.setColor(r.nextBoolean() ? 0xAA6B8E23 : 0xAA9C6B30);
                    oval(c, S, r.nextFloat() * S, r.nextFloat() * S, 14, 6, r.nextFloat() * 180, p);
                }
                break;
            case WorldTheme.FLOOR_STEEL: {
                c.drawColor(0xFFB8C0C6);
                p.setStyle(Paint.Style.FILL);
                for (int y = 0; y < S; y++) {
                    int v = 170 + r.nextInt(40);
                    p.setColor(Color.argb(70, v, v + 5, v + 10));
                    c.drawRect(0, y, S, y + 1, p);
                }
                noise(c, S, r, 40, 1, 2.5f, 0xFFFFFFFF, 40, 120);
                noise(c, S, r, 12, 10, 30, 0xFF8E9AA3, 10, 30); // water spots
                break;
            }
            case WorldTheme.FLOOR_PORCELAIN:
                c.drawColor(0xFFF3F7F9);
                noise(c, S, r, 60, 20, 60, 0xFFDCE7EE, 20, 60);
                noise(c, S, r, 80, 0.8f, 1.8f, 0xFF9FB3BF, 60, 140);
                break;
            case WorldTheme.FLOOR_GRAVEL:
                c.drawColor(0xFF3B4A52);
                pebbles(c, S, r, 170, 4, 9, new int[]{0xFFCFC6B6, 0xFFA89C8B, 0xFF847A6C, 0xFF7C8C92, 0xFFB8A88F, 0xFFB98E86, 0xFF8BA3B3});
                break;
            case WorldTheme.FLOOR_POND:
                c.drawColor(0xFF2E3A26);
                noise(c, S, r, 160, 10, 32, 0xFF3F5A2A, 30, 90);
                noise(c, S, r, 120, 6, 20, 0xFF1C2416, 40, 100);
                pebbles(c, S, r, 30, 5, 13, new int[]{0xFF6E6A5C, 0xFF59564B, 0xFF7E7A6A});
                break;
            case WorldTheme.FLOOR_POOL:
                c.drawColor(0xFFB7ECF7);
                for (int i = 0; i < 8; i++) for (int j = 0; j < 8; j++) {
                    p.setColor(Color.argb(40 + r.nextInt(40), 120, 200, 230));
                    c.drawRect(i * 32 + 1, j * 32 + 1, i * 32 + 31, j * 32 + 31, p);
                }
                grid(c, S, 8, 0xFF8FCADB, 2.5f);
                break;
            case WorldTheme.FLOOR_MARBLE: {
                c.drawColor(0xFFEDE7E1);
                noise(c, S, r, 50, 20, 60, 0xFFD9D0C7, 20, 60);
                p.setStyle(Paint.Style.STROKE);
                for (int i = 0; i < 7; i++) {
                    p.setColor(Color.argb(60 + r.nextInt(60), 140, 125, 115));
                    p.setStrokeWidth(1 + r.nextFloat() * 2);
                    Path path = new Path();
                    float y0 = r.nextFloat() * S;
                    path.moveTo(-10, y0);
                    for (int x = 0; x <= S + 10; x += 32) path.lineTo(x, y0 + (r.nextFloat() - 0.5f) * 40);
                    c.drawPath(path, p);
                    c.save();
                    c.translate(0, S);
                    c.drawPath(path, p);
                    c.translate(0, -2 * S);
                    c.drawPath(path, p);
                    c.restore();
                }
                p.setStyle(Paint.Style.FILL);
                grid(c, S, 2, 0x55A89C92, 2f);
                break;
            }
            case WorldTheme.FLOOR_FIELD:
                c.drawColor(0xFF4E4A2E);
                noise(c, S, r, 220, 6, 22, 0xFF6B6236, 30, 90);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2);
                for (int i = 0; i < 18; i++) { // rice sprouts
                    float x = r.nextFloat() * S, y = r.nextFloat() * S;
                    p.setColor(0xFF8BC34A);
                    for (int k = -2; k <= 2; k++) c.drawLine(x, y, x + k * 3, y - 10 - r.nextInt(6), p);
                }
                p.setStyle(Paint.Style.FILL);
                break;
            case WorldTheme.FLOOR_ROCK:
                c.drawColor(0xFF3E3A36);
                pebbles(c, S, r, 40, 10, 28, new int[]{0xFF5D5753, 0xFF6E6762, 0xFF4E4844, 0xFF7A716A});
                noise(c, S, r, 60, 3, 8, 0xFFE57373, 60, 150); // tiny anemones
                noise(c, S, r, 80, 2, 4, 0xFFFFF59D, 40, 120);
                break;
            case WorldTheme.FLOOR_LAKE:
                c.drawColor(0xFF2E2A22);
                noise(c, S, r, 200, 8, 24, 0xFF433D31, 30, 90);
                pebbles(c, S, r, 40, 5, 14, new int[]{0xFF5C564A, 0xFF4C473D, 0xFF6E685B});
                break;
            case WorldTheme.FLOOR_CONCRETE: {
                c.drawColor(0xFF4B4F4D);
                noise(c, S, r, 400, 1, 4, 0xFF2E302F, 60, 160);
                noise(c, S, r, 200, 1, 3, 0xFF7A7F7C, 60, 140);
                noise(c, S, r, 10, 20, 50, 0xFF3B4A36, 30, 70); // algae stains
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.5f);
                p.setColor(0x88202221);
                Path crack = new Path();
                crack.moveTo(20, 40);
                crack.lineTo(70, 80);
                crack.lineTo(90, 140);
                crack.lineTo(150, 170);
                c.drawPath(crack, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            default: // basement
                c.drawColor(0xFF2B2833);
                noise(c, S, r, 300, 1, 4, 0xFF15131A, 60, 160);
                noise(c, S, r, 14, 20, 50, 0xFF3D3324, 30, 70);
                grid(c, S, 4, 0xFF1A1820, 3f);
                break;
        }
        return b;
    }

    // ---------------------------------------------------------------- walls
    private static Bitmap makeWall(int type) {
        int S = WALL;
        Bitmap b = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Random r = new Random(500 + type);
        switch (type) {
            case WorldTheme.WALL_STONE:
                c.drawColor(0xFF7D7468);
                pebbles(c, S, r, 26, 10, 22, new int[]{0xFF8C8376, 0xFF6F675C, 0xFF9A9183, 0xFF7B7265});
                noise(c, S, r, 30, 4, 10, 0xFF5E7D32, 60, 140); // moss
                break;
            case WorldTheme.WALL_TILE:
                c.drawColor(0xFFF1F5F7);
                for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
                    p.setShader(new RadialGradient(i * 32 + 10, j * 32 + 10, 34, 0x30FFFFFF, 0x10000000, Shader.TileMode.CLAMP));
                    c.drawRect(i * 32, j * 32, i * 32 + 32, j * 32 + 32, p);
                }
                p.setShader(null);
                grid(c, S, 4, 0xFFB0BEC5, 2.5f);
                break;
            case WorldTheme.WALL_WOOD:
                c.drawColor(0xFF7A5A3C);
                p.setStyle(Paint.Style.STROKE);
                for (int i = 0; i < 40; i++) {
                    p.setColor(Color.argb(40 + r.nextInt(80), r.nextBoolean() ? 60 : 160, r.nextBoolean() ? 40 : 120, 25));
                    p.setStrokeWidth(1 + r.nextFloat() * 2);
                    float y = r.nextFloat() * S;
                    Path path = new Path();
                    path.moveTo(0, y);
                    path.cubicTo(S * 0.33f, y + (r.nextFloat() - 0.5f) * 6, S * 0.66f, y + (r.nextFloat() - 0.5f) * 6, S, y);
                    c.drawPath(path, p);
                }
                p.setStyle(Paint.Style.FILL);
                noise(c, S, r, 3, 4, 7, 0xFF3E2A1A, 150, 220); // knots
                break;
            case WorldTheme.WALL_PLANT:
                c.drawColor(0xFF1F5E2A);
                for (int i = 0; i < 40; i++) {
                    p.setColor(Color.argb(200, 40 + r.nextInt(60), 120 + r.nextInt(80), 40 + r.nextInt(40)));
                    oval(c, S, r.nextFloat() * S, r.nextFloat() * S, 16, 5, r.nextFloat() * 180, p);
                }
                break;
            case WorldTheme.WALL_CONCRETE:
                c.drawColor(0xFF6B6F6C);
                noise(c, S, r, 260, 1, 3, 0xFF4A4E4B, 60, 160);
                noise(c, S, r, 120, 1, 2.5f, 0xFF9EA39F, 60, 160);
                noise(c, S, r, 6, 10, 24, 0xFF5A4A3A, 30, 70); // rust stains
                break;
            case WorldTheme.WALL_ROCK:
                c.drawColor(0xFF4E4844);
                pebbles(c, S, r, 22, 10, 24, new int[]{0xFF5D5753, 0xFF443F3B, 0xFF6A635D});
                noise(c, S, r, 40, 2, 5, 0xFFF5F5F5, 80, 180); // barnacles
                break;
            case WorldTheme.WALL_SPONGE:
                c.drawColor(0xFFF2C94C);
                noise(c, S, r, 120, 2, 6, 0xFFB8922A, 120, 220);
                noise(c, S, r, 40, 1, 3, 0xFFFFF3C4, 80, 180);
                break;
            default: // lane rope
                for (int i = 0; i < 8; i++) {
                    p.setColor(i % 4 < 2 ? 0xFFE53935 : 0xFFFFFFFF);
                    c.drawRect(i * 16, 0, i * 16 + 16, S, p);
                }
                p.setColor(0x33000000);
                for (int i = 0; i < 8; i++) c.drawRect(i * 16 + 13, 0, i * 16 + 16, S, p);
                break;
        }
        return b;
    }

    // ---------------------------------------------------------------- caustics
    private static Bitmap makeCaustic() {
        int S = CAUSTIC;
        int[] px = new int[S * S];
        double tp = Math.PI * 2;
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double u = (double) x / S, v = (double) y / S;
                double a = Math.sin(tp * (3 * u + 0.35 * Math.sin(tp * (2 * v + u))));
                double b2 = Math.sin(tp * (3 * v + 0.35 * Math.sin(tp * (2 * u - v))));
                double c2 = Math.sin(tp * (2 * (u + v) + 0.4 * Math.sin(tp * (u - 2 * v))));
                double m = Math.min(Math.abs(a + b2), Math.min(Math.abs(b2 + c2), Math.abs(a - c2)));
                double lum = Math.max(0, 1 - m * 5.5);
                lum = lum * lum;
                int al = (int) (lum * 200);
                px[y * S + x] = Color.argb(al, 255, 255, 240);
            }
        }
        return Bitmap.createBitmap(px, S, S, Bitmap.Config.ARGB_8888);
    }

    private Textures() {}
}
