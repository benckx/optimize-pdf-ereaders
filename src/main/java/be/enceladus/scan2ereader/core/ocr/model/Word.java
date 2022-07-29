package be.enceladus.scan2ereader.core.ocr.model;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.text.DecimalFormat;

public class Word extends TextZone {

    private static final DecimalFormat CONFIDENCE_FORMAT = new DecimalFormat("00%");

    @Getter
    @Setter
    private String text;

    @Getter
    @Setter
    private float confidence;

    public Word(Rectangle rectangle, Page page) {
        super(rectangle, page);
    }

    @Override
    public boolean isValid() {
        if (!text.matches("^[a-zA-Z0-9]*$") && confidence <= 0.5) {
            return false;
        }

        return super.isValid();
    }

    @Override
    public String toString() {
        Rectangle rectangle = super.getRectangle();

        return "Word[" +
                "\"" + text + '\"' +
                ", " + CONFIDENCE_FORMAT.format(confidence) +
                ", [" +
                "x=" + rectangle.x +
                ", y=" + rectangle.y +
                ", width=" + rectangle.width +
                ", height=" + rectangle.height +
                "]" +
                "]";
    }
}
