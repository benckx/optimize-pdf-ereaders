package dev.encelade.processing.corrections;

import dev.encelade.ocr.model.TextBlock;
import dev.encelade.ocr.OCR;
import dev.encelade.ocr.model.Page;
import dev.encelade.ocr.model.Side;
import dev.encelade.utils.ImageUtils;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static dev.encelade.ocr.model.Side.getAnglesPairs;
import static dev.encelade.ocr.model.Side.listAllSides;
import static dev.encelade.utils.GeometryUtils.distanceBetween;
import static dev.encelade.utils.GeometryUtils.getIntersectionPoint;
import static java.lang.Math.abs;
import static java.util.Collections.emptyList;
import static java.util.Collections.max;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

public class AngleCorrection {

    private final static double MIN_ANGLE = 0.03; // TODO: increase to avoid unnecessary work
    private final static double MAX_ANGLE = 10;
    private static final int MIN_LINES_TO_DETECT_ANGLES = 5;

    private Page originalPage;
    private Page correctedMostLikely;
    private Page correctLeastLikely;

    private AngleCorrectionQuadrilateral clockWiseQuadrilateral;
    private AngleCorrectionQuadrilateral counterClockWiseQuadrilateral;

//    private static Comparator<Page> resultComparator = comparingInt(Page::getLinesTotalArea);
//    private static Comparator<Page> resultComparator = comparingDouble(Page::getWordsOverlappingArea);

    public AngleCorrection(Page originalPage) {
        this.originalPage = originalPage;
    }

    public void detect() {
        TextBlock largestTextBlock = max(originalPage.getTextBlocks(), comparingInt(TextBlock::getArea));
        if (largestTextBlock.countLines() >= MIN_LINES_TO_DETECT_ANGLES) {
            clockWiseQuadrilateral = detectCorrectiveAngle(largestTextBlock, true);
            counterClockWiseQuadrilateral = detectCorrectiveAngle(largestTextBlock, false);
        }
    }

    public Page process() {
        if (originalPage.countLines() > 0 && hasCorrectiveAngleData()) {
            correctedMostLikely = rotateInMostLikelyDirection();

            ImageUtils.dumpToImageFile("MOST_LIKELY_", correctedMostLikely);

            if (correctedMostLikely != null) {
                // if result seems worse or not very different, or angle is too big or too small; take the best of the 3
                if (seemsMoreSkewedThan(correctedMostLikely, originalPage)) {
                    correctLeastLikely = rotateInLeastLikelyDirection();
                    ImageUtils.dumpToImageFile("LEAST_LIKELY_", correctLeastLikely);
                    return getBestChoice();
                } else {
                    return correctedMostLikely;
                }
            }

        }

        return originalPage;
    }

    // must be more "sensitive" than "moreSkewedThan" comparator (i.e. we must check if there is the slightest doubt)
    private boolean seemsMoreSkewedThan(Page page1, Page page2) {
        return page1.countLines() < page2.countLines()
                || page1.getLinesOverlappingArea() > page2.getLinesOverlappingArea()
                || page1.getLinesTotalArea() > page2.getLinesTotalArea()
                || page1.getLinesOverlappingArea() > page2.getLinesOverlappingArea()
                || page1.getWordsTotalArea() > page2.getWordsTotalArea();
    }

    private Comparator<Page> moreSkewedThan = (page1, page2) -> {
        int compare = Double.compare(page1.getWordsOverlappingArea(), page2.getWordsOverlappingArea());
        if (compare != 0) {
            return compare;
        }

        compare = Double.compare(page1.getLinesOverlappingArea(), page2.getLinesOverlappingArea());
        if (compare != 0) {
            return compare;
        }

        return Double.compare(page1.getLinesTotalArea(), page2.getLinesTotalArea());
    };

    private Page getBestChoice() {
        return of(originalPage, correctedMostLikely, correctLeastLikely)
                .filter(Objects::nonNull)
                .min(moreSkewedThan)
                .orElseThrow(IllegalStateException::new);
    }

    private AngleCorrectionQuadrilateral getMostLikelyQuadrilateral() {
        if (isLeaningClockwise()) {
            return clockWiseQuadrilateral;
        } else {
            return counterClockWiseQuadrilateral;
        }
    }

