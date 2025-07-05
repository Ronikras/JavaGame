package GUI;

import logic.*;
import figures.Figure;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ActionType;
import utils.FigureType;
import utils.GameMode;
import utils.StepResult;

import static logic.Game.MAX_TURNS_STEPS;

/**
 * JPanel responsible for drawing the game board and figures.
 * JPanel responsible for handling drag-and-drop moves, undo and turn controls
 * <p>Displays optional timing labels in FAST mode and provides buttons for saving, ending turn and undo</p>
 */
public class BoardPanel extends JPanel implements MouseListener, MouseMotionListener {
    private static final int TILE_SIZE = 80;
    private static final int BOARD_SIZE = Board.SIZE;
    static final int LABEL_MARGIN = 30;
    private static final Logger log = LoggerFactory.getLogger(BoardPanel.class);
    private Board startingBoard;

    private Game game;
    private Board board;
    private boolean modeFast;

    //Drag-and-drop state
    private Position dragFrom = null;
    private Figure draggedFigure = null;
    private BufferedImage dragImage = null;
    private Point dragPosition = null;
    private Point dragOffset = null;
    private Position selected = null;

    //UI controls state
    private JButton btnSave, btnEndTurn, btnPrevMove;
    private JLabel lblPlayer, lblSteps, lblTimer, lblTotalTime;

    private Stack<String> undoStack = new Stack<>();
    private boolean awaitingAction = false;
    private Position actionFrom, actionTo;
    private ActionType actionType;
    private List<Position> actionOptions;

    private final boolean setupMode;
    private final Figure[][] customSetup;

    private final boolean againstAI;
    private final boolean humanPlaysGold;
    private Timer aiTimer;


    /**
     * Constructs a playing BoardPanel, initialized with a Game.
     * <p>
     * Adds game controls, drag-and-drop, and (if enabled) timers.
     *
     * @param initialGame    the Game to use for moves and board state
     * @param againstAI      true if this is a human vs. AI match
     * @param humanPlaysGold true if the human is playing gold side (otherwise plays silver)
     */
    public BoardPanel(Game initialGame, boolean againstAI, boolean humanPlaysGold) {
        this.game = initialGame;
        this.board = initialGame.getBoard();
        this.modeFast = initialGame.getMode() == GameMode.FAST;
        this.setupMode = false;
        this.customSetup = null;
        this.againstAI = againstAI;
        this.humanPlaysGold = humanPlaysGold;
        for (String tok : game.getRawHistory()) undoStack.push(tok);
        initUI();
    }

    /**
     * Constructs the board panel for custom setup mode, displaying an empty board for piece placement
     *
     * @param board       the Board to display
     * @param customSetup a 2D array to populate with setup figures
     */
    public BoardPanel(Board board, Figure[][] customSetup) {
        this.board = board;
        this.setupMode = true;
        this.customSetup = customSetup;
        this.game = null;
        this.modeFast = false;
        this.againstAI = false;
        this.humanPlaysGold = true;

        initUI();
    }


