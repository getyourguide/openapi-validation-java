package com.getyourguide.openapi.validation.integration.exception;

public class WithoutResponseStatusException extends RuntimeException {

    public WithoutResponseStatusException(String message) {
        super(message);
    }
}
