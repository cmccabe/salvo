package com.senchas.salvo;

import com.senchas.salvo.GameState.ComputerMoveState;
import com.senchas.salvo.GameState.HumanMoveState;
import com.senchas.salvo.ModelFactory.MyVars;
import android.os.Bundle;

/**
 * Controls a player.
 */
public abstract class Brain {
    /*================= Access =================*/
    public abstract GameState getMoveState();

    /*================= Operations =================*/
    public abstract void saveState(int index, Bundle map);

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
            return HumanMoveState.create();
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
