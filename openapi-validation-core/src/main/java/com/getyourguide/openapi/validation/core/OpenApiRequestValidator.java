package com.getyourguide.openapi.validation.core;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.api.model.ValidationResult;
import com.getyourguide.openapi.validation.core.validator.OpenApiInteractionValidatorWrapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URLEncodedUtils;

@Slf4j
public class OpenApiRequestValidator {
    private final ThreadPoolExecutor threadPoolExecutor;
    private final OpenApiInteractionValidatorWrapper validator;
    private final ValidationReportHandler validationReportHandler;
    private final OpenApiRequestValidationConfiguration configuration;

    public OpenApiRequestValidator(
        ThreadPoolExecutor threadPoolExecutor,
        ValidationReportHandler validationReportHandler,
        MetricsReporter metricsReporter,
        OpenApiInteractionValidatorWrapper validator,
        OpenApiRequestValidationConfiguration configuration
    ) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.validator = validator;
        this.validationReportHandler = validationReportHandler;
        this.configuration = configuration;

        metricsReporter.reportStartup(
            validator != null,
            configuration.getSampleRate(),
            configuration.getValidationReportThrottleWaitSeconds()
        );
    }

    public boolean isReady() {
        return validator != null;
    }

    public void validateRequestObjectAsync(final RequestMetaData request, @Nullable ResponseMetaData response, String requestBody) {
        executeAsync(() -> validateRequestObject(request, response, requestBody));
    }

    public void validateResponseObjectAsync(final RequestMetaData request, ResponseMetaData response, final String responseBody) {
        executeAsync(() -> validateResponseObject(request, response, responseBody));
    }

    private void executeAsync(Runnable command) {
        try {
            threadPoolExecutor.execute(command);
        } catch (RejectedExecutionException ignored) {
            // ignored
        }
    }

    public ValidationResult validateRequestObject(final RequestMetaData request, String requestBody) {
        return validateRequestObject(request, null, requestBody);
    }

    public ValidationResult validateRequestObject(
        final RequestMetaData request,
        @Nullable final ResponseMetaData response,
        String requestBody
    ) {
        try {
            var simpleRequest = buildSimpleRequest(request, requestBody);
            var result = validator.validateRequest(simpleRequest);
            // TODO this should not be done here, but currently the only way to do it -> Refactor this so that logging
            //      is actually done in the interceptor/filter where logging can easily be skipped then.
            if (!configuration.isShouldFailOnRequestViolation()) {
                validationReportHandler
                    .handleValidationReport(request, response, Direction.REQUEST, requestBody, result);
            }
            return buildValidationResult(result);
        } catch (Exception e) {
            log.error("Could not validate request", e);
            return ValidationResult.NOT_APPLICABLE;
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

    public ValidationResult validateResponseObject(
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
            validationReportHandler.handleValidationReport(request, response, Direction.RESPONSE, responseBody, result);
            return buildValidationResult(result);
        } catch (Exception e) {
            log.error("Could not validate response", e);
            return ValidationResult.NOT_APPLICABLE;
        }
    }

    private ValidationResult buildValidationResult(ValidationReport validationReport) {
        if (validationReport == null) {
            return ValidationResult.NOT_APPLICABLE;
        }

        if (validationReport.getMessages().isEmpty()) {
            return ValidationResult.VALID;
        }

        return ValidationResult.INVALID;
    }
}
