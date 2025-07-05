package logic;

import figures.Figure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.FigureType;
import utils.GameMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Game class
 */
public class GameTest {
    private Game game;
    private Board board;

    @BeforeEach
    public void setUp() {
        game = new Game(GameMode.CLASSIC);
        board = game.getBoard();
        //Clear any initial pieces for isolated testes
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                board.setFigureAt(row, col, null);
            }
        }
    }

    @Test
    public void initialState_shouldHaveGoldToMoveAndCorrectMode() {
        assertTrue(game.getCurrentPlayer().isGold(), "Gold should move first");
        assertEquals(GameMode.CLASSIC, game.getMode(), "Game mode should be CLASSIC");
    }

    @Test
    public void step_nullFrom_throwsNullPointerException(){
        assertThrows(NullPointerException.class, () -> game.step(null, new Position(1,1)));
    }

    @Test
    public void step_invalidFrom_nullPointerOrIllegalArgument() {
        //Null from should cause NullPointerException due to getRow() usage
        assertThrows(NullPointerException.class, () -> game.step(null, new Position(0,0)));
        //Empty cell causes IllegalArgumentException
        Position p = new Position(3,3);
        assertThrows(IllegalArgumentException.class, () -> game.step(p, new Position(3,4)));
    }

    @Test
    public void step_wrongPlayerPiece_throwsIllegalArgumentException() {
        // Place a silver piece but gold's turn
        Figure enemy = new Figure(FigureType.CAT, false);
        board.setFigureAt(5,5, enemy);
        assertThrows(IllegalArgumentException.class, () -> game.step(new Position(5,5), new Position(5,6)));
    }


    @Test
    public void step_emptyFromCell_throwsIllegalArgumentException(){
        Position from = new Position(3,3);
        Position to = new Position(3, 4);
        //No figures placed at from
        assertThrows(IllegalArgumentException.class, () -> game.step(from, to));
    }

    @Test
    public void step_invalidDistance_throwsIllegalArgumentException() {
        Figure fig = new Figure(FigureType.DOG, true);
        board.setFigureAt(4,4, fig);
        // Move two squares
        assertFalse(game.stepIsLegal(new Position(4,4), new Position(6,4)));
        assertThrows(IllegalArgumentException.class, () -> game.step(new Position(4,4), new Position(6,4)));
    }


    @Test
    public void stepIsLegal_variousScenarios() {
        Figure rabbit = new Figure(FigureType.RABBIT, true);
        board.setFigureAt(5,5, rabbit);
        // Legal forward move
        assertTrue(game.stepIsLegal(new Position(5,5), new Position(4,5)));
        // Illegal backward for rabbit
        assertFalse(game.stepIsLegal(new Position(5,5), new Position(6,5)));
        // Out of bounds move
        assertFalse(game.stepIsLegal(new Position(5,5), new Position(5,8)));
        // Empty source
        assertFalse(game.stepIsLegal(new Position(0,0), new Position(0,1)));
    }

    @Test
    public void skipStep_flipsAfterMaxSteps() {
        //Initially gold
        assertTrue(game.getCurrentPlayer().isGold());
        for (int i = 0; i < Game.MAX_TURNS_STEPS; i++) {
            game.skipStep();
        }
        //After max skips, should flip
        assertFalse(game.getCurrentPlayer().isGold());
    }

    @Test
    public void endTurnEarly_resetsAndSwitchesPlayer() {
        List<String> fillers = game.endTurnEarly();
        //Should return exactly MAX_TURNS_STEPS "-" tokens
        assertEquals(Game.MAX_TURNS_STEPS, fillers.size());
        assertTrue(fillers.stream().allMatch(s -> s.equals("-")));
        //Current player should have switched
        assertFalse(game.getCurrentPlayer().isGold());
        //Step counter should be reset
        assertEquals(Game.MAX_TURNS_STEPS, game.getTurnsSteps());
    }

}
