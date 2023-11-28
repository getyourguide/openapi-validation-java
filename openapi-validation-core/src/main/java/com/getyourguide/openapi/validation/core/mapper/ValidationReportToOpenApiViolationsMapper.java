package com.getyourguide.openapi.validation.core.mapper;

import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.log.LogLevel;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

public class ValidationReportToOpenApiViolationsMapper {
    public List<OpenApiViolation> map(
        ValidationReport validationReport,
        RequestMetaData request,
        @Nullable ResponseMetaData response,
        Direction direction,
        String body
    ) {
        return validationReport
            .getMessages()
            .stream()
            .map(message -> buildOpenApiViolation(message, request, response, body, direction))
            .toList();
    }

    private OpenApiViolation buildOpenApiViolation(
        ValidationReport.Message message,
        RequestMetaData request,
        @Nullable ResponseMetaData response,
        String body,
        Direction direction
    ) {
        var requestUri = request.getUri().toString();
        var requestString = String.format("%s %s", request.getMethod(), requestUri);
        var pointersInstance = getPointersInstance(message);
        var instance = pointersInstance.map(i -> String.format("Instance: %s\n", i)).orElse("");
        var parameterName = getParameterName(message);
        var parameter = parameterName.map(i -> String.format("Parameter: %s\n", i)).orElse("");
        var responseStatusCode = response != null
            ? String.format("Response Status Code: %s\n", response.getStatusCode())
            : "";

        var logMessage = String.format(
            "OpenAPI spec validation error [%s]\n%s\nUser Agent: %s\n%s%s%s\n%s",
            message.getKey(),
            requestString,
            request.getHeaders().get("User-Agent"),
            responseStatusCode,
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
            .operationId(getOperationId(message).orElse(null))
            .normalizedPath(getNormalizedPath(message).orElse(null))
            .instance(pointersInstance.orElse(null))
            .parameter(parameterName.orElse(null))
            .schema(getPointersSchema(message).orElse(null))
            .responseStatus(getResponseStatus(response, message).orElse(null))
            .logMessage(logMessage)
            .message(message.getMessage())
            .build();
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

    private static Optional<Integer> getResponseStatus(
        @Nullable ResponseMetaData response,
        ValidationReport.Message message
    ) {
        if (response != null && response.getStatusCode() != null) {
            return Optional.of(response.getStatusCode());
        }

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
