package it.denzosoft.jfx2.ui.command;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages command history for undo/redo operations.
 * Maintains a stack of commands with configurable maximum size.
 */
public class CommandHistory {

    private static final int DEFAULT_MAX_SIZE = 50;

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final int maxSize;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    // For coalescing rapid changes
    private Command pendingCommand;
    private long lastCommandTime;
    private static final long COALESCE_WINDOW_MS = 500;

    public CommandHistory() {
        this(DEFAULT_MAX_SIZE);
    }

    public CommandHistory(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Execute a command and add it to the history.
     */
    public void execute(Command command) {
        execute(command, true);
    }

    /**
     * Execute a command with optional coalescing.
     */
    public void execute(Command command, boolean coalesce) {
        // Try to coalesce with pending command
        if (coalesce && pendingCommand != null) {
            long now = System.currentTimeMillis();
            if (now - lastCommandTime < COALESCE_WINDOW_MS && pendingCommand.canMergeWith(command)) {
                Command merged = pendingCommand.mergeWith(command);
                if (merged != null) {
                    pendingCommand = merged;
                    lastCommandTime = now;
                    merged.execute();
                    return;
                }
            }
            // Commit pending command before adding new one
            commitPending();
        }

        // Execute the command
        command.execute();

        // Store as pending for potential coalescing
        if (coalesce && command.canMergeWith(command)) {
            pendingCommand = command;
            lastCommandTime = System.currentTimeMillis();
        } else {
            addToUndoStack(command);
        }

        // Clear redo stack
        redoStack.clear();

        firePropertyChange();
    }

    /**
     * Commit any pending coalesced command.
     */
    public void commitPending() {
        if (pendingCommand != null) {
            addToUndoStack(pendingCommand);
            pendingCommand = null;
        }
    }

    /**
     * Add command to undo stack, respecting max size.
     */
    private void addToUndoStack(Command command) {
        undoStack.push(command);

        // Trim if over max size
        while (undoStack.size() > maxSize) {
            undoStack.removeLast();
        }
    }

    /**
     * Undo the last command.
     */
    public void undo() {
        commitPending();

        if (undoStack.isEmpty()) return;

        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);

        firePropertyChange();
    }

    /**
     * Redo the last undone command.
     */
    public void redo() {
        commitPending();

        if (redoStack.isEmpty()) return;

        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);

        firePropertyChange();
    }

    /**
     * Check if undo is available.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty() || pendingCommand != null;
    }

    /**
     * Check if redo is available.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Get description of command to undo.
     */
    public String getUndoDescription() {
        if (pendingCommand != null) {
            return pendingCommand.getDescription();
        }
        if (undoStack.isEmpty()) return null;
        return undoStack.peek().getDescription();
    }

    /**
     * Get description of command to redo.
     */
    public String getRedoDescription() {
        if (redoStack.isEmpty()) return null;
        return redoStack.peek().getDescription();
    }

    /**
     * Clear all history.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        pendingCommand = null;
        firePropertyChange();
    }

    /**
     * Get the number of undoable commands.
     */
    public int getUndoCount() {
        return undoStack.size() + (pendingCommand != null ? 1 : 0);
    }

    /**
     * Get the number of redoable commands.
     */
    public int getRedoCount() {
        return redoStack.size();
    }

    // ==================== PROPERTY CHANGE SUPPORT ====================

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private void firePropertyChange() {
        propertyChangeSupport.firePropertyChange("historyChanged", null, this);
    }
}
