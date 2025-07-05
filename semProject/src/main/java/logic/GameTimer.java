package logic;

import utils.GameMode;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages per-turn and total game time tracking for Arimaa game.
 * Uses virtual threads to update UI without blocking game logic.
 * <p>Supports two modes: CLASSIC (timing disabled) and FAST (timing enable)</p>
 * Tracks individual player elapsed times and triggers callbacks on time updates and timeouts
 */
public class GameTimer {
    private static Logger log = LoggerFactory.getLogger(GameTimer.class);

    public static final long MAX_TURN_DURATION = 90_000;
    public static final long MAX_TOTAL_DURATION = 35*60_000;

    private long goldPlayerTime = 0;
    private long silverPlayerTime = 0;

    private long turnStartTime = 0;
    private boolean running = false;
    private boolean isGoldTurn = false;

    private boolean timedMode = false;

    private Thread turnTimerThread;
    private Thread totalTimerThread;

    /**
     * Listener interface for timer events to update UI components
     */
    public interface TimerListener {
        //Called periodically with the elapsed time of the current turn
        void onTimeUpdate(long milliseconds, boolean isGoldTurn);
        //Called periodically with the accumulated total game time
        void onTotalTimeUpdate(long milliseconds);
        //Called when the current turn exceeds MAX_TURN_DURATION
        void onTurnTimeout(boolean isGoldTurn);
        //Called when the combined game time exceeds MAX_TOTAL_DURATION
        void onTotalTimeout ();
    }

    private TimerListener uiUpdater;

    /**
     * Configure this timer according to game mode
     * @param mode GAME_MODE.CLASSIC disables timing, GAME_MODE.FAST enables it
     */
    public void setMode(GameMode mode) {
        this.timedMode = (mode == GameMode.FAST);
        log.info("Timer mode: {}", timedMode ? "FAST":"CLASSIC");
    }

    /**
     * Register a listener to receive timer callbacks for UI updates
     * @param listener implementation of {@link TimerListener}
     */
    public void setTimerListener(TimerListener listener) {
        this.uiUpdater = listener;
        log.debug("Timer listener set");
    }

    /**
     * Begins total game timing if FAST mode is selected
     * Launches a background virtual thread to report total elapsed time
     */
    public void startGame(){
        if (!timedMode){
            log.debug("Starting game: timing disabled");
            return;
        }
        //Start total timer only once
        if (totalTimerThread == null || !totalTimerThread.isAlive()) {
            log.info("Starting total timer thread");
            startTotalTimerThread();
        }
    }

    /**
     * Starts the timer for the single player's turn.
     * If the previous turn was running, it's ended first
     * @param isGold true, if it is gold player's turn, false for silver
     */
    public void startTurn(boolean isGold) {
        if (!timedMode) return;
        log.info("Starting turn timer for {}", isGold ? "gold" : "silver");
        //If a turn already running, finalize its elapsed time
        if (running){
            endTurn();
        }
        //Interrupt any stale timer thread before starting a new one
        if (turnTimerThread != null && turnTimerThread.isAlive()) {
            turnTimerThread.interrupt();
            turnTimerThread = null;
            log.debug("Previous turn timer thread interrupted");
        }

        isGoldTurn = isGold;
        turnStartTime = System.currentTimeMillis();
        running = true;

       startTurnTimerThread();
    }

    /**
     * Ends the turn of the current player, accumulates elapsed time into their total and stops the turn timer thread.
     */
    public void endTurn() {
        if (!timedMode || !running) {
            log.debug("endTurn called but timer not running or disable");
            return;
        }

        long elapsed = System.currentTimeMillis() - turnStartTime;
        if (isGoldTurn) {
            goldPlayerTime += elapsed;
        } else {
            silverPlayerTime += elapsed;
        }
        log.info("Turn ended for {}: elapsed {} ms", isGoldTurn ? "gold" : "silver", elapsed);

        running = false;
        if (turnTimerThread != null && turnTimerThread.isAlive()){
            turnTimerThread.interrupt();
            turnTimerThread = null;
            log.debug("Turn timer thread interrupted");
        }
    }

    /**
     * Stops all timing activity, ending the current turn and total timers
     */
    public void stopGameTimer() {
        log.info("Stopping game timer");
        endTurn();
        if (totalTimerThread != null && totalTimerThread.isAlive()) {
            totalTimerThread.interrupt();
            totalTimerThread = null;
            log.debug("Total timer thread interrupted");
        }
    }

    /**
     * Geta the elapsed time for the current turn without stopping it
     * @return milliseconds since turn start, or 0 if not running or disable
     */
    public long getCurrentTurnTime() {
        if (!timedMode || !running) return 0;
        return System.currentTimeMillis() - turnStartTime;
    }

    /**
     * Computes the combined elapsed time of both players, including the ongoing turn
     * @return total game time in milliseconds
     */
    public long getTotalTime() {
        long total = goldPlayerTime + silverPlayerTime;
        if (running){
            total += System.currentTimeMillis() - turnStartTime;
        }
        return total;
    }

    /**
     * Starts a virtual thread that periodically updates the current turn elapsed time and triggers a timeout callback when MAX_TURN_DURATION is exceeded
     */
    private void startTurnTimerThread() {
        turnTimerThread = Thread.startVirtualThread(() -> {
            try{
                while (running){
                    long elapsed = System.currentTimeMillis() - turnStartTime;
                    if (elapsed > MAX_TURN_DURATION){
                        log.warn("Turn timer timed out for {}", isGoldTurn ? "gold" : "silver");
                        SwingUtilities.invokeLater(() -> {
                            if (uiUpdater != null) uiUpdater.onTurnTimeout(isGoldTurn);
                        });
                        break;
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (uiUpdater != null) uiUpdater.onTimeUpdate(elapsed, isGoldTurn);
                    });
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Turn timer thread interrupted exception");
            }
        });
        log.debug("Turn timer thread started");
    }

    /**
     * Starts a virtual thread that periodically updates the total game time and triggers a timeout callback when MAX_TOTAL_DURATION is exceeded
     */
    private void startTotalTimerThread() {
        totalTimerThread = Thread.startVirtualThread(() -> {
            try {
                while (true){
                    long total = getTotalTime();
                    if (total > MAX_TOTAL_DURATION){
                        log.warn("Total game timeout");
                        SwingUtilities.invokeLater(() -> {
                            if (uiUpdater != null) uiUpdater.onTotalTimeout();
                        });
                        break;
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (uiUpdater != null) uiUpdater.onTotalTimeUpdate(total);
                    });
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Total timer thread interrupted exception");
            }
        });
        log.debug("Total timer thread started");
    }
}
