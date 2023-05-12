package com.getyourguide.openapi.validation.core.throttle;

import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.model.Direction;
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
    public void throttle(ValidationReport.Message message, Direction direction, Runnable runnable) {
        if (isThrottled(message, direction)) {
            return;
        }

        runnable.run();
        registerLoggedMessage(message, direction);
    }

    private void registerLoggedMessage(ValidationReport.Message message, Direction direction) {
        loggedMessages.put(buildKey(message, direction), DateTime.now());
    }

    private boolean isThrottled(ValidationReport.Message message, Direction direction) {
        var key = buildKey(message, direction);
        var lastLoggedTime = loggedMessages.get(key);
        if (lastLoggedTime == null) {
            return false;
        }
        return lastLoggedTime.plusSeconds(waitSeconds).isAfterNow();
    }

    @NonNull
    private String buildKey(ValidationReport.Message message, Direction direction) {
        var keyBuilder = new StringBuilder(direction.toString() + ":");
        message.getContext().ifPresentOrElse(
            messageContext -> {
                var method = messageContext.getRequestMethod().map(Enum::name).orElse("N/A");
                var apiOperationPathNormalized = messageContext.getApiOperation().map(apiOperation -> apiOperation.getApiPath().normalised()).orElse("N/A");
                var responseStatus = messageContext.getResponseStatus().map(Object::toString).orElse("N/A");
                var schema = messageContext.getPointers().map(ValidationReport.MessageContext.Pointers::getSchema).orElse("N/A");
                keyBuilder.append(String.format("%s:%s:%s:%s", method, apiOperationPathNormalized, responseStatus, schema));
            },
            () -> keyBuilder.append("N/A")
        );
        return keyBuilder.toString();
    }
}
