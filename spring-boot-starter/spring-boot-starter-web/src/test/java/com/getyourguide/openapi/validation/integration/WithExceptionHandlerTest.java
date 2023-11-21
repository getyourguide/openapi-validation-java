package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getyourguide.openapi.validation.example.openapi.model.BadRequestResponse;
import com.getyourguide.openapi.validation.integration.exception.WithResponseStatusException;
import com.getyourguide.openapi.validation.integration.openapi.TestViolationLogger;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@SpringBootTest(classes = {SpringBootTestApplication.class,
    WithExceptionHandlerTest.ExceptionHandlerConfiguration.class})
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class WithExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestViolationLogger openApiViolationLogger;

    @BeforeEach
    public void setup() {
        openApiViolationLogger.clearViolations();
    }

    @Test
    public void whenTestSuccessfulResponseThenReturns200() throws Exception {
        mockMvc.perform(get("/test").accept("application/json"))
            .andExpectAll(
                status().isOk(),
                jsonPath("$.value").value("test")
            );


        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    @Test
    public void whenTestInvalidQueryParamThenReturns400WithoutViolationLogged() throws Exception {
        mockMvc.perform(get("/test").queryParam("date", "not-a-date").contentType(MediaType.APPLICATION_JSON))
            .andExpectAll(
                status().is4xxClientError(),
                jsonPath("$.error").value("Invalid parameter")
            );
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    @Test
    public void whenTestThrowExceptionWithResponseStatusThenReturns400WithoutViolationLogged()
        throws Exception {
        // Note: This case tests that an endpoint that throws an exception that is not handled by any code (no global error handler either)
        //       is correctly intercepted by the library with the response body.
        mockMvc
            .perform(
                get("/test").queryParam("testCase", "throwExceptionWithResponseStatus")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpectAll(
                status().is4xxClientError(),
                jsonPath("$.error").value("Unhandled exception")
            );
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    // Note: Throwing a RuntimeException that has no `@ResponseStatus` annotation will cause `.perform()` to throw.

    @Test
    public void whenTestThrowExceptionWithoutResponseStatusThenReturns500WithoutViolationLogged()
        throws Exception {
        // Note: This case tests that an endpoint that throws an exception that is not handled by any code (no global error handler either)
        //       is correctly intercepted by the library with the response body.

        mockMvc
            .perform(
                get("/test").queryParam("testCase", "throwExceptionWithoutResponseStatus")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpectAll(
                status().is5xxServerError(),
                content().string(Matchers.blankOrNullString())
            );
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    @ControllerAdvice
    public static class ExceptionHandlerConfiguration {
        @ExceptionHandler(Exception.class)
        public ResponseEntity<?> handle(Exception exception) {
            return ResponseEntity.internalServerError().build();
        }

        @ExceptionHandler(WithResponseStatusException.class)
        public ResponseEntity<?> handle(WithResponseStatusException exception) {
            return ResponseEntity.badRequest().body(new BadRequestResponse().error("Unhandled exception"));
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<?> handle(MethodArgumentTypeMismatchException exception) {
            return ResponseEntity.badRequest().body(new BadRequestResponse().error("Invalid parameter"));
        }
    }
}
