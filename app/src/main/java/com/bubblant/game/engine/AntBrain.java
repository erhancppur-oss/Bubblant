package com.bubblant.game.engine;

import java.util.Random;

/**
 * The ant's personality: watches where the player goes, gives directions,
 * shakes its head when you go the wrong way, gets angry, gives a thumbs up, says thanks.
 */
public final class AntBrain {
    public enum Mood { NORMAL, HAPPY, ANGRY, SCARED, SAD }

    private final GameWorld w;
    private final Phrases p;
    private final Random rnd = new Random();

    public Mood mood = Mood.NORMAL;
    private float moodTimer;
    public String speech;
    public float speechTimer;
    public boolean speechAngry;
    private int speechPriority;
    public float shakeTimer;      // head shake ("no no no")
    public float thumbTimer;      // thumbs up
    public float jumpTimer;       // happy jump
    public float pointAngle;      // where the ant is pointing (radians)
    public float pointTimer;
    public float desiredX, desiredY;

    private int lastDir = -1;
    private int pendingDir = -1;
    private float pendingTime;
    private float lastDirSaid = -10;
    private float wrongTimer, goodTimer, idleTimer, carefulCd, ouchCd, airCd, hurryCd, needCd, eelCd, whirlCd, fishCd, almostCd;
    private int wrongCount;
    private float wrongDecay;
    private float clock;
    private int targetCell = -1;
    private int[] field;
    private boolean saidAlmost;

    AntBrain(GameWorld w, Phrases p) {
        this.w = w;
        this.p = p;
    }

    private void say(String text, int priority, boolean angry, float dur) {
        if (speechTimer > 0 && priority < speechPriority) return;
        speech = text;
        speechTimer = dur;
        speechPriority = priority;
        speechAngry = angry;
        w.say(text, angry);
    }

    private void setMood(Mood m, float t) {
        mood = m;
        moodTimer = t;
    }

