package nl.ronmoeijes;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.sun.istack.internal.NotNull;
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
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.*;
import static java.util.Objects.nonNull;
import static java.util.regex.Pattern.matches;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class Main {

    private static final Path PICTURE_ROOT = Paths.get("D:\\Afbeeldingen\\Foto's");
    private static final String DATE_TIME_REGEX = "\\d{4}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}";
    private static final String SUFFIX_REGEX = "\\d{2,4}\\W\\d{2}\\W\\d{2,4} ";
    private static final DateTimeFormatter YEAR_PATTERN = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_PATTERN = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter META_DATA_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private static List<String> years = new ArrayList<>();
    private static List<String> dates = new ArrayList<>();
    private static List<String> times = new ArrayList<>();
    private static Path source;
    private static Path target;
    private static boolean addPrefix;
    private static String suffix = EMPTY;

    public static void main(String[] args) throws IOException {
        // write your code here

        // create the command line parser
        CommandLineParser parser = new DefaultParser();

        // create the Options
        Options options = new Options();
        options.addOption( "p", "prefix", false, "add a prefix for each file" );
        Option suffixOption = Option.builder("x")
                .longOpt( "suffix" )
                .desc( "specify a suffix for the new directory [Default = name of previous parent directory]" )
                .optionalArg(true)
                .argName( "SUFFIX" )
                .build();
        options.addOption( suffixOption );
        Option sourceOption = Option.builder("s")
                .longOpt( "source" )
                .desc( "specify a source path"  )
                .hasArg()
                .argName( "SOURCE" )
                .build();
        options.addOption( sourceOption );
        Option targetOption = Option.builder("t")
                .longOpt( "target" )
                .desc( "specify a target path"  )
                .hasArg()
                .argName( "TARGET" )
                .build();
        options.addOption( targetOption );

        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );

            // validate that TARGET_DIR has been set
            if( line.hasOption( "target" ) ) {
                target = Paths.get(line.getOptionValue("target"));
            } else {
                target = PICTURE_ROOT;
            }
            source = Paths.get(line.getOptionValue("source"));
            if( line.hasOption( "prefix" ) ) {
                addPrefix = true;
            }
            if( line.hasOption( "suffix" ) ) {
                suffix = "_" + line.getOptionValue("suffix");
                if (suffix.equals("_null")) {
                    suffix = "_" + source.getFileName();
                    suffix = suffix.replaceAll(SUFFIX_REGEX, EMPTY);
                }
            }
        }
        catch(ParseException exp ) {
            System.out.println( "Unexpected exception:" + exp.getMessage() );
        }
        System.out.println("SOURCE: " + source);
//        System.out.println("TARGET: " + TARGET_DIR + "\\" + DATE_PLACEHOLDER);
        analyseDates(source);
    }

    private static boolean analyseDates(Path source) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        LocalDateTime dateTime = fetchDateTime(path);
                        addDateTimeToList(dateTime,YEAR_PATTERN,years);
                        addDateTimeToList(dateTime,DATE_PATTERN,dates);
                        addDateTimeToList(dateTime,TIME_PATTERN,times);
                    });
        }
        System.out.println(years);
        System.out.println(dates);
        System.out.println(times);
        return !(dates.size() == 1) && years.size() == 1;
    }

    private static LocalDateTime fetchDateTime(Path path) {
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

                        return LocalDateTime.parse(tag.getDescription(), META_DATA_FORMATTER);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void addDateTimeToList(LocalDateTime dateTime, DateTimeFormatter dateTimeFormatter, List<String> list){
        if (nonNull(dateTime)) {
            String formattedDateTime = dateTime.format(dateTimeFormatter);
            if (!list.contains(formattedDateTime)) {
                list.add(formattedDateTime);
                System.out.println(formattedDateTime);
            }
        }
    }

    // Move file
    private static void moveFiles(Path source, Path target) throws IOException {
        Files.move(source, target);
        if (source.equals(target)) {
            System.out.println("INFO: " + source.getFileName() + " was already in " + source.getParent());
        } else {
            System.out.println("MOVED: " + source.getFileName() + " moved from " + source + " to " + target);
        }
    }

    // Create directory
    private static void createDirectory(Path target) {
        File targetDir = target.toFile();
        if (targetDir.mkdir()) {
            System.out.println("TARGET: " + targetDir);
        }
    }

//    // Loop directory
//    private static void createDateDirectories(Path source, @NotNull Path target, boolean addPrefix, String suffix) throws IOException {
//        try (Stream<Path> paths = Files.walk(source)) {
//            paths
//                    .filter(Files::isRegularFile)
//                    .forEach(path -> {
//                        try {
//                            String dateTime = fetchDateTime(path);
//                            Path datePath = createDateTimePath(dateTime, suffix);
//                            Path targetPath = target.resolve(datePath);
//                            createDirectory(targetPath);
//                            Path filePath;
//                            if (addPrefix) {
//                                Path prefix = createDateTimePath(dateTime);
//                                filePath = targetPath.resolve(prefix + "_" + path.getFileName());
//                            } else {
//                                filePath = targetPath.resolve(path.getFileName());
//                            }
//                            moveFiles(path, filePath);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    });
//        }
//    }
}