    /**
     * Initializes the UI layout, adding game controls and listeners if npt in setup mode
     */
    public void initUI() {
        setLayout(new BorderLayout());
        if (!setupMode) {
            installGameControls();
            installGameListeners();
            if (modeFast) installTimerListeners();
            updateStatus();
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        setPreferredSize(new Dimension(
                LABEL_MARGIN * 2 + BOARD_SIZE * TILE_SIZE + 130,
                LABEL_MARGIN * 2 + BOARD_SIZE * TILE_SIZE
        ));
    }

    /**
     * Attaches action listeners for Save, Undo (previous move), and End Turn buttons
     */
    private void installGameListeners() {
        btnSave.addActionListener(e -> {
            log.info("Save button clicked");
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    game.saveHistoryToFile(fc.getSelectedFile().getPath());
                    log.info("Game history saved to {}", fc.getSelectedFile().getPath());
                    int cont = JOptionPane.showConfirmDialog(
                            BoardPanel.this, "Game saved. Do you want to continue playing?",
                            "Continue?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
                    );
                    if (cont == JOptionPane.NO_OPTION) {
                        SwingUtilities.getWindowAncestor(BoardPanel.this).dispose();
                    }
                } catch (IOException ex) {
                    log.error("Save failed: {}", ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
                }
            }
        });

        btnPrevMove.addActionListener(e -> {
            log.info("Previous move button clicked");

            selected = null;
            dragFrom = null;
            dragImage = null;
            dragPosition = null;
            dragOffset = null;

            boolean ok = game.undo();
            if (!ok) {
                btnPrevMove.setEnabled(false);
                return;
            }

            updateStatus();
            revalidate();
            repaint();

            btnPrevMove.setEnabled(!game.getRawHistory().isEmpty());

            log.info("Undo complete: history size={}, current player={}", game.getRawHistory().size(), game.getCurrentPlayer().isGold() ? "Gold" : "Silver");
        });

        btnEndTurn.addActionListener(e -> {
            log.info("End turn button clicked");
            List<String> fillers = BoardPanel.this.game.endTurnEarly();
            fillers.forEach(undoStack::push);

            selected = null;
            dragFrom = null;
            dragImage = null;
            dragPosition = null;
            dragOffset = null;

            updateStatus();
            repaint();

            if (againstAI
                    && !game.isGameOver()
                    && game.getCurrentPlayer().isGold() != humanPlaysGold) {
                startAITurn();
            }
        });
    }

    /**
     * Creates and lays out control buttons and status labels on the right side
     */
    private void installGameControls() {
        JPanel control = new JPanel();
        control.setLayout(new BoxLayout(control, BoxLayout.Y_AXIS));
        control.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        btnSave = new JButton("Save game");
        btnPrevMove = new JButton("Undo");
        btnPrevMove.setEnabled(false);
        btnEndTurn = new JButton("End turn");
        lblPlayer = new JLabel("Player: ");
        lblSteps = new JLabel("Steps left: ");
        control.add(lblPlayer);
        control.add(Box.createVerticalStrut(5));
        control.add(lblSteps);
        control.add(Box.createVerticalStrut(5));
        control.add(btnPrevMove);
        control.add(Box.createVerticalStrut(10));
        control.add(btnEndTurn);
        control.add(Box.createVerticalStrut(20));
        control.add(btnSave);
        control.add(Box.createVerticalStrut(10));

        //If timed mode FAST set up labels and timer callbacks
        if (modeFast) {
            lblTimer = new JLabel("Turn: 0s");
            lblTotalTime = new JLabel("Total: 0s");
            control.add(Box.createVerticalStrut(20));
            control.add(lblTimer);
            control.add(Box.createVerticalStrut(5));
            control.add(lblTotalTime);
        }
        add(control, BorderLayout.EAST);
    }

