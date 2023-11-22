package com.getyourguide.openapi.validation.test.exception;

public class WithoutResponseStatusException extends RuntimeException {

    public WithoutResponseStatusException(String message) {
        super(message);
    }
}
