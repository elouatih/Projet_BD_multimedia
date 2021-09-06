CREATE TABLE KLEX_USER(
    email varchar(255) NOT NULL PRIMARY KEY,
    first_name varchar(255) NOT NULL,
    last_name varchar(255) NOT NULL,
    age int NOT NULL CHECK(age > 0),
    access_code int NOT NULL CHECK(access_code < 10000),
    preferred_language varchar(2) NOT NULL -- Taille 2 car ISO-639-1
);

CREATE TABLE MEDIA_FILE(
    file_id int NOT NULL PRIMARY KEY,
    file_size int NOT NULL CHECK(file_size >= 0),
	added_date date NOT NULL,
	user_email varchar(255) NOT NULL REFERENCES KLEX_USER(email)
);

CREATE TABLE VIDEO_CODEC(
	video_codec_name varchar(255) NOT NULL PRIMARY KEY
);

CREATE TABLE AUDIO_CODEC(
    audio_codec_name varchar(255) NOT NULL PRIMARY KEY
);

CREATE TABLE TEXT_CODEC(
    text_codec_name varchar(255) NOT NULL PRIMARY KEY
);

CREATE TABLE FLUX(
	file_id int NOT NULL REFERENCES MEDIA_FILE(file_id) ON DELETE CASCADE,
	flux_id int  NOT NULL,
	data_rate int NOT NULL CHECK(data_rate >= 0),
    PRIMARY KEY(file_id, flux_id)
);

CREATE TABLE VIDEO_FLUX(
	file_id int NOT NULL,
	flux_id int NOT NULL,
	video_codec_name varchar(255) NOT NULL REFERENCES VIDEO_CODEC(video_codec_name),
	width int NOT NULL CHECK(width > 0),
	height int NOT NULL CHECK(height > 0),
	data_rate int NOT NULL CHECK(data_rate >= 0),
	PRIMARY KEY(file_id, flux_id),
	CONSTRAINT FK_VFLUX FOREIGN KEY (file_id, flux_id) REFERENCES FLUX(file_id, flux_id) ON DELETE CASCADE
);

CREATE TABLE AUDIO_FLUX(
	file_id int NOT NULL,
	flux_id int NOT NULL,
	audio_codec_name varchar(255) NOT NULL REFERENCES AUDIO_CODEC(audio_codec_name),
	sampling int NOT NULL CHECK(sampling IN (16, 24, 32)),
	audio_language varchar(2) NOT NULL,
	data_rate int NOT NULL CHECK(data_rate >=0),
	PRIMARY KEY(file_id, flux_id),
	CONSTRAINT FK_AFLUX FOREIGN KEY (file_id, flux_id) REFERENCES FLUX(file_id, flux_id) ON DELETE CASCADE
);

CREATE TABLE TEXT_FLUX(
    file_id int NOT NULL,
    flux_id int NOT NULL,
    text_codec_name varchar(255) NOT NULL REFERENCES TEXT_CODEC(text_codec_name),
    text_language varchar(2) NOT NULL,
    data_rate int NOT NULL CHECK(data_rate >= 0),
    PRIMARY KEY(file_id, flux_id),
	CONSTRAINT FK_TFLUX FOREIGN KEY (file_id, flux_id) REFERENCES FLUX(file_id, flux_id) ON DELETE CASCADE
);

CREATE TABLE CLIENT(
    brand varchar(255) NOT NULL,
    model varchar(255) NOT NULL,
    max_width int NOT NULL CHECK(max_width > 0),
    max_height int NOT NULL CHECK(max_height > 0),
    PRIMARY KEY (brand, model)
);

CREATE TABLE CLIENT_SUPPORTE_CODEC_VIDEO(
    brand varchar(255) NOT NULL,
    model varchar(255) NOT NULL,
    video_codec_name varchar(255) NOT NULL REFERENCES VIDEO_CODEC(video_codec_name),
    PRIMARY KEY (brand, model, video_codec_name),
    CONSTRAINT FK_CLIENT_VCODEC FOREIGN KEY (brand, model) REFERENCES CLIENT(brand, model) ON DELETE CASCADE
);

CREATE TABLE CLIENT_SUPPORTE_CODEC_AUDIO(
    brand varchar(255) NOT NULL,
    model varchar(255) NOT NULL,
    audio_codec_name varchar(255) NOT NULL REFERENCES AUDIO_CODEC(audio_codec_name),
    PRIMARY KEY (brand, model, audio_codec_name),
    CONSTRAINT FK_CLIENT_ACODEC FOREIGN KEY (brand, model) REFERENCES CLIENT(brand, model) ON DELETE CASCADE
);

CREATE TABLE CLIENT_SUPPORTE_CODEC_TEXTE(
    brand varchar(255) NOT NULL,
    model varchar(255) NOT NULL,
    text_codec_name varchar(255) NOT NULL REFERENCES TEXT_CODEC(text_codec_name),
    PRIMARY KEY (brand, model, text_codec_name),
    CONSTRAINT FK_CLIENT_TCODEC FOREIGN KEY (brand, model) REFERENCES CLIENT(brand, model) ON DELETE CASCADE
);

CREATE TABLE ARTIST(
    artist_id int NOT NULL PRIMARY KEY,
    name varchar(255) NOT NULL,
    picture_url varchar(255) NOT NULL,
    main_specialty varchar(255) NOT NULL,
    birth_date date NOT NULL,
    biography varchar(255) NOT NULL
);

