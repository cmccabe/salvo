package scorched.android;

import java.util.HashMap;

public abstract class WeaponType {
    /*================= Constants =================*/
    public final static String TAG = "WeaponType";

    public final static int UNLIMITED = -1;

    /*================= Types =================*/

    /*================= Access =================*/
    /** Returns the name of the weapon */
    public abstract String getName();

    /** Returns the size of the explosion
      */
    public abstract float getExplosionSize();

    /** Returns the number of weapons of this type initially in the player's
     * inventory */
    public abstract int initialAmount();

    /*================= Static =================*/
    //public static EmergencyTeleport
        //sEmergencyTeleport = new EmergencyTeleport();
    public static WeaponType
        sBabyMissile = new BabyMissile();
    public static WeaponType
        sMediumMissile = new MediumMissile();
    public static LargeMissile
        sLargeMissile = new LargeMissile();
    //public static NuclearMissile
        //sNuclearMissile = new NuclearMissile();

    /*================= Data =================*/
    private static HashMap < String, WeaponType >
        sNameToWeaponType = new HashMap < String, WeaponType >();

    /*================= Lifecycle =================*/
    public WeaponType(String name) {
        //sNameToWeaponType.put(name, this);
    }

    /*================= Weapons =================*/
    static class BabyMissile extends WeaponType {
        private final static String NAME = "BabyMissile";
        public String getName() { return NAME; }
        public float getExplosionSize() { return 1.0f; }
        public int initialAmount() { return UNLIMITED; }
        protected BabyMissile() { super(NAME); }
    }

    static class MediumMissile extends WeaponType {
        private final static String NAME = "MediumMissile";
        public String getName() { return NAME; }
        public float getExplosionSize() { return 2.0f; }
        public int initialAmount() { return 1; }
        protected MediumMissile() { super(NAME); }
    }

    static class LargeMissile extends WeaponType {
        private final static String NAME = "LargeMissile";
        public String getName() { return NAME; }
        public float getExplosionSize() { return 4.0f; }
        public int initialAmount() { return 0; }
        protected LargeMissile() { super(NAME); }
    }

    /*================= Constants =================*/
    /*================= Types =================*/
    /*================= Operations =================*/
    /*================= Members =================*/
    /*================= Accessors =================*/
    /*================= Lifecycle =================*/
}
