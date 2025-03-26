package com.lynzzyr.kcnafetch;

/** Exception if broadcast does not exist. */
public class NullBroadcastException extends RuntimeException {
    public NullBroadcastException(String message) {
        super(message);
    }
}
