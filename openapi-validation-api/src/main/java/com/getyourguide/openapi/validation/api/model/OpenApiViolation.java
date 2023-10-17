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
    private final String operationId;
    private final String normalizedPath;
    private final String instance;
    private final String parameter;
    private final String schema;
    private final Integer responseStatus;
    private final String message;
    private final String logMessage;

    public Optional<String> getOperationId() {
        return Optional.ofNullable(operationId);
    }

    public Optional<String> getNormalizedPath() {
        return Optional.ofNullable(normalizedPath);
    }

    public Optional<String> getInstance() {
        return Optional.ofNullable(instance);
    }

    public Optional<String> getParameter() {
        return Optional.ofNullable(parameter);
    }

    public Optional<String> getSchema() {
        return Optional.ofNullable(schema);
    }

    public Optional<Integer> getResponseStatus() {
        return Optional.ofNullable(responseStatus);
    }
}
