package dev.encelade.utils;

import dev.encelade.ocr.model.Page;
import dev.encelade.ocr.model.TextZone;
import dev.encelade.ocr.model.WhiteSpace;
import dev.encelade.ocr.model.Word;
import dev.encelade.processing.corrections.AngleCorrectionQuadrilateral;
import dev.encelade.ocr.model.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static dev.encelade.utils.TimeUtils.formatMillis;
import static java.awt.Color.*;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class Printer {

    private final static Logger logger = Logger.getLogger(Printer.class.getName());

    private static final DecimalFormat DEBUG_VALUE_FORMAT = new DecimalFormat("####0.00");

    private static final boolean PRINT_IMAGE = true;
    private static final boolean PRINT_SPACES_AND_MARGIN = true;

    private static final boolean PRINT_TEXT_BLOCKS = false;
    private static final boolean PRINT_PARAGRAPHS = false;
    private static final boolean PRINT_TEXT_LINES = true;
    private static final boolean PRINT_WORDS = false;

    private static final boolean PRINT_CONFIDENCE = false;
    private static final boolean PRINT_AREAS = true;
    private static final boolean PRINT_ANGLE_DATA = true;
    private static final boolean PRINT_PAGE_LAYOUT_DATA = true;

    private static final int TEXT_BLOCK_FRAME_THICKNESS = 6;
    private static final int PARAGRAPH_FRAME_THICKNESS = 3;
    private static final int LINE_FRAME_THICKNESS = 2;
    private static final int WORD_FRAME_THICKNESS = 1;

    private static final Stroke DASHED_STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
    private int consoleTextSize;

    private Page page;
    private final String fileName;

    private Graphics2D graphics2D;
    private Stroke backupStroke;
    private int consoleTextPosition;
    private BufferedImage outputImage;
    private BufferedImage originalImage;

    public Printer(Page page, String fileName) {
        this.page = page;
        this.fileName = fileName;
    }

    private void restoreInitialStroke() {
        graphics2D.setStroke(backupStroke);
    }

    public void dumpToImageFile() {
        String fileNameWithExtension = fileName + ".png";

        long begin = currentTimeMillis();
        try {
            originalImage = page.getOriginalImage();
            outputImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), TYPE_INT_RGB);
            graphics2D = outputImage.createGraphics();
            backupStroke = graphics2D.getStroke();
            consoleTextSize = outputImage.getHeight() / 50;
            consoleTextPosition = outputImage.getHeight();
            graphics2D.setFont(graphics2D.getFont().deriveFont((float) (0.75 * consoleTextSize)));

            printBackground();
            printWhiteSpacesAndMargin();
            printTextZones();
            printAreas();
            printAngleData();
            printLayoutData();

            graphics2D.dispose();

            // Save as image
            ImageIO.write(outputImage, "png", new File(fileNameWithExtension));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("dumped " + fileNameWithExtension + " in" + formatMillis(currentTimeMillis() - begin));
    }

    private void printBackground() {
        if (PRINT_IMAGE) {
            graphics2D.drawRenderedImage(originalImage, new AffineTransform());
        } else {
            graphics2D.setColor(WHITE);
            graphics2D.fillRect(0, 0, originalImage.getWidth(), originalImage.getHeight());
        }
    }

    private void printWhiteSpacesAndMargin() {
        if (PRINT_SPACES_AND_MARGIN) {
            if (isNotEmpty(page.getWhiteSpaces())) {
                // printAngleCorrectionQuadrilateral white space
                graphics2D.setColor(RED);
                graphics2D.fillRect(0, 0, page.computeParagraphXOffSet(), outputImage.getHeight());

                // printAngleCorrectionQuadrilateral left margin
                graphics2D.setColor(new Color(150, 150, 150, 100));
                for (WhiteSpace whiteSpace : page.getWhiteSpaces()) {
                    graphics2D.fill(whiteSpace.getRectangle());
                }
            }
        }

        Color lightOrange = new Color(ORANGE.getRed(), ORANGE.getGreen(), ORANGE.getBlue(), 100);
        graphics2D.setColor(lightOrange);
        for (Rectangle rectangle : page.getMargins()) {
            graphics2D.fill(rectangle);
        }
    }

    private void printTextZones() {
        if (PRINT_WORDS) {
            drawBoundingBoxes(page.getWords(), LIGHT_GRAY, WORD_FRAME_THICKNESS, false, false);
        }

        if (PRINT_TEXT_LINES) {
            drawBoundingBoxes(page.getLines(), BLUE, LINE_FRAME_THICKNESS, true, true);
        }

        if (PRINT_PARAGRAPHS) {
            drawBoundingBoxes(page.getParagraphs(), ORANGE, PARAGRAPH_FRAME_THICKNESS, false, false);
        }

        if (PRINT_TEXT_BLOCKS) {
            drawBoundingBoxes(page.getTextBlocks(), GREEN, TEXT_BLOCK_FRAME_THICKNESS, false, false);
        }

        if (PRINT_WORDS && PRINT_CONFIDENCE) {
            graphics2D.setColor(BLUE);
            for (Word word : page.getWords()) {
                int x = word.getX1() + word.getWidth() - 10;
                int y = word.getY1() + word.getHeight() - 10;
                graphics2D.drawString(DEBUG_VALUE_FORMAT.format(word.getConfidence()), x, y);
            }
        }
    }

    private void printAreas() {
        if (PRINT_AREAS) {
            addToConsole("words area: ", page.getWordsTotalArea() / 1024d);
            addToConsole("lines area: ", page.getLinesTotalArea() / 1024d);
            double wordsOverlappingArea = page.getWordsOverlappingArea();
            if (wordsOverlappingArea > 0) {
                addToConsole("words overlapping: ", wordsOverlappingArea / 1024d);
                addToConsole("words overlapping ratio: ", page.getWordsOverlappingRatio());
            }
            double linesOverlappingArea = page.getLinesOverlappingArea();
            if (linesOverlappingArea > 0) {
                addToConsole("lines overlapping: ", linesOverlappingArea / 1024d);
            }
        }
    }

    private void printAngleData() {
        if (PRINT_ANGLE_DATA) {
            if (page.getCorrectedAngleValue() != 0) {
                addToConsole("corrected angle: ", page.getCorrectedAngleValue());
            }

            if (page.hasCorrectiveAngleData()) {
                graphics2D.setStroke(DASHED_STROKE);
                printAngleCorrectionQuadrilateral(page.getClockWiseQuadrilateral(), MAGENTA);
                printAngleCorrectionQuadrilateral(page.getCounterClockWiseQuadrilateral(), BLUE);
                restoreInitialStroke();

                addToConsole("quadri offset: ", page.getAngleQuadrilateralsOffset());
                addToConsole("is leaning clockwise: " + page.isLeaningClockwise());
            }
        }
    }

    private void printLayoutData() {
        if (PRINT_PAGE_LAYOUT_DATA && page.hasLayoutData()) {
            graphics2D.setColor(RED);
            graphics2D.setStroke(new BasicStroke(4));
            graphics2D.draw(page.getDividingLine());
            restoreInitialStroke();
        }
    }

    private void addToConsole(String text, double value) {
        addToConsole(text, value, BLACK);
    }

    private void addToConsole(String text, double value, Color color) {
        addToConsole(text + DEBUG_VALUE_FORMAT.format(value), color);
    }

    private void addToConsole(String text) {
        addToConsole(text, BLACK);
    }

    private void addToConsole(String text, Color color) {
        graphics2D.setColor(color);
        consoleTextPosition -= consoleTextSize;
        drawDebugText(text, color, consoleTextPosition);
    }

    private void drawDebugText(String text, Color color, int y) {
        int x = consoleTextSize / 2;
        graphics2D.setColor(WHITE);
        graphics2D.fillRect(x, y - consoleTextSize, (originalImage.getWidth() / 2), consoleTextSize);

        graphics2D.setColor(color);
        graphics2D.drawString(text, x, y);
    }

    private void printAngleCorrectionQuadrilateral(AngleCorrectionQuadrilateral quadrilateral, Color color) {
        drawLines(quadrilateral.getLinesData(), color);
        String offset = "offset -> length: " + DEBUG_VALUE_FORMAT.format(quadrilateral.getLengthsOffset());
        offset += " / angle: " + DEBUG_VALUE_FORMAT.format(quadrilateral.getAnglesOffset());
        offset += " [" + DEBUG_VALUE_FORMAT.format(quadrilateral.getCorrectiveAngle()) + "]";
        addToConsole(offset, color);
    }

    private void drawLines(List<Line2D> lines, Color color) {
        graphics2D.setColor(color);
        for (Line2D line2D : lines) {
            graphics2D.draw(line2D);
            graphics2D.fillRect((int) line2D.getX1() - 3, (int) line2D.getY1() - 3, 7, 7);
            graphics2D.fillRect((int) line2D.getX2() - 3, (int) line2D.getY2() - 3, 7, 7);
        }
    }

    private <TZ extends TextZone> void drawBoundingBoxes(Collection<TZ> textZones, Color color, int size, boolean displayNumbers, boolean expanded) {
        int i = 0;
        if (textZones != null) {
            graphics2D.setStroke(new BasicStroke(size));
            for (TextZone textZone : textZones) {
                if (textZone.isMarked()) {
                    graphics2D.setColor(textZone.getMarkColor());
                } else {
                    graphics2D.setColor(color);
                }

                if (expanded) {
                    if (textZone instanceof Word) {
                        if (Objects.equals(textZone.getMarkColor(), Side.LEFT.getColor())) {
                            graphics2D.draw(Side.LEFT.getCorrectedCollisionRectangle(textZone));
                        } else if (Objects.equals(textZone.getMarkColor(), Side.RIGHT.getColor())) {
                            graphics2D.draw(Side.RIGHT.getCorrectedCollisionRectangle(textZone));
                        } else if (Objects.equals(textZone.getMarkColor(), Side.TOP.getColor())) {
                            graphics2D.draw(Side.TOP.getCorrectedCollisionRectangle(textZone));
                        } else if (Objects.equals(textZone.getMarkColor(), Side.BOTTOM.getColor())) {
                            graphics2D.draw(Side.BOTTOM.getCorrectedCollisionRectangle(textZone));
                        } else {
                            graphics2D.draw(textZone.getRectangle());
                        }
                    } else {
                        graphics2D.draw(textZone.getRectangle());
                    }
                } else {
                    graphics2D.draw(textZone.getRectangle());
                }

                if (displayNumbers) {
                    graphics2D.drawString(Integer.toString(i++), textZone.getX1() + textZone.getWidth() + 10, textZone.getY1() + (textZone.getHeight() / 2));
                }
            }
            restoreInitialStroke();
        }
    }
}
