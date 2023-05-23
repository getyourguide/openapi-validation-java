package com.getyourguide.openapi.validation.core;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.api.model.ValidationResult;
import com.getyourguide.openapi.validation.api.model.ValidatorConfiguration;
import com.getyourguide.openapi.validation.core.validator.OpenApiInteractionValidatorWrapper;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URLEncodedUtils;

@Slf4j
public class OpenApiRequestValidator {
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 2, 1000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10));

    private final OpenApiInteractionValidatorWrapper validator;
    private final ValidationReportHandler validationReportHandler;

    public OpenApiRequestValidator(ValidationReportHandler validationReportHandler, String specificationFilePath, ValidatorConfiguration configuration) {
        this.validator = new OpenApiInteractionValidatorFactory().build(specificationFilePath, configuration);
        this.validationReportHandler = validationReportHandler;
    }

    public boolean isReady() {
        return validator != null;
    }

    public void validateRequestObjectAsync(final RequestMetaData request, String requestBody) {
        threadPool.execute(() -> validateRequestObject(request, requestBody));
    }

    public void validateResponseObjectAsync(final RequestMetaData request, ResponseMetaData response, final String responseBody) {
        threadPool.execute(() -> validateResponseObject(request, response, responseBody));
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
