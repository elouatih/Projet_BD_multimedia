package gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GuiUtils {
    public static ProgressIndicator getProgressIndicator(int width, int height) {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxWidth(width);
        progressIndicator.setMaxHeight(height);
        return progressIndicator;
    }

    public static GridPane createForm(Map<String, Control> fields) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        AtomicInteger row = new AtomicInteger(0);
        fields.forEach((label, field) -> {
            grid.add(new Label(label), 0, row.get());
            grid.add(field, 1, row.getAndIncrement());
        });

        return grid;
    }

    public static TextField getNumericField(int value) {
        TextField numericField = new TextField(String.valueOf(value));
        numericField.textProperty().addListener(((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d+")) {
                numericField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        }));
        return numericField;
    }
}
