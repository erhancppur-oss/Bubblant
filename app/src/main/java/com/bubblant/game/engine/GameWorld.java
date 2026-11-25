package com.bubblant.game.engine;

import com.bubblant.game.audio.Sfx;

import java.util.ArrayList;
import java.util.Random;

/** Simulation of one level: bubble physics, hazards, win / lose. */
public final class GameWorld {
    public enum State { READY, PLAYING, POPPED, WON }

    public static final int CAUSE_WALL = 0, CAUSE_SPIKE = 1, CAUSE_FISH = 2, CAUSE_WHIRL = 3,
            CAUSE_AIR = 4, CAUSE_TIME = 5, CAUSE_EEL = 6;

    public interface Listener {
        void onSfx(int id, float vol);
        void onSay(String text, boolean angry);
        void onVibrate(int ms);
        void onFinished(boolean won, int stars, int sugar, float time, int cause);
    }

    public static final class Ripple {
        public float x, y, age;
        Ripple(float x, float y) { this.x = x; this.y = y; }
    }

    public static final class Particle {
        public float x, y, vx, vy, life, max, size;
        public int kind; // 0 pop droplet, 1 rising bubble, 2 sparkle
    }

    public final Level level;
    public final LevelSpec spec;
    public final AntBrain ant;
    private final Listener listener;
    private final Random rnd = new Random();

    public State state = State.READY;
    public float stateTime;
    public float time;           // seconds played
    public float timeLeft;       // for timed levels
    public float x, y, vx, vy, r;
    public int sugar;
    public int riders;
    public int popCause = -1;
    public float popX, popY;
    public float windX, windY, gust;
    private float windAngle, gustTimer, gustT = -1;
    public final ArrayList<Ripple> ripples = new ArrayList<>();
    public final ArrayList<Particle> particles = new ArrayList<>();
    public float flash;
    private float lightningTimer, thunderDelay = -1;
    private float rainAcc;
    private float checkpointTimer;
    public float cpX, cpY;
    private boolean finishedSent;
    public float wobble;
    public float lastImpact;
    private int[] eelField;
    private float eelFieldTimer;
    private int eelFieldCell = -1;
    private float tickTimer;
    public float viewL, viewT, viewR, viewB;
    public boolean revived;
    /** how long the "get ready" intro lasts (longer when there is a story or tip to read) */
    public float readyTime = 1.6f;

    public GameWorld(Level level, Phrases phrases, Listener listener) {
        this.level = level;
        this.spec = level.spec;
        this.listener = listener;
        x = cpX = level.startX;
        y = cpY = level.startY;
        r = Level.BUBBLE_R;
        timeLeft = level.timeLimit;
        windAngle = rnd.nextFloat() * 6.28f;
        gustTimer = 3 + rnd.nextFloat() * 4;
        lightningTimer = 4 + rnd.nextFloat() * 5;
        ant = new AntBrain(this, phrases);
    }

    void say(String text, boolean angry) { listener.onSay(text, angry); }
    void sfx(int id) { listener.onSfx(id, 1f); }

    public float popSpeed() {
        // passengers make the bubble heavier and more fragile
        return spec.popSpeed * (1f - riders * 0.05f);
    }

    public float shrinkFrac() {
        return (r - Level.BUBBLE_MIN_R) / (Level.BUBBLE_R - Level.BUBBLE_MIN_R);
    }

