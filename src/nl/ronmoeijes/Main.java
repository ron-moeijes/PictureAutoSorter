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
import java.util.stream.Stream;

public class Main {

    private static final Path PICTURE_ROOT = Paths.get("D:\\Afbeeldingen\\Foto's");
    private static final String DATE_PLACEHOLDER = "jjjjmmdd";
    private static Path SOURCE_DIR;
    private static Path TARGET_DIR;
    private static String SUFFIX = "";

    public static void main(String[] args) throws IOException {
        // write your code here

        // create the command line parser
        CommandLineParser parser = new DefaultParser();

// create the Options
        Options options = new Options();
        options.addOption( "c", "camera", false, "create directorys in camera directory" );
        Option suffix = Option.builder("x")
                .longOpt( "suffix" )
                .desc( "specify a suffix for the new directory [Default = name of previous parent directory]" )
                .optionalArg(true)
                .argName( "SUFFIX" )
                .build();
        options.addOption( suffix );
        Option source = Option.builder("s")
                .longOpt( "source" )
                .desc( "specify a source path"  )
                .hasArg()
                .argName( "SOURCE" )
                .build();
        options.addOption( source );
        Option target = Option.builder("t")
                .longOpt( "target" )
                .desc( "specify a target path"  )
                .hasArg()
                .argName( "TARGET" )
                .build();
        options.addOption( target );

        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );

            // validate that TARGET_DIR has been set
            if( line.hasOption( "target" ) ) {
                TARGET_DIR = Paths.get(line.getOptionValue("target"));
            } else if( line.hasOption( "camera" ) ) {
                TARGET_DIR = PICTURE_ROOT.resolve("Fotocamera");
            } else {
                TARGET_DIR = PICTURE_ROOT.resolve("Mobile");
            }
            SOURCE_DIR = Paths.get(line.getOptionValue("source"));
            if( line.hasOption( "suffix" ) ) {
                SUFFIX = "_" + line.getOptionValue("suffix");
                if (SUFFIX.equals("_null")) {
                    SUFFIX = "_" + SOURCE_DIR.getFileName();
                    SUFFIX = SUFFIX.replaceAll("\\d{4}-\\d{2}-\\d{2} ", "");
                }
            }
        }
        catch(ParseException exp ) {
            System.out.println( "Unexpected exception:" + exp.getMessage() );
        }
        System.out.println("SOURCE: " + SOURCE_DIR);
//        System.out.println("TARGET: " + TARGET_DIR + "\\" + DATE_PLACEHOLDER);
        createDateDirectories(SOURCE_DIR,TARGET_DIR, SUFFIX);

//        boolean useCameraRoot = false;
//        Path subRoot;
//        Path mobileRoot;
//        for (String arg : args) {
//            if (arg.equals("-c")) {
//                useCameraRoot = true;
//                break;
//            }
//        }
//        if (useCameraRoot) {
//            subRoot = pictureRoot.resolve("Fotocamera");
//        } else {
//            subRoot = pictureRoot.resolve("Mobiel");
//        }
//        System.out.println(subRoot);
//        createDateFolders(sourceDir,subRoot);
    }

    private static Path createDateTimePath(String str, String suffix) {
        return Paths.get(str.substring(0,10)
                     .concat(suffix)
                     .replaceAll(":","")
                     .replaceAll(" ","_"));
    }

    private static Path createDateTimePath(String str) {
        return Paths.get(str.substring(11)
                            .replaceAll(":","")
                            .replaceAll(" ","_"));
    }

    private static String extractDateTime(Path path) throws IOException {
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
                        return tag.getDescription();
                    }
                }
            }
            System.out.println("FAILED: Could not find original date for " + path.getFileName() +
                    " falling back to last modified time...");
        } catch (ImageProcessingException | IOException e) {
            System.err.println("ERROR: " + e);
        }
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

        return attr.lastModifiedTime().toString();
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
//        System.out.println("Failed to create directory!");
    }

    // Create directory name
//    private static Path createDatePath(Path file) throws IOException {
//        Path file = Paths.get("D:\\Reservekopieën\\Recovery MyPassport\\Afbeeldingen\\Recovered data 03-09 13_13_16\\Deep Scan result\\Existing Partition(NTFS)\\Afbeeldingen\\Back-up\\201507190316_Panasonic\\DCIM\\107_PANA\\P1070128.JPG");
//
//        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
//
//        return Paths.get(attr
//                .creationTime()
//                .toString()
//                .substring(0, 10)
//                .replaceAll("-", ""));
//    }

    // Loop directory
    private static void createDateDirectories(Path source, @NotNull Path target, String suffix) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(sourcePath -> {
                        try {
                            String dateTime = extractDateTime(sourcePath);
                            Path datePath = createDateTimePath(dateTime, suffix);
                            Path prefix = createDateTimePath(dateTime);
                            Path targetPath = target.resolve(datePath);
                            createDirectory(targetPath);
                            Path filePath = targetPath.resolve(prefix + "_" + sourcePath.getFileName());
//                              System.out.println(datePath);
//                              System.out.println(prefix);
//                            System.out.println("TARGET: " + filePath);
//                            System.out.println("SOURCE: " + sourcePath);
                            moveFiles(sourcePath, filePath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }
}

// Other file meta data attributes
//        System.out.println("creationTime: " + attr.creationTime());
//        System.out.println("lastAccessTime: " + attr.lastAccessTime());
//        System.out.println("lastModifiedTime: " + attr.lastModifiedTime());
//
//        System.out.println("isDirectory: " + attr.isDirectory());
//        System.out.println("isOther: " + attr.isOther());
//        System.out.println("isRegularFile: " + attr.isRegularFile());
//        System.out.println("isSymbolicLink: " + attr.isSymbolicLink());
//        System.out.println("size: " + attr.size());

//return Paths.get(tag.getDescription()
//        .substring(0,10)
//        .concat(suffix)
//        .replaceAll(":","")
//        .replaceAll(" ","_"));

//return Paths.get(attr
//        .lastModifiedTime()
//        .toString()
//        .substring(0, 10)
//        .concat(suffix)
//        .replaceAll("-", "")
//        .replaceAll(" ","_"));
