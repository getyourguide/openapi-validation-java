package com.getyourguide.openapi.validation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getyourguide.openapi.validation.integration.controller.EventStreamController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class EventStreamIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void whenEventStreamThenEventsReachTheClientBeforeTheStreamCompletes() throws Exception {
        var result = mockMvc.perform(get("/sse").accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(request().asyncStarted())
            .andReturn();

        assertEquals("data:first\n\n", result.getResponse().getContentAsString());

        var emitter = EventStreamController.CURRENT.get();
        emitter.send("second");
        emitter.complete();

        mockMvc.perform(asyncDispatch(result))
            .andExpectAll(
                status().isOk(),
                content().string("data:first\n\ndata:second\n\n")
            );
    }
}
