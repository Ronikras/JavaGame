package utils;

import logic.Position;

import java.util.List;

/**
 * Encapsulates the result of attempting a step (move) in Arimaa.
 * <p>
 * Used to communicate whether a move is simple, requires further action (push/pull), and/or produces move notation.
 * </p>
 */
public class StepResult {
    public final ActionType type;
    public final List<Position> options;
    public final List<String> notation;

    /**
     * Constructs a step result.
     *
     * @param t    ActionType for this step
     * @param opts List of possible positions (for push/pull options)
     * @param n    List of notation tokens produced by this step
     */
    private StepResult(ActionType t, List<Position> opts, List<String> n) {
        this.type = t;
        this.options = opts;
        this.notation = n;
    }

    /**
     * Creates a StepResult for a simple (completed) move.
     *
     * @param note List of notation tokens (usually one step/capture)
     * @return StepResult representing a simple action
     */
    public static StepResult simple(List<String> note) {
        return new StepResult(ActionType.SIMPLE, List.of(), note);
    }

    /**
     * Creates a StepResult indicating that a push or pull is required, with possible target positions.
     *
     * @param t    PUSH, PULL, or BOTH (from {@link ActionType})
     * @param opts List of possible positions for the push or pull
     * @return StepResult representing the choice phase
     */
    public static StepResult pushOrPull(ActionType t, List<Position> opts) {
        return new StepResult(t, opts, List.of());
    }
}
