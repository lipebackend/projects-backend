package com.taskmanagement.api.infra.web.http.exception;

public class GenericException extends RuntimeException {
    private final int statusCode;
    private final Object details;
    protected GenericException(int statusCode, String message) {
        this(statusCode, message, null);
    }
    protected GenericException(int statusCode, String message, Object details) {
        super(message);
        this.statusCode = statusCode;
        this.details = details;
    }
    public int getStatusCode() {
        return statusCode;
    }
    public Object getDetails() {
        return details;
    }
}