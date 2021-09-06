package model;

import javafx.scene.control.Dialog;
import utils.ValidationError;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AlbumCategory extends EditableModel<AlbumCategory>{
    private String name;



    @Override
    protected List<ValidationError> validate() {
        return null;
    }

    @Override
    public Dialog<AlbumCategory> getEditDialog(boolean create) {
        return null;
    }

    @Override
    public AlbumCategory loadFromResultSet(ResultSet resultSet) throws SQLException {
        return null;
    }

    public String getName() {
        return name;
    }
}
