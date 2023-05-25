package com.getyourguide.openapi.validation.api.log;

import java.io.Closeable;
import java.util.Map;
import lombok.NonNull;

public interface LoggerExtension {
    Closeable addToLoggingContext(@NonNull Map<String, String> newTags);
}
