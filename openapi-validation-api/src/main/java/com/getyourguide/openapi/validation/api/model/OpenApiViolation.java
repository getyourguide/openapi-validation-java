package com.getyourguide.openapi.validation.api.model;

import com.getyourguide.openapi.validation.api.log.LogLevel;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OpenApiViolation {
    private final LogLevel level;
    private final Direction direction;
    private final RequestMetaData requestMetaData;
    private final String body;
    private final String rule;
    private final Optional<String> operationId;
    private final Optional<String> normalizedPath;
    private final Optional<String> instance;
    private final Optional<String> parameter;
    private final Optional<String> schema;
    private final Optional<Integer> responseStatus;
    private final String message;
    private final String logMessage;
}
