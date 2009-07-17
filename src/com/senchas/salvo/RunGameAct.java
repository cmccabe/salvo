package com.senchas.salvo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ZoomButton;

import com.senchas.salvo.Model;
import com.senchas.salvo.Cosmos.PlayerInfo;
import com.senchas.salvo.WeaponType.Armory;
import com.senchas.salvo.WeaponType.WeaponListAdapter;

import android.view.View.OnClickListener;

public class RunGameAct extends Activity {
    /*================= Constants =================*/
    /** Result returned when the game ends */
    public static final int RESULT_GAME_OVER = RESULT_FIRST_USER;

    /** Result returned when the user presses back to abort the game */
    public static final int RESULT_USER_PRESSED_BACK = RESULT_FIRST_USER + 1;

    /* NOTE: We will return RESULT_CANCELLED if the activity crashes */

    /*================= Handles to Views =================*/
    /** A view representing the part of the screen where most of the graphics
     * are drawn */
    private GameControlView mGameControlView;

    /** Displays the current angle */
    private TextView mAngleText;

    /** The button you press to choose the previous weapon in your armory */
    private Button mArmoryLeftButton;

    /** The main text displayed in the center */
    private TextView mArmoryMainText;

    /** The secondary text displayed in the center */
    private TextView mArmorySecondaryText;

    /** The button you press to choose the next weapon in your armory */
    private Button mArmoryRightButton;

    /** The layout of the central portion of the armory */
    private RelativeLayout mArmoryCenter;

    /** The button the user presses to fire */
    private Button mFireButton;

    /*================= Temporary Data =================*/
    /** Lock that protects game state */
    private Object mStateLock;

    /** Provides access to RunGameAct internals */
    private RunGameActAccessor mAcc;

    /** Observes changes in the GameControlView */
    private GameControlViewObserver mGameControlViewObserver;

    /** The main thread */
    private RunGameThread mThread;

    private XmlColors mXmlColors;

    /*================= Permanent Data =================*/
    /** The game data */
    private Cosmos mCosmos;

    /** The round data */
    private Model mModel;

    /** The current game state. */
    private GameState mState;

    /*================= Types =================*/
    /** Represents the colors in the colors.xml file */
    public final static class XmlColors {
        /*================= Data =================*/
        private final int mArmoryBackground;

        private final int mClear;

        private final int mGameTextGrey;

        private final int mGameTextDark;

        /*================= Access =================*/
        public int getArmoryBackground() {
            return mArmoryBackground;
        }

        public int getClear() {
            return mClear;
        }

        public int getGameTextGrey() {
            return mGameTextGrey;
        }

        public int getGameTextDark() {
            return mGameTextDark;
        }

        /*================= Lifecycle =================*/
        public static XmlColors fromXml(Resources res) {
            return new XmlColors(res.getColor(R.drawable.armory_bg_color),
                            res.getColor(R.drawable.clear),
                            res.getColor(R.drawable.game_text_grey),
                            res.getColor(R.drawable.game_text_dark));
        }

        private XmlColors(int armoryBackground,
                          int clear,
                          int gameTextGrey,
                          int gameTextDark) {
            mArmoryBackground = armoryBackground;
            mClear = clear;;
            mGameTextGrey = gameTextGrey;
            mGameTextDark = gameTextDark;
        }
    }

    /** Provides access to RunGameAct internals.
     *
     * Normally RunGameAct hides its internals pretty well. For some
     * classes, we want to be more generous by providing them with this
     * accessor.
     *
     * If Java allowed "friend" classes, this code would be a lot shorter.
     *
     * This class contains no locking
     */
    public class RunGameActAccessor {
        /*================= Access =================*/
        public RunGameAct getRunGameAct() {
            return RunGameAct.this;
        }

        public GameControlView getGameControlView() {
            return mGameControlView;
        }

        public Cosmos getCosmos() {
            return mCosmos;
        }

        public Model getModel() {
            return mModel;
        }

        public TextView getAngleText() {
            return mAngleText;
        }

        public TextView getArmoryMainText() {
            return mArmoryMainText;
        }

        public TextView getArmorySecondaryText() {
            return mArmorySecondaryText;
        }

