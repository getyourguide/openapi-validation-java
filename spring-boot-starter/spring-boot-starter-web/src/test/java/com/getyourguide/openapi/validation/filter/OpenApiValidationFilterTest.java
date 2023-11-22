package com.getyourguide.openapi.validation.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class OpenApiValidationFilterTest extends BaseFilterTest {
    private final OpenApiValidationFilter httpFilter =
        new OpenApiValidationFilter(validator, trafficSelector, metaDataFactory, contentCachingWrapperFactory);

    @Test
    public void testWhenNotReadyThenSkipValidation() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().isReady(false).build());

        httpFilter.doFilterInternal(mockData.request(), mockData.response(), mockData.filterChain());

        verifyChainCalled(mockData.filterChain(), mockData.request(), mockData.response());
        verifyValidationDisabledPerAttribute(mockData.request());
    }

    @Test
    public void testWhenShouldRequestBeValidatedFalseThenSkipValidation() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().shouldRequestBeValidated(false).build());

        httpFilter.doFilterInternal(mockData.request(), mockData.response(), mockData.filterChain());

        verifyChainCalled(mockData.filterChain(), mockData.request(), mockData.response());
        verifyValidationDisabledPerAttribute(mockData.request());
    }

    @Test
    public void testWhenValidationThenCorrectlyHandled() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().build());

        httpFilter.doFilterInternal(mockData.request(), mockData.response(), mockData.filterChain());

        verifyChainCalled(mockData.filterChain(), mockData.cachingRequest(), mockData.cachingResponse());
        assertEquals(mockData.requestMetaData(),
            mockData.request().getAttribute(OpenApiValidationFilter.ATTRIBUTE_REQUEST_META_DATA));
    }

    private static void verifyChainCalled(FilterChain chain, ServletRequest request, ServletResponse response)
        throws ServletException, IOException {
        verify(chain).doFilter(request, response);
    }

    private void verifyValidationDisabledPerAttribute(HttpServletRequest request) {
        assertEquals(true, request.getAttribute(OpenApiValidationFilter.ATTRIBUTE_SKIP_VALIDATION));
    }
}
