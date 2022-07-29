package dev.encelade.ocr;

import dev.encelade.ocr.model.Page;
import dev.encelade.ocr.model.Word;
import net.sourceforge.tess4j.Tesseract;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Logger;

import static dev.encelade.ocr.Tess4IteratorLevel.WORDS;
import static dev.encelade.utils.TimeUtils.formatMillis;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class OCR {

    private final static Logger logger = Logger.getLogger(OCR.class.getName());

    private final Tesseract instance;

    {
        instance = new Tesseract();
        instance.setLanguage("eng");
        // instance.setTessVariable("load_system_dawg", "0");
    }

    public Page analyze(BufferedImage image, Integer... idx) {
        return analyze(image, asList(idx));
    }

    public Page analyze(BufferedImage image, List<Integer> idx) {
        Page page = new Page();
        page.setOriginalImage(image);
        page.setIdx(idx);

        // words
        long beginLines = currentTimeMillis();

        List<Word> words = instance
                .getWords(image, WORDS.ordinal())
                .stream()
                .map(tesseractWord -> {
                    Word word = new Word(tesseractWord.getBoundingBox(), page);
                    word.setText(tesseractWord.getText());
                    word.setConfidence(tesseractWord.getConfidence() / 100);
                    return word;
                })
                .filter(Word::isValid)
                .filter(page::isNotWithinMargin)
                .collect(toList());

        logger.info("detect and filter words " + idx + " -> " + formatMillis(currentTimeMillis() - beginLines));

        page.setWords(words);

        return page;
    }
}