CREATE TABLE FILM(
    film_title varchar(255) NOT NULL,
    film_year int NOT NULL CHECK(film_year > 0),
    abstract varchar(255) NOT NULL,
    recommended_age int NOT NULL CHECK (recommended_age >= 0),
	poster_url varchar(255) NOT NULL,
	PRIMARY KEY(film_title, film_year)
);

CREATE TABLE FILM_PICTURE (
    film_title varchar(255) NOT NULL,
    film_year int NOT NULL,
    film_picture_url varchar(255) NOT NULL,
    PRIMARY KEY (film_title, film_year, film_picture_url),
    CONSTRAINT FK_FILM_PICTURE FOREIGN KEY (film_title, film_year) REFERENCES FILM(film_title, film_year) ON DELETE CASCADE
);

CREATE TABLE FILM_CATEGORY (
    film_category_name varchar(255) NOT NULL PRIMARY KEY
);

CREATE TABLE FILM_A_POUR_CAT (
    film_title varchar(255) NOT NULL,
    film_year int NOT NULL,
    film_category_name varchar(255) NOT NULL REFERENCES FILM_CATEGORY(film_category_name),
    PRIMARY KEY(film_title, film_year, film_category_name),
    CONSTRAINT FK_FILM_CAT FOREIGN KEY (film_title, film_year) REFERENCES FILM(film_title, film_year) ON DELETE CASCADE
);

CREATE TABLE PARTICIPE_AU_FILM(
	artist_id int NOT NULL REFERENCES ARTIST(artist_id) ON DELETE CASCADE,
	film_title varchar(255) NOT NULL,
	film_year int NOT NULL,
	PRIMARY KEY (artist_id, film_title, film_year),
	CONSTRAINT FK_PART_FILM FOREIGN KEY (film_title, film_year) REFERENCES FILM(film_title, film_year) ON DELETE CASCADE
);

CREATE TABLE JOUE_ROLE (
    artist_id int NOT NULL REFERENCES ARTIST(artist_id) ON DELETE CASCADE,
    film_title varchar(255) NOT NULL,
    film_year int NOT NULL,
    character_name varchar(255) NOT NULL,
    PRIMARY KEY (artist_id, film_title, film_year, character_name),
    CONSTRAINT FK_JOUE_FILM FOREIGN KEY (film_title, film_year) REFERENCES FILM(film_title, film_year) ON DELETE CASCADE
);

CREATE TABLE ALBUM (
    album_id int NOT NULL PRIMARY KEY,
    album_title varchar(255) NOT NULL,
    release_date date NOT NULL,
    cover_url varchar(255) NOT NULL,
    artist_name varchar(255) NOT NULL
);

CREATE TABLE TRACK(
    album_id int NOT NULL REFERENCES ALBUM(album_id) ON DELETE CASCADE,
    track_id int NOT NULL,
    track_title varchar(255) NOT NULL,
    length int NOT NULL CHECK(length > 0),
    PRIMARY KEY (album_id, track_id)
);

CREATE TABLE JOUE_DANS (
    artist_id int NOT NULL REFERENCES ARTIST(artist_id) ON DELETE CASCADE,
    album_id int NOT NULL,
    track_id int NOT NULL,
    instrument_name varchar(255) NOT NULL,
    PRIMARY KEY (artist_id, track_id, instrument_name),
    CONSTRAINT FK_JOUE_TRACK FOREIGN KEY (album_id, track_id) REFERENCES TRACK(album_id, track_id) ON DELETE CASCADE
);

CREATE TABLE MUSIC_CATEGORY(
    music_category_name varchar(255) NOT NULL PRIMARY KEY
);

CREATE TABLE ALBUM_A_POUR_CAT (
    album_id int NOT NULL REFERENCES ALBUM(album_id) ON DELETE CASCADE,
    music_category_name varchar(255) NOT NULL REFERENCES MUSIC_CATEGORY(music_category_name),
    PRIMARY KEY(album_id, music_category_name)
);

CREATE TABLE TRACK_A_POUR_CAT (
    album_id int NOT NULL,
    track_id int NOT NULL,
    music_category_name varchar(255) NOT NULL REFERENCES MUSIC_CATEGORY(music_category_name),
    PRIMARY KEY (album_id, track_id, music_category_name),
	CONSTRAINT FK_TRACK_CAT FOREIGN KEY (album_id, track_id) REFERENCES TRACK(album_id, track_id) ON DELETE CASCADE
);

CREATE TABLE FILE_CONCERNE_FILM (
    file_id int NOT NULL REFERENCES MEDIA_FILE(file_id) ON DELETE CASCADE,
    film_title varchar(255) NOT NULL,
    film_year int NOT NULL,
    PRIMARY KEY (file_id, film_title, film_year),
    CONSTRAINT FK_CONCERN_FILM FOREIGN KEY (film_title, film_year) REFERENCES FILM(film_title, film_year)
);

CREATE TABLE FILE_CONCERNE_TRACK(
    file_id int NOT NULL REFERENCES MEDIA_FILE(file_id) ON DELETE CASCADE,
    album_id int NOT NULL,
    track_id int NOT NULL,
    PRIMARY KEY(file_id, album_id, track_id),
    CONSTRAINT FK_CONCERN_TRACK FOREIGN KEY (album_id, track_id) REFERENCES TRACK(album_id, track_id)
);