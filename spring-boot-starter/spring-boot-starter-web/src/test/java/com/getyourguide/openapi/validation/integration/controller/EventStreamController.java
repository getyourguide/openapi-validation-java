package com.getyourguide.openapi.validation.integration.controller;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class EventStreamController {
    public static final AtomicReference<SseEmitter> CURRENT = new AtomicReference<>();

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() throws IOException {
        var emitter = new SseEmitter(10_000L);
        emitter.send("first");
        CURRENT.set(emitter);
        return emitter;
    }
}
