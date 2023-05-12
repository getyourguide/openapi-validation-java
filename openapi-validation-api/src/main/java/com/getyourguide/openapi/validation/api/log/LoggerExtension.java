package com.getyourguide.openapi.validation.api.log;

import java.io.Closeable;
import java.util.Map;
import lombok.NonNull;

// TODO CHK-8357 can we get rid of this one?
public interface LoggerExtension {
    Closeable addToLoggingContext(@NonNull Map<String, String> newTags);
}
