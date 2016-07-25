package com.sam_chordas.android.stockhawk.exceptions;

/**
 * @author gaurav.se on 25/07/16.
 */
public class StockDoesNotExistException extends Exception{
    private final String message;

    public StockDoesNotExistException(final String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
