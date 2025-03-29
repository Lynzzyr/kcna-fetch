/*
 * Copyright © 2025 Brandon Namgoong. Licensed under GNU GPLv3.
 * This project is purely for educational and personal purposes.
 */

package com.lynzzyr.kcnafetch.output;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.tinylog.Logger;

/** Class of static methods for performing any video processing and reencoding with ffmpeg. */
public final class Refine {
    /**
     * Saves snapshot images of video at each specified frame interval to a new folder within the specified directory.
     * @param file The File to take snapshots of
     * @param tempDir The temporary directory within which will be the folder of images, will also be the working directory for FFmpeg
     * @param frameInterval How often snapshots are taken using number of frames
     * @param start The start timestamp in seconds since beginning
     * @param end The end timestamp in seconds since beginning
     */
    public static boolean saveSnapshots(File file, Path tempDir, int start, int end, int frameInterval) {
        // create subdir
        Path images = tempDir.resolve(
            file.getName() + "_images_" +
            Integer.toString(start) + "-" +
            Integer.toString(end));
        images.toFile().mkdir();

        // take images
        try {
            ProcessBuilder ffmpeg = new ProcessBuilder(
                "ffmpeg",
                "-i", file.getName(),
                "-ss", Integer.toString(start),
                "-to", Integer.toString(end),
                "-vf", "select=\'not(mod(n," + Integer.toString(frameInterval) + "))\'",
                "-fps_mode", "vfr",
                images.resolve("image_%04d.jpg").toFile().getAbsolutePath()
            ).directory(tempDir.toFile());
            
            int result = ffmpeg.start().waitFor();

            if (result == 0) {
                Logger.info(
                    "Saved images of {} every {} frame{} from {} to {} seconds",
                    file.getName(),
                    frameInterval,
                    frameInterval > 1 ? "s" : "",
                    start,
                    end
                );
                return true;
            } else {
                Logger.warn("FFmpeg exited with non-zero status code!");
                return false;
            }
        } catch (IOException e) {
            Logger.warn("Unexpected IO error occured whilst creating snapshots!");
            return false;
        } catch (InterruptedException e) {
            Logger.warn("Unexpected thread interruption whilst creating snapshots!");
            return false;
        }
    }

    /**
     * Attempts to remove letterboxing on video if occurs the whole way through.
     * @param file The File to fix
     * @param oldAspect The current aspect ratio
     * @param newAspect The new aspect ratio
     * @param force Will ignore letterboxing check and perform fix regardless
     * @return The new File representing a new video file created if fixed, else returns input File
     */
    public static File fixAspectRatio(File file, int[] oldAspect, int[] newAspect, boolean force) {
        return null;
    }

    /**
     * Attempts to search video for timestamps using OCR API.
     * @param file The File to search through
     * @param apiKey The OCR API key
     * @return Array of timestamps in seconds since beginning
     */
    public static int[] searchTimestamps(File file, String apiKey) {
        return null;
    }

    /**
     * Adds specified timestamps to video.
     * @param file The File to work on
     * @param timestamps Array of timestamps in seconds since beginning
     * @return The new File representing a new video file created
     */
    public static File addTimestamps(File file, int[] timestamps) {
        return null;
    }
}
