package nl.ronmoeijes;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.Objects.nonNull;
import static nl.ronmoeijes.Constants.*;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class Main {

    public static void main(String[] args) {
        // write your code here
        Options options = new Options();
        options.parse(args);
        Path source = options.getSource();
        new Message("source", source).print();
        step("analysing dates");
        options.setSingleDate(hasSingleMatch(source, DATE_PATTERN));
        options.setSingleYear(hasSingleMatch(source, YEAR_PATTERN));
        moveFiles(source, options);
    }

    private static boolean hasSingleMatch(Path source, DateTimeFormatter dateTimeFormatter) {
        List<String> list = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(source)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        LocalDateTime dateTime = fetchDateTime(path);
                        if (nonNull(dateTime)) {
                            String formattedDateTime = dateTime.format(dateTimeFormatter);
                            if (!list.contains(formattedDateTime)) {
                                list.add(formattedDateTime);
                            }
                        }
                    });
            return list.size() == 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void moveFiles(Path source, Options options) {
        step("moving files");
        try (Stream<Path> paths = Files.walk(source)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        LocalDateTime dateTime = fetchDateTime(file);
                        preparePaths(file, dateTime, options);
                    });
            cleanUpFolders(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        } catch (ImageProcessingException | ArrayIndexOutOfBoundsException e) {
            System.err.println("ERROR: " + e + " for " + file.getName());
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Move file
    private static void preparePaths(Path path, LocalDateTime dateTime, Options options) {
        Path target = options.getTarget();
        assert target != null;

        boolean addSuffix = options.addSuffix();
        boolean validFileNumber;
        String fileNumber = EMPTY;
        String fileName = path.getFileName().toString();

        if (fileName.length()>3) {
            fileNumber = fileName.substring(fileName.length() - 7, fileName.length() - 4);
            validFileNumber = FILE_NUMBER_PATTERN.matcher(fileNumber).matches();
        } else { validFileNumber = false; }

        StringBuilder filePrefix = new StringBuilder();
        StringBuilder millenniumFolder = new StringBuilder();
        StringBuilder yearFolder = new StringBuilder();
        StringBuilder dateFolder = new StringBuilder();

        if (path.equals(target)) {
            new Message("info", fileName, "was already in", path.getParent()).print();
        } else if (nonNull(dateTime)) {
            if (options.addPrefix()) {
                filePrefix.append(dateTime.format(TIME_PATTERN)).append(UNDERSCORE);
            }
            if (options.isSingleDate()) {
                dateFolder.append(dateTime.format(DATE_PATTERN));
                if (addSuffix) {
                    appendSuffix(dateFolder, options);
                }
            } else if (!options.addMillenniumFolder() || options.isSingleYear()) {
                yearFolder.append(dateTime.format(YEAR_PATTERN));
                dateFolder.append(dateTime.format(DATE_PATTERN));
                if (addSuffix) {
                    appendSuffix(yearFolder, options);
                }
            }
            else {
                millenniumFolder.append(dateTime.format(YEAR_PATTERN).substring(0,2));
                if (addSuffix) {
                    appendSuffix(millenniumFolder, options);
                }
                yearFolder.append(dateTime.format(YEAR_PATTERN));
                dateFolder.append(dateTime.format(DATE_PATTERN));
            }
            try {
                if (validFileNumber) {
                    moveFile(path, target, millenniumFolder.toString(), yearFolder.toString(), dateFolder.toString(), filePrefix + fileNumber + DOT + getExtension(fileName));
                } else {
                    moveFile(path, target, millenniumFolder.toString(), yearFolder.toString(), dateFolder.toString(), "errors", filePrefix + fileName);
                }
            } catch (Exception e) {
                System.err.println("Unexpected error occured: " + e);
            }
        } else {
            try {
                moveFile(path, target, "Other files", fileName);
            } catch (Exception e) {
                System.err.println("Unexpected error occured: " + e);
            }
        }
    }

    private static void appendSuffix(StringBuilder builder, Options options) {
        String customSuffix = options.getCustomSuffix();

        builder.append(UNDERSCORE);
            if (customSuffix != null) {
                builder.append(customSuffix);
            } else {
                builder.append(options.getSource()
                        .getFileName()
                        .toString()
                        .replaceAll(FOLDER_DATE_PATTERN, ""));
            }
        }

    private static void cleanUpFolders(Path source) {
        File file = source.toFile();
        try {
//            if (file.isDirectory() && file.list() != null && file.list().length > 0) {
//                new Message("warn", source, "still contains files!", "Manual deletion required!").print();
//            } else {
                FileUtils.deleteDirectory(file);
                new Message("deleted", source).print();
//            }
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void moveFile(Path file, Path target, String... resolution) {
        for (String str : resolution) {
            target = target.resolve(str);
        }
        Path targetDir = createTargetDirectory(target);
        try {
            String targetName = target.getFileName().toString();
            for (int i = 1; target.toFile().exists(); i++) {
                target = targetDir.resolve(removeExtension(targetName) + UNDERSCORE + i + DOT + getExtension(targetName));
            }
            Files.move(file, target);
            new Message("moved", file, "to", target).print();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Path createTargetDirectory(Path target) {
        // Create directory
//        step("creating directory");
        File targetDir = target.getParent().toFile();
        if (targetDir.mkdirs()) {
            new Message("created", targetDir.getName(), "in", targetDir.getParent()).print();
        } return targetDir.toPath();
//        System.out.println(targetDir);
    }

    private static void step(String output) {
        System.out.println(DASHES);
        System.out.println(output.toUpperCase());
        System.out.println(DASHES);
    }
}