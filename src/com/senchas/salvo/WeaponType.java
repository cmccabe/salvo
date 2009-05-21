package com.senchas.salvo;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

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
    ROLLER_PAYLOAD("Roller Payload", Const.UNSELECTABLE,
        new ExplosionAttributes(22, Const.RED, 150),
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.ROLLER)
    ),
    LARGE_ROLLER("Large Roller", 0,
        null,
        DetonationAttr.MAKE_ROLLER,
        EnumSet.of(Attr.PROJECTILE, Attr.LARGE)
    ),
    LARGE_ROLLER_PAYLOAD("Large Roller Payload", Const.UNSELECTABLE,
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
    ),
    PLAYER_DEATH("Player Death", Const.UNSELECTABLE,
        new ExplosionAttributes(36, Const.RED, 200),
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE)
    );

    /*================= Constants =================*/
    /** Contains the constants for WeaponType.
     *
     * Unfortunately, it is not safe to refer to an enum's own static
     * variables in its constructor. It's unclear whether they will be
     * initialized by the time the constructor is run.  This is a big issue
     * for primitive types, because we could end up capturing the
     * pre-initialization value in the enum constructor (that means 0, or
     * false.)
     *
     * We get around this by putting the constants in a "fake" static class
     * that doesn't do anything.
     */
    public static abstract class Const {
        public static final int RED = Color.argb(0xff, 0xff, 0, 0);

        public static final int GREY = Color.argb(0xff, 0xaa, 0xaa, 0xaa);

        /** There is an infinite supply of this weapon */
        public static final int UNLIMITED = -1;

        /** The user can never select or buy this weapon */
        public static final int UNSELECTABLE = -2;

        /** When a cluster bomb detonates and splits into fragments, this
         * determines the initial power used to launch these fragments.
         */
        private static final float CLUSTER_BOMB_FRAG_INIT_POWER = 4;

        /** When a roller deploys its payload, this determines how fast it
         * moves.
         */
        private static final float ROLLER_PAYLOAD_INIT_POWER = 1;
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
        /*================= Access =================*/
        /** Returns the first valid weapon choice in our armory */
        public WeaponType getFirstValidWeapon() {
            WeaponType weapons[] = WeaponType.values();
            return getNextWeapon(weapons[weapons.length - 1]);
        }

        /** Returns the weapon after 'curWeapon' in the armory.
         *
         * NOTE: we assume that the armory is never empty. This is easy to
         * enforce because we have an unlimited supply of small missiles...
         */
        public WeaponType getNextWeapon(WeaponType curWeapon) {
            WeaponType weapons[] = WeaponType.values();
            for (int i = 1; i < weapons.length; i++) {
                int j = (curWeapon.ordinal() + i) % weapons.length;
                int w = mV.mWeapons[j];
                if ((w != Const.UNSELECTABLE) & (w > 0) |
                        (w == Const.UNLIMITED))
                    return weapons[j];
            }
            throw new RuntimeException("getNextWeapon: there is " +
                                         "no valid next weapon");
        }

        /** Returns the weapon before 'curWeapon' in the armory.
         *
         * NOTE: we assume that the armory is never empty. This is easy to
         * enforce because we have an unlimited supply of small missiles...
         */
        public WeaponType getPrevWeapon(WeaponType curWeapon) {
            WeaponType weapons[] = WeaponType.values();
            for (int i = 1; i < weapons.length; i++) {
                int j = (weapons.length + curWeapon.ordinal() - i) %
                            weapons.length;
                int w = mV.mWeapons[j];
                if ((w != Const.UNSELECTABLE) & (w > 0) |
                        (w == Const.UNLIMITED))
                    return weapons[j];
            }
            throw new RuntimeException("getPrevWeapon: there is " +
                                         "no valid previous weapon");
        }

        /** Returns how much we have left of a particular weapon type */
        public int getAmount(WeaponType weapon) {
            return mV.mWeapons[weapon.ordinal()];
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /** Uses one instance of WeaponType "weapon" from the armory */
        public void useWeapon(WeaponType weapon) {
            int amount = mV.mWeapons[weapon.ordinal()];
            if (amount == Const.UNLIMITED) {
                return;
            }
            else if (amount == Const.UNSELECTABLE) {
                throw new RuntimeException("useWeapon: used a weapon " +
                                           "that is UNSELECTABLE!");
            }
            else if (amount <= 0) {
                throw new RuntimeException("useWeapon: used a weapon " +
                           "that we don't have! amount = " + amount);
            }
            else {
                amount--;
                mV.mWeapons[weapon.ordinal()] = amount;
            }
        }

        /*================= Data =================*/
        public static class MyVars {
            public int mWeapons[];
        }
        private MyVars mV;

        /*================= Lifecycle =================*/
        /** Returns the Armory of weapons that all players start with */
        public static Armory fromDefault() {
            WeaponType weapons[] = WeaponType.values();

            MyVars v = new MyVars();
            v.mWeapons = new int[weapons.length];
            for (int i = 0; i < weapons.length; i++) {
                int startingAmount = weapons[i].getStartingAmount();

                // Give a good selection of weapons...
                // TODO: take this out
                if (startingAmount == 0)
                    startingAmount = 4;

                v.mWeapons[i] = startingAmount;
            }

            return new Armory(v);
        }

        public static Armory fromBundle(int index, Bundle map) {
            MyVars v = (MyVars) AutoPack.
                autoUnpack(map, Util.indexToString(index), MyVars.class);
            return new Armory(v);
        }

        private Armory(MyVars v) {
            mV = v;
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
                expl.initialize(x, y, mExplosionAttributes, ball.getPerp());
                break;
            }
            case MAKE_ROLLER: {
                WeaponType rollerType;
                if (mAttrs.contains(Attr.LARGE))
                    rollerType = LARGE_ROLLER_PAYLOAD;
                else
                    rollerType = ROLLER_PAYLOAD;

                // start rolling in the downhill direction
                Terrain terrain = model.getTerrain();
                float deltaX = (terrain.hasDownwardTangent(x)) ?
                                    Const.ROLLER_PAYLOAD_INIT_POWER :
                                    -Const.ROLLER_PAYLOAD_INIT_POWER;

                // initialize payload
                Projectile proj = ball.newProjectile();
                proj.initialize(x, y, deltaX, 0,
                                model.getWind(), rollerType, 0);
                break;
            }
            case MAKE_CLUSTER: {
                WeaponType clusterType;
                int numFragments;
                if (mAttrs.contains(Attr.EXTRA_LARGE)) {
                    clusterType = LARGE_MISSILE;
                    numFragments = 5;
                }
                else if (mAttrs.contains(Attr.LARGE)) {
                    clusterType = MEDIUM_MISSILE;
                    numFragments = 4;
                }
                else {
                    clusterType = SMALL_MISSILE;
                    numFragments = 3;
                }

                float terrainAngle = model.getTerrain().getTerrainAngle(x);
                float fragAngle = (float)(Math.PI / (numFragments + 1));
                for (int i = 0; i < numFragments; i++) {
                    float launchAngle =
                        (float)(Math.PI - terrainAngle -((i+1) * fragAngle));
                    float deltaX = Const.CLUSTER_BOMB_FRAG_INIT_POWER *
                        (float)Math.cos(launchAngle);
                    float deltaY = -Const.CLUSTER_BOMB_FRAG_INIT_POWER *
                        (float)Math.sin(launchAngle);
                    Projectile proj = ball.newProjectile();
                    proj.initialize(x, y, deltaX, deltaY,
                                    model.getWind(), clusterType, 8);
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
