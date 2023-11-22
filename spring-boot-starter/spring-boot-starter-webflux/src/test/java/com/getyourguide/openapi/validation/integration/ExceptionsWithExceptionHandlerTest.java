package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.getyourguide.openapi.validation.test.TestViolationLogger;
import com.getyourguide.openapi.validation.test.exception.WithResponseStatusException;
import com.getyourguide.openapi.validation.test.exception.WithoutResponseStatusException;
import com.getyourguide.openapi.validation.test.openapi.webflux.model.BadRequestResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebInputException;

@SpringBootTest(classes = {
    SpringBootTestConfiguration.class,
    ExceptionsWithExceptionHandlerTest.ExceptionHandlerConfiguration.class,
})
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class ExceptionsWithExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestViolationLogger openApiViolationLogger;

    @BeforeEach
    public void setup() {
        openApiViolationLogger.clearViolations();
    }

    @Test
    public void whenTestInvalidQueryParamThenReturns400WithoutViolationLogged() throws Exception {
        webTestClient
            .get().uri("/test?date=not-a-date")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().jsonPath("$.error").isEqualTo("ServerWebInputException");
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    @Test
    public void whenTestThrowExceptionWithResponseStatusThenReturns400WithoutViolationLogged()
        throws Exception {
        webTestClient
            .get().uri("/test?testCase=throwExceptionWithResponseStatus")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().jsonPath("$.error").isEqualTo("Unhandled exception");
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    @Test
    public void whenTestThrowExceptionWithoutResponseStatusThenReturns500WithoutViolationLogged()
        throws Exception {
        webTestClient
            .get().uri("/test?testCase=throwExceptionWithoutResponseStatus")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is5xxServerError()
            .expectBody().isEmpty();
        Thread.sleep(100);

        // Note: We return no body on purpose in the exception handler below to test this violation appears.
        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.response.body.missing", violation.getRule());
        assertEquals(Optional.of(500), violation.getResponseStatus());
    }

    @ControllerAdvice
    public static class ExceptionHandlerConfiguration {
        @ExceptionHandler(ServerWebInputException.class)
        public ResponseEntity<?> handle(ServerWebInputException exception) {
            return ResponseEntity.badRequest().body(new BadRequestResponse().error("ServerWebInputException"));
        }

        @ExceptionHandler(WithResponseStatusException.class)
        public ResponseEntity<?> handle(WithResponseStatusException exception) {
            return ResponseEntity.badRequest().body(new BadRequestResponse().error("Unhandled exception"));
        }

        @ExceptionHandler(WithoutResponseStatusException.class)
        public ResponseEntity<?> handle(WithoutResponseStatusException exception) {
            return ResponseEntity.internalServerError().build();
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<?> handle(MethodArgumentTypeMismatchException exception) {
            return ResponseEntity.badRequest().body(new BadRequestResponse().error("Invalid parameter"));
        }
    }
}
