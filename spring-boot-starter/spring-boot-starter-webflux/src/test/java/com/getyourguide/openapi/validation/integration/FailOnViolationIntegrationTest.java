package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getyourguide.openapi.validation.test.TestViolationLogger;
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
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;

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
            .bodyValue("{ \"value\": \"test\", \"responseStatusCode\": 200 }")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.value").isEqualTo("test");
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
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
            .expectBody().isEmpty();
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());

        // TODO check that controller did not execute (inject controller here and have addition method to check & clear at setup)
        // TODO check that something else gets logged?
    }

    @Test
    public void whenInvalidResponseThenReturn500AndViolationLogged() throws Exception {
        webTestClient
            .get().uri("/test?value=invalid-response-value!")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is5xxServerError()
            .expectBody().isEmpty();
        Thread.sleep(100);

        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.response.body.schema.pattern", violation.getRule());
        assertEquals(Optional.of(200), violation.getResponseStatus());
        assertEquals(Optional.of("/value"), violation.getInstance());
    }
}
