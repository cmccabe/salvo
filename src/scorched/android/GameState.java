package scorched.android;

import android.graphics.Canvas;
import android.util.Log;
import scorched.android.SalvoSlider.Listener;


public interface GameState {
    /*================= Constants =================*/
    public final static String TAG = "GameState";

    /*================= Types =================*/
    enum GameButton {
        OK,
        DONE,
        FIRE,
        WEAP
    }

    public class DomainException extends RuntimeException {
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
     */
    public void onButton(GameButton b);

    /** Called when the user moves a slider
     * Under MainThread lock
     */
    public void onSlider(Model model, boolean isPowerSlider, int val);

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

        public void onButton(GameButton b) {
            if (b == GameButton.OK)
                mFinished = true;
        }

        public void onSlider(Model model, boolean isPowerSlider, int val) { }

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

        public void onButton(GameState.GameButton b) {
            if (b == GameButton.DONE)
                mFinished = true;
        }

        public void onSlider(Model model, boolean isPowerSlider, int val) { }

        public boolean needRedraw() { return false; }

        public void redraw(Canvas canvas, Model model) { }

        public BuyWeaponsState() { }

        private boolean mFinished;
    }

    /** The start of a turn. */
    class TurnStartState implements GameState {
        public String toString() {
            return "TurnStartState";
        }

        public void onEnter(Model model,
                            SalvoSlider powerSlider, SalvoSlider angleSlider,
                            Listener powerAdaptor, Listener angleAdaptor) { }

        public GameState main(Model model) {
            int oldPlayer = model.getCurPlayerId();
            model.nextPlayer();
            int newPlayer = model.getCurPlayerId();
            if (newPlayer == Player.INVALID_PLAYER_ID) {
                // Everyone died. It was a draw.
                return GameState.sLeaderBoardState;
            }
            if (newPlayer == oldPlayer) {
                // There's only one player left!
                // That means he's the winner!
                return GameState.sLeaderBoardState;
            }

            Player curPlayer = model.getCurPlayer();
            return curPlayer.getGameState();
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) { }

        public int getBlockingDelay() {
            return 0;
        }

        public void onButton(GameButton b) { }

        public void onSlider(Model model, boolean isPowerSlider, int val) { }

        public boolean needRedraw() { return false; }

        public void redraw(Canvas canvas, Model model) { }

        public TurnStartState() { }
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

        public void onButton(GameButton b) {
            switch (b) {
                case OK:
                case DONE:
                    break;
                case WEAP:
                    // TODO: display the user's armory and allow him to
                    // choose another weapon.  Probably want to create a
                    // ListView programmatically and fiddle around with it.
                    // Maybe needs a new State to handle.
                case FIRE:
                    mFired = true;
                break;
            }
        }

        public void onSlider(Model model, boolean isPowerSlider, int val) {
            if (isPowerSlider) {
                model.getCurPlayer().setPower(val);
            }
            else {
                model.getCurPlayer().setAngleDeg(val);
            }
        }

        public boolean needRedraw() {
            return Graphics.instance.getNeedRedrawAll();
        }

        // The human move state doesn't draw anything special on the screen.
        // Perhaps later we'll implement a special partial draw mode for
        // HumanMoveState where we redraw only the player when moving around
        // his turret...
        public void redraw(Canvas canvas, Model model) {
            Graphics.instance.drawScreen(canvas, model);
        }

        public HumanMoveState() { }

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
            model.getCurPlayer().fireWeapon();
            Weapon.instance.calculateTrajectory(model);
            mCurSample = 1;
            // todo: zoom so that start and end points are both visible
        }

        public GameState main(Model model) {
            short nextSample = (short)(mCurSample + 1);
            if (mCurSample > Weapon.instance.getTotalSamples()) {
                return sExplosionState;
            }
            mCurSample = nextSample;
            Log.w(TAG, "mCurSample = " + mCurSample);
            return null;
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) {
        }

        public int getBlockingDelay() {
            return 10;
        }

        public void onButton(GameButton b) { }

        public void onSlider(Model model, boolean isPowerSlider, int val) { }

        public boolean needRedraw() {
            return true;
        }

        public void redraw(Canvas canvas, Model model) {
            Graphics gfx = Graphics.instance;
            //gfx.setNeedRedrawAll();
            gfx.drawScreen(canvas, model);
            gfx.drawTrajectory(canvas, model.getCurPlayer(), mCurSample);
            gfx.clearNeedRedrawAll();
        }

        public BallisticsState() {
        }

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
                            Listener powerAdaptor, Listener angleAdaptor)
                            { }

        public GameState main(Model model) {
            return sTurnStartState;
        }

        public void onExit(SalvoSlider powerSlider,
                           SalvoSlider angleSlider) {
             // examine Weapon.instance to find out where the boom is,
             // then modify the terrain in the model
        }

        public int getBlockingDelay() {
            return 10;
        }

        public void onButton(GameButton b) { }

        public void onSlider(Model model, boolean isPowerSlider, int val) { }

        public boolean needRedraw() {
            return true;
        }

        public void redraw(Canvas canvas, Model model) {
            Graphics.instance.drawScreen(canvas, model);
            //gfx.drawExplosion(canvas, model.getCurPlayer(),
            //                   mX, mY, mCurSample);
            Graphics.instance.clearNeedRedrawAll();
        }

        public ExplosionState() { }
    }

    /*================= Static =================*/
    // We use static storage for the game states. This avoid dynamic memory
    // allocation. Be careful not to hold on to any important memory in the
    // states, though.
    public LeaderboardState sLeaderBoardState = new LeaderboardState();
    public BuyWeaponsState sBuyWeaponsState = new BuyWeaponsState();
    public TurnStartState sTurnStartState = new TurnStartState();
    public HumanMoveState sHumanMoveState = new HumanMoveState();
    //public ComputerMoveState sComputerMoveState = new ComputerMoveState();
    public BallisticsState sBallisticsState = new BallisticsState();
    public ExplosionState sExplosionState = new ExplosionState();

    /*================= Constants =================*/
    /*================= Types =================*/
    /*================= Operations =================*/
    /*================= Members =================*/
    /*================= Accessors =================*/
    /*================= Lifecycle =================*/
}
