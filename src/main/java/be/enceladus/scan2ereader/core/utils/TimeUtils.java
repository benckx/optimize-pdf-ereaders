package be.enceladus.scan2ereader.core.utils;

public class TimeUtils {

    public static String formatMillis(long millis) {
        if (millis < 1000) {
            return millis + " ms.";
        } else {
            return millis / 1000d + " sec.";
        }
    }

}
