package utils;

public class ValidationError {
    private final String field;
    private final String message;

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    @Override
    public String toString() {
        return field + ": " + message;
    }
}
