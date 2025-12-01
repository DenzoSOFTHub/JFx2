package it.denzosoft.jfx2.ui.command;

/**
 * Command interface for undo/redo operations.
 * Implements the Command pattern for undoable actions.
 */
public interface Command {

    /**
     * Execute the command.
     */
    void execute();

    /**
     * Undo the command.
     */
    void undo();

    /**
     * Get a description of the command for display.
     */
    String getDescription();

    /**
     * Check if this command can be merged with another.
     * Used for coalescing rapid parameter changes.
     */
    default boolean canMergeWith(Command other) {
        return false;
    }

    /**
     * Merge this command with another.
     * Returns a new merged command or null if merge not possible.
     */
    default Command mergeWith(Command other) {
        return null;
    }
}
