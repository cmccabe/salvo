package scorched.android;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
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
    private static final String GAME_STATE_ID = "GAME_STATE_ID";
    private static final String EMPTY_STRING = "";

    /*================= Types =================*/
    public static enum GameButton {
        ARMORY_LEFT,
        ARMORY_RIGHT,
        OK,
        PRESS_FIRE,
        RELEASE_FIRE
    }

    public static class DomainException extends RuntimeException {
        public static final long serialVersionUID = 1;
        public DomainException(String message) {
           super(message);
        }
    }

    /** Runnable which sets a text view to a specified string. */
    private static class SetTextView implements Runnable {
        /*================= Operations =================*/
        public void run() {
            mTextView.setText(mStr);
        }
        /*================= Data =================*/
        private TextView mTextView;
        private String mStr;

        /*================= Lifecycle =================*/
        SetTextView(TextView textView, String str) {
            mTextView = textView;
            mStr = str;
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
    public boolean onTouchEvent(RunGameActAccessor game, MotionEvent event) {
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

        @Override
        public void onEnter(RunGameActAccessor game) {
            // TODO: give a reward to the 'surviving' player
            // use: int newPlayer = model.getCurPlayerId();, etc.

            // TODO: set leaderboard layout...
            // TODO: set leaderboard menus...
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            if (mFinished) {
                return BuyWeaponsState.create();
            }
            else {
                return null;
            }
        }

        @Override
        public int getBlockingDelay() {
            return 0;
        }

        @Override
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

        @Override
        public void onEnter(RunGameActAccessor game) {
            mFinished = false;
            // TODO: implement weapons layout...
            // TODO: implement weapons menus...
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            if (mFinished) {
                return BuyWeaponsState.create();
            }
            else {
                return null;
            }
        }

        @Override
        public void onExit(RunGameActAccessor game) {
            // TODO: implement game layout
            // TODO: implement game menus
        }

        @Override
        public int getBlockingDelay() {
            return 0;
        }

        @Override
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

        /*================= Static =================*/
        private static TurnStartState sMe = new TurnStartState();

        /*================= Data =================*/
        private Model.NextTurnInfo mInfo;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        @Override
        public void onEnter(RunGameActAccessor game) {
            game.getModel().getNextPlayerInfo(mInfo);
            // TODO: set location of floating arrow thing to current player
        }

        @Override
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
                Util.DoToast doToast = new Util.DoToast(
                    game.getGameControlView().getContext(),
                    play.getIntroductionString());
                game.getRunGameAct().runOnUiThread(doToast);

                model.setCurPlayerId(mInfo.getNextPlayerId());
                return play.getGameState();
            }
        }

        @Override
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

        @Override
        public void onEnter(RunGameActAccessor game) {
            GameState.setCurPlayerAngleText(game);
            game.getGameControlView().cacheTerrain(game);
          //            mNeedRedraw = true;
//            mFingerDown = false;
//            mTouchX = 0;
//            mTouchY = 0;
//            mFired = false;
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            game.getGameControlView().drawScreen(game);
            return null; //(mFired) ? sBallisticsState : null;
        }

        @Override
        public void onExit(RunGameActAccessor game) {
            // TODO: grey out buttons and whatnot
            GameState.setCustomAngleText(game, EMPTY_STRING);
        }

        @Override
        public int getBlockingDelay() {
            return 0;
        }

        @Override
        public boolean onButton(RunGameActAccessor game, GameButton b) {
            switch (b) {
                case ARMORY_LEFT:
                    return true;
                case ARMORY_RIGHT:
                    return true;
                case PRESS_FIRE:
                    hideArmory(game);
                    return true;
                case RELEASE_FIRE:
                    showArmory(game);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onTouchEvent(
                RunGameActAccessor game, MotionEvent event) {

            Player curPlayer = game.getModel().getCurPlayer();
            int startAngle = curPlayer.getAngleDeg();
            int finishAngle = startAngle;

            int act = event.getAction();
            if ((act == MotionEvent.ACTION_DOWN) ||
                (act == MotionEvent.ACTION_MOVE) ||
                (act == MotionEvent.ACTION_UP))
            {
                float x = event.getX();
                float y = event.getY();

                float tx = curPlayer.getX();
                float ty = curPlayer.getTurretCenterY();

                float y_diff = ty - y;
                if (y_diff <= 0) {
                    if (x >= tx)
                        finishAngle = Player.MIN_TURRET_ANGLE;
                    else
                        finishAngle = Player.MAX_TURRET_ANGLE;
                }
                else {
                    float x_diff = tx - x;
                    float angleRad = (float)Math.atan2(y_diff, x_diff);
                    int angleDeg = (int)Math.toDegrees(angleRad);
                    finishAngle = Player.MAX_TURRET_ANGLE - angleDeg;
                }
            }
            if (finishAngle != startAngle) {
                curPlayer.setAngleDeg(finishAngle);
                GameState.setCurPlayerAngleText(game);
                return true;
            }
            else
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

        @Override
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

        @Override
        public GameState main(RunGameActAccessor game) {
            return null;
        }

        @Override
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

        @Override
        public void onEnter(RunGameActAccessor game) {
//            Weapon wpn = Weapon.instance;
//            WeaponType wtp = wpn.getWeaponType();
//            mMaxExplosionSize = wtp.getExplosionSize();
//            mCurExplosionSize = 0;
//            Graphics.instance.initializeExplosion();
//            Sound.instance.playBoom(
//                powerSlider.getContext().getApplicationContext());
        }

        @Override
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

        @Override
        public void onExit(RunGameActAccessor game) {
             // examine Weapon.instance to find out where the boom is,
             // then modify the terrain in the model
        }

        @Override
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
    /** Sets the current angle text to the turret angle of the current
     * player.
     */
    private static void setCurPlayerAngleText(RunGameActAccessor game) {
        Player curPlayer = game.getModel().getCurPlayer();
        TextView angleText = game.getAngleText();
        StringBuilder b = new StringBuilder(10);
        b.append(curPlayer.getAngleDeg()).append("°");
        game.getRunGameAct().runOnUiThread(new SetTextView(angleText,
                                                           b.toString()));
    }

    /** Sets the current angle text to a custom string */
    private static void setCustomAngleText(RunGameActAccessor game,
                                           String text) {
        Player curPlayer = game.getModel().getCurPlayer();
        TextView angleText = game.getAngleText();
        game.getRunGameAct().runOnUiThread(new SetTextView(angleText, text));
    }

    /** Hides the armory in the middle of the screen */
    private static void hideArmory(RunGameActAccessor game) {
        game.getAngleText().setVisibility(View.INVISIBLE);
        game.getArmoryMainText().setVisibility(View.INVISIBLE);
        game.getArmorySecondaryText().setVisibility(View.INVISIBLE);
        game.getArmoryLeftButton().setVisibility(View.INVISIBLE);
        game.getArmoryRightButton().setVisibility(View.INVISIBLE);
        int color = game.getRunGameAct().getResources().
                        getColor(R.drawable.clear);
        game.getArmoryCenter().setBackgroundColor(color);
    }

    /** Un-hides the armory in the middle of the screen */
    private static void showArmory(RunGameActAccessor game) {
        game.getAngleText().setVisibility(View.VISIBLE);
        game.getArmoryMainText().setVisibility(View.VISIBLE);
        game.getArmorySecondaryText().setVisibility(View.VISIBLE);
        game.getArmoryLeftButton().setVisibility(View.VISIBLE);
        game.getArmoryRightButton().setVisibility(View.VISIBLE);
        int color = game.getRunGameAct().getResources().
                        getColor(R.drawable.armory_bg_color);
        game.getArmoryCenter().setBackgroundColor(color);
    }

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
