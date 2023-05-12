package com.getyourguide.openapi.validation.api.log;

import java.io.Closeable;
import java.util.Map;
import lombok.NonNull;

public class NoOpLoggerExtension implements LoggerExtension {
    private static final Closeable closable = new NoOpCloseable();

    @Override
    public Closeable addToLoggingContext(@NonNull Map<String, String> newTags) {
        return closable;
    }

    private static final class NoOpCloseable implements Closeable {

        @Override
        public void close() {
            // Do nothing
        }
    }
}
