package be.enceladus.scan2ereader.core.ocr.model;

import java.awt.*;
import java.util.logging.Logger;

public class TextLine extends WordsContainerTextZone {

    private final static Logger logger = Logger.getLogger(TextLine.class.getName());

    public TextLine(Rectangle rectangle, Page page) {
        super(rectangle, page);
    }

    @Override
    public String toString() {
        return "Line{page=" + getPage() + ", rectangle=" + getRectangle() + "}";
    }
}
