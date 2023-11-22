package com.getyourguide.openapi.validation.integration.controller;

import com.getyourguide.openapi.validation.example.openapi.DefaultApi;
import com.getyourguide.openapi.validation.example.openapi.model.TestResponse;
import com.getyourguide.openapi.validation.integration.exception.WithResponseStatusException;
import com.getyourguide.openapi.validation.integration.exception.WithoutResponseStatusException;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefaultRestController implements DefaultApi {

    @Override
    public ResponseEntity<TestResponse> getTest(String testCase, LocalDate date, String value) {
        if (Objects.equals(testCase, "throwExceptionWithResponseStatus")) {
            throw new WithResponseStatusException("Unhandled exception");
        }
        if (Objects.equals(testCase, "throwExceptionWithoutResponseStatus")) {
            throw new WithoutResponseStatusException("Unhandled exception");
        }

        var responseValue = value != null ? value : "test";
        return ResponseEntity.ok(new TestResponse().value(responseValue));
    }
}
