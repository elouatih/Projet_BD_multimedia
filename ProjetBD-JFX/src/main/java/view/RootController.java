package view;

import database.OracleDB;
import gui.ExceptionAlert;
import gui.GuiUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;
import javafx.util.StringConverter;
import model.*;
import org.controlsfx.control.CheckComboBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class RootController extends view.BaseController {

    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<Film> films = FXCollections.observableArrayList();
    private final ObservableList<Film> films1 = FXCollections.observableArrayList();
    private final ObservableList<Track> tracks = FXCollections.observableArrayList();
    private final ObservableList<Track> tracks1 = FXCollections.observableArrayList();

    @FXML
    private TableView<User> usersListView;

    @FXML
    private TableView<Film> filmsListView;

    @FXML
    private TableView<Track> tracksListView;

    @FXML
    private TableView<Film> films1ListView;

    @FXML
    private TableView<Track> tracks1ListView;


    @FXML
    private TabPane tabPane;

    @FXML
    private GridPane actionsGridPane;

    private final Separator separator1 = new Separator();
    private final Separator separator2 = new Separator();

    // region Users
    private final Button addUser = new Button("Add user");

    private final Label selectedUserLabel = new Label();

    private final Button editUser = new Button("Edit user");
    private final Button deleteUser = new Button("Delete user");
    // endregion

    // region Medias
    private final Button addNewFile = new Button("Add file as new media");
    private final Button editMedia = new Button("Edit media");

    // region Films
    private final Label selectedFilmLabel = new Label();
    private final Button deleteFilm = new Button("Delete film");
    private final Label filmAbstractLabel = new Label();
    // endregion

    // region filmUser
    private ComboBox<User> userField = new ComboBox<>();
    private ComboBox<FilmCategory> categoryField = new ComboBox<>();
    private Button apply = new Button("Apply");
    // endregion

    // region TrackUser
    private ComboBox<User> userFieldTrack = new ComboBox<>();
    private ComboBox<MusicCategory> categoryFieldTrack = new ComboBox<>();
    private Button applyTrack = new Button("Apply");
    // endregion

    // region Tracks
    private final Label selectedTrackLabel = new Label();
    private final Button deleteTrack = new Button("Delete track");
    // endregion
    // endregion

    public RootController() {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usersListView.getColumns().forEach(column ->
                column.setCellValueFactory(new PropertyValueFactory<>(column.getId()))
        );
        usersListView.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue == null) {
                actionsGridPane.getChildren().remove(selectedUserLabel);
                actionsGridPane.getChildren().remove(editUser);
                actionsGridPane.getChildren().remove(deleteUser);
            } else {
                selectedUserLabel.setText(newValue.getFirstName() + " " + newValue.getLastName());
                if (oldValue == null) {
                    actionsGridPane.add(selectedUserLabel, 0, 2);
                    actionsGridPane.add(editUser, 0, 3);
                    actionsGridPane.add(deleteUser, 0, 4);
                }
            }
        }));

        filmsListView.getColumns().forEach(column ->
                column.setCellValueFactory(new PropertyValueFactory<>(column.getId()))
        );
        filmsListView.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue == null) {
                actionsGridPane.getChildren().remove(selectedFilmLabel);
                actionsGridPane.getChildren().remove(separator1);
                actionsGridPane.getChildren().remove(filmAbstractLabel);
                actionsGridPane.getChildren().remove(separator2);
                actionsGridPane.getChildren().remove(editMedia);
                actionsGridPane.getChildren().remove(deleteFilm);
            } else {
                selectedFilmLabel.setText(newValue.getTitle() + " (" + newValue.getYear() + ")");
                filmAbstractLabel.setText(newValue.getAbstract());
                if (oldValue == null) {
                    int row = 2;
                    actionsGridPane.add(selectedFilmLabel, 0, row++);
                    actionsGridPane.add(separator1, 0, row++);
                    actionsGridPane.add(filmAbstractLabel, 0, row++);
                    actionsGridPane.add(separator2, 0, row++);
                    actionsGridPane.add(editMedia, 0, row++);
                    actionsGridPane.add(deleteFilm, 0, row);
                }
            }
        }));

        films1ListView.getColumns().forEach(column ->
                column.setCellValueFactory(new PropertyValueFactory<>(column.getId()))
        );
        userField.prefWidthProperty().bind(actionsGridPane.widthProperty());
        categoryField.prefWidthProperty().bind(actionsGridPane.widthProperty());
        apply.prefWidthProperty().bind(actionsGridPane.widthProperty());

        apply.setOnAction(e -> {
            try {
                loadFilmsUserSpecified();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
        
        tracks1ListView.getColumns().forEach(column ->
                column.setCellValueFactory(new PropertyValueFactory<>(column.getId()))
        );
        userFieldTrack.prefWidthProperty().bind(actionsGridPane.widthProperty());
        categoryFieldTrack.prefWidthProperty().bind(actionsGridPane.widthProperty());
        applyTrack.prefWidthProperty().bind(actionsGridPane.widthProperty());

        applyTrack.setOnAction(e -> {
            loadTrackUserSpecified();
        });
       
        

        tracksListView.getColumns().forEach(column ->
                column.setCellValueFactory(new PropertyValueFactory<>(column.getId()))
        );
        tracksListView.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue == null) {
                actionsGridPane.getChildren().remove(selectedTrackLabel);
                actionsGridPane.getChildren().remove(separator1);
                actionsGridPane.getChildren().remove(editMedia);
                actionsGridPane.getChildren().remove(deleteTrack);
            } else {
                selectedTrackLabel.setText(newValue.getTitle());
                if (oldValue == null) {
                    int row = 2;
                    actionsGridPane.add(selectedTrackLabel, 0, row++);
                    actionsGridPane.add(separator1, 0, row++);
                    actionsGridPane.add(editMedia, 0, row++);
                    actionsGridPane.add(deleteTrack, 0, row);
                }
            }
        }));
        
        

        addUser.prefWidthProperty().bind(actionsGridPane.widthProperty());
        selectedUserLabel.prefWidthProperty().bind(actionsGridPane.widthProperty());
        editUser.prefWidthProperty().bind(actionsGridPane.widthProperty());
        deleteUser.prefWidthProperty().bind(actionsGridPane.widthProperty());

        addUser.setOnAction(e -> {
            if (new User().getEditDialog(true).showAndWait().orElse(null) != null) {
                try {
                    OracleDB.commit();
                    loadUsers();
                } catch (SQLException throwables) {
                    new ExceptionAlert(throwables).showAndWait();
                }
            }
        });
        editUser.setOnAction(e -> {
            User user = usersListView.getSelectionModel().getSelectedItem();
            if (user != null && user.getEditDialog().showAndWait().orElse(null) != null) {
                try {
                    OracleDB.commit();
                    loadUsers();
                } catch (SQLException throwables) {
                    new ExceptionAlert(throwables).showAndWait();
                }
            }
        });
        deleteUser.setOnAction(e -> {
            User user = usersListView.getSelectionModel().getSelectedItem();
            if (user != null) {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("User suppression confirmation");
                alert.setHeaderText("User suppression confirmation");
                alert.setContentText(
                        "Are you sure you want to delete the user " + user.getFirstName() + " " + user.getLastName() + "?\n" +
                        "This action is irreversible!"
                );
                if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    OracleDB.executeThen(
                            () -> {
                                OracleDB.deleteUser(user);
                                OracleDB.commit();
                            },
                            this::loadUsers
                    );
                }
            }
        });

        addNewFile.prefWidthProperty().bind(actionsGridPane.widthProperty());
        selectedFilmLabel.prefWidthProperty().bind(actionsGridPane.widthProperty());
        selectedTrackLabel.prefWidthProperty().bind(actionsGridPane.widthProperty());
        editMedia.prefWidthProperty().bind(actionsGridPane.widthProperty());
        deleteFilm.prefWidthProperty().bind(actionsGridPane.widthProperty());
        deleteTrack.prefWidthProperty().bind(actionsGridPane.widthProperty());

        addNewFile.setOnAction(e -> {
            MediaFile.MediaType mediaType = tabPane.getSelectionModel().getSelectedItem().getId().equals("films")
                    ? MediaFile.MediaType.FILM : MediaFile.MediaType.TRACK;
            MediaFile file = new MediaFile().getEditDialog(true).showAndWait().orElse(null);
            if (file != null) {
                try {
                    if (mediaType == MediaFile.MediaType.FILM) {
                        Film film = new Film().getEditDialog(true).showAndWait().orElse(null);
                        if (film == null) {
                            OracleDB.rollback();
                        } else {
                            OracleDB.executeThen(
                                    () -> OracleDB.addFileToFilm(file, film),
                                    () -> {
                                        try {
                                            OracleDB.commit();
                                            loadFilms();
                                        } catch (SQLException throwables) {
                                            new ExceptionAlert(throwables).showAndWait();
                                        }
                                    },
                                    error -> {
                                        new ExceptionAlert(error).showAndWait();
                                        try {
                                            OracleDB.rollback();
                                        } catch (SQLException throwables) {
                                            new ExceptionAlert(throwables).showAndWait();
                                        }
                                    }
                            );
                        }
                    } else {
                        Track track = new Track().getEditDialog(true).showAndWait().orElse(null);
                        if (track == null) {
                            OracleDB.rollback();
                        } else {
                            OracleDB.executeThen(
                                    () -> OracleDB.addFileToTrack(file, track),
                                    () -> {
                                        try {
                                            OracleDB.commit();
                                            loadTracks();
                                        } catch (SQLException throwables) {
                                            new ExceptionAlert(throwables).showAndWait();
                                        }
                                    },
                                    error -> {
                                        new ExceptionAlert(error).showAndWait();
                                        try {
                                            OracleDB.rollback();
                                        } catch (SQLException throwables) {
                                            new ExceptionAlert(throwables).showAndWait();
                                        }
                                    }
                            );
                        }
                    }
                } catch (SQLException throwables) {
                    new ExceptionAlert(throwables).showAndWait();
                }
            }
        });

        editMedia.setOnAction(e -> {
            if (tabPane.getSelectionModel().getSelectedItem().getId().equals("films")) {
                Film film = filmsListView.getSelectionModel().getSelectedItem();
                if (film != null && film.getEditDialog().showAndWait().orElse(null) != null) {
                    try {
                        OracleDB.commit();
                        loadFilms();
                    } catch (SQLException throwables) {
                        new ExceptionAlert(throwables).showAndWait();
                    }
                }
            } else {
                Track track = tracksListView.getSelectionModel().getSelectedItem();
                if (track != null && track.getEditDialog().showAndWait().orElse(null) != null) {
                    try {
                        OracleDB.commit();
                        loadTracks();
                    } catch (SQLException throwables) {
                        new ExceptionAlert(throwables).showAndWait();
                    }
                }
            }
        });

        deleteFilm.setOnAction(e -> {
            Film film = filmsListView.getSelectionModel().getSelectedItem();
            if (film != null) {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Film suppression confirmation");
                alert.setHeaderText("Film suppression confirmation");
                alert.setContentText(
                        "Are you sure you want to delete \"" + film.getTitle() + "\" (" + film.getYear() + ")?\n" +
                                "This will also delete all the associated files.\n" +
                                "This action is irreversible!"
                );
                if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    OracleDB.executeThen(
                            () -> {
                                OracleDB.deleteFilm(film);
                                OracleDB.commit();
                            },
                            this::loadFilms
                    );
                }
            }
        });

        deleteTrack.setOnAction(e -> {
            Track track = tracksListView.getSelectionModel().getSelectedItem();
            if (track != null) {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Track suppression confirmation");
                alert.setHeaderText("Track suppression confirmation");
                alert.setContentText(
                        "Are you sure you want to delete \"" + track.getTitle() + "?\n" +
                                "This will also delete all the associated files.\n" +
                                "This action is irreversible!"
                );
                if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    OracleDB.executeThen(
                            () -> {
                                OracleDB.deleteTrack(track);
                                OracleDB.commit();
                            },
                            this::loadTracks
                    );
                }
            }
        });

        onSelectUsers();
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            switch (newValue.getId()) {
                case "users": onSelectUsers(); break;
                case "films": onSelectFilms(); break;
                case "tracks": onSelectTracks(); break;
                case "tracks1": onSelectTracksUser(); break;
                case "films1" : onSelectFilmsUser(); break;
                default: new ExceptionAlert(new IllegalStateException("You selected a pane that doesn't exist. Error in the GUI code?")).showAndWait(); break;
            }
        });
    }

    private void onSelectTracksUser() {
        clearActionButtons();
        clearList();
        actionsGridPane.add(userFieldTrack, 0, 0);
        actionsGridPane.add(categoryFieldTrack, 0, 1);
        actionsGridPane.add(applyTrack, 0, 2);
        loadTracksUser();
    }

    private void loadTracksUser() {
        tracks1.clear();
        tracks1ListView.getSelectionModel().clearSelection();
        tracks1ListView.setPlaceholder(GuiUtils.getProgressIndicator(50, 50));
        tracks1ListView.setItems(tracks1);
        OracleDB.executeThen(
                () -> OracleDB.listTracks(tracks1),
                () -> onLoad(tracks1ListView, tracks1, "track"),
                error -> onLoadError(tracks1ListView, error)
        );
        userFieldTrack.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user == null ? "Select a user..." : user.getFirstName() + " " + user.getLastName();
            }

            @Override
            public User fromString(String s) {
                return userFieldTrack.getItems().stream().filter(
                        user -> (user.getFirstName() + " " + user.getLastName()).equals(s)
                ).findFirst().orElse(null);
            }
        });
        OracleDB.executeThen(
                () -> OracleDB.listUsers(userFieldTrack.getItems()),
                () -> userFieldTrack.setValue(userFieldTrack.getItems().stream().findFirst().orElse(null))
        );

        categoryFieldTrack.setConverter(new StringConverter<>() {
            @Override
            public String toString(MusicCategory trackCategory) {
                return trackCategory == null ? "Select a category " : trackCategory.getName();
            }

            @Override
            public MusicCategory fromString(String s) {
                return categoryFieldTrack.getItems().stream().filter(fc -> fc.getName().equals(s)).findFirst().orElseThrow();
            }
        });
        OracleDB.executeThen(
                () -> OracleDB.listMusicCategories(categoryFieldTrack.getItems()),
                () -> categoryFieldTrack.setValue(categoryFieldTrack.getItems().stream().findFirst().orElse(null))
        );
    }

    private void loadTrackUserSpecified() {

        tracks1.clear();
        tracks1ListView.getSelectionModel().clearSelection();
        tracks1ListView.setPlaceholder(GuiUtils.getProgressIndicator(50, 50));
        tracks1ListView.setItems(tracks1);

        if (categoryFieldTrack.getValue() != null && userFieldTrack.getValue() != null) {
            Pair<String, String> data = new Pair<>(categoryFieldTrack.getValue().getName(), userFieldTrack.getValue().getEmail());
            OracleDB.executeThen(
                    () -> OracleDB.listTracksUser(tracks1, data),
                    () -> onLoad(tracks1ListView, tracks1, "trackUser"),
                    error -> onLoadError(tracks1ListView, error)
            );
        }

    }

    private void loadFilmsUserSpecified() throws SQLException {
        films1.clear();
        films1ListView.getSelectionModel().clearSelection();
        films1ListView.setPlaceholder(GuiUtils.getProgressIndicator(50, 50));
        films1ListView.setItems(films1);
        if (categoryField.getValue() != null && userField.getValue() != null) {
            Pair<String, String> data = new Pair<>(categoryField.getValue().getName(), userField.getValue().getEmail());

            OracleDB.executeThen(
                    () -> OracleDB.listFilmsUser(films1, data),
                    () -> onLoad(films1ListView, films1, "filmUser"),
                    error -> onLoadError(films1ListView, error)
            );
        }

    }

    private void clearList() {
        userFieldTrack.getItems().clear();
        categoryFieldTrack.getItems().clear();
        userField.getItems().clear();
        categoryField.getItems().clear();
    }

    private void clearActionButtons() {
        actionsGridPane.getChildren().clear();

    }

    private void onSelectFilmsUser() {
        clearActionButtons();
        actionsGridPane.add(userField, 0, 0);
        actionsGridPane.add(categoryField, 0, 1);
        actionsGridPane.add(apply, 0, 2);
        clearList();
        loadFilmsUser();
    }

    private void loadFilmsUser() {
        clearList();
        films1.clear();
        films1ListView.getSelectionModel().clearSelection();
        films1ListView.setPlaceholder(GuiUtils.getProgressIndicator(50, 50));
        films1ListView.setItems(films1);
        OracleDB.executeThen(
                () -> OracleDB.listFilms(films1),
                () -> onLoad(films1ListView, films1, "film"),
                error -> onLoadError(films1ListView, error)
        );
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
                () -> userField.setValue(userField.getItems().stream().findFirst().orElse(null))
        );

        categoryField.setConverter(new StringConverter<>() {
            @Override
            public String toString(FilmCategory filmCategory) {
                return filmCategory == null ? "Select a category " : filmCategory.getName();
            }

            @Override
            public FilmCategory fromString(String s) {
                return categoryField.getItems().stream().filter(fc -> fc.getName().equals(s)).findFirst().orElseThrow();
            }
        });
        OracleDB.executeThen(
                () -> OracleDB.listFilmCategories(categoryField.getItems()),
                () -> categoryField.setValue(categoryField.getItems().stream().findFirst().orElse(null))
        );
    }

    private void onSelectUsers() {
        clearActionButtons();
        actionsGridPane.add(addUser, 0, 0);
        loadUsers();

    }

    private void onSelectFilms() {
        clearActionButtons();
        actionsGridPane.add(addNewFile, 0, 0);
        loadFilms();
    }

    private void onSelectTracks() {
        clearActionButtons();
        actionsGridPane.add(addNewFile,0, 0);
        loadTracks();
    }

    private void onLoad(TableView<? extends EditableModel<?>> tableView, ObservableList<? extends EditableModel<?>> list, String objectName) {
        if (list.isEmpty()) {
            Label emptyLabel = new Label("There are currently no " + objectName + " in the system.");
            emptyLabel.setAlignment(Pos.BASELINE_CENTER);
            emptyLabel.setTextAlignment(TextAlignment.CENTER);
            emptyLabel.setMaxWidth(tableView.getWidth() * 0.8D);
            tableView.setPlaceholder(emptyLabel);
        }
    }

    private void onLoadError(TableView<? extends EditableModel<?>> tableView, Exception error) {
        tableView.setItems(null);
        Label errorLabel = new Label("An error occurred:\n" + error.getClass().getName() + ": " + error.getMessage());
        errorLabel.setTextFill(Color.RED);
        errorLabel.setAlignment(Pos.BASELINE_CENTER);
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(usersListView.getWidth() * 0.8D);
        tableView.setPlaceholder(errorLabel);
    }

    private void loadUsers() {
        users.clear();
        usersListView.getSelectionModel().clearSelection();
        usersListView.setPlaceholder(GuiUtils.getProgressIndicator(50, 50));
        usersListView.setItems(users);
        OracleDB.executeThen(
                () -> OracleDB.listUsers(users),
                () -> onLoad(usersListView, users, "user"),
                error -> onLoadError(usersListView, error)
        );
    }

    private void loadFilms() {
        films.clear();
        filmsListView.getSelectionModel().clearSelection();
        filmsListView.setPlaceholder(GuiUtils.getProgressIndicator(50, 50));
        filmsListView.setItems(films);
        OracleDB.executeThen(
                () -> OracleDB.listFilms(films),
                () -> onLoad(filmsListView, films, "film"),
                error -> onLoadError(filmsListView, error)
        );
    }
     private void loadTracks() {
        tracks.clear();
        tracksListView.getSelectionModel().clearSelection();
        tracksListView.setPlaceholder(GuiUtils.getProgressIndicator(50, 50));
        tracksListView.setItems(tracks);
        OracleDB.executeThen(
                () -> OracleDB.listTracks(tracks),
                () -> onLoad(tracksListView, tracks, "track"),
                error -> onLoadError(tracksListView, error)
        );
     }


}
