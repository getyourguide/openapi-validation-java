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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "openapi.validation.should-fail-on-request-violation=true",
    "openapi.validation.should-fail-on-response-violation=true",
})
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class FailOnViolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestViolationLogger openApiViolationLogger;

    @BeforeEach
    public void setup() {
        openApiViolationLogger.clearViolations();
    }

    @Test
    public void whenInvalidRequestThenReturn400AndNoViolationLogged() throws Exception {
        mockMvc.perform(post("/test").content("{ \"value\": 1 }").contentType(MediaType.APPLICATION_JSON))
            .andExpectAll(
                status().is4xxClientError(),
                content().string(Matchers.blankOrNullString())
            );
        Thread.sleep(100);

        assertEquals(0, openApiViolationLogger.getViolations().size());

        // TODO check that controller did not execute (inject controller here and have addition method to check & clear at setup)
        // TODO check that something else gets logged?
    }

    @Test
    public void whenInvalidResponseThenReturn500AndViolationLogged() throws Exception {
        mockMvc.perform(get("/test").queryParam("value", "invalid-response-value!"))
            .andExpectAll(
                status().is5xxServerError(),
                content().string(Matchers.blankOrNullString())
            );
        Thread.sleep(100);

        assertEquals(1, openApiViolationLogger.getViolations().size());
        var violation = openApiViolationLogger.getViolations().get(0);
        assertEquals("validation.response.body.schema.pattern", violation.getRule());
        assertEquals(Optional.of(200), violation.getResponseStatus());
        assertEquals(Optional.of("/value"), violation.getInstance());
    }
}
