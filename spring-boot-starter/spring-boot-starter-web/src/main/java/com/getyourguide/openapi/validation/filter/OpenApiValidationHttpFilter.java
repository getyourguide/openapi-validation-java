package com.getyourguide.openapi.validation.filter;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.api.model.ValidationResult;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ContentCachingWrapperFactory;
import com.getyourguide.openapi.validation.factory.ServletMetaDataFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@AllArgsConstructor
public class OpenApiValidationHttpFilter extends HttpFilter {

    private final OpenApiRequestValidator validator;
    private final TrafficSelector trafficSelector;
    private final ServletMetaDataFactory metaDataFactory;
    private final ContentCachingWrapperFactory contentCachingWrapperFactory;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            super.doFilter(request, response, chain);
            return;
        }

        var httpServletRequest = (HttpServletRequest) request;
        var httpServletResponse = (HttpServletResponse) response;
        var requestMetaData = metaDataFactory.buildRequestMetaData(httpServletRequest);
        if (!validator.isReady() || !trafficSelector.shouldRequestBeValidated(requestMetaData)) {
            super.doFilter(request, response, chain);
            return;
        }

        var requestWrapper = contentCachingWrapperFactory.buildContentCachingRequestWrapper(httpServletRequest);
        var responseWrapper = contentCachingWrapperFactory.buildContentCachingResponseWrapper(httpServletResponse);

        var alreadyDidRequestValidation = validateRequestWithFailOnViolation(requestWrapper, requestMetaData);
        try {
            super.doFilter(requestWrapper, responseWrapper, chain);
        } finally {
            var responseMetaData = metaDataFactory.buildResponseMetaData(responseWrapper);
            if (!alreadyDidRequestValidation) {
                validateRequest(requestWrapper, requestMetaData, responseMetaData, RunType.ASYNC);
            }

            var validateResponseResult = validateResponse(
                responseWrapper,
                requestMetaData,
                responseMetaData,
                getRunTypeForResponseValidation(requestMetaData)
            );
            throwStatusExceptionOnViolation(validateResponseResult, "Response validation failed");

            responseWrapper.copyBodyToResponse(); // Needs to be done on every call, otherwise there won't be a response body
        }
    }

    private RunType getRunTypeForResponseValidation(RequestMetaData requestMetaData) {
        if (trafficSelector.shouldFailOnResponseViolation(requestMetaData)) {
            return RunType.SYNC;
        } else {
            return RunType.ASYNC;
        }
    }

    private boolean validateRequestWithFailOnViolation(
        ContentCachingRequestWrapper request,
        RequestMetaData requestMetaData
    ) {
        if (!trafficSelector.shouldFailOnRequestViolation(requestMetaData)) {
            return false;
        }

        var validateRequestResult = validateRequest(request, requestMetaData, null, RunType.SYNC);
        throwStatusExceptionOnViolation(validateRequestResult, "Request validation failed");
        return true;
    }

    private ValidationResult validateRequest(
        ContentCachingRequestWrapper request,
        RequestMetaData requestMetaData,
        @Nullable ResponseMetaData responseMetaData,
        RunType runType
    ) {
        if (!trafficSelector.canRequestBeValidated(requestMetaData)) {
            return ValidationResult.NOT_APPLICABLE;
        }

        var requestBody = request.getContentType() != null
            ? new String(request.getContentAsByteArray(), StandardCharsets.UTF_8)
            : null;

        if (runType == RunType.ASYNC) {
            validator.validateRequestObjectAsync(requestMetaData, responseMetaData, requestBody);
            return ValidationResult.NOT_APPLICABLE;
        } else {
            return validator.validateRequestObject(requestMetaData, requestBody);
        }
    }

    private ValidationResult validateResponse(
        ContentCachingResponseWrapper response,
        RequestMetaData requestMetaData,
        ResponseMetaData responseMetaData,
        RunType runType
    ) {
        if (!trafficSelector.canResponseBeValidated(requestMetaData, responseMetaData)) {
            return ValidationResult.NOT_APPLICABLE;
        }

        var responseBody = response.getContentType() != null
            ? new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)
            : null;

        if (runType == RunType.ASYNC) {
            validator.validateResponseObjectAsync(requestMetaData, responseMetaData, responseBody);
            return ValidationResult.NOT_APPLICABLE;
        } else {
            return validator.validateResponseObject(requestMetaData, responseMetaData, responseBody);
        }
    }

    private void throwStatusExceptionOnViolation(ValidationResult validateRequestResult, String message) {
        if (validateRequestResult == ValidationResult.INVALID) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), message);
        }
    }

    private enum RunType { SYNC, ASYNC }
}
