package com.pushkar.smart_toll.exception;

public class InvalidPincodeException extends RuntimeException {
    public InvalidPincodeException(String message) {
        super(message);
    }

    public InvalidPincodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
