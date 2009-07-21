package com.senchas.salvo;

import android.util.Log;

import com.senchas.salvo.Brain.*;

/**
 * Creates a Brain
 */
public enum BrainFactory {
    HUMAN("Human player", HumanBrain.class),
    RANDOM("Random Computer", RandomBrain.class),
    MEDIUM("Medium Computer", MediumBrain.class),
    HARD("Hard Computer", HardBrain.class);

    /*================= Data =================*/
    private final String mName;

    @SuppressWarnings("unchecked")
    private final Class mClass;

    /*================= Access =================*/
    public String toString() {
        return mName;
    }

    public Brain createBrain() {
        try {
            return (Brain)mClass.newInstance();
        }
        catch (Exception e) {
            Log.w(this.getClass().toString(),
                  "createBrain: newInstance failed: " + e.toString());
            return null;
        }
    }

    public boolean isHuman() {
        return (this == HUMAN);
    }

    /*================= Lifecycle =================*/
    @SuppressWarnings("unchecked")
    private BrainFactory(String n, Class c) {
        mName = n;
        mClass = c;
    }
}
