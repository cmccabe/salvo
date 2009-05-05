package com.senchas.salvo;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import android.graphics.Color;
import android.os.Bundle;

/** Represents a type of weapon that can be fired or used.
 *
 * This class suffers a little bit from the fact that different weapons
 * require fairly different interfaces.
 * It could probably be refactored in terms of subtypes. For now I'm going to
 * leave it, since it's not (yet) *too* confusing.
 */
public enum WeaponType {
    SMALL_MISSILE("Small Missile",
                    10,
                    WeaponType.UNLIMITED,
                    EnumSet.of(Attr.PROJECTILE),
                    125),
    MEDIUM_MISSILE("Medium Missile",
                    20,
                    2,
                    EnumSet.of(Attr.PROJECTILE),
                    175),
    LARGE_MISSILE("Large Missile",
                    35,
                    0,
                    EnumSet.of(Attr.PROJECTILE),
                    200),
    EARTHMOVER("Earthmover",
                    25,
                    0,
                    EnumSet.of(Attr.PROJECTILE),
                    0),
    LARGE_EARTHMOVER("Large Earthmover",
                    42,
                    0,
                    EnumSet.of(Attr.PROJECTILE),
                    0),
    /*EXTRA_ARMOR("Extra Armor",
                    0,
                    0,
                    EnumSet.of(Attr.EXTRA_ARMOR),
                    100),*/
    TELEPORTER("Teleporter",
                    0,
                    0,
                    EnumSet.of(Attr.TELEPORTER),
                    100),
    DOOMHAMMER("Doomhammer",
                    10,
                    0,
                    EnumSet.of(Attr.PROJECTILE),
                    100),
    MEDIUM_ROLLER("Roller",
                    10,
                    0,
                    EnumSet.of(Attr.PROJECTILE, Attr.ROLLER),
                    100),
    LARGE_ROLLER("Large Roller",
                    10,
                    0,
                    EnumSet.of(Attr.PROJECTILE, Attr.ROLLER),
                    100),
    MIRV_WARHEAD("MIRV Warhead",
                    10,
                    0,
                    EnumSet.of(Attr.PROJECTILE, Attr.MIRV),
                    100);

    /*================= Constants =================*/
    public static final int UNLIMITED = -1;

    public static final int RED = Color.argb(0xff, 0xff, 0, 0);

    public static final int GREY = Color.argb(0xff, 0xaa, 0xaa, 0xaa);

    /*================= Types =================*/
    public static enum Attr {
        /** Teleports the user to a new location */
        TELEPORTER,

        /** Gives the user life */
        EXTRA_ARMOR,

        /** Represents a ballistic weapon */
        PROJECTILE,

        /** Weapon rolls down hills to find its target */
        ROLLER,

        DOOMHAMMER,

        /** Weapon splits into multiple warheads at apogee */
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

                // Give a good selection of weapons...
                // TODO: take this out
                if (startingAmount == 0)
                    startingAmount = 4;

                if (startingAmount != 0)
                    weapons.put(types[i], new Integer(startingAmount));
            }
            return new Armory(weapons);
        }

        public static Armory fromBundle(int index, Bundle map) {
            MyMap weapons = new MyMap();
            WeaponType types[] = WeaponType.values();
            for (int i = 0; i < types.length; i++) {
                String name = types[i].getName();
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
                                   type.getName()), amount);
            }
        }

        /** Uses one instance of WeaponType "type" from the armory
         *
         * @return     the new currently selected weapon
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
    private final int mExplosionRadius;
    private final int mStartingAmount;
    private final EnumSet<Attr> mAttrs;
    private final int mFullDamage;

    /*================= Access =================*/
    public boolean isProjectile() {
        return (mAttrs.contains(Attr.PROJECTILE));
    }

    public boolean isTeleporter() {
        return (mAttrs.contains(Attr.TELEPORTER));
    }

    public boolean isExtraArmor() {
        return (mAttrs.contains(Attr.EXTRA_ARMOR));
    }

    public int getExplosionColor() {
        if (! mAttrs.contains(Attr.PROJECTILE)) {
            throw new RuntimeException("Only PROJECTILE weapons have " +
                                        "an explosion color");
        }
        if (mFullDamage == 0) {
            return GREY;
        }
        else {
            return RED;
        }
    }

    public String getName() {
        return mName;
    }

    public int getExplosionRadius() {
        if (! mAttrs.contains(Attr.PROJECTILE)) {
            throw new RuntimeException("Only PROJECTILE weapons have " +
                                        "an explosion radius");
        }
        return mExplosionRadius;
    }

    public int getStartingAmount() {
        return mStartingAmount;
    }

    public int getFullDamage() {
        return mFullDamage;
    }

    /*================= Lifecycle =================*/
    private WeaponType(String name, int explosionRadius,
                        int startingAmount, EnumSet<Attr> attrs,
                        int fullDamage) {
        mName = name;
        mExplosionRadius = explosionRadius;
        mStartingAmount = startingAmount;
        mAttrs = attrs;
        mFullDamage = fullDamage;
    }
}
