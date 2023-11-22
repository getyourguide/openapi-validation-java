package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.test.TestViolationLogger;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class OpenApiValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestViolationLogger openApiViolationLogger;

    @BeforeEach
    public void setup() {
        openApiViolationLogger.clearViolations();
    }

    @Test
    public void whenTestSuccessfulResponseThenShouldNotLogViolation() throws Exception {
        mockMvc.perform(get("/test"))
            .andExpectAll(
                status().isOk(),
                jsonPath("$.value").value("test")
            );
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    @Test
    public void whenTestValidRequestWithInvalidResponseThenShouldReturnSuccessAndLogViolation() throws Exception {
        mockMvc.perform(get("/test").queryParam("value", "invalid-response-value!"))
            .andExpectAll(
                status().isOk(),
                jsonPath("$.value").value("invalid-response-value!")
            );
        Thread.sleep(100);

        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.response.body.schema.pattern", violation.getRule());
        assertEquals(Optional.of(200), violation.getResponseStatus());
        assertEquals(Optional.of("/value"), violation.getInstance());
    }

    @Test
    public void whenTestInvalidRequestNotHandledBySpringBootThenShouldReturnSuccessAndLogViolation() throws Exception {
        mockMvc.perform(post("/test").content("{ \"value\": 1 }").contentType(MediaType.APPLICATION_JSON))
            .andExpectAll(
                status().isNoContent(),
                content().string(Matchers.blankOrNullString())
            );
        Thread.sleep(100);

        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.request.body.schema.type", violation.getRule());
        assertEquals(Optional.of(204), violation.getResponseStatus());
        assertEquals(Optional.of("/value"), violation.getInstance());
    }

    @Test
    public void whenTestInvalidRequestAndInvalidResponseThenShouldReturnSuccessAndLogViolation() throws Exception {
        mockMvc.perform(
                post("/test")
                    .content("{ \"value\": 1, \"responseStatusCode\": 200 }")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpectAll(
                status().isOk(),
                jsonPath("$.value").value("1")
            );
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
        mockMvc.perform(options("/test"))
            .andExpectAll(
                status().isOk(),
                content().string(Matchers.blankOrNullString())
            );
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    // TODO Add test that fails on request violation immediately (maybe needs separate test class & setup) should not log violation

    @Nullable
    private OpenApiViolation getViolationByRule(List<OpenApiViolation> violations, String rule) {
        return violations.stream()
            .filter(violation -> violation.getRule().equals(rule))
            .findFirst()
            .orElse(null);
    }
}
