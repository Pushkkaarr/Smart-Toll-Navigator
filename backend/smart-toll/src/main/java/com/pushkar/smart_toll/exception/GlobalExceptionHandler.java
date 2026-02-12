package com.pushkar.smart_toll.exception;

import com.pushkar.smart_toll.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPincodeException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidPincodeException(
            InvalidPincodeException ex,
            WebRequest request) {
        log.warn("Invalid pincode exception: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .error("INVALID_PINCODE")
                .message(ex.getMessage() != null ? ex.getMessage() : "Invalid source or destination pincode")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(SamePincodeException.class)
    public ResponseEntity<ErrorResponseDTO> handleSamePincodeException(
            SamePincodeException ex,
            WebRequest request) {
        log.warn("Same pincode exception: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .error("SAME_PINCODE")
                .message(ex.getMessage() != null ? ex.getMessage() : "Source and destination pincodes cannot be the same")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RouteNotAvailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleRouteNotAvailableException(
            RouteNotAvailableException ex,
            WebRequest request) {
        log.error("Route not available exception: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .error("ROUTE_NOT_AVAILABLE")
                .message(ex.getMessage() != null ? ex.getMessage() : "Unable to find route between the given pincodes")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        log.warn("Validation exception: {}", ex.getMessage());
        
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .error("VALIDATION_ERROR")
                .message(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(
            Exception ex,
            WebRequest request) {
        log.error("Unexpected exception: ", ex);
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
