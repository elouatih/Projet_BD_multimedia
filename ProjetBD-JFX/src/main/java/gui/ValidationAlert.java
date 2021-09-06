package gui;

import javafx.scene.control.Alert;
import utils.ValidationError;

import java.util.List;
import java.util.stream.Collectors;

public class ValidationAlert extends Alert {

    public ValidationAlert(List<ValidationError> errors) {
        super(AlertType.ERROR);
        setTitle("Validation error!");
        setHeaderText("Please fix these errors before submitting:");
        setContentText(errors.stream().map(ValidationError::toString).collect(Collectors.joining("\n")));
    }
}
