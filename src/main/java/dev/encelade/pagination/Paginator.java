package dev.encelade.pagination;

import dev.encelade.ocr.model.Page;
import dev.encelade.ocr.model.Paragraph;
import dev.encelade.processing.RequestConfig;
import dev.encelade.ocr.model.TextZone;
import dev.encelade.ocr.model.WhiteSpace;
import dev.encelade.utils.Counter;
import dev.encelade.utils.ImageUtils;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static java.awt.Color.BLUE;
import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static java.lang.Math.min;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

// TODO: cases where the first pages and the last pages are really off
public class Paginator {

    private final static Logger logger = Logger.getLogger(Paginator.class.getName());

    private final static float MARGIN_SIZE = 0.03f;

    private final static boolean DRAW_DEBUG_RECTANGLES_AROUND_ELEMENTS = false;
    private final static int BUFFERED_IMAGE_TYPE = TYPE_BYTE_GRAY;

    private static final float RENDER_HEIGHT_RATIO_BREAK_DOWN = 0.25f;
    private static final int NBR_PIECES_BREAK_DOWN = 6;
    private static final int MIN_LINES_BREAK_DOWN = NBR_PIECES_BREAK_DOWN;

    private final int renderWidth;
    private final int renderHeight;

    private final int maxWhiteSpaceHeight;
    private final int pageTopBottomMargin;
    private final int pageLeftRightMargin;

    private final List<Page> pages;
    private final RequestConfig requestConfig;

    private List<TextZone> elementsForPagination;

    @Getter
    private List<byte[]> outputImages;

    private int yOffset;
    private BufferedImage renderBuffer;
    private Graphics2D graphics2D;

    private float ratio = 1;

    private Counter counter;

    private Thread thread;

    private final static Comparator<Page> PAGE_COMPARATOR = (page1, page2) -> {
        if (page1.getIdx().size() != page2.getIdx().size()) {
            return Integer.compare(page1.getIdx().size(), page2.getIdx().size());
        } else {
            for (int i = 0; i < page1.getIdx().size(); i++) {
                int compare = Integer.compare(page1.getIdx().get(i), page2.getIdx().get(i));
                if (compare != 0) {
                    return compare;
                }
            }

            return 0;
        }
    };

    public Paginator(List<Page> pages, RequestConfig requestConfig) {
        this.pages = pages;
        this.requestConfig = requestConfig;
        this.pages.sort(PAGE_COMPARATOR);

        renderWidth = (int) (1080 * 0.75 * getQuality());
        renderHeight = (int) (1440 * 0.75 * getQuality());
        maxWhiteSpaceHeight = (int) (0.10 * renderHeight);
        pageTopBottomMargin = (int) (MARGIN_SIZE * renderHeight);
        pageLeftRightMargin = (int) (MARGIN_SIZE * renderWidth);

        logger.info("render size: " + renderWidth + ", " + renderHeight);
    }

    public float getRatio() {
        return ratio;
    }

    public float getProgress() {
        if (counter == null) {
            return 0;
        } else {
            float progress = counter.getValue() / (float) elementsForPagination.size();
            assert progress <= 1;
            return progress;
        }
    }

    public void process() {
        thread = new Thread(new PaginatorRunnable() {
        });
        thread.start();
    }

    public void joinThread() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private class PaginatorRunnable implements Runnable {

        @Override
        public void run() {
            extractElementsToRender();
            breakDownElementsToRender();
            ratio = computeRatio(elementsForPagination);
            counter = new Counter();

            reInitBuffer();
            outputImages = new ArrayList<>();
            Page lastPage = elementsForPagination.get(0).getPage();
            for (TextZone textZone : elementsForPagination) {
                if (textZone instanceof Paragraph) {
                    if (exceedsBuffer(textZone)) {
                        flushBuffer();
                    }

                    addParagraphToBuffer((Paragraph) textZone);
                } else if (textZone instanceof WhiteSpace) {
                    yOffset += min(textZone.getHeight(), maxWhiteSpaceHeight);
                }
                counter.increment();
                lastPage = updateLastPage(lastPage, textZone.getPage());
            }

            flushBuffer();
        }
    }

    // delete image data from page we already fully processed
    private static Page updateLastPage(Page lastPage, Page currentPage) {
        if (!Objects.equals(lastPage, currentPage)) {
            lastPage.deleteImageData();
            return currentPage;
        } else {
            return lastPage;
        }
    }

    private boolean exceedsBuffer(TextZone textZone) {
        return yOffset + (textZone.getHeight() * ratio) > (renderHeight - pageTopBottomMargin)
                && textZone instanceof Paragraph;
    }

