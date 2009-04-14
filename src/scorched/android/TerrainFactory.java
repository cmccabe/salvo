package scorched.android;

import java.util.HashMap;
import java.util.Random;

import android.os.Bundle;
import android.util.Log;

/**
 * Creates a Terrain
 *
 * Each TerrainFactory creates a different looking terrain.
 */
public enum TerrainFactory {
    /*================= Values =================*/
    Triangular(new TriangularStrat()),
    Flat(new FlatStrat()),
    Jagged(new JaggedStrat()),
    Hilly(new HillyStrat()),
    Rolling(new RollingStrat());

    /*================= Types =================*/
    private interface TerrainStrategy {
        public abstract Terrain toTerrain();
    }

    public static class TriangularStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            float h[] = new float[Terrain.MAX_X];
            for (int i = 0; i < Terrain.MAX_X; i++) {
                int tmp = (Terrain.MAX_X - 1);
                h[i] = (float)((tmp - i) / tmp);
            }
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class FlatStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            float[] h = new float[Terrain.MAX_X];
            for (int i = 0; i < Terrain.MAX_X; i++)
                h[i] = (float)(Terrain.MAX_DISPLAYABLE_Y / 2);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class JaggedStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            float[] h = getRandomHeights();
            h = movingWindow(h, 3);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class HillyStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            float[] h = getRandomHeights();
            h = movingWindow(h, 10);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class RollingStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            float[] h = getRandomHeights();
            h = movingWindow(h, 20);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    /*================= Utility =================*/
    private static float[] getRandomHeights() {
        float[] h = new float[Terrain.MAX_X];
        for (int i = 0; i < Terrain.MAX_X; i++) {
            h[i] = (float)(Util.mRandom.nextInt() * Terrain.MAX_ELEVATION);
        }
        return h;
    }

    private static float[] movingWindow(float[] input, int windowSize) {
        float[] h = new float[input.length];
        for (int i = 0; i < Terrain.MAX_X; i++) {
            float acc = 0;
            for (int j = 0; j < windowSize; ++j) {
                acc += input[(i + j) % Terrain.MAX_X];
            }
            h[i] = acc / windowSize;
        }
        return h;
    }

    /*================= Data =================*/
    private final TerrainStrategy mStrat;

    /*================= Access =================*/
    public Terrain createTerrain() {
        return mStrat.toTerrain();
    }

    public String toString() {
        return this.name() + " terrain";
    }

    /*================= Lifecycle =================*/
    private TerrainFactory(TerrainStrategy strat) {
        mStrat = strat;
    }
}
