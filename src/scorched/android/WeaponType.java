package scorched.android;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import android.os.Bundle;

public enum WeaponType {
    SMALL_MISSILE("Small Missile", 1, WeaponType.UNLIMITED,
                    EnumSet.noneOf(Attr.class)),
    MEDIUM_MISSILE("Medium Missile", 3, 2,
                    EnumSet.noneOf(Attr.class)),
    LARGE_MISSILE("Large Missile", 5, 0,
                    EnumSet.noneOf(Attr.class)),
    DOOMHAMMER("Doomhammer", 0, 0,
                    EnumSet.of(Attr.DOOMHAMMER)),
    MEDIUM_ROLLER("Medium Roller", 2, 0,
                    EnumSet.of(Attr.ROLLER)),
    LARGE_ROLLER("Large Roller", 4, 0,
                    EnumSet.of(Attr.ROLLER)),
    MIRV_WARHEAD("MIRV Warhead", 4, 0,
                    EnumSet.of(Attr.MIRV));

    /*================= Constants =================*/
    public static final int UNLIMITED = -1;

    /*================= Types =================*/
    public static enum Attr {
        ROLLER,
        DOOMHAMMER,
        MIRV
    }

    /** An armory is a collection of weapons owned by a player.
     *
     * This class is essentially a wrapper around a TreeMap of weapon types
     * to amounts.
     */
    public static class Armory {
        /*================= Types =================*/
        private static class MyMap extends TreeMap < WeaponType, Integer > {
            private static final long serialVersionUID = 1L;
        }

        /*================= Static =================*/
        /** Returns the Armory of weapons that all players start with */
        public static Armory fromDefault() {
            MyMap weapons = new MyMap();
            WeaponType types[] = WeaponType.values();
            for (int i = 0; i < types.length; i++) {
                weapons.put(types[i],
                        new Integer(types[i].getStartingAmount()));
            }
            return new Armory(weapons);
        }

        public static Armory fromBundle(int index, Bundle map) {
            MyMap weapons = new MyMap();
            WeaponType types[] = WeaponType.values();
            for (int i = 0; i < types.length; i++) {
                String name = types[i].getClass().toString();
                String keyName =
                    AutoPack.fieldNameToKey(Util.indexToString(index), name);
                if (map.containsKey(keyName)) {
                    int amount = map.getInt(keyName);
                    weapons.put(types[i], new Integer(amount));
                }
            }
            return new Armory(weapons);
        }

        /*================= Access =================*/
        public WeaponType firstKey() {
            return mWeapons.firstKey();
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            for (WeaponType type : mWeapons.keySet()) {
                int amount = mWeapons.get(type).intValue();
                map.putInt(AutoPack.fieldNameToKey(Util.indexToString(index),
                                   type.getClass().toString()), amount);
            }
        }

        /*================= Data =================*/
        private MyMap mWeapons;

        /*================= Lifecycle =================*/
        private Armory(MyMap weapons) {
            mWeapons = weapons;
        }
    }

    /*================= Data =================*/
    private final String mName;
    private final int mExplosionSize;
    private final int mStartingAmount;
    private final EnumSet<Attr> mAttrs;

    /*================= Access =================*/
    public String getName() {
        return mName;
    }

    public int getExplosionSize() {
        return mExplosionSize;
    }

    public int getStartingAmount() {
        return mStartingAmount;
    }

    /*================= Lifecycle =================*/
    private WeaponType(String name, int explosionSize,
                        int startingAmount, EnumSet<Attr> attrs) {
        mName = name;
        mExplosionSize = explosionSize;
        mStartingAmount = startingAmount;
        mAttrs = attrs;
    }
}
