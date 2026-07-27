// exception/JobDescriptionNotFoundException.java
package com.example.demo.exception;

public class JobDescriptionNotFoundException extends RuntimeException {
    public JobDescriptionNotFoundException(String message) {
        super(message);
    }
}