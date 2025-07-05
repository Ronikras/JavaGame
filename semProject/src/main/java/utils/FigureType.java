package utils;

/**
 * Represents the type of figure in the Arimaa game.
 * Represents strength, image code and push & pull rules of all figures.
 */

public enum FigureType {
    ELEPHANT('e', 6, false),
    CAMEL('m', 5, false),
    HORSE('h', 3, true),
    DOG('d', 4, true),
    CAT('c', 2, true),
    RABBIT('r', 1, false);

    private final char code;
    private final int strength;
    private final boolean canBePushPull;
    private static final String PATH_FORMAT = "/imgs/Arimaa_%c%c.svg";

    FigureType(char code, int strength, boolean canBePushPull) {
        this.code = code;
        this.strength = strength;
        this.canBePushPull = canBePushPull;
    }

    /**
     * @return int strength value (higher means stronger)
     */
    public int getStrength() {
        return strength;
    }


    /**
     * Builds the image path based on type code and color initial.
     * @param isGold full color name (e.g., "gold", "silver")
     * @return formatted resource path
     */
    public String getPath(boolean isGold) {
        char colorChar = isGold ? 'g' : 's';
        return String.format(PATH_FORMAT, code, colorChar);
    }

    /**
     * Converts a single-letter string (case-insensitive) to a corresponding FigureType.
     *
     * @param s letter representing the figure ("R", "C", "D", "H", "M", "E")
     * @return corresponding FigureType
     * @throws IllegalArgumentException if the letter does not correspond to any type
     */
    public static FigureType fromLetter(String s) {
        switch (s.toUpperCase()) {
            case "R": return RABBIT;
            case "C": return CAT;
            case "D": return DOG;
            case "H": return HORSE;
            case "M": return CAMEL;
            case "E": return ELEPHANT;
            default: throw new IllegalArgumentException("Unknown type: " + s);
        }
    }
}
