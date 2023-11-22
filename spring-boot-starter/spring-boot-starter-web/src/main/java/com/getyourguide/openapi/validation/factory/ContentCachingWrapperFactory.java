package com.getyourguide.openapi.validation.factory;

import com.getyourguide.openapi.validation.filter.MultiReadHttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.annotation.Nullable;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

public class ContentCachingWrapperFactory {
    public MultiReadHttpServletRequestWrapper buildContentCachingRequestWrapper(HttpServletRequest request) {
        if (request instanceof MultiReadHttpServletRequestWrapper) {
            return (MultiReadHttpServletRequestWrapper) request;
        }

        return new MultiReadHttpServletRequestWrapper(request);
    }

    public ContentCachingResponseWrapper buildContentCachingResponseWrapper(HttpServletResponse response) {
        var cachingResponse = getCachingResponse(response);
        if (cachingResponse != null) {
            return cachingResponse;
        }

        return new ContentCachingResponseWrapper(response);
    }

    @Nullable
    public MultiReadHttpServletRequestWrapper getCachingRequest(HttpServletRequest request) {
        return request instanceof MultiReadHttpServletRequestWrapper ? (MultiReadHttpServletRequestWrapper) request : null;
    }

    @Nullable
    public ContentCachingResponseWrapper getCachingResponse(final HttpServletResponse response) {
        return WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
    }
}
