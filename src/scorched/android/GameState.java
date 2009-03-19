package scorched.android;

import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;
import scorched.android.SalvoSlider.Listener;


public interface GameState {
    /*================= Constants =================*/
    public final static String TAG = "GameState";

    /*================= Types =================*/
    enum GameButton {
        OK,
        DONE,
        FIRE,
        WEAP,
        ZOOM_IN,
        ZOOM_OUT,
    }

    public class DomainException extends RuntimeException {
        public final static long serialVersionUID = 1;
        public DomainException(String message) {
           super(message);
        }
     }

    /*================= Operations =================*/
    /** Returns the name of the state */
    public String toString();

    /** Called when we enter the state.
     * Under MainThread lock
     */
    public void onEnter(Model model,
                        SalvoSlider powerSlider, SalvoSlider angleSlider,
                        Listener powerAdaptor, Listener angleAdaptor);

    /** The function that will be executed for this state in the main event
     *  loop.
     *
     * @return          the next state, or null if we want to stay in this
     *                  state
     *
     * Under MainThread lock
     */
    public GameState main(Model model);

    /** Called when we exit the state.
     *
     * Under MainThread lock
     */
    public void onExit(SalvoSlider powerSlider,
                       SalvoSlider angleSlider);

    /** Returns the minimum of time that should elapse between calls to
     *  main(). If this is 0, we just block forever waiting for user input.
      */
    public int getBlockingDelay();

    /** Called when the user presses a button
     * Under MainThread lock
     *
     * @return  true if the main thread needs to be notified of a change
     */
    public boolean onButton(GameButton b);

    /** Called when the user moves a slider
     * Under MainThread lock
     *
     * @return  true if the main thread needs to be notified of a change
     */
    public boolean onSlider(Model model, boolean isPower, int val);

    /** Handles a touchscreen event
     * Under MainThread lock
     *
     * @return  true if the main thread needs to be notified of a change
     */
    public boolean onTouchEvent(MotionEvent me);

    /** Return true if you need to redraw the screen */
    public boolean needRedraw();

    /** Called after main() to redraw all or part of the screen.
      *
      * This would happen if the screen got resized, zoom level
      * changed, etc.
      *
      * Under MainThread lock
      */
    public void redraw(Canvas canvas, Model model);

    /*================= Game States =================*/
    //         +-------------------------+        no more rounds left
    //         | LeaderboardState        |--------------> game over
    //  force  |                         |
    //  draw   | show the leaderboard    |<---------+
    //  -----> |                         |          |
    //         +-------------------------+          |
    //                     | next round button      |
    //                     V                        |
    //         +------------------------------+     |
    //  new    | BuyWeaponsState              |     |
    //  game   |                              |     |
    //  -----> | allow humans to buy          |     |
    //         | weapons (1 at a time)        |     |
    //         | (comps buy too, silently)    |     |
    //         +------------------------------+     |
    //                     |                        |
    //                     V                        |
    //         +------------------------------+     |
    //         | TurnStartState               |     |
    //         |                              |     | No more valid players
    //   +---->| find next valid player       |-----+
    //   |     | (if any)                     |
    //   |  +->|                              |-----+
    //   |  |  +------------------------------+     |
    //   |  |              | next player is human   | next player is AI
    //   |  |              V                        V
    //   |  |  +------------------------------+  +------------------------+
    //   |  |  | HumanMoveState               |  | ComputerMoveState      |
    //   |  |  |                              |  |                        |
    //   |  |  | allow human to move his tank |  | control comes from     |
    //   |  +--| accept input from buttons +  |  | AI                     |
    //   | give| slider                       |  |                        |
    //   |  up +------------------------------+  +------------------------+
    //   |                 |                        |
    //   |                 V                        |
    //   |     +------------------------------+     |
    //   |     | Ballistics                   |     |
    //   |     |                              |<----+
    //   |     | display missles flying       |
    //   |     | through the air              |
    //   |     |                              |
    //   |     +------------------------------+
    //   |                 |
    //   |                 V
    //   |     +------------------------------+
    //   |     | Explosion                    |
    //   |     |                              |
    //   |     | display explosion            |
    //   |     | mutate terrain               |
    //   |     |                              |
    //   |     +------------------------------+
    //   |                 |
    //   +-----------------+
    //

