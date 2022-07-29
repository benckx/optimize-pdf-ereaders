package dev.encelade.processing.aggregation;

import dev.encelade.ocr.model.TextLine;
import dev.encelade.ocr.model.TextZone;

import java.awt.*;
import java.util.List;
import java.util.function.BiPredicate;

import static java.util.Comparator.comparingInt;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class SameTextBlockPredicate implements BiPredicate<TextLine, List<TextLine>> {

    private static final double PERCENT = 0.6;

    @Override
    public boolean test(TextLine line, List<TextLine> group) {
        if (isEmpty(group)) {
            return true;
        }

        TextLine lowestElement = group.stream().max(comparingInt(TextZone::getY2)).get();
        Rectangle lowestElementRectangle = lowestElement.getDownRectangle(PERCENT);
        Rectangle thisRectangle = line.getUpRectangle(PERCENT);

//        lowestElementRectangle.grow(1, 1);
//        thisRectangle.grow(1, 1);

        return lowestElementRectangle.intersects(thisRectangle);
    }
}
