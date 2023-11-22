package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getyourguide.openapi.validation.integration.openapi.TestViolationLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    public void whenTestSuccessfulResponseThenReturns200() throws Exception {
        mockMvc.perform(get("/test").accept("application/json"))
            .andExpectAll(
                status().isOk(),
                jsonPath("$.value").value("test")
            );

        assertEquals(0, openApiViolationLogger.getViolations().size());
    }

    // TODO Add test with request violation
    // TODO Add test with response violation
    // TODO Add test with request & response violation in same request
    // TODO Add test that fails on request violation immediately (maybe needs separate test class & setup)
}