    public void update(float dt, float tx, float ty) {
        if (dt > 0.05f) dt = 0.05f;
        stateTime += dt;
        wobble += dt;
        updateEnvironment(dt);
        updateHazards(dt);
        updateParticles(dt);

        switch (state) {
            case READY:
                if (stateTime > readyTime) {
                    state = State.PLAYING;
                    stateTime = 0;
                }
                ant.update(dt);
                return;
            case POPPED:
            case WON:
                ant.update(dt);
                if (!finishedSent && stateTime > (state == State.WON ? 1.8f : 2.2f)) {
                    finishedSent = true;
                    int stars = sugar >= 3 ? 3 : sugar >= 1 ? 2 : 1;
                    listener.onFinished(state == State.WON, stars, sugar, time, popCause);
                }
                if (state == State.WON) {
                    // drift into the safe spot
                    x += (level.goalX - x) * Math.min(1, dt * 3);
                    y += (level.goalY - y) * Math.min(1, dt * 3);
                }
                return;
            default:
                break;
        }

        time += dt;
        if (spec.timed) {
            timeLeft -= dt;
            if (timeLeft < 10) {
                tickTimer -= dt;
                if (tickTimer <= 0) { sfx(Sfx.TICK); tickTimer = 1f; }
            }
            if (timeLeft <= 0) { timeLeft = 0; pop(CAUSE_TIME); return; }
        }
        if (spec.shrink) {
            r -= (Level.BUBBLE_R - Level.BUBBLE_MIN_R) / level.shrinkTime * dt;
            if (r <= Level.BUBBLE_MIN_R) { r = Level.BUBBLE_MIN_R; pop(CAUSE_AIR); return; }
        }
        float tension = 0;
        if (spec.timed && timeLeft < 12) tension = 1;
        if (spec.shrink && shrinkFrac() < 0.3f) tension = 1;
        com.bubblant.game.audio.AudioEngine.get().setTension(tension);

        // physics in sub-steps for stable collisions
        int steps = Math.max(1, (int) Math.ceil(dt / (1f / 240f)));
        float h = dt / steps;
        float mass = 1f + riders * 0.18f;
        for (int i = 0; i < steps && state == State.PLAYING; i++) {
            float ax = tx * 820f / mass + windX / mass;
            float ay = ty * 820f / mass + windY / mass;
            for (Level.Current c : level.currents) {
                if (c.contains(x, y)) { ax += c.fx; ay += c.fy; }
            }
            for (Level.Whirl w : level.whirls) {
                float dx = w.x - x, dy = w.y - y;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < w.radius && d > 0.01f) {
                    float k = 1f - d / w.radius;
                    float pull = w.strength * (0.35f + k * 1.4f);
                    ax += dx / d * pull - dy / d * pull * 0.8f;
                    ay += dy / d * pull + dx / d * pull * 0.8f;
                    if (d < 20f) { pop(CAUSE_WHIRL); return; }
                }
            }
            vx += ax * h;
            vy += ay * h;
            float drag = (float) Math.exp(-1.25f * h);
            vx *= drag;
            vy *= drag;
            float sp = (float) Math.hypot(vx, vy);
            if (sp > 620f) { vx *= 620f / sp; vy *= 620f / sp; }
            x += vx * h;
            y += vy * h;
            collideWalls();
        }
        if (state != State.PLAYING) return;

        // hazards contact
        for (Level.Urchin u : level.urchins) {
            if (dist2(u.x, u.y) < sq(u.r + r * 0.85f)) { pop(CAUSE_SPIKE); return; }
        }
        for (Level.Fish f : level.fish) {
            if (dist2(f.x, f.y) < sq(f.r + r * 0.8f)) { pop(CAUSE_FISH); return; }
        }
        for (Level.Eel e : level.eels) {
            if (dist2(e.x, e.y) < sq(e.r + r * 0.8f)) { pop(CAUSE_EEL); return; }
        }
        for (Level.Pickup p : level.pickups) {
            if (p.taken) continue;
            if (dist2(p.x, p.y) < sq(r + 22)) {
                p.taken = true;
                if (p.type == Level.SUGAR) {
                    sugar++;
                    sfx(Sfx.SUGAR);
                    sparkle(p.x, p.y, 0xFFFFFFFF);
                    ant.onSugar();
                } else {
                    r = Math.min(Level.BUBBLE_R, r + (Level.BUBBLE_R - Level.BUBBLE_MIN_R) * 0.42f);
                    sfx(Sfx.AIR);
                    ant.onAir();
                }
            }
        }
        for (Level.Friend f : level.friends) {
            if (!f.rescued && dist2(f.x, f.y) < sq(r + 30)) {
                f.rescued = true;
                riders++;
                sfx(Sfx.FRIEND);
                ant.onFriend();
            }
        }
        if (dist2(level.goalX, level.goalY) < sq(level.goalR)) {
            if (riders >= level.friends.size()) {
                state = State.WON;
                stateTime = 0;
                vx = vy = 0;
                sfx(Sfx.WIN);
                for (int i = 0; i < 24; i++) sparkle(level.goalX, level.goalY, 0xFFFFEB3B);
                ant.onWin();
                return;
            } else {
                ant.onNeedFriends();
            }
        }

