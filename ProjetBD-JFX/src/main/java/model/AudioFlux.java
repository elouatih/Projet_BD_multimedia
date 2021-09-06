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

public class AudioFlux extends Flux<AudioFlux> {

    private String audioCodecName;
    private int sampling;
    private String language;
    
    private final List<Integer> usedIds;

    public AudioFlux(List<Integer> usedIds) {
        this.usedIds = usedIds;
    }

    @Override
    protected List<ValidationError> validate() {
        List<ValidationError> errors = super.validate();
        if (usedIds.contains(fluxId))
            errors.add(new ValidationError("ID", "Flux with this ID already exists in file"));
        if (audioCodecName == null)
            errors.add(new ValidationError("Codec", "Must be specified"));
        if (sampling != 16 && sampling != 24 && sampling != 32)
            errors.add(new ValidationError("Sampling", "Must be 16, 24, or 32"));
        if (language.length() != 2)
            errors.add(new ValidationError("Language", "Please use a 2-characters ISO code (eg. fr, en)"));
        return errors;
    }

    @Override
    public Dialog<Flux<AudioFlux>> getEditDialog(boolean create) {
        Dialog<Flux<AudioFlux>> dialog = new Dialog<>();
        dialog.setTitle(create ? "Audio flux creation" : "Audio flux edition");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        
        TextField fluxIdField = GuiUtils.getNumericField(create ? 0 : fluxId);
        TextField dataRateField = GuiUtils.getNumericField(create ? 0 : dataRate);
        ComboBox<AudioCodec> audioCodecField = new ComboBox<>();
        audioCodecField.setConverter(new StringConverter<>() {
            @Override
            public String toString(AudioCodec audioCodec) {
                return audioCodec == null ? "Select an audio codec..." : audioCodec.getName();
            }

            @Override
            public AudioCodec fromString(String s) {
                return audioCodecField.getItems().stream().filter(
                        audioCodec -> audioCodec.getName().equals(s)
                ).findFirst().orElse(null);
            }
        });
        OracleDB.executeThen(
                () -> OracleDB.listAudioCodecs(audioCodecField.getItems()),
                () -> {
                    if (audioCodecName != null)
                        audioCodecField.setValue(audioCodecField.getItems().filtered(vc -> vc.getName().equals(audioCodecName)).stream().findFirst().orElse(null));
                }
        );

        ComboBox<Integer> samplingField = new ComboBox<>(FXCollections.observableArrayList(16, 24, 32));
        samplingField.setPromptText("Select an audio sampling...");
        TextField languageField = new TextField();

        dialog.getDialogPane().setContent(GuiUtils.createForm(ImmutableMap.of(
                "Flux ID", fluxIdField,
                "Data rate", dataRateField,
                "Audio codec", audioCodecField,
                "Sampling", samplingField,
                "Language", languageField
        )));

        Platform.runLater(fluxIdField::requestFocus);

        dialog.setResultConverter(b -> null);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            int previousFluxId = fluxId;
            fluxId = Integer.parseInt(fluxIdField.getText());
            dataRate = Integer.parseInt(dataRateField.getText());
            audioCodecName = audioCodecField.getValue() == null ? null : audioCodecField.getValue().getName();
            sampling = samplingField.getValue() == null ? 0 : samplingField.getValue();
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

    public String getAudioCodecName() {
        return audioCodecName;
    }

    public int getSampling() {
        return sampling;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "AudioFlux: ID=" + fluxId + " Codec=" + audioCodecName + " Sampling=" + sampling + " Lang=" + language;
    }
}