    ///** Displays the leaderboard */
    class LeaderboardState implements GameState {
        public String toString() {
            return "LeaderboardState";
        }

        public void onEnter(Model model,
                            SalvoSlider powerSlider, SalvoSlider angleSlider,
                            Listener powerAdaptor, Listener angleAdaptor) {
            mFinished = false;

            // TODO: give a reward to the 'surviving' player
            // use: int newPlayer = model.getCurPlayerId();, etc.

            // TODO: set leaderboard layout...
            // TODO: set leaderboard menus...
        }

        public GameState main(Model model) {
            return (mFinished) ? sBuyWeaponsState : null;
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) { }

        public int getBlockingDelay() {
            return 0;
        }

        public boolean onButton(GameButton b) {
            if (b == GameButton.OK) {
                mFinished = true;
                return true;
            }
            else {
                return false;
            }
        }

        public boolean onSlider(Model model, boolean isPower, int val) {
            return false;
        }

        public boolean onTouchEvent(MotionEvent me) {
            return false;
        }

        public boolean needRedraw() { return false; }

        public void redraw(Canvas canvas, Model model) { }

        public LeaderboardState() { }

        private boolean mFinished;
    }

    /** Allows the user to buy weapons */
    class BuyWeaponsState implements GameState {
        public String toString() {
            return "BuyWeaponsState";
        }

        public void onEnter(Model model,
                            SalvoSlider powerSlider, SalvoSlider angleSlider,
                            Listener powerAdaptor, Listener angleAdaptor) {
            mFinished = false;
            // TODO: implement weapons layout...
            // TODO: implement weapons menus...
        }

        public GameState main(Model model) {
            return (mFinished) ? sBuyWeaponsState : null;
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) {
            // TODO: implement game layout
            // TODO: implement game menus
        }

        public int getBlockingDelay() {
            return 0;
        }

        public boolean onButton(GameButton b) {
            if (b == GameButton.DONE) {
                mFinished = true;
                return true;
            }
            else
                return false;
        }

        public boolean onSlider(Model model, boolean isPower, int val) {
            return false;
        }

        public boolean onTouchEvent(MotionEvent me) {
            return false;
        }

        public boolean needRedraw() { return false; }

        public void redraw(Canvas canvas, Model model) { }

        public BuyWeaponsState() { }

        private boolean mFinished;
    }

    /** The start of a turn. */
    class TurnStartState implements GameState {
        private final static int MAX_ANIMATION_STEP = 100;

        public String toString() {
            return "TurnStartState";
        }

        public void onEnter(Model model,
                            SalvoSlider powerSlider, SalvoSlider angleSlider,
                            Listener powerAdaptor, Listener angleAdaptor) {
            // Determine next game state
            int oldPlayer = model.getCurPlayerId();
            model.nextPlayer();
            int newPlayer = model.getCurPlayerId();
            if (newPlayer == Player.INVALID_PLAYER_ID) {
                // Everyone died. It was a draw.
                // TODO: display "it was a draw!" or similar
                mCurAnimationStep = MAX_ANIMATION_STEP;
                mNextGameState = GameState.sLeaderBoardState;
                return;
            }
            else if (oldPlayer == Player.INVALID_PLAYER_ID) {
                // If the previous player was "invalid player," that means
                // that this is the first turn that anyone has had.
                // Skip the intro animation.
                model.getCurPlayer().getIdealViewSettings(mInitViewSettings);
                Graphics.instance.setViewSettings(mInitViewSettings);
                mCurAnimationStep = MAX_ANIMATION_STEP;
            }
            else if (newPlayer == oldPlayer) {
                // There's only one player left!
                // That means he's the winner!
                // TODO: display "foo wins" or similar
                mCurAnimationStep = MAX_ANIMATION_STEP;
                mNextGameState = GameState.sLeaderBoardState;
                return;
            }
            else {
                // Do the animation
                mCurAnimationStep = 0;
            }

            Player curPlayer = model.getCurPlayer();
            Graphics.instance.getViewSettings(mInitViewSettings);
            curPlayer.getIdealViewSettings(mFinalViewSettings);
            mNextGameState = curPlayer.getGameState();
        }

