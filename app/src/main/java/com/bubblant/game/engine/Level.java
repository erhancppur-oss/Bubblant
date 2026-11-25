package com.bubblant.game.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** A fully built, playable level: walls + hazards + pickups in world units. */
public final class Level {
    public static final float CS = 180f;      // cell size
    public static final float WALL = 22f;     // wall thickness
    public static final float BUBBLE_R = 38f;
    public static final float BUBBLE_MIN_R = 17f;

    public static final class Wall {
        public final float l, t, r, b;
        public final int seed;
        Wall(float l, float t, float r, float b, int seed) { this.l = l; this.t = t; this.r = r; this.b = b; this.seed = seed; }
    }

    public static final class Urchin {
        public final float x, y, r;
        Urchin(float x, float y, float r) { this.x = x; this.y = y; this.r = r; }
    }

    public static final class Fish {
        public final float ax, ay, bx, by, speed, len;
        public float x, y, t, dir = 1, phase;
        public final float r = 17f;
        Fish(float ax, float ay, float bx, float by, float speed, float start) {
            this.ax = ax; this.ay = ay; this.bx = bx; this.by = by; this.speed = speed;
            len = (float) Math.hypot(bx - ax, by - ay);
            t = start;
            x = ax + (bx - ax) * t;
            y = ay + (by - ay) * t;
        }
    }

    public static final class Current {
        public final float l, t, r, b, fx, fy;
        Current(float l, float t, float r, float b, float fx, float fy) { this.l = l; this.t = t; this.r = r; this.b = b; this.fx = fx; this.fy = fy; }
        public boolean contains(float x, float y) { return x >= l && x <= r && y >= t && y <= b; }
    }

    public static final class Whirl {
        public final float x, y, radius, strength;
        Whirl(float x, float y, float radius, float strength) { this.x = x; this.y = y; this.radius = radius; this.strength = strength; }
    }

    public static final int SUGAR = 0, AIR = 1;

    public static final class Pickup {
        public final int type;
        public final float x, y;
        public boolean taken;
        public final float phase;
        Pickup(int type, float x, float y, float phase) { this.type = type; this.x = x; this.y = y; this.phase = phase; }
    }

    public static final class Friend {
        public final float x, y;
        public final int cell;
        public final int look; // color variant
        public boolean rescued;
        Friend(float x, float y, int cell, int look) { this.x = x; this.y = y; this.cell = cell; this.look = look; }
    }

    public static final class Eel {
        public float x, y, vx, vy, phase;
        public final float homeX, homeY, speed;
        public final float r = 16f;
        public boolean hunting;
        Eel(float x, float y, float speed) { this.x = homeX = x; this.y = homeY = y; this.speed = speed; }
    }

    public final LevelSpec spec;
    public final Maze maze;
    public final float worldW, worldH;
    public final ArrayList<Wall> walls = new ArrayList<>();
    public final ArrayList<Urchin> urchins = new ArrayList<>();
    public final ArrayList<Fish> fish = new ArrayList<>();
    public final ArrayList<Current> currents = new ArrayList<>();
    public final ArrayList<Whirl> whirls = new ArrayList<>();
    public final ArrayList<Pickup> pickups = new ArrayList<>();
    public final ArrayList<Friend> friends = new ArrayList<>();
    public final ArrayList<Eel> eels = new ArrayList<>();
    public final float startX, startY, goalX, goalY;
    public final int startCell, goalCell;
    public final float goalR = 52f;
    public final int pathCells;
    public final float timeLimit;
    public final float shrinkTime;

