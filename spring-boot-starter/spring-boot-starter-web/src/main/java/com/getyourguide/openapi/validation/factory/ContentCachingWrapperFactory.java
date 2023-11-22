package com.getyourguide.openapi.validation.factory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.annotation.Nullable;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

public class ContentCachingWrapperFactory {
    public ContentCachingRequestWrapper buildContentCachingRequestWrapper(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        }

        return new ContentCachingRequestWrapper(request);
    }

    public ContentCachingResponseWrapper buildContentCachingResponseWrapper(HttpServletResponse response) {
        var cachingResponse = getCachingResponse(response);
        if (cachingResponse != null) {
            return cachingResponse;
        }

        return new ContentCachingResponseWrapper(response);
    }

    @Nullable
    public ContentCachingResponseWrapper getCachingResponse(final HttpServletResponse response) {
        return WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
    }

    @Nullable
    public ContentCachingRequestWrapper getCachingRequest(HttpServletRequest request) {
        return request instanceof ContentCachingRequestWrapper ? (ContentCachingRequestWrapper) request : null;
    }
}
