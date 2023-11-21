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
public class SomeRestController implements DefaultApi {

    @Override
    public ResponseEntity<TestResponse> getTest(String testCase, LocalDate date) {
        if (Objects.equals(testCase, "throwExceptionWithResponseStatus")) {
            throw new WithResponseStatusException("Unhandled exception");
        }
        if (Objects.equals(testCase, "throwExceptionWithoutResponseStatus")) {
            throw new WithoutResponseStatusException("Unhandled exception");
        }

        return ResponseEntity.ok(new TestResponse().value("test"));
    }
}
