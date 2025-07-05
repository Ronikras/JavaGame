package logic;

import figures.Figure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.FigureType;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {
    private Board board;

    @BeforeEach
    public void setUp() {
        board = new Board();
        //Clear initial figures for isolated testing
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                board.setFigureAt(row, col, null);
            }
        }
    }

    @Test
    public void testSetAndGetFigure() {
        assertNull(board.getFigureAt(3, 3));
        Figure rabbit = new Figure(FigureType.RABBIT, true);
        board.setFigureAt(3, 3, rabbit);
        assertSame(rabbit, board.getFigureAt(3, 3));
    }


    @Test
    public void testCountFriends_NoFriends() {
        //Single rabbit in isolation
        Figure rabbit = new Figure(FigureType.RABBIT, false);
        board.setFigureAt(4, 4, rabbit);
        assertEquals(0, board.countFriends(4, 4));
    }

    @Test
    public void testCountFriends_WithFriends() {
        //Place center figure and two adjacent friends
        Figure cat = new Figure(FigureType.CAT, true);
        board.setFigureAt(2, 2, cat);
        board.setFigureAt(2, 1, new Figure(FigureType.CAMEL, true));
        board.setFigureAt(3, 2, new Figure(FigureType.DOG, true));
        //Enemy shouldn't count
        board.setFigureAt(1, 2, new Figure(FigureType.RABBIT, false));
        assertEquals(2, board.countFriends(2, 2));
    }

    @Test
    public void testIsFrozen_NullFigures(){
        //Empty cell isn't frozen
        assertFalse(board.isFrozen(0, 0));
    }

    @Test
    public void testIsFrozen_WithStrongerEnemyAndNoFriends(){
        //Weaker piece surrounded by stronger enemies
        Figure rabbit = new Figure(FigureType.RABBIT, true);
        board.setFigureAt(4, 4, rabbit);
        //Surround with stronger enemies
        board.setFigureAt(4, 3, new Figure(FigureType.CAMEL, false));
        board.setFigureAt(4, 5, new Figure(FigureType.CAT, false));
        //No adjacent friend
        assertTrue(board.isFrozen(4, 4));
    }

    @Test
    public void testIsFrozen_WithStrongerEnemyAndFriend(){
        //Figure has both stronger enemy and adjacent friend that isn't frozen
        Figure rabbit = new Figure(FigureType.RABBIT, false);
        board.setFigureAt(6, 6, rabbit);
        board.setFigureAt(6, 5, new Figure(FigureType.ELEPHANT, true)); //Stronger enemy
        board.setFigureAt(5, 6, new Figure(FigureType.RABBIT, false)); //Adjacent friend
        assertFalse(board.isFrozen(6, 6));
    }
}
