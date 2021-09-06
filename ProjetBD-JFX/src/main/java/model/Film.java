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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class Film extends EditableModel<Film> {

    private String title;
    private int year;
    private String _abstract;
    private int recommendedAge;
    private String posterUrl;
    private final List<FilmCategory> filmCategories = new ArrayList<>();

    private List<Artist> artists;

    @Override
    public Film loadFromResultSet(ResultSet resultSet) throws SQLException {
        title = resultSet.getString("FILM_TITLE");
        year = resultSet.getInt("FILM_YEAR");
        _abstract = resultSet.getString("ABSTRACT");
        recommendedAge = resultSet.getInt("RECOMMENDED_AGE");
        posterUrl = resultSet.getString("POSTER_URL");
        OracleDB.listFilmCategories(this);
        return this;
    }

    @Override
    protected List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if (title.isBlank())
            errors.add(new ValidationError("Title", "Cannot be blank"));
        if (recommendedAge < 0)
            errors.add(new ValidationError("Recommended age", "Must be positive"));
        if (filmCategories.size() == 0)
            errors.add(new ValidationError("Categories", "You must specify at least one category"));
        return errors;
    }

    @Override
    public Dialog<Film> getEditDialog(boolean create) {
        Dialog<Film> dialog = new Dialog<>();
        dialog.setTitle(create ? "Film creation" : "Film edition");
        dialog.setHeaderText(create
                ? "Please fill in the details of the new film."
                : "You can edit the details of \"" + title + "\" below."
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TextField titleField = new TextField(create ? "" : title);
        TextField yearField = GuiUtils.getNumericField(create ? Calendar.getInstance().get(Calendar.YEAR) : year);
        TextArea abstractField = new TextArea(create ? "" : _abstract);
        TextField recommendedAgeField = GuiUtils.getNumericField(create ? 0 : recommendedAge);
        TextField posterUrlField = new TextField(create ? "" : posterUrl);

        CheckComboBox<FilmCategory> categoryField = new CheckComboBox<>();
        categoryField.setConverter(new StringConverter<>() {
            @Override
            public String toString(FilmCategory filmCategory) {
                return filmCategory.getName();
            }

            @Override
            public FilmCategory fromString(String s) {
                return categoryField.getItems().stream().filter(fc -> fc.getName().equals(s)).findFirst().orElseThrow();
            }
        });
        OracleDB.executeThen(
                () -> OracleDB.listFilmCategories(categoryField.getItems()),
                () -> filmCategories.forEach(fc -> categoryField.getCheckModel().check(fc))
        );

        Button newCategoryButton = new Button("Create new category");

        newCategoryButton.setOnAction(e -> new FilmCategory().getEditDialog(true).showAndWait().ifPresent(
                filmCategory -> categoryField.getItems().add(filmCategory)
        ));

        GridPane grid = GuiUtils.createForm(new ImmutableMap.Builder<String, Control>()
                .put("Title:", titleField)
                .put("Year:", yearField)
                .put("Abstract:", abstractField)
                .put("Recommended age:", recommendedAgeField)
                .put("Poster URL:", posterUrlField)
                .build()
        );

        grid.add(new Label("Category"), 0, 6);
        grid.add(categoryField, 1, 6);
        grid.add(newCategoryButton, 2, 6);

        grid.add(new Separator(), 0, 7, 3, 1);
        grid.add(new Label("Participating artists"), 0, 8, 3, 1);
        ListView<Artist> artistList = new ListView<>();
        grid.add(artistList, 0, 9, 3, 1);
        artists = artistList.getItems();
        artistList.setPlaceholder(new Label("You can add artists to this film"));

        OracleDB.execute(() -> {
            OracleDB.listFilmArtists(this, artists);
            OracleDB.listFilmActors(this, artists);
        });

        Button addArtist = new Button("Add an artist");
        Button addActor = new Button("Add an actor");

        addArtist.setOnAction(e -> {
            Artist artist = Artist.getChoiceDialog().showAndWait().orElse(null);
            if (artist != null) {
                if (artists.stream().filter(a -> !(a instanceof Artist.Actor)).map(Artist::getId).collect(Collectors.toList()).contains(artist.getId())) {
                    new ExceptionAlert(new IllegalArgumentException("This artist is already part of the film.")).showAndWait();
                    return;
                }
                artists.add(artist);
            }
        });

        addActor.setOnAction(e -> {
            Artist.Actor actor = (Artist.Actor) Artist.Actor.getChoiceDialog().showAndWait().orElse(null);
            if (actor != null) {
                if (artists.stream().anyMatch(a -> a instanceof Artist.Actor && a.getId() == actor.getId() && ((Artist.Actor) a).getRoleName().equals(actor.getRoleName()))) {
                    new ExceptionAlert(new IllegalArgumentException("This actor is already playing this role in the film.")).showAndWait();
                    return;
                }
                artists.add(actor);
            }
        });

        grid.add(addArtist, 0, 10);
        grid.add(addActor, 1, 10);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(titleField::requestFocus);

        dialog.setResultConverter(b -> null);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            List<FilmCategory> selectedCategories = categoryField.getItems().filtered(fc -> categoryField.getCheckModel().isChecked(fc));
            String previousTitle = title;
            int previousYear = year;
            title = titleField.getText();
            year = Integer.parseInt(yearField.getText());
            _abstract = abstractField.getText();
            recommendedAge = Integer.parseInt(recommendedAgeField.getText());
            posterUrl = posterUrlField.getText();
            filmCategories.clear();
            filmCategories.addAll(selectedCategories);
            List<ValidationError> errors = validate();
            if (!errors.isEmpty()) {
                title = previousTitle;
                year = previousYear;
                new ValidationAlert(errors).showAndWait();
                return;
            }
            OracleDB.executeThen(
                    () -> {
                        if (create) {
                            OracleDB.addFilm(this);
                        } else {
                            OracleDB.updateFilm(previousTitle, previousYear, this);
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

    public int getYear() {
        return year;
    }

    public String getAbstract() {
        return _abstract;
    }

    public int getRecommendedAge() {
        return recommendedAge;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public List<FilmCategory> getFilmCategories() {
        return filmCategories;
    }

    public String getCategoriesStr() {
        return filmCategories.stream().map(FilmCategory::getName).collect(Collectors.joining(", "));
    }

    public List<Artist> getArtists() {
        return artists;
    }
}
