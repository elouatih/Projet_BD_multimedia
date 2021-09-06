package model;

import com.google.common.collect.ImmutableMap;
import database.OracleDB;
import gui.GuiUtils;
import gui.ValidationAlert;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import utils.ValidationError;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Album extends EditableModel<Album> {
    private int id;
    private String title;
    private Date releaseDate;
    private String coverUrl;
    private String artistName;
    private List<AlbumCategory> albumCatgeories = new ArrayList<>();


    @Override
    public Album loadFromResultSet(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt("ALBUM_ID");
        title = resultSet.getString("ALBUM_TITLE");
        releaseDate = resultSet.getDate("RELEASE_DATE");
        coverUrl = resultSet.getString("COVER_URL");
        artistName = resultSet.getString("ARTIST_NAME");
        OracleDB.listAlbumCategory(this);

        return this;
    }

    @Override
    protected List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if (id < 0)
            errors.add(new ValidationError("ID", "Must be positive"));
        if (title.isBlank())
            errors.add(new ValidationError("Title", "Cannot be blank"));
        if (coverUrl.isBlank())
            errors.add(new ValidationError("Cover URL", "Cannot be blank"));
        if (artistName.isBlank())
            errors.add(new ValidationError("Artiste name", "Cannot be blank"));
        return errors;
    }

    @Override
    public Dialog<Album> getEditDialog(boolean create) {
        Dialog<Album> dialog = new Dialog<>();
        dialog.setTitle(create ? "Album creation" : "Album edition");
        dialog.setHeaderText(create
                ? "Please fill in the details of the new Album."
                : "You can edit the details of " + id + " | " + title + " below."
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TextField albumId = GuiUtils.getNumericField(create ? 0 : id);
        TextField albumTitle = new TextField(create ? "" : title);
        DatePicker releaseAlbum = new DatePicker(LocalDate.now());
        TextField coverAlbum = new TextField(create ? "" : coverUrl);
        TextField artistAlbum = new TextField(create ? "" : artistName);

        dialog.getDialogPane().setContent(GuiUtils.createForm(new ImmutableMap.Builder<String, Control>()
                .put("Id : ", albumId)
                .put("Title : ", albumTitle)
                .put("Release date : ", releaseAlbum)
                .put("Cover : ", coverAlbum)
                .put("Artist : ", artistAlbum)
                .build()
        ));

        Platform.runLater(albumId::requestFocus);

        dialog.setResultConverter(b -> null);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            int previousId = id;
            id = Integer.parseInt(albumId.getText());
            title = albumTitle.getText();
            releaseDate = java.sql.Date.valueOf(releaseAlbum.getValue());
            coverUrl = coverAlbum.getText();
            artistName = artistAlbum.getText();

            List<ValidationError> errors = validate();
            if (!errors.isEmpty()) {
                id = previousId;
                new ValidationAlert(errors).showAndWait();
                return;
            }
            OracleDB.executeThen(
                    () -> {
                        if (create) {
                            OracleDB.addAlbums(this);
                        } else {
                            OracleDB.updateAlbum(previousId, this);
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

    public String getTitle() {
        return title;
    }

    public Date getRelease_date() {
        return releaseDate;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getArtistName() {
        return artistName;
    }

    public int getId() {
        return id;
    }

    public List<AlbumCategory> getAlbumCatgeories() {
        return albumCatgeories;
    }
}
