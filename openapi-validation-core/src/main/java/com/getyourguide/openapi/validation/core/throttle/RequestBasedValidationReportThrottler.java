package com.getyourguide.openapi.validation.core.throttle;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.joda.time.DateTime;

@AllArgsConstructor
public class RequestBasedValidationReportThrottler implements ValidationReportThrottler {

    private final int waitSeconds;

    private final Map<String, DateTime> loggedMessages = new ConcurrentHashMap<>();

    @Override
    public void throttle(OpenApiViolation openApiViolation, Runnable runnable) {
        if (isThrottled(openApiViolation)) {
            return;
        }

        runnable.run();
        registerLoggedMessage(openApiViolation);
    }

    private void registerLoggedMessage(OpenApiViolation openApiViolation) {
        loggedMessages.put(buildKey(openApiViolation), DateTime.now());
    }

    private boolean isThrottled(OpenApiViolation openApiViolation) {
        var key = buildKey(openApiViolation);
        var lastLoggedTime = loggedMessages.get(key);
        if (lastLoggedTime == null) {
            return false;
        }
        return lastLoggedTime.plusSeconds(waitSeconds).isAfterNow();
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
