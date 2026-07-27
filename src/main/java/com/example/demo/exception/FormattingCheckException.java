// exception/FormattingCheckException.java
package com.example.demo.exception;

public class FormattingCheckException extends RuntimeException {
    public FormattingCheckException(String message) {
        super(message);
    }
    public FormattingCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}