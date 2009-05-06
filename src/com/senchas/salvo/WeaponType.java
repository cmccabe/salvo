package com.senchas.salvo;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import android.graphics.Color;
import android.os.Bundle;

/** Represents a type of weapon that can be fired or used.
 */
public enum WeaponType {
    SMALL_MISSILE("Small Missile", Const.UNLIMITED,
        new ExplosionAttributes(10, Const.RED, 125),
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE)
    ),
    MEDIUM_MISSILE("Medium Missile", 2,
        new ExplosionAttributes(20, Const.RED, 175),
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE)
    ),
    LARGE_MISSILE("Large Missile", 0,
        new ExplosionAttributes(35, Const.RED, 200),
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE)
    ),
    EARTHMOVER("Earthmover", 0,
        new ExplosionAttributes(25, Const.GREY, 0),
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE)
    ),
    LARGE_EARTHMOVER("Large Earthmover", 0,
        new ExplosionAttributes(42, Const.GREY, 0),
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE)
    ),
    EXTRA_ARMOR("Extra Armor", 0,
        null,
        DetonationAttr.CANNOT_DETONATE,
        EnumSet.of(Attr.EXTRA_ARMOR)
    ),
    TELEPORTER("Teleporter", 0,
        null,
        DetonationAttr.CANNOT_DETONATE,
        EnumSet.of(Attr.TELEPORTER)
    ),
    ROLLER("Roller", 0,
        null,
        DetonationAttr.MAKE_ROLLER,
        EnumSet.of(Attr.PROJECTILE)
    ),
    ROLLER_IMPL("RollerImpl", Const.UNSELECTABLE,
        new ExplosionAttributes(22, Const.RED, 150),
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.ROLLER)
    ),
    LARGE_ROLLER("Large Roller", 0,
        null,
        DetonationAttr.MAKE_ROLLER,
        EnumSet.of(Attr.PROJECTILE, Attr.LARGE)
    ),
    LARGE_ROLLER_IMPL("LargeRollerImpl", Const.UNSELECTABLE,
        new ExplosionAttributes(35, Const.RED, 200),
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.ROLLER)
    ),
    CLUSTER_BOMB("Cluster Bomb", 0,
        null,
        DetonationAttr.MAKE_CLUSTER,
        EnumSet.of(Attr.PROJECTILE)
    ),
    LARGE_CLUSTER_BOMB("Large Cluster Bomb", 0,
        null,
        DetonationAttr.MAKE_CLUSTER,
        EnumSet.of(Attr.PROJECTILE, Attr.LARGE)
    ),
    DOOMHAMMER("Doomhammer", 0,
        null,
        DetonationAttr.MAKE_CLUSTER,
        EnumSet.of(Attr.PROJECTILE, Attr.EXTRA_LARGE)
    );

    /*================= Constants =================*/
    /** Contains the constants for WeaponType.
     *
     * Unfortunately, it is not safe to refer to an enum's own static
     * variables in its constructor. It's unclear whether they will be
     * initialized by the time the constructor is run.
     * This is a big issue for primitive types, because we could end up capturing
     * the pre-initialization value in the enum constructor (that means 0, or false.)
     *
     * We get around this by putting the constants in a "fake" static class that
     * doesn't do anything.
     */
    public static abstract class Const {
        public static final int RED = Color.argb(0xff, 0xff, 0, 0);

        public static final int GREY = Color.argb(0xff, 0xaa, 0xaa, 0xaa);

        /** There is an infinite supply of this weapon */
        public static final int UNLIMITED = -1;

        /** The user can never select or buy this weapon */
        public static final int UNSELECTABLE = -2;
    }

    /*================= Types =================*/
    public static enum DetonationAttr {
        /** This weapon can't detonate */
        CANNOT_DETONATE,

        /** On detonation, this weapon explodes */
        EXPLODE,

        /** On detonation, weapon creates a rolling projectile which slides
         * downhill to find its target */
        MAKE_ROLLER,

        /** On detonation, weapon splits into 3 missiles */
        MAKE_CLUSTER,
    }

    public static enum Attr {
        /** Represents a ballistic weapon */
        PROJECTILE,

        /** Represents a roller */
        ROLLER,

        /** Teleports the user to a new location */
        TELEPORTER,

        /** Gives the user life */
        EXTRA_ARMOR,

        /** This weapon is LARGE */
        LARGE,

        /** This weapon is EXTRA_LARGE */
        EXTRA_LARGE,
    }

    /** Represents the attributes of an explosion */
    public static class ExplosionAttributes {
        /*================= Data =================*/
        private final int mRadius;
        private final int mColor;
        private final int mFullDamage;

        /*================= Access =================*/
        public int getRadius() {
            return mRadius;
        }

        public int getColor() {
            return mColor;
        }

        public int getFullDamage() {
            return mFullDamage;
        }

        /*================= Lifecycle =================*/
        public ExplosionAttributes(int radius, int color, int fullDamage) {
            mRadius = radius;
            mColor = color;
            mFullDamage = fullDamage;
        }
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

                if ((startingAmount != 0) &
                        (startingAmount != Const.UNSELECTABLE))
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
            if (amt == Const.UNLIMITED) {
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
    private final int mStartingAmount;
    private final ExplosionAttributes mExplosionAttributes;
    private final DetonationAttr mDetonationAttr;
    private final EnumSet<Attr> mAttrs;

    /*================= Access =================*/
    public String getName() {
        return mName;
    }

    public int getStartingAmount() {
        return mStartingAmount;
    }

    public ExplosionAttributes getExplosionAttributes() {
        return mExplosionAttributes;
    }

    public boolean isProjectile() {
        return (mAttrs.contains(Attr.PROJECTILE));
    }

    public boolean isTeleporter() {
        return (mAttrs.contains(Attr.TELEPORTER));
    }

    public boolean isExtraArmor() {
        return (mAttrs.contains(Attr.EXTRA_ARMOR));
    }

    public boolean isRoller() {
        return (mAttrs.contains(Attr.ROLLER));
    }

    /*================= Operations =================*/
    public void detonate(Model model,
                        int x, int y,
                        GameState.BallisticsState.Accessor ball) {
        switch (mDetonationAttr) {
            case CANNOT_DETONATE: {
                throw new RuntimeException("logic error: tried to " +
                "detonate a weapon which cannot detonate.");
            }
            case EXPLODE: {
                Explosion expl = ball.newExplosion();
                expl.initialize(x, y, mExplosionAttributes);
                break;
            }
            case MAKE_ROLLER: {
                throw new RuntimeException("unimplemented");
            }
            case MAKE_CLUSTER: {
                WeaponType clusterType;
                if (mAttrs.contains(Attr.EXTRA_LARGE))
                    clusterType = LARGE_MISSILE;
                else if (mAttrs.contains(Attr.LARGE))
                    clusterType = MEDIUM_MISSILE;
                else
                    clusterType = SMALL_MISSILE;

                // TODO: vary the angles that the projectiles are shot off
                // at based on the local terrain shape
                final float deltaX[] = { -1, 0, 1 };
                final float deltaY[] = { 1, 2, 1 };
                for (int i = 0; i < 3; i++) {
                    Projectile proj = ball.newProjectile();
                    proj.initialize(x, y, deltaX[i], deltaY[i],
                                    model.getWind(), clusterType);
                }
                break;
            }
        }
    }

    /*================= Lifecycle =================*/
    private WeaponType(String name, int startingAmount,
                       ExplosionAttributes explosionAttributes,
                       DetonationAttr detonationAttr,
                       EnumSet<Attr> attrs)
    {
        // validation
        if (detonationAttr == DetonationAttr.EXPLODE) {
            if (explosionAttributes == null) {
                throw new RuntimeException("can't have " +
                    "DetonationAttr.EXPLODE with no explosion " +
                    "attributes");
            }
        }
        else {
            if (explosionAttributes != null) {
                throw new RuntimeException("can't have " +
                    "explosion attributes without " +
                    "DetonationAttr.EXPLODE");
            }
        }

        // fill in fields
        mName = name; mStartingAmount = startingAmount;
        mExplosionAttributes = explosionAttributes;
        mDetonationAttr = detonationAttr;
        mAttrs = attrs;
    }
}
