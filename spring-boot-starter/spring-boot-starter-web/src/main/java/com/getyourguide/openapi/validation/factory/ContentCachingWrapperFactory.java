package com.getyourguide.openapi.validation.factory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class ContentCachingWrapperFactory {
    public ContentCachingRequestWrapper buildContentCachingRequestWrapper(HttpServletRequest request) {
        return new ContentCachingRequestWrapper(request);
    }

    public ContentCachingResponseWrapper buildContentCachingResponseWrapper(HttpServletResponse response) {
        return new ContentCachingResponseWrapper(response);
    }
}
