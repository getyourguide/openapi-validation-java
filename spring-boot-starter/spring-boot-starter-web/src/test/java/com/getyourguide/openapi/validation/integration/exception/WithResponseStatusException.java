package com.getyourguide.openapi.validation.integration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class WithResponseStatusException extends RuntimeException {

    public WithResponseStatusException(String message) {
        super(message);
    }
}
