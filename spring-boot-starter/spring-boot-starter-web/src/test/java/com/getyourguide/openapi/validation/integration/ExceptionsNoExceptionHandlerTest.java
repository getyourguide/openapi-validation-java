package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getyourguide.openapi.validation.integration.exception.WithResponseStatusException;
import com.getyourguide.openapi.validation.integration.exception.WithoutResponseStatusException;
import com.getyourguide.openapi.validation.integration.openapi.TestViolationLogger;
import jakarta.servlet.ServletException;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class ExceptionsNoExceptionHandlerTest {

    // These test cases test that requests to an endpoint that throws an exception
    // that is not handled by any code (no global error handler either) is correctly intercepted by the library.

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
                content().string(Matchers.blankOrNullString())
            );
        Thread.sleep(100);

        // No body as this one is not handled by an exception handler and therefore default body is added by spring boot
        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.response.body.missing", violation.getRule());
        assertEquals(Optional.of(400), violation.getResponseStatus());
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
                content().string(Matchers.blankOrNullString()),
                result -> assertEquals(WithResponseStatusException.class, result.getResolvedException().getClass())
            );
        Thread.sleep(100);

        // No body as this one is not handled by an exception handler and therefore default body is added by spring boot
        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.response.body.missing", violation.getRule());
        assertEquals(Optional.of(400), violation.getResponseStatus());
    }

    // Note: Throwing a RuntimeException that has no `@ResponseStatus` annotation will cause `.perform()` to throw.

    @Test
    public void whenTestThrowExceptionWithoutResponseStatusThenReturns500WithoutViolationLogged()
        throws Exception {
        var exception = assertThrows(ServletException.class, () -> {
            mockMvc
                .perform(
                    get("/test").queryParam("testCase", "throwExceptionWithoutResponseStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is5xxServerError());
        });
        Thread.sleep(100);

        var cause = exception.getCause();
        assertEquals(WithoutResponseStatusException.class, cause.getClass());
        assertEquals("Unhandled exception", cause.getMessage());

        // No body as this one is not handled by an exception handler and therefore default body is added by spring boot
        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.response.body.missing", violation.getRule());
        assertEquals(Optional.of(500), violation.getResponseStatus());
    }
}
