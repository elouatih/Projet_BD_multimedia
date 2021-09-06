package model;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class Model<T> {
    public abstract T loadFromResultSet(ResultSet resultSet) throws SQLException;
}
