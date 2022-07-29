package dev.encelade.ocr.model;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public abstract class /**/WordsContainerTextZone extends TextZone {

    private Map<Side, List<Word>> wordsOnSidesBorders;

    public WordsContainerTextZone(Rectangle rectangle, Page page) {
        super(rectangle, page);
    }

    public List<Word> getIncludedWords() {
        return getPage().getWords()
                .stream()
                .filter(word -> word.isMostlyIncludedIn(this))
                .collect(toList());
    }

    // TODO: call wordsOnSidesBorders [?]
    List<Word> getWordsOnTheSideBorder(Side side) {
        return getIncludedWords()
                .stream()
                .filter(word -> isOnTheSide(side, word))
                .peek(word -> word.mark(side.getColor()))
                .collect(toList());
    }

    // TODO: call wordsOnSidesBorders [?]
    List<Word> getWordsOnTheSideOf(TextZone textZone, Side side) {
        Rectangle sameLine = textZone.getControlRectangle(side);

        return getIncludedWords()
                .stream()
                .filter(word -> word.intersects(sameLine))
                .collect(toList());
    }

    /**
     * Words that don't have other word on their left side are on considered to be the left side border.
     */
    private boolean isOnTheSide(Side side, Word wordToTest) {
        Rectangle zoneToControl = wordToTest.getControlRectangle(side);

        return getIncludedWords()
                .stream()
                .filter(word -> !Objects.equals(wordToTest, word))
                .noneMatch(otherWord -> otherWord.intersects(zoneToControl));
    }

    public int countLines() {
        return Math.max(getWordsOnTheSideBorder(Side.LEFT).size(), getWordsOnTheSideBorder(Side.RIGHT).size());
    }

    public Map<Side, List<Word>> getWordsOnSideBorders() {
        if (wordsOnSidesBorders == null) {
            wordsOnSidesBorders = new HashMap<>();
            for (Side side : Side.values()) {
                wordsOnSidesBorders.put(side, getWordsOnTheSideBorder(side));
            }
        }

        return wordsOnSidesBorders;
    }

    public String getText() {
        StringBuilder builder = new StringBuilder();
        getIncludedWords().forEach(word -> {
            builder.append(word.getText());
            builder.append(" ");
        });
        return builder.toString();
    }

    public Rectangle getUpRectangle(double percent) {
        return getVerticallyExpandedRectangle(percent, -1);
    }

    public Rectangle getDownRectangle(double percent) {
        return getVerticallyExpandedRectangle(percent, +1);
    }

    private Rectangle getVerticallyExpandedRectangle(double percent, int side) {
        List<Word> includedWords = getIncludedWords();

        int deltaHeight;
        if (isNotEmpty(includedWords)) {
            deltaHeight = includedWords.stream().max(comparingInt(Word::getHeight)).get().getHeight();
        } else {
            deltaHeight = getHeight();
        }
        deltaHeight = (int) (deltaHeight * percent);

//        double h = (int) ((getHeight() * percent) / 2);
//        int x = getRectangle().x;
//        int y = getRectangle().y;
//        int width = getRectangle().width;
//        int height = getRectangle().height;
//
//        Rectangle expandedRectangle = new Rectangle(x, y, width, height);
//        expandedRectangle.grow(0, (int) h);
//        expandedRectangle.translate(0, (int) (h * side));
//        return expandedRectangle;

        int y;
        if (side < 0) {
            y = getRectangle().y - deltaHeight;
        } else {
            y = getRectangle().y + getRectangle().height;
        }

        return new Rectangle(getRectangle().x, y, getRectangle().width, deltaHeight);
    }
}
