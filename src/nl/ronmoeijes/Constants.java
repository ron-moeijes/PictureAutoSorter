package nl.ronmoeijes;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public final class Constants {

    // Path constants
    static final String PICTURE_ROOT = "D:\\Afbeeldingen\\Foto's";

    // Regex constants
    static final Pattern FILE_NUMBER_PATTERN = Pattern.compile("\\d{3}\\.\\w{3,5}");

    // String constants
    static final String FOLDER_DATE_PATTERN = "\\d{4,8}_";
    static final String UNDERSCORE = "_";
    static final String DOT = ".";
    static final String COLON = ":";
    static final String SPACE = " ";
    static final String DASHES = "---------------------------------------"
            + "---------------------------------------"
            + "---------------------------------------";

    // DateTime constants
    static final DateTimeFormatter META_DATA_PATTERN = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    static final DateTimeFormatter TIME_PATTERN = DateTimeFormatter.ofPattern("HHmmss");
    static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");
    static final DateTimeFormatter YEAR_PATTERN = DateTimeFormatter.ofPattern("yyyy");
}
