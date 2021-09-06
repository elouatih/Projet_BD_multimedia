package model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TextCodec extends Model<TextCodec> {

    private String name;

    @Override
    public TextCodec loadFromResultSet(ResultSet resultSet) throws SQLException {
        name = resultSet.getString("TEXT_CODEC_NAME");
        return this;
    }

    public String getName() {
        return name;
    }
}
