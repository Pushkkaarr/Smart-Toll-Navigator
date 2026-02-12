package com.pushkar.smart_toll.exception;

public class SamePincodeException extends RuntimeException {
    public SamePincodeException(String message) {
        super(message);
    }

    public SamePincodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
