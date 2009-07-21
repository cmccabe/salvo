package com.senchas.salvo;

import com.senchas.salvo.GameState.ComputerMoveState;
import com.senchas.salvo.GameState.HumanMoveState;
import com.senchas.salvo.ModelFactory.MyVars;
import com.senchas.salvo.RunGameAct.RunGameActAccessor;
import com.senchas.salvo.WeaponType.Armory;

import android.os.Bundle;
import android.util.Log;

/**
 * Controls a player.
 */
public abstract class Brain {
    // Each player has a Brain object which makes the decisions for that
    // player. Human players have a Brain object which is more or less a
    // placeholder and delegates everything to the human.
    //
    // In general, Brains make decisions in two contexts: while the game is
    // running, and while buying weapons.
    // Brains need information to make decisions, so here is a list of the
    // inputs that flow into the Brain objects of all players.
    //
    // In-game
    // =======
    // INPUTS
    // y movements of players
    //     if self, must adjust targetting
    //     if target, must adjust targetting
    // teleportations of players
    //     if self, must give up targetting
    //     if target, must give up targetting
    // missile collisions
    //     if near self, increase rage at shooter
    //     Possibly retarget at shooter
    //     Possibly take evasive action (teleport, earthmover, extra armor)
    //
    // OUTPUTS
    // Weapon to use for ComputerMoveState
    // If ballistic weapon, angle and power
    //
    // In weapon buying state
    // ======================
    // INPUTS
    // Number of players
    // Type of players
    // Amount of money left
    //
    // OUTPUTS
    // Weapons to buy
    //

    /*================= Types =================*/
    public static class ArmoryView {
        /*================= Constants =================*/
        public static final int TOTAL_PROB = 10000;

        public static final int INVALID_PROB = -1;

        /*================= Data =================*/
        /** Probability that a given weapon will be chosen, in terms of
         * 100ths of a percent.
         * If a given slot has a probability of -1, that means that that
         * weapon cannot be selected. */
        private int mProbs[];

        /*================= Access =================*/
        public void verifyStats() {
            // verify that all probabilities sum to TOTAL_PROB
            int sum = 0;
            for (int i = 0; i < mProbs.length; i++) {
                if (mProbs[i] != INVALID_PROB)
                    sum += mProbs[i];
            }
            if (sum != TOTAL_PROB) {
                logStats();
                throw new RuntimeException("verifyStats: " +
                        "probabilities must sum to TOTAL_PROB");
            }
        }

        public void logStats() {
            WeaponType values[] = WeaponType.values();
            StringBuilder b = new StringBuilder(80 * 20);
            int total = 0;
            for (int i = 0; i < mProbs.length; i++) {
                b.append("P[").append(values[i].getName()).append("]=");
                b.append(mProbs[i]).append(" ");
                if (mProbs[i] != INVALID_PROB)
                    total += mProbs[i];
            }
            b.append("total=").append(total);
            Log.w(this.getClass().getName(), b.toString());
        }

        /** Chooses a random weapon.
         *
         * The probability that any given weapon will be chosen is
         * linearly proportional to its mProbs entry.
         */
        public WeaponType getRandomWeapon() {
            verifyStats();
            int num = Util.mRandom.nextInt(TOTAL_PROB);
            int sum = 0;
            for (int i = 0; i < mProbs.length; i++) {
                if (mProbs[i] != INVALID_PROB)
                    sum += mProbs[i];
                if (sum > num)
                    return WeaponType.values()[i];
            }
            throw new RuntimeException("getRandomWeapon: " +
                    "probabilities must sum to TOTAL_PROB");
        }

        public int[] getProbs() {
            return mProbs;
        }

        /*================= Lifecycle =================*/
        public void initialize(Armory arm) {
            WeaponType weapons[] = WeaponType.values();
            for (int i = 0; i < weapons.length; i++) {
                int amount = arm.getAmount(weapons[i]);
                if ((amount == WeaponType.Const.UNLIMITED) || (amount > 0))
                    mProbs[i] = 0;
                else
                    mProbs[i] = INVALID_PROB;
            }
        }

        public ArmoryView() {
            mProbs = new int[WeaponType.values().length];
        }
    }

    /*================= Constants =================*/
    public static final int AGGRESSION_NOTIFICATION_DISTANCE = 20;

    /*================= Access =================*/

    /*================= Inputs =================*/
    /** Notify us that player 'playerId' has teleported. */
    public void notifyPlayerTeleported(int playerId)
    {}

