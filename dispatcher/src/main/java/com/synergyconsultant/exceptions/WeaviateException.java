package com.synergyconsultant.exceptions;

public class WeaviateException extends RuntimeException {
    public WeaviateException(String message, Throwable cause) {
        super(message, cause);
    }
}
