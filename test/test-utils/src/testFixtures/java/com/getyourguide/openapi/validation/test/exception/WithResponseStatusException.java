package com.getyourguide.openapi.validation.test.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WithResponseStatusException extends RuntimeException {

    public WithResponseStatusException(String message) {
        super(message);
    }
}