    /** Notify us that a player has fallen
     *
     * @param perp              The player who caused the fall
     * @param victim            The player who fell
     */
    public void notifyPlayerFell(int perp, int victim)
    {}

    /** Notify us that player 'playerId' has shot near where we are.
     *
     * @param perp              The player who caused the explosion
     * @param distance          The distance from us
     * @param damagedUs         True if we were damaged in the explosion
     */
    public void notifyAggression(int perp,
                            float distance, boolean damagedUs)
    {}

    /*================= Outputs =================*/
    /** Make a move */
    public abstract void makeMove(RunGameActAccessor game, Move out);

    /*================= Operations =================*/
    public abstract void saveState(int index, Bundle map);

    /*================= Types =================*/
    /** Represents a move that the Brain wants to make.
     *
     * Move objects can be frozen using the saveState mechanism.
     * If move objects could *not* be frozen, and we were interrupted in the
     * middle of executing a computer move, we would have to call
     * curPlayer.getBrain().makeMove() again to get another move.
     * That would mean that makeMove() would not be able to have any side
     * effects, which seems a heavy burden to bear.
     *
     * So forgive the ugliness of having the clanking fromBundle() machinery
     * here-- it makes things nicer elsewhere.
     */
    public static class Move {
        /*================= Data =================*/
        public class MyVars {
            public boolean mIsHuman;
            public int mAngle;
            public int mPower;
            public WeaponType mWeapon;
        }
        private MyVars mV;

        /*================= Access =================*/
        public boolean isHuman() {
            return mV.mIsHuman;
        }

        public boolean isProjectile() {
            return mV.mWeapon.isProjectile();
        }

        public int getAngle() {
            if (mV.mIsHuman)
                throw new RuntimeException("getAngle: " +
                                    "not valid for human move");
            if (! mV.mWeapon.isProjectile())
                throw new RuntimeException("getAngle: not a " +
                                    "projectile firing move");
            return mV.mAngle;
        }

        public int getPower() {
            if (mV.mIsHuman)
                throw new RuntimeException("getPower: " +
                                    "not valid for human move");
            if (! mV.mWeapon.isProjectile())
                throw new RuntimeException("getPower: not a " +
                                    "projectile firing move");
            return mV.mPower;
        }

        public WeaponType getWeapon() {
            if (mV.mIsHuman)
                throw new RuntimeException("getWeapon: " +
                                    "not valid for human move");
            return mV.mWeapon;
        }

        /*================= Operations =================*/
        public void saveState(Bundle map) {
            AutoPack.autoPack(map, AutoPack.EMPTY_STRING, mV);
        }

        /*================= Lifecycle =================*/
        public void initializeAsHuman() {
            mV.mIsHuman = true;
            mV.mAngle = 0;
            mV.mPower = 0;
            mV.mWeapon = null;
        }

        public void initializeAsCpu(int angle, int power,
                                    WeaponType weapon) {
            mV.mIsHuman = false;
            mV.mAngle = angle;
            mV.mPower = power;
            mV.mWeapon = weapon;
        }

        public static Move fromBundle(Bundle map) {
            MyVars v = (MyVars)AutoPack.
                autoUnpack(map, AutoPack.EMPTY_STRING, MyVars.class);
            return new Move(v);
        }

        private Move(MyVars v) {
            mV = v;
        }

        public Move() {
            mV = new MyVars();
        }
    }


    /*================= Brains =================*/
    public static class HumanBrain extends Brain {
        /*================= Constants =================*/
        public static final short ID = 1;

        /*================= Static =================*/
        public static HumanBrain fromBundle(int index, Bundle map) {
            MyVars v = (MyVars)AutoPack.
                autoUnpack(map, Util.indexToString(index), MyVars.class);
            return new HumanBrain(v);
        }

        /*================= Data =================*/
        public static class MyVars {
        }
        private MyVars mV;

        /*================= Access =================*/

        /*================= Input =================*/

        /*================= Output =================*/
        public void makeMove(RunGameActAccessor game, Move out) {
            out.initializeAsHuman();
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            map.putShort(Util.indexToString(index, KEY_BRAIN_TYPE_ID), ID);
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /*================= Lifecycle =================*/
        public HumanBrain() {
            super();
            mV = new MyVars();
        }

        public HumanBrain(MyVars v) {
            super();
            mV = v;
        }
    }

    public static class SillyBrain extends Brain {
        /*================= Constants =================*/
        public static final short ID = 20;

