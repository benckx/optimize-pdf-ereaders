package dev.encelade.processing.corrections;

import dev.encelade.ocr.model.Side;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static dev.encelade.utils.GeometryUtils.computeAngle;
import static dev.encelade.utils.GeometryUtils.getLength;
import static java.lang.Math.abs;
import static java.util.stream.Collectors.toList;

public class AngleCorrectionQuadrilateral {

    private final static Logger logger = Logger.getLogger(AngleCorrectionQuadrilateral.class.getName());

    private List<Line2D> linesData = new ArrayList<>();

    /**
     * The higher the value, the more irregular the shape
     */
    public double getLengthsOffset() {
        return getDeltaBetween(Side.LEFT, Side.RIGHT);// + getDeltaBetween(TOP, BOTTOM);
    }

    public double getAnglesOffset() {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        getAngles().forEach(stats::addValue);
        return stats.getStandardDeviation();
    }

    private double getDeltaBetween(Side side1, Side side2) {
        assert side1.isHorizontal() && side2.isHorizontal() || !side1.isHorizontal() && !side2.isHorizontal();

        Line2D line1 = getLine(side1);
        Line2D line2 = getLine(side2);

        return abs(getLength(line1) - getLength(line2));
    }

    private Line2D getLine(Side side) {
        Point p1, p2;

        switch (side) {
            case TOP:
                p1 = getQuadrilateralPointAt(Side.TOP, Side.LEFT);
                p2 = getQuadrilateralPointAt(Side.TOP, Side.RIGHT);
                break;
            case LEFT:
                p1 = getQuadrilateralPointAt(Side.TOP, Side.LEFT);
                p2 = getQuadrilateralPointAt(Side.BOTTOM, Side.LEFT);
                break;
            case RIGHT:
                p1 = getQuadrilateralPointAt(Side.TOP, Side.RIGHT);
                p2 = getQuadrilateralPointAt(Side.BOTTOM, Side.RIGHT);
                break;
            case BOTTOM:
                p1 = getQuadrilateralPointAt(Side.BOTTOM, Side.LEFT);
                p2 = getQuadrilateralPointAt(Side.BOTTOM, Side.RIGHT);
                break;
            default:
                throw new RuntimeException();
        }

        return getLine(p1, p2);
    }

    private Line2D getLine(Point p1, Point p2) {
        return linesData
                .stream()
                .filter(line
                        -> (line.getP1().equals(p1) && line.getP2().equals(p2))
                        || (line.getP1().equals(p2) && line.getP2().equals(p1))
                )
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public List<Line2D> getLinesData() {
        return linesData;
    }

    public void setLinesData(List<Line2D> linesData) {
        this.linesData = linesData;
    }

    public Point getQuadrilateralPointAt(Side side1, Side side2) {
        assert side1.isHorizontal();
        assert !side2.isHorizontal();

        // get horizontal points (the 2 at top or the 2 at bottom)
        List<Point> allPoints = getQuadrilateralPoints();
        if (side1 == Side.TOP) {
            allPoints.sort((p1, p2) -> Integer.compare(p1.y, p2.y));
        } else if (side1 == Side.BOTTOM) {
            allPoints.sort((p1, p2) -> Integer.compare(p2.y, p1.y));
        }
        List<Point> horizontalPoints = allPoints.subList(0, 2);

        // get the point on on the left or the point on the right
        if (side2 == Side.LEFT) {
            horizontalPoints.sort((p1, p2) -> Integer.compare(p1.x, p2.x));
        } else if (side2 == Side.RIGHT) {
            horizontalPoints.sort((p1, p2) -> Integer.compare(p2.x, p1.x));
        }

        return horizontalPoints.get(0);
    }

    public double getCorrectiveAngle() {
        return -getAngles().stream().mapToDouble(i -> i).average().getAsDouble();
    }

    public boolean isValid() {
        return linesData.size() == 4;
    }

    private List<Double> getAngles() {
        // TODO: use line and not point
        Point topLeft = getQuadrilateralPointAt(Side.TOP, Side.LEFT);
        Point bottomLeft = getQuadrilateralPointAt(Side.BOTTOM, Side.LEFT);

        Point topRight = getQuadrilateralPointAt(Side.TOP, Side.RIGHT);
        Point bottomRight = getQuadrilateralPointAt(Side.BOTTOM, Side.RIGHT);

        List<Double> angles = new ArrayList<>();
        angles.add(computeAngle(topLeft, bottomLeft));
        angles.add(computeAngle(topRight, bottomRight));

        // TODO: numbers are very different when we compute this way, there must be something wrong
//      angles.add(computeAngle(getLine(LEFT)));
//      angles.add(computeAngle(getLine(RIGHT)));

//        angles.add(90 + computeAngle(topRight, topLeft));
//        angles.add(90 + computeAngle(bottomRight, bottomLeft));

        logger.info("angles: " + angles.toString());

        return angles;
    }

    private List<Point> getQuadrilateralPoints() {
        return linesData
                .stream()
                .map(line2D -> new Point((int) line2D.getX1(), (int) line2D.getY1()))
                .collect(toList());
    }
}
