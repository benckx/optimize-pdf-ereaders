package dev.encelade.ocr.model;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static dev.encelade.ocr.model.Side.LEFT;
import static dev.encelade.ocr.model.Side.RIGHT;
import static java.lang.Math.abs;
import static java.util.Collections.min;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparingInt;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class TextBlock extends WordsContainerTextZone {

    private final static Logger logger = Logger.getLogger(TextBlock.class.getName());

    public TextBlock(Rectangle rectangle, Page page) {
        super(rectangle, page);
    }

    public List<Paragraph> detectParagraphs() {
        List<Word> wordsOnTheLeft = getWordsOnSideBorders().get(LEFT);

        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Word word : wordsOnTheLeft) {
            stats.addValue(word.getX1());
        }
        double standardDeviation = stats.getStandardDeviation();
        double average = stats.getMean();

        List<Integer> yCuts = new ArrayList<>();
        for (Word word : wordsOnTheLeft) {
            double delta = abs(word.getX1() - average);
            if (delta > 2 * standardDeviation) {
                if (word.getY1() > getY1()) {
                    List<Word> wordsOnTheSameLine = getWordsOnTheSideOf(word, RIGHT);

                    int yCut;
                    if (isNotEmpty(wordsOnTheSameLine)) {
                        // we take the word with the smallest Y1, so we don't cut thought other words on the line
                        Word minLineY1 = min(wordsOnTheSameLine, comparingInt(TextZone::getY1));
                        yCut = minLineY1.getY1();
                    } else {
                        yCut = word.getY1();
                    }

                    yCut -= this.getY1(); // relative

                    if (isValidCutToAdd(yCut, yCuts)) {
                        yCuts.add(yCut);
                    }
                }
            }
        }

        if (isEmpty(yCuts)) {
            return singletonList(new Paragraph(getRectangle(), getPage()));
        } else {
            return sliceOnY(yCuts, Paragraph.class);
        }
    }
}
