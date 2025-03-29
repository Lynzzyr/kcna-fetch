/*
 * Copyright © 2025 Brandon Namgoong. Licensed under GNU GPLv3.
 * This project is purely for educational and personal purposes.
 */

package com.lynzzyr.kcnafetch.output;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.tinylog.Logger;

/** Class of static methods for final operations to a processed file. */
public final class Finish {
    /**
     * Saves specified temporary file as a definitive product.
     * @param temp The temporary File
     */
    public static void saveFinal(File temp, Path destination, String finalFileName) {
        // check destination
        if (!destination.toFile().isDirectory()) {
            Logger.error("Supplied save location(s) is/are not a directory! Exiting.");
            System.exit(1);
        }

        // file copy
        try {
            Files.copy(temp.toPath(), destination.resolve(finalFileName));
        } catch (IOException e) {
            Logger.error(e, "Unexpected error whilst copying processed video file! Exiting");
            System.exit(1);
        }
    }

    public static void cleanTemp(Path tempDir) {

    }
}
