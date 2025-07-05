package GUI;

import figures.Figure;
import logic.Board;
import logic.Game;
import utils.FigureType;
import utils.GameMode;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static GUI.BoardPanel.LABEL_MARGIN;

/**
 * Main game window for Arimaa, handling menu navigation, game initialization, loading/saving and embedding the game board UI
 */
public class GameWindow extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(GameWindow.class);

    private Game game;
    private BoardPanel boardPanel;
    private GameMode mode;
    private boolean gameActive = false;
    private boolean againstAI = false;
    private boolean humanPlaysGold = true;


    private Figure[][] customSetup = new Figure[Board.SIZE][Board.SIZE];

    private JPanel startPanel;

    /**
     * Constructs and displays the initial game window in CLASSIC mode
     */
    public GameWindow() {
        super("Arimaa");
        log.info("Initializing GameWindow with mode {}", mode);

        this.mode = GameMode.CLASSIC;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        initStartPanel();
        add(startPanel, BorderLayout.CENTER);

        pack();
        setSize(800, 600);
        setResizable(false);
        setVisible(true);
        log.info("Start panel displayed");
    }

    /**
     * Initializes the main menu panel allowing the player to:
     * <ul>
     *   <li>View game rules and link to official site</li>
     *   <li>Select CLASSIC or FAST mode</li>
     *   <li>Start a new game, load a saved game, or exit</li>
     * </ul>
     */
    private void initStartPanel() {
        if (startPanel != null) remove(startPanel);
        startPanel = new JPanel(new BorderLayout(10, 10));
        startPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Welcome to Arimaa", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        startPanel.add(title, BorderLayout.NORTH);

        //Center HTML instructions and mode selector
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        String html = "<html><div style='text-align:center; font-size:14px;'>"
                + "We are happy to see you here!<br>" + "Let us introduce some of the rules: <br>"
                + "• Each player has 16 pieces (8 rabbits, 2 cats, 2 dogs, 2 horses, 1 camel and 1 elephant);<br>"
                + "• Each piece has its own straight so for example the rabbit is the weakest - 1 and the elephant is the strongest - 6;<br>"
                + "• Rabbits move forward only;<br>"
                + "• Goal: get a rabbit to opponent’s home row or eliminate all rabbits;<br>"
                + "• Trapped pieces without support are captured;<br>"
                + "• 4 steps/turn (CLASSIC), timed (FAST) or PvC.<br>" + "For more information you can visit the <a href=\"https://arimaa.com/arimaa/\">official website</a>.<br>"
                + "Please choose mode of the game."
                + "</div></html>";
        JEditorPane htmlPane = new JEditorPane("text/html", html);
        htmlPane.setEditable(false);
        htmlPane.setOpaque(false);
        htmlPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        htmlPane.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));

        //Prevent caret bliking
        DefaultCaret caret = (DefaultCaret) htmlPane.getCaret();
        caret.setBlinkRate(0);
        htmlPane.setFocusable(false);

        htmlPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String url = e.getURL().toString();
                log.info("User clicked hyperlink: {}", url);
                try {
                    Desktop.getDesktop().browse(new URI(url));
                    log.debug("Opened browser for URL: {}", url);
                } catch (Exception ex) {
                    log.info("Failed to open URL {}: {}", url, ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(this, "Cannot open link: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        centerPanel.add(htmlPane);

        //Mode selection dropdown
        JComboBox<GameMode> combo = new JComboBox<>(GameMode.values());
        combo.setSelectedItem(mode);
        combo.setMaximumSize(new Dimension(300, combo.getPreferredSize().height));
        combo.setAlignmentX(Component.CENTER_ALIGNMENT);
        combo.addActionListener(e -> this.mode = (GameMode) combo.getSelectedItem());
        centerPanel.add(combo);
        startPanel.add(centerPanel, BorderLayout.CENTER);

        //Buttons New, Load, Exit
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btns.setOpaque(false);
        JButton btnNew = new JButton("New Game");
        JButton btnLoad = new JButton("Load Game");
        JButton btnExit = new JButton("Exit");
        btns.add(btnNew);
        btns.add(btnLoad);
        btns.add(btnExit);
        startPanel.add(btns, BorderLayout.SOUTH);


        btnNew.addActionListener(e -> {
            if (mode != GameMode.CLASSIC && mode != GameMode.FAST) {
                againstAI = true;
                humanPlaysGold = true;
                showSinglePlayerSetup();
            } else {
                againstAI = false;
                showSetupScreen();
            }
            log.info("Game started with mode {}", mode);
        });

        btnLoad.addActionListener(e -> loadFromFile());

        btnExit.addActionListener(e -> {
            log.info("Exit selected from game window");
            handleExit();
        });

    }

    /**
     * Prompts the user to select a saved game file and loads it into the current game
     */
    private void loadFromFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                Game loaded = Game.loadFromFile(file.getPath(), mode);
                log.info("Game loaded from {} with mode {}", file.getPath(), mode);
                this.game = loaded;
                gameActive = true;
                initPlayPhase();
            } catch (IOException ex) {
                log.error("Load failed: {}", ex.getMessage(), ex);
                JOptionPane.showMessageDialog(this, "Loaded failed: " + ex.getMessage());
            }
        }
    }

    /**
     * Displays the setup screen for manual piece placement before starting the game (2-player mode).
     * Handles gold/silver setup and validates correct piece counts.
     */
    private void showSetupScreen() {
        final int TOTAL_PIECES = 16;
        //Expected number of pieces by type for each player
        Map<FigureType, Integer> EXPECTED_COUNTS = Map.of(
                FigureType.RABBIT, 8,
                FigureType.CAT, 2,
                FigureType.DOG, 2,
                FigureType.HORSE, 2,
                FigureType.CAMEL, 1,
                FigureType.ELEPHANT, 1
        );

        //Counters for Gold and Silver players
        Map<FigureType, Integer> goldCounts = new EnumMap<>(FigureType.class);
        Map<FigureType, Integer> silverCounts = new EnumMap<>(FigureType.class);
        EXPECTED_COUNTS.keySet().forEach(type -> {
            goldCounts.put(type, 0);
            silverCounts.put(type, 0);
        });

        AtomicBoolean isGoldTurn = new AtomicBoolean(true);

        //Status lbl at the top showing placement progress
        JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 16f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        //"Start Game" button, disabled until setup is completed
        JButton btnStart = new JButton("Start Game");
        btnStart.setEnabled(false);

        //Empty board for piedce placement
        Board setupBoard = new Board();
        setupBoard.clear();
        BoardPanel setupPanel = new BoardPanel(setupBoard, customSetup);
        log.info("Entering setup screen");

        //Update status lbl function
        Runnable updateStatus = () -> {
            boolean gold = isGoldTurn.get();
            Map<FigureType, Integer> cnts = gold ? goldCounts : silverCounts;
            String player = gold ? "Gold" : "Silver";
            int placed = cnts.values().stream().mapToInt(Integer::intValue).sum();
            String rows = gold ? "1–2" : "7–8";
            statusLabel.setText(
                    String.format("%s: %d/%d pieces (rows %s)",
                            player, placed, TOTAL_PIECES, rows)
            );
            log.debug("Status updated: {}", statusLabel.getText());
        };
        updateStatus.run();

        setupPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = (e.getX() - LABEL_MARGIN) / setupPanel.getCellSize();
                int row = (e.getY() - LABEL_MARGIN) / setupPanel.getCellSize();
                if (col < 0 || col >= Board.SIZE || row < 0 || row >= Board.SIZE) return;

                boolean gold = isGoldTurn.get();
                Map<FigureType, Integer> cnts = gold ? goldCounts : silverCounts;
                int minRow = gold ? 6 : 0, maxRow = gold ? 7 : 1;

                //Row placement validation
                if (row < minRow || row > maxRow) {
                    JOptionPane.showMessageDialog(
                            GameWindow.this,
                            String.format("%s may only place on rows %d-%d",
                                    gold ? "Gold" : "Silver",
                                    minRow, maxRow),
                            "Invalid row",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                //Switch turn, if current player placed all pieces
                int placed = cnts.values().stream().mapToInt(Integer::intValue).sum();
                if (placed >= TOTAL_PIECES) {
                    isGoldTurn.set(!gold);
                    gold = !gold;
                    cnts = gold ? goldCounts : silverCounts;
                    log.info("All pieces placed. Switching turn to {}", gold ? "Gold" : "Silver");
                    updateStatus.run();
                }

                //Build popup menu with only the piece types still available
                JPopupMenu menu = new JPopupMenu();
                for (FigureType type : FigureType.values()) {
                    if (cnts.get(type) < EXPECTED_COUNTS.get(type)) {
                        JMenuItem item = new JMenuItem((gold ? "Gold " : "Silver ") + type);
                        boolean finalGold = gold;
                        Map<FigureType, Integer> finalCnts = cnts;
                        item.addActionListener(ae -> {
                            //Remove old piece if any and decrement its counter
                            Figure old = setupBoard.getFigureAt(row, col);
                            if (old != null) {
                                Map<FigureType, Integer> oldMap = old.isGold() ? goldCounts : silverCounts;
                                oldMap.put(old.getType(), oldMap.get(old.getType()) - 1);
                                log.debug("Removed {} at ({}, {})", old, row, col);
                            }
                            //Place new piece
                            Figure f = new Figure(type, finalGold);
                            setupBoard.setFigureAt(row, col, f);
                            customSetup[row][col] = f;
                            finalCnts.put(type, finalCnts.get(type) + 1);
                            log.info("{} placed {} at ({}, {})", finalGold ? "Gold" : "Silver", type, row, col);

                            // Update UI
                            updateStatus.run();
                            setupPanel.repaint();

                            //Enable start button if both players placed all pieces
                            int sumGold = goldCounts.values().stream().mapToInt(i -> i).sum();
                            int sumSilver = silverCounts.values().stream().mapToInt(i -> i).sum();
                            if (sumGold == TOTAL_PIECES && sumSilver == TOTAL_PIECES) {
                                btnStart.setEnabled(true);
                                log.info("Both players have completed setup. Start Game enabled.");
                            }

                            //Switch turn if the current player just finished
                            if (finalCnts.values().stream().mapToInt(i -> i).sum() >= TOTAL_PIECES
                                    && !(sumGold == TOTAL_PIECES && sumSilver == TOTAL_PIECES)) {
                                isGoldTurn.set(!finalGold);
                                log.info("{} has finished. Switching turn to {}", finalGold ? "Gold" : "Silver", !finalGold ? "Gold" : "Silver");
                                updateStatus.run();
                            }
                        });
                        menu.add(item);
                    }
                }
                menu.show(setupPanel, e.getX(), e.getY());
            }
        });

        //Handle "Start Game" button click
        btnStart.addActionListener(ae -> {
            log.info("Start Game button clicked. Validating setup.");
            boolean goldOk = goldCounts.equals(EXPECTED_COUNTS);
            boolean silverOk = silverCounts.equals(EXPECTED_COUNTS);

            if (!goldOk || !silverOk) {
                if (!goldOk && silverOk) {
                    log.error("Gold setup is incorrect. Expected: {}", EXPECTED_COUNTS);
                    JOptionPane.showMessageDialog(this,
                            "Gold setup is incorrect.\nExpected: " + EXPECTED_COUNTS,
                            "Gold Setup Error", JOptionPane.ERROR_MESSAGE);
                } else if (goldOk && !silverOk) {
                    log.error("Silver setup is incorrect. Expected: {}", EXPECTED_COUNTS);
                    JOptionPane.showMessageDialog(this,
                            "Silver setup is incorrect.\nExpected: " + EXPECTED_COUNTS,
                            "Silver Setup Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    log.error("Both setups are incorrect. Expected each: {}", EXPECTED_COUNTS);
                    JOptionPane.showMessageDialog(this,
                            "Both setups are incorrect.\nExpected each: " + EXPECTED_COUNTS,
                            "Setup Errors", JOptionPane.ERROR_MESSAGE);
                }
                return;
            }
            //Setup is valid - start the game
            log.info("Setup valid. Starting game.");
            List<String> goldSetup = buildSetupNotation(customSetup, true);
            List<String> silverSetup = buildSetupNotation(customSetup, false);
            game = new Game(mode);
            game.addSetupMove(goldSetup, true);
            game.addSetupMove(silverSetup, false);
            startGameWithBoard();
        });

        //Layout the setup screen
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(statusLabel, BorderLayout.NORTH);
        wrapper.add(setupPanel, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnStart);
        wrapper.add(south, BorderLayout.SOUTH);

        getContentPane().removeAll();
        add(wrapper, BorderLayout.CENTER);
        revalidate();
        repaint();
        pack();
    }

    /**
     * Displays the single-player setup screen for gold piece placement (AI mode).
     * Silver pieces are randomized after gold setup.
     */
    private void showSinglePlayerSetup() {
        final int TOTAL = 16;
        Map<FigureType, Integer> EXPECTED = Map.of(
                FigureType.RABBIT, 8,
                FigureType.CAT, 2,
                FigureType.DOG, 2,
                FigureType.HORSE, 2,
                FigureType.CAMEL, 1,
                FigureType.ELEPHANT, 1
        );
        Map<FigureType, Integer> goldCounts = new EnumMap<>(FigureType.class);
        EXPECTED.keySet().forEach(t -> goldCounts.put(t, 0));

        JLabel status = new JLabel("", SwingConstants.CENTER);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 16f));
        status.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        JButton btnStart = new JButton("Start Game");
        btnStart.setEnabled(false);

        Board b = new Board();
        b.clear();
        BoardPanel panel = new BoardPanel(b, customSetup);

        Runnable upd = () -> {
            int placed = goldCounts.values().stream().mapToInt(i -> i).sum();
            status.setText("Gold: " + placed + "/" + TOTAL + " pieces (rows 1–2)");
        };
        upd.run();
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = (e.getX() - LABEL_MARGIN) / panel.getCellSize();
                int row = (e.getY() - LABEL_MARGIN) / panel.getCellSize();
                if (row < 6 || row > 7 || col < 0 || col >= Board.SIZE) {
                    JOptionPane.showMessageDialog(GameWindow.this,
                            "Gold may only place on rows 1–2", "Invalid row", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                JPopupMenu m = new JPopupMenu();
                for (FigureType t : FigureType.values()) {
                    if (goldCounts.get(t) < EXPECTED.get(t)) {
                        JMenuItem mi = new JMenuItem("Gold " + t);
                        mi.addActionListener(a -> {
                            Figure old = b.getFigureAt(row, col);
                            if (old != null) {
                                goldCounts.put(old.getType(), goldCounts.get(old.getType()) - 1);
                            }
                            Figure f = new Figure(t, true);
                            b.setFigureAt(row, col, f);
                            customSetup[row][col] = f;
                            goldCounts.put(t, goldCounts.get(t) + 1);
                            upd.run();
                            panel.repaint();
                            if (goldCounts.values().stream().mapToInt(i -> i).sum() == TOTAL) {
                                btnStart.setEnabled(true);
                            }
                        });
                        m.add(mi);
                    }
                }
                m.show(panel, e.getX(), e.getY());
            }
        });
        btnStart.addActionListener(ae -> {
            if (!EXPECTED.equals(goldCounts)) {
                JOptionPane.showMessageDialog(this,
                        "Gold setup incorrect, expected: " + EXPECTED,
                        "Setup Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (int row = 6; row <= 7; row++) {
                for (int col = 0; col < Board.SIZE; col++) {
                    if (customSetup[row][col] != null)
                        b.setFigureAt(row, col, customSetup[row][col]);
                }
            }
            b.randomizeSilver();
            List<String> goldSetup = buildSetupNotation(b.getBoardMatrix(), true);
            List<String> silverSetup = buildSetupNotation(b.getBoardMatrix(), false);
            game = new Game(mode);
            game.addSetupMove(goldSetup, true);
            game.addSetupMove(silverSetup, false);
            startGameWithBoard();
        });
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(status, BorderLayout.NORTH);
        wrap.add(panel, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnStart);
        wrap.add(south, BorderLayout.SOUTH);

        getContentPane().removeAll();
        add(wrap, BorderLayout.CENTER);
        revalidate();
        repaint();
        pack();
    }

    /**
     * Starts a new game using the given board configuration.
     * <p>Creates the Game instance in the current mode, marks the game as active,
     * and—if running in FAST mode—initializes and starts the game timer for the current player's turn</p>
     *
     */
    private void startGameWithBoard() {
        gameActive = true;

        if (mode == GameMode.FAST) {
            game.getTimer().startGame();
            game.getTimer().startTurn(game.getCurrentPlayer().isGold());
        }

        initPlayPhase();
    }

    /**
     * Transitions the UI to the play phase (shows board, control buttons).
     */
    private void initPlayPhase(){
        getContentPane().removeAll();
        boardPanel = new BoardPanel(game, againstAI, humanPlaysGold);
        JButton btnBack = makeBackButton();
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(btnBack, BorderLayout.NORTH);
        wrapper.add(boardPanel, BorderLayout.CENTER);

        getContentPane().add(wrapper, BorderLayout.CENTER);
        revalidate();
        repaint();
        pack();
    }

    /**
     * Creates a "Back to menu" button.
     * Prompts to save before quitting and returns to the main menu.
     *
     * @return ready-to-use JButton
     */
    private JButton makeBackButton() {
        JButton btnBack = new JButton("Back to menu");
        btnBack.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    GameWindow.this,
                    "Do you want to save the current game before returning to the main menu?",
                    "Save Game?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }
            if (mode == GameMode.FAST && game != null) {
                game.getTimer().stopGameTimer();
            }
            if (choice == JOptionPane.YES_OPTION) {
                JFileChooser chooser = new JFileChooser();
                if (chooser.showSaveDialog(GameWindow.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        game.saveHistoryToFile(chooser.getSelectedFile().getPath());
                        log.info("Game saved to {}", chooser.getSelectedFile().getPath());
                    } catch (IOException ex) {
                        log.error("Save failed: {}", ex.getMessage(), ex);
                        JOptionPane.showMessageDialog(
                                GameWindow.this,
                                "Save failed: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }
                } else {
                    return;
                }
            }
            log.info("Returning to main menu");
            gameActive = false;
            getContentPane().removeAll();
            initStartPanel();
            getContentPane().add(startPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
            pack();
        });
        return btnBack;
    }

    /**
     * Builds the initial placement notation (setup row) for gold or silver pieces.
     *
     * @param setup 2D array with placed figures
     * @param gold  true for gold, false for silver
     * @return list of setup notations for the given color
     */
    private List<String> buildSetupNotation(Figure[][] setup, boolean gold) {
        List<String> res = new ArrayList<>();
        int minRow = gold ? 6 : 0, maxRow = gold ? 7 : 1;
        for (int row = minRow; row <= maxRow; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Figure f = setup[row][col];
                if (f != null && f.isGold() == gold) {
                    char pieceChar = gold
                            ? Character.toUpperCase(f.getType().name().charAt(0))
                            : Character.toLowerCase(f.getType().name().charAt(0));
                    char file = (char) ('a' + col);
                    char rank = (char) ('1' + (Board.SIZE - 1 - row));
                    res.add("" + pieceChar + file + rank);
                }
            }
        }
        return res;
    }


    /**
     * Handles window closing: prompts to save if a game is active, then exist the application
     */
    private void handleExit() {
        if (!gameActive) {
            System.exit(0);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Do you want to save the game before the exit?",
                "Save Game",
                JOptionPane.YES_NO_CANCEL_OPTION
        );
        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }
        if (choice == JOptionPane.YES_OPTION) {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    game.saveHistoryToFile(chooser.getSelectedFile().getPath());
                    log.info("Game saved before exit");
                } catch (IOException ex) {
                    log.error("Save failed: {}", ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
                }
            } else {
                return;
            }
        }
        System.exit(0);
    }

    /**
     * Application entry point: crates and shows the GameWindow on the EDT
     */
    public static void main(String[] args) {
        log.info("Launching GameWindow");
        SwingUtilities.invokeLater(GameWindow::new);
    }
}
