/*
 * Copyright © 2025 Brandon Namgoong. Licensed under GNU GPLv3.
 * This project is purely for educational and personal purposes.
 */

package com.lynzzyr.kcnafetch;

/** Constants for entire program. */
public final class Constants {
    public static final class ScraperConstants {
        public static final int PROGRESS_BAR_WIDTH  = 50;

        public static final int CURSOR_OFFSET_X     = 10;
        public static final int CURSOR_OFFSET_Y     = 10;
        public static final int WAIT_FOR_JS         = 30; // seconds
        public static final int FETCH_ATTEMPTS      = 3;
        public static final int BYTES_PER_BUFFER    = 8192;
    }
    
    public static final class RefineConstants {
        public static final String API_URL          = "http://api.ocr.space/parse/image";

        public static final int PRIMARY_START       = 300; // 5 min
        public static final int PRIMARY_END         = 720; // 12 min
        public static final int LATER_START         = 900; // 15 min
        public static final int LATER_END           = 2400; // 40 min
        public static final int INTERVAL            = 125; // 5 sec
        public static final int[] BOX               = {250, 110, 30, 75};
    }
}
