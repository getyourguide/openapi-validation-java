package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getyourguide.openapi.validation.integration.exception.WithResponseStatusException;
import com.getyourguide.openapi.validation.integration.exception.WithoutResponseStatusException;
import jakarta.servlet.ServletException;
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
public class NoExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenTestSuccessfulResponseThenReturns200() throws Exception {
        mockMvc.perform(get("/test").accept("application/json"))
            .andDo(print())
            .andExpectAll(
                status().isOk(),
                jsonPath("$.value").value("test")
            );
    }

    @Test
    void whenTestInvalidQueryParamThenReturns400WithoutViolationLogged() throws Exception {
        mockMvc.perform(get("/test").queryParam("date", "not-a-date").contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().is4xxClientError());
        Thread.sleep(10);

        // TODO check there is no reported violation if spec has correct response in there
    }

    @Test
    void whenTestThrowExceptionWithResponseStatusThenReturns500WithoutViolationLogged()
        throws Exception {
        // Note: This case tests that an endpoint that throws an exception that is not handled by any code (no global error handler either)
        //       is correctly intercepted by the library with the response body.
        mockMvc
            .perform(
                get("/test").queryParam("testCase", "throwExceptionWithResponseStatus")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpectAll(
                status().is5xxServerError(),
                result -> assertEquals(WithResponseStatusException.class, result.getResolvedException().getClass())
            );
        Thread.sleep(10);

        // TODO check there is no reported violation if spec has correct response in there

    }

    // Note: Throwing a RuntimeException that has no `@ResponseStatus` annotation will cause `.perform()` to throw.

    @Test
    void whenTestThrowExceptionWithoutResponseStatusThenReturns500WithoutViolationLogged()
        throws Exception {
        // Note: This case tests that an endpoint that throws an exception that is not handled by any code (no global error handler either)
        //       is correctly intercepted by the library with the response body.

        var exception = assertThrows(ServletException.class, () -> {
            mockMvc
                .perform(
                    get("/test").queryParam("testCase", "throwExceptionWithoutResponseStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().is5xxServerError());
        });
        Thread.sleep(10);

        var cause = exception.getCause();
        assertEquals(WithoutResponseStatusException.class, cause.getClass());
        assertEquals("Unhandled exception", cause.getMessage());

        // TODO check there is no reported violation if spec has correct response in there
    }

}
