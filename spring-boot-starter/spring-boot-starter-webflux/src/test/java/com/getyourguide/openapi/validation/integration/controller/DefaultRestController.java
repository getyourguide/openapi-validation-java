package com.getyourguide.openapi.validation.integration.controller;

import com.getyourguide.openapi.validation.test.exception.WithResponseStatusException;
import com.getyourguide.openapi.validation.test.exception.WithoutResponseStatusException;
import com.getyourguide.openapi.validation.test.openapi.webflux.DefaultApi;
import com.getyourguide.openapi.validation.test.openapi.webflux.model.PostTestRequest;
import com.getyourguide.openapi.validation.test.openapi.webflux.model.TestResponse;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class DefaultRestController implements DefaultApi {
    @Override
    public Mono<ResponseEntity<TestResponse>> getTest(
        String testCase, LocalDate date, String value,
        ServerWebExchange exchange
    ) {
        if (Objects.equals(testCase, "throwExceptionWithResponseStatus")) {
            return Mono.error(new WithResponseStatusException("Unhandled exception"));
        }
        if (Objects.equals(testCase, "throwExceptionWithoutResponseStatus")) {
            return Mono.error(new WithoutResponseStatusException("Unhandled exception"));
        }

        var responseValue = value != null ? value : "test";
        return Mono.just(ResponseEntity.ok(new TestResponse().value(responseValue)));
    }

    @Override
    public Mono<ResponseEntity<TestResponse>> postTest(
        Mono<PostTestRequest> postTestRequest,
        ServerWebExchange exchange
    ) {
        return postTestRequest.flatMap( request -> {
            var responseStatus = request.getResponseStatusCode();
            if (responseStatus != null && responseStatus == 200) {
                return Mono.just(ResponseEntity.ok(new TestResponse().value(request.getValue())));
            }
            return Mono.just(ResponseEntity.noContent().build());
        });
    }

    @Override
    public Mono<ResponseEntity<TestResponse>> postTestNoBody(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.noContent().build());
    }
}