    private void addParagraphToBuffer(Paragraph paragraph) {
        paragraph.getPage().enableImageCache();
        BufferedImage subImage = paragraph.extractSubImageFromPage();
        if (ratio != 1) {
            subImage = ImageUtils.resize(subImage, ratio, BUFFERED_IMAGE_TYPE);
        }

        float compressionRate = getQuality();
        if (compressionRate < 1f) {
            subImage = ImageUtils.compress(subImage, compressionRate);
        }

        float xOffset = 0;
        xOffset += ratio * (paragraph.getX1() - paragraph.getPage().computeParagraphXOffSet()); // original X1
        xOffset += getLeftMargin(paragraph); // new margins
        graphics2D.drawImage(subImage, (int) xOffset, yOffset, null);

        if (DRAW_DEBUG_RECTANGLES_AROUND_ELEMENTS) {
            graphics2D.setColor(BLUE);
            graphics2D.drawRect((int) xOffset, yOffset, subImage.getWidth(), subImage.getHeight());
        }
        yOffset += paragraph.getHeight() * ratio;
    }

    // FIXME: unclear
    private int getLeftMargin(Paragraph paragraph) {
        int margin = Integer.MAX_VALUE;
        for (TextZone textZone : elementsForPagination) {
            if (textZone instanceof Paragraph) {
                if (Objects.equals(textZone.getPage(), paragraph.getPage())) {
                    int blockMargin = (int) (renderWidth - (textZone.getX2() - textZone.getPage().computeParagraphXOffSet()) * ratio);
                    if (blockMargin < margin) {
                        margin = blockMargin;
                    }
                }
            }
        }

        return (margin / 2);
    }

    private void addWhiteBackGround() {
        graphics2D.setColor(WHITE);
        graphics2D.fillRect(0, 0, renderWidth, renderHeight);
    }

    private void reInitBuffer() {
        yOffset = pageTopBottomMargin;
        renderBuffer = new BufferedImage(renderWidth, renderHeight, BUFFERED_IMAGE_TYPE);
        graphics2D = renderBuffer.createGraphics();
        addWhiteBackGround();
    }

    private void flushBuffer() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(renderBuffer, "jpg", byteArrayOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        outputImages.add(byteArrayOutputStream.toByteArray());
        reInitBuffer();
    }

    private float computeRatio(List<TextZone> textZones) {
        float reduceRatio = 1;
        float increaseRatio = Integer.MAX_VALUE;

        float widthToFitIn = renderWidth - 2 * pageLeftRightMargin;
        float heightToFitIn = renderHeight - 2 * pageTopBottomMargin;

        for (TextZone textZone : textZones) {
            if (textZone instanceof Paragraph) {
                int widthToBeFitted = textZone.getWidth();
                int heightToBeFitted = textZone.getHeight();

                float widthRatio = widthToFitIn / widthToBeFitted;
                float heightRatio = heightToFitIn / heightToBeFitted;

                assert widthRatio > 0;
                assert heightRatio > 0;

                float textZoneRatio = min(widthRatio, heightRatio);

                if (textZoneRatio < reduceRatio) {
                    reduceRatio = textZoneRatio;
                }

                if (textZoneRatio < increaseRatio) {
                    increaseRatio = textZoneRatio;
                }
            }
        }

        float ratio = 0;
        if (reduceRatio < 1) {
            ratio = reduceRatio;
        }

        if (increaseRatio > 1) {
            ratio = increaseRatio;
        }

        logger.info("reduce ratio: " + reduceRatio);
        logger.info("increase ratio: " + increaseRatio);
        logger.info("ratio: " + ratio);
        return ratio;
    }

    private void breakDownElementsToRender() {
        List<TextZone> result = elementsForPagination
                .stream()
                .flatMap(textZone -> {
                    float minHeight = renderHeight * RENDER_HEIGHT_RATIO_BREAK_DOWN;

                    if (textZone instanceof Paragraph
                            && textZone.getHeight() >= minHeight
                            && ((Paragraph) textZone).countLines() >= MIN_LINES_BREAK_DOWN) {

                        return ((Paragraph) textZone).breakDown(NBR_PIECES_BREAK_DOWN).stream();
                    } else {
                        return of(textZone);
                    }
                })
                .collect(toList());

        logger.info("old elements: " + elementsForPagination.size());
        logger.info("new elements: " + result.size());

        // TODO: figure out why there are so many margins on the side (there should be none, right?)
        TextZone textZone = result.stream().filter(el -> el instanceof Paragraph).max(comparing(TextZone::getHeight)).get();
        logger.info("biggest textZone: " + textZone);

        this.elementsForPagination = result;
    }

    private void extractElementsToRender() {
        elementsForPagination = pages
                .stream()
                .flatMap(p -> p.getElementsForPagination().stream())
                .collect(toList());
    }

    private float getQuality() {
        return requestConfig == null ? 1f : requestConfig.getQuality();
    }
}
