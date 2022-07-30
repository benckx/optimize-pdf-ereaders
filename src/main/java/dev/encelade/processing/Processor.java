package dev.encelade.processing;

import dev.encelade.ocr.OCR;
import dev.encelade.ocr.model.Page;
import dev.encelade.pagination.Paginator;
import dev.encelade.utils.Counter;
import dev.encelade.utils.pdf.PDFUtils;
import com.itextpdf.text.DocumentException;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static dev.encelade.utils.pdf.PDFUtils.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.lang3.StringUtils.leftPad;

public class Processor {

    private final static Logger logger = Logger.getLogger(Processor.class.getName());

    private static final boolean PRINT_OUTPUT = false;

    private final RequestConfig requestConfig;

    private List<Page> pages;
    private List<byte[]> outputImages;
    private Thread thread;
    private Paginator paginator;

    private ProcessorStatus status = ProcessorStatus.INITIATED;
    private Counter processedPages;
    private int numberOfPagesToProcess;

    public Processor(RequestConfig.RequestConfigBuilder builder) {
        this(builder.build());
    }

    public Processor(RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
        int effectiveMaxPage = getEffectiveMaxPage(requestConfig);
        this.numberOfPagesToProcess = effectiveMaxPage - requestConfig.getMinPage() + 1;
        requestConfig.setMaxPage(effectiveMaxPage);
        logger.info("number of pages to process: " + numberOfPagesToProcess);

        assert requestConfig.validate();
    }

    public float getRatio() {
        if (status == ProcessorStatus.FINISHED) {
            return paginator.getRatio();
        } else {
            throw new IllegalStateException();
        }
    }

    public int getNumberOfPagesToProcess() {
        return pages.size();
    }

    public float getProgress() {
        if (status == ProcessorStatus.RENDERING) {
            return paginator.getProgress();
        }

        if (status == ProcessorStatus.FINISHED) {
            return 1f;
        }

        if (processedPages == null) {
            return 0;
        }

        float progress = processedPages.getValue() / (float) numberOfPagesToProcess;
        assert progress <= 1;
//        logger.info("processed pages: " + processedPages.getValue());
        return progress;
    }

    public ProcessorStatus getStatus() {
        return status;
    }

    public Processor process() {
        thread = new Thread(new ProcessorRunnable() {
        });
        thread.start();

        return this;
    }

    public void joinThread() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isAlive() {
        return thread.isAlive();
    }

    private class ProcessorRunnable implements Runnable {

        // FIXME: can we get this number dynamically?
        private static final float PROGRESS_STEP = 0.2f;

        @Override
        public void run() {
            status = ProcessorStatus.ANALYZING;
            processedPages = new Counter();

            pages = getImagesMapAsStream(requestConfig)
                    .map(pair -> {
                        processedPages.increment(PROGRESS_STEP);
                        BufferedImage image = pair.getValue();
                        int idx = pair.getKey();
                        Page page = new OCR().analyze(image, idx);
                        processedPages.increment(PROGRESS_STEP);
                        return page;
                    })
                    .flatMap(page -> {
                        boolean isTwoPagesLayout = page.detectTwoPagesLayout();
                        Stream<Page> result = isTwoPagesLayout ? page.splitForLayout().stream() : of(page);
                        processedPages.increment(PROGRESS_STEP);
                        return result;
                    })
                    .peek(Page::detectTextBlocks)
                    .map(page -> {
                        if (requestConfig.isCorrectAngle()) {
                            page.detectCorrectiveAngle();
                            return page.correctAngle();
                        }

                        processedPages.increment(PROGRESS_STEP / (float) page.getSplitIn());
                        return page;
                    })
                    .peek(page -> {
                        page.detectParagraphs();
                        processedPages.increment(PROGRESS_STEP / (float) page.getSplitIn());
                    })
                    .collect(toList());

            status = ProcessorStatus.RENDERING;
            paginator = new Paginator(pages, requestConfig);
            paginator.process();
            paginator.joinThread();
            outputImages = paginator.getOutputImages();
            status = ProcessorStatus.FINISHED;

            if (PRINT_OUTPUT) {
                printOutput();
            }
        }
    }

    private String getRequestFileName() {
        return requestConfig.getPdfFile().getName().split(".pdf")[0];
    }

    public File writeToPDFFile(String path) {
        if (status != ProcessorStatus.FINISHED) {
            throw new IllegalStateException();
        }

        try {
            File outputFile = new File(path);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            PDFUtils.imagesToPDF(this.outputImages, fileOutputStream, this.requestConfig.getQuality());
            return outputFile;
        } catch (DocumentException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] writeToByteArray() {
        if (status != ProcessorStatus.FINISHED) {
            throw new IllegalStateException();
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            imagesToPDF(outputImages, byteArrayOutputStream, requestConfig.getQuality());
            return byteArrayOutputStream.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    private void printOutput() {
        if (PRINT_OUTPUT) {
            int i = 0;
            for (byte[] image : outputImages) {
                String id = leftPad(Integer.toString(i++), 3, '0');
                try {
                    writeByteArrayToFile(new File("OUTPUT_" + id + ".jpg"), image);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
}
