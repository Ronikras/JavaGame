package utils;

/**
 * Enum representing the type of action required after a game step in Arimaa.
 * <ul>
 *     <li>{@link #SIMPLE} – Move is complete, no further input needed</li>
 *     <li>{@link #PUSH} – A push action is possible and the user must select a push destination</li>
 *     <li>{@link #PULL} – A pull action is possible and the user must select a pull destination</li>
 *     <li>{@link #BOTH} – Both push and pull actions are possible; user must choose which to perform</li>
 * </ul>
 */
public enum ActionType {
    SIMPLE, PUSH, PULL, BOTH;
}

