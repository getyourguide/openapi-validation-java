package com.getyourguide.openapi.validation.filter;

import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ContentCachingWrapperFactory;
import com.getyourguide.openapi.validation.factory.ServletMetaDataFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@AllArgsConstructor
public class OpenApiValidationFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE_SKIP_VALIDATION = "gyg.openapi-validation.skipValidation";
    public static final String ATTRIBUTE_REQUEST_META_DATA = "gyg.openapi-validation.requestMetaData";

    private final OpenApiRequestValidator validator;
    private final TrafficSelector trafficSelector;
    private final ServletMetaDataFactory metaDataFactory;
    private final ContentCachingWrapperFactory contentCachingWrapperFactory;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        var requestMetaData = metaDataFactory.buildRequestMetaData(request);
        request.setAttribute(ATTRIBUTE_REQUEST_META_DATA, requestMetaData);
        if (!validator.isReady() || !trafficSelector.shouldRequestBeValidated(requestMetaData)) {
            request.setAttribute(ATTRIBUTE_SKIP_VALIDATION, true);
            request.setAttribute(ATTRIBUTE_SKIP_VALIDATION, true);
            filterChain.doFilter(request, response);
            return;
        }

        var requestToUse = contentCachingWrapperFactory.buildContentCachingRequestWrapper(request);
        var responseToUse = contentCachingWrapperFactory.buildContentCachingResponseWrapper(response);
        filterChain.doFilter(requestToUse, responseToUse);

        // in case the response was cached it has to be written to the original response
        if (!isAsyncStarted(requestToUse)) {
            var cachingResponse = contentCachingWrapperFactory.getCachingResponse(responseToUse);
            if (cachingResponse != null) {
                cachingResponse.copyBodyToResponse();
            }
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}