    public Level(LevelSpec spec) {
        this.spec = spec;
        Random rnd = new Random(spec.seed);
        int rows = spec.rows, cols = spec.cols;
        int sr = rows - 1, sc = 0;
        maze = new Maze(cols, rows, rnd, spec.braid, sr, sc);
        worldW = cols * CS;
        worldH = rows * CS;
        buildWalls(rnd);

        startCell = sr * cols + sc;
        int[] fromStart = maze.distancesFrom(sr, sc);
        int far = startCell;
        for (int i = 0; i < fromStart.length; i++) if (fromStart[i] > fromStart[far]) far = i;
        goalCell = far;
        startX = cx(startCell);
        startY = cy(startCell);
        goalX = cx(goalCell);
        goalY = cy(goalCell);

        // solution path
        int[] toGoal = maze.distancesFrom(goalCell / cols, goalCell % cols);
        ArrayList<Integer> path = new ArrayList<>();
        int cur = startCell;
        path.add(cur);
        while (cur != goalCell) {
            int r = cur / cols, c = cur % cols, next = cur;
            for (int d = 0; d < 4; d++) {
                if (!maze.open(r, c, d)) continue;
                int ni = (r + Maze.DR[d]) * cols + c + Maze.DC[d];
                if (toGoal[ni] == toGoal[cur] - 1) { next = ni; break; }
            }
            if (next == cur) break;
            cur = next;
            path.add(cur);
        }
        HashSet<Integer> onPath = new HashSet<>(path);

        HashSet<Integer> used = new HashSet<>();
        used.add(startCell);
        used.add(goalCell);
        HashSet<Integer> safe = new HashSet<>(); // no lethal hazards here
        safe.add(startCell);
        safe.add(goalCell);
        if (path.size() > 1) safe.add(path.get(1));

        ArrayList<Integer> all = new ArrayList<>();
        for (int i = 0; i < rows * cols; i++) all.add(i);
        Collections.shuffle(all, rnd);
        ArrayList<Integer> deadEnds = new ArrayList<>();
        for (int i : all) if (maze.degree(i / cols, i % cols) == 1 && !used.contains(i)) deadEnds.add(i);
        ArrayList<Integer> offPathDead = new ArrayList<>();
        for (int i : deadEnds) if (!onPath.contains(i)) offPathDead.add(i);

        // friends to rescue: off path, far from start
        int lookIdx = 0;
        ArrayList<Integer> friendCand = new ArrayList<>(offPathDead);
        for (int i : all) if (!onPath.contains(i) && !friendCand.contains(i)) friendCand.add(i);
        for (int i : all) if (!friendCand.contains(i)) friendCand.add(i);
        int detour = 0;
        for (int i : friendCand) {
            if (friends.size() >= spec.rescue) break;
            if (used.contains(i) || fromStart[i] < 2) continue;
            friends.add(new Friend(cx(i), cy(i), i, lookIdx++));
            used.add(i);
            safe.add(i);
            detour += 2 * Math.max(1, nearestPathDistance(i, path, cols));
        }

        // sugar cubes: reward exploring
        int sugarPlaced = 0;
        ArrayList<Integer> sugarCand = new ArrayList<>(offPathDead);
        sugarCand.addAll(all);
        for (int i : sugarCand) {
            if (sugarPlaced >= 3) break;
            if (used.contains(i)) continue;
            pickups.add(new Pickup(SUGAR, cx(i) + rnd.nextFloat() * 40 - 20, cy(i) + rnd.nextFloat() * 40 - 20, rnd.nextFloat() * 6f));
            used.add(i);
            sugarPlaced++;
        }

        pathCells = path.size() + detour;

        // air bubbles along the path for shrinking levels
        if (spec.shrink) {
            for (int k = 3; k < path.size() - 1; k += 4) {
                int i = path.get(k);
                pickups.add(new Pickup(AIR, cx(i) + rnd.nextFloat() * 30 - 15, cy(i) + rnd.nextFloat() * 30 - 15, rnd.nextFloat() * 6f));
            }
            for (Friend f : friends) pickups.add(new Pickup(AIR, f.x + 45, f.y - 30, 1f));
        }

        // currents in straight corridors
        int placed = 0;
        for (int i : all) {
            if (placed >= spec.currents) break;
            if (i == startCell || i == goalCell) continue;
            int r = i / cols, c = i % cols;
            boolean horiz = maze.open(r, c, 1) && maze.open(r, c, 3);
            boolean vert = maze.open(r, c, 0) && maze.open(r, c, 2);
            if (!horiz && !vert) continue;
            if (!onPath.contains(i) && rnd.nextFloat() < 0.5f) continue;
            float s = spec.currentStrength * (0.8f + rnd.nextFloat() * 0.4f) * (rnd.nextBoolean() ? 1 : -1);
            float x0 = c * CS, y0 = r * CS;
            boolean useH = horiz && (!vert || rnd.nextBoolean());
            if (useH) currents.add(new Current(x0, y0 + WALL / 2, x0 + CS, y0 + CS - WALL / 2, s, 0));
            else currents.add(new Current(x0 + WALL / 2, y0, x0 + CS - WALL / 2, y0 + CS, 0, s));
            placed++;
        }

        // whirlpools
        placed = 0;
        for (int i : all) {
            if (placed >= spec.whirlpools) break;
            if (used.contains(i) || safe.contains(i) || maze.degree(i / cols, i % cols) < 2) continue;
            whirls.add(new Whirl(cx(i), cy(i), CS * 0.75f, 230f + spec.number * 2f));
            used.add(i);
            placed++;
        }

        // eels lurk in far dead ends
        placed = 0;
        ArrayList<Integer> eelCand = new ArrayList<>(deadEnds);
        eelCand.addAll(all);
        for (int i : eelCand) {
            if (placed >= spec.eels) break;
            if (used.contains(i) || safe.contains(i) || fromStart[i] < 4) continue;
            eels.add(new Eel(cx(i), cy(i), spec.chaserSpeed));
            used.add(i);
            placed++;
        }

        // sea urchins against walls
        placed = 0;
        int tries = 0;
        while (placed < spec.urchins && tries++ < 400) {
            int i = all.get(rnd.nextInt(all.size()));
            if (safe.contains(i)) continue;
            int r = i / cols, c = i % cols;
            int d = rnd.nextInt(4);
            boolean closed = !maze.open(r, c, d);
            if (!closed) continue;
            float ur = 17f;
            float off = CS / 2 - WALL / 2 - ur - 2;
            float x = cx(i) + Maze.DC[d] * off + (Maze.DC[d] == 0 ? (rnd.nextFloat() - 0.5f) * 60 : 0);
            float y = cy(i) + Maze.DR[d] * off + (Maze.DR[d] == 0 ? (rnd.nextFloat() - 0.5f) * 60 : 0);
            boolean clash = false;
            for (Urchin u : urchins) if (Math.hypot(u.x - x, u.y - y) < 120) clash = true;
            for (Pickup p : pickups) if (Math.hypot(p.x - x, p.y - y) < 60) clash = true;
            if (clash) continue;
            urchins.add(new Urchin(x, y, ur));
            placed++;
        }

        // fish patrol straight corridors
        placed = 0;
        tries = 0;
        while (placed < spec.fish && tries++ < 300) {
            int i = all.get(rnd.nextInt(all.size()));
            int r = i / cols, c = i % cols;
            boolean horiz = rnd.nextBoolean();
            int d = horiz ? 1 : 2;
            int len = 0, rr = r, cc = c;
            while (maze.open(rr, cc, d) && len < 3) { rr += Maze.DR[d]; cc += Maze.DC[d]; len++; }
            if (len < 1) continue;
            boolean bad = false;
            for (int k = 0, ar = r, ac = c; k <= len; k++, ar += Maze.DR[d], ac += Maze.DC[d]) {
                if (safe.contains(ar * cols + ac)) bad = true;
            }
            if (bad) continue;
            float off = (rnd.nextFloat() - 0.5f) * 50;
            float ax = cx(i) + (horiz ? 0 : off), ay = cy(i) + (horiz ? off : 0);
            int j = rr * cols + cc;
            float bx = cx(j) + (horiz ? 0 : off), by = cy(j) + (horiz ? off : 0);
            fish.add(new Fish(ax, ay, bx, by, spec.fishSpeed * (0.8f + rnd.nextFloat() * 0.4f), rnd.nextFloat()));
            for (int k = 0, ar = r, ac = c; k <= len; k++, ar += Maze.DR[d], ac += Maze.DC[d]) safe.add(ar * cols + ac);
            placed++;
        }

        float travel = pathCells * CS;
        timeLimit = spec.timed ? Math.max(20f, travel / 118f + 5f + friends.size() * 6f) : 0f;
        shrinkTime = spec.shrink ? Math.max(22f, travel / 95f * 0.75f + 10f) : 0f;
    }

