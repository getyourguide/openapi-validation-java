package com.getyourguide.openapi.validation.core;

import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.exclusions.ViolationExclusions;
import com.getyourguide.openapi.validation.api.log.LogLevel;
import com.getyourguide.openapi.validation.api.log.ViolationLogger;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.core.throttle.ValidationReportThrottler;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ValidationReportHandler {
    private final ValidationReportThrottler throttleHelper;
    private final ViolationLogger logger;
    private final MetricsReporter metrics;
    private final ViolationExclusions violationExclusions;

    public void handleValidationReport(
        RequestMetaData request,
        Direction direction,
        String body,
        ValidationReport result
    ) {
        if (!result.getMessages().isEmpty()) {
            result
                .getMessages()
                .stream()
                .map(message -> buildOpenApiViolation(message, request, body, direction))
                .filter(violation -> !isViolationExcluded(violation))
                .forEach(violation -> throttleHelper.throttle(violation, () -> logValidationError(violation)));
        }
    }

    private void logValidationError(OpenApiViolation openApiViolation) {
        logger.log(openApiViolation);
        metrics.reportViolation(openApiViolation);
    }

    private OpenApiViolation buildOpenApiViolation(
        ValidationReport.Message message,
        RequestMetaData request,
        String body,
        Direction direction
    ) {
        var requestUri = request.getUri().toString();
        var requestString = String.format("%s %s", request.getMethod(), requestUri);
        var pointersInstance = getPointersInstance(message);
        var instance = pointersInstance.map(i -> String.format("Instance: %s\n", i)).orElse("");
        var parameterName = getParameterName(message);
        var parameter = parameterName.map(i -> String.format("Parameter: %s\n", i)).orElse("");

        var logMessage = String.format(
            "OpenAPI spec validation error [%s]\n%s\nUser Agent: %s\n%s%s\n%s",
            message.getKey(),
            requestString,
            request.getHeaders().get("User-Agent"),
            instance,
            parameter,
            message
        );

        return OpenApiViolation.builder()
            .level(mapLogLevel(message.getLevel()))
            .direction(direction)
            .requestMetaData(request)
            .body(body)
            .rule(message.getKey())
            .operationId(getOperationId(message))
            .normalizedPath(getNormalizedPath(message))
            .instance(pointersInstance)
            .parameter(parameterName)
            .schema(getPointersSchema(message))
            .responseStatus(getResponseStatus(message))
            .logMessage(logMessage)
            .message(message.getMessage())
            .build();
    }

    private boolean isViolationExcluded(OpenApiViolation openApiViolation) {
        return
            violationExclusions.isExcluded(openApiViolation)
                // If it matches more than 1, then we don't want to log a validation error
                || openApiViolation.getMessage().matches(
                ".*\\[Path '[^']+'] Instance failed to match exactly one schema \\(matched [1-9][0-9]* out of \\d\\).*");
    }

    private static Optional<String> getPointersInstance(ValidationReport.Message message) {
        return message.getContext()
            .flatMap(ValidationReport.MessageContext::getPointers)
            .map(ValidationReport.MessageContext.Pointers::getInstance);
    }

    private static Optional<String> getPointersSchema(ValidationReport.Message message) {
        return message.getContext()
            .flatMap(ValidationReport.MessageContext::getPointers)
            .map(ValidationReport.MessageContext.Pointers::getSchema);
    }

    private static Optional<String> getParameterName(ValidationReport.Message message) {
        return message.getContext()
            .flatMap(ValidationReport.MessageContext::getParameter)
            .map(Parameter::getName);
    }

    private static Optional<String> getOperationId(ValidationReport.Message message) {
        return message.getContext()
            .flatMap(ValidationReport.MessageContext::getApiOperation)
            .map(apiOperation -> apiOperation.getOperation().getOperationId());
    }

    private static Optional<String> getNormalizedPath(ValidationReport.Message message) {
        return message.getContext()
            .flatMap(ValidationReport.MessageContext::getApiOperation)
            .map(apiOperation -> apiOperation.getApiPath().normalised());
    }

    private static Optional<Integer> getResponseStatus(ValidationReport.Message message) {
        return message.getContext().flatMap(ValidationReport.MessageContext::getResponseStatus);
    }

    private LogLevel mapLogLevel(ValidationReport.Level level) {
        if (level == null) {
            return null;
        }

        return switch (level) {
            case ERROR -> LogLevel.ERROR;
            case WARN -> LogLevel.WARN;
            case INFO -> LogLevel.INFO;
            case IGNORE -> LogLevel.IGNORE;
        };
    }
}
