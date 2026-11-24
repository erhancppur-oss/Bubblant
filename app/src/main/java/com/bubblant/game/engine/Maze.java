package com.bubblant.game.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;

/** Grid maze (recursive backtracker + braiding for loops). Directions: 0=up 1=right 2=down 3=left. */
public final class Maze {
    public static final int[] DR = {-1, 0, 1, 0};
    public static final int[] DC = {0, 1, 0, -1};

    public final int cols, rows;
    /** h[r][c]: wall on top edge of cell (r,c); r in 0..rows */
    public final boolean[][] h;
    /** v[r][c]: wall on left edge of cell (r,c); c in 0..cols */
    public final boolean[][] v;

    public Maze(int cols, int rows, Random rnd, float braid, int startR, int startC) {
        this.cols = cols;
        this.rows = rows;
        h = new boolean[rows + 1][cols];
        v = new boolean[rows][cols + 1];
        for (boolean[] a : h) java.util.Arrays.fill(a, true);
        for (boolean[] a : v) java.util.Arrays.fill(a, true);
        carve(rnd, startR, startC);
        braid(rnd, braid);
    }

    private void carve(Random rnd, int sr, int sc) {
        boolean[][] seen = new boolean[rows][cols];
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{sr, sc});
        seen[sr][sc] = true;
        int[] order = {0, 1, 2, 3};
        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            // shuffle directions
            for (int i = 3; i > 0; i--) {
                int j = rnd.nextInt(i + 1);
                int t = order[i]; order[i] = order[j]; order[j] = t;
            }
            boolean moved = false;
            for (int d : order) {
                int nr = cur[0] + DR[d], nc = cur[1] + DC[d];
                if (nr < 0 || nc < 0 || nr >= rows || nc >= cols || seen[nr][nc]) continue;
                removeWall(cur[0], cur[1], d);
                seen[nr][nc] = true;
                stack.push(new int[]{nr, nc});
                moved = true;
                break;
            }
            if (!moved) stack.pop();
        }
    }

    private void braid(Random rnd, float amount) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (degree(r, c) == 1 && rnd.nextFloat() < amount) {
                    ArrayList<Integer> cand = new ArrayList<>();
                    for (int d = 0; d < 4; d++) {
                        int nr = r + DR[d], nc = c + DC[d];
                        if (nr < 0 || nc < 0 || nr >= rows || nc >= cols) continue;
                        if (!open(r, c, d)) cand.add(d);
                    }
                    if (!cand.isEmpty()) removeWall(r, c, cand.get(rnd.nextInt(cand.size())));
                }
            }
        }
    }

    public void removeWall(int r, int c, int d) {
        switch (d) {
            case 0: h[r][c] = false; break;
            case 2: h[r + 1][c] = false; break;
            case 3: v[r][c] = false; break;
            default: v[r][c + 1] = false; break;
        }
    }

    public boolean open(int r, int c, int d) {
        int nr = r + DR[d], nc = c + DC[d];
        if (nr < 0 || nc < 0 || nr >= rows || nc >= cols) return false;
        switch (d) {
            case 0: return !h[r][c];
            case 2: return !h[r + 1][c];
            case 3: return !v[r][c];
            default: return !v[r][c + 1];
        }
    }

    public int degree(int r, int c) {
        int n = 0;
        for (int d = 0; d < 4; d++) if (open(r, c, d)) n++;
        return n;
    }

    /** BFS distances (in cells) from target cell; -1 = unreachable. Index r*cols+c. */
    public int[] distancesFrom(int tr, int tc) {
        int[] dist = new int[rows * cols];
        java.util.Arrays.fill(dist, -1);
        int[] q = new int[rows * cols];
        int head = 0, tail = 0;
        dist[tr * cols + tc] = 0;
        q[tail++] = tr * cols + tc;
        while (head < tail) {
            int cur = q[head++];
            int r = cur / cols, c = cur % cols;
            for (int d = 0; d < 4; d++) {
                if (!open(r, c, d)) continue;
                int ni = (r + DR[d]) * cols + (c + DC[d]);
                if (dist[ni] >= 0) continue;
                dist[ni] = dist[cur] + 1;
                q[tail++] = ni;
            }
        }
        return dist;
    }
}