    private int nearestPathDistance(int cell, List<Integer> path, int cols) {
        int[] d = maze.distancesFrom(cell / cols, cell % cols);
        int best = 99;
        for (int p : path) if (d[p] >= 0 && d[p] < best) best = d[p];
        return best;
    }

    private void buildWalls(Random rnd) {
        int rows = maze.rows, cols = maze.cols;
        float hw = WALL / 2f;
        // horizontal runs
        for (int r = 0; r <= rows; r++) {
            int c = 0;
            while (c < cols) {
                if (!maze.h[r][c]) { c++; continue; }
                int start = c;
                while (c < cols && maze.h[r][c]) c++;
                walls.add(new Wall(start * CS - hw, r * CS - hw, c * CS + hw, r * CS + hw, rnd.nextInt()));
            }
        }
        // vertical runs
        for (int c = 0; c <= cols; c++) {
            int r = 0;
            while (r < rows) {
                if (!maze.v[r][c]) { r++; continue; }
                int start = r;
                while (r < rows && maze.v[r][c]) r++;
                walls.add(new Wall(c * CS - hw, start * CS - hw, c * CS + hw, r * CS + hw, rnd.nextInt()));
            }
        }
    }

    public float cx(int cell) { return (cell % maze.cols + 0.5f) * CS; }
    public float cy(int cell) { return (cell / maze.cols + 0.5f) * CS; }

    public int cellAt(float x, float y) {
        int c = Math.max(0, Math.min(maze.cols - 1, (int) (x / CS)));
        int r = Math.max(0, Math.min(maze.rows - 1, (int) (y / CS)));
        return r * maze.cols + c;
    }
}
