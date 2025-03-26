package com.lynzzyr.kcnafetch;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// Usage: kcnafetch <start> <location> --end END_DATE --remove_existing --aspect --timestamps
@Command(
    name = "kcnafetch",
    mixinStandardHelpOptions = true,
    description = "A needlessly-comprehensive webscraping utility for fetching Korean Central Television broadcast archives",
    version = "kcnafetch 0.1"
)
public class KCNAFetch implements Runnable {
    // parameters
    @Parameters(
        index = "0",
        description = "Start date of range. Format in ISO 8601 (YYYY-MM-DD). If range spans only 1 day, use this parameter exclusively.",
        defaultValue = ""
    )
    private String start;
    @Parameters(
        index = "1",
        description = "Directory to save the result. Both POSIX and Windows paths are accepted.",
        defaultValue = ""
    )
    private String location;

    // options
    @Option(
        names = {"-e", "--end"},
        description = "End date of range. Format in ISO 8601 (YYYY-MM-DD). Use only when range spans more than 1 day."
    )
    private String end;
    @Option(
        names = {"-r", "--remove_existing"},
        description = "Whether to remove existing broadcasts in the event of an overlap."
    )
    private boolean rm;
    @Option(
        names = {"-a", "--aspect"},
        description = "Whether to attempt an aspect ratio fix to remove letterboxing."
    )
    private boolean aspect;
    @Option(
        names = {"-t", "--timestamps"},
        description = "Whether to attempt to detect and insert timestamps."
    )
    private boolean timestamps;

    // main program actions
    @Override
    public void run() {
        
    }

    public static void main(String[] args) {
        new CommandLine(new KCNAFetch()).execute(args);
    }
}