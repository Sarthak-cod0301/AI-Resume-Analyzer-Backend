// exception/ProjectAnalysisException.java
package com.example.demo.exception;

public class ProjectAnalysisException extends RuntimeException {
    public ProjectAnalysisException(String message) {
        super(message);
    }
    public ProjectAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}