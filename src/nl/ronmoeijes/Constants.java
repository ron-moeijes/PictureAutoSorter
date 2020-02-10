package nl.ronmoeijes;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public final class Constants {
    static final String PICTURE_ROOT = "D:\\Afbeeldingen\\Foto's";
    static final Pattern FILE_NUMBER_PATTERN = Pattern.compile("\\d{3}");
    static final String FOLDER_DATE_PATTERN = "\\d{4,8}_";
    static final String UNDERSCORE = "_";
    static final String DOT = ".";
    static final DateTimeFormatter YEAR_PATTERN = DateTimeFormatter.ofPattern("yyyy");
    static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");
    static final DateTimeFormatter TIME_PATTERN = DateTimeFormatter.ofPattern("HHmmss");
    static final DateTimeFormatter META_DATA_PATTERN = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    static final String DASHES = "----------------------------------------"
            + "----------------------------------------"
            + "----------------------------------------";
}
