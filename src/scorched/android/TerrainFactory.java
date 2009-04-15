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
            short h[] = new short[Terrain.MAX_X];
            for (int i = 0; i < Terrain.MAX_X; i++) {
                int tmp = (Terrain.MAX_X - 1);
                h[i] = (short)((Terrain.MAX_Y * i) / Terrain.MAX_X);
            }
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class FlatStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            short[] h = new short[Terrain.MAX_X];
            for (int i = 0; i < Terrain.MAX_X; i++)
                h[i] = (short)(Terrain.MAX_Y / 2);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class JaggedStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            short[] h = getRandomHeights();
            h = movingWindow(h, 3);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class HillyStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            short[] h = getRandomHeights();
            h = movingWindow(h, 10);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class RollingStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            short[] h = getRandomHeights();
            h = movingWindow(h, 20);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    /*================= Utility =================*/
    private static short[] getRandomHeights() {
        short[] h = new short[Terrain.MAX_X];
        for (int i = 0; i < Terrain.MAX_X; i++) {
            h[i] = (short)Util.mRandom.nextInt(Terrain.MAX_Y);
        }
        return h;
    }

    private static short[] movingWindow(short[] input, int windowSize) {
        short[] h = new short[input.length];
        for (int i = 0; i < Terrain.MAX_X; i++) {
            short acc = 0;
            for (int j = 0; j < windowSize; ++j) {
                acc += input[(i + j) % Terrain.MAX_X];
            }
            h[i] = (short)(acc / windowSize);
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
