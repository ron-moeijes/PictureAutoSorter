package nl.ronmoeijes;

import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.ronmoeijes.Constants.PICTURE_ROOT;

class Options {
    static Path source;
    static Path target;
    static boolean addPrefix;
    static String prefix;
    static boolean addSuffix;
    static String customSuffix;


    static void parseOptions(String[] args) {
        // create the command line parser
//        step("parsing options");
        CommandLineParser parser = new DefaultParser();

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(createOptions(), args);

            if (!line.hasOption("source")) { throw new ParseException("Source option is required!"); }

            // validate that TARGET_DIR has been set
            if (line.hasOption("target")) {
                target = Paths.get(line.getOptionValue("target"));
            } else {
                target = Paths.get(PICTURE_ROOT);
            }
           source = Paths.get(line.getOptionValue("source"));
            if (line.hasOption("prefix")) {
                addPrefix = true;
            }
            if (line.hasOption("suffix")) {
                addSuffix = true;
                customSuffix = line.getOptionValue("suffix");
            }
        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
        }
    }

    private static org.apache.commons.cli.Options createOptions() {
        // create the Options
//        step("creating options");
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
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
}
