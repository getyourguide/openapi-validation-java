package com.getyourguide.openapi.validation.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Caches the response body for validation, except for Server-Sent Events: a {@code text/event-stream}
 * response is written and flushed straight through so each event reaches the client as it is sent.
 * Its body is never validated anyway, as only JSON, XML and HTML responses are.
 */
public class EventStreamAwareContentCachingResponseWrapper extends ContentCachingResponseWrapper {

    public EventStreamAwareContentCachingResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return isEventStream() ? getResponse().getOutputStream() : super.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return isEventStream() ? getResponse().getWriter() : super.getWriter();
    }

    @Override
    public void flushBuffer() throws IOException {
        if (isEventStream()) {
            getResponse().flushBuffer();
        } else {
            super.flushBuffer();
        }
    }

    private boolean isEventStream() {
        var contentType = getContentType();
        return contentType != null
            && contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
