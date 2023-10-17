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

    public Optional<String> getOperationId() {
        return getOptional(operationId);
    }

    public Optional<String> getNormalizedPath() {
        return getOptional(normalizedPath);
    }

    public Optional<String> getInstance() {
        return getOptional(instance);
    }

    public Optional<String> getParameter() {
        return getOptional(parameter);
    }

    public Optional<String> getSchema() {
        return getOptional(schema);
    }

    public Optional<Integer> getResponseStatus() {
        return getOptional(responseStatus);
    }

    private <T> Optional<T> getOptional(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<T> field) {
        //noinspection OptionalAssignedToNull
        return field != null ? field : Optional.empty();
    }

}
