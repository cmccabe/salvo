package com.senchas.salvo;

import com.senchas.salvo.GameState.ComputerMoveState;
import com.senchas.salvo.GameState.HumanMoveState;
import com.senchas.salvo.ModelFactory.MyVars;
import android.os.Bundle;

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

    /*================= Constants =================*/
    public static final int AGGRESSION_NOTIFICATION_DISTANCE = 20;

    /*================= Access =================*/
    public abstract GameState getMoveState();

    /*================= Operations =================*/
    public abstract void saveState(int index, Bundle map);

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
        public GameState getMoveState() {
            return HumanMoveState.create();
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
        public static class MyVars {
        }
        private MyVars mV;

        /*================= Access =================*/
        public GameState getMoveState() {
            return HumanMoveState.create();
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
        }

        public SillyBrain(MyVars v) {
            super();
            mV = v;
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
        public static class MyVars {
        }
        private MyVars mV;

        /*================= Access =================*/
        public GameState getMoveState() {
            return HumanMoveState.create();
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            map.putShort(Util.indexToString(index, KEY_BRAIN_TYPE_ID), ID);
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        /*================= Lifecycle =================*/
        public EasyBrain() {
            super();
            mV = new MyVars();
        }

        public EasyBrain(MyVars v) {
            super();
            mV = v;
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
        public GameState getMoveState() {
            return HumanMoveState.create();
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
        public GameState getMoveState() {
            return ComputerMoveState.create();
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
