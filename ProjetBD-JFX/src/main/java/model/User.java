package model;

import com.google.common.collect.ImmutableMap;
import database.OracleDB;
import gui.GuiUtils;
import gui.ValidationAlert;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import utils.ValidationError;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class User extends EditableModel<User> {

    private String email;
    private String firstName;
    private String lastName;
    private int age;
    private int accessCode;
    private String preferredLanguage;

    @Override
    public User loadFromResultSet(ResultSet resultSet) throws SQLException {
        email = resultSet.getString("EMAIL");
        firstName = resultSet.getString("FIRST_NAME");
        lastName = resultSet.getString("LAST_NAME");
        age = resultSet.getInt("AGE");
        accessCode = resultSet.getInt("ACCESS_CODE");
        preferredLanguage = resultSet.getString("PREFERRED_LANGUAGE");
        return this;
    }

    @Override
    protected List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$"))
            errors.add(new ValidationError("Email", "Invalid"));
        if (firstName.isBlank())
            errors.add(new ValidationError("First name", "Cannot be blank"));
        if (lastName.isBlank())
            errors.add(new ValidationError("Last name", "Cannot be blank"));
        if (age < 0)
            errors.add(new ValidationError("Age", "Must be positive"));
        if (accessCode < 0 || accessCode > 9999)
            errors.add(new ValidationError("Access code", "Cannot contain more than 4 digits"));
        if (preferredLanguage.length() != 2)
            errors.add(new ValidationError("Preferred language", "Please use a 2-characters ISO code (eg. fr, en)"));
        return errors;
    }

    @Override
    public Dialog<User> getEditDialog(boolean create) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(create ? "User creation" : "User edition");
        dialog.setHeaderText(create
                ? "Please fill in the details of the new user."
                : "You can edit the details of " + firstName + " " + lastName + " below."
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TextField emailField = new TextField(create ? "" : email);
        TextField firstNameField = new TextField(create ? "" : firstName);
        TextField lastNameField = new TextField(create ? "" : lastName);
        TextField ageField = GuiUtils.getNumericField(create ? 1 : age);
        TextField accessCodeField = GuiUtils.getNumericField(create ? 0 : accessCode);
        TextField preferredLanguageField = new TextField(create ? "" : preferredLanguage);

        dialog.getDialogPane().setContent(GuiUtils.createForm(new ImmutableMap.Builder<String, Control>()
                .put("Email:", emailField)
                .put("First name:", firstNameField)
                .put("Last name:", lastNameField)
                .put("Age:", ageField)
                .put("Access code:", accessCodeField)
                .put("Preferred Language:", preferredLanguageField)
                .build()
        ));

        Platform.runLater(emailField::requestFocus);

        dialog.setResultConverter(b -> null);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            String previousEmail = email;
            email = emailField.getText();
            firstName = firstNameField.getText();
            lastName = lastNameField.getText();
            age = Integer.parseInt(ageField.getText());
            accessCode = Integer.parseInt(accessCodeField.getText());
            preferredLanguage = preferredLanguageField.getText();
            List<ValidationError> errors = validate();
            if (!errors.isEmpty()) {
                email = previousEmail;
                new ValidationAlert(errors).showAndWait();
                return;
            }
            OracleDB.executeThen(
                    () -> {
                        if (create) {
                            OracleDB.addUser(this);
                        } else {
                            OracleDB.updateUser(previousEmail, this);
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

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getAge() {
        return age;
    }

    public int getAccessCode() {
        return accessCode;
    }

    public String getAccessCodeStr() {
        return String.format("%04d", accessCode);
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }
}
