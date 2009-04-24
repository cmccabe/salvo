package scorched.android;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import scorched.android.RunGameAct.RunGameActAccessor;
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
        ARMORY_LEFT,
        ARMORY_RIGHT,
        OK,
        PRESS_FIRE,
        RELEASE_FIRE
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

    /** Called when we enter the state.
     *
     * Any "side effects" to entering the state should be performed here,
     * rather than in the initialization function.
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
    public boolean onButton(RunGameActAccessor game, GameButton b) {
        return false;
    }

    /** Handles a touchscreen event in the GameControlView part of the screen
     *
     * @return  true if the main thread needs to be notified of a change
     */
    public boolean onTouchEvent(RunGameActAccessor game, MotionEvent me) {
        return false;
    }

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
    public static class LeaderboardState extends GameState {
        /*================= Constants =================*/
        public static final byte ID = 0;

        /*================= Static =================*/
        private static LeaderboardState sMe = new LeaderboardState();

        /*================= Data =================*/
        private boolean mFinished;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        public void onEnter(RunGameActAccessor game) {
            // TODO: give a reward to the 'surviving' player
            // use: int newPlayer = model.getCurPlayerId();, etc.

            // TODO: set leaderboard layout...
            // TODO: set leaderboard menus...
        }

        public GameState main(RunGameActAccessor game) {
            if (mFinished) {
                return BuyWeaponsState.create();
            }
            else {
                return null;
            }
        }

        public int getBlockingDelay() {
            return 0;
        }

        public boolean onButton(RunGameActAccessor game, GameButton b) {
            if (b == GameButton.OK) {
                mFinished = true;
                return true;
            }
            else {
                return false;
            }
        }

        /*================= Lifecycle =================*/
        private void initialize() {
            mFinished = false;
        }

        public static LeaderboardState create() {
            sMe.initialize();
            return sMe;
        }

        public static LeaderboardState createFromBundle(Bundle map) {
            sMe.initialize();
            return sMe;
        }

        private LeaderboardState() { }
    }

    /** Allows the user to buy weapons */
    public static class BuyWeaponsState extends GameState {
        /*================= Constants =================*/
        public static final byte ID = 5;

        /*================= Static =================*/
        private static BuyWeaponsState sMe = new BuyWeaponsState();

        /*================= Data =================*/
        private boolean mFinished;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        public void onEnter(RunGameActAccessor game) {
            mFinished = false;
            // TODO: implement weapons layout...
            // TODO: implement weapons menus...
        }

        public GameState main(RunGameActAccessor game) {
            if (mFinished) {
                return BuyWeaponsState.create();
            }
            else {
                return null;
            }
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) {
            // TODO: implement game layout
            // TODO: implement game menus
        }

        public int getBlockingDelay() {
            return 0;
        }

        public boolean onButton(RunGameActAccessor game, GameButton b) {
            if (b == GameButton.OK) {
                mFinished = true;
                return true;
            }
            else {
                return false;
            }
        }

        /*================= Lifecycle =================*/
        private void initialize() {
            mFinished = false;
        }

        public static BuyWeaponsState create() {
            sMe.initialize();
            return sMe;
        }

        public static BuyWeaponsState createFromBundle(Bundle map) {
            sMe.initialize();
            return sMe;
        }

        private BuyWeaponsState() { }
    }

    /** The start of a turn. */
    public static class TurnStartState extends GameState {
        /*================= Constants =================*/
        public static final byte ID = 10;

        /*================= Types =================*/
        private static class DoToast implements Runnable {
            private Context mContext;
            private String mString;

            /*================= Operations =================*/
            public void run() {
                Toast toast = Toast.makeText(mContext, mString, 30);
                toast.setGravity(Gravity.TOP, 0, 30);
                toast.show();
            }

            /*================= Lifecycle =================*/
            public DoToast(Context context, String string) {
                mContext = context;
                mString = string;
            }
        }

        /*================= Static =================*/
        private static TurnStartState sMe = new TurnStartState();

        /*================= Data =================*/
        private Model.NextTurnInfo mInfo;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        public void onEnter(RunGameActAccessor game) {
            game.getModel().getNextPlayerInfo(mInfo);
            // TODO: set location of floating arrow thing to current player
        }

        public GameState main(RunGameActAccessor game) {
            if (mInfo.isDraw()) {
                // TODO: display "it was a draw!" or similar
                return LeaderboardState.create();
            }
            else if (mInfo.curPlayerHasWon()) {
                // Someone won the round.
                // TODO: display "foo wins" or similar
                // TODO: add gold to account, or whatever
                return LeaderboardState.create();
            }
            else {
                Model model = game.getModel();
                int nextPlayerId = mInfo.getNextPlayerId();
                Player play = model.getPlayers()[nextPlayerId];
                DoToast doToast = new DoToast(
                    game.getGameControlView().getContext(),
                    play.getIntroductionString());
                game.getRunGameAct().runOnUiThread(doToast);

                model.setCurPlayerId(mInfo.getNextPlayerId());
                return play.getGameState();
            }
        }

        public int getBlockingDelay() {
            throw new RuntimeException("unreachable");
        }

        /*================= Lifecycle =================*/
        private void initialize() {
        }

        public static TurnStartState create() {
            sMe.initialize();
            return sMe;
        }

        public static TurnStartState createFromBundle(Bundle map) {
            sMe.initialize();
            return sMe;
        }

        private TurnStartState() {
            mInfo = new Model.NextTurnInfo();
        }
    }

    /** A human turn. We will accept input from the touchscreen and do all
     * that stuff. */
    public static class HumanMoveState extends GameState {
        /*================= Constants =================*/
        public static final byte ID = 15;

        /*================= Static =================*/
        private static HumanMoveState sMe = new HumanMoveState();

        /*================= Data =================*/
//        /** True if we need a screen refresh */
//        private boolean mNeedRedraw;
//
//        /** True while the user's finger is down on the touch screen */
//        private boolean mFingerDown;
//
//        /** The zoom, pan settings that were in effect when the user started
//         * pressing on the screen */
//        //private Graphics.ViewSettings mTouchViewSettings;
//
//        /** Last X coordinate the user touched (in game coordinates) */
//        private float mTouchX;
//
//        /** Last Y coordinate the user touched (in game coordinates) */
//        private float mTouchY;
//
//        /** True if the user has just fired his weapon */
//        private boolean mFired;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        public void onEnter(RunGameActAccessor game) {
//            mNeedRedraw = true;
//            mFingerDown = false;
//            mTouchX = 0;
//            mTouchY = 0;
//            mFired = false;
//
//            // Activate sliders
//            // TODO: look up sliders by layout
//            Player curPlayer = model.getCurPlayer();
//            int myColor = curPlayer.getColor().toInt();
//
//            powerSlider.setState(SalvoSlider.SliderState.BAR,
//                        powerAdaptor,
//                        Player.MIN_POWER, Player.MAX_POWER,
//                        curPlayer.getPower(),
//                        myColor);
//            angleSlider.setState(SalvoSlider.SliderState.ANGLE,
//                        angleAdaptor,
//                        Player.MAX_TURRET_ANGLE, Player.MIN_TURRET_ANGLE,
//                        curPlayer.getAngleDeg(),
//                        myColor);
        }

        public GameState main(RunGameActAccessor game) {
            game.getGameControlView().drawScreen(game);
            return null; //(mFired) ? sBallisticsState : null;
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) {
            // TODO: grey out buttons and whatnot
        }

        public int getBlockingDelay() {
            return 0;
        }

        public boolean onTouchEvent(RunGameActAccessor game, MotionEvent me) {
            // TODO: implement touch-to-angle

            /*int action = me.getAction();
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
            return notify;*/

            return false;
        }

        /*================= Lifecycle =================*/
        private void initialize() {
            //mNeedRedraw = true;
            //mFingerDown = false;
            //mTouchViewSettings = new Graphics.ViewSettings(0,0,0);
            //mTouchX = 0;
            //mTouchY = 0;
            //mFired = false;
        }

        public static HumanMoveState create() {
            sMe.initialize();
            return sMe;
        }

        public static HumanMoveState createFromBundle(Bundle map) {
            sMe.initialize();
            return sMe;
        }

        public HumanMoveState() { }
    }

    /** Draw missiles flying through the sky. The fun state. */
    public static class BallisticsState extends GameState {
        /*================= Constants =================*/
        public static final byte ID = 20;

        /*================= Static =================*/
        private static BallisticsState sMe = new BallisticsState();

        /*================= Data =================*/
        //private short mCurSample;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        public void onEnter(RunGameActAccessor game) {
            /*
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
            */
        }

        public GameState main(RunGameActAccessor game) {
            return null;
        }

        public int getBlockingDelay() {
            return 10;
        }

        /*================= Lifecycle =================*/
        private void initialize() {
        }

        public static BallisticsState create() {
            sMe.initialize();
            return sMe;
        }

        public static BallisticsState createFromBundle(Bundle map) {
            sMe.initialize();
            return sMe;
        }

        private BallisticsState() { }
    }

    /** Do explosions.
     * Subtract from players' life points if necessary.
     * Make craters if necessary.
     */
    public static class ExplosionState extends GameState {
        /*================= Constants =================*/
        public static final byte ID = 25;

        /*================= Static =================*/
        private static ExplosionState sMe = new ExplosionState();

        /*================= Data =================*/
        //private float mMaxExplosionSize;
        //private float mCurExplosionSize;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        public void onEnter(RunGameActAccessor game) {
//            Weapon wpn = Weapon.instance;
//            WeaponType wtp = wpn.getWeaponType();
//            mMaxExplosionSize = wtp.getExplosionSize();
//            mCurExplosionSize = 0;
//            Graphics.instance.initializeExplosion();
//            Sound.instance.playBoom(
//                powerSlider.getContext().getApplicationContext());
        }

        public GameState main(RunGameActAccessor game) {
//            if (mCurExplosionSize > mMaxExplosionSize) {
//                // TODO: explosion retreating animation
//                Weapon wpn = Weapon.instance;
//                model.doExplosion(wpn.getFinalX(), wpn.getFinalY(),
//                                mMaxExplosionSize);
//                return sTurnStartState;
//            }
//            mCurExplosionSize += 0.01;
            return null;
        }

        public void onExit(RunGameActAccessor game) {
             // examine Weapon.instance to find out where the boom is,
             // then modify the terrain in the model
        }

        public int getBlockingDelay() {
            return 10;
        }

        /*================= Lifecycle =================*/
        private void initialize() {
        }

        public static ExplosionState create() {
            sMe.initialize();
            return sMe;
        }

        public static ExplosionState createFromBundle(Bundle map) {
            sMe.initialize();
            return sMe;
        }

        private ExplosionState() { }
    }

    /*================= Static =================*/
    /** Initialize and return a game state object from a Bundle */
    public static GameState fromBundle(Bundle map) {
        byte id = map.getByte(GAME_STATE_ID);
        switch (id) {
            case LeaderboardState.ID:
                return LeaderboardState.createFromBundle(map);
            case BuyWeaponsState.ID:
                return BuyWeaponsState.createFromBundle(map);
            case TurnStartState.ID:
                return TurnStartState.createFromBundle(map);
            case HumanMoveState.ID:
                return HumanMoveState.createFromBundle(map);
            //case ComputerMoveState.ID:
            //    return ComputerMoveState.createFromBundle(map);
            case BallisticsState.ID:
                return BallisticsState.createFromBundle(map);
            case ExplosionState.ID:
                return ExplosionState.createFromBundle(map);
            default:
                throw new RuntimeException("can't recognize state with ID = "
                                            + id);
        }
    }

    public static GameState createInitialGameState() {
        return TurnStartState.create();
    }
}

// HOWTO: draw stuff on the canvas
                        // redraw whatever needs to be redrawn
//                        if (mState.needRedraw()) {
//                            Canvas canvas = null;
//                            try {
//                                canvas = mSurfaceHolder.lockCanvas(null);
//                                mState.redraw(canvas, mModel);
//                            }
//                            finally {
//                                if (canvas != null) {
//                                    // Don't leave the Surface in an
//                                    // inconsistent state
//                                    mSurfaceHolder.
//                                        unlockCanvasAndPost(canvas);
//                                }
//                            }
//                        }
