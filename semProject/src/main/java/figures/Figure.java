package figures;

import utils.FigureType;

/**
 * Represents a game figure in Arimaa.
 * Each figure has a strength, a position, and specific movement rules.
 * This class defines common behavior for all figures in the game.
 */
public class Figure {
    private final FigureType type;
    private final boolean isGold;

    public Figure(FigureType type, boolean isGold) {
        this.type = type;
        this.isGold = isGold;
    }

    public FigureType getType() {return type;}

    public boolean isGold() {return isGold;}

    public int getStrength() {return type.getStrength();}


    /**
     * Provides the SVG path for the figure.
     */
    public String getImagePath() {
        return type.getPath(isGold);
    }

    @Override
    public String toString() {
        String color = isGold ? "gold" : "silver";
        return String.format("Figure[type=%s, color=%s]", type, color);
    }
}