    private AngleCorrectionQuadrilateral getLeastLikelyQuadrilateral() {
        if (isLeaningClockwise()) {
            return counterClockWiseQuadrilateral;
        } else {
            return clockWiseQuadrilateral;
        }
    }

    private Page rotateInMostLikelyDirection() {
        double angle = getMostLikelyQuadrilateral().getCorrectiveAngle();
        return rotateByAngle(angle);
    }

    private Page rotateInLeastLikelyDirection() {
        double angle = getLeastLikelyQuadrilateral().getCorrectiveAngle();
        return rotateByAngle(angle);
    }

    private Page rotateByAngle(double angle) {
        if (abs(angle) > MIN_ANGLE && abs(angle) < MAX_ANGLE) {
            BufferedImage rotatedImage = ImageUtils.rotate(originalPage.getOriginalImage(), angle);
            Page corrected = new OCR().analyze(rotatedImage, originalPage.getIdx());
            corrected.detectTextBlocks();
            corrected.setCorrectedAngleValue(angle);
            return corrected;
        } else {
            return originalPage;
        }
    }

    private boolean isLeaningClockwise() {
//        return clockWiseQuadrilateral.getLengthsOffset() < counterClockWiseQuadrilateral.getLengthsOffset();
        return clockWiseQuadrilateral.getAnglesOffset() < counterClockWiseQuadrilateral.getAnglesOffset();
    }

    /**
     * Offset between the clockwise quadrilateral and the counter clockwise quadrilateral (distance between matching points).
     * <br>If low, the text is straight
     * <br>If high, the text is leaned
     */
    public double getAngleQuadrilateralsOffset() {
        double offset = 0;

        for (List<Side> pair : getAnglesPairs()) {
//        for (List<Side> pair : asList(asList(TOP, LEFT), asList(BOTTOM, LEFT))) {
            Point p1 = clockWiseQuadrilateral.getQuadrilateralPointAt(pair.get(0), pair.get(1));
            Point p2 = counterClockWiseQuadrilateral.getQuadrilateralPointAt(pair.get(0), pair.get(1));
            offset += distanceBetween(p1, p2);
        }

        return offset;
    }

    public AngleCorrectionQuadrilateral getClockWiseQuadrilateral() {
        return clockWiseQuadrilateral;
    }

    public AngleCorrectionQuadrilateral getCounterClockWiseQuadrilateral() {
        return counterClockWiseQuadrilateral;
    }

    public boolean hasCorrectiveAngleData() {
        return clockWiseQuadrilateral != null
                && clockWiseQuadrilateral.isValid()
                && counterClockWiseQuadrilateral != null
                && counterClockWiseQuadrilateral.isValid();
    }

    private static AngleCorrectionQuadrilateral detectCorrectiveAngle(TextBlock textBlock, boolean clockWise) {
        List<Line2D> lines = listAllSides(clockWise)
                .stream()
                .map(pointPlacementSide -> {
                    Point oppositePoint = pointPlacementSide.getOppositePoint(textBlock, clockWise);
                    Side wordsSide = pointPlacementSide.previous(clockWise);

                    return pointPlacementSide.getPoints(textBlock, clockWise)
                            .stream()
                            .map(p -> new Line2D.Double(p.x, p.y, oppositePoint.x, oppositePoint.y))
                            .max(comparingInt(line -> countIntersects(line, textBlock, wordsSide)))
                            .get();
                })
                .collect(toList());

        AngleCorrectionQuadrilateral angleCorrectionQuadrilateral = new AngleCorrectionQuadrilateral();
        angleCorrectionQuadrilateral.setLinesData(buildQuadrilateral(lines));
        return angleCorrectionQuadrilateral;
    }

    private static int countIntersects(Line2D line, TextBlock textBlock, Side side) {
        return (int) textBlock.getWordsOnSideBorders()
                .get(side)
                .stream()
                .filter(word -> line.intersects(side.getCorrectedCollisionRectangle(word)))
                .count();
    }

    /**
     * Build irregular quadrilateral shape from intersecting line segments
     */
    private static List<Line2D> buildQuadrilateral(List<Line2D> lines) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            points.add(getIntersectionPoint(lines.get(i), lines.get(i == 3 ? 0 : i + 1)));
        }

        if (points.size() != 4) {
            return emptyList();
        }

        List<Line2D> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            result.add(new Line2D.Double(points.get(i), points.get(i == 3 ? 0 : i + 1)));
        }
        return result;
    }
}
