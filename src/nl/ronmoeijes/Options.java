package nl.ronmoeijes;

import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.ronmoeijes.Constants.PICTURE_ROOT;

class Options {
    private Path source;
    private Path target;
    private boolean addPrefix;
    private String prefix;
    private boolean addSuffix;
    private String customSuffix;
    private boolean generateSuffix;
    private boolean isSingleDate;
    private boolean isSingleYear;

    void parse(String[] args) {
        // create the command line parser
//        step("parsing options");
        CommandLineParser parser = new DefaultParser();

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(create(), args);

            if (!line.hasOption("source")) { throw new ParseException("Source option is required!"); }
            source = Paths.get(line.getOptionValue("source"));
            // validate that TARGET_DIR has been set
            if (line.hasOption("target")) {
                target = Paths.get(line.getOptionValue("target"));
            } else {
                target = Paths.get(PICTURE_ROOT);
            }
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

    private org.apache.commons.cli.Options create() {
        // create the Options
//        step("creating options");
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
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
        options.addOption("p", "prefix", false, "add a prefix for each file");
        Option suffixOption = Option.builder("x")
                .longOpt("suffix")
                .desc("specify a suffix for the new directory [Default = " + PICTURE_ROOT + "]")
                .hasArg()
                .argName("SUFFIX")
                .build();
        options.addOption(suffixOption);

        return options;
    }

    Path getSource() {
        return source;
    }

    Path getTarget() {
        return target;
    }

    boolean addPrefix() {
        return addPrefix;
    }

    public String getPrefix() {
        return prefix;
    }

    boolean addSuffix() {
        return addSuffix;
    }

    String getCustomSuffix() {
        return customSuffix;
    }

    public void setCustomSuffix(String customSuffix) {
        this.customSuffix = customSuffix;
    }

    boolean isSingleDate() {
        return isSingleDate;
    }

    void setSingleDate(boolean singleDate) {
        isSingleDate = singleDate;
    }

    boolean isSingleYear() {
        return isSingleYear;
    }

    void setSingleYear(boolean singleYear) {
        isSingleYear = singleYear;
    }
}
