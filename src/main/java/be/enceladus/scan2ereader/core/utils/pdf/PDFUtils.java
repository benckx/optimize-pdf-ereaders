package be.enceladus.scan2ereader.core.utils.pdf;

import be.enceladus.scan2ereader.core.processing.RequestConfig;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;
import static org.apache.pdfbox.rendering.ImageType.GRAY;

public class PDFUtils {

    private final static Logger logger = Logger.getLogger(PDFUtils.class.getName());

    private static final int MIN_RESOLUTION = 100;
    private static final int DEFAULT_RESOLUTION = 300;

    // uses itext library
    public static void imagesToPDF(List<byte[]> imageDatas, OutputStream outputStream, float compressionRate) throws IOException, DocumentException {
        BufferedImage firstImage = ImageIO.read(new ByteArrayInputStream(imageDatas.get(0)));
        Document document = new Document(new Rectangle(0, 0, firstImage.getWidth(), firstImage.getHeight()));
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        writer.setCompressionLevel(10 - ((int) (10 * compressionRate))); // between 0 and 9
        document.open();

        for (byte[] imageData : imageDatas) {
            document.newPage();
            Image pdfImage = Image.getInstance(imageData);
            pdfImage.setAbsolutePosition(0, 0);
            pdfImage.setBorderWidth(0);
            document.add(pdfImage);
        }
        document.close();
    }

    private static byte[] getByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    public static int getEffectiveMaxPage(RequestConfig requestConfig) {
        int numberOfPagesInDocument = getNumberOfPagesInPDF(requestConfig);
        if (requestConfig.getMaxPage() == null) {
            return numberOfPagesInDocument;
        } else {
            return min(numberOfPagesInDocument, requestConfig.getMaxPage());
        }
    }

    public static int getNumberOfPagesInPDF(RequestConfig requestConfig) {
        try (PDDocument initDocument = loadDocument(requestConfig)) {
            return initDocument.getNumberOfPages();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static PDDocument loadDocument(RequestConfig requestConfig) throws IOException {
        if (requestConfig.isFile()) {
            return PDDocument.load(requestConfig.getPdfFile());
        } else if (requestConfig.isByteArray()) {
            return PDDocument.load(requestConfig.getPdfData());
        } else {
            throw new IllegalStateException();
        }
    }

    public static Map<Integer, BufferedImage> getImagesMap(RequestConfig requestConfig) {
        return getImagesMapAsStream(requestConfig)
                .collect(toMap(ImmutablePair::getLeft, ImmutablePair::getRight));
    }

    public static Stream<ImmutablePair<Integer, BufferedImage>> getImagesMapAsStream(RequestConfig requestConfig) {
        assert requestConfig.getMinPage() >= 1;
        assert requestConfig.getMaxPage() < Integer.MAX_VALUE;

        final int begin = requestConfig.getMinPage();
        final int end = requestConfig.getMaxPage();

        final float resolution = max(DEFAULT_RESOLUTION * requestConfig.getQuality(), MIN_RESOLUTION);

        return IntStream
                .rangeClosed(begin, end)
                .parallel()
                .mapToObj(pageIdx -> {
                    try (PDDocument document = loadDocument(requestConfig)) {
                        logger.info("loading page " + (pageIdx - 1));
                        PDFRenderer pdfRenderer = new PDFRenderer(document);
                        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI((pageIdx - 1), resolution, GRAY);
                        return new ImmutablePair<>(pageIdx, bufferedImage);
                    } catch (IOException e) {
                        logger.warning(e.toString());
                        return null;
                    }
                })
                .filter(Objects::nonNull);
    }
}