        /*================= Static =================*/
        public static SillyBrain fromBundle(int index, Bundle map) {
            MyVars v = (MyVars)AutoPack.
                autoUnpack(map, Util.indexToString(index), MyVars.class);
            return new SillyBrain(v);
        }

        /*================= Data =================*/
        private ArmoryView mArmTmp;

        public static class MyVars {
        }
        private MyVars mV;

        /*================= Access =================*/

        /*================= Inputs =================*/

        /*================= Outputs =================*/
        /** Make a move */
        public void makeMove(RunGameActAccessor game, Move out) {
            Player curPlayer = game.getModel().getCurPlayer();
            int power = Util.mRandom.nextInt(Player.MAX_POWER);
            int angle = Util.mRandom.nextInt(Player.MAX_TURRET_ANGLE + 1);

            // Decide which weapon to choose
            Armory armory = curPlayer.getArmory(game.getCosmos());
            mArmTmp.initialize(armory);
            int probs[] = mArmTmp.getProbs();
            int validProbs = 0;
            for (int i = 0; i < probs.length; i++) {
                if (probs[i] != ArmoryView.INVALID_PROB)
                    validProbs++;
            }

            int share = ArmoryView.TOTAL_PROB / validProbs;
            int lastShare = ArmoryView.TOTAL_PROB -
                (share * (validProbs - 1));
            int v = 0;
            for (int i = 0; i < probs.length; i++) {
                if (probs[i] != ArmoryView.INVALID_PROB) {
                    if (v != (validProbs-1))
                        probs[i] = share;
                    else
                        probs[i] = lastShare;
                    v++;
                }
            }

            WeaponType weapon = mArmTmp.getRandomWeapon();
            out.initializeAsCpu(angle, power, weapon);
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            map.putShort(Util.indexToString(index, KEY_BRAIN_TYPE_ID), ID);
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /*================= Lifecycle =================*/
        public SillyBrain() {
            super();
            mV = new MyVars();
            mArmTmp = new ArmoryView();
        }

        public SillyBrain(MyVars v) {
            super();
            mV = v;
            mArmTmp = new ArmoryView();
        }
    }

    public static class EasyBrain extends Brain {
        /*================= Constants =================*/
        public static final short ID = 2;

        /*================= Static =================*/
        public static EasyBrain fromBundle(int index, Bundle map) {
            MyVars v = (MyVars)AutoPack.
                autoUnpack(map, Util.indexToString(index), MyVars.class);
            return new EasyBrain(v);
        }

        /*================= Data =================*/
        private ArmoryView mArmTmp;

        private Projectile mProjTmp;

        private int mArTmp[];

        public static class MyVars {
            /// Id of the player we're targeting now, or INVALID_PLAYER_ID if
            /// there is no such player.
            private int mTargetId;

            private int mAngle;

            private int mPower;
        }

        private MyVars mV;

        /*================= Utility =================*/
        // Get a random float from [minVal, maxVal].
        //
        // The results will have a distribution which is sort of a truncated
        // gaussian. Increasing "compression" will make the values farther
        // from minVal and maxVal.
        float getSkewedRandom(float minVal, float maxVal, float compression)
        {
            float r = (float)Util.mRandom.nextGaussian();
            r /= compression;
            if (r < -3f)
                return minVal;
            if (r > 3f)
                return maxVal;
            return ((r + 3f) * (maxVal - minVal)) / 6f;
        }

        // Simulate what firing with the given angle and power would do.
        // The result will be in mProjTmp.getCurX() and mProjTmp.getCurY()
        void computeImpact(RunGameActAccessor game, float angle, int power)
        {
            Projectile.launchProjectile(game.getModel(), angle,
                                    power, WeaponType.SMALL_MISSILE,
                                    mProjTmp);
            Model model = game.getModel();
            while (mProjTmp.getInUse()) {
                mProjTmp.step(model, null);
            }
        }

        // given four numbers, returns the index of the minimum one
        int getMinimumOfFour(int x0, int x1, int x2, int x3)
        {
            mArTmp[0] = x0; mArTmp[1] = x1; mArTmp[2] = x2; mArTmp[3] = x3;
            int minIdx = 0;
            for (int i = 1; i < 4; i++) {
                if (mArTmp[i] < mArTmp[minIdx])
                    minIdx = i;
            }
            return minIdx;
        }

