package logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a position on the game board.
 */
public class Position {
    private static final Logger log = LoggerFactory.getLogger(Position.class);

    private int row;
    private int col;

    /**
     * Constructs a new Position.
     * @param row the row index (0–7)
     * @param col the column index (0–7)
     */
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
        log.debug("Position created: row={}, col={}", row, col);
    }

    /**
     * @return zero-based row index
     */
    public int getRow() {
        return row;
    }

    /**
     * @return zero-based column index
     */
    public int getCol() {
        return col;
    }

    /**
     * Compares this position to another object for equality
     * @param obj the object to compare with
     * @return true if obj is a Position with the same row and column
     */
    @Override
    public boolean equals(Object obj) {
        log.trace("Equals: comparing {} with {}", this, obj);
        if (!(obj instanceof Position)) return false;
        Position other = (Position) obj;
        boolean result = this.row == other.row && this.col == other.col;
        log.trace("Position equals result={}", result);
        return result;
    }

    /**
     * @return hash code combining row and column
     */
    @Override
    public int hashCode() {
        int result = row * 31 + col;
        log.trace("Position hashCode result for {} = {}", this, result);
        return result;
    }

    /**
     * @return formatted string of the position
     */
    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
