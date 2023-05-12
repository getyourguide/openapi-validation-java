package com.getyourguide.openapi.validation.filter;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
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

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
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

        var requestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
        var responseWrapper = new ContentCachingResponseWrapper(httpServletResponse);
        try {
            super.doFilter(requestWrapper, responseWrapper, chain);
        } finally {
            validateRequest(requestWrapper, requestMetaData);
            validateResponse(responseWrapper, requestMetaData);
            responseWrapper.copyBodyToResponse(); // Needs to be done on every call, otherwise there won't be a response body
        }
    }

    private void validateRequest(ContentCachingRequestWrapper request, RequestMetaData requestMetaData) {
        if (!trafficSelector.canRequestBeValidated(requestMetaData)) {
            return;
        }

        var requestBody = request.getContentType() != null
            ? new String(request.getContentAsByteArray(), StandardCharsets.UTF_8)
            : null;
        validator.validateRequestObjectAsync(requestMetaData, requestBody);
    }

    private void validateResponse(ContentCachingResponseWrapper response, RequestMetaData requestMetaData) {
        var responseMetaData = metaDataFactory.buildResponseMetaData(response);
        if (!trafficSelector.canResponseBeValidated(requestMetaData, responseMetaData)) {
            return;
        }

        var responseBody = response.getContentType() != null
            ? new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)
            : null;
        validator.validateResponseObjectAsync(requestMetaData, responseMetaData, responseBody);
    }
}
