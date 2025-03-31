/*
 * Copyright © 2025 Brandon Namgoong. Licensed under GNU GPLv3.
 * This project is purely for educational and personal purposes.
 */
  
package com.lynzzyr.kcnafetch;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Scanner;

import org.tinylog.Logger;

import com.lynzzyr.kcnafetch.output.Finish;
import com.lynzzyr.kcnafetch.output.Refine;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// Usage: kcnafetch <dir> --start START_DATE --end END_DATE --chromedriver_binary PATH --timeout TIMEOUT --replace_existing --temp_dir DIR --keep_temp --aspect --force_aspect --timestamps --ocr_api PATH
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
        description = "Directory to save the result. Both POSIX and Windows paths are accepted."
    )
    private String dir;

    // options
    @Option(
        names = {"-s", "--start"},
        description = "Start date of range, inclusive. Format in ISO 8601 (YYYY-MM-DD). Leave blank for default value of previous day in local time."
    )
    private String start;
    @Option(
        names = {"-e", "--end"},
        description = "End date of range, inclusve. Format in ISO 8601 (YYYY-MM-DD). Use only when range spans more than 1 day."
    )
    private String end;
    @Option(
        names = {"-b", "--chromedriver_binary"},
        description = "Path to chromedriver binary, if not using Selenium Manager. Both POSIX and Windows paths are accepted."
    )
    private String binaryPath;
    @Option(
        names = {"-tm", "--timeout"},
        description = "HTTP timeout in milliseconds for fetches. Default of 60 seconds (60,000 ms).",
        defaultValue = "60000"
    )
    private int timeout;
    @Option(
        names = {"-r", "--replace_existing"},
        description = "Whether to replace existing broadcasts in the event of an overlap."
    )
    private boolean rp;
    @Option(
        names = {"-tmp", "--temp_dir"},
        description = "Temporary directory to store working video files, if video processing is desired. Will be ignored if no video processing."
    )
    private String tempDir;
    @Option(
        names = {"-k", "--keep_temp"},
        description = "Whether to keep temporary files if processing occured."
    )
    private boolean keepTemp;
    @Option(
        names = {"-a", "--aspect"},
        description = "Whether to remove letterboxing if all frames are letterboxed."
    )
    private boolean aspect;
    @Option(
        names = {"-fa", "--force_aspect"},
        description = "Same as --aspect, but will force the fix instead of making sure all frames are letterboxed."
    )
    private boolean forceAspect;
    @Option(
        names = {"-ts", "--timestamps"},
        description = "Whether to attempt to detect and insert timestamps."
    )
    private boolean timestamps;
    @Option(
        names = {"-ocr", "--ocr_api"},
        description = "Path to text file containing OCR.SPACE OCR API key. Both POSIX and Windows paths are accepted."
    )
    private String apiPath;

    // main program run
    @Override
    public void run() {
        boolean process = aspect || forceAspect || timestamps;

        // check for parameters and options
        if (process && tempDir == null) {
            Logger.error("A temporary directory was not specified despite video processing being requested! Exiting.");
            System.exit(1);
        }

        // create day range
        LocalDate startDate = (start == null)
            ? LocalDate
                .now()
                .minusDays(1)
            : LocalDate.parse(start);
        LocalDate endDate = (end == null)
            ? startDate
            : LocalDate.parse(end);
        
        // last file
        File lastFile;

        // main scraping sequence
        try (Scraper scraper = (binaryPath == null)
            ? new Scraper(startDate)
            : new Scraper(startDate, binaryPath)
        ) {
            // each day in specified range
            for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                scraper.setDate(d);

                // scrape URL
                try {
                    scraper.getBroadcast(
                        scraper.scrapeURL(),
                        process ? new File(tempDir) : new File(dir),
                        timeout,
                        process,
                        rp
                    );
                } catch (URISyntaxException | IOException e) {
                    Logger.error(e, "Unexpected error whilst getting URL request! Exiting");
                    System.exit(1);
                } catch (NullBroadcastException e) {
                    Logger.warn("Broadcast does not exist!");
                    continue;
                }

                lastFile = scraper.getLastFile();

                // video processing
                if (process) {
                    // aspect
                    if (aspect || forceAspect) {
                        lastFile = Refine.fixAspectRatio(lastFile, new int[]{4, 3}, new int[]{16, 9}, forceAspect);
                    }
                    // chapter marks
                    if (timestamps) {
                        try (Scanner scan = new Scanner(apiPath)) {
                            lastFile = Refine.addTimestamps(
                                lastFile,
                                Refine.searchTimestamps(lastFile, new File(tempDir), scan.next())
                            );
                        }
                    }

                    // finish
                    Finish.saveFinal(lastFile, new File(dir), scraper.getFinalFileName());
                    if (!keepTemp) {
                        Finish.cleanTemp(new File(tempDir), false);
                        Logger.info("Cleaned temporary files in {}", tempDir);
                    }
                }
            }
        }
        Logger.info("Done!");
    }

    public static void main(String[] args) {
        new CommandLine(new KCNAFetch()).execute(args);
        System.exit(0);
    }
}