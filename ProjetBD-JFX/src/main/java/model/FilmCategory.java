package model;

import com.google.common.collect.ImmutableMap;
import database.OracleDB;
import gui.GuiUtils;
import gui.ValidationAlert;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import utils.ValidationError;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FilmCategory extends EditableModel<FilmCategory> {

    private String name;

    @Override
    public FilmCategory loadFromResultSet(ResultSet resultSet) throws SQLException {
        name = resultSet.getString("FILM_CATEGORY_NAME");
        return this;
    }

    @Override
    protected List<ValidationError> validate() {
        return name.isBlank()
                ? Collections.singletonList(new ValidationError("Name", "Cannot be blank"))
                : Collections.emptyList();
    }

    @Override
    public Dialog<FilmCategory> getEditDialog(boolean create) {
        Dialog<FilmCategory> dialog = new Dialog<>();
        dialog.setTitle(create ? "Film category creation" : "Film category edition");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TextField nameField = new TextField(create ? "" : name);
        dialog.getDialogPane().setContent(GuiUtils.createForm(ImmutableMap.of("Name:", nameField)));

        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(b -> null);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            String previousName = name;
            name = nameField.getText();
            List<ValidationError> errors = validate();
            if (!errors.isEmpty()) {
                name = previousName;
                new ValidationAlert(errors).showAndWait();
                return;
            }
            OracleDB.executeThen(
                    () -> {
                        if (create) {
                            OracleDB.addFilmCategory(this);
                        } else {
                            OracleDB.updateFilmCategory(previousName, this);
                        }
                    },
                    () -> {
                        dialog.setResult(this);
                        dialog.close();
                    }
            );
        });

        return dialog;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilmCategory that = (FilmCategory) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
