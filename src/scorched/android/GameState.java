package scorched.android;

import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;
import scorched.android.SalvoSlider.Listener;


/** Represents a state that the game can be in.
 *
 * Purpose
 * -------
 * Each GameState represents a state that the game can be in. GameState
 * objects all implement methods to handle user input and system events.
 *
 * GameState objects also implement much of the core game logic, like
 * deciding what to do next in the game.
 *
 * Each GameState object must be able to store itself in a Bundle with
 * saveState() and unpack itself with fromBundle().
 *
 * Locking
 * -------
 * Everything that GameState objects do is done under the RunGameThread lock.
 *
 * Memory management
 * -----------------
 * It would be easiest simply to use new() to create a new GameState each
 * time we transitioned to a new state. However, we want to minimize the
 * amount of times the garbage collector runs, because it is rather slow and
 * high-latency.
 *
 * So, GameState classes are singletons, stored in static data.
 * Instead of new(), each time we enter a new GameState we call initialize()
 * to set up the relevant private data.
 *
 * In android, it sometimes happens that an Activity is destroyed, but the
 * containing Application (and associated static data) is preserved.
 * If that static data holds references to the destroyed Activity,
 * the garbage collector will never finalize it.
 *
 * So, you must never hold a reference to an Activity in a GameState's
 * private data-- or else you will create a potential memory leak.
 *
 */
public abstract class GameState {
    /*================= Constants =================*/
    static final String GAME_STATE_ID = "GAME_STATE_ID";

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
        public static final long serialVersionUID = 1;
        public DomainException(String message) {
           super(message);
        }
     }

    /*================= Operations =================*/
    /** Pack this GameState into a Bundle.
     *
     * This method must set GAME_STATE_ID
     */
    public abstract void saveState(Bundle map);

    /** Unpack this GameState from a Bundle.
     *
     * This method should call the class-specific initializer
     */
    public void fromBundle(Bundle map) { }

    /** Called when we enter the state.
     */
    public void onEnter(RunGameActAccessor game) { }

    /** The function that will be executed for this state in the main event
     *  loop.
     *
     * @return          the next state, or null if we want to stay in this
     *                  state
     */
    public abstract GameState main(RunGameActAccessor game);

    /** Called when we exit the state.
     */
    public void onExit(RunGameActAccessor game) { }

    /** Returns the minimum of time that should elapse between calls to
     *  main(). If this is 0, we just block forever waiting for user input.
      */
    public abstract int getBlockingDelay();

    /** Called when the user presses a button
     *
     * @return  true if the main thread needs to be notified of a change
     */
    public boolean onButton(RunGameActAccessor game) {
        return false;
    }

    /** Handles a touchscreen event in the GameControlView part of the screen
     *
     * @return  true if the main thread needs to be notified of a change
     */
    public boolean onGameControlViewTouchEvent(MotionEvent me) { }

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
    //   |     | BallisticsState              |     |
    //   |     |                              |<----+
    //   |     | display missles flying       |
    //   |     | through the air              |
    //   |     |                              |
    //   |     +------------------------------+
    //   |                 |
    //   |                 V
    //   |     +------------------------------+
    //   |     | ExplosionState               |
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
        /*================= Constants =================*/
        public static final byte ID = 0;

        /*================= Operations =================*/
        public void saveState() {
            map.putByte(ID, GAME_STATE_ID);
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
        /*================= Constants =================*/
        public static final byte ID = 5;

        /*================= Operations =================*/
        public void saveState() {
            map.putByte(ID, GAME_STATE_ID);
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
        /*================= Constants =================*/
        public static final byte ID = 10;

        private static final int MAX_ANIMATION_STEP = 100;

        /*================= Operations =================*/
        public void saveState() {
            map.putByte(ID, GAME_STATE_ID);
        }

        public GameState main(Model model) {
            // Determine next game state
            int oldPlayer = model.getCurPlayerId();
            model.nextPlayer();
            int newPlayer = model.getCurPlayerId();
            if (newPlayer == Player.INVALID_PLAYER_ID) {
                // Everyone died. It was a draw.
                // TODO: display "it was a draw!" or similar
                return GameState.sLeaderBoardState;
            }
            else if (newPlayer == oldPlayer) {
                // Someone won the round.
                // TODO: display "foo wins" or similar
                return GameState.sLeaderBoardState;
            }
            else {
                return model.getCurPlayer().getPlayerGameState();
            }
        }

        public int getBlockingDelay() {
            return 10;
        }

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
        }

        private GameState mNextGameState;
    }

    /** A human turn. We will accept input from the touchscreen and do all
     * that stuff. */
    class HumanMoveState implements GameState {
        /*================= Constants =================*/
        public static final byte ID = 15;

        /*================= Operations =================*/
        public void saveState() {
            map.putByte(ID, GAME_STATE_ID);
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
            int myColor = curPlayer.getColor().toInt();

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
            return null; //(mFired) ? sBallisticsState : null;
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
                    //...
                }
                else {
                    //
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
            //mTouchViewSettings = new Graphics.ViewSettings(0,0,0);
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
        //private Graphics.ViewSettings mTouchViewSettings;

        /** Last X coordinate the user touched (in game coordinates) */
        private float mTouchX;

        /** Last Y coordinate the user touched (in game coordinates) */
        private float mTouchY;

        /** True if the user has just fired his weapon */
        private boolean mFired;
    }

    /** Draw missiles flying through the sky. The fun state. */
    class BallisticsState implements GameState {
        /*================= Constants =================*/
        public static final byte ID = 20;

        /*================= Operations =================*/
        public void saveState() {
            map.putByte(ID, GAME_STATE_ID);
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
        /*================= Constants =================*/
        public static final byte ID = 25;

        /*================= Operations =================*/
        public void saveState() {
            map.putByte(ID, GAME_STATE_ID);
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
    public static LeaderboardState
        sLeaderBoardState = new LeaderboardState();
    public static BuyWeaponsState
        sBuyWeaponsState = new BuyWeaponsState();
    public static TurnStartState
        sTurnStartState = new TurnStartState();
    public static HumanMoveState
        sHumanMoveState = new HumanMoveState();
    public ComputerMoveState
      sComputerMoveState = new ComputerMoveState();
    public static BallisticsState
        sBallisticsState = new BallisticsState();
    public static ExplosionState
        sExplosionState = new ExplosionState();

    /** Initialize and return a game state object from a Bundle */
    public static GameState fromBundle(Bundle map) {
        byte id = getByte(map, GAME_STATE_ID);
        switch (id) {
            case LeaderboardState.ID:
                return sLeaderBoardState.fromBundle(map);
            case BuyWeaponsState.ID:
                return sBuyWeaponsState.fromBundle(map);
            case TurnStartState.ID:
                return sTurnStartState.fromBundle(map);
            case HumanMoveState.ID:
                return sHumanMoveState.fromBundle(map);
            case ComputerMoveState.ID:
                return sComputerMoveState.fromBundle(map);
            case BallisticsState.ID:
                return sBallisticsState.fromBundle(map);
            case ExplosionState.ID:
                return sExplosionState.fromBundle(map);
        }
    }

    public static GameState createInitialGameState() {
        return sTurnStartState;
    }
}
