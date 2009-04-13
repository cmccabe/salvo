package scorched.android;

import scorched.android.Brain.*;

/**
 * Creates a Brain
 */
public enum BrainFactory {
    HUMAN("Human player", HumanBrain.class),
    EASY("Computer: Easy", EasyBrain.class),
    MEDIUM("Computer: Medium", MediumBrain.class),
    HARD("Computer: Hard", HardBrain.class);

    /*================= Data =================*/
    private String mName;

    @SuppressWarnings("unchecked")
    private Class mClass;

    /*================= Access =================*/
    public String toString() {
        return mName;
    }

    public Brain toBrain() {
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
