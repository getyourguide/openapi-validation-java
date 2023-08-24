package com.getyourguide.openapi.validation.core;

import com.getyourguide.openapi.validation.api.log.LoggerExtension;
import com.getyourguide.openapi.validation.api.log.ViolationLogger;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DefaultViolationLogger implements ViolationLogger {

    private final LoggerExtension loggerExtension;

    @Override
    public void log(OpenApiViolation violation) {
        try (var ignored = loggerExtension.addToLoggingContext(buildLoggingContext(violation))) {
            switch (violation.getLevel()) {
                case INFO -> log.info(violation.getLogMessage());
                case WARN -> log.warn(violation.getLogMessage());
                case ERROR -> log.error(violation.getLogMessage());
                default -> { /* do nothing */ }
            }
        } catch (IOException e) {
            log.error("Could not add to LoggingContext", e);
        }
    }

    private Map<String, String> buildLoggingContext(OpenApiViolation violation) {
        var context = new HashMap<String, String>();
        context.put("validation.rule", violation.getRule());
        violation.getNormalizedPath().ifPresent(normalizedPath -> context.put("validation.api.path", normalizedPath));
        violation.getOperationId().ifPresent(operationId -> context.put("validation.api.operation_id", operationId));
        violation.getInstance().ifPresent(instance -> context.put("validation.instance", instance));
        violation.getParameter().ifPresent(instance -> context.put("validation.parameter", instance));
        return context;
    }
}
