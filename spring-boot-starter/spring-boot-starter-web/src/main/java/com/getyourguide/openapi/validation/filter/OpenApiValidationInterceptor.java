package com.getyourguide.openapi.validation.filter;

import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ContentCachingWrapperFactory;
import com.getyourguide.openapi.validation.factory.ServletMetaDataFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@AllArgsConstructor
public class OpenApiValidationInterceptor implements AsyncHandlerInterceptor {
    private static final String ATTRIBUTE_SKIP_REQUEST_VALIDATION = "gyg.openapi-validation.skipRequestValidation";
    private static final String ATTRIBUTE_SKIP_RESPONSE_VALIDATION = "gyg.openapi-validation.skipResponseValidation";

    private final OpenApiRequestValidator validator;
    private final TrafficSelector trafficSelector;
    private final ServletMetaDataFactory metaDataFactory;
    private final ContentCachingWrapperFactory contentCachingWrapperFactory;
    private final OpenApiViolationHandler openApiViolationHandler;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldSkipValidation(request)) {
            return true;
        }

        var requestToUse = contentCachingWrapperFactory.getCachingRequest(request);
        var requestMetaData = getRequestMetaData(request);
        if (requestToUse != null
            && requestMetaData != null
            && trafficSelector.shouldFailOnRequestViolation(requestMetaData)) {
            var violations = validateRequest(requestToUse, requestMetaData, null, RunType.SYNC);
            if (!violations.isEmpty()) {
                request.setAttribute(OpenApiValidationFilter.ATTRIBUTE_SKIP_VALIDATION, true);
                throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Request validation failed");
            }
        }

        return true;
    }

    @Override
    public void postHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        ModelAndView modelAndView
    ) {
        if (shouldSkipValidation(request)) {
            return;
        }

        var requestMetaData = getRequestMetaData(request);
        var responseMetaData = metaDataFactory.buildResponseMetaData(response);
        var requestToUse = contentCachingWrapperFactory.getCachingRequest(request);
        if (requestToUse != null) {
            validateRequest(requestToUse, requestMetaData, responseMetaData, RunType.ASYNC);
        }

        validateResponse(request, response);
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception ex
    ) {
        if (shouldSkipValidation(request)) {
            return;
        }

        validateResponse(request, response, ex);
    }

    private boolean shouldSkipValidation(HttpServletRequest request) {
        return request == null || request.getAttribute(OpenApiValidationFilter.ATTRIBUTE_SKIP_VALIDATION) != null;
    }

    private void validateResponse(HttpServletRequest request, HttpServletResponse response) {
        validateResponse(request, response, null);
    }

    private void validateResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        @Nullable Exception exception
    ) {
        var requestMetaData = getRequestMetaData(request);
        var responseMetaData = metaDataFactory.buildResponseMetaData(response, exception);
        var responseToUse = contentCachingWrapperFactory.getCachingResponse(response);
        if (responseToUse != null) {
            var violations = validateResponse(
                request,
                responseToUse,
                requestMetaData,
                responseMetaData,
                trafficSelector.shouldFailOnResponseViolation(requestMetaData) ? RunType.SYNC : RunType.ASYNC
            );
            violations.forEach(openApiViolationHandler::onOpenApiViolation);
            // Note: violations will always be empty on ASYNC
            if (!violations.isEmpty()) {
                response.reset();
                response.setStatus(500);
                throw new ResponseStatusException(HttpStatusCode.valueOf(500), "Response validation failed");
            }
        }
    }

    @Nullable
    private static RequestMetaData getRequestMetaData(HttpServletRequest request) {
        var metaData = request.getAttribute(OpenApiValidationFilter.ATTRIBUTE_REQUEST_META_DATA);
        return metaData instanceof RequestMetaData ? (RequestMetaData) metaData : null;
    }

    private List<OpenApiViolation> validateRequest(
        MultiReadContentCachingRequestWrapper request,
        RequestMetaData requestMetaData,
        @Nullable ResponseMetaData responseMetaData,
        RunType runType
    ) {
        var skipRequestValidation = request.getAttribute(ATTRIBUTE_SKIP_REQUEST_VALIDATION) != null;
        request.setAttribute(ATTRIBUTE_SKIP_REQUEST_VALIDATION, true);
        if (skipRequestValidation || !trafficSelector.canRequestBeValidated(requestMetaData)) {
            return List.of();
        }

        var requestBody = request.getContentType() != null ? readBodyCatchingException(request) : null;

        if (runType == RunType.ASYNC) {
            validator
                .validateRequestObjectAsync(requestMetaData, responseMetaData, requestBody, openApiViolationHandler);
            return List.of();
        } else {
            return validator.validateRequestObject(requestMetaData, requestBody);
        }
    }

    private static String readBodyCatchingException(MultiReadContentCachingRequestWrapper request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())
            && !"PUT".equalsIgnoreCase(request.getMethod())
            && !"PATCH".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        try {
            return StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private List<OpenApiViolation> validateResponse(
        HttpServletRequest request,
        ContentCachingResponseWrapper response,
        RequestMetaData requestMetaData,
        ResponseMetaData responseMetaData,
        RunType runType
    ) {
        var skipResponseValidation = request.getAttribute(ATTRIBUTE_SKIP_RESPONSE_VALIDATION) != null;
        request.setAttribute(ATTRIBUTE_SKIP_RESPONSE_VALIDATION, true);

        if (skipResponseValidation || !trafficSelector.canResponseBeValidated(requestMetaData, responseMetaData)) {
            return List.of();
        }

        var responseBody = response.getContentType() != null
            ? new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)
            : null;

        if (runType == RunType.ASYNC) {
            validator
                .validateResponseObjectAsync(requestMetaData, responseMetaData, responseBody, openApiViolationHandler);
            return List.of();
        } else {
            return validator.validateResponseObject(requestMetaData, responseMetaData, responseBody);
        }
    }

    private enum RunType { SYNC, ASYNC }
}
