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
import com.getyourguide.openapi.validation.api.model.ValidatorConfiguration;
import com.getyourguide.openapi.validation.core.validator.OpenApiInteractionValidatorWrapper;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URLEncodedUtils;

@Slf4j
public class OpenApiRequestValidator {
    private final ThreadPoolExecutor threadPool;
    private final OpenApiInteractionValidatorWrapper validator;
    private final ValidationReportHandler validationReportHandler;

    public OpenApiRequestValidator(
        ThreadPoolExecutor threadPool,
        ValidationReportHandler validationReportHandler,
        MetricsReporter metricsReporter,
        String specificationFilePath,
        ValidatorConfiguration configuration
    ) {
        this.threadPool = threadPool;
        this.validator = new OpenApiInteractionValidatorFactory().build(specificationFilePath, configuration);
        this.validationReportHandler = validationReportHandler;

        metricsReporter.reportStartup(validator != null);
    }

    public boolean isReady() {
        return validator != null;
    }

    public void validateRequestObjectAsync(final RequestMetaData request, String requestBody) {
        executeAsync(() -> validateRequestObject(request, requestBody));
    }

    public void validateResponseObjectAsync(final RequestMetaData request, ResponseMetaData response, final String responseBody) {
        executeAsync(() -> validateResponseObject(request, response, responseBody));
    }

    private void executeAsync(Runnable command) {
        try {
            threadPool.execute(command);
        } catch(RejectedExecutionException ignored) {
        }
    }

    public ValidationResult validateRequestObject(final RequestMetaData request, String requestBody) {
        try {
            var simpleRequest = buildSimpleRequest(request, requestBody);
            var result = validator.validateRequest(simpleRequest);
            validationReportHandler.handleValidationReport(request, Direction.REQUEST, requestBody, result);
            return buildValidationResult(result);
        } catch (Exception e) {
            log.error("Could not validate request", e);
            return ValidationResult.NOT_APPLICABLE;
        }
    }

    private static SimpleRequest buildSimpleRequest(RequestMetaData request, String requestBody) {
        var requestBuilder = new SimpleRequest.Builder(request.getMethod(), request.getUri().getPath());
        URLEncodedUtils.parse(request.getUri(), StandardCharsets.UTF_8)
            .forEach(p -> requestBuilder.withQueryParam(p.getName(), p.getValue()));
        if (requestBody != null) {
            requestBuilder.withBody(requestBody);
        }
        request.getHeaders().forEach(requestBuilder::withHeader);
        return requestBuilder.build();
    }

    public ValidationResult validateResponseObject(
        final RequestMetaData request,
        ResponseMetaData response,
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
            validationReportHandler.handleValidationReport(request, Direction.RESPONSE, responseBody, result);
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
