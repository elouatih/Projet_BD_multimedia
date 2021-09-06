package model;

import com.google.common.collect.ImmutableMap;
import database.OracleDB;
import gui.GuiUtils;
import gui.ValidationAlert;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import utils.ValidationError;

import java.util.List;

public class VideoFlux extends Flux<VideoFlux> {

    private String videoCodecName;
    private int width;
    private int height;

    private final List<Integer> usedIds;

    public VideoFlux(List<Integer> usedIds) {
        this.usedIds = usedIds;
    }

    @Override
    protected List<ValidationError> validate() {
        List<ValidationError> errors = super.validate();
        if (usedIds.contains(fluxId))
            errors.add(new ValidationError("ID", "Flux with this ID already exists in file"));
        if (videoCodecName == null)
            errors.add(new ValidationError("Codec", "Must be specified"));
        if (width <= 0)
            errors.add(new ValidationError("Width", "Must be positive"));
        if (height <= 0)
            errors.add(new ValidationError("Height", "Must be positive"));
        return errors;
    }

    @Override
    public Dialog<Flux<VideoFlux>> getEditDialog(boolean create) {
        Dialog<Flux<VideoFlux>> dialog = new Dialog<>();
        dialog.setTitle(create ? "Video flux creation" : "Video flux edition");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TextField fluxIdField = GuiUtils.getNumericField(create ? 0 : fluxId);
        TextField dataRateField = GuiUtils.getNumericField(create ? 0 : dataRate);
        ComboBox<VideoCodec> videoCodecField = new ComboBox<>();
        videoCodecField.setConverter(new StringConverter<>() {
            @Override
            public String toString(VideoCodec videoCodec) {
                return videoCodec == null ? "Select a video codec..." : videoCodec.getName();
            }

            @Override
            public VideoCodec fromString(String s) {
                return videoCodecField.getItems().stream().filter(
                        videoCodec -> videoCodec.getName().equals(s)
                ).findFirst().orElse(null);
            }
        });
        OracleDB.executeThen(
                () -> OracleDB.listVideoCodecs(videoCodecField.getItems()),
                () -> {
                    if (videoCodecName != null)
                        videoCodecField.setValue(videoCodecField.getItems().filtered(vc -> vc.getName().equals(videoCodecName)).stream().findFirst().orElse(null));
                }
        );

        TextField widthField = GuiUtils.getNumericField(create ? 1920 : width);
        TextField heightField = GuiUtils.getNumericField(create ? 1080 : height);

        dialog.getDialogPane().setContent(GuiUtils.createForm(ImmutableMap.of(
                "Flux ID", fluxIdField,
                "Data rate", dataRateField,
                "Video codec", videoCodecField,
                "Width", widthField,
                "Height", heightField
        )));

        Platform.runLater(fluxIdField::requestFocus);

        dialog.setResultConverter(b -> null);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            int previousFluxId = fluxId;
            fluxId = Integer.parseInt(fluxIdField.getText());
            dataRate = Integer.parseInt(dataRateField.getText());
            videoCodecName = videoCodecField.getValue() == null ? null : videoCodecField.getValue().getName();
            width = Integer.parseInt(widthField.getText());
            height = Integer.parseInt(heightField.getText());
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

    public String getVideoCodecName() {
        return videoCodecName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "VideoFlux: ID=" + fluxId + " Codec=" + videoCodecName + " Resolution=" + width + "x" + height;
    }
}
