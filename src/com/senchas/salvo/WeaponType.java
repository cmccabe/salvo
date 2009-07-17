package com.senchas.salvo;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import com.senchas.salvo.Cosmos.PlayerInfo;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Represents a type of weapon that can be fired or used.
 */
public enum WeaponType {
    SMALL_MISSILE("Small Missile", Const.UNLIMITED,
        new ExplosionAttributes(10, Const.RED, 125),
        Const.UNBUYABLE,
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE),
        AutoPack.EMPTY_STRING
    ),
    MEDIUM_MISSILE("Medium Missile", 2,
        new ExplosionAttributes(20, Const.RED, 175),
        100,
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE),
        "A bigger missle"
        ),
    LARGE_MISSILE("Large Missile", 0,
        new ExplosionAttributes(35, Const.RED, 200),
        150,
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE),
        "The biggest missle"
    ),
    EARTHMOVER("Earthmover", 0,
        new ExplosionAttributes(45, Const.GREY, 0),
        50,
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE),
        "Does no direct damage, but reshapes the terrain"
    ),
    LARGE_EARTHMOVER("Large Earthmover", 0,
        new ExplosionAttributes(74, Const.GREY, 0),
        100,
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE),
        "A bigger version of the Earthmover"
    ),
    EXTRA_ARMOR("Extra Armor", 0,
        null,
        400,
        DetonationAttr.CANNOT_DETONATE,
        EnumSet.of(Attr.EXTRA_ARMOR),
        "Gives you back your health"
    ),
    TELEPORTER("Teleporter", 0,
        null,
        250,
        DetonationAttr.CANNOT_DETONATE,
        EnumSet.of(Attr.TELEPORTER),
        "Moves you to a random location"
    ),
    ROLLER("Roller", 0,
        null,
        350,
        DetonationAttr.MAKE_ROLLER,
        EnumSet.of(Attr.PROJECTILE),
        "Rolls downhill until it hits something"
    ),
    ROLLER_PAYLOAD("Roller Payload", Const.UNSELECTABLE,
        new ExplosionAttributes(22, Const.RED, 150),
        Const.UNBUYABLE,
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.ROLLER),
        AutoPack.EMPTY_STRING
    ),
    LARGE_ROLLER("Large Roller", 0,
        null,
        500,
        DetonationAttr.MAKE_ROLLER,
        EnumSet.of(Attr.PROJECTILE, Attr.LARGE),
        "A bigger Roller"
    ),
    LARGE_ROLLER_PAYLOAD("Large Roller Payload", Const.UNSELECTABLE,
        new ExplosionAttributes(35, Const.RED, 200),
        Const.UNBUYABLE,
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.ROLLER),
        AutoPack.EMPTY_STRING
    ),
    CLUSTER_BOMB("Cluster Bomb", 0,
        null,
        330,
        DetonationAttr.MAKE_CLUSTER,
        EnumSet.of(Attr.PROJECTILE, Attr.LARGE),
        "Splits into multiple grenades on impact"
    ),
    CLUSTER_BOMB_PAYLOAD("Cluster Bomb Payload", Const.UNSELECTABLE,
        new ExplosionAttributes(30, Const.RED, 50),
        Const.UNBUYABLE,
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE),
        AutoPack.EMPTY_STRING
    ),
    DOOMHAMMER("Doomhammer", 0,
        null,
        650,
        DetonationAttr.MAKE_CLUSTER,
        EnumSet.of(Attr.PROJECTILE, Attr.EXTRA_LARGE),
        "A powerful but wildly inaccurate weapon"
    ),
    PLAYER_DEATH("Player Death", Const.UNSELECTABLE,
        new ExplosionAttributes(36, Const.RED, 200),
        Const.UNBUYABLE,
        DetonationAttr.EXPLODE,
        EnumSet.of(Attr.PROJECTILE),
        AutoPack.EMPTY_STRING
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

        /** The user can never buy this weapon. Implies UNSELECTABLE. */
        public static final int UNBUYABLE = -1;

        /** The user can never select this weapon. Implies UNBUYABLE. */
        public static final int UNSELECTABLE = -2;

        /** When a cluster bomb detonates and splits into fragments, this
         * determines the initial power used to launch these fragments.
         */
        private static final float CLUSTER_BOMB_FRAG_INIT_POWER = 2.0f;

        /** When a doomhammer detonates and splits into fragments, this
         * determines the initial power used to launch these fragments.
         */
        private static final float DOOMHAMMER_FRAG_INIT_POWER = 4.0f;

        /** When a roller deploys its payload, this determines how fast it
         * moves.
         */
        private static final float ROLLER_PAYLOAD_INIT_POWER = 1.0f;
    }

    /*================= Static =================*/
    /** An array of just the weapons that can be selected by the player and
     * bought in the weapons store. */
    static WeaponType sSelectableWeapons[];

    static {
        // Initialize sSelectableWeapons
        int i, numSelectableWeapons = 0;
        WeaponType weapons[] = WeaponType.values();
        for (WeaponType w : weapons) {
            // UNSELECTABLE weapons must also be UNBUYABLE
            assert ((w.getPrice() == Const.UNBUYABLE) ==
                    (w.getStartingAmount() == Const.UNSELECTABLE));
            if (w.getPrice() != Const.UNBUYABLE) {
                numSelectableWeapons++;
            }
        }
        sSelectableWeapons = new WeaponType[numSelectableWeapons];
        i = 0;
        for (WeaponType w : weapons) {
            if (w.getPrice() != Const.UNBUYABLE) {
                sSelectableWeapons[i] = w;
                i++;
            }
        }
    }

    static public WeaponListAdapter
            getWeaponListAdapter(Cosmos cosmos, Player player) {
        return new WeaponListAdapter(cosmos, player);
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

        /** Add another instance of this weapon to our armory */
        public void addWeapon(WeaponType weapon) {
            int amt = mV.mWeapons[weapon.ordinal()];
            if (amt < 0) {
                StringBuilder b = new StringBuilder(200);
                b.append("addWeapon: tried to add another ");
                b.append(weapon.getName());
                b.append(" to our armory, but we have ");
                b.append(amt);
                b.append(" of those already. This appears to be a ");
                b.append("restricted weapon type which we should ");
                b.append("not be able to buy.");
                throw new RuntimeException(b.toString());
            }
            mV.mWeapons[weapon.ordinal()] = amt + 1;
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
                v.mWeapons[i] = weapons[i].getStartingAmount();
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

    public static class WeaponListAdapter extends BaseAdapter
    {
        /*================= Data =================*/
        private PlayerInfo mPlayerInfo;

        /*================= Access =================*/
        public boolean areAllItemsEnabled() { return false; }

        public boolean isEnabled(int position) {
            return (sSelectableWeapons[position].getPrice() <=
                        mPlayerInfo.getCash());
        }

        public boolean areAllItemsSelectable() {
            return true;
        }

        public long getItemId(int position) { return position; }

        public int getCount() { return sSelectableWeapons.length; }

        public Object getItem(int position) {
            return sSelectableWeapons[position];
        }

        public View getView(int position, View convertView,
                            ViewGroup parent) {
            WeaponType weapon = sSelectableWeapons[position];
            Context c = parent.getContext();
            LinearLayout lay = null;
            TextView upper = null, lower = null;

            // This whole complicated rain-dance is to figure out if
            // we can reuse convertView for our purposes, or if we
            // need to create a new view.
            // The doxygen for BaseAdatper.getView() just hints darkly that
            // "you should check that this view is non-null and of an
            // appropriate type before using" so I don't really know
            // what kind of crazy garbage convertView might be.
            if (convertView != null) {
                if (convertView instanceof LinearLayout) {
                    LinearLayout ll = (LinearLayout)convertView;
                    if (ll.getChildCount() == 2) {
                        View u = ll.getChildAt(0);
                        View l = ll.getChildAt(1);
                        if ((u instanceof TextView) &&
                            (l instanceof TextView)) {
                            upper = (TextView)u;
                            lower = (TextView)l;
                            lay = ll;
                        }
                    }
                }
            }
            if (lay == null) {
                lay = new LinearLayout(c);
                upper = new TextView(c);
                lower = new TextView(c);
                lay.addView(upper);
                lay.addView(lower);
            }

            // Set up the layout
            lay.setOrientation(LinearLayout.VERTICAL);
            lay.setHorizontalGravity(Gravity.LEFT);

            // Set up upper view
            StringBuilder b = new StringBuilder(50);
            b.append(weapon.getPrice());
            b.append("            ");
            b.append(weapon.getName());
            upper.setText(b.toString());
            upper.setTextSize(TypedValue.COMPLEX_UNIT_MM, 2.5f);

            lower.setText(weapon.getDescription());
            lower.setTextSize(TypedValue.COMPLEX_UNIT_MM, 2.2f);

            if (isEnabled(position)) {
                upper.setTextColor(Color.argb(0xff, 0xff, 0xff, 0xff));
                lower.setTextColor(Color.argb(0xff, 0xff, 0xff, 0xff));
            }
            else {
                upper.setTextColor(Color.argb(0xff, 0xee, 0x22, 0x22));
                lower.setTextColor(Color.argb(0xff, 0xee, 0x22, 0x22));
            }

            return lay;
        }

        /*================= Lifecycle =================*/
        public WeaponListAdapter(Cosmos cosmos, Player player) {
            mPlayerInfo = cosmos.getPlayerInfo()[ player.getId() ];
        }
    }

    /*================= Data =================*/
    private final String mName;
    private final int mStartingAmount;
    private final ExplosionAttributes mExplosionAttributes;
    private final int mPrice;
    private final DetonationAttr mDetonationAttr;
    private final EnumSet<Attr> mAttrs;
    private final String mDescription;

    /*================= Access =================*/
    public String getName() {
        return mName;
    }

    public int getStartingAmount() {
        return mStartingAmount;
    }

    public int getPrice() {
        return mPrice;
    }

    public ExplosionAttributes getExplosionAttributes() {
        return mExplosionAttributes;
    }

    public String getDescription() {
        return mDescription;
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
                float init_power = 0;
                if (mAttrs.contains(Attr.EXTRA_LARGE)) {
                    clusterType = LARGE_MISSILE;
                    numFragments = 5;
                    init_power = Const.DOOMHAMMER_FRAG_INIT_POWER;
                }
                else if (mAttrs.contains(Attr.LARGE)) {
                    clusterType = CLUSTER_BOMB_PAYLOAD;
                    numFragments = 4;
                    init_power = Const.CLUSTER_BOMB_FRAG_INIT_POWER;
                }
                else {
                    throw new RuntimeException("wrong Attrs for MAKE_CLUSTER");
                }

                float terrainAngle = model.getTerrain().getTerrainAngle(x);
                float fragAngle = (float)(Math.PI / (numFragments + 1));
                for (int i = 0; i < numFragments; i++) {
                    float launchAngle =
                        (float)(Math.PI - terrainAngle -((i+1) * fragAngle));
                    float deltaX = init_power * (float)Math.cos(launchAngle);
                    float deltaY = -init_power * (float)Math.sin(launchAngle);
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
                       int price,
                       DetonationAttr detonationAttr,
                       EnumSet<Attr> attrs,
                       String description)
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
        mPrice = price;
        mDetonationAttr = detonationAttr;
        mAttrs = attrs;
        mDescription = description;
    }
}
