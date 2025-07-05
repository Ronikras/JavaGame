package logic;

import figures.Figure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for parsing and formatting single-step Arimaa move notation
 * Notation format: <code>PieceFileRankAction</code>, for example "Ra3n" indicates a golden Rabbit moves from square a3 to the north
 */
public class Notation {
    private static final Logger logger = LoggerFactory.getLogger(Notation.class);

    /**
     * Represents a parsed Arimaa move consisting of the moving piece, source and destination position, action character
     */
    public static class Move {
        public final char piece; // Figure code
        private final Position from;
        private final Position to;
        public final char action; // Action character: 'n', 's', 'e', 'w' for direction, or 'x' for traps
        private final Position destination;

        /**
         * @param piece       figure code
         * @param from        source {@link Position}
         * @param to          destination {@link Position}, or null if no movement
         * @param action      action character indicating movement direction or 'x'
         * @param destination
         */
        public Move(char piece, Position from, Position to, char action, Position destination) {
            this.piece = piece;
            this.from = from;
            this.to = to;
            this.action = action;
            this.destination = destination;
        }

        /**
         * @return source {@link Position}
         */
        public Position getFrom() {
            return from;
        }

        /**
         * @return destination {@link Position} or null
         */
        public Position getTo() {
            return to;
        }

        /**
         * @return extra destination for push/pull (may be null)
         */
        public Position getDestination() {
            return destination;
        }

        /**
         * Formats this move back into Arimaa notation.
         * <p>Simple move: "Ra3n", Push: "Rh4>h5", Pull: "Rh4<h3", Capture: "Ch3x"</p>
         *
         * @return string in standard notation
         */
        @Override
        public String toString() {
            if (action == '-') return "-";
            String coord = fileRank(from);
            if ((action == '>' || action == '<') && destination != null) {
                return String.format("%c%s%c%s", piece, coord, action, fileRank(destination));
            }
            return String.format("%c%s%c", piece, coord, action);
        }
    }

    /**
     * Parses a single Arimaa step notation string into a {@link Move} object.
     *
     * @param s 3–6 character notation string ("Ra3n", "Re2>e3", "Ch3x", "-")
     * @return parsed {@link Move}
     * @throws IllegalArgumentException if the notation is invalid
     */
    public static Move parse(String s) {
        logger.debug("Beginning of a notation parsing: {}", s);
        if ("-".equals(s)) {
            return new Move('-', null, null, '-', null);
        }
        if (s == null) {
            logger.error("Invalid notation: {}", s);
            throw new IllegalArgumentException("Invalid notation: " + s);
        }

        if (!s.contains(">") && !s.contains("<") && s.length() != 3 && s.length() != 4) {
            logger.error("Invalid notation: {}", s);
            throw new IllegalArgumentException("Invalid notation: " + s);
        }

        char piece = s.charAt(0);

        if (s.contains(">")) {
            int idx = s.indexOf('>');
            String base = s.substring(1, idx); // "b4e"
            String dest = s.substring(idx + 1); // "c3"
            Position from = parsePosition(base.substring(0, 2));
            char dir = base.charAt(2);
            Position to = applyDir(from, dir);
            Position destination = parsePosition(dest);
            return new Move(piece, from, to, '>', destination);
        }

        if (s.contains("<")) {
            int idx = s.indexOf('<');
            String base = s.substring(1, idx); // "b4e"
            String dest = s.substring(idx + 1); // "c3"
            Position from = parsePosition(base.substring(0, 2));
            char dir = base.charAt(2);
            Position to = applyDir(from, dir);
            Position destination = parsePosition(dest);
            return new Move(piece, from, to, '<', destination);
        }

        if (s.length() == 4 && s.endsWith("x")) {
            Position from = parsePosition(s.substring(1, 3));
            return new Move(piece, from, null, 'x', null);
        }

        if (s.length() == 4) {
            String pos = s.substring(1, 3);
            char dir = s.charAt(3);
            Position from = parsePosition(pos);
            Position to = applyDir(from, dir);
            return new Move(piece, from, to, dir, null);
        }

        if (s.length() == 3) {
            Position from = parsePosition(s.substring(1, 3));
            return new Move(piece, from, from, '=', null);
        }

        logger.error("Invalid notation: {}", s);
        throw new IllegalArgumentException("Invalid notation: " + s);

    }

    /**
     * Formats a simple move: Piece + from-file/rank + direction char
     */
    public static String formatSimple(Figure mover, Position from, Position to) {
        char pChar = mover.isGold()
                ? Character.toUpperCase(mover.getType().name().charAt(0))
                : Character.toLowerCase(mover.getType().name().charAt(0));
        char action = direction(from, to);
        return String.format("%c%s%c", pChar, fileRank(from), action);
    }

