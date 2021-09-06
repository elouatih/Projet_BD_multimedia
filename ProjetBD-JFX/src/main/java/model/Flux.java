package model;

import utils.ValidationError;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class Flux<T> extends EditableModel<Flux<T>> {

    protected int fileId;
    protected int fluxId;
    protected int dataRate;

    @Override
    public Flux<T> loadFromResultSet(ResultSet resultSet) throws SQLException {
        fileId = resultSet.getInt("FILE_ID");
        fluxId = resultSet.getInt("FLUX_ID");
        dataRate = resultSet.getInt("DATA_RATE");
        return this;
    }

    @Override
    protected List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if (fileId < 0)
            errors.add(new ValidationError("File ID", "Must be positive"));
        if (fluxId < 0)
            errors.add(new ValidationError("Flux ID", "Must be positive"));
        if (dataRate < 0)
            errors.add(new ValidationError("Data rate", "Must be positive"));
        return errors;
    }

    public int getFileId() {
        return fileId;
    }

    public int getFluxId() {
        return fluxId;
    }

    public int getDataRate() {
        return dataRate;
    }
}
