// exception/DashboardException.java
package com.example.demo.exception;

public class DashboardException extends RuntimeException {
    public DashboardException(String message) {
        super(message);
    }
    public DashboardException(String message, Throwable cause) {
        super(message, cause);
    }
}