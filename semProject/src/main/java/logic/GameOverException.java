package logic;

/**
 * Throws to signal that the game has ended with a winner
 */
public class GameOverException extends RuntimeException {

    /**
     * Constructs a GAmeOverException with the specified detail message
     * @param message the victory message to display
     */
    public GameOverException(String message) {
        super(message);
    }
}
