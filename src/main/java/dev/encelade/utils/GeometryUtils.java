package dev.encelade.utils;

import dev.encelade.ocr.model.TextZone;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.max;
import static java.util.Collections.min;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;

public class GeometryUtils {

    /**
     * In degree
     * <br>Point(0, 0), Point(0, 100) returns 0 (i.e. a straight vertical line from top to bottom)
     */
    public static double computeAngle(Line2D line) {
        return computeAngle(line.getP1(), line.getP2());
    }

    /**
     * In degree
     * <br>Point(0, 0), Point(0, 100) returns 0 (i.e. a straight vertical line from top to bottom)
     */
    public static double computeAngle(Point2D p1, Point2D p2) {
        return Math.toDegrees(Math.atan2(p2.getX() - p1.getX(), p2.getY() - p1.getY()));
    }

    public static double computeAngle(Point center, Point current, Point previous) {
        return Math.toDegrees(Math.atan2(current.x - center.x, current.y - center.y) - Math.atan2(previous.x - center.x, previous.y - center.y));
    }

    public static Rectangle intersection(Rectangle textZone1, Rectangle textZone2) {
        int newX = Math.max(textZone1.x, textZone2.x);
        int newY = Math.max(textZone1.y, textZone2.y);

        int newWidth = Math.min(textZone1.x + textZone1.width, textZone2.x + textZone2.width) - newX;
        int newHeight = Math.min(textZone1.y + textZone1.height, textZone2.y + textZone2.height) - newY;

        if (newWidth <= 0d || newHeight <= 0d) {
            return null;
        }

        return new Rectangle(newX, newY, newWidth, newHeight);
    }

    public static double getLength(Line2D line2D) {
        return distanceBetween(line2D.getP1(), line2D.getP2());
    }

    public static double distanceBetween(Point2D p1, Point2D p2) {
        return distanceBetween(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Between two points
     */
    public static double distanceBetween(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    public static double distanceBetween(TextZone textZone1, TextZone textZone2) {
        return distanceBetween(textZone1.getRectangle(), textZone2.getRectangle());
    }

    // https://stackoverflow.com/questions/14431032/i-want-to-calculate-the-distance-between-two-points-in-java
    public static double distanceBetween(Rectangle rectangle1, Rectangle rectangle2) {
        int x1 = rectangle1.x;
        int y1 = rectangle1.y;
        int x1b = rectangle1.x + rectangle1.width;
        int y1b = rectangle1.y + rectangle1.height;

        int x2 = rectangle2.x;
        int y2 = rectangle2.y;
        int x2b = rectangle2.x + rectangle2.width;
        int y2b = rectangle2.y + rectangle2.height;

        boolean left, right, bottom, top;

        left = x2b < x1;
        right = x1b < x2;
        bottom = y2b < y1;
        top = y1b < y2;

        if (top && left) {
            return distanceBetween(x1, y1b, x2b, y2);
        } else if (left && bottom) {
            return distanceBetween(x1, y1, x2b, y2b);
        } else if (bottom && right) {
            return distanceBetween(x1b, y1, x2, y2b);
        } else if (right && top) {
            return distanceBetween(x1b, y1b, x2, y2);
        } else if (left) {
            return x1 - x2b;
        } else if (right) {
            return x2 - x1b;
        } else if (bottom) {
            return y1 - y2b;
        } else if (top) {
            return y2 - y1b;
        } else {
            return 0;
        }
    }

    public static Rectangle mergeRectangles(java.util.List<Rectangle> rectangles) {
        switch (rectangles.size()) {
            case 0:
                throw new IllegalArgumentException();
            case 1:
                return getOnlyElement(rectangles);
            default:
                int x1 = (int) min(rectangles, comparingDouble(RectangularShape::getMinX)).getMinX();
                int y1 = (int) min(rectangles, comparingDouble(RectangularShape::getMinY)).getMinY();

                int x2 = (int) max(rectangles, comparingDouble(RectangularShape::getMaxX)).getMaxX();
                int y2 = (int) max(rectangles, comparingDouble(RectangularShape::getMaxY)).getMaxY();

                assert y2 > y1;
                assert x2 > x1;

                return new Rectangle(x1, y1, x2 - x1, y2 - y1);
        }
    }

    public static <TZ extends TextZone> Rectangle mergeBlocks(java.util.List<TZ> textZones) {
        return mergeRectangles(textZones.stream().map(TextZone::getRectangle).collect(toList()));
    }

    public static boolean intersects(Rectangle rectangle, Point p1, Point p2) {
        Line2D l1 = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
        return l1.intersects(rectangle);
    }

    /**
     * // Returns 1 if the lines intersect, otherwise 0. In addition, if the lines
     * // intersect the intersection point may be stored in the floats i_x and i_y.
     */
    // https://stackoverflow.com/questions/563198/whats-the-most-efficent-way-to-calculate-where-two-line-segments-intersect
    public static Point getIntersectionPoint(Line2D line1, Line2D line2) {
        double p0_x, p0_y, p1_x, p1_y, p2_x, p2_y, p3_x, p3_y;
        double i_x;
        double i_y;

        p0_x = line1.getX1();
        p0_y = line1.getY1();
        p1_x = line1.getX2();
        p1_y = line1.getY2();

        p2_x = line2.getX1();
        p2_y = line2.getY1();
        p3_x = line2.getX2();
        p3_y = line2.getY2();


        double s1_x, s1_y, s2_x, s2_y;
        s1_x = p1_x - p0_x;
        s1_y = p1_y - p0_y;
        s2_x = p3_x - p2_x;
        s2_y = p3_y - p2_y;

        double s, t;
        s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
        t = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            i_x = p0_x + (t * s1_x);
            i_y = p0_y + (t * s1_y);

            return new Point((int) i_x, (int) i_y);
        }

        return null;
    }
}
