package com.taskmanagement.api.infra.web.http.exception;

public class UnsupportedMediaTypeException extends GenericException {
    public UnsupportedMediaTypeException(String message) {
        super(415, message);
    }
}