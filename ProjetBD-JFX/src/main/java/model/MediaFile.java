package model;

import com.google.common.collect.ImmutableMap;
import database.OracleDB;
import gui.GuiUtils;
import gui.ValidationAlert;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import utils.ValidationError;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MediaFile extends EditableModel<MediaFile> {

    private int id;
    private int size;
    private Date addedDate;
    private String userEmail;

    private List<Flux<?>> flux;

    @Override
    public MediaFile loadFromResultSet(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt("FILE_ID");
        size = resultSet.getInt("FILE_SIZE");
        addedDate = resultSet.getDate("ADDED_DATE");
        userEmail = resultSet.getString("USER_EMAIL");
        return this;
    }

    @Override
    protected List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if (id < 0)
            errors.add(new ValidationError("ID", "Must be positive"));
        if (size < 0)
            errors.add(new ValidationError("Size", "Must be positive"));
        if (addedDate == null)
            errors.add(new ValidationError("Added date", "Must be specified"));
        if (userEmail == null)
            errors.add(new ValidationError("Added by", "User must be specified"));
        if (flux.isEmpty())
            errors.add(new ValidationError("Flux", "Add at least one flux"));
        return errors;
    }

    @Override
    public Dialog<MediaFile> getEditDialog(boolean create) {
        Dialog<MediaFile> dialog = new Dialog<>();
        dialog.setTitle(create ? "Media file creation" : "Media file edition");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TextField idField = GuiUtils.getNumericField(create ? 0 : id);
        TextField sizeField = GuiUtils.getNumericField(create ? 1 : size);
        DatePicker dateField = new DatePicker(LocalDate.now());

        ComboBox<User> userField = new ComboBox<>();
        userField.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user == null ? "Select a user..." : user.getFirstName() + " " + user.getLastName();
            }

            @Override
            public User fromString(String s) {
                return userField.getItems().stream().filter(
                        user -> (user.getFirstName() + " " + user.getLastName()).equals(s)
                ).findFirst().orElse(null);
            }
        });
        OracleDB.executeThen(
                () -> OracleDB.listUsers(userField.getItems()),
                () -> {
                    if (userEmail != null)
                        userField.setValue(userField.getItems().filtered(u -> u.getEmail().equals(userEmail)).stream().findFirst().orElse(null));
                }
        );


        Button newUserButton = new Button("Create new user");

        newUserButton.setOnAction(e -> new User().getEditDialog(true).showAndWait().ifPresent(
                user -> userField.getItems().add(user)
        ));

        GridPane grid = GuiUtils.createForm(ImmutableMap.of(
                "ID:", idField,
                "Size:", sizeField,
                "Added date:", dateField,
                "Added by:", userField
        ));

        grid.add(newUserButton, 2, 3);

        grid.add(new Separator(), 0, 4, 3, 1);
        grid.add(new Label("Flux contained in the file"), 0, 5, 3, 1);
        ListView<Flux<?>> fluxList = new ListView<>();
        grid.add(fluxList, 0, 6, 3, 1);
        flux = fluxList.getItems();
        fluxList.setPlaceholder(new Label("You need to add flux to this file"));

        Button addVideoFlux = new Button("Add video flux");
        Button addAudioFlux = new Button("Add audio flux");
        Button addTextFlux = new Button("Add text flux");

        GridPane.setHalignment(addVideoFlux, HPos.LEFT);
        GridPane.setHalignment(addAudioFlux, HPos.CENTER);
        GridPane.setHalignment(addTextFlux, HPos.RIGHT);

        addVideoFlux.setOnAction(e -> {
            VideoFlux videoFlux = (VideoFlux) new VideoFlux(
                    fluxList.getItems().stream().map(Flux::getFluxId).collect(Collectors.toList())
            ).getEditDialog(true).showAndWait().orElse(null);
            if (videoFlux != null)
                fluxList.getItems().add(videoFlux);
        });

        addAudioFlux.setOnAction(e -> {
            AudioFlux audioFlux = (AudioFlux) new AudioFlux(
                    fluxList.getItems().stream().map(Flux::getFluxId).collect(Collectors.toList())
            ).getEditDialog(true).showAndWait().orElse(null);
            if (audioFlux != null)
                fluxList.getItems().add(audioFlux);
        });

        addTextFlux.setOnAction(e -> {
            TextFlux textFlux = (TextFlux) new TextFlux(
                    fluxList.getItems().stream().map(Flux::getFluxId).collect(Collectors.toList())
            ).getEditDialog(true).showAndWait().orElse(null);
            if (textFlux != null)
                fluxList.getItems().add(textFlux);
        });

        grid.add(addVideoFlux, 0, 7);
        grid.add(addAudioFlux, 1, 7);
        grid.add(addTextFlux, 2, 7);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(idField::requestFocus);

        dialog.setResultConverter(b -> null);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            int previousId = id;
            id = Integer.parseInt(idField.getText());
            size = Integer.parseInt(sizeField.getText());
            addedDate = Date.valueOf(dateField.getValue());
            userEmail = userField.getValue() == null ? null : userField.getValue().getEmail();
            List<ValidationError> errors = validate();
            if (!errors.isEmpty()) {
                id = previousId;
                new ValidationAlert(errors).showAndWait();
                return;
            }
            OracleDB.executeThen(
                    () -> {
                        if (create) {
                            OracleDB.addMediaFile(this);
                        } else {
                            OracleDB.updateMediaFile(previousId, this);
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

    public int getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public Date getAddedDate() {
        return addedDate;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public List<Flux<?>> getFlux() {
        return flux;
    }

    public enum MediaType {
        FILM, TRACK
    }
}
