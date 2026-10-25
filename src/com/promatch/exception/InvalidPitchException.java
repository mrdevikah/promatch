package com.promatch.exception;

/**
 * Custom unchecked exception thrown when a project pitch contains 
 * invalid or missing data (e.g., empty title, invalid team size, or missing required skills).
 */
public class InvalidPitchException extends RuntimeException {
    
    // Default constructor
    public InvalidPitchException() {
        super("Error: The project pitch is invalid.");
    }

    // Constructor that accepts a custom error message
    public InvalidPitchException(String message) {
        super(message);
    }

    // Constructor that accepts a message and a cause
    public InvalidPitchException(String message, Throwable cause) {
        super(message, cause);
    }
}
