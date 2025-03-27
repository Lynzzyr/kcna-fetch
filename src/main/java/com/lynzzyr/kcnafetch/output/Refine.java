/*
 * Copyright © 2025 Brandon Namgoong. Licensed under GNU GPLv3.
 * This project is purely for educational and personal purposes.
 */

package com.lynzzyr.kcnafetch.output;

import java.io.File;
import java.util.List;

/** Class of static methods for performing any video processing and reencoding with ffmpeg. */
public final class Refine {
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
     * Attempts to search through start of broadcast for timestamps.
     * @return Positions of timestamps in seconds since beginning, if found
     */
    public static List<Integer> searchChapters() {
        return null;
    }

    /**
     * Adds specified chapter marks to video.
     * @return The new File representing a new video file created
     */
    public static File addChapters(File file, List<Integer> timestampts) {
        return null;
    }
}
