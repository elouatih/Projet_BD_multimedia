package gui;

import com.google.common.collect.ImmutableMap;
import database.OracleDB;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OracleConnectDialog extends Dialog<Connection> {

    private Connection result = null;

    public OracleConnectDialog() {
        setTitle("Oracle Database Connection");
        setHeaderText("Please connect to the Oracle database to use.");
        setGraphic(new ImageView(OracleDB.class.getResource("/oracle-64.png").toString()));

        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        ButtonType quitButtonType = new ButtonType("Quit", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(connectButtonType, quitButtonType);

        TextField address = new TextField("oracle1.ensimag.fr");
        address.setPromptText("Oracle server address");
        address.setAccessibleHelp("Address format: host:port");
        TextField database = new TextField("oracle1");
        database.setPromptText("Oracle System ID");
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        GridPane grid = GuiUtils.createForm(ImmutableMap.of(
                "Address:", address,
                "SID:", database,
                "Username:", username,
                "Password:", password
        ));

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        grid.add(progressIndicator, 2, 0, 1, 4);

        Button connectButton = (Button) getDialogPane().lookupButton(connectButtonType);
        connectButton.setDisable(true);

        ChangeListener<String> listener = (observable, oldValue, newValue) -> {
            boolean valid = !address.getText().trim().isEmpty()
                    && !database.getText().trim().isEmpty()
                    && !username.getText().trim().isEmpty()
                    && !password.getText().trim().isEmpty();
            if (valid && !address.getText().contains(":"))
                address.setText(address.getText() + ":1521"); // default port
            connectButton.setDisable(!valid);
        };

        address.textProperty().addListener(listener);
        database.textProperty().addListener(listener);
        username.textProperty().addListener(listener);
        password.textProperty().addListener(listener);

        getDialogPane().setContent(grid);

        Platform.runLater(username::requestFocus);

        connectButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (result == null) {
                event.consume();
                connectButton.setDisable(true);
                progressIndicator.setVisible(true);
                OracleDB.getExecutor().submit(() -> {
                    try {
                        result = DriverManager.getConnection("jdbc:oracle:thin:@" + address.getText() + ":" + database.getText(), username.getText(), password.getText());
                        Platform.runLater(this::close);
                    } catch (SQLException throwables) {
                        // display exception
                        Platform.runLater(() -> new ExceptionAlert(throwables).showAndWait());
                    }
                    progressIndicator.setVisible(false);
                    connectButton.setDisable(false);
                });
            }
        });

        setResultConverter(buttonType -> result);
    }
}
