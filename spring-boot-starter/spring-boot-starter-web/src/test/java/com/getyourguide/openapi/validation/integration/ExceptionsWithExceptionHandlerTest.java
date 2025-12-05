package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getyourguide.openapi.validation.test.TestViolationLogger;
import com.getyourguide.openapi.validation.test.exception.WithResponseStatusException;
import com.getyourguide.openapi.validation.test.openapi.web.model.BadRequestResponse;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@SpringBootTest(classes = {
    SpringBootTestConfiguration.class,
    ExceptionsWithExceptionHandlerTest.ExceptionHandlerConfiguration.class,
})
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class ExceptionsWithExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestViolationLogger openApiViolationLogger;

    @BeforeEach
    public void setup() {
        openApiViolationLogger.clearViolations();
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

    @Test
    public void whenTestThrowExceptionWithoutResponseStatusThenReturns500WithoutViolationLogged()
        throws Exception {
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

        // No body as this one is not handled by an exception handler and therefore default body is added by spring boot
        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.response.body.missing", violation.getRule());
        assertEquals(Optional.of(500), violation.getResponseStatus());
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
