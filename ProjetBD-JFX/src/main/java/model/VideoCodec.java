package model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class VideoCodec extends Model<VideoCodec> {

    private String name;

    @Override
    public VideoCodec loadFromResultSet(ResultSet resultSet) throws SQLException {
        name = resultSet.getString("VIDEO_CODEC_NAME");
        return this;
    }

    public String getName() {
        return name;
    }
}
