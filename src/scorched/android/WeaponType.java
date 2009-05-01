package scorched.android;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import android.os.Bundle;

public enum WeaponType {
    SMALL_MISSILE("Small Missile", 10, WeaponType.UNLIMITED,
                    EnumSet.noneOf(Attr.class), 125),
    MEDIUM_MISSILE("Medium Missile", 20, 2,
                    EnumSet.noneOf(Attr.class), 175),
    LARGE_MISSILE("Large Missile", 5, 0,
                    EnumSet.noneOf(Attr.class), 100),
    DOOMHAMMER("Doomhammer", 0, 0,
                    EnumSet.of(Attr.DOOMHAMMER), 100),
    MEDIUM_ROLLER("Medium Roller", 2, 0,
                    EnumSet.of(Attr.ROLLER), 100),
    LARGE_ROLLER("Large Roller", 4, 0,
                    EnumSet.of(Attr.ROLLER), 100),
    MIRV_WARHEAD("MIRV Warhead", 4, 0,
                    EnumSet.of(Attr.MIRV), 100);

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
                int startingAmount = types[i].getStartingAmount();
                if (startingAmount != 0)
                    weapons.put(types[i], new Integer(startingAmount));
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
        public TreeMap < WeaponType, Integer > getMap() {
            return (TreeMap < WeaponType, Integer >) mWeapons;
        }

        /** Returns the weapon after 'curWeapon' in the armory.
         *
         * NOTE: we assume that the armory is never empty. This is easy to
         * enforce because we have an unlimited supply of small missiles...
         */
        public WeaponType getNextWeapon(WeaponType curWeapon) {
            if (curWeapon == mWeapons.lastKey()) {
                return mWeapons.firstKey();
            }
            else {
                WeaponType nextWeapon =
                    WeaponType.values()[curWeapon.ordinal() + 1];
                SortedMap < WeaponType, Integer > smap =
                    mWeapons.tailMap(nextWeapon);
                return smap.firstKey();
            }
        }

        /** Returns the weapon before 'curWeapon' in the armory.
         *
         * NOTE: we assume that the armory is never empty. This is easy to
         * enforce because we have an unlimited supply of small missiles...
         */
        public WeaponType getPrevWeapon(WeaponType curWeapon) {
            if (curWeapon == mWeapons.firstKey()) {
                return mWeapons.lastKey();
            }
            else {
                SortedMap < WeaponType, Integer > smap =
                    mWeapons.headMap(curWeapon);
                return smap.lastKey();
            }
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            for (WeaponType type : mWeapons.keySet()) {
                int amount = mWeapons.get(type).intValue();
                map.putInt(AutoPack.fieldNameToKey(Util.indexToString(index),
                                   type.getClass().toString()), amount);
            }
        }

        /** Uses one instance of WeaponType "type" from the armory
         *
         * @returns     the new currently selected weapon
         */
        public WeaponType useWeapon(WeaponType type) {
            Integer amount = mWeapons.get(type);
            int amt = amount.intValue();
            if (amt == UNLIMITED) {
                // Weapons with an unlimited supply can never be used up
                return type;
            }
            if (amt <= 0) {
                throw new RuntimeException("useWeapon: used a weapon " +
                                           "that we don't have?");
            }
            amt--;
            if (amt == 0) {
                WeaponType ret = getNextWeapon(type);
                mWeapons.remove(type);
                return ret;
            }
            else {
                mWeapons.put(type, new Integer(amt));
                return type;
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
    private final int mFullDamage;

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

    public int getFullDamage() {
        return mFullDamage;
    }

    /*================= Lifecycle =================*/
    private WeaponType(String name, int explosionSize,
                        int startingAmount, EnumSet<Attr> attrs,
                        int fullDamage) {
        mName = name;
        mExplosionSize = explosionSize;
        mStartingAmount = startingAmount;
        mAttrs = attrs;
        mFullDamage = fullDamage;
    }
}
