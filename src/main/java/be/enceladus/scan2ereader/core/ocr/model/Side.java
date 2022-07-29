package be.enceladus.scan2ereader.core.ocr.model;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static java.awt.Color.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;

public enum Side {

    TOP, RIGHT, BOTTOM, LEFT;

    private final static Logger logger = Logger.getLogger(Side.class.getName());

    public static List<List<Side>> getAnglesPairs() {
        List<List<Side>> result = new ArrayList<>();
        result.add(asList(TOP, LEFT));
        result.add(asList(TOP, RIGHT));
        result.add(asList(BOTTOM, RIGHT));
        result.add(asList(BOTTOM, LEFT));
        return result;
    }

    public Point getOppositePoint(TextZone textZone, boolean isClockWise) {
        return previous(isClockWise).getBeginPoint(textZone, isClockWise);
    }

    Point getBeginPoint(TextZone textZone, boolean isClockWise) {
        if (isHorizontal()) {
            return getPoint(textZone, this, previous(isClockWise));
        } else {
            return getPoint(textZone, previous(isClockWise), this);
        }
    }

    Point getEndPoint(TextZone textZone, boolean isClockWise) {
        if (isHorizontal()) {
            return getPoint(textZone, this, next(isClockWise));
        } else {
            return getPoint(textZone, next(isClockWise), this);
        }
    }

    public List<Point> getPoints(TextZone textZone, boolean clockWise) {
        Point beginPoint = getBeginPoint(textZone, clockWise);
        Point endPoint = getEndPoint(textZone, clockWise);

        int direction = getDirection(clockWise);

        List<Point> points;
        if (isHorizontal()) {
            // X varies
            int from = Math.min(beginPoint.x, endPoint.x);
            int to = Math.max(beginPoint.x, endPoint.x);

            points = rangeClosed(from, to).mapToObj(x -> new Point(x, beginPoint.y)).collect(toList());
        } else {
            // Y varies
            int from = Math.min(beginPoint.y, endPoint.y);
            int to = Math.max(beginPoint.y, endPoint.y);

            points = rangeClosed(from, to).mapToObj(y -> new Point(beginPoint.x, y)).collect(toList());
        }

        if (direction < 0) {
            Collections.reverse(points);
        }

        return points;
    }

    static Point getPoint(TextZone textZone, Side side1, Side side2) {
        if (side1 == TOP && side2 == LEFT) {
            return new Point(textZone.getX1(), textZone.getY1());
        }
        if (side1 == TOP && side2 == RIGHT) {
            return new Point(textZone.getX2(), textZone.getY1());
        }
        if (side1 == BOTTOM && side2 == RIGHT) {
            return new Point(textZone.getX2(), textZone.getY2());
        }
        if (side1 == BOTTOM && side2 == LEFT) {
            return new Point(textZone.getX1(), textZone.getY2());
        }
        throw new IllegalArgumentException();
    }

    int getDirection(boolean clockWise) {
        int i = clockWise ? +1 : -1;

        switch (this) {
            case TOP:
            case RIGHT:
                return i;
            case LEFT:
            case BOTTOM:
                return -i;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static List<Side> listAllSides() {
        return listAllSides(true);
    }

    public static List<Side> listAllSides(boolean clockWise) {
        if (clockWise) {
            return new ArrayList<>(asList(TOP, RIGHT, BOTTOM, LEFT));
        } else {
            return new ArrayList<>(asList(TOP, LEFT, BOTTOM, RIGHT));
        }
    }

    Side next(boolean clockWise) {
        return clockWise ? next() : previous();
    }

    Side next() {
        if (this == values()[values().length - 1]) {
            return values()[0];
        }
        return values()[this.ordinal() + 1];
    }

    public Side previous(boolean clockWise) {
        return clockWise ? previous() : next();
    }

    Side previous() {
        if (this == values()[0]) {
            return values()[values().length - 1];
        }
        return values()[this.ordinal() - 1];
    }

    public boolean isHorizontal() {
        return this == TOP || this == BOTTOM;
    }

    public Color getColor() {
        switch (this) {
            case LEFT:
                return RED;
            case TOP:
                return BLUE;
            case RIGHT:
                return GREEN;
            case BOTTOM:
                return CYAN;
            default:
                throw new IllegalArgumentException();

        }
    }

    public Rectangle getCorrectedCollisionRectangle(TextZone textZone) {
        Rectangle rectangle = textZone.getRectangle();
        final int correctedAngleSize = 2;

        switch (this) {
            case LEFT:
                return new Rectangle(rectangle.x, rectangle.y, correctedAngleSize, rectangle.height);
            case TOP:
                return new Rectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height / 2);
            case RIGHT:
                return new Rectangle(rectangle.x + rectangle.width - correctedAngleSize, rectangle.y, correctedAngleSize, rectangle.height);
            case BOTTOM:
//                return new Rectangle(rectangle.x, rectangle.y + rectangle.height - correctedAngleSize, rectangle.width, correctedAngleSize);
                return rectangle;
            default:
                throw new IllegalArgumentException();

        }
    }

    public static List<Rectangle> getMarginRectangles(final Rectangle rectangle) {
        return listAllSides(true).stream().map(side -> side.getMarginRectangle(rectangle)).collect(toList());
    }

    public Rectangle getMarginRectangle(Rectangle rectangle) {
        double margin = 0.02; // TODO: make this a request parameter
        int marginWidth = (int) (margin * rectangle.width);
        int marginHeight = (int) (margin * rectangle.height);

        switch (this) {
            case LEFT:
                return new Rectangle(0, 0, marginWidth, rectangle.height);
            case TOP:
                return new Rectangle(0, 0, rectangle.width, marginHeight);
            case RIGHT:
                return new Rectangle(rectangle.width - marginWidth, 0, marginWidth, rectangle.height);
            case BOTTOM:
                return new Rectangle(0, rectangle.height - marginHeight, rectangle.width, marginHeight);
            default:
                throw new IllegalArgumentException();
        }
    }
}
