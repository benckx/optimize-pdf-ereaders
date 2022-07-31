package dev.encelade.ocr.model;

import dev.encelade.ocr.OCR;
import dev.encelade.processing.aggregation.Aggregator;
import dev.encelade.processing.aggregation.SameLinePredicate;
import dev.encelade.processing.aggregation.SameTextBlockPredicate;
import dev.encelade.processing.corrections.AngleCorrection;
import dev.encelade.processing.corrections.AngleCorrectionQuadrilateral;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;

import static dev.encelade.ocr.model.Side.*;
import static dev.encelade.ocr.model.TextZone.mergeHorizontally;
import static dev.encelade.utils.GeometryUtils.intersection;
import static dev.encelade.utils.GeometryUtils.mergeBlocks;
import static java.util.Arrays.asList;
import static java.util.Collections.min;
import static java.util.Comparator.comparingDouble;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * The image data is stored as a byte[] instead of a {@link BufferedImage} to consume less space in the heap.
 */
public class Page {

    private final static float MAX_RATIO_INTERSECTING_WORDS_TWO_PAGES_LAYOUT = 0.01f;

    @Getter
    @Setter
    private List<Integer> idx;

    private boolean cachedEnabled = false;
    private BufferedImage imageCache;
    private byte[] imageData;
    private int imageWidth;
    private int imageHeight;

    @Getter
    @Setter
    private List<Word> words;

    @Getter
    private List<TextLine> lines;

    @Getter
    private List<TextBlock> textBlocks;

    @Getter
    private List<Paragraph> paragraphs;

    @Getter
    private List<WhiteSpace> whiteSpaces;

    private AngleCorrection angleCorrection;

    @Getter
    @Setter
    private double correctedAngleValue;

    @Getter
    private Line2D dividingLine;

    @Getter
    private int splitIn = 1;

