package be.enceladus.scan2ereader.core.ocr.model;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;

public class Paragraph extends WordsContainerTextZone {

    private final static Logger logger = Logger.getLogger(Paragraph.class.getName());

    public Paragraph(Rectangle rectangle, Page page) {
        super(rectangle, page);
    }

    public List<Paragraph> breakDown(int pieces) {
        if (pieces < 2) {
            throw new IllegalArgumentException();
        }

        logger.info("cut " + toString() + " into " + pieces);

        List<Integer> yCuts = new ArrayList<>();
        double percent = 1d / (double) pieces;
        logger.info("percent: " + percent);
        for (int i = 1; i < pieces; i++) {
            int yCut = adjustCutY(getHeight() * percent * i);
            if (isValidCutToAdd(yCut, yCuts)) {
                yCuts.add(yCut);
            }
        }

        logger.info("yCuts: " + yCuts);

        if (yCuts.size() < 1) {
            return singletonList(this);
        } else {
            return sliceOnY(yCuts, Paragraph.class);
        }
    }

    /**
     * Find the value of y with the minimum number of collisions with words
     */
    private int adjustCutY(double relativeCutY) {
        if (relativeCutY <= 0) {
            throw new IllegalArgumentException();
        }

        List<Word> words = getIncludedWords();

        int downMargin = (int) (0.03 * getHeight());
        int upMargin = (int) (0.03 * getHeight());

        int from = Math.max(0, (int) relativeCutY - downMargin);
        int to = Math.min(getHeight(), (int) relativeCutY + upMargin);

        logger.info(relativeCutY + " to adjust in [" + from + "-" + to + "] [margins: " + downMargin + ", " + upMargin + "]");

        Line2D.Double minimalCollisionsLine = IntStream
                .iterate(from, y -> y + 1)
                .limit(to)
                .mapToObj(y -> new Line2D.Double(0, y + getY1(), getWidth(), y + getY1()))
                .min(comparing(line -> words.stream().filter(word -> word.intersects(line)).count()))
                .get();

        assert minimalCollisionsLine.y1 == minimalCollisionsLine.y2;

        logger.info("resulting yCut --> " + (minimalCollisionsLine.y1 - getY1()));
        return (int) (minimalCollisionsLine.y1 - getY1());
    }

    @Override
    public String toString() {
        return "Paragraph{page=" + getPage() + ", rectangle=" + getRectangle() + "}";
    }
}
