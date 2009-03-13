package scorched.android;


public interface GameState {
    /*================= Types =================*/
    public class DomainException extends RuntimeException {
        public DomainException(String message) {
           super(message);
        }
     }

    /*================= Operations =================*/
    /** The function that will be executed for this state in the main event
     *  loop. 
     *
     * @return  The next game state
     */
    public GameState execute();

    /** Called when the user presses the 'weap' button
      * Called with mUserInputMutex held.
      */
    public void weapButton();

    /** Called when the user presses the 'fire' button
      * Called with mUserInputMutex held.
      */
    public void fireButton();

    /** Callback by GameControlView when we want to redraw all or 
      * part of the screen.
      *
      * This would happen if the screen got resized, zoom level
      * changed, etc.
      *
      * Called with mUserInputMutex held.
      *
      * @param    all       True if we want to redraw everything
      */
    public void redraw(Canvas canvas, Model model, 
                       Graphics gfx, bool all);

    /*================= Game States =================*/
    /** The start of a turn. */
    class InitMoveState implements GameState {
        public void onEnter() { }

        public GameState execute() {
            int oldPlayer = mModel.getCurPlayerId();
            model.nextPlayer();
            int newPlayer = mModel.getCurPlayerId();
            if (newPlayer == Player.INVALID_PLAYER_ID) {
                // Everyone died. It was a draw.
                sRoundOver.initialize(Player.INVALID_PLAYER_ID);
                return sRoundOver;
            }
            if (newPlayer == oldPlayer) {
                // There's only one player left!
                // That means he's the winner!
                sRoundOver.initialize(oldPlayer);
                return sRoundOver;
            }

            Player curPlayer = model.getCurPlayer();
            return curPlayer.getGameState();
        }

        public boolean onExit() {
            return true;
        }

        public void weapButton() { }

        public void fireButton() { }

        public void redraw(Canvas canvas, Model model, 
                           Graphics gfx, bool all) { }

        public void initialize() { }

        public InitMoveState() { }
    }

    /** A human turn. We will accept input from the touchscreen and do all
     * that stuff. */
    class HumanMoveState implements GameState {
        public GameState execute() {

            try {
                synchronized (userInputSem) {
                    while (true) {
                        gameControlView.updateGraphics(this);
                        mUserInputSem.wait();
                        if (mNextGameState != null) {
                            break;
                        }
                    }
                }
            }
            finally {
            }
            return mNextGameState;
        }

        public void onEnter(SalvoSlider powerSlider, 
                            SalvoSlider angleSlider) {
            // Activate sliders
            int myColor = mGraphics.getPlayerColor(curPlayer.getId());
            powerSlider.setState(SalvoSlider.SliderState.BAR, 
                        mPowerAdaptor,
                        Player.MIN_POWER, Player.MAX_POWER,
                        curPlayer.getPower(),
                        myColor);
            angleSlider.setState(SalvoSlider.SliderState.ANGLE,
                        mAngleAdaptor,
                        Player.MAX_TURRET_ANGLE, Player.MIN_TURRET_ANGLE,
                        curPlayer.getAngleDeg(),
                        myColor);
        }

        public void onExit(SalvoSlider powerSlider, 
                           SalvoSlider angleSlider) {
            // Deactivate sliders
            powerSlider.setState(SalvoSlider.SliderState.DISABLED, 
                        mPowerAdaptor, 0, 0, 0, 0);
            angleSlider.setState(SalvoSlider.SliderState.ANGLE,
                        mAngleAdaptor, 0, 0, 0, 0);
        }

        public void weapButton() {
            // TODO: display the user's armory and allow him to choose 
            // another weapon.
            // Probably want to create a ListView programmatically and 
            // fiddle around with it. Maybe needs a new State to handle.
        }

        public void fireButton() {
            mNextGameState = sBallisticsState;
        }

        // The human move state doesn't draw anything special on the screen.
        // Perhaps later we'll implement a special partial draw mode for
        // HumanMoveState where we redraw only the player when moving around
        // his turret...
        public void redraw(Canvas canvas, Model model,
                           Graphics gfx, bool all) { }

        public void initialize() {
            mNextGameState = null;
        }

        public HumanMoveState() { }

        private GameState mNextGameState;
    }

    /** Draw missiles flying through the sky. The fun state. */
    class BallisticsState implements GameState {
        final static int MAX_SAMPLES = 100;

        public GameState execute() {
            Weapon weapon = curPlayer.getWeapon();
            while (true) {
                runScreenRefresh(curPlayer, weapon);
                synchronized (mUserInputSem) {
                    //mUserInputSem.wait(1);
                    weapon.nextSample();
                    Weapon.Point collisionPoint = weapon.testCollision();
                    if (collisionPoint != null) {
                        mNextGameState = GameState.EXPLOSION;
                    }
                    if (mNextGameState != GameState.BALLISTICS) {
                        return mNextGameState;
                    }
                    if (weapon.getNeedsRedraw()) {
                        mGraphics.setNeedScreenRedraw();
                    }
                }
            }
        }

        public void weapButton() { }

        public void fireButton() { }

        public void redraw(Canvas canvas, Model model, 
                           Graphics gfx, bool all) {
            gfx.drawTrajectory(canvas, mModel.getCurPlayer(), all,
                               mX, mY, mCurSample);
        }

        public void initialize() {
            numSamples = 0;
        }

        public BallisticsState() {
            // Grab some memory for weapon trajectories
            mX = new float[MAX_SAMPLES];
            mY = new float[MAX_SAMPLES];
        }

        private float mX[];
        private float mY[];
        short mNumSamples;
        short mCurSample;
    }

    /** Do explosions.
     * Subtract from players' life points if necessary.
     * Make craters if necessary.
     */
    class ExplosionState implements GameState {
        public GameState execute() {
            Weapon weapon = curPlayer.getWeapon();

            // The projectile is exploding onscreen
//            Canvas canvas = mSurfaceHolder.lockCanvas(null);
//            Paint p = new Paint();
//            p.setAntiAlias(false);
//            p.setARGB(128, 255, 0, 255);
//            Rect rect = new Rect();
//            rect.set(0, 0, 200, 200);
//            canvas.drawRect(rect, p);
//            mSurfaceHolder.unlockCanvasAndPost(canvas);
//            Thread.sleep(10000);
//            return GameState.PLAYER_MOVE;
        }

        public void weapButton() { }

        public void fireButton() { }

        public void initialize() { }

        public ExplosionState() { }
    }

    /** The round is over. Either someone has won or, it's a draw */
    class RoundOverState implements GameState {
        public GameState execute() {
        	throw GameState.DomainException("not executable");
        }

        public void weapButton() { }

        public void fireButton() { }

        public void initialize(int winningPlayer) {
            mWinningPlayer = winningPlayer;
        }

        public RoundOverState() { }

        private int mWinningPlayer;
    }

    /*================= Static =================*/
    // We use static storage for the game states. This avoid dynamic memory
    // allocation. Be careful not to hold on to any important memory in the
    // states, though.
    InitMoveState sInitMoveState = new InitMoveState();
    HumanMoveState sHumanMoveState = new HumanMoveState();
    BallisticsState sBallisticsState = new BallisticsState();
    ExplosionState sExplosionState = new ExplosionState();
    RoundOverState sRoundOverState = new RoundOverState();

    /*================= Constants =================*/
    /*================= Types =================*/
    /*================= Operations =================*/
    /*================= Members =================*/
    /*================= Accessors =================*/
    /*================= Lifecycle =================*/
}
