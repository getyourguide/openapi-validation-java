package com.getyourguide.openapi.validation.example.logging;

import com.getyourguide.openapi.validation.api.log.LoggerExtension;
import java.io.Closeable;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExampleLoggerExtension implements LoggerExtension {
    @Override
    public Closeable addToLoggingContext(@NonNull Map<String, String> newTags) {
        log.info("addToLoggingContext called with {}", newTags);
        return () -> log.info("Closing context");
    }
}
