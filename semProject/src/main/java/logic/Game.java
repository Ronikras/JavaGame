package logic;

import GUI.GameWindow;
import figures.Figure;
import utils.ActionType;
import utils.FigureType;
import utils.GameMode;

import javax.swing.*;
import java.nio.file.Paths;
import java.util.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.StepResult;

/**
 * Represents the main controller for an Arimaa game.
 * Manages the board, players, current turn, game state, move execution, timing enforcement, move history tracking, UI launching
 */
public class Game {
    private static Logger log = LoggerFactory.getLogger(Game.class);

    private final GameMode mode;
    private Board board;
    private final Player goldPlayer;
    private final Player silverPlayer;
    private Player currentPlayer;
    private final GameTimer timer;
    private final List<String> moveHistory;
    private final List<Integer> stepsHistory;
    private final Deque<GameState> undoStack = new ArrayDeque<>();
    int turnsSteps;


    public static final int MAX_TURNS_STEPS = 4;

    private static final Position[] TRAPS = {
            new Position(2, 2),
            new Position(2, 5),
            new Position(5, 2),
            new Position(5, 5)
    };

    /**
     * Initializes a new Arimaa game with two players, board in the given mode
     * <p>Starts timing if FAST mode</p>
     *
     * @param mode CLASSIC (without timer) or FAST (with timer)
     */
    public Game(GameMode mode) {
        log.info("Initializing a new game with mode {}", mode);
        this.mode = mode;
        this.board = new Board();
        this.goldPlayer = new Player(true);
        this.silverPlayer = new Player(false);
        this.currentPlayer = goldPlayer;

        //Fill in the lists of players' pieces from the initial board setup
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Figure f = board.getFigureAt(row, col);
                if (f != null) {
                    if (f.isGold()) goldPlayer.addFigure(f);
                    else silverPlayer.addFigure(f);
                }
            }
        }

        this.timer = new GameTimer();
        this.timer.setMode(mode);

        this.moveHistory = new ArrayList<>();
        this.stepsHistory = new ArrayList<>();
        this.turnsSteps = 0;
    }

    /**
     * Initializes a new game with a custom board (for replay or tests).
     *
     * @param mode         Game mode
     * @param initialBoard Board to use
     */
    public Game(GameMode mode, Board initialBoard) {
        log.info("Initializing a new game with custom board and mode {}", mode);
        this.mode = mode;
        this.board = initialBoard;
        this.goldPlayer = new Player(true);
        this.silverPlayer = new Player(false);
        this.currentPlayer = goldPlayer;

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Figure f = board.getFigureAt(row, col);
                if (f != null) {
                    if (f.isGold()) goldPlayer.addFigure(f);
                    else silverPlayer.addFigure(f);
                }
            }
        }

        this.timer = new GameTimer();
        this.timer.setMode(mode);

        this.moveHistory = new ArrayList<>();
        this.stepsHistory = new ArrayList<>();
        this.turnsSteps = 0;
    }

    /**
     * Starts the game loop.
     * Launches the Swing UI for this game session.
     */
    public void startGame() {
        log.info("Launching game window");
        SwingUtilities.invokeLater((GameWindow::new));
    }


    /**
     * Performs a game step (move or attempt push/pull) from one position to another.
     * Handles validation, updates move history, and checks for captures.
     *
     * @param from Source position
     * @param to   Destination position
     * @return StepResult describing the outcome or options for push/pull
     * @throws IllegalArgumentException if move is illegal or out of steps
     */
    public StepResult step(Position from, Position to) {
        saveState();

        log.debug("Player {} attempts to move from {} to {}", currentPlayer.isGold() ? "gold" : "silver", from, to);

        if (turnsSteps >= MAX_TURNS_STEPS) {
            throw new IllegalArgumentException("No steps left: please end your turn first");
        }

        if (!moveHistory.isEmpty()) {
            String lastNote = moveHistory.get(moveHistory.size() - 1);
            if (lastNote.startsWith("1g") || lastNote.startsWith("1s")) {

            } else {
                try {
                    Notation.Move prev = Notation.parse(lastNote);
                    if (prev.getFrom() != null && prev.getTo() != null
                            && prev.getFrom().equals(to) && prev.getTo().equals(from)) {
                        Figure f = board.getFigureAt(from.getRow(), from.getCol());
                        board.setFigureAt(to.getRow(), to.getCol(), f);
                        board.setFigureAt(from.getRow(), from.getCol(), null);
                        moveHistory.remove(moveHistory.size() - 1);
                        if (lastNote.contains(">") || lastNote.contains("<")) {
                            turnsSteps = Math.max(0, turnsSteps - 2);
                        } else {
                            turnsSteps = Math.max(0, turnsSteps - 1);
                        }
                        log.info("Inverse move {} undone automatically", lastNote);
                        return StepResult.simple(Collections.emptyList());
                    }
                } catch (IllegalArgumentException ex) {

                }
            }

        }

        enforceTimers();
        //Validate and perform move
        Figure figure = board.getFigureAt(from.getRow(), from.getCol());
        if (figure == null) {
            log.error("No figure at row {} and col {}", from.getRow(), from.getCol());
            throw new IllegalArgumentException("No figure at  " + from);
        }
        if (figure.isGold() != currentPlayer.isGold()) {
            log.warn("Figure at {} does not belong to current player", from);
            throw new IllegalArgumentException("The figure is not yours");
        }
        if (!validateStep(from, to, figure)) {
            log.warn("Illegal step from {} to {} by {}", from, to, figure);
            throw new IllegalArgumentException("Illegal step from " + from + " to " + to);
        }

        Figure target = board.getFigureAt(to.getRow(), to.getCol());
        if (target == null) {
            String notation = Notation.formatSimple(figure, from, to);
            board.setFigureAt(to.getRow(), to.getCol(), figure);
            board.setFigureAt(from.getRow(), from.getCol(), null);
            turnsSteps++;
            recordStep(notation, 1);
            log.info("Player {} made move: {}", currentPlayer.isGold() ? "Gold" : "Silver", notation);

            List<String> allNotes = new ArrayList<>();
            allNotes.add(notation);
            allNotes.addAll(handleTraps());

            return StepResult.simple(allNotes);
        }
        List<Position> pushDest = getPushDestinations(from, to);
        List<Position> pullDest = getPullDestinations(from, to);

        if (turnsSteps + 2 > MAX_TURNS_STEPS) {
            throw new IllegalArgumentException("Not enough steps left for push/pull");
        }

        if (!pushDest.isEmpty() && !pullDest.isEmpty()) {
            List<Position> both = new ArrayList<>();
            both.addAll(pushDest);
            both.addAll(pullDest);
            return StepResult.pushOrPull(ActionType.BOTH, both);
        }

        if (!pushDest.isEmpty()) {
            log.debug("Offering PUSH options: {}", pushDest);
            return StepResult.pushOrPull(ActionType.PUSH, pushDest);
        }
        if (!pullDest.isEmpty()) {
            log.debug("Offering PULL options: {}", pullDest);
            return StepResult.pushOrPull(ActionType.PULL, pullDest);
        }
        throw new IllegalArgumentException("Cannot push or pull target at " + to);
    }

    /**
     * Resolves push or pull move using source, victim, and destination positions.
     * Handles validation, updates board state, and move history.
     *
     * @param from        Mover's position
     * @param to          Victim's position
     * @param destination Where the victim or mover will be moved
     * @return StepResult after push or pull
     * @throws IllegalArgumentException if invalid
     */
    public StepResult resolveStep(Position from, Position to, Position destination) {
        if (turnsSteps + 2 > MAX_TURNS_STEPS) {
            throw new IllegalArgumentException("Not enough steps left for push/pull");
        }

        saveState();

        enforceTimers();
        Figure mover = board.getFigureAt(from.getRow(), from.getCol());
        Figure victim = board.getFigureAt(to.getRow(), to.getCol());
        if (mover == null || victim == null)
            throw new IllegalArgumentException("Invalid push/pull source or victim");

        List<Position> pushDest = getPushDestinations(from, to);
        List<Position> pullDest = getPullDestinations(from, to);
        String notation;

        if (pushDest.contains(destination)) {
            notation = Notation.formatPush(mover, from, to, destination);
            performPush(from, to, destination);
        } else if (pullDest.contains(destination)) {
            notation = Notation.formatPull(mover, from, to, destination);
            performPull(from, to, destination);
        } else {
            throw new IllegalArgumentException("Destination " + destination + " is not valid for push/pull");
        }

        turnsSteps += 2;
        recordStep(notation, 2);
        log.info("Player {} made : {}", currentPlayer.isGold() ? "Gold" : "Silver", notation);

        List<String> captures = handleTraps();
        for (String cap : captures) {
            log.info("Auto-capture: {}", cap);
        }

        List<String> allNotes = new ArrayList<>();

        return StepResult.simple(allNotes);
    }

    /**
     * Throws an exception if timer constraints are exceeded (in FAST mode).
     * Used internally before step and resolveStep.
     */
    private void enforceTimers() {
        if (mode == GameMode.FAST) {
            if (timer.getCurrentTurnTime() > GameTimer.MAX_TURN_DURATION)
                throw new GameOverException("Turn time exceeded. You lost :(");
            if (timer.getTotalTime() > GameTimer.MAX_TOTAL_DURATION)
                throw new GameOverException("Total time exceeded. You lost :(");
        }
    }

    /**
     * Adds a token (move or capture) to move history.
     *
     * @param tok move token
     */
    public void addToHistory(String tok) {
        moveHistory.add(tok);
    }

    /**
     * Performs a push operation: victim piece is moved to destination, mover takes victim's position.
     */
    private void performPush(Position from, Position victim, Position destination) {
        Figure m = board.getFigureAt(from.getRow(), from.getCol());
        Figure v = board.getFigureAt(victim.getRow(), victim.getCol());
        board.setFigureAt(destination.getRow(), destination.getCol(), v);
        board.setFigureAt(victim.getRow(), victim.getCol(), m);
        board.setFigureAt(from.getRow(), from.getCol(), null);
    }

    /**
     * Performs a pull operation: mover moves to destination, victim moves into mover's previous position.
     */
    private void performPull(Position from, Position victim, Position destination) {
        Figure m = board.getFigureAt(from.getRow(), from.getCol());
        Figure v = board.getFigureAt(victim.getRow(), victim.getCol());
        board.setFigureAt(destination.getRow(), destination.getCol(), m);
        board.setFigureAt(from.getRow(), from.getCol(), v);
        board.setFigureAt(victim.getRow(), victim.getCol(), null);
    }

    /**
     * Adds a step notation to move history.
     *
     * @param notation step in Arimaa notation
     */
    private void recordStep(String notation, int stepsTaken) {
        moveHistory.add(notation);
        stepsHistory.add(stepsTaken);
    }

    /**
     * Handles trap squares: removes unprotected figures, adds capture notations.
     *
     * @return list of capture notations for this step
     */
    private List<String> handleTraps() {
        List<String> captures = new ArrayList<>();
        for (Position trap : TRAPS) {
            Figure f = board.getFigureAt(trap.getRow(), trap.getCol());
            if (f != null && board.countFriends(trap.getRow(), trap.getCol()) == 0) {
                board.setFigureAt(trap.getRow(), trap.getCol(), null);
                String cap = Notation.formatCapture(f, trap);
                moveHistory.add(cap);
                captures.add(cap);
            }
        }
        return captures;
    }

    /**
     * Validates whether a single move step follows Arimaa rules:
     * <ul>
     * <li>Destination must be orthogonally adjacent.</li>
     * <li>Frozen pieces cannot move.</li>
     * <li>Rabbits cannot move backward.</li>
     * <li>Push/pull only allowed against weaker enemy pieces with empty space.</li>
     * </ul>
     *
     * @param from   source {@link Position}
     * @param to     target {@link Position}
     * @param figure moving {@link Figure}
     * @return true if the step is legal, false otherwise
     */
    private boolean validateStep(Position from, Position to, Figure figure) {
        log.trace("Validate step from {} to {} for {}", from, to, figure);
        //Cannot move if frozen
        if (board.isFrozen(from.getRow(), from.getCol())) {
            log.trace("Step invalid: piece at {} is frozen", from);
            return false;
        }

        //Must move exactly one square orthogonally
        int dr = to.getRow() - from.getRow();
        int dc = to.getCol() - from.getCol();
        if (Math.abs(dr) + Math.abs(dc) != 1) return false;

        //Inspect target square
        Figure target = board.getFigureAt(to.getRow(), to.getCol());
        if (target == null) {
            //Empty destination: rabbits cannot move backward
            if (figure.getType() == FigureType.RABBIT) {
                if ((figure.isGold() && dr > 0) || (!figure.isGold() && dr < 0)) return false;
            }
            return true;
        }
        //Cannot capture own piece
        if (target.isGold() == figure.isGold()) return false;

        //Compare strengths including adjacent support
        int strengthA = figure.getType().getStrength() + board.countFriends(from.getRow(), from.getCol());
        int strengthB = target.getType().getStrength() + board.countFriends(to.getRow(), to.getCol());
        if (strengthA <= strengthB) return false;

        if (!getPushDestinations(from, to).isEmpty()) return true;
        if (!getPullDestinations(from, to).isEmpty()) return true;
        return false;
    }

    /**
     * Returns legal destination squares for a push action.
     */
    private List<Position> getPushDestinations(Position from, Position to) {
        List<Position> res = new ArrayList<>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            Position p = new Position(to.getRow() + d[0], to.getCol() + d[1]);
            if (p.equals(from)) continue;
            if (isInBounds(p) && board.getFigureAt(p.getRow(), p.getCol()) == null) {
                res.add(p);
            }
        }
        return res;
    }

    /**
     * Returns legal destination squares for a pull action.
     */
    private List<Position> getPullDestinations(Position from, Position to) {
        List<Position> res = new ArrayList<>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            Position p = new Position(from.getRow() + d[0], from.getCol() + d[1]);
            if (p.equals(to)) continue;
            if (isInBounds(p) && board.getFigureAt(p.getRow(), p.getCol()) == null) {
                res.add(p);
            }
        }
        return res;
    }


    /**
     * Checks if a move from one position to another is legal for the ccurrent player
     *
     * @param from source {@link Position}
     * @param to   destination {@link Position}
     * @return true if a piece exists at 'from', belongs to the current player and passes core step validation
     */
    public boolean stepIsLegal(Position from, Position to) {
        Figure figure = board.getFigureAt(from.getRow(), from.getCol());
        if (figure == null || figure.isGold() != currentPlayer.isGold()) return false;
        return validateStep(from, to, figure);
    }

    /**
     * Checks if a position is within board bounds
     */
    private boolean isInBounds(Position pos) {
        return pos.getRow() >= 0 && pos.getRow() < Board.SIZE && pos.getCol() >= 0 && pos.getCol() < Board.SIZE;
    }

    /**
     * Ends the current turn prematurely and switches player
     */
    public List<String> endTurnEarly() {
        log.info("Ending turn early for {}", currentPlayer.isGold() ? "gold" : "silver");
        int unused = MAX_TURNS_STEPS - turnsSteps;
        List<String> fillers = new ArrayList<>();
        for (int i = 0; i < unused; i++) {
            moveHistory.add("-");
            fillers.add("-");
        }
        if (timer != null) {
            timer.endTurn();
        }
        switchPlayer();
        turnsSteps = 0;
        return fillers;
    }

    /**
     * Increments step counter and switches player if turn steps reach max.
     */
    public void skipStep() {
        turnsSteps++;
        if (turnsSteps >= MAX_TURNS_STEPS) {
            switchPlayer();
            turnsSteps = 0;
        }
    }

    /**
     * Checks if the game has ended.
     *
     * @return true if the game is over by rabbit goal or elimination
     */
    public boolean isGameOver() {
        log.info("Checking game over condition");
        for (int col = 0; col < Board.SIZE; col++) {
            Figure top = board.getFigureAt(0, col);
            if (top != null && top.getType() == FigureType.RABBIT && top.isGold()) {
                log.info("Game over: gold rabbit reached goal");
                return true;
            }
            Figure bottom = board.getFigureAt(Board.SIZE - 1, col);
            if (bottom != null && bottom.getType() == FigureType.RABBIT && !bottom.isGold()) {
                log.info("Game over: silver rabbit reached goal");
                return true;
            }
        }
        if (!goldPlayer.hasRabbit(board) || !silverPlayer.hasRabbit(board)) {
            log.info("Game over: {} side have no rabbits", goldPlayer.hasRabbit(board) ? "silver" : "gold");
            return true;
        }
        return false;
    }


    /**
     * Saves move history to a text file
     *
     * @param filePath path to the output file
     * @throws IOException if writing fails
     */
    public void saveHistoryToFile(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Setup
            writer.write(moveHistory.get(0));
            writer.write("\n"); // 1g
            writer.write(moveHistory.get(1));
            writer.write("\n"); // 1s

            int idx = 2;
            int turn = 2;
            boolean goldTurn = true;

            while (idx < moveHistory.size()) {
                writer.write(turn + (goldTurn ? "g" : "s"));
                int steps = 0;
                List<String> captures = new ArrayList<>();
                while (idx < moveHistory.size() && steps < 4) {
                    String token = moveHistory.get(idx++);
                    if (token.endsWith("x")) {
                        captures.add(token);
                    } else {
                        writer.write(" " + token);
                        steps++;
                    }
                }
                while (steps < 4) {
                    writer.write(" -");
                    steps++;
                }
                for (String cap : captures) {
                    writer.write(" " + cap);
                }
                writer.write("\n");
                if (!goldTurn) turn++;
                goldTurn = !goldTurn;
            }
        }
    }


    /**
     * Loads game from aa notation file by replaying valid moves
     */
    public static Game loadFromFile(String filePath, GameMode mode) throws IOException {
        List<String> lines = java.nio.file.Files.readAllLines(Paths.get(filePath));
        Game game = new Game(mode);

        int lineNum = 0;
        for (String line : lines) {
            lineNum++;
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("1g") || line.startsWith("1s")) {
                String[] parts = line.split("\\s+");
                boolean isGold = line.startsWith("1g");
                for (int i = 1; i < parts.length; i++) {  // с 1, пропуская 1g/1s
                    String fig = parts[i];
                    char pieceChar = fig.charAt(0);
                    FigureType type = FigureType.fromLetter("" + Character.toUpperCase(pieceChar));
                    boolean goldSide = Character.isUpperCase(pieceChar);
                    int col = fig.charAt(1) - 'a';
                    int row = Board.SIZE - (fig.charAt(2) - '1') - 1;
                    game.getBoard().setFigureAt(row, col, new Figure(type, goldSide));
                }
                continue;
            }
            if (line.matches("^\\d+[gs].*")) {
                String[] parts = line.split("\\s+");
                boolean isGoldLine = parts[0].endsWith("g");

                if (game.currentPlayer.isGold() != isGoldLine) {
                    game.switchPlayer();
                }
                game.turnsSteps = 0;

                for (int i = 1; i < parts.length; i++) {
                    String token = parts[i];
                    if (token.equals("-")) {
                        continue;
                    }
                    if (token.endsWith("x")) {
                        log.debug("[loadFromFile] Trap/capture token: {} (ignored for replay)", token);
                        continue;
                    }
                    try {
                        Notation.Move move = Notation.parse(token);
                        if (token.contains(">") || token.contains("<")) {
                            // Push or Pull
                            game.resolveStep(move.getFrom(), move.getTo(), move.getDestination());
                        } else {
                            // Simple move
                            game.step(move.getFrom(), move.getTo());
                        }
                    } catch (Exception ex) {

                    }

                }
                log.info("[loadFromFile] End of {}: currentPlayer={}, turnsSteps={}",
                        parts[0], game.getCurrentPlayer().isGold() ? "Gold" : "Silver", game.turnsSteps);
            }
        }
        log.info("[loadFromFile] Game loaded. Final player={}, turnsSteps={}",
                game.getCurrentPlayer().isGold() ? "Gold" : "Silver", game.turnsSteps);
        return game;
    }


    /**
     * @return a new List containing all move tokens from moveHistory
     */
    public List<String> getRawHistory() {
        return new ArrayList<>(moveHistory);
    }

    /**
     * Adds setup moves (piece placement before game) and updates board.
     *
     * @param setupMoves List of figure codes (e.g., ["Ra1", "Hb2", ...])
     * @param isGold     true if gold setup, false if silver
     */
    public void addSetupMove(List<String> setupMoves, boolean isGold) {
        String prefix = isGold ? "1g" : "1s";
        String setupStr = prefix + " " + String.join(" ", setupMoves);
        moveHistory.add(setupStr);
        for (String fig : setupMoves) {
            char pieceChar = fig.charAt(0);
            FigureType type = FigureType.fromLetter("" + Character.toUpperCase(pieceChar));
            boolean goldSide = Character.isUpperCase(pieceChar);
            int col = fig.charAt(1) - 'a';
            int row = Board.SIZE - (fig.charAt(2) - '1') - 1;
            board.setFigureAt(row, col, new Figure(type, goldSide));
        }
    }

    private void saveState() {
        Figure[][] currentMatrix = board.getBoardMatrix();
        boolean goldTurn = currentPlayer.isGold();
        int steps = turnsSteps;

        undoStack.push(new GameState(currentMatrix, goldTurn, steps));
    }


    /**
     * Undoes the most recent move or action, restoring previous game state
     *
     * @return true if undo was successful, false if no state to revert to
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }

        GameState prev = undoStack.pop();

        Figure[][] mat = prev.boardCopy;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Figure f = mat[r][c];
                if (f != null) {
                    board.setFigureAt(r, c, new Figure(f.getType(), f.isGold()));
                } else {
                    board.setFigureAt(r, c, null);
                }
            }
        }

        if (prev.isGoldTurn != currentPlayer.isGold()) {
            switchPlayer();
        }
        this.turnsSteps = prev.turnsSteps;

        if (!moveHistory.isEmpty()) {
            moveHistory.remove(moveHistory.size() - 1);
        }

        return true;
    }


    /**
     * @return the logical board object
     */
    public Board getBoard() {
        return board;
    }

    /**
     * @return current game mode
     */
    public GameMode getMode() {
        return mode;
    }

    /**
     * @return current timer object
     */
    public GameTimer getTimer() {
        return timer;
    }

    /**
     * @return remaining steps in the current turn
     */
    public int getTurnsSteps() {
        return MAX_TURNS_STEPS - turnsSteps;
    }

    /**
     * @return player whose turn it is
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Switches the turn to the other player, reset step counter and manages timing
     */
    public void switchPlayer() {
        log.info("Switching turn from {} to {}", currentPlayer.isGold() ? "gold" : "silver", !currentPlayer.isGold() ? "gold" : "silver");
        if (mode == GameMode.FAST) {
            timer.endTurn();
        }

        currentPlayer = (currentPlayer == goldPlayer) ? silverPlayer : goldPlayer;
        turnsSteps = 0;

        if (mode == GameMode.FAST) {
            timer.startTurn(currentPlayer.isGold());
        }
    }

    /**
     * Internal class to store game state for undo functionality
     */
    private static class GameState {
        final Figure[][] boardCopy;
        final boolean isGoldTurn;
        final int turnsSteps;

        GameState(Figure[][] boardMatrix, boolean isGoldTurn, int turnsSteps) {
            this.boardCopy = new Figure[Board.SIZE][Board.SIZE];
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    Figure f = boardMatrix[r][c];
                    if (f != null) {
                        this.boardCopy[r][c] = new Figure(f.getType(), f.isGold());
                    } else {
                        this.boardCopy[r][c] = null;
                    }
                }
            }
            this.isGoldTurn = isGoldTurn;
            this.turnsSteps = turnsSteps;
        }
    }

}
