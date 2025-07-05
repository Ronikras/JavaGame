package logic;

import figures.Figure;
import utils.FigureType;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a player in the game
 */
public class Player {
    private static final Logger log = LoggerFactory.getLogger(Player.class);

    private final int id;
    private final boolean isGold;
    private final List<Figure> pieces;
    private final long timeSpent;
    private static int counter = 0;

    /**
     * Constructs a new Player
     * @param isGold true if the player controls the gold side
     */
    public Player(boolean isGold) {
        this.id = counter++;
        this.isGold = isGold;
        this.pieces = new ArrayList<>();
        this.timeSpent = 0;
        log.info("Created Player {} ({} side)", id, isGold ? "gold" : "silver");
    }

    /**
     * Adds a figure to this player's collection
     * @param f the figure that was added
     */
    public void addFigure(Figure f) {
        pieces.add(f);
        log.debug("Added figure {}", f);
    }

    /**
     * Removes a figure from this player's collection
     * @param f the figure was captured
     */
    public void removeFigure(Figure f) {
        pieces.remove(f);
        log.debug("Removed figure {}", f);
    }


    /**
     * Check if player still has any rabbit
     * @return true if any owned figure is rabbit
     */
    public boolean hasRabbit(Board board){
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Figure f = board.getFigureAt(row, col);
                if (f != null && f.getType() == FigureType.RABBIT && f.isGold() == this.isGold()) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * @return true if this player controls gold side
     */
    public boolean isGold() {
        return isGold;
    }

    /**
     * @return id of the player
     */
    public int getId() {
        return id;
    }
}
