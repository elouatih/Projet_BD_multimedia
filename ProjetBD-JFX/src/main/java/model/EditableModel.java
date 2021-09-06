package model;

import javafx.scene.control.Dialog;
import utils.ValidationError;

import java.util.List;

public abstract class EditableModel<T> extends Model<T> {
    protected abstract List<ValidationError> validate();
    public abstract Dialog<T> getEditDialog(boolean create);

    public Dialog<T> getEditDialog() {
        return getEditDialog(false);
    }
}
