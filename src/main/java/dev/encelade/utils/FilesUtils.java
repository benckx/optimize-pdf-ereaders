package dev.encelade.utils;

import java.text.DecimalFormat;

public class FilesUtils {

    private final static DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return decimalFormat.format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
