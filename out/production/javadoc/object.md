Class Hierarchy and Structure
-----------------------------

Figure (abstract)
- Represents a game piece on the board.
- Common properties: position, owner (player), strength.
- Defines abstract methods for movement and interactions.

Subclasses of Figure:
- Rabbit – weakest piece; only moves forward.
- Cat
- Dog
- Horse
- Camel
- Elephant – strongest piece; cannot be pushed or pulled.
  Each subclass overrides interaction logic based on strength (e.g. freezing, pushing, pulling).

logic.Board
- 2D array of logic.Cell.
- Central class for managing game state and rule enforcement.
- Handles checking for traps, valid moves, freezing, and win conditions.

logic.Cell
- Represents one tile on the board.
- Stores reference to a Figure (if present) and its position.

Player
- Stores player identity (gold or silver).
- Optionally tracks time or captured figures.

Game
- Main controller class.
- Holds the board, players, current turn, and game state.
- Handles turn logic, switching players, and invoking movement.

GameTimer
- Tracks elapsed time per player.
- Used to limit thinking time or for performance statistics.

Position
- Encapsulates board coordinates (row, col).
- Used throughout for referencing positions safely.

Direction (enum)
- Represents direction of movement: UP, DOWN, LEFT, RIGHT.

MoveType (enum)
- Represents movement types: STEP, PUSH, PULL.
