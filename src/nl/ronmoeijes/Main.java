package nl.ronmoeijes;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.*;
import static java.util.Objects.nonNull;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class Main {

    private static final Path PICTURE_ROOT = Paths.get("D:\\Afbeeldingen\\Foto's");
    private static final String DATE_TIME_REGEX = "\\d{4}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}";
    private static final String SUFFIX_REGEX = "\\d{2,4}\\W\\d{2}\\W\\d{2,4} ";
    private static final Pattern FILE_NUMBER_PATTERN = Pattern.compile("\\d{3}");
    private static final String UNDERSCORE = "_";
    private static final String DOT = ".";
    private static final DateTimeFormatter YEAR_PATTERN = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_PATTERN = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter META_DATA_PATTERN = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private static final String DASHES = "----------------------------------------"
            + "----------------------------------------"
            + "----------------------------------------";
    private static List<String> years = new ArrayList<>();
    private static List<String> dates = new ArrayList<>();
    private static List<String> times = new ArrayList<>();
    private static Path source;
    private static Path target;
    private static boolean addPrefix;
    private static boolean addSuffix;
    private static String suffix = EMPTY;
    private static int duplicates;

    public static void main(String[] args) throws IOException {
        // write your code here

        parseOptions(args);

        System.out.println("SOURCE: " + source);
        System.out.println("DUPLICATE TIMES: " + duplicates);
        moveFiles(source, target);
    }

    private static void parseOptions(String[] args) {
        // create the command line parser
//        step("parsing options");
        CommandLineParser parser = new DefaultParser();

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(createOptions(), args);

            // validate that TARGET_DIR has been set
            if (line.hasOption("target")) {
                target = Paths.get(line.getOptionValue("target"));
            } else {
                target = PICTURE_ROOT;
            }
            source = Paths.get(line.getOptionValue("source"));
            if (line.hasOption("prefix")) {
                addPrefix = true;
            }
            if (line.hasOption("suffix")) {
                addSuffix = true;
                suffix = UNDERSCORE + line.getOptionValue("suffix");
                if (suffix.equals("_null")) {
                    suffix = EMPTY;
                }
            }
        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
        }
    }

    private static Options createOptions() {
        // create the Options
//        step("creating options");
        Options options = new Options();
        options.addOption("p", "prefix", false, "add a prefix for each file");
        Option suffixOption = Option.builder("x")
                .longOpt("suffix")
                .desc("specify a suffix for the new directory [Default = name of previous parent directory]")
                .hasArg()
                .argName("SUFFIX")
                .build();
        options.addOption(suffixOption);
        Option sourceOption = Option.builder("s")
                .longOpt("source")
                .desc("specify a source path")
                .hasArg()
                .argName("SOURCE")
                .build();
        options.addOption(sourceOption);
        Option targetOption = Option.builder("t")
                .longOpt("target")
                .desc("specify a target path")
                .hasArg()
                .argName("TARGET")
                .build();
        options.addOption(targetOption);
        return options;
    }

    private static boolean isSingleDate(Path source) throws IOException {
        step("analysing dates");
        try (Stream<Path> paths = Files.walk(source)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        LocalDateTime dateTime = fetchDateTime(file);
                        addDateTimeToList(dateTime, YEAR_PATTERN, years, false);
                        addDateTimeToList(dateTime, DATE_PATTERN, dates, false);
                        addDateTimeToList(dateTime, TIME_PATTERN, times, true);
                    });
        }
        return (dates.size() == 1);
    }

    private static LocalDateTime fetchDateTime(Path path) {
//        step("fetching datetime");
        File file = path.toFile();
        // There are multiple ways to get a Metadata object for a file

        //
        // SCENARIO 1: UNKNOWN FILE TYPE
        //
        // This is the most generic approach.  It will transparently determine the file type and invoke the appropriate
        // readers.  In most cases, this is the most appropriate usage.  This will handle JPEG, TIFF, GIF, BMP and RAW
        // (CRW/CR2/NEF/RW2/ORF) files and extract whatever metadata is available and understood.
        //
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            //
            // A Metadata object contains multiple Directory objects
            //
            for (Directory directory : metadata.getDirectories()) {

                //
                // Each Directory stores values in Tag objects
                //
                for (Tag tag : directory.getTags()) {
                    if (tag.getTagName().equals("Date/Time Original")) {

                        return LocalDateTime.parse(tag.getDescription(), META_DATA_PATTERN);
                    }
                }
            }
            System.out.print("FAILED: Could not find original date for " + file.getName() +
                    " falling back to last modified time: ");
            String lastModifiedTime = Files
                    .readAttributes(path, BasicFileAttributes.class)
                    .lastModifiedTime()
                    .toString();
            System.out.println(lastModifiedTime);
            return ZonedDateTime.parse(lastModifiedTime, ISO_DATE_TIME).toLocalDateTime();
        } catch (ImageProcessingException e) {
            System.err.println("ERROR: " + e + " for " + file.getName());
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void addDateTimeToList(LocalDateTime dateTime, DateTimeFormatter dateTimeFormatter, List<String> list, boolean checkDuplicates) {
//        step("add datetime to list");
        if (nonNull(dateTime)) {
            String formattedDateTime = dateTime.format(dateTimeFormatter);
            if (!list.contains(formattedDateTime)) {
                list.add(formattedDateTime);
            } else if (checkDuplicates) { duplicates++; }
        }
    }

    private static void createDirectory(Path target) {
        // Create directory
//        step("creating directory");
        File targetDir = target.getParent().toFile();
        targetDir.mkdirs();
//        if (targetDir.mkdir()) {
//            System.out.println("TARGET: " + targetDir);
//        }
//        System.out.println(targetDir);
    }

    private static void moveFiles(Path source, Path target) throws IOException {
        step("moving files");
        try (Stream<Path> paths = Files.walk(source)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        LocalDateTime dateTime = fetchDateTime(file);
                        preparePaths(file, target, dateTime);
                    });
        }
    }

    // Move file
    private static void preparePaths(Path file, Path target, LocalDateTime dateTime) {
        assert target!=null;
        String fileName = file.getFileName().toString();
        String fileNumber = fileName.substring(fileName.length()-7, fileName.length()-4);
        boolean validFileNumber = FILE_NUMBER_PATTERN.matcher(fileNumber).matches();
        String timePrefix = EMPTY;

        if (file.equals(target)) {
            System.out.println("INFO: " + fileName + " was already in " + file.getParent());
        } else if (nonNull(dateTime)) {
            if (addPrefix) { timePrefix = dateTime.format(TIME_PATTERN); }
            if (addSuffix) {
                suffix = dateTime.format(DATE_PATTERN) + suffix;
            }
            else {
                suffix = dateTime.format(DATE_PATTERN);
            }

            if (validFileNumber) {
                moveFile(file, target, suffix, timePrefix + UNDERSCORE + fileNumber + DOT + getExtension(fileName));
            } else {
                moveFile(file, target, suffix,"err" + UNDERSCORE + timePrefix + UNDERSCORE + fileName);
            }
        } else {
            moveFile(file, target, "Other files", fileName);
        }
    }

    private static void moveFile(Path file, Path target, String ... resolution) {
        for (String str : resolution) {
            target = target.resolve(str);
        }
        createDirectory(target);
        try {
            for (int i = 0; target.toFile().exists(); i++) {
                target.resolve(UNDERSCORE + i);
            }
            Files.move(file, target);
            moveMessage(file, target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Simulate movement of file by printing paths
    private static void fakeMoveFile(Path file, Path target, LocalDateTime dateTime) {
        assert target!=null;
        String fileName = file.getFileName().toString();
        String fileNumber = fileName.substring(fileName.length()-7, fileName.length()-4);
        boolean validFileNumber = FILE_NUMBER_PATTERN.matcher(fileNumber).matches();
        if (file.equals(target)) {
            System.out.println("INFO: " + fileName + " was already in " + file.getParent());
        } else if (nonNull(dateTime)) {
            if (validFileNumber) {
                moveMessage(file, target.resolve(dateTime.format(DATE_PATTERN)).resolve(dateTime.format(TIME_PATTERN) + UNDERSCORE + fileNumber + DOT + getExtension(fileName)));
            } else {
                moveMessage(file, target.resolve(dateTime.format(DATE_PATTERN)).resolve("err" + UNDERSCORE + dateTime.format(TIME_PATTERN) + UNDERSCORE + fileName));
            }
        } else {
            moveMessage(file, target.resolve("Other files").resolve(fileName));
        }
    }

    private static void moveMessage(Path file, Path target) {
        System.out.println("MOVED: " + file + " to " + target);
    }

    private static void step(String output) {
        System.out.println(DASHES);
        System.out.println(output.toUpperCase());
        System.out.println(DASHES);
    }
}