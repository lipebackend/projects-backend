package com.taskmanagement.api.infra.web.http.exception;

public class PayloadTooLargeException extends GenericException {
    public PayloadTooLargeException(String message) {
        super(413, message);
    }
}
