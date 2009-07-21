package com.senchas.salvo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import java.lang.System;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.senchas.salvo.RunGameAct.AnnounceWinnerDialog;
import com.senchas.salvo.RunGameAct.BuyWeaponsDialog;
import com.senchas.salvo.RunGameAct.LeaderboardDialog;
import com.senchas.salvo.RunGameAct.RunGameActAccessor;
import com.senchas.salvo.RunGameAct.XmlColors;
import com.senchas.salvo.WeaponType.Armory;


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
 * Threading
 * ---------
 * Some GameState callbacks are called by the UI thread; others are called by
 * the main game thread. There are a lot of things that you can only do in the
 * UI thread, so use runOnUiThread() for those if you must.
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
        DONE,
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

    /** Runnable which starts the 'buy weapons' dialog box */
    private static class StartBuyWeaponsDialog implements Runnable {
        /*================= Data =================*/
        private RunGameAct mRunGameAct;

        private Cosmos mCosmos;

        private Player mPlayer;

        /*================= Operations =================*/
        public void run() {
            BuyWeaponsDialog buyWeapons = mRunGameAct.new
                    BuyWeaponsDialog(mRunGameAct, mCosmos, mPlayer);
            buyWeapons.show();
        }

        /*================= Lifecycle=================*/
        StartBuyWeaponsDialog(RunGameAct runGameAct,
                              Cosmos cosmos, Player player) {
            mRunGameAct = runGameAct;
            mCosmos = cosmos;
            mPlayer = player;
        }
    }

    /** Runnable which starts the 'leaderboard' dialog box */
    private static class StartLeaderboardDialog implements Runnable {
        /*================= Data =================*/
        RunGameAct mRunGameAct;

        /*================= Operations =================*/
        public void run() {
            LeaderboardDialog leaderboard =
                mRunGameAct.new LeaderboardDialog(mRunGameAct);
            leaderboard.show();
        }

        /*================= Lifecycle=================*/
        StartLeaderboardDialog(RunGameAct runGameAct) {
            mRunGameAct = runGameAct;
        }
    }

    /** Runnable which starts the announce winner dialog box */
    private static class StartAnnounceWinnerDialog implements Runnable {
        /*================= Data =================*/
        RunGameAct mRunGameAct;

        /*================= Operations =================*/
        public void run() {
            AnnounceWinnerDialog announceWinner =
                mRunGameAct.new AnnounceWinnerDialog(mRunGameAct);
            announceWinner.show();
        }

        /*================= Lifecycle=================*/
        StartAnnounceWinnerDialog(RunGameAct runGameAct) {
            mRunGameAct = runGameAct;
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
    //         +-------------------------+
    //         | AnnounceWinnerState     |
    //         |                         |--------> end game
    //         | announces the winner    |
    //         |                         |
    //         +-------------------------+
    //                     ^
    //                     |  no more rounds left
    //         +-------------------------+
    //         | LeaderboardState        |
    //         |                         |
    // force   | show the leaderboard    |<---------+
    // draw -> |                         |          |
    //         +-------------------------+          |
    //                     | next round             |
    //                     V                        |
    //  start  +------------------------------+     |
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
    //   |     | through the air, draw        |
    //   |     | explosions                   |
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
        private boolean mDisplayActive;
        private long mDisplayTime;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        @Override
        public void onEnter(RunGameActAccessor game) {
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            if (! mDisplayActive) {
                long curTime = System.currentTimeMillis();
                if (curTime < mDisplayTime)
                    return null;
                mDisplayActive = true;
                RunGameAct runGameAct = game.getRunGameAct();
                StartLeaderboardDialog dial =
                    new StartLeaderboardDialog(runGameAct);
                runGameAct.runOnUiThread(dial);
                game.getGameControlView().drawSky();
            }

            if (!mFinished)
                return null;
            else if (game.getCosmos().moreRoundsRemaining())
                return BuyWeaponsState.create();
            else
                return AnnounceWinnerState.create();
        }

        @Override
        public int getBlockingDelay() {
            return mDisplayActive ? 0 : 100;
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
        private void initialize(int initialDelay) {
            mFinished = false;
            mDisplayActive = false;
            mDisplayTime = System.currentTimeMillis() + initialDelay;
        }

        public static LeaderboardState create(int initialDelay) {
            sMe.initialize(initialDelay);
            return sMe;
        }

        public static LeaderboardState createFromBundle(Bundle map) {
            sMe.initialize(0);
            return sMe;
        }

        private LeaderboardState() { }
    }

    ///** Displays the "and the winner is..." message */
    public static class AnnounceWinnerState extends GameState {
        /*================= Constants =================*/
        public static final byte ID = 1;

        /*================= Static =================*/
        private static AnnounceWinnerState sMe = new AnnounceWinnerState();

        /*================= Data =================*/
        private boolean mFinished;

        private boolean mDisplayActive;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        @Override
        public void onEnter(RunGameActAccessor game) {
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            if (!mDisplayActive) {
                RunGameAct runGameAct = game.getRunGameAct();
                StartAnnounceWinnerDialog dial =
                    new StartAnnounceWinnerDialog(runGameAct);
                runGameAct.runOnUiThread(dial);
                game.getGameControlView().drawSky();
                mDisplayActive = true;
            }

            if (mFinished) {
                game.getRunGameAct().endGame();
                return null;
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
            mDisplayActive = false;
        }

        public static AnnounceWinnerState create() {
            sMe.initialize();
            return sMe;
        }

        public static AnnounceWinnerState createFromBundle(Bundle map) {
            sMe.initialize();
            return sMe;
        }

        private AnnounceWinnerState() { }
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
            RunGameAct runGameAct = game.getRunGameAct();
            StartBuyWeaponsDialog dial =
                new StartBuyWeaponsDialog(runGameAct,
                        game.getCosmos(),
                        game.getModel().getPlayers()[0]);
            runGameAct.runOnUiThread(dial);

            game.getGameControlView().drawSky();
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            return (mFinished) ? TurnStartState.create() : null;
        }

        @Override
        public void onExit(RunGameActAccessor game) {
            game.getRunGameAct().startRound(false);
            game.getRunGameAct().continueRound();
        }

        @Override
        public int getBlockingDelay() {
            return 0;
        }

        @Override
        public boolean onButton(RunGameActAccessor game, GameButton b) {
            if (b == GameButton.DONE) {
                mFinished = true;
                return true;
            }
            else {
                return false;
            }
        }

        /*================= Lifecycle =================*/
        private void initialize() {
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
        public static final int AFTER_ROUND_PAUSE = 3000;

        /*================= Static =================*/
        private static TurnStartState sMe = new TurnStartState();

        /*================= Data =================*/
        private Model.NextTurnInfo mInfo;
        private Brain.Move mMove;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        @Override
        public void onEnter(RunGameActAccessor game) {
            game.getModel().getNextPlayerInfo(mInfo);
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            if (mInfo.isDraw()) {
                // TODO: display "it was a draw!" or similar
                return LeaderboardState.create(AFTER_ROUND_PAUSE);
            }
            else if (mInfo.curPlayerHasWon()) {
                // Someone won the round.
                // TODO: display "foo wins" or similar
                // TODO: add gold to account, or whatever
                game.getCosmos().getPlayerInfo()
                    [ game.getModel().getCurPlayer().getId() ].
                        earnMoney(Explosion.SURVIVOR_BONUS);
                return LeaderboardState.create(AFTER_ROUND_PAUSE);
            }
            else {
                Model model = game.getModel();
                int nextPlayerId = mInfo.getNextPlayerId();
                Player play = model.getPlayers()[nextPlayerId];
                Util.DoToast doToast = new Util.DoToast(
                    game.getGameControlView().getContext(),
                    play.getIntroductionString());

                model.setCurPlayerId(mInfo.getNextPlayerId());

                play.getBrain().makeMove(game, mMove);
                if (mMove.isHuman())
                    return HumanMoveState.create();
                else
                    return ComputerMoveState.create(mMove);
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
            mMove = new Brain.Move();
        }
    }

    /** Superclass of HumanMoveState and ComputerMoveState */
    public abstract static class MoveState extends GameState {
        /*================= Constants =================*/
        private static final long MAX_FIRE_TIME = 2400;

        /*================= Static =================*/

        /*================= Data =================*/

        /*================= Operations =================*/
        @Override
        public void onEnter(RunGameActAccessor game) {
            GameState.setCurPlayerAngleText(game);
            game.getGameControlView().cacheTerrain(game);
            game.getRunGameAct().runOnUiThread(new DoShowArmory(game));
            GameState.setCurPlayerArmoryText(game);
            game.getModel().getCurPlayer().setAuraAlpha(
                Player.SELECTED_AURA_ALPHA);
        }

        /** Calculate what the power should be, given the current time, and
         * the time that the user pressed the fire button.
         */
        protected int timeToPower(long diff) {
            if (diff > MAX_FIRE_TIME)
                return Player.MAX_POWER;
            else {
                return (int)((diff * Player.MAX_POWER) / MAX_FIRE_TIME);
            }
        }

        /** Given a specified power, and a starting time, calculate how
         * long the animation should go on.
         */
        protected long powerToDuration(int power) {
            if ((power < 0) || (power > Player.MAX_POWER)) {
                throw new RuntimeException("can't have power = " + power);
            }
            return (power * MAX_FIRE_TIME) / Player.MAX_POWER;
        }

        /** Execute a non-projectile move.
         *
         * @return        the state we're in after the move, or null if
         *                the move was aborted. Computer moves should never be
         *                aborted.
         */
        protected GameState doNonProjectileMove(RunGameActAccessor game,
                                        boolean isHuman) {
            Player curPlayer = game.getModel().getCurPlayer();
            WeaponType weapon = curPlayer.getCurWeaponType();
            if (weapon.isTeleporter()) {
                Armory armory = curPlayer.getArmory(game.getCosmos());
                armory.useWeapon(weapon);
                if (armory.getAmount(weapon) == 0) {
                    curPlayer.setCurWeaponType(armory.
                        getNextWeapon(curPlayer.getCurWeaponType()));
                }
                return doTeleport(game);
            }
            else if (weapon.isExtraArmor()) {
                if (isHuman) {
                    // Special sanity check for humans only. Will bring up
                    // dialog box if we can't use the armor.
                    if (!curPlayer.canUseExtraArmor()) {
                        ExtraArmorState.notifyPlayerThatArmorIsMaxed(game);
                        return null;
                    }
                }
                Armory armory = curPlayer.getArmory(game.getCosmos());
                curPlayer.setCurWeaponType(armory.
                        getNextWeapon(curPlayer.getCurWeaponType()));
                return ExtraArmorState.create();
            }
            else {
                throw new RuntimeException("don't know how to handle " +
                            "firing weapon: " + weapon.toString());
            }
        }

        /** Execute a projectile move.
         *
         * @return        the state we're in after the move
         */
        protected GameState doProjectileMove(RunGameActAccessor game,
                                          int power) {
            Player curPlayer = game.getModel().getCurPlayer();
            WeaponType weapon = curPlayer.getCurWeaponType();
            Armory arm = curPlayer.getArmory(game.getCosmos());
            arm.useWeapon(weapon);
            if (arm.getAmount(weapon) == 0)
                curPlayer.setCurWeaponType(arm.getNextWeapon(weapon));
            return BallisticsState.create(power, weapon);
        }

        @Override
        public void onExit(RunGameActAccessor game) {
            GameState.setCustomAngleText(game, EMPTY_STRING);
            GameState.clearCurPlayerArmoryText(game);
        }

        class DoShowArmory implements Runnable {
            private RunGameActAccessor mGame;
            public void run() {
                showArmory(mGame);
            }
            DoShowArmory(RunGameActAccessor game) {
                mGame = game;
            }
        }
    }

    /** A human turn. We will accept input from the touchscreen and do all
     * that stuff. */
    public static class HumanMoveState extends MoveState {
        /*================= Constants =================*/
        public static final byte ID = 15;

        /*================= Static =================*/
        private static HumanMoveState sMe = new HumanMoveState();

        /*================= Data =================*/
        /** True only if the user pressed the fire button to fire a
         * special weapon. */
        private boolean mFireSpecial;

        /** The time at which the user pressed the fire button */
        private long mFireTime;

        /** The time at which the user released the fire button, or 0 if
         * the user has not yet released the fire button. */
        private long mFireReleaseTime;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        @Override
        public void onEnter(RunGameActAccessor game) {
            super.onEnter(game);
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            if (mFireSpecial) {
                GameState ret = doNonProjectileMove(game, true);
                if (ret != null)
                    return ret;
                mFireSpecial = false;
            }

            int power = 0;
            if (mFireTime == 0) {
                game.getGameControlView().
                    drawScreen(game, Player.INVALID_POWER,
                            Projectile.EMPTY_ARRAY, Explosion.EMPTY_ARRAY);
            }
            else {
                power = timeToPower(System.currentTimeMillis() - mFireTime);
                game.getGameControlView().drawScreen(game, power,
                        Projectile.EMPTY_ARRAY, Explosion.EMPTY_ARRAY);
                if (power == Player.MAX_POWER)
                    doReleaseFire(game);
            }
            if (mFireReleaseTime == 0) {
                // The user hasn't released the fire button yet. Continue in
                // this state.
                return null;
            }
            else {
                // The user released the fire button
                return doProjectileMove(game, power);
            }
        }

        @Override
        public void onExit(RunGameActAccessor game) {
            super.onExit(game);
        }

        @Override
        public int getBlockingDelay() {
            if (mFireTime == 0) {
                // If the user hasn't pressed the fire button yet, block
                // until we get some input.
                return 0;
            }
            else {
                // If the user has already pressed the fire button, update
                // every 1 ms or so
                return 1;
            }
        }

        private void doReleaseFire(RunGameActAccessor game) {
            if (mFireTime != 0) {
                mFireReleaseTime = System.currentTimeMillis();
                game.getRunGameAct().runOnUiThread(new DoShowArmory(game));
            }
        }

        @Override
        public boolean onButton(RunGameActAccessor game, GameButton b) {
            switch (b) {
                case ARMORY_LEFT: {
                    Player curPlayer = game.getModel().getCurPlayer();
                    Armory arm = curPlayer.getArmory(game.getCosmos());
                    curPlayer.setCurWeaponType(
                        arm.getPrevWeapon(
                            curPlayer.getCurWeaponType()));
                    GameState.setCurPlayerArmoryText(game);
                    return true;
                }
                case ARMORY_RIGHT: {
                    Player curPlayer = game.getModel().getCurPlayer();
                    Armory arm = curPlayer.getArmory(game.getCosmos());
                    curPlayer.setCurWeaponType(
                        arm.getNextWeapon(
                            curPlayer.getCurWeaponType()));
                    GameState.setCurPlayerArmoryText(game);
                    return true;
                }
                case PRESS_FIRE: {
                    Player curPlayer = game.getModel().getCurPlayer();
                    if (curPlayer.getCurWeaponType().isProjectile()) {
                        hideArmory(game);
                        mFireTime = System.currentTimeMillis();
                    }
                    else {
                        highlightFireButton(game);
                    }
                    return true;
                }
                case RELEASE_FIRE: {
                    Player curPlayer = game.getModel().getCurPlayer();
                    if (curPlayer.getCurWeaponType().isProjectile()) {
                        doReleaseFire(game);
                    }
                    else {
                        deHighlightFireButton(game);
                        mFireSpecial = true;
                    }
                    return true;
                }
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
            mFireSpecial = false;
            mFireTime = 0;
            mFireReleaseTime = 0;
        }

        public static HumanMoveState create() {
            sMe.initialize();
            return sMe;
        }

        public static HumanMoveState createFromBundle(Bundle map) {
            sMe.initialize();
            return sMe;
        }

        public HumanMoveState() {
        }
    }

    /** A computer turn. We use the Move object to determine what to do.
     * This code is concerned with animating the action.
     */
    public static class ComputerMoveState extends MoveState {
        /*================= Constants =================*/
        public static final byte ID = 16;

        /*================= Static =================*/
        private static ComputerMoveState sMe = new ComputerMoveState();

        /*================= Types =================*/
        private abstract class Stage {
            /*================= Data =================*/
            private final long mTimeAfterStart;

            /*================= Access =================*/
            public long getTimeAfterStart() {
                return mTimeAfterStart;
            }

            /*================= Operations =================*/
            public abstract GameState doStage(RunGameActAccessor game);

            /*================= Lifecycle =================*/
            private Stage(long timeAfterStart) {
                mTimeAfterStart = timeAfterStart;
            }
        }

        private class Initial extends Stage {
            /*================= Operations =================*/
            public GameState doStage(RunGameActAccessor game) {
                // just draw the screen
                game.getGameControlView().
                    drawScreen(game, Player.INVALID_POWER,
                            Projectile.EMPTY_ARRAY, Explosion.EMPTY_ARRAY);
                return null;
            }

            /*================= Lifecycle =================*/
            private Initial() {
                super(0);
            }
        }

        private class SetWeapon extends Stage {
            /*================= Operations =================*/
            public GameState doStage(RunGameActAccessor game) {
                // change which weapon we're pointing to in the armory
                Player curPlayer = game.getModel().getCurPlayer();
                curPlayer.setCurWeaponType(mMove.getWeapon());
                GameState.setCurPlayerArmoryText(game);

                game.getGameControlView().
                    drawScreen(game, Player.INVALID_POWER,
                            Projectile.EMPTY_ARRAY, Explosion.EMPTY_ARRAY);
                return null;
            }

            /*================= Lifecycle =================*/
            private SetWeapon() {
                super(500);
            }
        }

        private class SetTurret extends Stage {
            /*================= Operations =================*/
            public GameState doStage(RunGameActAccessor game) {
                // No need to rotate the turret for non-projectile moves
                if (!mMove.isProjectile())
                    return null;

                // Change where turret is pointing
                game.getModel().getCurPlayer().setAngleDeg(mMove.getAngle());
                GameState.setCurPlayerAngleText(game);

                game.getGameControlView().
                    drawScreen(game, Player.INVALID_POWER,
                            Projectile.EMPTY_ARRAY, Explosion.EMPTY_ARRAY);
                return null;
            }

            /*================= Lifecycle =================*/
            private SetTurret() {
                super(1000);
            }
        }

        private class StartFire extends Stage {
            /*================= Operations =================*/
            public GameState doStage(RunGameActAccessor game) {
                // For non-projectile moves, we immediately transition to
                // some other state
                if (!mMove.isProjectile()) {
                    return doNonProjectileMove(game, false);
                }

                // For projectile moves, we set the stage for the firing
                // animation
                mFireStartTime = System.currentTimeMillis();
                mFireReleaseTime = mFireStartTime +
                                powerToDuration(mMove.getPower());
                return null;
            }

            /*================= Lifecycle =================*/
            private StartFire() {
                super(2500);
            }
        }

        /*================= Data =================*/
        /** All of the animation stages */
        private final Stage mStages[];

        /** The move we're going to make */
        private Brain.Move mMove;

        /** The current animation stage */
        private int mCurStage;

        /** The time when the state began */
        private long mStartTime;

        /** The time when we started pressing the fire button */
        private long mFireStartTime;

        /** The time when we're going to release the fire button */
        private long mFireReleaseTime;

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
            mMove.saveState(map);
        }

        @Override
        public void onEnter(RunGameActAccessor game) {
            super.onEnter(game);
            mCurStage = 0;
            mStartTime = System.currentTimeMillis();
            mFireStartTime = 0;
            mFireReleaseTime = 0;
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            long curTime = System.currentTimeMillis();
            if (mCurStage < mStages.length) {
                long diff = curTime - mStartTime;
                Stage stage = mStages[mCurStage];
                if (diff > stage.getTimeAfterStart()) {
                    mCurStage++;
                    GameState ret = stage.doStage(game);
                    if (ret != null)
                        return ret;
                }
            }

            if (mFireReleaseTime == 0)
                return null;
            else {
                if (curTime > mFireReleaseTime) {
                    return doProjectileMove(game, mMove.getPower());
                }
                // Draw the power bar
                int power = timeToPower(curTime - mFireStartTime);
                game.getGameControlView().drawScreen(game, power,
                        Projectile.EMPTY_ARRAY, Explosion.EMPTY_ARRAY);
                return null;
            }
        }

        @Override
        public void onExit(RunGameActAccessor game) {
            GameState.setCustomAngleText(game, EMPTY_STRING);
            GameState.clearCurPlayerArmoryText(game);
        }

        @Override
        public int getBlockingDelay() {
            return 1;
        }

        /*================= Lifecycle =================*/
        private void initialize(Brain.Move move) {
            mMove = move;
        }

        public static ComputerMoveState create(Brain.Move move) {
            sMe.initialize(move);
            return sMe;
        }

        public static ComputerMoveState createFromBundle(Bundle map) {
            Brain.Move move = Brain.Move.fromBundle(map);
            sMe.initialize(move);
            return sMe;
        }

        public ComputerMoveState() {
            mStages = new Stage[4];
            mStages[0] = new Initial();
            mStages[1] = new SetWeapon();
            mStages[2] = new SetTurret();
            mStages[3] = new StartFire();
        }
    }

    /** Draw missiles flying through the sky. The fun state. */
    public static class BallisticsState extends GameState {
        /*================= Constants =================*/
        public static final byte ID = 20;
        public static final String BALLISTICS_POWER = "BALLISTICS_POWER";
        public static final String WEAPON_TYPE = "WEAPON_TYPE";
        private static final int MAX_PROJECTILES = 6;

        /*================= Types =================*/
        /** The Accessor is a convenient way for other classes to interface
         * with BallisticsState */
        public class Accessor {
            /** Returns a valid uninitialized Projectile object */
            public Projectile newProjectile() {
                for (Projectile proj : mProjectiles) {
                    if (! proj.getInUse())
                        return proj;
                }
                throw new RuntimeException("newProjectile: there are " +
                    "no more empty slots for projectiles. Perhaps " +
                    "you should increase the number of slots?");
            }

            /** Returns a valid uninitialized Explosion object */
            public Explosion newExplosion() {
                for (Explosion expl : mExplosions) {
                    if (! expl.getInUse())
                        return expl;
                }
                throw new RuntimeException("newExplosion: there are " +
                    "no more empty slots for explosions. Perhaps " +
                    "you should increase the number of slots?");
            }

            /** Returns the perpetrator of these explosions (the current
             * player) */
            public int getPerp() {
                return mCurPlayerId;
            }
        }

        /*================= Static =================*/
        private static BallisticsState sMe = new BallisticsState();

        /*================= Data =================*/
        private int mPower;
        private WeaponType mInitWeapon;

        private Projectile mProjectiles[];
        private Explosion mExplosions[];
        private Accessor mAcc;
        private int mCurPlayerId;

        /*================= Access =================*/

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
            map.putInt(BALLISTICS_POWER, mPower);
            map.putInt(WEAPON_TYPE, mInitWeapon.ordinal());
        }

        @Override
        public void onEnter(RunGameActAccessor game) {
            Model model = game.getModel();
            Player curPlayer = model.getCurPlayer();
            mCurPlayerId = curPlayer.getId();

            Projectile.launchProjectile(
                model, curPlayer.getAngleRad(), mPower, mInitWeapon,
                mAcc.newProjectile());

            game.getGameControlView().cacheTerrain(game);
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            boolean finished = true;
            final Model model = game.getModel();
            final Player allPlayers[] = model.getPlayers();
            for (Projectile proj : mProjectiles) {
                if (!proj.getInUse())
                    continue;
                finished = false;
                proj.step(model, mAcc);
            }
            for (Explosion expl : mExplosions) {
                if (! expl.getInUse())
                    continue;
                finished = false;
                if (expl.getFinished(System.currentTimeMillis())) {
                    expl.clearInUse();
                    expl.doDirectDamage(game);
                    expl.editTerrain(game);
                    game.getGameControlView().cacheTerrain(game);

                    Terrain terrain = model.getTerrain();
                    for (Player victim : allPlayers) {
                        if (victim.doFalling(terrain)) {
                            // notify Brains about the fall
                            int perp = expl.getPerp();
                            for (Player p : allPlayers) {
                                if (! p.isAlive())
                                    continue;
                                p.getBrain().notifyPlayerFell(
                                    perp, victim.getId());
                            }
                        }
                    }
                }
            }

            for (Player p : allPlayers) {
                if (p.getDeathExplosionPending()) {
                    finished = false;
                    p.resetDeathExplosion();
                    WeaponType.PLAYER_DEATH.detonate(model,
                        p.getX(), p.getY(), mAcc);
                }
            }

            game.getGameControlView().
                drawScreen(game, Player.INVALID_POWER,
                           mProjectiles, mExplosions);

            if (finished)
                return TurnStartState.create();
            else
                return null;
        }

        @Override
        public void onExit(RunGameActAccessor game) {
            game.getModel().getCurPlayer().setAuraAlpha(
                    Player.DESELECTED_AURA_ALPHA);
        }

        @Override
        public int getBlockingDelay() {
            return 1;
        }

        /*================= Lifecycle =================*/
        private void initialize(int power, WeaponType weapon) {
            mPower = power;
            mInitWeapon = weapon;
            for (Projectile p : mProjectiles)
                p.changeInUse(false);
            for (Explosion e : mExplosions)
                e.clearInUse();
        }

        public static BallisticsState create(int power,
                                            WeaponType initWeapon) {
            sMe.initialize(power, initWeapon);
            return sMe;
        }

        public static BallisticsState createFromBundle(Bundle map) {
            int power = map.getInt(BALLISTICS_POWER);
            int wType = map.getInt(WEAPON_TYPE);
            WeaponType weapons[] = WeaponType.values();
            WeaponType initWeapon = weapons[wType];
            sMe.initialize(power, initWeapon);
            return sMe;
        }

        private BallisticsState() {
            mProjectiles = new Projectile[MAX_PROJECTILES];
            for (int i = 0; i < mProjectiles.length; i++) {
                mProjectiles[i] = new Projectile();
            }
            mExplosions = new Explosion[MAX_PROJECTILES + Model.MAX_PLAYERS];
            for (int i = 0; i < mExplosions.length; i++) {
                mExplosions[i] = new Explosion();
            }
            mAcc = new Accessor();
        }
    }

    /** Animate a player teleporting */
    public static class TeleportState extends GameState {
        /*================= Constants =================*/
        public static final byte ID = 25;

        /*================= Types =================*/
        private static interface SpecialEffect {
            /** Apply a special effect that is "percent" complete to
             * Player p */
            public void applyEffect(Player p, int percent);
        }

        enum State {
            START_AURA_WHITENING(1000, new SpecialEffect() {
                public void applyEffect(Player p, int percent) {
                    p.setAuraWhitening(percent);
                }
            }),
            START_FADE(1000, new SpecialEffect() {
                public void applyEffect(Player p, int percent) {
                    p.setFadeAmount(percent);
                }
            }),
            START_PAUSE(500, new SpecialEffect() {
                public void applyEffect(Player p, int percent) {
                }
            }),
            START_FADE_IN(1000, new SpecialEffect() {
                public void applyEffect(Player p, int percent) {
                    p.setFadeAmount(100 - percent);
                }
            }),
            START_AURA_DIMMING(1000, new SpecialEffect() {
                public void applyEffect(Player p, int percent) {
                    p.setAuraWhitening(100 -percent);
                }
            });

            /*================= Data =================*/
            /** How many milliseconds this state lasts */
            private int mDuration;

            /** The special effect to apply to the player while we're in this
              * state */
            private SpecialEffect mSpecialEffect;

            /*================= Access =================*/
            public int getDuration() {
                return mDuration;
            }

            /*================= Operations =================*/
            public void applySpecialEffect(Player p, int percent) {
                mSpecialEffect.applyEffect(p, percent);
            }

            /*================= Lifecycle =================*/
            private State(int duration, SpecialEffect specialEffect) {
                mDuration = duration;
                mSpecialEffect = specialEffect;
            }
        }

        /*================= Static =================*/
        private static TeleportState sMe = new TeleportState();

        /*================= Data =================*/
        public class MyVars {
            /** Index of the first player being teleported */
            public int mP1Index;

            /** Index of the second player being teleported, or
             * INVALID_PLAYER_ID if there is no second player being
             * teleported. */
            public int mP2Index;

            /** Starting X of the first player being teleported */
            public int mP1x0;

            /** Starting X of the second player being teleported */
            public int mP2x0;

            /** Destination X of the first player being teleported */
            public int mP1xf;

            /** Destination X of the second player being teleported */
            public int mP2xf;
        }

        private MyVars mV;

        /** The current animation state */
        private State mCurState;

        /** The time when the current animation state began */
        private long mStateStartTime;


        /*================= Access =================*/

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
            AutoPack.autoPack(map, AutoPack.EMPTY_STRING, mV);
        }

        @Override
        public void onEnter(RunGameActAccessor game) {
            Model model = game.getModel();
            stateTransition(State.values()[0]);

            // Place players in original positions
            Player p1 = getPlayer1(model);
            p1.setX(mV.mP1x0, model.getTerrain());
            Player p2 = getPlayer2(model);
            if (p2 != null)
                p2.setX(mV.mP2x0, model.getTerrain());

            // display Toasts
            StringBuilder s = new StringBuilder(80);
            s.append(p1.getName());
            if ((p2 != null) && (p2.isAlive())) {
                s.append(" has switched places with ");
                s.append(p2.getName()).append("!");
            }
            else
                s.append(" has teleported!");
            Util.DoToast doToast = new Util.DoToast(
                game.getGameControlView().getContext(),
                s.toString());
            game.getRunGameAct().runOnUiThread(doToast);
        }

        private void stateTransition(State val) {
            mCurState = val;
            mStateStartTime = System.currentTimeMillis();
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            Model model = game.getModel();
            Player p1 = getPlayer1(model);
            Player p2 = getPlayer2(model);
            long d = System.currentTimeMillis() - mStateStartTime;
            int percent;
            if (d >= mCurState.getDuration())
                percent = 100;
            else {
                percent = (int) ((d * 100) / mCurState.getDuration());
            }
            mCurState.applySpecialEffect(p1, percent);
            if ((p2 != null) && (p2.isAlive()))
                mCurState.applySpecialEffect(p2, percent);

            game.getGameControlView().
                drawScreen(game, Player.INVALID_POWER,
                        Projectile.EMPTY_ARRAY, Explosion.EMPTY_ARRAY);

            if (percent == 100) {
                State values[] = State.values();
                int next = mCurState.ordinal() + 1;
                if (next == values.length)
                    return TurnStartState.create();
                stateTransition(values[next]);

                if (mCurState == State.START_PAUSE) {
                    // Swap players
                    p1.setX(mV.mP1xf, model.getTerrain());
                    if (p2 != null)
                        p2.setX(mV.mP2xf, model.getTerrain());
                }
            }

            return null;
        }

        @Override
        public void onExit(RunGameActAccessor game) {
            game.getModel().getCurPlayer().setAuraAlpha(
                    Player.DESELECTED_AURA_ALPHA);

            // notify Brains about the teleport
            for (Player p : game.getModel().getPlayers()) {
                if (! p.isAlive())
                    continue;
                Brain brain = p.getBrain();
                brain.notifyPlayerTeleported(mV.mP1Index);
                if ((mV.mP2Index != Player.INVALID_PLAYER_ID) &&
                    (game.getModel().getPlayers()[mV.mP2Index].isAlive()))
                    brain.notifyPlayerTeleported(mV.mP2Index);
            }
        }

        @Override
        public int getBlockingDelay() {
            return 1;
        }

        private Player getPlayer1(Model model) {
            return model.getPlayers()[mV.mP1Index];
        }

        private Player getPlayer2(Model model) {
            if (mV.mP2Index == Player.INVALID_PLAYER_ID)
                return null;
            else {
                return model.getPlayers()[mV.mP2Index];
            }
        }

        /*================= Lifecycle =================*/
        private void initialize(MyVars v) {
            mV = v;
        }

        public void initialize(int p1index, int p2index,
                               int p1x0, int p2x0,
                               int p1xf, int p2xf) {
            mV.mP1Index = p1index; mV.mP2Index = p2index;
            mV.mP1x0 = p1x0; mV.mP2x0 = p2x0;
            mV.mP1xf = p1xf; mV.mP2xf = p2xf;
        }

        public static TeleportState create(int p1Index, int p2index,
                                           int p1x0, int p2x0,
                                           int p1xf, int p2xf) {
            sMe.initialize(p1Index, p2index,
                            p1x0, p2x0,
                            p1xf, p2xf);
            return sMe;
        }

        public static TeleportState createFromBundle(Bundle map) {
            MyVars v = (MyVars)AutoPack.
                autoUnpack(map, AutoPack.EMPTY_STRING, MyVars.class);
            sMe.initialize(v);
            return sMe;
        }

        private TeleportState() {
            mCurState = null;
            mStateStartTime = 0;
            mV = new MyVars();
        }
    }

    /** Animate a player using extra armor */
    public static class ExtraArmorState extends GameState {
        /*================= Constants =================*/
        public static final byte ID = 26;
        public static final int EXTRA_ARMOR_AMOUNT = 100;

        /*================= Types =================*/
        /** Runnable that creates a dialog box and displays it */
        public static class DoDialog implements Runnable {
            /*================= Data =================*/
            private Context mContext;
            private String mString;

            /*================= Operations =================*/
            public void run() {
                AlertDialog.Builder b = new AlertDialog.Builder(mContext);

                b.setMessage(mString);
                b.setCancelable(true);
                b.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                        }
                    });
                b.show();
            }

            /*================= Lifecycle =================*/
            public DoDialog(Context context, String string) {
                mContext = context;
                mString = string;
            }
        }

        /*================= Static =================*/
        private static ExtraArmorState sMe = new ExtraArmorState();

        /** Display an alert dialog box informing the (human) player that
         * it is futile to use extra armor, since he is already at
         * maximum life.
         */
        private static void
                notifyPlayerThatArmorIsMaxed(RunGameActAccessor game) {
            DoDialog doDialog = new DoDialog(
                game.getGameControlView().getContext(),
                "Can't use Extra Armor.\nYou already " +
                "have maximum armor!");
            game.getRunGameAct().runOnUiThread(doDialog);
        };

        /*================= Data =================*/
        /** The time when the state began */
        private long mStartTime;

        /*================= Access =================*/

        /*================= Operations =================*/
        @Override
        public void saveState(Bundle map) {
            map.putByte(GAME_STATE_ID, ID);
        }

        @Override
        public void onEnter(RunGameActAccessor game) {
            Player curPlayer = game.getModel().getCurPlayer();

            mStartTime = System.currentTimeMillis();

            // display Toast
            StringBuilder s = new StringBuilder(80);
            s.append(curPlayer.getName());
            s.append(" has gained extra armor!");
            Util.DoToast doToast = new Util.DoToast(
                game.getGameControlView().getContext(),
                s.toString());
            game.getRunGameAct().runOnUiThread(doToast);
        }

        @Override
        public GameState main(RunGameActAccessor game) {
            Player curPlayer = game.getModel().getCurPlayer();
            long d = System.currentTimeMillis() - mStartTime;
            boolean finished = false;

            if (d < 2000) {
                int percent = (int) ((d * 100) / 2000);
                curPlayer.setAuraWhitening(percent);
            }
            else {
                int percent = (int) (((d - 2000) * 100) / 2000);
                if (percent > 100) {
                    finished = true;
                    percent = 100;
                }
                curPlayer.setAuraWhitening(100 - percent);
            }

            game.getGameControlView().
                drawScreen(game, Player.INVALID_POWER,
                        Projectile.EMPTY_ARRAY, Explosion.EMPTY_ARRAY);

            if (finished)
                return TurnStartState.create();
            else
                return null;
        }

        @Override
        public void onExit(RunGameActAccessor game) {
            Player curPlayer = game.getModel().getCurPlayer();
            curPlayer.setAuraAlpha(Player.DESELECTED_AURA_ALPHA);
            curPlayer.gainLife(EXTRA_ARMOR_AMOUNT);
        }

        @Override
        public int getBlockingDelay() {
            return 1;
        }

        /*================= Lifecycle =================*/
        private void initialize() {
        }

        public static ExtraArmorState create() {
            sMe.initialize();
            return sMe;
        }

        public static ExtraArmorState createFromBundle(Bundle map) {
            sMe.initialize();
            return sMe;
        }

        private ExtraArmorState() {
            mStartTime = 0;
        }
    }

    /*================= Static =================*/
    /** Helper function which takes a list of X positions and a list of
     * players, and returns the first unused X position
     */
    private static int getFirstUnusedXPosition(List < Short > positions,
                                              Player players[]) {
        for (Short s : positions) {
            boolean used = false;
            int x = s.intValue();
            for (Player p : players) {
                if (p.getX() == x)
                    used = true;
            }
            if (used == false)
                return x;
        }
        throw new RuntimeException("getUnusedPosition: all " +
            "X positions are used");
    }

    /** Figures out where to teleport and transitions to TeleportState.
     *
     * @return  The new GameState
     */
    private static GameState doTeleport(RunGameActAccessor game) {
        Model model = game.getModel();
        Player players[] = model.getPlayers();
        Player curPlayer = model.getCurPlayer();
        int p1index = model.getCurPlayerId();
        int p2index;
        int p1x0;
        int p2x0;
        int p1xf;
        int p2xf;

        // Look for dead players
        if (! curPlayer.isAlive()) {
            throw new RuntimeException("current player is dead!");
        }
        LinkedList < Player > deadPlayers = new LinkedList < Player >();
        for (Player p : model.getPlayers()) {
            if (! p.isAlive())
                deadPlayers.add(p);
        }
        if (deadPlayers.size() != 0) {
            // Switch places with a dead player
            int i = Util.mRandom.nextInt(deadPlayers.size());
            p2index = deadPlayers.get(i).getId();
            p1x0 = players[p1index].getX();
            p2x0 = players[p2index].getX();
            p1xf = p2x0;
            p2xf = p1x0;
        }
        else {
            // There are no dead players to switch places with.
            if (model.getPlayers().length == 2) {
                // Special case for 2 player games: there is a lot of room
                // when there are only two players.
                //
                // Rather than using the normal algorithm, check which of the
                // valid 3-player game starting spots is not in use, and move
                // the teleporting player to there.
                //
                // Please note: this only applies to 2-player games.
                // Dead player entries still appear in Model.mPlayers,
                // so we won't have Model.mPlayer.length == 2 unless we only
                // started with 2.
                List < Short > positions =
                    ModelFactory.getValidPlayerPlacements(3);
                int x = getFirstUnusedXPosition(positions, players);
                p2index = Player.INVALID_PLAYER_ID;
                p1x0 = players[p1index].getX();
                p2x0 = 0;
                p1xf = x;
                p2xf = 0;
            }
            else {
                // Switch places with a living player
                p2index = Util.mRandom.nextInt(players.length - 1);
                // make sure that we don't swap p1 with itself
                if (p2index >= p1index)
                    p2index++;
                p1x0 = players[p1index].getX();
                p2x0 = players[p2index].getX();
                p1xf = p2x0;
                p2xf = p1x0;
            }
        }

        return TeleportState.create(p1index, p2index,
                                    p1x0, p2x0,
                                    p1xf, p2xf);
    }

    private static void setCurPlayerArmoryText(RunGameActAccessor game) {
        Player curPlayer = game.getModel().getCurPlayer();
        WeaponType type = curPlayer.getCurWeaponType();
        Armory arm = curPlayer.getArmory(game.getCosmos());
        int amount = arm.getAmount(type);

        TextView armoryMain = game.getArmoryMainText();
        TextView armorySecondary = game.getArmorySecondaryText();

        game.getRunGameAct().runOnUiThread(new SetTextView(armoryMain,
                                                           type.getName()));
        StringBuilder b = new StringBuilder(14);
        b.append("[");
        if (amount == WeaponType.Const.UNLIMITED)
            b.append("∞");
        else
            b.append(amount);
        b.append("]");
        game.getRunGameAct().runOnUiThread(new SetTextView(armorySecondary,
                                                           b.toString()));
    }

    private static void clearCurPlayerArmoryText(RunGameActAccessor game) {
        TextView armoryMain = game.getArmoryMainText();
        TextView armorySecondary = game.getArmorySecondaryText();

        game.getRunGameAct().runOnUiThread(
            new SetTextView(armoryMain, EMPTY_STRING));
        game.getRunGameAct().runOnUiThread(
            new SetTextView(armorySecondary, EMPTY_STRING));
    }

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
        hideArmoryTextView(game.getAngleText());
        hideArmoryTextView(game.getArmoryMainText());
        hideArmoryTextView(game.getArmorySecondaryText());
        hideArmoryTextView(game.getArmoryLeftButton());
        hideArmoryTextView(game.getArmoryRightButton());
        highlightFireButton(game);
        int color = game.getXmlColors().getClear();
        game.getArmoryCenter().setBackgroundColor(color);
    }

    private static void highlightFireButton(RunGameActAccessor game) {
        game.getFireButton().setTextColor(Color.argb(0xff, 0xff, 0xff, 0xff));
    }

    private static void hideArmoryTextView(TextView t) {
        t.setVisibility(View.INVISIBLE);
    }

    /** Un-hides the armory in the middle of the screen */
    private static void showArmory(RunGameActAccessor game) {
        XmlColors xmlColors = game.getXmlColors();
        int textColor = game.getModel().foregroundIsLight() ?
                            xmlColors.getGameTextDark() :
                            xmlColors.getGameTextGrey();

        showArmoryTextView(game.getAngleText(), textColor);
        showArmoryTextView(game.getArmoryMainText(), textColor);
        showArmoryTextView(game.getArmorySecondaryText(), textColor);
        showArmoryTextView(game.getArmoryLeftButton(), textColor);
        showArmoryTextView(game.getArmoryRightButton(), textColor);
        deHighlightFireButton(game);
        int bgColor = game.getXmlColors().getArmoryBackground();
        game.getArmoryCenter().setBackgroundColor(bgColor);
    }

    private static void showArmoryTextView(TextView t, int textColor) {
        t.setVisibility(View.VISIBLE);
        t.setTextColor(textColor);
    }

    private static void deHighlightFireButton(RunGameActAccessor game) {
        XmlColors xmlColors = game.getXmlColors();
        int textColor = game.getModel().foregroundIsLight() ?
                            xmlColors.getGameTextDark() :
                            xmlColors.getGameTextGrey();
        game.getFireButton().setTextColor(textColor);
    }

    /** Initialize and return a game state object from a Bundle */
    public static GameState fromBundle(Bundle map) {
        byte id = map.getByte(GAME_STATE_ID);
        switch (id) {
            case LeaderboardState.ID:
                return LeaderboardState.createFromBundle(map);
            case AnnounceWinnerState.ID:
                return AnnounceWinnerState.createFromBundle(map);
            case BuyWeaponsState.ID:
                return BuyWeaponsState.createFromBundle(map);
            case TurnStartState.ID:
                return TurnStartState.createFromBundle(map);
            case HumanMoveState.ID:
                return HumanMoveState.createFromBundle(map);
            case ComputerMoveState.ID:
                return ComputerMoveState.createFromBundle(map);
            case BallisticsState.ID:
                return BallisticsState.createFromBundle(map);
            case TeleportState.ID:
                return TeleportState.createFromBundle(map);
            default:
                throw new RuntimeException("can't recognize state with ID = "
                                            + id);
        }
    }

    public static GameState createInitialGameState() {
        return BuyWeaponsState.create();
    }
}