        public Button getArmoryLeftButton() {
            return mArmoryLeftButton;
        }

        public Button getArmoryRightButton() {
            return mArmoryRightButton;
        }

        public RelativeLayout getArmoryCenter() {
            return mArmoryCenter;
        }

        public Button getFireButton() {
            return mFireButton;
        }

        public XmlColors getXmlColors() {
            return mXmlColors;
        }
    }

    /** Observes changes in the GameControlView.
     *
     * This class does its own locking with mStateLock
     */
    private class GameControlViewObserver implements SurfaceHolder.Callback {
        /*================= Operations =================*/
        /** Callback invoked when the Surface has been created and is
         * ready to be used. */
        public void surfaceCreated(SurfaceHolder holder) {
            synchronized (mStateLock) {
                mThread.getStateController().setSurfaceAvailable();
            }
        }

        public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
                int arg3) {
            // Callback invoked when the surface dimensions change.
        }

        public void surfaceDestroyed(SurfaceHolder arg0) {
            // We have to tell thread to shut down and wait for it to finish,
            // or else it might touch the Surface after we return and explode
            boolean retry = true;
            synchronized (mStateLock) {
                mThread.getStateController().changeTerminateRequested(true);
                // Get the thread out of blocking state.
                // If we ever start doing old-style IO or using sockets, will
                // have to do some additional rain dances here to get the
                // thread out of blocking state.
                mThread.interrupt();
            }
            while (retry) {
                try {
                    mThread.join();
                    retry = false;
                }
                catch (InterruptedException e) {
                }
            }
        }
    }

    /** Controls the state of the RunGameThread.
     *
     * This class contains no locking
     */
    private class RunGameThreadStateController {
        /*================= Data =================*/
        /** GameControlView has been created; we can access its
         * data without crashing and burning. */
        private boolean mSurfaceAvailable;

        /** RunGameAct is done initializing itself. */
        private boolean mInitializationComplete;

        /** mRunGameThread has been told to stop; it either is
         * blocking on mStateLock or is about to do so. */
        private boolean mStopRequested;

        /** mRunGameThread has been told to terminate itself */
        private boolean mTerminateRequested;

        /*================= Access =================*/
        public boolean getStopRequested() {
            return mStopRequested;
        }

        public boolean getTerminateRequested() {
            return mTerminateRequested;
        }

        /*================= Operations =================*/
        public void setInitializationComplete() {
            assert (Thread.holdsLock(mStateLock));
            if (mInitializationComplete)
                return;
            mInitializationComplete = true;
            if (mSurfaceAvailable)
                mThread.start();
        }

        public void setSurfaceAvailable() {
            assert (Thread.holdsLock(mStateLock));
            if (mSurfaceAvailable)
                return;
            mSurfaceAvailable = true;
            if (mInitializationComplete)
                mThread.start();
        }

        public void changeStopRequested(boolean stopRequested) {
            assert (Thread.holdsLock(mStateLock));
            mStopRequested = stopRequested;
        }

        public void changeTerminateRequested(boolean terminateRequested) {
            assert (Thread.holdsLock(mStateLock));
            mTerminateRequested = terminateRequested;
        }

        /*================= Lifecycle =================*/
        public RunGameThreadStateController() {
            mSurfaceAvailable = false;
            mInitializationComplete = false;
        }
    }

    /** The main game thread.
     *
     * RunGameThread executes the game state machine.
     *
     * State transitions can only happen in the main thread.
     *
     * The main thread also draws everything on the screen.
     *
     * If you have done anything that might require a screen redraw, or
     * cause a transition to another game state, be sure to use notify() to
     * wake up the main thread.
     */
    public class RunGameThread extends Thread {
        /*================= Data =================*/
        private RunGameThreadStateController mStateController;

        /*================= Access =================*/
        public RunGameThreadStateController getStateController() {
            return mStateController;
        }

        /*================= Operations =================*/
        @Override
        public void run() {
            Log.w(this.getClass().getName(), "Starting RunGameThread...");

            try {
                while (true) {
                    // Enter the state
                    synchronized (mStateLock) {
                        stateLog("onEnter", mState);
                        mState.onEnter(mAcc);
                    }

                    // Execute the state's main loop
                    GameState next = null;
                    synchronized (mStateLock) {
                        stateLog("starting main", mState);
                        while (true) {
                            if (doCancellationPoint())
                                return;
                            next = mState.main(mAcc);
                            if (next != null)
                                break;
                            // Delay until the next call to main()
                            // If getBlockingDelay == 0, then we delay until
                            // someone calls notify() on mStateLock
                            mStateLock.wait(mState.getBlockingDelay());
                        }
                        if (doCancellationPoint())
                            return;
                    }

                    synchronized (mStateLock) {
                        stateLog("onExit", mState);
                        mState.onExit(mAcc);
                        mState = next;
                    }
                }
            }
            catch (InterruptedException e) {
                Log.e(getClass().getName(),
                      "caught InterruptedException: quitting.");
            }
        }

        /** Print out state logging information */
        private void stateLog(String text, GameState state) {
            /*StringBuilder b = new StringBuilder(80);
            b.append(text).append("(");
            b.append(mState.toString()).append(")");
            Log.w(this.getClass().getName(), b.toString()); */
        }

        /** Process requests that the main thread sleep or exit.
         *
         * This function checks to see if someone has requested that the
         * main thread go to sleep or terminate itself.
         *
         * In the event that someone has requested sleep, we sleep using
         * wait().
         * In the event that someone has requested termination, we return
         * true.
         *
         * @return      true if we should exit run(), false otherwise
         * @throws InterruptedException
         */
        private boolean doCancellationPoint() throws InterruptedException {
            assert (Thread.holdsLock(mStateLock));
            if (mStateController.getTerminateRequested())
                return true;
            if (mStateController.getStopRequested()) {
                mStateLock.wait();
            }
            return false;
        }

        /*================= Lifecycle =================*/
        public RunGameThread() {
            mStateController = new RunGameThreadStateController();
        }
    }

    /** Implements the Buy Weapons dialog box */
    public class BuyWeaponsDialog extends Dialog implements OnClickListener {
        /*================= Data =================*/
        private TextView mCreditText;

        private Cosmos mCosmos;

        private Player mPlayer;

        /*================= Operations =================*/
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.done:
                    synchronized (mStateLock) {
                        if (mState.onButton(mAcc,
                                GameState.GameButton.DONE)) {
                            mStateLock.notify();
                        }
                    }
                    dismiss();
                    break;
            }
        }

        /*================= Lifecycle =================*/
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setCancelable(false);
            setContentView(R.layout.buy_weapons);

            Button done = (Button) findViewById(R.id.done);
            done.setOnClickListener(this);

            TextView playerNameText =
                (TextView) findViewById(R.id.player_name);
            playerNameText.setText(mPlayer.getName());
            playerNameText.setTextColor(mPlayer.getBaseColor().toInt());

            mCreditText = (TextView) findViewById(R.id.credits);
            updateCreditText();

            final BuyWeaponsDialog enclosing = this;
            ListView weaponList = (ListView)findViewById(R.id.weapons_list);
            final ListAdapter wla = WeaponType.
                                getWeaponListAdapter(mCosmos, mPlayer);
            weaponList.setAdapter(wla);
            weaponList.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Armory armory = mPlayer.getArmory(mCosmos);
                    WeaponType weapon =
                        WeaponType.sSelectableWeapons[position];
                    armory.addWeapon(weapon);

                    PlayerInfo playerInfo =
                        mCosmos.getPlayerInfo()[ mPlayer.getId() ];
                    playerInfo.spendMoney(weapon.getPrice());

                    enclosing.updateCreditText();
                    ((BaseAdapter) wla).notifyDataSetChanged();
                }
            });
        }

        public void updateCreditText() {
            StringBuilder b = new StringBuilder(80);
            b.append("$");
            b.append(mCosmos.getPlayerInfo()[ mPlayer.getId() ].
                    getCash());
            mCreditText.setText(b.toString());
        }

        public BuyWeaponsDialog(Context context,
                                Cosmos cosmos, Player player) {
            super(context, R.style.buy_weapons_dialog);
            mCosmos = cosmos;
            mPlayer = player;
        }
    }

    /** Implements the Leaderboard dialog box */
    public class LeaderboardDialog extends Dialog implements OnClickListener {
        /*================= Types =================*/

        /*================= Operations =================*/
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.ok:
                    synchronized (mStateLock) {
                        if (mState.onButton(mAcc,
                                GameState.GameButton.OK)) {
                            mStateLock.notify();
                        }
                    }
                    dismiss();
                    break;
            }
        }

        /*================= Lifecycle =================*/
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setCancelable(false);
            setContentView(R.layout.leaderboard);

            Button ok = (Button) findViewById(R.id.ok);
            ok.setOnClickListener(this);

            ListView scoresList = (ListView) findViewById(R.id.scores);
            scoresList.setAdapter(mCosmos.getLeaderboardAdaptor(mModel));
            scoresList.setDivider(null);
            scoresList.setDividerHeight(0);
            scoresList.setChoiceMode(ListView.CHOICE_MODE_NONE);
        }

        public LeaderboardDialog(Context context) {
            super(context, R.style.leaderboard_dialog);
        }
    }

    /** Implements the "announce winner" dialog box */
    public class AnnounceWinnerDialog extends Dialog
                                        implements OnClickListener {
        /*================= Types =================*/

        /*================= Operations =================*/
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.ok:
                    synchronized (mStateLock) {
                        if (mState.onButton(mAcc,
                                GameState.GameButton.OK)) {
                            mStateLock.notify();
                        }
                    }
                    dismiss();
                    break;
            }
        }

        /*================= Lifecycle =================*/
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setCancelable(false);
            setContentView(R.layout.announce_winner);

            Button ok = (Button) findViewById(R.id.ok);
            ok.setOnClickListener(this);

            TextView intro = (TextView) findViewById(R.id.intro);
            Cosmos.LeaderboardAdaptor adapt =
                mCosmos.getLeaderboardAdaptor(mModel);
            if (adapt.tieForWinner()) {
                intro.setText("It's a tie between...");
            }
            else {
                intro.setText("And the winner is...");
            }

            TextView player = (TextView) findViewById(R.id.player);
            player.setText(adapt.getWinnerText());
            player.setTextColor(adapt.getWinnerColor());
        }

        public AnnounceWinnerDialog(Context context) {
            super(context, R.style.announce_winner_dialog);
        }
    }

    /*================= Operations =================*/
    /** Called from GameControlView to handle keystrokes */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        synchronized (mStateLock) {
            switch(keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    showAreYouSureYouWantToQuit();
                    return true;
            }
        }
        return false;
    }

    /** Called from GameControlView to handle touch events */
    public boolean onTouchEvent(MotionEvent me) {
        synchronized (mStateLock) {
            if (mState.onTouchEvent(mAcc, me)) {
                mStateLock.notify();
            }
        }
        return true;
    }

    private void showAreYouSureYouWantToQuit() {
        // display alert that we can't have any more players
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setMessage("Are you sure you want to end this game?\n" +
                    "If you want to switch apps without quitting, " +
                    "press \"HOME\"");
        b.setCancelable(false);
        b.setNegativeButton("End Game",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                                    int whichButton) {
                    setResult(RESULT_USER_PRESSED_BACK);
                    finish();
                }
            });
        b.setPositiveButton("Cancel",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                                    int whichButton) {
                    // Let's just forget we ever had this
                    // conversation, OK?
                }
            });
        b.show();
    };

    /** Starts a new round.
     *
     * Creates a new Model and initializes things to suit it.
     *
     * @param firstRound  If true, create a new Cosmos.
     */
    public void startRound(boolean firstRound) {
        Bundle smap =
            getIntent().getBundleExtra(GameSetupAct.GAME_SETUP_BUNDLE);
        ModelFactory fac = ModelFactory.fromBundle(smap);
        if (firstRound) {
            mCosmos = Cosmos.fromInitial(fac.getNumRounds(),
                                         fac.getNumPlayers(),
                                         fac.getStartingCash());
        }
        mCosmos.nextRound();
        mModel = fac.createModel(mCosmos);
        if (firstRound) {
            mState = GameState.createInitialGameState();
        }
    }

    public void continueRound() {
        mGameControlView.initialize(mModel.getBackground(),
                                   mModel.getForeground());
    }

    public void endGame() {
        setResult(RESULT_GAME_OVER);
        finish();
    }

    /*================= Lifecycle =================*/
    @Override
    public void onCreate(Bundle map) {
        synchronized (mStateLock) {
            super.onCreate(map);
            mXmlColors = XmlColors.fromXml(getResources());

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

            setContentView(R.layout.game);

            ////////////////// Get pointers to widgets
            mGameControlView = (GameControlView)
                findViewById(R.id.game_control_view);
            mAngleText = (TextView)findViewById(R.id.angle_text);
            mArmoryLeftButton = (Button)findViewById(R.id.armory_left_button);
            mArmoryMainText = (TextView)findViewById(R.id.armory_main_text);
            mArmorySecondaryText = (TextView)
                findViewById(R.id.armory_secondary_text);
            mArmoryRightButton =
                (Button)findViewById(R.id.armory_right_button);
            mArmoryCenter = (RelativeLayout)findViewById(R.id.armory_center);
            mFireButton = (Button)findViewById(R.id.fire_button);

            ///////////////// Initialize game state
            if (map == null) {
                startRound(true);
                continueRound();
            }
            else {
                // Decompress saved state
                mCosmos = Cosmos.fromBundle(map);
                mModel = Model.fromBundle(map);
                mState = GameState.fromBundle(map);
                continueRound();
            }

            ////////////////// Initialize widgets
            mArmoryLeftButton.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    synchronized (mStateLock) {
                        if (mState.onButton(mAcc,
                                GameState.GameButton.ARMORY_LEFT)) {
                            mStateLock.notify();
                        }
                    }
                }
            });

            mArmoryRightButton.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    synchronized (mStateLock) {
                        if (mState.onButton(mAcc,
                                GameState.GameButton.ARMORY_RIGHT)) {
                            mStateLock.notify();
                        }
                    }
                }
            });

            mFireButton.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    int act = event.getAction();
                    if (act == MotionEvent.ACTION_DOWN) {
                        synchronized (mStateLock) {
                            if (mState.onButton(mAcc,
                                    GameState.GameButton.PRESS_FIRE)) {
                                mStateLock.notify();
                            }
                        }
                    }
                    else if (act == MotionEvent.ACTION_UP) {
                        synchronized (mStateLock) {
                            if (mState.onButton(mAcc,
                                    GameState.GameButton.RELEASE_FIRE)) {
                                mStateLock.notify();
                            }
                        }
                    }
                    return true;
                }
            });

            mGameControlView.getHolder().
                addCallback(mGameControlViewObserver);
            mThread.getStateController().setInitializationComplete();
        }
    }

    @Override
    protected void onPause() {
        // The game is no longer in the foreground
        super.onPause();
        synchronized (mStateLock) {
            Log.w(this.getClass().getName(), "onPause called");
            mThread.getStateController().changeStopRequested(true);
            mStateLock.notify();
        }
    }

    @Override
    public void onResume() {
        // The game is back in the foreground
        super.onResume();
        synchronized (mStateLock) {
            Log.w(this.getClass().getName(), "onResume called");
            mThread.getStateController().changeStopRequested(false);
            mStateLock.notify();
        }
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState a Bundle into which this Activity should save its
     *        state
     */
    @Override
    protected void onSaveInstanceState(Bundle map) {
        super.onSaveInstanceState(map);
        synchronized (mStateLock) {
            Log.w(this.getClass().getName(),
                    "RunGameAct.onSaveInstanceState");
            mCosmos.saveState(map);
            mModel.saveState(map);
            mState.saveState(map);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // note: GameControlViewObserver.surfaceDestroyed() cleans up the
        // thread-- so we don't have to do it here.
    }

    public RunGameAct() {
        super();
        mStateLock = new Object();
        mAcc = new RunGameActAccessor();
        mGameControlViewObserver = new GameControlViewObserver();
        mThread = new RunGameThread();
    }
}
