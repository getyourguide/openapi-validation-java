package com.getyourguide.openapi.validation.integration.controller;

import com.getyourguide.openapi.validation.test.exception.WithResponseStatusException;
import com.getyourguide.openapi.validation.test.exception.WithoutResponseStatusException;
import com.getyourguide.openapi.validation.test.openapi.web.DefaultApi;
import com.getyourguide.openapi.validation.test.openapi.web.model.PostTestRequest;
import com.getyourguide.openapi.validation.test.openapi.web.model.TestEmailResponse;
import com.getyourguide.openapi.validation.test.openapi.web.model.TestResponse;
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

    @Override
    public ResponseEntity<TestEmailResponse> getTestEmail() {
        return ResponseEntity.ok(new TestEmailResponse().email("mail@example.com"));
    }

    @Override
    public ResponseEntity<TestResponse> postTest(PostTestRequest postTestRequest) {
        var responseStatus = postTestRequest.getResponseStatusCode();
        if (responseStatus != null && responseStatus == 200) {
            return ResponseEntity.ok(new TestResponse().value(postTestRequest.getValue()));
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TestResponse> postTestNoBody() {
        return ResponseEntity.noContent().build();
    }
}
