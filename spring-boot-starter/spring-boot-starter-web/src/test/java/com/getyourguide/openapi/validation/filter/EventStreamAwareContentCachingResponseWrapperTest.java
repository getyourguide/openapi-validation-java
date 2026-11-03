package com.getyourguide.openapi.validation.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class EventStreamAwareContentCachingResponseWrapperTest {

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final EventStreamAwareContentCachingResponseWrapper wrapper =
        new EventStreamAwareContentCachingResponseWrapper(response);

    @Test
    public void whenJsonResponseThenBodyIsCachedUntilCopied() throws IOException {
        wrapper.setContentType("application/json");
        wrapper.getOutputStream().write("{}".getBytes(StandardCharsets.UTF_8));
        wrapper.flushBuffer();

        assertEquals("", response.getContentAsString());
        assertFalse(response.isCommitted());
        assertEquals("{}", new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8));

        wrapper.copyBodyToResponse();

        assertEquals("{}", response.getContentAsString());
    }

    @Test
    public void whenEventStreamThenEachWriteAndFlushReachesTheResponse() throws IOException {
        wrapper.setContentType("text/event-stream;charset=UTF-8");
        wrapper.getOutputStream().write("data: first\n\n".getBytes(StandardCharsets.UTF_8));
        wrapper.flushBuffer();

        assertEquals("data: first\n\n", response.getContentAsString());
        assertTrue(response.isCommitted());
        assertEquals(0, wrapper.getContentSize());
    }

    @Test
    public void whenEventStreamWrittenThroughWriterThenItReachesTheResponse() throws IOException {
        wrapper.setContentType("TEXT/EVENT-STREAM");
        wrapper.getWriter().write("data: first\n\n");
        wrapper.getWriter().flush();

        assertEquals("data: first\n\n", response.getContentAsString());
        assertEquals(0, wrapper.getContentSize());
    }
}
