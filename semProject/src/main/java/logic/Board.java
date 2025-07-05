package logic;

import figures.*;
import utils.FigureType;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.GameMode;

/**
 * Represents the game board as a 2D matrix of Figures and trap flags.
 * Provides methods to query, move, capture ana analyze board state
 */
public class Board {
    public static final int SIZE = 8;
    private final Figure[][] grid;
    private final boolean[][] traps;
    private static final Logger log = LoggerFactory.getLogger(Board.class);

    /**
     * Initializes an empty board, sets trap locations, and places initial piece setup:
     * <ul>
     * <li>Rabbits on rows 1 (silver) and 6 (gold)</li>
     * <li>Full silver rank on row 0</li>
     * <li>Full gold rank on row 7</li>
     * </ul>
     */
    public Board() {
        grid = new Figure[SIZE][SIZE];
        traps = new boolean[SIZE][SIZE];
        log.info("Initializing board of size {}x{}", SIZE, SIZE);

        // Traps squares initialization
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                traps[r][c] = (r == 2 || r == 5) && (c == 2 || c == 5);
            }
        }
        log.debug("Trap layout initialized");

    }

    /**
     * Sets up a standard Arimaa starting position for both gold and silver.
     * <ul>
     *   <li>Gold rabbits at row 7, silver rabbits at row 0.</li>
     *   <li>Other gold pieces on row 6, other silver pieces on row 1.</li>
     * </ul>
     */
    private void initStandard() {
        //Gold rabbits on the row 6 and silver rabbits on the row 1
        for (int c = 0; c < SIZE; c++) {
            grid[7][c] = new Figure(FigureType.RABBIT, true);
            grid[0][c] = new Figure(FigureType.RABBIT, false);
        }
        log.debug("Rabbits placed on rows 0 and 7");

        //Golden rank (row 6)
        grid[6][0] = new Figure(FigureType.ELEPHANT, true);
        grid[6][1] = new Figure(FigureType.CAMEL, true);
        grid[6][2] = new Figure(FigureType.HORSE, true);
        grid[6][3] = new Figure(FigureType.DOG, true);
        grid[6][4] = new Figure(FigureType.DOG, true);
        grid[6][5] = new Figure(FigureType.HORSE, true);
        grid[6][6] = new Figure(FigureType.CAT, true);
        grid[6][7] = new Figure(FigureType.CAT, true);
        log.debug("Gold pieces placed on row 6");

        //Silver rank (row 1)
        grid[1][0] = new Figure(FigureType.CAT, false);
        grid[1][1] = new Figure(FigureType.CAT, false);
        grid[1][2] = new Figure(FigureType.HORSE, false);
        grid[1][3] = new Figure(FigureType.DOG, false);
        grid[1][4] = new Figure(FigureType.DOG, false);
        grid[1][5] = new Figure(FigureType.HORSE, false);
        grid[1][6] = new Figure(FigureType.CAMEL, false);
        grid[1][7] = new Figure(FigureType.ELEPHANT, false);
        log.debug("Silver pieces placed on row 1");
    }

    /**
     * Gets the figure at the specified location or null (if it is empty).
     *
     * @param row zero-based row index
     * @param col zero-based column index
     * @return the {@link Figure} at the position, or null if empty
     */
    public Figure getFigureAt(int row, int col) {
        return grid[row][col];
    }

    /**
     * Places a figure at the specified location (null to clear).
     *
     * @param row    zero-based row index
     * @param col    zero-based column index
     * @param figure {@link Figure} to place or null to clear
     */
    public void setFigureAt(int row, int col, Figure figure) {
        log.debug("Setting figure {} at row {} and col {}", figure, row, col);
        grid[row][col] = figure;
    }

    /**
     * Checks if a piece at the specified position is frozen
     * A figure is frozen if there is at list one orthogonally adjacent enemy figure whose strength is strictly greater that the figure's, and there are no orthogonally adjacent friendly grid to support it
     *
     * @param row board row
     * @param col board col
     * @return true if frozen, false otherwise
     */
    public boolean isFrozen(int row, int col) {
        Figure figure = grid[row][col];
        if (figure == null) return false; //No grid = cannot be frozen
        log.debug("Checking if piece is frozen at row {} and col {}: {}", row, col, figure);

        boolean enemyStronger = false;
        boolean friendAdjacent = false;

        //Check the orthogonal direction for neighbors
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : dirs) {
            int r2 = row + dir[0], c2 = col + dir[1];
            //Skips out of bond position
            if (r2 < 0 || r2 >= SIZE || c2 < 0 || c2 >= SIZE) continue;
            Figure neighbor = grid[r2][c2];
            if (neighbor == null) continue;
            if (neighbor.isGold() == figure.isGold()) {
                //Friendly neighbor provides support
                friendAdjacent = true;
            } else {
                //Check if enemy is stronger
                if (neighbor.getType().getStrength() > figure.getType().getStrength()) {
                    enemyStronger = true;
                }
            }
        }
        //Frozen if there's at least one stronger enemy and no friendly support
        boolean frozen = enemyStronger && !friendAdjacent;
        log.debug("isFrozen result at row {} and col {}: {}", row, col, frozen);
        return frozen;
    }

    /**
     * Counts orthogonally adjacent friendly grid to the one at the given location
     *
     * @param row board row
     * @param col board column
     * @return number of adjacent friends
     */
    public int countFriends(int row, int col) {
        Figure figure = grid[row][col];
        if (figure == null) return 0;
        int count = 0;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : dirs) {
            int neighborRow = row + dir[0], neighborCol = col + dir[1];
            if (neighborRow >= 0 && neighborRow < SIZE && neighborCol >= 0 && neighborCol < SIZE) {
                Figure neighbor = grid[neighborRow][neighborCol];
                if (neighbor != null && neighbor.isGold() == figure.isGold())
                    count++; // Increment for each adjacent friendly piece
            }
        }
        log.trace("countFriends at row {} col {} is {}", row, col, count);
        return count;
    }

    /**
     * Clears the board, removing all pieces.
     */
    public void clear() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                grid[r][c] = null;
    }

    /**
     * Randomly places all silver pieces in the top two rows.
     * Used for random setup mode or AI testing.
     */
    public void randomizeSilver() {
        List<Position> free = new ArrayList<>();
        for (int r = 0; r <= 1; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == null) free.add(new Position(r, c));
            }
        }
        Map<FigureType, Integer> expected = Map.of(
                FigureType.RABBIT, 8,
                FigureType.CAT, 2,
                FigureType.DOG, 2,
                FigureType.HORSE, 2,
                FigureType.CAMEL, 1,
                FigureType.ELEPHANT, 1
        );
        List<FigureType> types = new ArrayList<>();
        expected.forEach((t, n) -> {
            for (int i = 0; i < n; i++) types.add(t);
        });
        Collections.shuffle(free);
        Collections.shuffle(types);

        int count = Math.min(types.size(), free.size());
        for (int i = 0; i < count; i++) {
            Position p = free.get(i);
            Figure f = new Figure(types.get(i), false);
            grid[p.getRow()][p.getCol()] = f;
        }
    }

    /**
     * Returns a deep copy of the board matrix (does NOT clone Figure objects).
     *
     * @return 8x8 array of Figure references (may contain nulls)
     */
    public Figure[][] getBoardMatrix() {
        Figure[][] result = new Figure[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++)
            for (int col = 0; col < SIZE; col++)
                result[row][col] = getFigureAt(row, col);
        return result;
    }

}


