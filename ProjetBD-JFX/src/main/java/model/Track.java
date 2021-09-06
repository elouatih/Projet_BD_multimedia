package model;

import com.google.common.collect.ImmutableMap;
import database.OracleDB;
import gui.ExceptionAlert;
import gui.GuiUtils;
import gui.ValidationAlert;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckComboBox;
import utils.ValidationError;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Track extends EditableModel<Track>{
    private int id;
    private int albumId;
    private String title;
    private int length;
    private Album album;
    private final List<MusicCategory> trackCategories = new ArrayList<>();
    private final Map<Artist, String> artistsInstruments = new HashMap<>();
    private List<Artist.Musician> artists = new ArrayList<>();

    @Override
    protected List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if (id < 0)
            errors.add(new ValidationError("ID", "Must be positive"));
        if (albumId < 0)
            errors.add(new ValidationError("ALBUM ID", "Must be positive"));
        if (length < 0)
            errors.add(new ValidationError("Length", "Must be positive"));
        if (title.isBlank())
            errors.add(new ValidationError("Title", "Cannot be blank"));
        if (trackCategories.size() == 0)
            errors.add(new ValidationError("Category", "Choose at least a category"));
        return errors;
    }

    @Override
    public Dialog<Track> getEditDialog(boolean create) {
        Dialog<Track> dialog = new Dialog<>();
        trackCategories.clear();
        if (create)
            artistsInstruments.clear();
        dialog.setTitle(create ? "Track creation" : "Track edition");
        dialog.setHeaderText(create
                ? "Please fill in the details of the new Track."
                : "You can edit the details of \"" + title + "\" below."
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TextField idField = GuiUtils.getNumericField(create ? 0 : id);
        TextField titleField = new TextField(create ? "" : title);
        TextField length = GuiUtils.getNumericField(create ? 1 : getLength());

        CheckComboBox<MusicCategory> categoryField = new CheckComboBox<>();
        categoryField.setConverter(new StringConverter<>() {
            @Override
            public String toString(MusicCategory musicCategory) {
                return musicCategory.getName();
            }

            @Override
            public MusicCategory fromString(String s) {
                return categoryField.getItems().stream().filter(fc -> fc.getName().equals(s)).findFirst().orElseThrow();
            }
        });
        OracleDB.executeThen(
                () -> OracleDB.listMusicCategories(categoryField.getItems()),
                () -> trackCategories.forEach(tc -> categoryField.getCheckModel().check(tc))
        );

        Button newCategoryButton = new Button("Create new category");

        ComboBox<Album> albumField = new ComboBox<>();
        albumField.setConverter(new StringConverter<>() {
            @Override
            public String toString(Album album) {
                return album == null ? "Select an Album..." : album.getId() + " | " + album.getTitle();
            }

            @Override
            public Album fromString(String s) {
                return albumField.getItems().stream().filter(
                        album -> (album.getId() + " " + album.getTitle()).equals(s)
                ).findFirst().orElse(null);
            }
        });
        OracleDB.executeThen(
                () -> OracleDB.listAlbum(albumField.getItems()),
                () -> albumField.setValue(albumField.getItems().filtered(u -> u.getId() == albumId).stream().findFirst().orElse(null))
        );


        Button newAlbumButton = new Button("Create new album");

        newAlbumButton.setOnAction(e -> new Album().getEditDialog(true).showAndWait().ifPresent(
                album -> albumField.getItems().add(album)
        ));
        newCategoryButton.setOnAction(e -> new MusicCategory().getEditDialog(true).showAndWait().ifPresent(
                musicCategory -> categoryField.getItems().add(musicCategory)
        ));

        GridPane grid = GuiUtils.createForm(new ImmutableMap.Builder<String, Control>()
                .put("Id", idField)
                .put("Title:", titleField)
                .put("Length:", length)
                .build()
        );

        grid.add(new Label("Category"), 0, 6);
        grid.add(categoryField, 1, 6);
        grid.add(newCategoryButton, 2, 6);
        grid.add(albumField, 1, 7);
        grid.add(newAlbumButton, 2, 7);

        grid.add(new Separator(), 0, 8, 3, 1);
        grid.add(new Label("Participating artists"), 0, 9, 3, 1);
        ListView<Artist.Musician> artistList = new ListView<>();
        grid.add(artistList, 0, 10, 3, 1);
        artistList.setPlaceholder(new Label("You can add artists to this track"));
        artistList.getItems().addAll(artists);
        Button addArtist = new Button("Add a musician");


        addArtist.setOnAction(e -> {
            Artist.Musician artist = (Artist.Musician) Artist.Musician.getChoiceDialog().showAndWait().orElse(null);
            if (artist != null) {
                if (artistsInstruments.entrySet().stream().anyMatch(en -> en.getKey().getId() == artist.getId() && en.getValue().equals(artist.getInstrument()))) {
                    new ExceptionAlert(new IllegalArgumentException("This musician is already playing this instrument in this track.")).showAndWait();
                    return;
                }
                artistList.getItems().add(artist);
            }
        });

        grid.add(addArtist, 0, 11, 3, 1);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(titleField::requestFocus);

        dialog.setResultConverter(b -> null);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            List<MusicCategory> selectedCategories = categoryField.getItems().filtered(fc -> categoryField.getCheckModel().isChecked(fc));
            String previousTitle = title;
            int previousId = this.id;
            int previousAlbumId = this.albumId;
            id = Integer.parseInt(idField.getText());
            albumId = albumField.getValue() == null ? -1 : albumField.getValue().getId();
            album = albumField.getValue();
            title = titleField.getText();
            this.length = Integer.parseInt(length.getText());
            trackCategories.clear();
            trackCategories.addAll(selectedCategories);
            artists.clear();
            artists.addAll(artistList.getItems());
            List<ValidationError> errors = validate();
            if (!errors.isEmpty()) {
                title = previousTitle;
                new ValidationAlert(errors).showAndWait();
                return;
            }
            OracleDB.executeThen(
                    () -> {
                        if (create) {
                            OracleDB.addTrack(this);
                        } else {
                            OracleDB.updateTrack(previousAlbumId, previousId, this);
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

    @Override
    public Track loadFromResultSet(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt("TRACK_ID");
        albumId = resultSet.getInt("ALBUM_ID");
        title = resultSet.getString("TRACK_TITLE");
        length = resultSet.getInt("LENGTH");
        OracleDB.listTrackCategories(this);
        OracleDB.setAlbum(this);
        OracleDB.setArtist(this);
        return this;
    }

    public int getId() {
        return id;
    }

    public int getAlbumId() {
        return albumId;
    }

    public String getTitle() {
        return title;
    }

    public int getLength() {
        return length;
    }

    public List<MusicCategory> getTrackCategories() {
        return trackCategories;
    }


    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public String getAlbumTitle() {
        return album.getTitle();
    }

    public String getCategories() {
        StringBuilder s = new StringBuilder();
        for (MusicCategory mc : trackCategories) {
            s.append(mc.getName());
            s.append(",");
        }
        if (s.length() != 0)
            s.deleteCharAt(s.length() - 1);
        return s.toString();
    }

    public Map<Artist, String> getArtistsInstruments() {
        return artistsInstruments;
    }

    public String getMusicians() {
        StringBuilder s = new StringBuilder();
        for (Artist.Musician art : artists) {
            s.append(art);

            s.append("\n");
        }
        return s.toString();
    }

    public List<Artist.Musician> getArtistsList() {
        return artists;
    }
}