        checkpointTimer += dt;
        if (checkpointTimer > 1f) {
            checkpointTimer = 0;
            if (isSafeSpot(x, y)) { cpX = x; cpY = y; }
        }
        ant.update(dt);
    }

    private boolean isSafeSpot(float px, float py) {
        if (Math.hypot(vx, vy) > 260) return false;
        for (Level.Urchin u : level.urchins) if (Math.hypot(u.x - px, u.y - py) < 120) return false;
        for (Level.Fish f : level.fish) if (distToSeg(px, py, f.ax, f.ay, f.bx, f.by) < 110) return false;
        for (Level.Whirl w : level.whirls) if (Math.hypot(w.x - px, w.y - py) < w.radius + 40) return false;
        for (Level.Eel e : level.eels) if (Math.hypot(e.x - px, e.y - py) < 220) return false;
        for (Level.Current c : level.currents) if (c.contains(px, py)) return false;
        return true;
    }

    private static float distToSeg(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay;
        float l2 = dx * dx + dy * dy;
        float t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / l2));
        return (float) Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    private void collideWalls() {
        for (Level.Wall w : level.walls) {
            if (x + r < w.l || x - r > w.r || y + r < w.t || y - r > w.b) continue;
            float qx = Math.max(w.l, Math.min(x, w.r));
            float qy = Math.max(w.t, Math.min(y, w.b));
            float dx = x - qx, dy = y - qy;
            float d2 = dx * dx + dy * dy;
            if (d2 >= r * r) continue;
            float d = (float) Math.sqrt(d2);
            float nx, ny;
            if (d < 0.001f) { // center inside wall: push along smallest axis
                float pl = x - w.l, pr = w.r - x, pt = y - w.t, pb = w.b - y;
                float m = Math.min(Math.min(pl, pr), Math.min(pt, pb));
                if (m == pl) { nx = -1; ny = 0; } else if (m == pr) { nx = 1; ny = 0; }
                else if (m == pt) { nx = 0; ny = -1; } else { nx = 0; ny = 1; }
                d = 0;
            } else { nx = dx / d; ny = dy / d; }
            x += nx * (r - d);
            y += ny * (r - d);
            float vn = vx * nx + vy * ny;
            if (vn < 0) {
                float impact = -vn;
                if (impact > popSpeed()) {
                    pop(CAUSE_WALL);
                    return;
                }
                vx -= 1.35f * vn * nx;
                vy -= 1.35f * vn * ny;
                vx *= 0.92f;
                vy *= 0.92f;
                if (impact > 70) {
                    listener.onSfx(Sfx.BUMP, Math.min(1f, impact / 250f));
                    lastImpact = Math.min(1f, impact / popSpeed());
                    ant.onBump(impact / popSpeed());
                }
            }
        }
    }

    private void pop(int cause) {
        if (state != State.PLAYING) return;
        state = State.POPPED;
        stateTime = 0;
        popCause = cause;
        popX = x;
        popY = y;
        sfx(Sfx.POP);
        listener.onVibrate(250);
        for (int i = 0; i < 26; i++) {
            Particle p = new Particle();
            double a = rnd.nextFloat() * Math.PI * 2;
            float s = 80 + rnd.nextFloat() * 260;
            p.x = x + (float) Math.cos(a) * r;
            p.y = y + (float) Math.sin(a) * r;
            p.vx = (float) Math.cos(a) * s;
            p.vy = (float) Math.sin(a) * s;
            p.life = p.max = 0.5f + rnd.nextFloat() * 0.4f;
            p.size = 3 + rnd.nextFloat() * 6;
            p.kind = 0;
            particles.add(p);
        }
        ant.onPop(cause);
        com.bubblant.game.audio.AudioEngine.get().setTension(0);
    }

    /** Continue after watching a rewarded ad. */
    public void revive() {
        x = cpX;
        y = cpY;
        vx = vy = 0;
        r = Level.BUBBLE_R;
        if (spec.timed && timeLeft < 20) timeLeft = 20;
        for (Level.Eel e : level.eels) { e.x = e.homeX; e.y = e.homeY; e.vx = e.vy = 0; }
        state = State.READY;
        stateTime = 0.4f;
        finishedSent = false;
        popCause = -1;
        revived = true;
        readyTime = 1.6f;
        ant.onRevive();
    }

    private void sparkle(float px, float py, int color) {
        for (int i = 0; i < 8; i++) {
            Particle p = new Particle();
            double a = rnd.nextFloat() * Math.PI * 2;
            float s = 40 + rnd.nextFloat() * 120;
            p.x = px;
            p.y = py;
            p.vx = (float) Math.cos(a) * s;
            p.vy = (float) Math.sin(a) * s;
            p.life = p.max = 0.6f + rnd.nextFloat() * 0.5f;
            p.size = 3 + rnd.nextFloat() * 4;
            p.kind = 2;
            particles.add(p);
        }
    }

    private void updateEnvironment(float dt) {
        if (spec.wind > 0) {
            windAngle += (float) Math.sin(time * 0.11f) * dt * 0.35f;
            gustTimer -= dt;
            if (gustTimer <= 0 && gustT < 0) {
                gustT = 0;
                gustTimer = (6 + rnd.nextFloat() * 5) / Math.max(0.3f, spec.gustiness);
                windAngle += (rnd.nextFloat() - 0.5f) * 2.2f;
                if (state == State.PLAYING) {
                    sfx(Sfx.WHOOSH);
                    ant.onGust();
                }
            }
            if (gustT >= 0) {
                gustT += dt;
                gust = (float) Math.sin(Math.min(1f, gustT / 1.8f) * Math.PI);
                if (gustT > 1.8f) { gustT = -1; gust = 0; }
            }
            float mag = spec.wind * (0.55f + 0.45f * (float) Math.sin(time * 0.6f)) * (1 + gust * 1.7f * spec.gustiness);
            windX = (float) Math.cos(windAngle) * mag;
            windY = (float) Math.sin(windAngle) * mag;
        }
        if (spec.rain) {
            rainAcc += dt * 7f;
            while (rainAcc > 1) {
                rainAcc -= 1;
                float rx = viewL + rnd.nextFloat() * (viewR - viewL);
                float ry = viewT + rnd.nextFloat() * (viewB - viewT);
                ripples.add(new Ripple(rx, ry));
                if (state == State.PLAYING) {
                    float d = (float) Math.hypot(rx - x, ry - y);
                    if (d < 90 && d > 1) {
                        float k = (1 - d / 90f) * 120f;
                        vx += (x - rx) / d * k;
                        vy += (y - ry) / d * k;
                    }
                }
            }
        }
        for (int i = ripples.size() - 1; i >= 0; i--) {
            Ripple rp = ripples.get(i);
            rp.age += dt;
            if (rp.age > 1.2f) ripples.remove(i);
        }
        if (spec.lightning) {
            lightningTimer -= dt;
            if (lightningTimer <= 0) {
                lightningTimer = 6 + rnd.nextFloat() * 7;
                flash = 1f;
                thunderDelay = 0.25f + rnd.nextFloat() * 0.4f;
                if (state == State.PLAYING) ant.onLightning();
                if (spec.wind > 0 && gustT < 0) gustTimer = 0;
            }
        }
        if (thunderDelay >= 0) {
            thunderDelay -= dt;
            if (thunderDelay < 0) {
                sfx(Sfx.THUNDER);
                listener.onVibrate(60);
            }
        }
        flash = Math.max(0, flash - dt * 2.2f);
        lastImpact = Math.max(0, lastImpact - dt * 2f);
    }

    private void updateHazards(float dt) {
        for (Level.Fish f : level.fish) {
            f.phase += dt * 8;
            if (f.len > 1) {
                f.t += f.dir * f.speed * dt / f.len;
                if (f.t > 1) { f.t = 1; f.dir = -1; }
                if (f.t < 0) { f.t = 0; f.dir = 1; }
            }
            f.x = f.ax + (f.bx - f.ax) * f.t;
            f.y = f.ay + (f.by - f.ay) * f.t;
        }
        if (!level.eels.isEmpty()) updateEels(dt);
    }

    private void updateEels(float dt) {
        Maze m = level.maze;
        int myCell = level.cellAt(x, y);
        eelFieldTimer -= dt;
        if (eelField == null || (eelFieldTimer <= 0 && myCell != eelFieldCell)) {
            eelField = m.distancesFrom(myCell / m.cols, myCell % m.cols);
            eelFieldCell = myCell;
            eelFieldTimer = 0.3f;
        }
        boolean active = state == State.PLAYING;
        for (Level.Eel e : level.eels) {
            e.phase += dt * 6;
            int ec = level.cellAt(e.x, e.y);
            int d = eelField[ec];
            float txp, typ;
            float speed = e.speed;
            boolean wasHunting = e.hunting;
            e.hunting = active && d >= 0 && d <= 4;
            if (e.hunting && !wasHunting) ant.onEel();
            if (e.hunting) {
                if (ec == myCell) { txp = x; typ = y; speed *= 1.15f; }
                else {
                    int er = ec / m.cols, ecc = ec % m.cols;
                    int next = ec;
                    for (int k = 0; k < 4; k++) {
                        if (!m.open(er, ecc, k)) continue;
                        int ni = (er + Maze.DR[k]) * m.cols + ecc + Maze.DC[k];
                        if (eelField[ni] == d - 1) { next = ni; break; }
                    }
                    txp = level.cx(next);
                    typ = level.cy(next);
                }
            } else {
                txp = e.homeX + (float) Math.sin(e.phase * 0.2f) * 40;
                typ = e.homeY + (float) Math.cos(e.phase * 0.15f) * 40;
                speed *= 0.4f;
            }
            float dx = txp - e.x, dy = typ - e.y;
            float dl = (float) Math.sqrt(dx * dx + dy * dy);
            if (dl > 1) { dx /= dl; dy /= dl; }
            float k = Math.min(1f, dt * 3f);
            e.vx += (dx * speed - e.vx) * k;
            e.vy += (dy * speed - e.vy) * k;
            e.x += e.vx * dt;
            e.y += e.vy * dt;
            // keep eel out of walls
            for (Level.Wall w : level.walls) {
                float qx = Math.max(w.l, Math.min(e.x, w.r));
                float qy = Math.max(w.t, Math.min(e.y, w.b));
                float ddx = e.x - qx, ddy = e.y - qy;
                float d2 = ddx * ddx + ddy * ddy;
                if (d2 < e.r * e.r && d2 > 0.0001f) {
                    float dd = (float) Math.sqrt(d2);
                    e.x += ddx / dd * (e.r - dd);
                    e.y += ddy / dd * (e.r - dd);
                }
            }
        }
    }

    private void updateParticles(float dt) {
        if (state == State.POPPED && rnd.nextFloat() < dt * 12) {
            Particle p = new Particle();
            p.x = popX + (rnd.nextFloat() - 0.5f) * 20;
            p.y = popY;
            p.vx = (rnd.nextFloat() - 0.5f) * 20;
            p.vy = -40 - rnd.nextFloat() * 40;
            p.life = p.max = 1.2f;
            p.size = 3 + rnd.nextFloat() * 5;
            p.kind = 1;
            particles.add(p);
        }
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) { particles.remove(i); continue; }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            if (p.kind != 1) { p.vx *= 0.94f; p.vy *= 0.94f; }
        }
    }

    private float dist2(float px, float py) { return (px - x) * (px - x) + (py - y) * (py - y); }
    private static float sq(float v) { return v * v; }
}
