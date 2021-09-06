package gui;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionAlert extends Alert {

    public ExceptionAlert(Exception exception) {
        super(AlertType.ERROR);
        setTitle("An exception occurred!");
        setHeaderText("An exception occurred!");
        setContentText(exception.getMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);

        Label label = new Label("The exception stacktrace was:");
        TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(false);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane stacktraceDetails = new GridPane();
        stacktraceDetails.setMaxWidth(Double.MAX_VALUE);
        stacktraceDetails.add(label, 0, 0);
        stacktraceDetails.add(textArea, 0, 1);

        getDialogPane().setExpandableContent(stacktraceDetails);
    }
}
