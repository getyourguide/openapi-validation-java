package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.test.TestViolationLogger;
import com.getyourguide.openapi.validation.test.openapi.webflux.model.TestResponse;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
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

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class OpenApiValidationIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestViolationLogger openApiViolationLogger;

    @BeforeEach
    public void setup() {
        openApiViolationLogger.clearViolations();
    }

    @Test
    public void whenTestSuccessfulResponseThenShouldNotLogViolation() throws Exception {
        webTestClient
            .get().uri("/test")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(TestResponse.class)
            .consumeWith(serverResponse -> {
                assertNotNull(serverResponse.getResponseBody());
                assertEquals("test", serverResponse.getResponseBody().getValue());
            });
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    @Test
    public void whenTestValidRequestWithInvalidResponseThenShouldReturnSuccessAndLogViolation() throws Exception {
        webTestClient
            .get().uri("/test?value=invalid-response-value!")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(TestResponse.class)
            .consumeWith(serverResponse -> {
                assertNotNull(serverResponse.getResponseBody());
                assertEquals("invalid-response-value!", serverResponse.getResponseBody().getValue());
            });
        Thread.sleep(100);

        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.response.body.schema.pattern", violation.getRule());
        assertEquals(Optional.of(200), violation.getResponseStatus());
        assertEquals(Optional.of("/value"), violation.getInstance());
    }

    @Test
    public void whenTestInvalidRequestNotHandledBySpringBootThenShouldReturnSuccessAndLogViolation() throws Exception {
        webTestClient
            .post().uri("/test")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{ \"value\": 1 }")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty();
        Thread.sleep(100);

        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.request.body.schema.type", violation.getRule());
        assertEquals(Optional.of(204), violation.getResponseStatus());
        assertEquals(Optional.of("/value"), violation.getInstance());
    }


    @Test
    public void whenTestInvalidRequestAndInvalidResponseThenShouldReturnSuccessAndLogViolation() throws Exception {
        webTestClient
            .post().uri("/test")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{ \"value\": 1, \"responseStatusCode\": 200 }")
            .exchange()
            .expectStatus().isOk()
            .expectBody(TestResponse.class)
            .consumeWith(serverResponse -> {
                assertNotNull(serverResponse.getResponseBody());
                assertEquals("1", serverResponse.getResponseBody().getValue());
            });
        Thread.sleep(100);

        var violations = openApiViolationLogger.getViolations();
        assertEquals(2, violations.size());
        var violation = getViolationByRule(violations, "validation.response.body.schema.pattern");
        assertNotNull(violation);
        assertEquals(Optional.of(200), violation.getResponseStatus());
        assertEquals(Optional.of("/value"), violation.getInstance());
        var violation2 = getViolationByRule(violations, "validation.request.body.schema.type");
        assertNotNull(violation2);
        assertEquals(Optional.of(200), violation2.getResponseStatus());
        assertEquals(Optional.of("/value"), violation2.getInstance());
    }

    @Test
    public void whenTestOptionsCallThenShouldNotValidate() throws Exception {
        // Note: Options is not in the spec and would report a violation if it was validated.
        webTestClient
            .options().uri("/test")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().isEmpty();
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    @Test
    public void whenTestNoBodyThenShouldReturnSuccessAndNoViolation() throws Exception {
        webTestClient
            .post().uri("/test/no-body")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty();
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    @Nullable
    private OpenApiViolation getViolationByRule(List<OpenApiViolation> violations, String rule) {
        return violations.stream()
            .filter(violation -> violation.getRule().equals(rule))
            .findFirst()
            .orElse(null);
    }
}