    private static byte[] compressImage(BufferedImage bufferedImage) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BufferedImage decompressImage(byte[] imageData) {
        try {
            return ImageIO.read(new ByteArrayInputStream(imageData));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void enableImageCache() {
        this.cachedEnabled = true;
    }

    public void setOriginalImage(BufferedImage originalImage) {
        this.imageWidth = originalImage.getWidth();
        this.imageHeight = originalImage.getHeight();
        this.imageData = compressImage(originalImage);
    }

    public BufferedImage getOriginalImage() {
        if (imageCache != null) {
            return imageCache;
        } else {
            BufferedImage image = decompressImage(imageData);
            if (cachedEnabled) {
                imageCache = image;
            }
            return image;
        }
    }

    public void deleteImageData() {
        this.imageData = null;
        this.imageCache = null;
    }

    public int getWidth() {
        return imageWidth;
    }

    public int getHeight() {
        return imageHeight;
    }

    public int countLines() {
        return lines == null ? 0 : lines.size();
    }

    public double getAverageConfidence() {
        return words.stream().mapToDouble(Word::getConfidence).average().getAsDouble();
    }

    public double getStandardDeviationConfidence() {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        words.stream().mapToDouble(Word::getConfidence).forEach(stats::addValue);
        return stats.getStandardDeviation();
    }

    public double getMinConfidence() {
        return words.stream().mapToDouble(Word::getConfidence).min().getAsDouble();
    }

    public double getMaxConfidence() {
        return words.stream().mapToDouble(Word::getConfidence).max().getAsDouble();
    }

    public int computeParagraphXOffSet() {
        return computeXOffSet(paragraphs);
    }

    private static <TZ extends TextZone> int computeXOffSet(List<TZ> input) {
        if (isNotEmpty(input)) {
            return min(input, comparingInt(TextZone::getX1)).getX1();
        }
        return 0;
    }

    public boolean hasCorrectiveAngleData() {
        return angleCorrection != null && angleCorrection.hasCorrectiveAngleData();
    }

    public double getAngleQuadrilateralsOffset() {
        if (!hasCorrectiveAngleData()) {
            return 0;
        }

        return angleCorrection.getAngleQuadrilateralsOffset();
    }

    public void detectCorrectiveAngle() {
        if (isNotEmpty(textBlocks)) {
            angleCorrection = new AngleCorrection(this);
            angleCorrection.detect();
        }
    }

    public AngleCorrectionQuadrilateral getClockWiseQuadrilateral() {
        return angleCorrection.getClockWiseQuadrilateral();
    }

    public AngleCorrectionQuadrilateral getCounterClockWiseQuadrilateral() {
        return angleCorrection.getCounterClockWiseQuadrilateral();
    }

    public boolean isLeaningClockwise() {
        return angleCorrection.getClockWiseQuadrilateral().getLengthsOffset() < angleCorrection.getCounterClockWiseQuadrilateral().getLengthsOffset();
    }

    public int getWordsTotalArea() {
        return getTotalArea(words);
    }

    public double getWordsOverlappingArea() {
        return getOverlappingArea(words);
    }

    public double getWordsOverlappingRatio() {
        return getWordsOverlappingArea() / getWordsTotalArea();
    }

    public int getLinesTotalArea() {
        return getTotalArea(lines);
    }

    public double getLinesOverlappingArea() {
        return getOverlappingArea(lines);
    }

    public boolean isNotWithinMargin(TextZone textZone) {
        return !isWithinMargin(textZone);
    }

    private boolean isWithinMargin(TextZone textZone) {
        return getMargins()
                .stream()
                .anyMatch(rectangle -> rectangle.intersects(textZone.getRectangle()));
    }

    public List<Rectangle> getMargins() {
        List<Side> allSides = listAllSides();

        if (idx.size() == 2) {
            switch (idx.get(1)) {
                case 1:
                    allSides.remove(RIGHT);
                    break;
                case 2:
                    allSides.remove(LEFT);
                    break;
            }
        }

        return allSides.stream().map(side -> side.getMarginRectangle(getRectangle())).collect(toList());
    }

    private static <TZ extends TextZone> int getTotalArea(List<TZ> textZones) {
        if (textZones == null) {
            return 0;
        }

        return textZones.stream().mapToInt(TextZone::getArea).sum();
    }

    private static <TZ extends TextZone> double getOverlappingArea(List<TZ> textZones) {
        double overlappingArea = 0;

        if (textZones != null) {
            for (TextZone textZone1 : textZones) {
                for (TextZone textZone2 : textZones) {
                    if (!Objects.equals(textZone1, textZone2)) {
                        Rectangle intersection = intersection(textZone1.getRectangle(), textZone2.getRectangle());
                        if (intersection != null) {
                            overlappingArea += intersection.width * intersection.height * 0.5;
                        }
                    }
                }
            }
        }

        return overlappingArea;
    }

    public Page correctAngle() {
        if (angleCorrection != null) {
            Page corrected = angleCorrection.process();
            angleCorrection = null;
            return corrected;
        } else {
            return this;
        }
    }

    public boolean hasLayoutData() {
        return dividingLine != null;
    }

    public boolean detectTwoPagesLayout() {
        if (isNotEmpty(words)) {
            int from = (int) (getWidth() * 0.4);
            int to = (int) (getWidth() * 0.6);
            int nbrOfTestLines = 20;
            int stepSize = (int) ((to - from) / (double) nbrOfTestLines);

            Set<Map.Entry<Line2D.Double, Double>> entries = IntStream
                    .iterate(from, x -> x + stepSize).limit(nbrOfTestLines)
                    .parallel()
                    .mapToObj(x -> new Line2D.Double(x, 0, x, getHeight()))
                    .collect(toMap(
                            line -> line,
                            line -> countIntersectingWords(line) / words.size()) // Line2D -> intersecting words ratio
                    )
                    .entrySet();

            // entries.forEach(entry -> logger.info(PERCENT_FORMAT.format(entry.getValue())));

            dividingLine = entries
                    .stream()
                    .min(comparingDouble(Map.Entry::getValue))
                    .map(entry -> entry.getValue() <= MAX_RATIO_INTERSECTING_WORDS_TWO_PAGES_LAYOUT ? entry.getKey() : null)
                    .orElse(null);
        }

        return dividingLine != null;
    }

    // TODO: maybe count word area
    private double countIntersectingWords(Line2D dividingLine) {
        return words.stream()
                .filter(word -> word.intersects(dividingLine))
                .count();
    }

    private List<Rectangle> getLayoutRectangles() {
        assert dividingLine != null && dividingLine.getX1() == dividingLine.getX2();

        int x = (int) dividingLine.getX1();

        Rectangle leftSide = new Rectangle(0, 0, x, getHeight());
        Rectangle rightSide = new Rectangle(x, 0, getWidth() - x, getHeight());

        assert leftSide.width * leftSide.height + rightSide.width * rightSide.height == getArea();
        assert !leftSide.intersects(rightSide);

        return asList(leftSide, rightSide);
    }

    public List<Page> splitForLayout() {
        if (!hasLayoutData()) {
            throw new IllegalStateException();
        }

        assert idx.size() == 1;

        return getLayoutRectangles().stream()
                .map(layoutRectangle -> {
                    // TODO: can we still use the division without re-detecting? if the PDF quality is good enough? --> if the confidence std dev is high (>20)
                    BufferedImage subImage = getOriginalImage().getSubimage(layoutRectangle.x, layoutRectangle.y, layoutRectangle.width, layoutRectangle.height);
                    int subIdx = (layoutRectangle.x == 0 ? 1 : 2);
                    Page split = new OCR().analyze(subImage, idx.get(0), subIdx);
                    split.splitIn = 2;
                    return split;
                })
                .collect(toList());
    }

    public void detectTextBlocks() {
        detectLinesFromWords();
        mergeLinesHorizontally();
        detectTextBlockFromLines();
        mergeTextBlocksHorizontally(); // TODO: needed?
    }

    public void detectLinesFromWords() {
        if (isNotEmpty(words)) {
            Aggregator<Word> aggregator = new Aggregator<>(new SameLinePredicate(), words);

            // TODO: make the merge in the aggregator as well
            lines = aggregator.buildGroups()
                    .stream()
                    .map(group -> new TextLine(mergeBlocks(group), this))
                    .collect(toList());
        }
    }

    private void detectTextBlockFromLines() {
        if (isNotEmpty(lines)) {
            Aggregator<TextLine> aggregator = new Aggregator<>(new SameTextBlockPredicate(), lines);

            // TODO: make the merge in the aggregator as well
            textBlocks = aggregator.buildGroups()
                    .stream()
                    .map(group -> new TextBlock(mergeBlocks(group), this))
                    .filter(TextBlock::isValid)
                    .collect(toList());

            textBlocks = cleanUp(textBlocks);
        }
    }

    private static List<TextBlock> cleanUp(List<TextBlock> textBlocks) {
        Set<TextBlock> allTextBlocks = new HashSet<>(textBlocks);
        boolean didMerge;
        do {
            didMerge = false;
            List<TextBlock> toRemove = new ArrayList<>();
            List<TextBlock> merged = new ArrayList<>();
            for (TextBlock textZone1 : allTextBlocks) {
                for (TextBlock textZone2 : allTextBlocks) {
                    if (!Objects.equals(textZone1, textZone2) && canBeMerged(textZone1, textZone2)) {
                        merged.add(merge(textZone1, textZone2));
                        toRemove.add(textZone1);
                        toRemove.add(textZone2);
                        didMerge = true;
                    }
                }
            }
            allTextBlocks.removeAll(toRemove);
            allTextBlocks.addAll(merged);
        } while (didMerge);

        List<TextBlock> filterTextBlocks = new ArrayList<>(allTextBlocks);
        filterTextBlocks.sort(comparingInt(TextZone::getY1));
        return filterTextBlocks;
    }

    private static TextBlock merge(TextBlock textBlock1, TextBlock textBlock2) {
        Rectangle rectangle = mergeBlocks(asList(textBlock1, textBlock2));
        return new TextBlock(rectangle, textBlock1.getPage());
    }

    private static boolean canBeMerged(TextBlock textBlock1, TextBlock textBlock2) {
        return textBlock1.getRectangle().intersects(textBlock2.getRectangle());
    }

    /**
     * Get both paragraphs and white spaces in the order they appear on the page.
     */
    public List<TextZone> getElementsForPagination() {
        List<TextZone> elementsToRender = new ArrayList<>();
        if (isNotEmpty(paragraphs)) {
            elementsToRender.addAll(paragraphs);
        }
        if (isNotEmpty(whiteSpaces)) {
            elementsToRender.addAll(whiteSpaces);
        }
        elementsToRender.sort(comparingInt(TextZone::getY1));

        return elementsToRender;
    }

    private void detectWhiteSpaces() {
        if (isNotEmpty(textBlocks)) { // FIXME: paragraphs?
            // scan and collect
            List<Integer> whiteSpaceCoordinates = new ArrayList<>();

            Line2D.Double scanner;
            boolean isIntersecting = true;
            for (int y = 0; y < getHeight(); y += 1) {
                scanner = new Line2D.Double(0, y, getWidth(), y);
                if (textIntersects(scanner)) {
                    if (!isIntersecting) {
                        whiteSpaceCoordinates.add(y);
                    }
                    isIntersecting = true;
                } else {
                    if (isIntersecting) {
                        whiteSpaceCoordinates.add(y);
                    }
                    isIntersecting = false;
                }
            }
            if (!isIntersecting) {
                whiteSpaceCoordinates.add(getHeight());
            }
            assert whiteSpaceCoordinates.size() % 2 == 0;

            // build
            whiteSpaces = IntStream
                    .iterate(0, i -> i + 2)
                    .limit(whiteSpaceCoordinates.size() / 2)
                    .mapToObj(i -> {
                        int y1 = whiteSpaceCoordinates.get(i);
                        int y2 = whiteSpaceCoordinates.get(i + 1);
                        return new Rectangle(0, y1, getWidth(), y2 - y1);
                    })
                    .map(rectangle -> new WhiteSpace(rectangle, this))
                    .collect(toList());
        }
    }

    private boolean textIntersects(Shape shape) {
        return paragraphs
                .stream()
                .anyMatch(paragraph -> shape.intersects(paragraph.getRectangle()));
    }

    public void detectParagraphs() {
        if (isNotEmpty(textBlocks)) {
            paragraphs = textBlocks
                    .stream()
                    .flatMap(textBlock -> textBlock.detectParagraphs().stream())
                    .sorted(comparingInt(TextZone::getY1))
                    .collect(toList());

            if (isNotEmpty(paragraphs)) {
                detectWhiteSpaces();
            }

            mergeParagraphsHorizontally();
        }
    }

    private void mergeParagraphsHorizontally() {
        paragraphs = mergeHorizontally(paragraphs);
    }

    private void mergeLinesHorizontally() {
        lines = mergeHorizontally(lines);
    }

    private void mergeTextBlocksHorizontally() {
        textBlocks = mergeHorizontally(textBlocks);
    }

    public Rectangle getRectangle() {
        return new Rectangle(0, 0, imageWidth, imageHeight);
    }

    private int getArea() {
        return getWidth() * getHeight();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return Objects.equals(idx, page.idx);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idx);
    }

    @Override
    public String toString() {
        return "Page " + idx;
    }
}
