package dev.encelade.detection;

import dev.encelade.utils.GeometryUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

public class PictureDetector {

    private final static Logger logger = Logger.getLogger(PictureDetector.class.getName());

    private static final int SCANNER_SIZE = 66;
    private static final int LIGHT_LEVEL = 30;
    private static final int BLACK_LEVEL = 240;

    private BufferedImage bufferedImage;

    private Map<Rectangle, Pair<Double, Double>> result;

    public PictureDetector(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
    }

    public Map<Rectangle, Pair<Double, Double>> getAnalysisData() {
        return result;
    }

    public void detect() {
        result = new HashMap<>();
        for (int x = 0; x < bufferedImage.getWidth() - SCANNER_SIZE; x += SCANNER_SIZE) {
            for (int y = 0; y < bufferedImage.getHeight() - SCANNER_SIZE; y += SCANNER_SIZE) {
                Rectangle square = new Rectangle(x, y, SCANNER_SIZE, SCANNER_SIZE);
                result.put(square, getAverages(square));
            }
        }
    }

    private Pair<Double, Double> getAverages(Rectangle square) {
        double light = 0;
        double black = 0;
        double total = SCANNER_SIZE * SCANNER_SIZE;

        for (int x = square.x; x < SCANNER_SIZE + square.x; x++) {
            for (int y = square.y; y < SCANNER_SIZE + square.y; y++) {
                int clr = bufferedImage.getRGB(x, y);
                int red = (clr & 0x00ff0000) >> 16;
                int green = (clr & 0x0000ff00) >> 8;
                int blue = clr & 0x000000ff;

                if (red > BLACK_LEVEL && green > BLACK_LEVEL && blue > BLACK_LEVEL) {
                    light++;
                } else if (red < LIGHT_LEVEL && green < LIGHT_LEVEL && blue < LIGHT_LEVEL) {
                    black++;
                }
            }
        }

        return new ImmutablePair<>(black / total, light / total);
    }

    public static boolean isText(Pair<Double, Double> pair) {
        Double black = pair.getLeft();
        Double light = pair.getRight();
        return black <= 0.3 && light > 0.7;
    }

    public static boolean isWhite(Pair<Double, Double> pair) {
        Double black = pair.getLeft();
        Double light = pair.getRight();
        return black < 0.05 && light > 0.8;
    }

    public static boolean isPicture(Pair<Double, Double> pair) {
        return !isWhite(pair) && !isText(pair);
    }

    public List<Rectangle> getPictureZones() {
        List<List<Rectangle>> groups = new ArrayList<>();

        for (int x = 0; x < bufferedImage.getWidth() - SCANNER_SIZE; x += SCANNER_SIZE) {
            for (int y = 0; y < bufferedImage.getHeight() - SCANNER_SIZE; y += SCANNER_SIZE) {
                Rectangle square = new Rectangle(x, y, SCANNER_SIZE, SCANNER_SIZE);
                Pair<Double, Double> pair = result.get(square);
                if (isPicture(pair)) {
                    List<Rectangle> group = findGroupBelongsTo(square, groups);
                    if (group == null) {
                        group = new ArrayList<>();
                        groups.add(group);
                    }
                    group.add(square);
                }
            }
        }

        logger.info("groups: " + groups.size());

        return groups
                .stream()
                .map(GeometryUtils::mergeRectangles)
                .collect(toList());
    }

    private static List<Rectangle> findGroupBelongsTo(Rectangle rectangle, List<List<Rectangle>> groups) {
        return null;

//        return groups
//                .stream()
//                .filter(group -> group
//                        .stream()
//                        .anyMatch(rectangleInGroup
//                                -> rectangle.getMinX() == rectangleInGroup.getMaxX()
//                                || rectangle.getMaxX() == rectangleInGroup.getMinX()
//                                || rectangle.getMinY() == rectangleInGroup.getMaxY()
//                                || rectangle.getMaxY() == rectangleInGroup.getMinY()))
//                .findFirst()
//                .orElse(null);
    }
}
