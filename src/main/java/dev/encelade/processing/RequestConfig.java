package dev.encelade.processing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

// TODO: validation
// TODO: resolution [WIDTH, HEIGHT]

@Getter
@Setter
@Builder
public class RequestConfig {

    private byte[] pdfData;
    private File pdfFile;
    private List<BufferedImage> images;

    private int minPage;
    private Integer maxPage;
    private boolean correctAngle;
    private float quality;

    @Setter(AccessLevel.NONE)
    private String errorDescription;

    @SuppressWarnings("unused")
    public static class RequestConfigBuilder {
        private int minPage = 1;
        private boolean correctAngle = true;
        private float quality = 1f;
    }

    public boolean isFile() {
        return pdfFile != null;
    }

    public boolean isByteArray() {
        return pdfData != null;
    }

    public boolean validate() {
        if (minPage <= 0) {
            errorDescription = "minPage can not be < 0";
            return false;
        }

        if (maxPage != null && minPage > maxPage) {
            errorDescription = "minPage must be <= maxPage";
            return false;
        }

        if (quality <= 0.5f && quality > 1f) {
            errorDescription = "quality must be [0.5f-1f]";
            return false;
        }

        if (pdfData == null && pdfFile == null) {
            errorDescription = "No data provided";
            return false;
        }

        return true;
    }
}