    /**
     * Parses a 2-charecter board coordinate into a {@link Position}
     * <p>Converts file letter ('a' - 'h') and rank digit ('1' - '8') into zero-based column and row indices </p>
     *
     * @param coord 2-character coordinate string
     * @return corresponding {@link Position}
     */
    private static Position parsePosition(String coord) {
        logger.trace("Beginning of a notation parsing: {}", coord);
        char file = coord.charAt(0);
        char rank = coord.charAt(1);
        int col = file - 'a';
        int row = Board.SIZE - (rank - '1') - 1;

        Position pos = new Position(row, col);
        logger.trace("Result of parsing the position '{}' -> {}", coord, pos);
        return pos;
    }

    /**
     * Formats a push move into notation (e.g. "Re2>e3").
     *
     * @param mover     the moving figure
     * @param from      mover's position
     * @param victimPos victim's position
     * @param pushDest  where the victim is pushed
     * @return formatted string
     */
    public static String formatPush(Figure mover,
                                    Position from,
                                    Position victimPos,
                                    Position pushDest) {
        char pChar = mover.isGold()
                ? Character.toUpperCase(mover.getType().name().charAt(0))
                : Character.toLowerCase(mover.getType().name().charAt(0));
        char dir = direction(from, victimPos);
        return String.format("%c%s>%s",
                pChar,
                fileRankWithDir(from, dir),
                fileRank(pushDest)
        );
    }

    /**
     * Formats a pull move into notation (e.g. "Re3<e2").
     *
     * @param mover     the moving figure
     * @param from      mover's position
     * @param victimPos victim's position
     * @param pullDest  where the mover goes after pulling
     * @return formatted string
     */
    public static String formatPull(Figure mover,
                                    Position from,
                                    Position victimPos,
                                    Position pullDest) {
        char pChar = mover.isGold()
                ? Character.toUpperCase(mover.getType().name().charAt(0))
                : Character.toLowerCase(mover.getType().name().charAt(0));
        char dir = direction(from, victimPos);
        return String.format("%c%s<%s",
                pChar,
                fileRankWithDir(from, dir),
                fileRank(pullDest)
        );
    }

    /**
     * Formats a capture event: Piece + file/rank + 'x' (e.g. "Ch3x").
     *
     * @param captured the captured figure
     * @param pos      the capture position
     * @return formatted capture string
     */
    public static String formatCapture(Figure captured, Position pos) {
        char raw = captured.getType().name().charAt(0);
        char pieceChar = captured.isGold() ? Character.toUpperCase(raw) : Character.toLowerCase(raw);
        return String.format("%c%sx", pieceChar, fileRank(pos));
    }

    /**
     * Converts Position to Arimaa file/rank notation (e.g. b3)
     */
    private static String fileRank(Position pos) {
        char file = (char) ('a' + pos.getCol());
        char rank = (char) ('1' + (Board.SIZE - 1 - pos.getRow()));
        return "" + file + rank;
    }

    /**
     * Appends direction char to file/rank, e.g. "b3n"
     */
    private static String fileRankWithDir(Position pos, char dir) {
        return fileRank(pos) + dir;
    }

    /**
     * Determines direction char between two adjacent positions.
     *
     * @param a from
     * @param b to
     * @return direction char ('n','s','e','w')
     * @throws IllegalArgumentException if positions are not adjacent
     */
    private static char direction(Position a, Position b) {
        int dr = b.getRow() - a.getRow();
        int dc = b.getCol() - a.getCol();
        if (dr == -1 && dc == 0) return 'n';
        if (dr == 1 && dc == 0) return 's';
        if (dr == 0 && dc == 1) return 'e';
        if (dr == 0 && dc == -1) return 'w';
        throw new IllegalArgumentException("Positions not adjacent: " + a + " / " + b);
    }

    /**
     * Applies a direction char to a position and returns the resulting adjacent position.
     *
     * @param from source position
     * @param dir  direction char
     * @return new adjacent position
     */
    private static Position applyDir(Position from, char dir) {
        int dr = 0, dc = 0;
        switch (dir) {
            case 'n':
                dr = -1;
                break;
            case 's':
                dr = 1;
                break;
            case 'e':
                dc = 1;
                break;
            case 'w':
                dc = -1;
                break;
            default:
                throw new IllegalArgumentException("Invalid direction: " + dir);
        }
        return new Position(from.getRow() + dr, from.getCol() + dc);
    }

}
