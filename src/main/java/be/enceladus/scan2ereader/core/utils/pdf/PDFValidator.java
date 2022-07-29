package be.enceladus.scan2ereader.core.utils.pdf;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

@Getter
@Setter
public class PDFValidator {

    private final static Logger logger = Logger.getLogger(PDFUtils.class.getName());

    private byte[] pdfData;
    private File pdfFile;

    @Setter(AccessLevel.NONE)
    private boolean valid;

    @Setter(AccessLevel.NONE)
    private int numberOfPages;

    private PDDocument loadDocument() throws IOException {
        if (pdfData != null) {
            return PDDocument.load(pdfData);
        } else if (pdfFile != null) {
            return PDDocument.load(pdfFile);
        } else {
            throw new IllegalStateException();
        }
    }

    public void validate() {
        try {
            PDDocument document = loadDocument();
            valid = true;
            numberOfPages = document.getNumberOfPages();
        } catch (IOException e) {
        }
    }
}