        public GameState main(Model model) {
            if (mCurAnimationStep >= MAX_ANIMATION_STEP)
                return mNextGameState;
            mCurAnimationStep++;

            mCurViewSettings.interpolate(
                mInitViewSettings, mFinalViewSettings,
                mCurAnimationStep, MAX_ANIMATION_STEP);
            Graphics.instance.setViewSettings(mCurViewSettings);
            return null;
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) { }

        public int getBlockingDelay() {
            return 10;
        }

        public boolean onButton(GameButton b) { return false; }

        public boolean onSlider(Model model, boolean isPower, int val) {
            return false;
        }

        public boolean onTouchEvent(MotionEvent me) {
            return false;
        }

        public boolean needRedraw() {
            return true;
        }

        public void redraw(Canvas canvas, Model model) {
            Graphics.instance.drawScreen(canvas, model);
        }

        public TurnStartState() {
            mInitViewSettings = new Graphics.ViewSettings(0,0,0);
            mCurViewSettings = new Graphics.ViewSettings(0,0,0);
            mFinalViewSettings = new Graphics.ViewSettings(0,0,0);
        }

        private int mCurAnimationStep;

        Graphics.ViewSettings mInitViewSettings;

        Graphics.ViewSettings mCurViewSettings;

        Graphics.ViewSettings mFinalViewSettings;

        private GameState mNextGameState;
    }

    /** A human turn. We will accept input from the touchscreen and do all
     * that stuff. */
    class HumanMoveState implements GameState {
        public String toString() {
            return "HumanMoveState";
        }

        public void onEnter(Model model,
                            SalvoSlider powerSlider, SalvoSlider angleSlider,
                            Listener powerAdaptor, Listener angleAdaptor)
        {
            mNeedRedraw = true;
            mFingerDown = false;
            mTouchX = 0;
            mTouchY = 0;
            mFired = false;

            // Activate sliders
            // TODO: look up sliders by layout
            Player curPlayer = model.getCurPlayer();
            int myColor =
                Graphics.instance.getPlayerColor(curPlayer.getId());

            powerSlider.setState(SalvoSlider.SliderState.BAR,
                        powerAdaptor,
                        Player.MIN_POWER, Player.MAX_POWER,
                        curPlayer.getPower(),
                        myColor);
            angleSlider.setState(SalvoSlider.SliderState.ANGLE,
                        angleAdaptor,
                        Player.MAX_TURRET_ANGLE, Player.MIN_TURRET_ANGLE,
                        curPlayer.getAngleDeg(),
                        myColor);
        }

        public GameState main(Model model) {
            return (mFired) ? sBallisticsState : null;
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) {
            // Deactivate sliders
            // TODO: look up sliders by layout
            powerSlider.setState(SalvoSlider.SliderState.DISABLED,
                        null, 0, 0, 0, 0);
            angleSlider.setState(SalvoSlider.SliderState.DISABLED,
                        null, 0, 0, 0, 0);
        }

        public int getBlockingDelay() {
            return 0;
        }

        public boolean onButton(GameButton b) {
            switch (b) {
                case OK:
                    return false;
                case DONE:
                    return false;
                case WEAP:
                    // TODO: display the user's armory and allow him to
                    // choose another weapon.  Probably want to create a
                    // ListView programmatically and fiddle around with it.
                    // Maybe needs a new State to handle.
                    return false;
                case FIRE:
                    mFired = true;
                    return true;
                case ZOOM_IN:
                    if (Graphics.instance.userZoomIn()) {
                        mNeedRedraw = true;
                        return true;
                    }
                    else
                        return false;
                case ZOOM_OUT:
                    if (Graphics.instance.userZoomOut()) {
                        mNeedRedraw = true;
                        return true;
                    }
                    else
                        return false;
            }
            return false;
        }