        // Test some alternate shots and pick the best one.
        void refinementPass(RunGameActAccessor game, Player target)
        {
            float angleRad = (float)Math.toRadians(mV.mAngle);

            // Smaller angle shot.
            // Remember that we are dealing with angles in radians from
            // 0 to pi.
            float smallerAngle = getSkewedRandom
                (Player.MIN_TURRET_ANGLE_RAD, angleRad, 1.0f);
            computeImpact(game, smallerAngle, mV.mPower);
            int smallerAngleError =
                Math.abs((int)mProjTmp.getCurX() - target.getX());

            // Larger angle shot.
            float biggerAngle = getSkewedRandom
                (angleRad, Player.MAX_TURRET_ANGLE_RAD, 1.0f);
            computeImpact(game, biggerAngle, mV.mPower);
            int biggerAngleError =
                Math.abs((int)mProjTmp.getCurX() - target.getX());

            // Smaller power shot.
            int smallerPower = (int)getSkewedRandom
                (0, mV.mPower, 1.0f);
            computeImpact(game, angleRad, smallerPower);
            int smallerPowerError =
                Math.abs((int)mProjTmp.getCurX() - target.getX());

            // Bigger power shot.
            int biggerPower = (int)getSkewedRandom
                (mV.mPower, Player.MAX_POWER, 1.0f);
            computeImpact(game, angleRad, biggerPower);
            int biggerPowerError =
                Math.abs((int)mProjTmp.getCurX() - target.getX());

            // This switch statement is pretty clumsy, but at least it avoids
            // memory allocations.
            int minIdx = getMinimumOfFour(smallerAngleError,
                                          biggerAngleError,
                                          smallerPowerError,
                                          biggerPowerError);
            switch (minIdx) {
                case 0:
                	int smallerAngleDeg = (int)Math.toDegrees(smallerAngle);
                	StringBuilder b = new StringBuilder(80);
                	b.append("reducing angle to ");
                	b.append(smallerAngleDeg);
                    Log.w(this.getClass().getName(), b.toString());
                    mV.mAngle = smallerAngleDeg;
                    break;
                case 1:
                	int biggerAngleDeg = (int)Math.toDegrees(biggerAngle);
                	StringBuilder b2 = new StringBuilder(80);
                	b2.append("increasing angle to ");
                	b2.append(biggerAngleDeg);
                    Log.w(this.getClass().getName(), b2.toString());
                    mV.mAngle = biggerAngleDeg;
                    break;
                case 2:
                	StringBuilder b3 = new StringBuilder(80);
                	b3.append("reducing power to ");
                	b3.append(smallerPower);
                    Log.w(this.getClass().getName(), b3.toString());
                	mV.mPower = smallerPower;
                    break;
                case 3:
                	StringBuilder b4 = new StringBuilder(80);
                	b4.append("enlarging power to ");
                	b4.append(biggerPower);
                    Log.w(this.getClass().getName(), b4.toString());
                	mV.mPower = biggerPower;
                    break;
                default:
                    throw new RuntimeException("logic error in " +
                        "getMinimumOfFour: unknown return " + minIdx);
            }
        }

        /*================= Access =================*/

        /*================= Inputs =================*/
        public void notifyPlayerTeleported(int playerId) {
            if (playerId == mV.mTargetId) {
                StringBuilder b = new StringBuilder(80 * 2);
                b.append("notifyPlayerFell: we were targetting player ");
                b.append(playerId);
                b.append(", but he teleported away. Resetting mV.mTargetId.");
                Log.w(this.getClass().getName(), b.toString());

                mV.mTargetId = Player.INVALID_PLAYER_ID;
            }
        }

        public void notifyPlayerFell(int perp, int victim)
        {
            if (victim == mV.mTargetId) {
                StringBuilder b = new StringBuilder(80 * 2);
                b.append("notifyPlayerFell: we were targetting player ");
                b.append(victim);
                b.append(", but he was destroyed. Resetting mV.mTargetId.");
                Log.w(this.getClass().getName(), b.toString());

                mV.mTargetId = Player.INVALID_PLAYER_ID;
            }
        }

        /*================= Outputs =================*/

