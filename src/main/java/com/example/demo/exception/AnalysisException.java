// exception/AnalysisException.java
package com.example.demo.exception;

public class AnalysisException extends RuntimeException {
    public AnalysisException(String message) {
        super(message);
    }
    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}