    void update(float dt) {
        clock += dt;
        if (speechTimer > 0) {
            speechTimer -= dt;
            if (speechTimer <= 0) speechPriority = 0;
        }
        shakeTimer = Math.max(0, shakeTimer - dt);
        thumbTimer = Math.max(0, thumbTimer - dt);
        jumpTimer = Math.max(0, jumpTimer - dt);
        pointTimer = Math.max(0, pointTimer - dt);
        carefulCd -= dt; ouchCd -= dt; airCd -= dt; hurryCd -= dt; needCd -= dt; eelCd -= dt; whirlCd -= dt; fishCd -= dt; almostCd -= dt;
        if (moodTimer > 0) {
            moodTimer -= dt;
            if (moodTimer <= 0 && w.state == GameWorld.State.PLAYING) mood = Mood.NORMAL;
        }

        if (w.state == GameWorld.State.READY) {
            if (w.stateTime < 0.1f && speech == null) say(p.pick(p.start), 2, false, 2.2f);
            computeDesired();
            return;
        }
        if (w.state != GameWorld.State.PLAYING) return;

        computeDesired();
        float sp = (float) Math.hypot(w.vx, w.vy);

        // direction instructions
        int dir = classify(desiredX, desiredY);
        if (dir != lastDir) {
            if (dir != pendingDir) { pendingDir = dir; pendingTime = 0; }
            pendingTime += dt;
            if (pendingTime > 0.25f) {
                lastDir = dir;
                pointTimer = 1.6f;
                if (clock - lastDirSaid > 1.1f) {
                    lastDirSaid = clock;
                    say(dirPhrase(dir), 1, false, 1.4f);
                }
            }
        } else pendingDir = -1;
        pointAngle = (float) Math.atan2(desiredY, desiredX);

        // wrong way?
        if (sp > 55) {
            float dot = (w.vx * desiredX + w.vy * desiredY) / sp;
            if (dot < -0.35f) {
                wrongTimer += dt;
                goodTimer = 0;
            } else {
                wrongTimer = Math.max(0, wrongTimer - dt);
                if (dot > 0.7f && sp > 70) goodTimer += dt;
            }
        }
        if (wrongTimer > 0.75f) {
            wrongTimer = -1.8f;
            wrongCount++;
            wrongDecay = 14f;
            shakeTimer = 1.3f;
            pointTimer = 2f;
            if (wrongCount >= 3) {
                setMood(Mood.ANGRY, 3f);
                say(p.pick(p.angry), 3, true, 2.4f);
                wrongCount = 1;
            } else {
                setMood(Mood.SAD, 1.5f);
                say(p.pick(p.wrong) + " " + dirPhrase(dir), 3, false, 2f);
            }
        }
        wrongDecay -= dt;
        if (wrongDecay <= 0) wrongCount = 0;

        if (goodTimer > 3.8f) {
            goodTimer = 0;
            thumbTimer = 1.6f;
            setMood(Mood.HAPPY, 1.6f);
            if (rnd.nextFloat() < 0.7f) say(p.pick(p.good), 2, false, 1.6f);
        }

        // too fast near a wall?
        if (carefulCd <= 0 && sp > w.popSpeed() * 0.72f && wallAhead(70f)) {
            carefulCd = 3.5f;
            setMood(Mood.SCARED, 1.2f);
            say(p.pick(p.careful), 4, false, 1.4f);
        }

        // idle
        if (sp < 15) idleTimer += dt; else idleTimer = 0;
        if (idleTimer > 6f) {
            idleTimer = 0;
            say(p.pick(p.idle), 1, false, 2f);
        }

        if (w.spec.shrink && w.shrinkFrac() < 0.3f && airCd <= 0) {
            airCd = 5f;
            setMood(Mood.SCARED, 1.5f);
            say(p.pick(p.air), 4, false, 1.8f);
        }
        if (w.spec.timed && w.timeLeft < 10 && hurryCd <= 0) {
            hurryCd = 6f;
            say(p.pick(p.hurry), 4, false, 1.6f);
        }
        if (whirlCd <= 0) {
            for (Level.Whirl wh : w.level.whirls) {
                if (Math.hypot(wh.x - w.x, wh.y - w.y) < wh.radius) {
                    whirlCd = 6f;
                    setMood(Mood.SCARED, 1.5f);
                    say(p.pick(p.whirl), 4, false, 1.6f);
                    break;
                }
            }
        }
        if (fishCd <= 0) {
            for (Level.Fish f : w.level.fish) {
                if (Math.hypot(f.x - w.x, f.y - w.y) < 150) {
                    fishCd = 8f;
                    say(p.pick(p.fish), 3, false, 1.5f);
                    break;
                }
            }
        }
        if (!saidAlmost && almostCd <= 0 && w.riders >= w.level.friends.size()
                && Math.hypot(w.level.goalX - w.x, w.level.goalY - w.y) < Level.CS * 1.3f) {
            saidAlmost = true;
            setMood(Mood.HAPPY, 1.5f);
            say(p.pick(p.almost), 3, false, 1.5f);
        }
    }

    private String dirPhrase(int dir) {
        switch (dir) {
            case 0: return p.pick(p.up);
            case 1: return p.pick(p.right);
            case 2: return p.pick(p.down);
            default: return p.pick(p.left);
        }
    }

    private static int classify(float dx, float dy) {
        if (Math.abs(dx) > Math.abs(dy)) return dx > 0 ? 1 : 3;
        return dy > 0 ? 2 : 0;
    }

    private boolean wallAhead(float look) {
        float sp = (float) Math.hypot(w.vx, w.vy);
        if (sp < 1) return false;
        float px = w.x + w.vx / sp * (w.r + look), py = w.y + w.vy / sp * (w.r + look);
        for (Level.Wall wl : w.level.walls) {
            if (px >= wl.l && px <= wl.r && py >= wl.t && py <= wl.b) return true;
        }
        return false;
    }

