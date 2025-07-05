# Arimaa Java Game

## Overview

Arimaa is a strategic board game where two players (Gold and Silver) maneuver animal pieces aiming to achieve goal or eliminate opponent's rabbits. This Java implementation provides:

* **Classic Mode**: Turn-based play, up to 4 steps per turn.
* **Fast Mode**: Enforced time limits per turn and total game.
* **Drag-and-drop GUI** built with Swing.
* **Arimaa notation**: Save/load games in human-readable format.
* **Push/pull & trap logic**: Full support for Arimaa rules.
* **Undo, step-by-step history navigation**.* **Configurable logging** with SLF4J + Logback.
* **Comprehensive unit tests** covering core logic.

## Class Hierarchy and Structure

## Project Structure

```
semProject/
├── src/
│   ├── main/java/figures        # Figure class 
│   ├── main/java/utils          # enums and utility classes
│   ├── main/java/logic          # core game logic
│   ├── main/java/GUI            # Swing UI components
│   ├── main/resources           # logback.xml, SVG assets
│   └── test/java/logic          # JUnit 5 test suites
├── pom.xml                      # Maven build and dependencies
└── README.md                    # User guide & technical documentation
```

**Package `figures`**

* `Figure`

  * Represents any game piece (Rabbit, Cat, Dog, Horse, Camel, Elephant).
  * Properties: `FigureType type`, `boolean isGold`, `Position position`.
  * Methods: movement, `getImagePath()`, `toString()`.

**Package `utils`**

* `FigureType` (enum)

  * Enum constants: `RABBIT`, `CAT`, `DOG`, `HORSE`, `CAMEL`, `ELEPHANT`.
  * Attributes: strength, image resource path, push/pull rules.
* `GameMode` (enum)

  * Enum values: `CLASSIC`, `FAST`.

* `ActionType` (enum)

  * Enum values: `SIMPLE`, `PUSH`, `PULL`, `BOTH`.

* `StepResult`
  *  Encapsulates result of a move (type, possible destinations, notation).

**Package `logic`**

* `Board`

  * 8×8 grid of `Figure` and static trap positions.
  * Manages valid moves, freezing, trap captures.
  * Helper methods: `isFrozen(Figure)`, `applyTraps()`, `countFriends(Position)`.
* `Position`

  * Immutable row/column wrapper.
  * Overrides `equals()`, `hashCode()`, and `toString()`.
* `Player`

  * Tracks side (`isGold`) and owned figures.
  * Methods: `hasRabbit()`, `remove(Figure)`, getters.
* `GameTimer`

  * Dedicated `Thread` for tracking per-turn and total game time.
  * Listener callbacks for UI updates.
* `Notation`

  * Parses and formats move tokens (`Ra7n`, `ra2x`, `-`).
  * Converts between algebraic notation and `Position`.
* `Game`

  * Controller: holds `Board`, two `Player`, `GameTimer`, and notation history.
  * Enforces rules, push/pull interactions, trap logic, victory conditions.
  * Public API: `step()`, `stepIsLegal()`, `endTurnEarly()`, `skipStep()`, `getGroupedNotation()`, `saveHistoryToFile()`, `loadFromFile()`, `isGameOver()`.
* `GameOverException`

  * Custom unchecked exception signaling game end.

**Package `GUI`**

* `GameWindow`

  * Extends `JFrame`, sets up main window on the Event Dispatch Thread.
  * Contains menu bar, status panels, and `BoardPanel`.
* `BoardPanel`

  * Extends `JPanel`, renders board grid, coordinates, and figures.
  * Handles mouse events: drag-and-drop, undo, save/load, end turn.
* `ImageLoader`

  * Loads and caches SVG assets via svg-salamander into `BufferedImage`.

---


## Technologies Used

* **Java 22**: Modern language features and performance.
* **Maven**: Build, dependency, and lifecycle management.
* **Swing**: Lightweight Java GUI toolkit.
* **svg-salamander**: Renders SVG piece icons to `BufferedImage`.
* **SLF4J + Logback**: Configurable logging framework.
* **JUnit 5**: Unit testing core logic.


## User Manual

## Overview

**Arimaa** is a strategy board game for two players (Gold and Silver).  
This Java implementation now includes several major updates:

- **Step-by-step move system** and detailed move history
- **Move notation**: all actions are saved in standard Arimaa notation
- **Undo/Redo** support for moves and turns
- **Save/Load games** in human-readable format
- **Fast mode** with time controls for each player
- **Improved drag-and-drop GUI**
- **Comprehensive logging**
- **Full support for all Arimaa rules** (push, pull, traps, etc.)

---

## 1. Getting Started

### Running the Game

1. Build and launch the project (via your IDE or command line).
2. The **main window** will open with a menu where you can:
  - Start a new game (Classic or Fast Mode)
  - Load a saved game
  - View rules or help (if implemented)

### Game Modes

- **Classic Mode:** Standard turn-based play (up to 4 steps per turn, no time limits).
- **Fast Mode:** Each turn and total game is time-limited.
- **PvC:** Standard turn-based play, but with a computer.

---

## 2. Setup Phase

1. Both players take turns placing their pieces:
  - Gold places 16 pieces on the first two rows.
  - Silver places 16 pieces on the last two rows. (In PvC mode it's randomized)
2. The setup phase ensures correct piece count per type.
3. Once setup is complete, the main game begins.

---

## 3. Making Moves

- **Drag and Drop:**  
  Click and drag a piece to an adjacent square to perform a move.
- **Steps:**  
  Each turn consists of up to 4 steps. All moves are recorded in Arimaa notation (e.g., `Ra3n` for a Rabbit moving north from a3).
- **Push and Pull:**  
  You can push or pull opposing pieces, following Arimaa rules. Just drag a piece to the target piece and you will see the options of push/pull movement.

---

## 4. Undo 

- **Undo:**  
  Press the **Undo** button to revert the most recent turn.
---

## 5. Saving and Loading Games

- **Save Game:**  
  Use the **Save** button to export the current move history and board state to a file (in Arimaa notation).
- **Load Game:**  
  Use the **Load** button to restore a game from a saved file. The board and move history will update accordingly.

---

## 6. Game Notation

- **Standard Arimaa Notation:**  
  All moves are saved and loaded in standard Arimaa move notation (e.g., `Ra3n`).

---

## 7. Game Timer (Fast Mode)

- **Turn Timer:**  
  In Fast Mode, each player has a set time per turn. Remaining time is displayed in the interface.
- **Game Over:**  
  If a player runs out of time, they automatically lose the game.

---

## 8. Ending the Game

- The game ends when one player achieves the goal (moves a Rabbit to the last row), all opponent's Rabbits are captured, or by time out (in Fast Mode).
- An **End Game** dialog will display the winner and key stats.

---

## 9. Additional Features

- **Comprehensive Logging:**  
  All moves, game states, and errors are logged for debugging and analysis.
- **Resizable Board:**  
  The board adjusts to the window size for improved usability.

---

## 10. Troubleshooting

- If the game crashes or misbehaves:
  - Check the log file for details.
  - Make sure you are running the latest Java version (>= 21).
  - For GUI issues, try resizing the window or restarting the application.
- For bug reports, provide the log output and a description of your actions.

---


Enjoy your Arimaa game!
