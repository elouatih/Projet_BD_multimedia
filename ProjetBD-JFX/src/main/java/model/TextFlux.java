package model;

import com.google.common.collect.ImmutableMap;
import database.OracleDB;
import gui.GuiUtils;
import gui.ValidationAlert;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import utils.ValidationError;

import java.util.List;

public class TextFlux extends Flux<TextFlux> {

    private String textCodecName;
    private String language;
    
    private final List<Integer> usedIds;

    public TextFlux(List<Integer> usedIds) {
        this.usedIds = usedIds;
    }

    @Override
    protected List<ValidationError> validate() {
        List<ValidationError> errors = super.validate();
        if (usedIds.contains(fluxId))
            errors.add(new ValidationError("ID", "Flux with this ID already exists in file"));
        if (textCodecName == null)
            errors.add(new ValidationError("Codec", "Must be specified"));
        if (language.length() != 2)
            errors.add(new ValidationError("Language", "Please use a 2-characters ISO code (eg. fr, en)"));
        return errors;
    }

    @Override
    public Dialog<Flux<TextFlux>> getEditDialog(boolean create) {
        Dialog<Flux<TextFlux>> dialog = new Dialog<>();
        dialog.setTitle(create ? "Text flux creation" : "Text flux edition");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        
        TextField fluxIdField = GuiUtils.getNumericField(create ? 0 : fluxId);
        TextField dataRateField = GuiUtils.getNumericField(create ? 0 : dataRate);
        ComboBox<TextCodec> textCodecField = new ComboBox<>();
        textCodecField.setConverter(new StringConverter<>() {
            @Override
            public String toString(TextCodec textCodec) {
                return textCodec == null ? "Select a text codec..." : textCodec.getName();
            }

            @Override
            public TextCodec fromString(String s) {
                return textCodecField.getItems().stream().filter(
                        textCodec -> textCodec.getName().equals(s)
                ).findFirst().orElse(null);
            }
        });
        OracleDB.executeThen(
                () -> OracleDB.listTextCodecs(textCodecField.getItems()),
                () -> {
                    if (textCodecName != null)
                        textCodecField.setValue(textCodecField.getItems().filtered(vc -> vc.getName().equals(textCodecName)).stream().findFirst().orElse(null));
                }
        );

        TextField languageField = new TextField();

        dialog.getDialogPane().setContent(GuiUtils.createForm(ImmutableMap.of(
                "Flux ID", fluxIdField,
                "Data rate", dataRateField,
                "Text codec", textCodecField,
                "Language", languageField
        )));

        Platform.runLater(fluxIdField::requestFocus);

        dialog.setResultConverter(b -> null);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            int previousFluxId = fluxId;
            fluxId = Integer.parseInt(fluxIdField.getText());
            dataRate = Integer.parseInt(dataRateField.getText());
            textCodecName = textCodecField.getValue() == null ? null : textCodecField.getValue().getName();
            language = languageField.getText();
            List<ValidationError> errors = validate();
            if (!errors.isEmpty()) {
                fluxId = previousFluxId;
                new ValidationAlert(errors).showAndWait();
                return;
            }
            dialog.setResult(this);
            dialog.close();
        });

        return dialog;
    }

    public String getTextCodecName() {
        return textCodecName;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "TextFlux: ID=" + fluxId + " Codec=" + textCodecName + " Lang=" + language;
    }
}
