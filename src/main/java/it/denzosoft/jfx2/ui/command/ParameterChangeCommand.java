package it.denzosoft.jfx2.ui.command;

import it.denzosoft.jfx2.effects.Parameter;

/**
 * Command for changing a parameter value.
 * Supports coalescing of rapid changes.
 */
public class ParameterChangeCommand implements Command {

    private final Parameter parameter;
    private final float oldValue;
    private float newValue;

    public ParameterChangeCommand(Parameter parameter, float oldValue, float newValue) {
        this.parameter = parameter;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public void execute() {
        parameter.setValue(newValue);
    }

    @Override
    public void undo() {
        parameter.setValue(oldValue);
    }

    @Override
    public String getDescription() {
        return "Change " + parameter.getName();
    }

    @Override
    public boolean canMergeWith(Command other) {
        if (other instanceof ParameterChangeCommand otherCmd) {
            return otherCmd.parameter == this.parameter;
        }
        return false;
    }

    @Override
    public Command mergeWith(Command other) {
        if (other instanceof ParameterChangeCommand otherCmd) {
            if (otherCmd.parameter == this.parameter) {
                return new ParameterChangeCommand(parameter, this.oldValue, otherCmd.newValue);
            }
        }
        return null;
    }
}
