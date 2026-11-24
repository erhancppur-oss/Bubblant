package com.bubblant.game.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Makes sure every one of the 60 levels can be generated and is solvable. */
public class LevelGenerationTest {
    @Test
    public void allLevelsBuildAndAreSolvable() {
        for (int n = 1; n <= LevelSpec.COUNT; n++) {
            LevelSpec spec = LevelSpec.of(n);
            Level lv = new Level(spec);
            int[] d = lv.maze.distancesFrom(lv.startCell / lv.maze.cols, lv.startCell % lv.maze.cols);
            assertTrue("goal reachable in level " + n, d[lv.goalCell] > 0);
            assertEquals("friends placed in level " + n, spec.rescue, lv.friends.size());
            for (Level.Friend f : lv.friends) assertTrue("friend reachable in level " + n, d[f.cell] >= 0);
            for (Level.Urchin u : lv.urchins) {
                assertTrue("urchin not on start, level " + n,
                        Math.hypot(u.x - lv.startX, u.y - lv.startY) > Level.CS * 0.5);
            }
            if (spec.timed) assertTrue(lv.timeLimit >= 20f);
            if (spec.shrink) assertTrue(lv.shrinkTime >= 22f);
        }
    }

    @Test
    public void difficultyGrows() {
        LevelSpec first = LevelSpec.of(1), last = LevelSpec.of(LevelSpec.COUNT);
        assertTrue(last.cols * last.rows > first.cols * first.rows);
        assertTrue(last.popSpeed < first.popSpeed);
    }
}
