package com.getyourguide.openapi.validation.factory;

import com.getyourguide.openapi.validation.filter.MultiReadContentCachingRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.annotation.Nullable;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

public class ContentCachingWrapperFactory {
    public MultiReadContentCachingRequestWrapper buildContentCachingRequestWrapper(HttpServletRequest request) {
        if (request instanceof MultiReadContentCachingRequestWrapper) {
            return (MultiReadContentCachingRequestWrapper) request;
        }

        return new MultiReadContentCachingRequestWrapper(request);
    }

    public ContentCachingResponseWrapper buildContentCachingResponseWrapper(HttpServletResponse response) {
        var cachingResponse = getCachingResponse(response);
        if (cachingResponse != null) {
            return cachingResponse;
        }

        return new ContentCachingResponseWrapper(response);
    }

    @Nullable
    public MultiReadContentCachingRequestWrapper getCachingRequest(HttpServletRequest request) {
        return request instanceof MultiReadContentCachingRequestWrapper ? (MultiReadContentCachingRequestWrapper) request : null;
    }

    @Nullable
    public ContentCachingResponseWrapper getCachingResponse(final HttpServletResponse response) {
        return WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
    }
}
