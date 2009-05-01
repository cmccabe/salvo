package com.senchas.salvo;

import com.senchas.salvo.Brain.*;

/**
 * Creates a Brain
 */
public enum BrainFactory {
    HUMAN("Human player", HumanBrain.class),
    EASY("Computer: Easy", EasyBrain.class),
    MEDIUM("Computer: Medium", MediumBrain.class),
    HARD("Computer: Hard", HardBrain.class);

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
