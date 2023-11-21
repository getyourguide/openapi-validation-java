package com.getyourguide.openapi.validation.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getyourguide.openapi.validation.example.openapi.model.BadRequestResponse;
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
            .andExpectAll(
                status().is4xxClientError(),
                jsonPath("$.error").value("Invalid parameter")
            );
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
                jsonPath("$.error").value("Unhandled exception")
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

        mockMvc
            .perform(
                get("/test").queryParam("testCase", "throwExceptionWithoutResponseStatus")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpectAll(
                status().is5xxServerError(),
                jsonPath("$.error").value("Unhandled exception")
            );
        Thread.sleep(10);

        // TODO check there is no reported violation if spec has correct response in there
    }

    @ControllerAdvice
    public static class ExceptionHandlerConfiguration {
        @ExceptionHandler(Exception.class)
        public ResponseEntity<?> handle(Exception exception) {
            return ResponseEntity.internalServerError().body(new BadRequestResponse().error("Unhandled exception"));
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<?> handle(MethodArgumentTypeMismatchException exception) {
            return ResponseEntity.badRequest().body(new BadRequestResponse().error("Invalid parameter"));
        }
    }
}