        public boolean onSlider(Model model, boolean isPower, int val) {
            // TODO: have setPower and setAngleDeg return true only if the
            // value was changed
            if (isPower)
                model.getCurPlayer().setPower(val);
            else
                model.getCurPlayer().setAngleDeg(val);
            mNeedRedraw = true;
            return true;
        }

        public boolean onTouchEvent(MotionEvent me) {
            int action = me.getAction();
            boolean notify = false;
            if ((action == MotionEvent.ACTION_DOWN) ||
                (action == MotionEvent.ACTION_MOVE) ||
                (action == MotionEvent.ACTION_UP))
            {
                Graphics gfx = Graphics.instance;
                if (mFingerDown == false) {
                    mFingerDown = true;
                    gfx.getViewSettings(mTouchViewSettings);
                    mTouchX = gfx.onscreenXtoGameX
                                (me.getX(), mTouchViewSettings);
                    mTouchY = gfx.onscreenYtoGameY
                                (me.getY(), mTouchViewSettings);
                }
                else {
                    float x = gfx.onscreenXtoGameX
                                (me.getX(), mTouchViewSettings);
                    float y = gfx.onscreenYtoGameY
                                (me.getY(), mTouchViewSettings);
                    if (gfx.userScrollBy(mTouchX - x, -(mTouchY - y)))
                        notify = true;
                    mTouchX = x;
                    mTouchY = y;
                }
            }
            // TODO: do edgeflags?

            if (action == MotionEvent.ACTION_UP) {
                mFingerDown = false;
            }
            if (notify)
                mNeedRedraw = true;
            return notify;
        }

        public boolean needRedraw() {
            return mNeedRedraw;
        }

        public void redraw(Canvas canvas, Model model) {
            Graphics.instance.drawScreen(canvas, model);
            mNeedRedraw = false;
        }

        public HumanMoveState() {
            mNeedRedraw = true;
            mFingerDown = false;
            mTouchViewSettings = new Graphics.ViewSettings(0,0,0);
            mTouchX = 0;
            mTouchY = 0;
            mFired = false;
        }

        /** True if we need a screen refresh */
        private boolean mNeedRedraw;

        /** True while the user's finger is down on the touch screen */
        private boolean mFingerDown;

        /** The zoom, pan settings that were in effect when the user started
         * pressing on the screen */
        private Graphics.ViewSettings mTouchViewSettings;

        /** Last X coordinate the user touched (in game coordinates) */
        private float mTouchX;

        /** Last Y coordinate the user touched (in game coordinates) */
        private float mTouchY;

        /** True if the user has just fired his weapon */
        private boolean mFired;
    }

    /** Draw missiles flying through the sky. The fun state. */
    class BallisticsState implements GameState {
        public String toString() {
            return "BallisticsState";
        }

        public void onEnter(Model model,
                            SalvoSlider powerSlider, SalvoSlider angleSlider,
                            Listener powerAdaptor, Listener angleAdaptor) {
            Graphics gfx = Graphics.instance;
            Weapon wpn = Weapon.instance;

            model.getCurPlayer().fireWeapon();
            wpn.calculateTrajectory(model);

            float x[] = wpn.getX();
            float y[] = wpn.getY();
            int total = wpn.getTotalSamples();
            gfx.getEnclosingViewSettings
                    (x[0], y[0], x[total-1], y[total-1], 1,
                    mViewSettingsTemp);
            gfx.setViewSettings(mViewSettingsTemp);
            mCurSample = 0;
            // todo: zoom so that start and end points are both visible
        }

