package dev.encelade.ocr.model;

import java.awt.*;

public class WhiteSpace extends TextZone {

    public WhiteSpace(Rectangle rectangle, Page page) {
        super(rectangle, page);
    }

    @Override
    public String toString() {
        return "White Space [" + getHeight() + "]";
    }
}
