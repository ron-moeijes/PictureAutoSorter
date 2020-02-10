package nl.ronmoeijes;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

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

import static java.time.format.DateTimeFormatter.*;
import static java.util.Objects.nonNull;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static nl.ronmoeijes.Constants.*;

public class Main {

    public static void main(String[] args) throws IOException {
        // write your code here
        Options options = new Options();
        options.parse(args);
        System.out.println("SOURCE: " + options.getSource());
        options.setSingleDate(hasSingleMatch(options.getSource(), DATE_PATTERN));
        options.setSingleYear(hasSingleMatch(options.getSource(), YEAR_PATTERN));
        moveFiles(options);
    }

    private static boolean hasSingleMatch(Path source, DateTimeFormatter dateTimeFormatter) {
//        step("analysing dates");
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

    private static void moveFiles(Options options) throws IOException {
        step("moving files");
        try (Stream<Path> paths = Files.walk(options.getSource())) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        LocalDateTime dateTime = fetchDateTime(file);
                        preparePaths(file, dateTime, options);
                    });
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
        } catch (ImageProcessingException e) {
            System.err.println("ERROR: " + e + " for " + file.getName());
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Move file
    private static void preparePaths(Path file, LocalDateTime dateTime, Options options) {
        Path target = options.getTarget();
        String customSuffix = options.getCustomSuffix();

        assert target != null;

        String fileName = file.getFileName().toString();
        String fileNumber = fileName.substring(fileName.length() - 7, fileName.length() - 4);
        boolean validFileNumber = FILE_NUMBER_PATTERN.matcher(fileNumber).matches();

        StringBuilder filePrefix = new StringBuilder();
        StringBuilder folderName = new StringBuilder();
        StringBuilder subFolderName = new StringBuilder();

        if (file.equals(target)) {
            System.out.println("INFO: " + fileName + " was already in " + file.getParent());
        } else if (nonNull(dateTime)) {
            if (options.addPrefix()) {
                filePrefix.append(dateTime.format(TIME_PATTERN)).append(UNDERSCORE);
            }
            if (options.isSingleDate()) {
                folderName.append(dateTime.format(DATE_PATTERN));
            } else {
                folderName.append(dateTime.format(YEAR_PATTERN));
                subFolderName.append(dateTime.format(DATE_PATTERN));
            }
            if (options.addSuffix()) {
                folderName.append(UNDERSCORE);
                if (!customSuffix.equals(EMPTY)) {
                    folderName.append(customSuffix);
                } else {
                    folderName.append(options.getSource()
                            .getFileName()
                            .toString()
                            .replaceAll(FOLDER_DATE_PATTERN, ""));
                }
            }

            if (validFileNumber) {
                moveFile(file, target, folderName.toString(), subFolderName.toString(), filePrefix + fileNumber + DOT + getExtension(fileName));
            } else {
                moveFile(file, target, folderName.toString(), subFolderName.toString(), "err_" + filePrefix + fileName);
            }
        } else {
            moveFile(file, target, "Other files", fileName);
        }
    }

    private static void moveFile(Path file, Path target, String... resolution) {
        for (String str : resolution) {
            target = target.resolve(str);
        }
        createTargetDirectory(target);
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

    private static void createTargetDirectory(Path target) {
        // Create directory
//        step("creating directory");
        File targetDir = target.getParent().toFile();
        if (targetDir.mkdirs()) {
            System.out.println(targetDir.getName() + " created in " + targetDir.getParent());
        }
//        System.out.println(targetDir);
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