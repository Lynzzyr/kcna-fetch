/*
 * Copyright © 2025 Brandon Namgoong. Licensed under GNU GPLv3.
 * This project is purely for educational and personal purposes.
 */

package com.lynzzyr.kcnafetch;

/** Exception if broadcast does not exist. */
public class NullBroadcastException extends RuntimeException {
    public NullBroadcastException(String message) {
        super(message);
    }
}