    /**
     * Configures listeners on the game timer to update UI labels and handle timeouts
     */
    private void installTimerListeners() {
        GameTimer timer = game.getTimer();
        timer.setTimerListener(new GameTimer.TimerListener() {
            @Override
            public void onTimeUpdate(long milliseconds, boolean isGold) {
                SwingUtilities.invokeLater(() -> {
                    lblTimer.setText("Turn time: " + (milliseconds / 1000) + "s");
                });
            }

            @Override
            public void onTotalTimeUpdate(long milliseconds) {
                SwingUtilities.invokeLater(() -> {
                    lblTotalTime.setText("Total time: " + (milliseconds / 1000) + "s");
                });
            }

            @Override
            public void onTurnTimeout(boolean isGoldTurn) {
                SwingUtilities.invokeLater(() -> {
                    log.warn("Turn timeout for {} player", isGoldTurn ? "gold" : "silver");
                    JOptionPane.showMessageDialog(BoardPanel.this, (isGoldTurn ? "Gold" : "Silver") + " turn timed out!", "Timeout", JOptionPane.WARNING_MESSAGE);
                    timer.stopGameTimer();
                    removeMouseListener(BoardPanel.this);
                    removeMouseMotionListener(BoardPanel.this);
                    btnEndTurn.setEnabled(false);
                    btnPrevMove.setEnabled(false);
                    btnSave.setEnabled(false);

                    String winner = isGoldTurn ? "Silver" : "Gold";
                    JOptionPane.showMessageDialog(BoardPanel.this, winner + " wins by timeout!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                });
            }

            @Override
            public void onTotalTimeout() {
                SwingUtilities.invokeLater(() -> {
                    log.warn("Total intialGame timeout");
                    JOptionPane.showMessageDialog(BoardPanel.this,
                            "Total intialGame time exceeded! Game over.",
                            "Game Over", JOptionPane.WARNING_MESSAGE);
                    timer.stopGameTimer();
                    removeMouseListener(BoardPanel.this);
                    removeMouseMotionListener(BoardPanel.this);
                    btnEndTurn.setEnabled(false);
                    btnPrevMove.setEnabled(false);
                    btnSave.setEnabled(false);
                });
            }
        });
    }

    /**
     * Update labels reflecting current player and remaining steps
     */
    public void updateStatus() {
        boolean gold = game.getCurrentPlayer().isGold();
        lblPlayer.setText("Player: " + (gold ? "Gold" : "Silver"));
        lblSteps.setText("Steps left: " + game.getTurnsSteps());
        log.debug("Status updated: player {}, steps {}", game.getCurrentPlayer(), game.getTurnsSteps());
        Color borderColor = gold ? new Color(255, 200, 50) : new Color(200, 200, 200);
        setBorder(BorderFactory.createLineBorder(borderColor, 5));
        lblPlayer.setForeground(borderColor.darker());
    }

    /**
     * Triggers an AI turn (for human-vs-AI mode).
     * Selects a random legal move or moves, plays them, and ends turn.
     */
    private void startAITurn() {
        aiTimer = new Timer(500, null);
        aiTimer.addActionListener(e -> {
            aiTimer.stop();

            if (game.isGameOver() || game.getCurrentPlayer().isGold() == humanPlaysGold) {
                return;
            }
            int movesToDo = new java.util.Random().nextInt(MAX_TURNS_STEPS) + 1;
            for (int i = 0; i < movesToDo && !game.isGameOver(); i++) {
                boolean aiIsGold = game.getCurrentPlayer().isGold();
                List<MoveOption> opts = AILogic.collectAllMoves(game, aiIsGold);
                if (opts.isEmpty()) break;

                MoveOption pick = opts.get(new java.util.Random().nextInt(opts.size()));
                StepResult res = game.step(pick.from, pick.to);

                if (res.type != ActionType.SIMPLE) {
                    List<Position> choices = res.options;
                    Position dest = choices.get(new java.util.Random().nextInt(choices.size()));
                    res = game.resolveStep(pick.from, pick.to, dest);
                }

                res.notation.forEach(undoStack::push);
            }
            List<String> fillers = game.endTurnEarly();
            fillers.forEach(undoStack::push);
            updateStatus();
            repaint();
        });
        aiTimer.setRepeats(false);
        aiTimer.setInitialDelay(500);
        aiTimer.start();
    }

    /**
     * Custom painting: draws the board, coordinates, figures, highlights, and dragged piece
     *
     * @param g the  Graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBoard(g);
        drawCoordinates(g);
        drawFigures(g);

        if (!setupMode) {
            if (awaitingAction) {
                highlightActionOptions((Graphics2D) g);
            }
            highlightMoves((Graphics2D) g);
            drawDragged((Graphics2D) g);
        }
    }

    /**
     * Highlights cells corresponding to current push/pull/AI action options.
     */
    private void highlightActionOptions(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 255, 120));
        for (Position p : actionOptions) {
            int x = LABEL_MARGIN + p.getCol() * TILE_SIZE;
            int y = LABEL_MARGIN + p.getRow() * TILE_SIZE;
            g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        }
    }


    /**
     * Draws the checkerboard and trap cells background.
     */
    private void drawBoard(Graphics g) {
        Color base = new Color(240, 217, 181);
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                int x = LABEL_MARGIN + col * TILE_SIZE;
                int y = LABEL_MARGIN + row * TILE_SIZE;
                if (isTrapCell(row, col)) {
                    g.setColor(new Color(181, 136, 99));
                } else {
                    g.setColor(base);
                }
                g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g.setColor(new Color(140, 90, 46));
                g.drawRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    /**
     * Checks if the given cell is one of the four trap squares.
     *
     * @param row the row index
     * @param col the column index
     * @return true if trap cell
     */
    private boolean isTrapCell(int row, int col) {
        return (row == 2 && col == 2) || (row == 2 && col == 5) || (row == 5 && col == 2) || (row == 5 && col == 5);
    }

    /**
     * Draws coordinate labels A–H across the top/bottom and 1–8 along the sides
     */
    private void drawCoordinates(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // Horizontal (A-H)
        for (int col = 0; col < BOARD_SIZE; col++) {
            String letter = String.valueOf((char) ('A' + col));
            int x = LABEL_MARGIN + col * TILE_SIZE + TILE_SIZE / 2 - 5;
            g.drawString(letter, x, 20); // top
            g.drawString(letter, x, LABEL_MARGIN + BOARD_SIZE * TILE_SIZE + 20); // bottom
        }

        // Vertical (1-8)
        for (int row = 0; row < BOARD_SIZE; row++) {
            String number = String.valueOf(8 - row);
            int y = LABEL_MARGIN + row * TILE_SIZE + TILE_SIZE / 2 + 5;
            g.drawString(number, 10, y); // left
            g.drawString(number, LABEL_MARGIN + BOARD_SIZE * TILE_SIZE + 10, y); // right
        }
    }

    /**
     * Renders all figures except the one currently being dragged.
     */
    private void drawFigures(Graphics g) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (!setupMode && dragFrom != null && dragFrom.getCol() == col && dragFrom.getRow() == row) {
                    continue;
                }
                Figure figure = board.getFigureAt(row, col);
                if (figure != null) {
                    BufferedImage img = ImageLoader.loadImage(
                            figure.getImagePath(), TILE_SIZE, TILE_SIZE
                    );
                    g.drawImage(img,
                            LABEL_MARGIN + col * TILE_SIZE,
                            LABEL_MARGIN + row * TILE_SIZE,
                            TILE_SIZE, TILE_SIZE,
                            null);
                }
            }
        }
    }

    /**
     * Highlights legal moves for the currently selected piece.
     *
     * @param g2d the Graphics2D context
     */
    private void highlightMoves(Graphics g2d) {
        if (selected == null) return;
        int sr = selected.getRow(), sc = selected.getCol();
        int sx = LABEL_MARGIN + sc * TILE_SIZE, sy = LABEL_MARGIN + sr * TILE_SIZE;
        g2d.setColor(new Color(0, 200, 0, 120));
        g2d.fillRect(sx, sy, TILE_SIZE, TILE_SIZE);

        for (Position nb : List.of(
                new Position(sr + 1, sc), new Position(sr - 1, sc),
                new Position(sr, sc + 1), new Position(sr, sc - 1)
        )) {
            int row = nb.getRow(), col = nb.getCol();
            if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) continue;
            if (!game.stepIsLegal(selected, nb)) continue;
            int nx = LABEL_MARGIN + col * TILE_SIZE, ny = LABEL_MARGIN + row * TILE_SIZE;
            boolean trap = (row == 2 || row == 5) && (col == 2 || col == 5);
            if (trap && board.countFriends(row, col) == 0) g2d.setColor(new Color(255, 0, 0, 120));
            else g2d.setColor(new Color(255, 255, 0, 120));
            g2d.fillRect(nx, ny, TILE_SIZE, TILE_SIZE);
        }
    }

    /**
     * Draws the dragged figure image at the current cursor position.
     *
     * @param g2d the Graphics2D context
     */
    private void drawDragged(Graphics g2d) {
        if (dragImage == null || dragPosition == null || dragOffset == null) return;
        g2d.drawImage(dragImage, dragPosition.x - dragOffset.x, dragPosition.y - dragOffset.y, TILE_SIZE, TILE_SIZE, null);
    }

    /**
     * @return the tile size
     */
    public int getCellSize() {
        return TILE_SIZE;
    }

    /**
     * Begins drag when mouse pressed on a movable figure
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (setupMode) return;
        if (againstAI && game.getCurrentPlayer().isGold() != humanPlaysGold) {
            return;
        }
        int col = (e.getX() - LABEL_MARGIN) / TILE_SIZE;
        int row = (e.getY() - LABEL_MARGIN) / TILE_SIZE;
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) return;

        Figure figure = board.getFigureAt(row, col);
        if (figure != null && figure.isGold() == game.getCurrentPlayer().isGold()) {
            dragFrom = new Position(row, col);
            selected = dragFrom;
            draggedFigure = figure;
            dragImage = ImageLoader.loadImage(figure.getImagePath(), TILE_SIZE, TILE_SIZE);
            dragPosition = e.getPoint();
            dragOffset = new Point(
                    (e.getX() - LABEL_MARGIN) % TILE_SIZE,
                    (e.getY() - LABEL_MARGIN) % TILE_SIZE
            );
            log.debug("Drag started at {}", dragFrom);
            repaint();
        } else {
            selected = null;
            log.debug("Invalid drag start at row={}, col={}", row, col);
        }

    }

    /**
     * Completes drag-and-drop on mouse release, performing or reverting the move
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (setupMode) return;
        if (againstAI && game.getCurrentPlayer().isGold() != humanPlaysGold) {
            return;
        }
        int col = (e.getX() - LABEL_MARGIN) / TILE_SIZE;
        int row = (e.getY() - LABEL_MARGIN) / TILE_SIZE;
        Position to = new Position(row, col);
        log.debug("Drag released at {}", to);

        if (awaitingAction) {
            if (actionOptions.contains(to)) {
                StepResult res = game.resolveStep(actionFrom, actionTo, to);
                res.notation.forEach(undoStack::push);
                btnPrevMove.setEnabled(true);
                updateStatus();
            }
            awaitingAction = false;
            repaint();
            return;
        }
        if (dragImage == null || dragFrom == null) return;

        try {
            StepResult res = game.step(dragFrom, to);
            if (res.type == ActionType.SIMPLE) {
                res.notation.forEach(undoStack::push);
                btnPrevMove.setEnabled(true);
                updateStatus();
                repaint();
            } else {
                awaitingAction = true;
                actionFrom = dragFrom;
                actionTo = to;
                actionType = res.type;
                actionOptions = res.options;
                repaint();
            }
        } catch (IllegalArgumentException ex) {
            // revert drag
            board.setFigureAt(dragFrom.getRow(), dragFrom.getCol(), draggedFigure);
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }

        //Clear drag state
        selected = null;
        dragFrom = null;
        dragImage = null;
        dragPosition = null;
        dragOffset = null;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!setupMode && dragImage != null) {
            dragPosition = e.getPoint();
            log.trace("Dragged at {}", dragPosition);
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

}

/**
 * Simple data class representing a possible move option for AI.
 */
class MoveOption {
    public final Position from, to;

    public MoveOption(Position from, Position to) {
        this.from = from;
        this.to = to;
    }
}

/**
 * Utility logic for AI: collects all possible legal moves for a given color.
 */
class AILogic {
    /**
     * Collects all simple legal moves for the specified color (gold or silver).
     *
     * @param game   the current game instance
     * @param isGold true to collect moves for gold, false for silver
     * @return a list of MoveOption objects representing all legal moves
     */
    public static List<MoveOption> collectAllMoves(Game game, boolean isGold) {
        List<MoveOption> all = new ArrayList<>();
        Board b = game.getBoard();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Position from = new Position(r, c);
                Figure f = b.getFigureAt(r, c);
                if (f != null && f.isGold() == isGold) {
                    for (Position to : List.of(
                            new Position(r + 1, c),
                            new Position(r - 1, c),
                            new Position(r, c + 1),
                            new Position(r, c - 1)
                    )) {
                        if (to.getRow() >= 0 && to.getRow() < Board.SIZE
                                && to.getCol() >= 0 && to.getCol() < Board.SIZE
                                && game.stepIsLegal(from, to)) {
                            all.add(new MoveOption(from, to));
                        }
                    }
                }
            }
        }
        return all;
    }
}
