package com.getyourguide.openapi.validation.core.log;

import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class ThrottlingOpenApiViolationHandler implements OpenApiViolationHandler {
    private final OpenApiViolationHandler delegate;
    private final int waitSeconds;
    private final Map<String, Instant> loggedMessages = new ConcurrentHashMap<>();

    @Override
    public void onOpenApiViolation(OpenApiViolation violation) {
        if (isThrottled(violation)) {
            return;
        }

        delegate.onOpenApiViolation(violation);
        registerLoggedMessage(violation);
    }

    private void registerLoggedMessage(OpenApiViolation openApiViolation) {
        loggedMessages.put(buildKey(openApiViolation), Instant.now());
    }

    private boolean isThrottled(OpenApiViolation openApiViolation) {
        var key = buildKey(openApiViolation);
        var lastLoggedTime = loggedMessages.get(key);
        return lastLoggedTime != null && lastLoggedTime.plusSeconds(waitSeconds).isAfter(Instant.now());
    }

    @NonNull
    private String buildKey(OpenApiViolation openApiViolation) {
        var keyBuilder = new StringBuilder(openApiViolation.getDirection().toString() + ":");
        var method = openApiViolation.getRequestMetaData().getMethod();
        var apiOperationPathNormalized = openApiViolation.getNormalizedPath().orElse("N/A");
        var responseStatus = openApiViolation.getResponseStatus().map(Object::toString).orElse("N/A");
        var schema = openApiViolation.getSchema().orElse("N/A");
        keyBuilder.append(String.format("%s:%s:%s:%s", method, apiOperationPathNormalized, responseStatus, schema));
        return keyBuilder.toString();
    }
}
