package com.getyourguide.openapi.validation.core;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.getyourguide.openapi.validation.api.log.LogLevel;
import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.core.exclusions.InternalViolationExclusions;
import com.getyourguide.openapi.validation.core.mapper.ValidationReportToOpenApiViolationsMapper;
import com.getyourguide.openapi.validation.core.validator.OpenApiInteractionValidatorWrapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URLEncodedUtils;

@Slf4j
public class OpenApiRequestValidator {
    private final Executor executor;
    private final OpenApiInteractionValidatorWrapper validator;
    private final ValidationReportToOpenApiViolationsMapper mapper;
    private final InternalViolationExclusions violationExclusions;

    public OpenApiRequestValidator(
        Executor executor,
        MetricsReporter metricsReporter,
        OpenApiInteractionValidatorWrapper validator,
        ValidationReportToOpenApiViolationsMapper mapper,
        InternalViolationExclusions violationExclusions,
        OpenApiRequestValidationConfiguration configuration
    ) {
        this.executor = executor;
        this.validator = validator;
        this.mapper = mapper;
        this.violationExclusions = violationExclusions;

        metricsReporter.reportStartup(
            validator != null,
            configuration.getSampleRate(),
            configuration.getValidationReportThrottleWaitSeconds()
        );
    }

    public boolean isReady() {
        return validator != null;
    }

    public void validateRequestObjectAsync(
        final RequestMetaData request,
        @Nullable ResponseMetaData response,
        String requestBody,
        OpenApiViolationHandler listener
    ) {
        executeAsync(() -> {
            var violations = validateRequestObject(request, response, requestBody);
            violations.forEach(listener::onOpenApiViolation);
        });
    }

    public void validateResponseObjectAsync(
        final RequestMetaData request,
        ResponseMetaData response,
        final String responseBody,
        OpenApiViolationHandler listener
    ) {
        executeAsync(() -> {
            var violations = validateResponseObject(request, response, responseBody);
            violations.forEach(listener::onOpenApiViolation);
        });
    }

    private void executeAsync(Runnable command) {
        try {
            executor.execute(command);
        } catch (RejectedExecutionException ignored) {
            // ignored
        }
    }

    public List<OpenApiViolation> validateRequestObject(final RequestMetaData request, String requestBody) {
        return validateRequestObject(request, null, requestBody);
    }

    public List<OpenApiViolation> validateRequestObject(
        final RequestMetaData request,
        @Nullable final ResponseMetaData response,
        String requestBody
    ) {
        try {
            var simpleRequest = buildSimpleRequest(request, requestBody);
            var result = validator.validateRequest(simpleRequest);
            var violations = mapper.map(result, request, response, Direction.REQUEST, requestBody);
            return violations.stream()
                .filter(this::isNonExcludedViolation)
                .toList();
        } catch (Exception e) {
            log.error("[OpenAPI Validation] Could not validate request", e);
            return List.of();
        }
    }

    private static SimpleRequest buildSimpleRequest(RequestMetaData request, String requestBody) {
        var requestBuilder = new SimpleRequest.Builder(request.getMethod(), request.getUri().getPath());
        URLEncodedUtils.parse(request.getUri(), StandardCharsets.UTF_8)
            .forEach(p -> requestBuilder.withQueryParam(p.getName(), nullSafeUrlDecode(p.getValue())));
        if (requestBody != null) {
            requestBuilder.withBody(requestBody);
        }
        request.getHeaders().forEach(requestBuilder::withHeader);
        return requestBuilder.build();
    }

    private static String nullSafeUrlDecode(String value) {
        if (value == null) {
            return null;
        }

        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public List<OpenApiViolation> validateResponseObject(
        final RequestMetaData request,
        final ResponseMetaData response,
        final String responseBody
    ) {
        try {
            var responseBuilder = new SimpleResponse.Builder(response.getStatusCode());
            response.getHeaders().forEach(responseBuilder::withHeader);
            if (responseBody != null) {
                responseBuilder.withContentType(response.getContentType());
                responseBuilder.withBody(responseBody);
            }

            var result = validator.validateResponse(
                request.getUri().getPath(),
                Request.Method.valueOf(request.getMethod().toUpperCase()),
                responseBuilder.build()
            );
            var violations = mapper.map(result, request, response, Direction.RESPONSE, responseBody);
            return violations.stream()
                .filter(this::isNonExcludedViolation)
                .toList();
        } catch (Exception e) {
            log.error("[OpenAPI Validation] Could not validate response", e);
            return List.of();
        }
    }

    private boolean isNonExcludedViolation(OpenApiViolation violation) {
        return !LogLevel.IGNORE.equals(violation.getLevel()) && !violationExclusions.isExcluded(violation);
    }
}
