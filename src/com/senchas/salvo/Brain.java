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

        /*================= Operations =================*/
        /** Set up a uniform random distribution of weapons */
        public void setUniformlyRandomProbs() 
        {
            int validProbs = 0;
            for (int i = 0; i < mProbs.length; i++) {
                if (mProbs[i] != ArmoryView.INVALID_PROB)
                    validProbs++;
            }

            int share = ArmoryView.TOTAL_PROB / validProbs;
            int lastShare = ArmoryView.TOTAL_PROB -
                (share * (validProbs - 1));
            int v = 0;
            for (int i = 0; i < mProbs.length; i++) {
                if (mProbs[i] != ArmoryView.INVALID_PROB) {
                    if (v != (validProbs-1))
                        mProbs[i] = share;
                    else
                        mProbs[i] = lastShare;
                    v++;
                }
            }
        }

        /** Given an array of probabilities where at least one is non-zero,
         * scale all entries so that the sum of all the entries becomes
         * exactly ArmoryView.TOTAL_PROB.  Note: We consistently round down
         * when scaling, and throw the difference in the final bucket. */
        public void normalize() {
            int totalProb = 0;
            for (int i = 0; i < probs.length; i++) {
                if (probs[i] != ArmoryView.INVALID_PROB)
                    totalProb += probs[i]; 
            }
            int v = 0;
            float multiplier = ArmoryView.TOTAL_PROB;
            multiplier /= totalProb;
            int count = 0;
            for (int i = 0; i < probs.length; i++) {
                if (probs[i] == ArmoryView.INVALID_PROB)
                    continue;
                else if (v != (validProbs-1))
                    probs[i] = ArmoryView.TOTAL_PROB - count;
                else {
                    probs[i] *= multiplier;
                    count += probs[i];
                }
                v++;
            }
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
        /** Get a list of weapons that are currently in our armory. */
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

        /** Get a list of buyable weapons whose cost is less than maxCost. */
        public void initialize(int maxCost) {
            WeaponType weapons[] = WeaponType.values();
            for (int i = 0; i < weapons.length; i++) {
                int cost = weapons[i].getCost();
                if (cost == Const.UNBUYABLE)
                    mProbs[i] = INVALID_PROB;
                else if (cost > maxCost)
                    mProbs[i] = INVALID_PROB;
                else
                    mProbs[i] = 0;
            }
        }

        public ArmoryView() {
            mProbs = new int[WeaponType.values().length];
        }
    }

    /*================= Constants =================*/
    public static final int AGGRESSION_NOTIFICATION_DISTANCE = 20;

    /*================= Access =================*/
    public abstract boolean isHuman();

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

    /** Buy weapons for the next round */
    public abstract void buyWeapons(Cosmos.PlayerInfo playerInfo);

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
        public boolean isHuman() {
            return true;
        }

        /*================= Input =================*/

        /*================= Output =================*/
        public void makeMove(RunGameActAccessor game, Move out) {
            out.initializeAsHuman();
        }

        public void buyWeapons(Cosmos.PlayerInfo playerInfo) {
            // Yes, it's lame to have methods-of-a-class that aren't
            // implemented.
            throw new RuntimeException("HumanBrain doesn't "
                "implement buyWeapons");
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

    public static class RandomBrain extends Brain {
        /*================= Constants =================*/
        public static final short ID = 2;

        /*================= Static =================*/
        public static RandomBrain fromBundle(int index, Bundle map) {
            MyVars v = (MyVars)AutoPack.
                autoUnpack(map, Util.indexToString(index), MyVars.class);
            return new RandomBrain(v);
        }

        /*================= Data =================*/
        private ArmoryView mArmTmp;

        public static class MyVars {
        }
        private MyVars mV;

        /*================= Access =================*/
        public boolean isHuman() {
            return false;
        }

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
            mArmTmp.setUniformlyRandomProbs();

            WeaponType weapon = mArmTmp.getRandomWeapon();
            out.initializeAsCpu(angle, power, weapon);
        }

        public void buyWeapons(Cosmos.PlayerInfo playerInfo) {
            // RandomBrain buys weapons... randomly
            int cash = playerInfo.getCash();
            Armory armory = playerInfo.getArmory();
            while (cash > WeaponType.sMinimumWeaponCost) {
                mArmTmp.initialize(cash);
                mArmTmp.setUniformlyRandomProbs();
                WeaponType weapon = mArmTmp.getRandomWeapon();
                armory.addWeapon(weapon);
                playerInfo.spendMoney(weapon.getPrice());
            }
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            map.putShort(Util.indexToString(index, KEY_BRAIN_TYPE_ID), ID);
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /*================= Lifecycle =================*/
        public RandomBrain() {
            super();
            mV = new MyVars();
            mArmTmp = new ArmoryView();
        }

        public RandomBrain(MyVars v) {
            super();
            mV = v;
            mArmTmp = new ArmoryView();
        }
    }

    public static class RefinementBrain extends Brain {
        /*================= Constants =================*/
        public static final int INVALID_ERROR = Integer.MAX_VALUE;

        /*================= Static =================*/

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
        private float getSkewedRandom(float minVal, float maxVal, int error)
        {
            if (error > 350) {
                // These results will have a distribution which is sort of a
                // truncated and reversed gaussian which emphasizes the
                // extremes.
                // This reflects the fact that our current fix is very bad.
                float r = (float)Util.mRandom.nextGaussian();
                if (r < -3f)
                    return minVal;
                if (r > 3f)
                    return maxVal;
                if (r < 0)
                    r = (-3.0f - r);
                if (r > 0)
                    r = (3.0f - r);
                r /= 6f;
                r += 0.5f;
                return (r * (maxVal - minVal)) + minVal;
            }
            if (error > 100) {
                // These results will be uniformly distributed and large.
                // This reflects the fact that we still don't have a good fix.
                float r = Util.mRandom.nextFloat();
                return (r * (maxVal - minVal)) + minVal;
            }
            else {
                // These results will have a distribution which is sort of a
                // truncated gaussian. This reflects the fact that we're already
                // doing pretty well and we want to be conservative.
                float r = (float)Util.mRandom.nextGaussian();
                if (r < -3f)
                    return minVal;
                if (r > 3f)
                    return maxVal;
                r /= 6f;
                r += 0.5f;
                return (r * (maxVal - minVal)) + minVal;
            }
        }

        // Simulate what firing with the given angle and power would do.
        // The result will be in mProjTmp.getCurX() and mProjTmp.getCurY()
        private void computeImpact(RunGameActAccessor game,
                                   float angle, int power)
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
        private int getMinimumOfFour(int x0, int x1, int x2, int x3)
        {
            mArTmp[0] = x0; mArTmp[1] = x1; mArTmp[2] = x2; mArTmp[3] = x3;
            int minIdx = 0;
            for (int i = 1; i < 4; i++) {
                if (mArTmp[i] < mArTmp[minIdx])
                    minIdx = i;
            }
            return minIdx;
        }

        // Computes the error between (tx, ty) and where mProjTmp
        // landed.
        private int computeError(int tx, int ty)
        {
            float px = mProjTmp.getCurX();
            float py = mProjTmp.getCurY();

            if ((px < 0) || (px > Terrain.MAX_X)) {
                // If our projectile ran into the edge of the screen,
                // include the Y error in the error metric.
                // If we don't do this, shooting at the edge of the screen (which is
                // a hard boundary) looks much more attractive to the AI than it really
                // should be.
                // It is not much use to explode your projectile at the edge of the
                // screen far up in the air.
                return (int)Math.sqrt(((py - ty) * (py - ty)) +
                                      ((px - tx) * (px - tx)));
            }
            else {
                return Math.abs((int)px - tx);
            }
        }

        // Test some alternate shots and pick the best one.
        // Returns the current error between the shot we're making and the target.
        protected int refinementPass(RunGameActAccessor game,
                                     Player target, int error)
        {
            int tx = target.getX();
            int ty = target.getY();
            float angleRad = (float)Math.toRadians(mV.mAngle);

            if (error == INVALID_ERROR) {
                computeImpact(game, angleRad, mV.mPower);
                error = computeError(tx, ty);
            }
            StringBuilder b = new StringBuilder(80);
            b.append("refinementPass: error = ");
            b.append(error);
            Log.w(this.getClass().getName(), b.toString());

            // Smaller angle shot.
            // Remember that we are dealing with angles in radians from
            // 0 to pi.
            float smallerAngle = getSkewedRandom
                (Player.MIN_TURRET_ANGLE_RAD, angleRad, error);
            computeImpact(game, smallerAngle, mV.mPower);
            int smallerAngleError = computeError(tx, ty);

            // Larger angle shot.
            float biggerAngle = getSkewedRandom
                (angleRad, Player.MAX_TURRET_ANGLE_RAD, error);
            computeImpact(game, biggerAngle, mV.mPower);
            int biggerAngleError = computeError(tx, ty);

            // Smaller power shot.
            int smallerPower = (int)getSkewedRandom
                (0, mV.mPower, error);
            computeImpact(game, angleRad, smallerPower);
            int smallerPowerError = computeError(tx, ty);

            // Bigger power shot.
            int biggerPower = (int)getSkewedRandom
                (mV.mPower, Player.MAX_POWER, error);
            computeImpact(game, angleRad, biggerPower);
            int biggerPowerError = computeError(tx, ty);

            // This switch statement is pretty clumsy, but at least it avoids
            // memory allocations.
            int minIdx = getMinimumOfFour(smallerAngleError,
                                          biggerAngleError,
                                          smallerPowerError,
                                          biggerPowerError);
            switch (minIdx) {
                case 0:
                    int smallerAngleDeg = (int)Math.toDegrees(smallerAngle);
                    StringBuilder b1 = new StringBuilder(80);
                    b1.append("reducing angle to ");
                    b1.append(smallerAngleDeg);
                    b1.append(" to get an error of ");
                    b1.append(smallerAngleError);
                    Log.w(this.getClass().getName(), b1.toString());
                    mV.mAngle = smallerAngleDeg;
                    return smallerAngleError;
                case 1:
                    int biggerAngleDeg = (int)Math.toDegrees(biggerAngle);
                    StringBuilder b2 = new StringBuilder(80);
                    b2.append("increasing angle to ");
                    b2.append(biggerAngleDeg);
                    b2.append(" to get an error of ");
                    b2.append(biggerAngleError);
                    Log.w(this.getClass().getName(), b2.toString());
                    mV.mAngle = biggerAngleDeg;
                    return biggerAngleError;
                case 2:
                    StringBuilder b3 = new StringBuilder(80);
                    b3.append("reducing power to ");
                    b3.append(smallerPower);
                    b3.append(" to get an error of ");
                    b3.append(smallerPowerError);
                    Log.w(this.getClass().getName(), b3.toString());
                    mV.mPower = smallerPower;
                    return smallerPowerError;
                case 3:
                    StringBuilder b4 = new StringBuilder(80);
                    b4.append("enlarging power to ");
                    b4.append(biggerPower);
                    b4.append(" to get an error of ");
                    b4.append(biggerPowerError);
                    Log.w(this.getClass().getName(), b4.toString());
                    mV.mPower = biggerPower;
                    return biggerPowerError;
                default:
                    throw new RuntimeException("logic error in " +
                        "getMinimumOfFour: unknown return " + minIdx);
            }
        }

        /*================= Access =================*/
        public boolean isHuman() {
            return false;
        }

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
                refinementPass(game, target, INVALID_ERROR);
            }
            out.initializeAsCpu(mV.mAngle, mV.mPower, weapon);
        }

        public void buyWeapons(Cosmos.PlayerInfo playerInfo) {
            // RefinementBrain tries to have roughly equal numbers of each
            // type of weapon
            int cash = playerInfo.getCash();
            Armory armory = playerInfo.getArmory();
            WeaponType weapons[] = WeaponType.values();
            while (cash > WeaponType.sMinimumWeaponCost) {
                mArmTmp.initialize(cash);
                int probs[] = mArmTmp.getProbs();
                
                int validProbs = 0;
                int totalWeapons = 0;
                for (int i = 0; i < probs.length; i++) {
                    if (probs[i] != ArmoryView.INVALID_PROB) {
                        validProbs++;
                        totalWeapons += armory.getAmount(weapons[i]);
                    }
                }
                if (totalWeapons == 0) {
                    mArmTmp.setUniformlyRandomProbs();
                }
                else {
                    int cur = 0;
                    for (int i = 0; i < probs.length; i++) {
                        if (probs[i] == ArmoryView.INVALID_PROB)
                            continue;
                        probs[i] = ArmoryView.TOTAL_PROB *
                            (totalWeapons - armory.getAmount(weapons[i]));
                    }

                    mArmTmp.normalize();
                }

                WeaponType weapon = mArmTmp.getRandomWeapon();
                armory.addWeapon(weapon);
                playerInfo.spendMoney(weapon.getPrice());
            }
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /*================= Lifecycle =================*/
        private void initializeTmp() {
            mArmTmp = new ArmoryView();
            mProjTmp = new Projectile();
            mArTmp = new int[4];
        }

        public RefinementBrain() {
            super();
            mV = new MyVars();
            mV.mTargetId = Player.INVALID_PLAYER_ID;
            mV.mAngle = 0;
            mV.mPower = 1000;
            initializeTmp();
        }

        public RefinementBrain(MyVars v) {
            super();
            mV = v;
            initializeTmp();
        }
    }

    public static class MediumBrain extends RefinementBrain {
        /*================= Constants =================*/
        public static final short ID = 3;

        /*================= Static =================*/
        public static MediumBrain fromBundle(int index, Bundle map) {
            RefinementBrain.MyVars refineV =
                (RefinementBrain.MyVars)AutoPack.
                    autoUnpack(map, Util.indexToString(index),
                        RefinementBrain.class);
            MyVars v = (MyVars)AutoPack.
                autoUnpack(map, Util.indexToString(index), MyVars.class);
            return new MediumBrain(refineV, v);
        }

        /*================= Data =================*/
        public static class MyVars {
        }

        private MyVars mV;

        /*================= Utility =================*/

        /*================= Outputs =================*/
        /** Make a move */
        public void makeMove(RunGameActAccessor game, Move out) {
            super.makeMove(game, out);
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            super.saveState(index, map);
            map.putShort(Util.indexToString(index, KEY_BRAIN_TYPE_ID), ID);
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /*================= Lifecycle =================*/
        public MediumBrain(RefinementBrain.MyVars refineV, MyVars v) {
            super(refineV);
            mV = v;
        }

        public MediumBrain() {
            super();
            mV = new MyVars();
        }
    }

    public static class HardBrain extends RefinementBrain {
        /*================= Constants =================*/
        public static final short ID = 4;

        /*================= Static =================*/
        public static HardBrain fromBundle(int index, Bundle map) {
            RefinementBrain.MyVars refineV =
                (RefinementBrain.MyVars)AutoPack.
                    autoUnpack(map, Util.indexToString(index),
                        RefinementBrain.class);
            MyVars v = (MyVars)AutoPack.
                autoUnpack(map, Util.indexToString(index), MyVars.class);
            return new HardBrain(refineV, v);
        }

        /*================= Data =================*/
        public static class MyVars {
        }

        private MyVars mV;

        /*================= Utility =================*/

        /*================= Outputs =================*/
        /** Make a move */
        public void makeMove(RunGameActAccessor game, Move out) {
            super.makeMove(game, out);
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            super.saveState(index, map);
            map.putShort(Util.indexToString(index, KEY_BRAIN_TYPE_ID), ID);
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /*================= Lifecycle =================*/
        public HardBrain(RefinementBrain.MyVars refineV, MyVars v) {
            super(refineV);
            mV = v;
        }

        public HardBrain() {
            super();
            mV = new MyVars();
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
            case RandomBrain.ID:
                return RandomBrain.fromBundle(i, b);
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
