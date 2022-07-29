package dev.encelade.processing.aggregation;

import dev.encelade.ocr.model.Word;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.logging.Logger;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class SameLinePredicate implements BiPredicate<Word, List<Word>> {

    private final static Logger logger = Logger.getLogger(SameLinePredicate.class.getName());

    @Override
    public boolean test(Word word, List<Word> group) {
        if (isEmpty(group)) {
            return true;
        }

        Word lastAddedToGroup = Iterables.getLast(group);
        return word.getWidthExpandedRectangle().intersects(lastAddedToGroup.getWidthExpandedRectangle());
    }
}