        /** Make a move */
        public void makeMove(RunGameActAccessor game, Move out) {
            Model model = game.getModel();
            Player curPlayer = model.getCurPlayer();
            Player players[] = model.getPlayers();
            Player target;

            if (mV.mTargetId == Player.INVALID_PLAYER_ID) {
                while (true) {
                    mV.mTargetId = Util.mRandom.nextInt(players.length);
                    if (mV.mTargetId != curPlayer.getId()) {
                        if (players[mV.mTargetId].isAlive())
                            break;
                    }
                }
                target = players[mV.mTargetId];

                StringBuilder b = new StringBuilder(80 * 2);
                b.append("acquired new target: player ");
                b.append(target.getName());
                Log.w(this.getClass().getName(), b.toString());

                mV.mAngle = Util.mRandom.nextInt(Player.MAX_TURRET_ANGLE);
                mV.mPower = Util.mRandom.nextInt(Player.MAX_POWER);
            }
            else {
                target = players[mV.mTargetId];
            }

            // Decide which weapon to choose
            Armory armory = curPlayer.getArmory(game.getCosmos());
            mArmTmp.initialize(armory);
            int probs[] = mArmTmp.getProbs();
            int validProbs = 0;
            for (int i = 0; i < probs.length; i++) {
                if (probs[i] != ArmoryView.INVALID_PROB)
                    validProbs++;
            }

            int share = ArmoryView.TOTAL_PROB / validProbs;
            int lastShare = ArmoryView.TOTAL_PROB -
                (share * (validProbs - 1));
            int v = 0;
            for (int i = 0; i < probs.length; i++) {
                if (probs[i] != ArmoryView.INVALID_PROB) {
                    if (v != (validProbs-1))
                        probs[i] = share;
                    else
                        probs[i] = lastShare;
                    v++;
                }
            }

            WeaponType weapon = mArmTmp.getRandomWeapon();

            if (weapon.isProjectile()) {
                refinementPass(game, target);
            }
            out.initializeAsCpu(mV.mAngle, mV.mPower, weapon);
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            map.putShort(Util.indexToString(index, KEY_BRAIN_TYPE_ID), ID);
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /*================= Lifecycle =================*/
        private void initializeTmp() {
            mArmTmp = new ArmoryView();
            mProjTmp = new Projectile();
            mArTmp = new int[4];
        }

        public EasyBrain() {
            super();
            mV = new MyVars();
            mV.mTargetId = Player.INVALID_PLAYER_ID;
            mV.mAngle = 0;
            mV.mPower = 1000;
            initializeTmp();
        }

        public EasyBrain(MyVars v) {
            super();
            mV = v;
            initializeTmp();
        }
    }

    public static class MediumBrain extends Brain {
        /*================= Constants =================*/
        public static final short ID = 3;

        /*================= Static =================*/
        public static MediumBrain fromBundle(int i, Bundle b) {
            return new MediumBrain();
        }

        /*================= Data =================*/
        public static class MyVars {
        }
        private MyVars mV;

        /*================= Access =================*/

        /*================= Inputs =================*/

        /*================= Outputs =================*/
        public void makeMove(RunGameActAccessor game, Move out) {
            out.initializeAsHuman();
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            map.putShort(Util.indexToString(index, KEY_BRAIN_TYPE_ID), ID);
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /*================= Lifecycle =================*/
        public MediumBrain() {
            super();
            mV = new MyVars();
        }

        public MediumBrain(MyVars v) {
            super();
            mV = v;
        }
    }


    public static class HardBrain extends Brain {
        /*================= Constants =================*/
        public static final short ID = 4;

        /*================= Static =================*/
        public static HardBrain fromBundle(int i, Bundle b) {
            return new HardBrain();
        }

        /*================= Data =================*/
        public static class MyVars {
        }
        private MyVars mV;

        /*================= Access =================*/

        /*================= Inputs =================*/

        /*================= Outputs =================*/
        public void makeMove(RunGameActAccessor game, Move out) {
            out.initializeAsHuman();
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            map.putShort(Util.indexToString(index, KEY_BRAIN_TYPE_ID), ID);
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /*================= Lifecycle =================*/
        public HardBrain() {
            super();
            mV = new MyVars();
        }

        public HardBrain(MyVars v) {
            super();
            mV = v;
        }
    }

    /*================= Static =================*/
    public static final String KEY_BRAIN_TYPE_ID = "BRAIN_TYPE_ID";

    /*================= Lifecycle =================*/
    public static Brain fromBundle(int i, Bundle b) {
        short brainTypeId =
            b.getShort(Util.indexToString(i, KEY_BRAIN_TYPE_ID));
        switch (brainTypeId) {
            case HumanBrain.ID:
                return HumanBrain.fromBundle(i, b);
            case SillyBrain.ID:
                return SillyBrain.fromBundle(i, b);
            case EasyBrain.ID:
                return EasyBrain.fromBundle(i, b);
            case MediumBrain.ID:
                return MediumBrain.fromBundle(i, b);
            case HardBrain.ID:
                return HardBrain.fromBundle(i, b);
            default:
                throw new RuntimeException("unknown brain type id: " +
                                            brainTypeId);
        }
    }

    protected Brain() { }
}