    private void computeDesired() {
        Level lv = w.level;
        Maze m = lv.maze;
        // target = nearest friend still waiting, otherwise the goal
        int myCell = lv.cellAt(w.x, w.y);
        int tgt = lv.goalCell;
        float tx = lv.goalX, ty = lv.goalY;
        int best = Integer.MAX_VALUE;
        int[] mine = null;
        for (Level.Friend f : lv.friends) {
            if (f.rescued) continue;
            if (mine == null) mine = m.distancesFrom(myCell / m.cols, myCell % m.cols);
            int d = mine[f.cell];
            if (d >= 0 && d < best) { best = d; tgt = f.cell; tx = f.x; ty = f.y; }
        }
        if (tgt != targetCell || field == null) {
            targetCell = tgt;
            field = m.distancesFrom(tgt / m.cols, tgt % m.cols);
        }
        float dx, dy;
        if (myCell == tgt) { dx = tx - w.x; dy = ty - w.y; }
        else {
            int r = myCell / m.cols, c = myCell % m.cols;
            int next = myCell;
            for (int k = 0; k < 4; k++) {
                if (!m.open(r, c, k)) continue;
                int ni = (r + Maze.DR[k]) * m.cols + c + Maze.DC[k];
                if (field[ni] == field[myCell] - 1) { next = ni; break; }
            }
            // aim at the doorway between cells so the hint matches the corridor
            float nx = lv.cx(next), ny = lv.cy(next);
            dx = nx - w.x;
            dy = ny - w.y;
        }
        float l = (float) Math.hypot(dx, dy);
        if (l > 0.01f) { desiredX = dx / l; desiredY = dy / l; }
    }

    // ---- events from the world ----
    void onBump(float severity) {
        if (severity > 0.55f && ouchCd <= 0) {
            ouchCd = 2.5f;
            setMood(Mood.SCARED, 1f);
            say(p.pick(p.ouch), 3, false, 1.3f);
        }
    }

    void onSugar() {
        jumpTimer = 0.6f;
        setMood(Mood.HAPPY, 1f);
    }

    void onAir() {
        jumpTimer = 0.4f;
        setMood(Mood.HAPPY, 0.8f);
    }

    void onFriend() {
        jumpTimer = 0.8f;
        thumbTimer = 1.4f;
        setMood(Mood.HAPPY, 1.6f);
        say(p.pick(p.friendJoin), 5, false, 2f);
        targetCell = -1;
        lastDir = -1;
    }

    void onNeedFriends() {
        if (needCd > 0) return;
        needCd = 4f;
        setMood(Mood.ANGRY, 1.5f);
        say(p.pick(p.needFriends), 5, true, 2.2f);
    }

    void onGust() {
        setMood(Mood.SCARED, 1.2f);
        say(p.pick(p.wind), 3, false, 1.4f);
    }

    void onLightning() {
        setMood(Mood.SCARED, 1.5f);
        if (rnd.nextFloat() < 0.6f) say(p.pick(p.scared), 3, false, 1.4f);
    }

    void onEel() {
        if (eelCd > 0) return;
        eelCd = 7f;
        setMood(Mood.SCARED, 2f);
        say(p.pick(p.eel), 5, false, 1.8f);
    }

    void onWin() {
        setMood(Mood.HAPPY, 99f);
        jumpTimer = 3f;
        thumbTimer = 3f;
        say(p.pick(p.thanks), 9, false, 3f);
    }

    void onPop(int cause) {
        setMood(Mood.SAD, 99f);
        say(p.pick(p.drown), 9, false, 3f);
    }

    void onRevive() {
        mood = Mood.NORMAL;
        moodTimer = 0;
        speech = null;
        speechTimer = 0;
        speechPriority = 0;
        lastDir = -1;
    }
}
