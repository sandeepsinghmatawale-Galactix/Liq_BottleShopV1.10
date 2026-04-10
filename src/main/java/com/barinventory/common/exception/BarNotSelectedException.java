package com.barinventory.common.exception;

public class BarNotSelectedException extends RuntimeException {
    public BarNotSelectedException() {
        super("Bar not selected");
    }
}