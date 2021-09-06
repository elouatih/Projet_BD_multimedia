package model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AudioCodec extends Model<AudioCodec> {

    private String name;

    @Override
    public AudioCodec loadFromResultSet(ResultSet resultSet) throws SQLException {
        name = resultSet.getString("AUDIO_CODEC_NAME");
        return this;
    }

    public String getName() {
        return name;
    }
}