        public GameState main(Model model) {
            short nextSample = (short)(mCurSample + 1);
            if (nextSample >= Weapon.instance.getTotalSamples()) {
                return sExplosionState;
            }
            mCurSample = nextSample;
            //Log.w(TAG, "mCurSample = " + mCurSample);
            return null;
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) {
        }

        public int getBlockingDelay() {
            return 10;
        }

        public boolean onButton(GameButton b) { return false; }

        public boolean onSlider(Model model, boolean isPower, int val) {
            return false;
        }

        public boolean onTouchEvent(MotionEvent me) {
            return false;
        }

        public boolean needRedraw() {
            return true;
        }

        public void redraw(Canvas canvas, Model model) {
            Graphics gfx = Graphics.instance;
            gfx.drawScreen(canvas, model);
            gfx.drawTrajectory(canvas, model.getCurPlayer(), mCurSample);
        }

        public BallisticsState() {
            mViewSettingsTemp = new Graphics.ViewSettings(0,0,0);
        }

        Graphics.ViewSettings mViewSettingsTemp;

        short mCurSample;
    }

    /** Do explosions.
     * Subtract from players' life points if necessary.
     * Make craters if necessary.
     */
    class ExplosionState implements GameState {
        public String toString() {
            return "ExplosionState";
        }

        public void onEnter(Model model,
                            SalvoSlider powerSlider, SalvoSlider angleSlider,
                            Listener powerAdaptor, Listener angleAdaptor) {
            Weapon wpn = Weapon.instance;
            WeaponType wtp = wpn.getWeaponType();
            mMaxExplosionSize = wtp.getExplosionSize();
            mCurExplosionSize = 0;
            Graphics.instance.initializeExplosion();
            Sound.instance.playBoom(powerSlider.getContext().getApplicationContext());
        }

        public GameState main(Model model) {
            if (mCurExplosionSize > mMaxExplosionSize) {
                // TODO: explosion retreating animation
                Weapon wpn = Weapon.instance;
                model.doExplosion(wpn.getFinalX(), wpn.getFinalY(),
                                mMaxExplosionSize);
                return sTurnStartState;
            }
            mCurExplosionSize += 0.01;
            return null;
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) {
             // examine Weapon.instance to find out where the boom is,
             // then modify the terrain in the model
        }

        public int getBlockingDelay() {
            return 10;
        }

        public boolean onButton(GameButton b) { return false; }

        public boolean onSlider(Model model, boolean isPower, int val) {
            return false;
        }

        public boolean onTouchEvent(MotionEvent me) {
            return false;
        }

        public boolean needRedraw() {
            return true;
        }

        public void redraw(Canvas canvas, Model model) {
            Graphics gfx = Graphics.instance;
            gfx.drawScreen(canvas, model);
                // TODO: scrollBy view randomly to make it look
                // like it's shaking
            gfx.drawExplosion(canvas, model.getCurPlayer(),
                              mCurExplosionSize);
        }

        public ExplosionState() { }

        private float mMaxExplosionSize;

        private float mCurExplosionSize;
    }

    /*================= Static =================*/
    // We use static storage for the game states. This avoid dynamic memory
    // allocation. Be careful not to hold on to any important memory in the
    // states, though.
    public static LeaderboardState
        sLeaderBoardState = new LeaderboardState();
    public static BuyWeaponsState
        sBuyWeaponsState = new BuyWeaponsState();
    public static TurnStartState
        sTurnStartState = new TurnStartState();
    public static HumanMoveState
        sHumanMoveState = new HumanMoveState();
    //public ComputerMoveState
    //  sComputerMoveState = new ComputerMoveState();
    public static BallisticsState
        sBallisticsState = new BallisticsState();
    public static ExplosionState
        sExplosionState = new ExplosionState();

    /*================= Constants =================*/
    /*================= Types =================*/
    /*================= Operations =================*/
    /*================= Members =================*/
    /*================= Accessors =================*/
    /*================= Lifecycle =================*/
}
