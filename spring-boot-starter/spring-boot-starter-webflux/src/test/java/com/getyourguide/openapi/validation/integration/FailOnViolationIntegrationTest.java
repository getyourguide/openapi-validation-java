package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.getyourguide.openapi.validation.integration.controller.DefaultRestController;
import com.getyourguide.openapi.validation.test.TestViolationLogger;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(properties = {
    "openapi.validation.should-fail-on-request-violation=true",
    "openapi.validation.should-fail-on-response-violation=true",
})
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class FailOnViolationIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestViolationLogger openApiViolationLogger;

    @SpyBean
    private DefaultRestController defaultRestController;

    @BeforeEach
    public void setup() {
        openApiViolationLogger.clearViolations();
    }

    @Test
    public void whenValidRequestThenReturnSuccessfully() throws Exception {
        webTestClient
            .post().uri("/test")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{ \"value\": \"testing\", \"responseStatusCode\": 200 }")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.value").isEqualTo("testing");
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
        verify(defaultRestController).postTest(any(), any());
    }

    @Test
    public void whenInvalidRequestThenReturn400AndNoViolationLogged() throws Exception {
        webTestClient
            .post().uri("/test")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{ \"value\": 1 }")
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.path").isEqualTo("/test")
            .jsonPath("$.error").isEqualTo("Bad Request");
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
        verify(defaultRestController, never()).postTest(any(), any());

        // TODO check that something else gets logged?
    }

    @Test
    public void whenInvalidResponseThenReturn500AndViolationLogged() throws Exception {
        webTestClient
            .get().uri("/test?value=invalid-response-value!")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is5xxServerError()
            .expectBody()
            .jsonPath("$.status").isEqualTo(500)
            .jsonPath("$.path").isEqualTo("/test")
            .jsonPath("$.error").isEqualTo("Internal Server Error");
        Thread.sleep(100);

        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.response.body.schema.pattern", violation.getRule());
        assertEquals(Optional.of(200), violation.getResponseStatus());
        assertEquals(Optional.of("/value"), violation.getInstance());
        verify(defaultRestController).getTest(any(), any(), any(), any());
    }
}
