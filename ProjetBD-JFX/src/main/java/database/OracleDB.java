package database;

import gui.ExceptionAlert;
import gui.OracleConnectDialog;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.util.Pair;
import model.*;
import oracle.jdbc.driver.OracleDriver;
import utils.SQLRunnable;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class OracleDB {

    private static final ExecutorService executor = Executors.newFixedThreadPool(5);
    private static Connection connection;

    public static boolean checkForOracleDriver() {
        try {
            DriverManager.registerDriver(new OracleDriver());
            return true;
        } catch (SQLException e) {
            System.err.println("Oracle SQL Driver not available: " + e.getMessage());
            new ExceptionAlert(e).showAndWait();
            Platform.exit();
            return false;
        }
    }

    public static boolean attemptConnect() {
        Optional<Connection> oConnection = new OracleConnectDialog().showAndWait();
        if (oConnection.isPresent()) {
            connection = oConnection.get();
            try {
                connection.setAutoCommit(false);
                return true;
            } catch (SQLException throwables) {
                new ExceptionAlert(throwables).showAndWait();
                Platform.exit();
                return false;
            }
        } else {
            Platform.exit();
            return false;
        }
    }

    public static void cleanup() {
        if (connection != null) {
            try {
                connection.rollback();
                connection.close();
            } catch (SQLException ignored) {}
        }
        executor.shutdownNow();
    }

    public static void commit() throws SQLException {
        connection.commit();
    }

    public static void rollback() throws SQLException {
        connection.rollback();
    }

    public static void listUsers(List<User> users) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM KLEX_USER");
        while (resultSet.next()) {
            User user = new User().loadFromResultSet(resultSet);
            if (user != null)
                users.add(user);
        }
    }

    private static void loadUserParameters(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.getEmail());
        statement.setString(2, user.getFirstName());
        statement.setString(3, user.getLastName());
        statement.setInt(4, user.getAge());
        statement.setInt(5, user.getAccessCode());
        statement.setString(6, user.getPreferredLanguage());
    }

    public static void addUser(User user) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO KLEX_USER VALUES (?, ?, ?, ?, ?, ?)"
        );
        loadUserParameters(statement, user);
        statement.executeUpdate();
    }

    public static void updateUser(String previousEmail, User user) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE KLEX_USER SET EMAIL = ?, FIRST_NAME = ?, LAST_NAME = ?, AGE = ?, ACCESS_CODE = ?, PREFERRED_LANGUAGE = ? WHERE EMAIL = ?"
        );
        loadUserParameters(statement, user);
        statement.setString(7, previousEmail);
        statement.executeUpdate();
    }

    public static void deleteUser(User user) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "DELETE KLEX_USER WHERE EMAIL = ?"
        );
        statement.setString(1, user.getEmail());
        statement.executeUpdate();
    }

    public static void listFilmCategories(List<FilmCategory> filmCategories) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM FILM_CATEGORY");
        while (resultSet.next()) {
            FilmCategory filmCategory = new FilmCategory().loadFromResultSet(resultSet);
            if (filmCategory != null)
                filmCategories.add(filmCategory);
        }
    }

    public static void listFilmCategories(Film film) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM FILM_A_POUR_CAT WHERE FILM_TITLE = ? AND FILM_YEAR = ?"
        );
        statement.setString(1, film.getTitle());
        statement.setInt(2, film.getYear());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            FilmCategory filmCategory = new FilmCategory().loadFromResultSet(resultSet);
            if (filmCategory != null)
                film.getFilmCategories().add(filmCategory);
        }
    }

    private static void loadMediaFileParameters(PreparedStatement statement, MediaFile file) throws SQLException {
        statement.setInt(1, file.getId());
        statement.setInt(2, file.getSize());
        statement.setDate(3, file.getAddedDate());
        statement.setString(4, file.getUserEmail());
    }
    
    public static void addMediaFile(MediaFile file) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO MEDIA_FILE VALUES (?, ?, ?, ?)"
        );
        loadMediaFileParameters(statement, file);
        statement.executeUpdate();
        for (Flux<?> flux : file.getFlux()) {
            statement = connection.prepareStatement("INSERT INTO FLUX VALUES (?, ?, ?)");
            statement.setInt(1, file.getId());
            statement.setInt(2, flux.getFluxId());
            statement.setInt(3, flux.getDataRate());
            statement.executeUpdate();
            if (flux instanceof VideoFlux) {
                VideoFlux videoFlux = (VideoFlux) flux;
                statement = connection.prepareStatement("INSERT INTO VIDEO_FLUX VALUES (?, ?, ?, ?, ?, ?)");
                statement.setInt(1, file.getId());
                statement.setInt(2, videoFlux.getFluxId());
                statement.setString(3, videoFlux.getVideoCodecName());
                statement.setInt(4, videoFlux.getWidth());
                statement.setInt(5, videoFlux.getHeight());
                statement.setInt(6, videoFlux.getDataRate());
                statement.executeUpdate();
            } else if (flux instanceof AudioFlux) {
                AudioFlux audioFlux = (AudioFlux) flux;
                statement = connection.prepareStatement("INSERT INTO AUDIO_FLUX VALUES (?, ?, ?, ?, ?, ?)");
                statement.setInt(1, file.getId());
                statement.setInt(2, audioFlux.getFluxId());
                statement.setString(3, audioFlux.getAudioCodecName());
                statement.setInt(4, audioFlux.getSampling());
                statement.setString(5, audioFlux.getLanguage());
                statement.setInt(6, audioFlux.getDataRate());
                statement.executeUpdate();
            } else if (flux instanceof TextFlux) {
                TextFlux textFlux = (TextFlux) flux;
                statement = connection.prepareStatement("INSERT INTO TEXT_FLUX VALUES (?, ?, ?, ?, ?)");
                statement.setInt(1, file.getId());
                statement.setInt(2, textFlux.getFluxId());
                statement.setString(3, textFlux.getTextCodecName());
                statement.setString(4, textFlux.getLanguage());
                statement.setInt(5, textFlux.getDataRate());
                statement.executeUpdate();
            }
        }
    }

    public static void updateMediaFile(int previousId, MediaFile file) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE MEDIA_FILE SET FILE_ID = ?, FILE_SIZE = ?, ADDED_DATE = ?, USER_EMAIL = ? WHERE FILE_ID = ?"
        );
        loadMediaFileParameters(statement, file);
        statement.setInt(5, previousId);
        statement.executeUpdate();
    }

    public static void deleteMediaFile(MediaFile file) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "DELETE MEDIA_FILE WHERE FILE_ID = ?"
        );
        statement.setInt(1, file.getId());
        statement.executeUpdate();
    }

    public static void listFilms(List<Film> films) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM FILM");
        while (resultSet.next()) {
            Film film = new Film().loadFromResultSet(resultSet);
            if (film != null)
                films.add(film);
        }
    }

    public static void listFilmArtists(Film film, List<Artist> artists) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT ARTIST.* FROM PARTICIPE_AU_FILM PAF " +
                        "INNER JOIN ARTIST on ARTIST.ARTIST_ID = PAF.ARTIST_ID " +
                        "WHERE FILM_TITLE = ? AND FILM_YEAR = ?"
        );
        statement.setString(1, film.getTitle());
        statement.setInt(2, film.getYear());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Artist artist = new Artist().loadFromResultSet(resultSet);
            if (artist != null)
                artists.add(artist);
        }
    }

    public static void listFilmActors(Film film, List<Artist> actors) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT ARTIST.*, JR.CHARACTER_NAME FROM JOUE_ROLE JR " +
                        "INNER JOIN ARTIST on ARTIST.ARTIST_ID = JR.ARTIST_ID " +
                        "WHERE FILM_TITLE = ? AND FILM_YEAR = ?"
        );
        statement.setString(1, film.getTitle());
        statement.setInt(2, film.getYear());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Artist.Actor actor = (Artist.Actor) new Artist.Actor(resultSet.getString("CHARACTER_NAME")).loadFromResultSet(resultSet);
            if (actor != null)
                actors.add(actor);
        }
    }

    private static void loadArtistParameters(PreparedStatement statement, Artist artist) throws SQLException {
        statement.setInt(1, artist.getId());
        statement.setString(2, artist.getName());
        statement.setString(3, artist.getPictureUrl());
        statement.setString(4, artist.getMainSpecialty());
        statement.setDate(5, artist.getBirthDate());
        statement.setString(6, artist.getBiography());
    }

    public static void listArtists(List<Artist> artists) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM ARTIST");
        while (resultSet.next()) {
            Artist artist = new Artist().loadFromResultSet(resultSet);
            if (artist != null)
                artists.add(artist);
        }
    }

    public static void addArtist(Artist artist) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ARTIST VALUES (?, ?, ?, ?, ?, ?)"
        );
        loadArtistParameters(statement, artist);
        statement.executeUpdate();
    }

    public static void updateArtist(int previousId, Artist artist) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE ARTIST SET ARTIST_ID = ?, NAME = ?, PICTURE_URL = ?, MAIN_SPECIALTY = ?, BIRTH_DATE = ?, BIOGRAPHY = ? WHERE ARTIST_ID = ?"
        );
        loadArtistParameters(statement, artist);
        statement.setInt(7, previousId);
        statement.executeUpdate();
    }

    public static void addFilmCategory(FilmCategory filmCategory) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO FILM_CATEGORY VALUES (?)"
        );
        statement.setString(1, filmCategory.getName());
        statement.executeUpdate();
    }

    public static void updateFilmCategory(String previousName, FilmCategory filmCategory) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE FILM_CATEGORY SET FILM_CATEGORY_NAME = ? WHERE FILM_CATEGORY_NAME = ?"
        );
        statement.setString(1, filmCategory.getName());
        statement.setString(2, previousName);
        statement.executeUpdate();
    }

    private static void loadFilmParameters(PreparedStatement statement, Film film) throws SQLException {
        statement.setString(1, film.getTitle());
        statement.setInt(2, film.getYear());
        statement.setString(3, film.getAbstract());
        statement.setInt(4, film.getRecommendedAge());
        statement.setString(5, film.getPosterUrl());
    }

    public static void addFilm(Film film) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO FILM VALUES (?, ?, ?, ?, ?)"
        );
        loadFilmParameters(statement, film);
        statement.executeUpdate();
        updateFilmCategories(film);
        updateFilmArtists(film);
    }

    public static void updateFilmCategories(Film film) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "DELETE FILM_A_POUR_CAT WHERE FILM_TITLE = ? AND FILM_YEAR = ?"
        );
        statement.setString(1, film.getTitle());
        statement.setInt(2, film.getYear());
        statement.executeUpdate();

        for (FilmCategory filmCategory : film.getFilmCategories()) {
            statement = connection.prepareStatement(
                    "INSERT INTO FILM_A_POUR_CAT VALUES (?, ?, ?)"
            );
            statement.setString(1, film.getTitle());
            statement.setInt(2, film.getYear());
            statement.setString(3, filmCategory.getName());
            statement.executeUpdate();
        }
    }

    public static void updateFilm(String previousTitle, int previousYear, Film film) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE FILM SET FILM_TITLE = ?, FILM_YEAR = ?, ABSTRACT = ?, RECOMMENDED_AGE = ?, POSTER_URL = ? WHERE FILM_TITLE = ? AND FILM_YEAR = ?"
        );
        loadFilmParameters(statement, film);
        statement.setString(6, previousTitle);
        statement.setInt(7, previousYear);
        statement.executeUpdate();
        updateFilmCategories(film);
        updateFilmArtists(film);
    }

    public static void updateFilmArtists(Film film) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "DELETE JOUE_ROLE WHERE FILM_TITLE = ? AND FILM_YEAR = ?"
        );
        statement.setString(1, film.getTitle());
        statement.setInt(2, film.getYear());
        statement.executeUpdate();

        statement = connection.prepareStatement(
                "DELETE PARTICIPE_AU_FILM WHERE FILM_TITLE = ? AND FILM_YEAR = ?"
        );
        statement.setString(1, film.getTitle());
        statement.setInt(2, film.getYear());
        statement.executeUpdate();

        for (Artist artist : film.getArtists()) {
            if (artist instanceof Artist.Actor) {
                Artist.Actor actor = (Artist.Actor) artist;
                statement = connection.prepareStatement("INSERT INTO JOUE_ROLE VALUES (?, ?, ?, ?)");
                statement.setInt(1, actor.getId());
                statement.setString(2, film.getTitle());
                statement.setInt(3, film.getYear());
                statement.setString(4, actor.getRoleName());
            } else {
                statement = connection.prepareStatement("INSERT INTO PARTICIPE_AU_FILM VALUES (?, ?, ?)");
                statement.setInt(1, artist.getId());
                statement.setString(2, film.getTitle());
                statement.setInt(3, film.getYear());
            }
            statement.executeUpdate();
        }
    }

    public static void addFileToFilm(MediaFile file, Film film) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO FILE_CONCERNE_FILM VALUES (?, ?, ?)"
        );
        statement.setInt(1, file.getId());
        statement.setString(2, film.getTitle());
        statement.setInt(3, film.getYear());
        statement.executeUpdate();
    }

    public static void deleteFilm(Film film) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT MF.* FROM FILM " +
                        "INNER JOIN FILE_CONCERNE_FILM FCF on FILM.FILM_TITLE = FCF.FILM_TITLE and FILM.FILM_YEAR = FCF.FILM_YEAR " +
                        "INNER JOIN MEDIA_FILE MF on FCF.FILE_ID = MF.FILE_ID " +
                        "WHERE FCF.FILM_TITLE = ? AND FCF.FILM_YEAR = ?"
        );
        statement.setString(1, film.getTitle());
        statement.setInt(2, film.getYear());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            deleteMediaFile(new MediaFile().loadFromResultSet(resultSet));
        }
        statement = connection.prepareStatement(
                "DELETE FILM WHERE FILM_TITLE = ? AND FILM_YEAR = ?"
        );
        statement.setString(1, film.getTitle());
        statement.setInt(2, film.getYear());
        statement.executeUpdate();
    }

    public static void listVideoCodecs(List<VideoCodec> videoCodecs) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM VIDEO_CODEC");
        while (resultSet.next()) {
            VideoCodec videoCodec = new VideoCodec().loadFromResultSet(resultSet);
            if (videoCodec != null)
                videoCodecs.add(videoCodec);
        }
    }

    public static void listAudioCodecs(List<AudioCodec> audioCodecs) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM AUDIO_CODEC");
        while (resultSet.next()) {
            AudioCodec audioCodec = new AudioCodec().loadFromResultSet(resultSet);
            if (audioCodec != null)
                audioCodecs.add(audioCodec);
        }
    }

    public static void listTextCodecs(List<TextCodec> textCodecs) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM TEXT_CODEC");
        while (resultSet.next()) {
            TextCodec textCodec = new TextCodec().loadFromResultSet(resultSet);
            if (textCodec != null)
                textCodecs.add(textCodec);
        }
    }

    public static void executeThen(SQLRunnable runnable, Runnable then, Consumer<Exception> onError) {
        executor.execute(() -> {
            try {
                runnable.run();
                if (then != null) {
                    Platform.runLater(then);
                }
            } catch (Exception e) {
                if (onError != null)
                    Platform.runLater(() -> onError.accept(e));
                else
                    Platform.runLater(() -> new ExceptionAlert(e).showAndWait());
            }
        });
    }

    public static void executeThen(SQLRunnable runnable, Runnable then) {
        executeThen(runnable, then, null);
    }

    public static void execute(SQLRunnable runnable, Consumer<Exception> onError) {
        executeThen(runnable, null, onError);
    }

    public static void execute(SQLRunnable runnable) {
        executeThen(runnable, null, null);
    }

    public static <R> CompletableFuture<R> query(Callable<R> query, Consumer<Exception> onError) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return query.call();
            } catch (Exception e) {
                if (onError != null)
                    Platform.runLater(() -> onError.accept(e));
                else
                    Platform.runLater(() -> new ExceptionAlert(e).showAndWait());
                return null;
            }
        }, executor);
    }

    public static <R> CompletableFuture<R> query(Callable<R> query) {
        return query(query, null);
    }

    public static <R> void queryThen(Callable<R> query, Consumer<R> then, Consumer<Exception> onError) {
        query(query, onError).thenAccept(result -> {
            if (result != null) {
                Platform.runLater(() -> then.accept(result));
            }
        });
    }

    public static <R> void queryThen(Callable<R> query, Consumer<R> then) {
        queryThen(query, then, null);
    }

    public static ExecutorService getExecutor() {
        return executor;
    }


    public static void listTracks(ObservableList<Track> tracks) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM TRACK");
        while (resultSet.next()) {
            Track track = new Track().loadFromResultSet(resultSet);
            if (track != null)
                tracks.add(track);
        }
    }

    public static void setAlbum(Track track) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM ALBUM WHERE ALBUM_ID = ?"
        );
        statement.setInt(1, track.getAlbumId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Album album = new Album().loadFromResultSet(resultSet);
            if (album != null)
                track.setAlbum(album);
        }
    }

    public static void listAlbumCategory(Album album) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM ALBUM_A_POUR_CAT WHERE ALBUM_ID = ?"
        );
        statement.setInt(1, album.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            AlbumCategory album_cat = new AlbumCategory().loadFromResultSet(resultSet);
            if (album_cat != null)
                album.getAlbumCatgeories().add(album_cat);
        }
    }

    public static void setArtist(Track track) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT ARTIST.*, JD.INSTRUMENT_NAME FROM JOUE_DANS JD " +
                        "INNER JOIN ARTIST on ARTIST.ARTIST_ID = JD.ARTIST_ID " +
                        "WHERE ALBUM_ID = ? AND TRACK_ID = ?"
        );
        statement.setInt(1, track.getAlbumId());
        statement.setInt(2, track.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Artist.Musician musician = (Artist.Musician) new Artist.Musician(resultSet.getString("INSTRUMENT_NAME")).loadFromResultSet(resultSet);
            if (musician != null)
                track.getArtistsList().add(musician);
        }
    }

    public static void addMusicCategory(MusicCategory musicCategory) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO MUSIC_CATEGORY VALUES (?)"
        );
        statement.setString(1, musicCategory.getName());
        statement.executeUpdate();
    }

    public static void updateMusicCategory(String previousName, MusicCategory musicCategory) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE FILM_CATEGORY SET FILM_CATEGORY_NAME = ? WHERE FILM_CATEGORY_NAME = ?"
        );
        statement.setString(1, musicCategory.getName());
        statement.setString(2, previousName);
        statement.executeUpdate();
    }

    public static void listMusicCategories(ObservableList<MusicCategory> items) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM MUSIC_CATEGORY");
        while (resultSet.next()) {
            MusicCategory musicCategory = new MusicCategory().loadFromResultSet(resultSet);
            if (musicCategory != null)
                items.add(musicCategory);
        }
    }

    public static void listTrackArtists(Track track, List<Artist.Musician> artistsList) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT ARTIST.*, JD.INSTRUMENT_NAME FROM JOUE_DANS JD " +
                        "INNER JOIN ARTIST ON ARTIST.ARTIST_ID = JD.ARTIST_ID " +
                        "WHERE TRACK_ID = ? AND ALBUM_ID = ?"
        );
        statement.setInt(1, track.getId());
        statement.setInt(2, track.getAlbumId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Artist.Musician musician = (Artist.Musician) new Artist.Musician(resultSet.getString("INSTRUMENT_NAME")).loadFromResultSet(resultSet);
            if (musician != null)
                artistsList.add(musician);
        }
    }

    public static void listAlbum(ObservableList<Album> items) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM ALBUM");
        while (resultSet.next()) {
            Album album = new Album().loadFromResultSet(resultSet);
            if (album != null)
                items.add(album);
        }
    }

    public static void addAlbums(Album album) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ALBUM VALUES (?, ?, ?, ?, ?)"
        );
        loadAlbumParameters(statement, album);
        statement.executeUpdate();
    }

    private static void loadAlbumParameters(PreparedStatement statement, Album album) throws SQLException {
        statement.setInt(1, album.getId());
        statement.setString(2, album.getTitle());
        statement.setDate(3, (Date) album.getRelease_date());
        statement.setString(4, album.getCoverUrl());
        statement.setString(5, album.getArtistName());

    }

    public static void updateAlbum(int previousId, Album album) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE ALBUM SET ALBUM_ID = ?, ALBUM_TITLE = ?, RELEASE_DATE = ?, COVER_URL = ?, ARTIST_NAME = ? WHERE ALBUM_ID = ?"
        );
        loadAlbumParameters(statement, album);
        statement.setInt(6, previousId);
        statement.executeUpdate();
    }

    public static void addTrack(Track track) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO TRACK VALUES (?, ?, ?, ?)"
        );
        loadTrackParameters(statement, track);
        statement.executeUpdate();
        updateTrackCategories(track);
        updateTrackArtists(track);
    }

    public static void updateTrackCategories(Track track) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "DELETE TRACK_A_POUR_CAT WHERE ALBUM_ID = ? AND TRACK_ID = ?"
        );
        statement.setInt(1, track.getAlbumId());
        statement.setInt(2, track.getId());
        statement.executeUpdate();

        for (MusicCategory mc : track.getTrackCategories()) {
            statement = connection.prepareStatement(
                    "INSERT INTO TRACK_A_POUR_CAT VALUES (?, ?, ?)"
            );
            statement.setInt(1, track.getAlbum().getId());
            statement.setInt(2, track.getId());
            statement.setString(3, mc.getName());
            statement.executeUpdate();
        }
    }

    public static void updateTrackArtists(Track track) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "DELETE JOUE_DANS WHERE ALBUM_ID = ? AND TRACK_ID = ?"
        );
        statement.setInt(1, track.getAlbumId());
        statement.setInt(2, track.getId());
        statement.executeUpdate();

        for (Artist.Musician art : track.getArtistsList()) {
            statement = connection.prepareStatement(
                    "INSERT INTO JOUE_DANS VALUES (?, ?, ?, ?)"
            );
            statement.setInt(1, art.getId());
            statement.setInt(2, track.getAlbum().getId());
            statement.setInt(3, track.getId());
            statement.setString(4, art.getInstrument());

            statement.executeUpdate();
        }
    }

    private static void loadTrackParameters(PreparedStatement statement, Track track) throws SQLException {
        statement.setInt(1, track.getAlbum().getId());
        statement.setInt(2, track.getId());
        statement.setString(3, track.getTitle());
        statement.setInt(4, track.getLength());
    }

    public static void updateTrack(int previousAlbumId, int previousId, Track track) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE TRACK SET ALBUM_ID = ?, TRACK_ID = ?, TRACK_TITLE = ?, LENGTH = ?  WHERE ALBUM_ID = ? AND TRACK_ID = ?"
        );
        loadTrackParameters(statement, track);
        statement.setInt(5, previousAlbumId);
        statement.setInt(6, previousId);
        statement.executeUpdate();
        updateTrackCategories(track);
        updateTrackArtists(track);
    }

    public static void addFileToTrack(MediaFile file, Track track) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO FILE_CONCERNE_TRACK VALUES (?, ?, ?)"
        );
        statement.setInt(1, file.getId());
        statement.setInt(2, track.getAlbumId());
        statement.setInt(3, track.getId());
        statement.executeUpdate();
    }

    public static void deleteTrack(Track track) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT MF.* FROM TRACK " +
                        "INNER JOIN FILE_CONCERNE_TRACK FCT on TRACK.ALBUM_ID = FCT.ALBUM_ID and TRACK.TRACK_ID = FCT.TRACK_ID " +
                        "INNER JOIN MEDIA_FILE MF on FCT.FILE_ID = MF.FILE_ID " +
                        "WHERE FCT.ALBUM_ID = ? AND FCT.TRACK_ID = ?"
        );
        statement.setInt(1, track.getAlbumId());
        statement.setInt(2, track.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            deleteMediaFile(new MediaFile().loadFromResultSet(resultSet));
        }
        statement = connection.prepareStatement(
                "DELETE TRACK WHERE ALBUM_ID = ? AND TRACK_ID = ?"
        );
        statement.setInt(1, track.getAlbumId());
        statement.setInt(2, track.getId());
        statement.executeUpdate();
    }

    public static void listTrackCategories(Track track) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM TRACK_A_POUR_CAT WHERE ALBUM_ID = ? AND TRACK_ID = ?"
        );
        statement.setInt(1, track.getAlbumId());
        statement.setInt(2, track.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            MusicCategory trackCategory = new MusicCategory().loadFromResultSet(resultSet);
            if (trackCategory != null)
                track.getTrackCategories().add(trackCategory);
        }
    }






    public static void listFilmsUser(ObservableList<Film> films1, Pair<String, String> data) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT film_cat_age.*" +
                        "FROM (SELECT FILM.* FROM FILM INNER JOIN FILM_A_POUR_CAT  ON FILM_A_POUR_CAT.film_title = FILM.film_title AND FILM_A_POUR_CAT.film_year = FILM.film_year " +
                        "WHERE FILM_CATEGORY_NAME = ? " +
                        "AND RECOMMENDED_AGE <= (SELECT age from KLEX_USER WHERE email = ?)) film_cat_age " +
                        "INNER JOIN FILE_CONCERNE_FILM  ON FILE_CONCERNE_FILM.film_year = film_cat_age.film_year AND  FILE_CONCERNE_FILM.film_title = film_cat_age.film_title " +
                        "LEFT JOIN MEDIA_FILE ON MEDIA_FILE.file_id = FILE_CONCERNE_FILM.file_id " +
                        "LEFT JOIN TEXT_FLUX ON TEXT_FLUX.file_id = MEDIA_FILE.file_id " +
                        "LEFT JOIN AUDIO_FLUX AF on FILE_CONCERNE_FILM.FILE_ID = AF.FILE_ID " +
                        "WHERE AF.AUDIO_LANGUAGE in (SELECT PREFERRED_LANGUAGE from KLEX_USER WHERE EMAIL = ?) OR "+
                        "TEXT_FLUX.text_language in (SELECT PREFERRED_LANGUAGE from KLEX_USER WHERE EMAIL = ?)"
        );
        statement.setString(1, data.getKey());
        statement.setString(2, data.getValue());
        statement.setString(3, data.getValue());
        statement.setString(4, data.getValue());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Film film = new Film().loadFromResultSet(resultSet);
            if (film != null)
                films1.add(film);
        }
    }

    public static void listTracksUser(ObservableList<Track> tracks1, Pair<String, String> data) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "select distinct tid.* from " +
                        "(select TRACK.* from TRACK " +
                "inner join FILE_CONCERNE_TRACK FCT on TRACK.ALBUM_ID = FCT.ALBUM_ID and TRACK.TRACK_ID = FCT.TRACK_ID " +
                "inner join FLUX F on FCT.FILE_ID = F.FILE_ID " +
                "inner join AUDIO_FLUX AF on F.FILE_ID = AF.FILE_ID and F.FLUX_ID = AF.FLUX_ID " +
                "where AUDIO_LANGUAGE = (select PREFERRED_LANGUAGE from KLEX_USER where EMAIL = ?)) tid " +
                "inner join ALBUM on ALBUM.ALBUM_ID = tid.ALBUM_ID " +
                "inner join JOUE_DANS on JOUE_DANS.ALBUM_ID = tid.ALBUM_ID " +
                "inner join ARTIST on ARTIST.ARTIST_ID = JOUE_DANS.ARTIST_ID " +
                        "inner join TRACK_A_POUR_CAT on tid.TRACK_ID = TRACK_A_POUR_CAT.TRACK_ID "+
                        " and tid.ALBUM_ID = TRACK_A_POUR_CAT.ALBUM_ID where MUSIC_CATEGORY_NAME = ?"

        );
        statement.setString(1, data.getValue());
        statement.setString(2, data.getKey());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Track track = new Track().loadFromResultSet(resultSet);
            if (track != null)
                tracks1.add(track);
        }
    }

}
