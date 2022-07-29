package be.enceladus.scan2ereader.core.ocr.model;

import be.enceladus.scan2ereader.core.utils.GeometryUtils;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

// TODO: extend RectangularShape
public abstract class TextZone {

    private final static Logger logger = Logger.getLogger(TextZone.class.getName());

    private Rectangle rectangle;
    private Page page;
    private Color markColor;

    @Getter
    @Setter
    private BufferedImage subImage;

    public TextZone(Rectangle rectangle, Page page) {
        this.rectangle = rectangle;
        this.page = page;
    }

    public int getX1() {
        return rectangle.x;
    }

    public int getY1() {
        return rectangle.y;
    }

    public int getWidth() {
        return rectangle.width;
    }

    public int getHeight() {
        return rectangle.height;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public Rectangle getHeightExpandedRectangle() {
        int h = (int) (getHeight() * 0.2);
        Rectangle expandedRectangle = new Rectangle(rectangle);
        expandedRectangle.grow(0, h);
        return expandedRectangle;
    }

    public Rectangle getWidthExpandedRectangle() {
        int w = (int) (getWidth() * 0.2); // e.g. 0.10 means 10% on each side
        Rectangle expandedRectangle = new Rectangle(rectangle);
        expandedRectangle.grow(w, 0);
        return expandedRectangle;
    }

    public int getArea() {
        return rectangle.width * rectangle.height;
    }

    public int getX2() {
        return getX1() + getWidth();
    }

    public int getY2() {
        return getY1() + getHeight();
    }

    void mark(Color color) {
        this.markColor = color;
    }

    public boolean isMarked() {
        return markColor != null;
    }

    public Color getMarkColor() {
        return markColor;
    }

    public boolean isValid() {
        if (!isCompletelyIncluded(getPage().getRectangle())) {
            return false;
        }

        if (getWidth() <= 10) {
            return false;
        }

        if (getWidth() < (0.01 * getPage().getWidth())) {
            return false;
        }

        if (getHeight() <= 3) {
            return false;
        }

        return true;
    }

    private boolean isCompletelyIncluded(Rectangle rectangle) {
        return isPartiallyIncluded(rectangle, 1d);
    }

    /**
     * Is 80% included.
     */
    public boolean isMostlyIncludedIn(TextZone textZone) {
        return isMostlyIncludedIn(textZone.getRectangle());
    }

    /**
     * Is 80% included.
     */
    private boolean isMostlyIncludedIn(Rectangle rectangle) {
        return isPartiallyIncluded(rectangle, 0.8d);
    }

    /**
     * The text zone is considered included in this one if {@code percentInside} is inside
     */
    private boolean isPartiallyIncluded(Rectangle rectangle, double percentInside) {
        Rectangle intersection = GeometryUtils.intersection(this.rectangle, rectangle);
        if (intersection == null) {
            return false;
        }
        double areaIntersection = intersection.width * intersection.height;
        double myArea = this.rectangle.width * this.rectangle.height;
        return (areaIntersection / myArea) >= percentInside;
    }

    public boolean intersects(TextZone textZone) {
        return intersects(textZone.rectangle);
    }

    public boolean intersects(Shape shape) {
        return shape.intersects(rectangle);
    }

    public Rectangle getHorizontalRectangle() {
        return new Rectangle(0, rectangle.y, getPage().getWidth(), rectangle.height);
    }

    public void mergeWith(TextZone textZone) {
        this.rectangle = GeometryUtils.mergeBlocks(asList(this, textZone));
    }

    public BufferedImage extractSubImageFromPage() {
        try {
            return page.getOriginalImage().getSubimage(getX1(), getY1(), getWidth(), getHeight());
        } catch (RasterFormatException e) {
            logger.warning("page: " + page);
            throw e;
        }
    }

    Rectangle getControlRectangle(Side side) {
        switch (side) {
            case LEFT:
                return new Rectangle(0, getY1(), getX1(), getHeight());
            case TOP:
                return new Rectangle(getX1(), 0, getWidth(), getY1());
            case RIGHT:
                return new Rectangle(getX2(), getY1(), getPage().getWidth() - getX2(), getHeight());
            case BOTTOM:
                return new Rectangle(getX1(), getY2(), getWidth(), getPage().getHeight() - getY2());
            default:
                throw new IllegalArgumentException();
        }
    }

    private int getMinYCutSpacing() {
        return (int) (0.05 * getHeight());
    }

    static <TZ extends TextZone> List<TZ> mergeHorizontally(List<TZ> input) {
        List<TZ> mergedTextZones = new ArrayList<>();

        if (isNotEmpty(input)) {
            input
                    .forEach(zoneToMerge -> {
                        Rectangle horizontalRectangle = zoneToMerge.getHorizontalRectangle();

                        // either merge the paragraph into another one that is already in the result list...
                        boolean isMerged = mergedTextZones
                                .stream()
                                .filter(textZone -> textZone.intersects(horizontalRectangle))
                                .peek(textZone -> textZone.mergeWith(zoneToMerge))
                                .findFirst()
                                .isPresent();

                        // ... either add a new element on the result list
                        if (!isMerged) {
                            mergedTextZones.add(zoneToMerge);
                        }
                    });
        }

        return mergedTextZones;
    }

    /**
     * Ensure yCut are not too close to one another.
     */
    boolean isValidCutToAdd(int yCut, List<Integer> yCuts) {
        return yCut > 0
                && yCut < getHeight()
                && yCuts
                .stream()
                .noneMatch(otherYCut
                        -> Math.abs(yCut - otherYCut) < getMinYCutSpacing()
                        || Math.abs(yCut - getHeight()) < getMinYCutSpacing()
                );
    }

    /**
     * Relative Ys
     */
    <TZ extends TextZone> List<TZ> sliceOnY(List<Integer> yCuts, Class<TZ> klazz) {
        // is not empty
        if (isEmpty(yCuts)) {
            throw new IllegalArgumentException();
        }

        // doesn't contain duplicate
        if (new HashSet<>(yCuts).size() != yCuts.size()) {
            throw new IllegalArgumentException();
        }

        // all values are [0-height]
        for (int yCut : yCuts) {
            if (yCut <= 0 || yCut >= getHeight()) {
                throw new IllegalArgumentException();
            }
        }

        // clean up arguments
        yCuts = new ArrayList<>(yCuts);
        yCuts.add(0, 0);
        sort(yCuts);

        Constructor<TZ> constructor;
        try {
            constructor = klazz.getConstructor(Rectangle.class, Page.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        logger.info("rectangle: " + rectangle);
        logger.info("yCuts: " + yCuts);

        int i = 0;
        List<TZ> slices = new ArrayList<>();
        for (int yCut : yCuts) {
            try {
                int nextYCut;
                if (i == (yCuts.size() - 1)) {
                    nextYCut = getHeight();
                } else {
                    nextYCut = yCuts.get(i + 1);
                }

                Rectangle rectangle = getSliceRectangle(yCut, nextYCut);
                TZ paragraph = constructor.newInstance(rectangle, getPage());
                slices.add(paragraph);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            i++;
        }

        return slices;
    }

    private Rectangle getSliceRectangle(int yCut, int nextYCut) {
        int x = getX1();
        int y = yCut + getY1();
        int width = getWidth();
        int height = nextYCut - yCut;

        Rectangle rectangle = new Rectangle(x, y, width, height);

        logger.info("slices: " + rectangle);

        assert x >= 0;
        assert y >= 0;
        assert width > 0;
        assert height > 0;

        return rectangle;
    }

    @Override
    public String toString() {
        return "TextZone[" + rectangle + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextZone textZone = (TextZone) o;
        return Objects.equals(rectangle, textZone.rectangle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rectangle);
    }
}
