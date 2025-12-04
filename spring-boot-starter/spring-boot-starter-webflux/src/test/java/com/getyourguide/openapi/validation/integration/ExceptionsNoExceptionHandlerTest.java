package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.getyourguide.openapi.validation.test.TestViolationLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class ExceptionsNoExceptionHandlerTest {

    // These test cases test that requests to an endpoint that throws an exception (Mono.error)
    // that is not handled by any code (no global error handler either) is correctly intercepted by the library.

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
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.path").isEqualTo("/test")
            .jsonPath("$.error").isEqualTo("Bad Request");
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
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.path").isEqualTo("/test")
            .jsonPath("$.error").isEqualTo("Bad Request");
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
            .expectBody()
            .jsonPath("$.status").isEqualTo(500)
            .jsonPath("$.path").isEqualTo("/test")
            .jsonPath("$.error").isEqualTo("Internal Server Error");
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }
}
