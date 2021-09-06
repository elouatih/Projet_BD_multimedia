package model;

import com.google.common.collect.ImmutableMap;
import database.OracleDB;
import gui.GuiUtils;
import gui.ValidationAlert;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import utils.ValidationError;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Artist extends EditableModel<Artist> {

    protected int id;
    protected String name;
    protected String pictureUrl;
    protected String mainSpecialty;
    protected Date birthDate;
    protected String biography;

    @Override
    public Artist loadFromResultSet(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt("ARTIST_ID");
        name = resultSet.getString("NAME");
        pictureUrl = resultSet.getString("PICTURE_URL");
        mainSpecialty = resultSet.getString("MAIN_SPECIALTY");
        birthDate = resultSet.getDate("BIRTH_DATE");
        biography = resultSet.getString("BIOGRAPHY");
        return this;
    }

    @Override
    protected List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if (id < 0)
            errors.add(new ValidationError("ID", "Must be positive"));
        if (name.isBlank())
            errors.add(new ValidationError("Name", "Cannot be blank"));
        if (pictureUrl.isBlank())
            errors.add(new ValidationError("Picture URL", "Cannot be blank"));
        if (mainSpecialty.isBlank())
            errors.add(new ValidationError("Main specialty", "Cannot be blank"));
        if (birthDate == null)
            errors.add(new ValidationError("Birth date", "Invalid"));
        return errors;
    }

    @Override
    public Dialog<Artist> getEditDialog(boolean create) {
        Dialog<Artist> dialog = new Dialog<>();
        dialog.setTitle(create ? "Artist creation" : "Artist edition");
        dialog.setHeaderText(create
                ? "Please fill in the details of the new artist."
                : "You can edit the details of " + name + " below."
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TextField idField = GuiUtils.getNumericField(create ? 0 : id);
        TextField nameField = new TextField(create ? "" : name);
        TextField pictureUrlField = new TextField(create ? "" : pictureUrl);
        TextField mainSpecialtyField = new TextField(create ? "" : mainSpecialty);
        DatePicker birthDateField = new DatePicker(create ? null : birthDate.toLocalDate());
        TextField biographyField = new TextField(create ? "" : biography);

        dialog.getDialogPane().setContent(GuiUtils.createForm(ImmutableMap.<String,Control>builder()
                .put("ID", idField)
                .put("Name", nameField)
                .put("Picture URL", pictureUrlField)
                .put("Main specialty", mainSpecialtyField)
                .put("Birth date", birthDateField)
                .put("Biography", biographyField)
                .build()
        ));

        Platform.runLater(idField::requestFocus);

        dialog.setResultConverter(b -> null);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            int previousId = id;
            id = Integer.parseInt(idField.getText());
            name = nameField.getText();
            pictureUrl = pictureUrlField.getText();
            mainSpecialty = mainSpecialtyField.getText();
            birthDate = Date.valueOf(birthDateField.getValue());
            biography = biographyField.getText();
            List<ValidationError> errors = validate();
            if (!errors.isEmpty()) {
                id = previousId;
                new ValidationAlert(errors).showAndWait();
                return;
            }
            OracleDB.executeThen(
                    () -> {
                        if (create) {
                            OracleDB.addArtist(this);
                        } else {
                            OracleDB.updateArtist(previousId, this);
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

    public static Dialog<Artist> getChoiceDialog() {
        Dialog<Artist> dialog = new Dialog<>();
        dialog.setTitle("Artist selection");
        dialog.setHeaderText("Please choose an artist");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        ComboBox<Artist> artistField = new ComboBox<>();
        artistField.setConverter(new StringConverter<>() {
            @Override
            public String toString(Artist artist) {
                return artist == null ? "Select an artist..." : artist.getName();
            }

            @Override
            public Artist fromString(String s) {
                return artistField.getItems().stream().filter(a -> a.getName().equals(s)).findFirst().orElseThrow();
            }
        });

        OracleDB.execute(() -> OracleDB.listArtists(artistField.getItems()));

        Button addArtist = new Button("New artist...");
        addArtist.setOnAction(e -> {
            Artist artist = new Artist().getEditDialog(true).showAndWait().orElse(null);
            if (artist != null) {
                artistField.getItems().add(artist);
                artistField.getSelectionModel().select(artist);
            }
        });

        GridPane grid = GuiUtils.createForm(ImmutableMap.of(
                "Artist", artistField
        ));
        grid.add(addArtist, 3, 0);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(artistField::requestFocus);

        dialog.setResultConverter(b -> !b.getButtonData().isCancelButton() ? artistField.getValue() : null);

        return dialog;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public String getMainSpecialty() {
        return mainSpecialty;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public String getBiography() {
        return biography;
    }

    @Override
    public String toString() {
        return "Artist: Name=" + name + " Specialty=" + mainSpecialty;
    }

    public static class Actor extends Artist {
        private final String roleName;

        public Actor(String roleName) {
            this.roleName = roleName;
        }

        public Actor(Artist artist, String roleName) {
            id = artist.id;
            name = artist.name;
            pictureUrl = artist.pictureUrl;
            mainSpecialty = artist.mainSpecialty;
            birthDate = artist.birthDate;
            biography = artist.biography;
            this.roleName = roleName;
        }

        public static Dialog<Artist> getChoiceDialog() {
            Dialog<Artist> dialog = new Dialog<>();
            dialog.setTitle("Actor selection");
            dialog.setHeaderText("Please choose an artist and a role");

            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            ComboBox<Artist> artistField = new ComboBox<>();
            artistField.setConverter(new StringConverter<>() {
                @Override
                public String toString(Artist artist) {
                    return artist == null ? "Select an artist..." : artist.getName();
                }

                @Override
                public Artist fromString(String s) {
                    return artistField.getItems().stream().filter(a -> a.getName().equals(s)).findFirst().orElseThrow();
                }
            });
            OracleDB.execute(() -> OracleDB.listArtists(artistField.getItems()));

            Button addArtist = new Button("New artist...");
            addArtist.setOnAction(e -> {
                Artist artist = new Artist().getEditDialog(true).showAndWait().orElse(null);
                if (artist != null) {
                    artistField.getItems().add(artist);
                    artistField.getSelectionModel().select(artist);
                }
            });

            TextField roleField = new TextField();

            GridPane grid = GuiUtils.createForm(ImmutableMap.of(
                    "Artist", artistField,
                    "Role", roleField
            ));
            grid.add(addArtist, 3, 0);

            dialog.getDialogPane().setContent(grid);

            Platform.runLater(artistField::requestFocus);

            dialog.setResultConverter(b -> {
                if (b.getButtonData().isCancelButton())
                    return null;
                Artist artist = artistField.getValue();
                if (artist != null && !roleField.getText().isBlank())
                    return new Actor(artist, roleField.getText());
                return null;
            });

            return dialog;
        }

        public String getRoleName() {
            return roleName;
        }

        @Override
        public String toString() {
            return "Actor: Name=" + getName() + " Role=" + roleName;
        }
    }

    public static class Musician extends Artist {
        private final String instrument;

        public Musician(Artist artist, String roleName) {
            id = artist.id;
            name = artist.name;
            pictureUrl = artist.pictureUrl;
            mainSpecialty = artist.mainSpecialty;
            birthDate = artist.birthDate;
            biography = artist.biography;
            this.instrument = roleName;
        }

        public Musician(String instrument) {
            this.instrument = instrument;
        }

        public static Dialog<Artist> getChoiceDialog() {
            Dialog<Artist> dialog = new Dialog<>();
            dialog.setTitle("Musician selection");
            dialog.setHeaderText("Please choose an artist and an instrument");

            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            ComboBox<Artist> artistField = new ComboBox<>();
            artistField.setConverter(new StringConverter<>() {
                @Override
                public String toString(Artist artist) {
                    return artist == null ? "Select an artist..." : artist.getName();
                }

                @Override
                public Artist fromString(String s) {
                    return artistField.getItems().stream().filter(a -> a.getName().equals(s)).findFirst().orElseThrow();
                }
            });
            OracleDB.execute(() -> OracleDB.listArtists(artistField.getItems()));

            Button addArtist = new Button("New artist...");
            addArtist.setOnAction(e -> {
                Artist artist = new Artist().getEditDialog(true).showAndWait().orElse(null);
                if (artist != null) {
                    artistField.getItems().add(artist);
                    artistField.getSelectionModel().select(artist);
                }
            });

            TextField roleField = new TextField();

            GridPane grid = GuiUtils.createForm(ImmutableMap.of(
                    "Artist", artistField,
                    "Instrument", roleField
            ));
            grid.add(addArtist, 3, 0);

            dialog.getDialogPane().setContent(grid);

            Platform.runLater(artistField::requestFocus);

            dialog.setResultConverter(b -> {
                if (b.getButtonData().isCancelButton())
                    return null;
                Artist artist = artistField.getValue();
                if (artist != null && !roleField.getText().isBlank())
                    return new Musician(artist, roleField.getText());
                return null;
            });

            return dialog;
        }

        public String getInstrument() {
            return instrument;
        }

        @Override
        public String toString() {
            return "Musician " + getName() + " Instrument  = " + getInstrument();
        }
    }
}
