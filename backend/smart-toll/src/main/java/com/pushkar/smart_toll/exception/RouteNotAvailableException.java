package com.pushkar.smart_toll.exception;

public class RouteNotAvailableException extends RuntimeException {
    public RouteNotAvailableException(String message) {
        super(message);
    }

    public RouteNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
