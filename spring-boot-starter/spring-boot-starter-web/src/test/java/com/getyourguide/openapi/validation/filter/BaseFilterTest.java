package com.getyourguide.openapi.validation.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ContentCachingWrapperFactory;
import com.getyourguide.openapi.validation.factory.ServletMetaDataFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import lombok.Builder;
import org.mockito.Mockito;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class BaseFilterTest {

    protected static final String REQUEST_BODY = "";
    protected static final String RESPONSE_BODY = "";

    protected final OpenApiRequestValidator validator = mock();
    protected final TrafficSelector trafficSelector = mock();
    protected final ServletMetaDataFactory metaDataFactory = mock();
    protected final ContentCachingWrapperFactory contentCachingWrapperFactory = mock();
    protected final OpenApiViolationHandler openApiViolationHandler = mock();

    protected static void mockRequestAttributes(ServletRequest... requests) {
        var requestAttributes = new HashMap<String, Object>();
        for (ServletRequest request : requests) {
            mockRequestAttributes(request, requestAttributes);
        }
    }

    private static void mockRequestAttributes(ServletRequest request, HashMap<String, Object> requestAttributes) {
        when(request.getAttribute(any()))
            .then(invocation -> requestAttributes.get((String) invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> requestAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(request).setAttribute(any(), any());
    }

    protected MockSetupData mockSetup(MockConfiguration configuration) {
        var request = mock(MultiReadContentCachingRequestWrapper.class);
        var response = mock(ContentCachingResponseWrapper.class);
        var cachingRequest = mockContentCachingRequest(request, configuration);
        var cachingResponse = mockContentCachingResponse(response, configuration);
        mockRequestAttributes(request, cachingRequest);

        when(response.getContentType()).thenReturn("application/json");
        when(response.getContentAsByteArray()).thenReturn(configuration.responseBody.getBytes(StandardCharsets.UTF_8));

        var requestMetaData = mock(RequestMetaData.class);
        when(metaDataFactory.buildRequestMetaData(request)).thenReturn(requestMetaData);
        when(request.getAttribute(OpenApiValidationFilter.ATTRIBUTE_REQUEST_META_DATA)).thenReturn(requestMetaData);

        var responseMetaData = mock(ResponseMetaData.class);
        when(metaDataFactory.buildResponseMetaData(response)).thenReturn(responseMetaData);
        when(metaDataFactory.buildResponseMetaData(eq(response), any())).thenReturn(responseMetaData);

        when(metaDataFactory.buildResponseMetaData(cachingResponse)).thenReturn(responseMetaData);

        when(validator.isReady()).thenReturn(configuration.isReady);
        mockTrafficSelectorMethods(requestMetaData, responseMetaData, configuration);

        when(contentCachingWrapperFactory.getCachingRequest(request)).thenReturn(request);
        when(contentCachingWrapperFactory.getCachingResponse(response)).thenReturn(response);


        return MockSetupData.builder()
            .request(request)
            .response(response)
            .cachingRequest(cachingRequest)
            .cachingResponse(cachingResponse)
            .requestMetaData(requestMetaData)
            .responseMetaData(responseMetaData)
            .filterChain(mock(FilterChain.class))
            .build();
    }

    private ContentCachingResponseWrapper mockContentCachingResponse(
        HttpServletResponse response,
        MockConfiguration configuration
    ) {
        var cachingResponse = mock(ContentCachingResponseWrapper.class);
        when(contentCachingWrapperFactory.buildContentCachingResponseWrapper(response)).thenReturn(cachingResponse);
        if (configuration.responseBody != null) {
            when(cachingResponse.getContentType()).thenReturn("application/json");
            when(cachingResponse.getContentAsByteArray())
                .thenReturn(configuration.responseBody.getBytes(StandardCharsets.UTF_8));
        }
        return cachingResponse;
    }

    private MultiReadContentCachingRequestWrapper mockContentCachingRequest(
        HttpServletRequest request,
        MockConfiguration configuration
    ) {
        var cachingRequest = mock(MultiReadContentCachingRequestWrapper.class);
        when(contentCachingWrapperFactory.buildContentCachingRequestWrapper(request)).thenReturn(cachingRequest);
        if (configuration.requestBody != null) {
            try {
                var sourceStream = new ByteArrayInputStream(configuration.requestBody.getBytes(StandardCharsets.UTF_8));
                when(request.getContentType()).thenReturn("application/json");
                when(request.getInputStream()).thenReturn(new DelegatingServletInputStream(sourceStream));

                sourceStream = new ByteArrayInputStream(configuration.requestBody.getBytes(StandardCharsets.UTF_8));
                when(cachingRequest.getContentType()).thenReturn("application/json");
                when(cachingRequest.getInputStream()).thenReturn(new DelegatingServletInputStream(sourceStream));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return cachingRequest;
    }

    private void mockTrafficSelectorMethods(
        RequestMetaData requestMetaData,
        ResponseMetaData responseMetaData,
        MockConfiguration configuration
    ) {
        when(trafficSelector.shouldRequestBeValidated(any())).thenReturn(configuration.shouldRequestBeValidated);
        when(trafficSelector.canRequestBeValidated(requestMetaData)).thenReturn(configuration.canRequestBeValidated);
        when(trafficSelector.canResponseBeValidated(requestMetaData, responseMetaData))
            .thenReturn(configuration.canResponseBeValidated);
        when(trafficSelector.shouldFailOnRequestViolation(requestMetaData)).thenReturn(
            configuration.shouldFailOnRequestViolation);
        when(trafficSelector.shouldFailOnResponseViolation(requestMetaData)).thenReturn(
            configuration.shouldFailOnResponseViolation);
    }

    @Builder
    protected static class MockConfiguration {
        @Builder.Default
        private boolean isReady = true;
        @Builder.Default
        private boolean shouldRequestBeValidated = true;

        @Builder.Default
        private boolean canRequestBeValidated = true;
        @Builder.Default
        private boolean canResponseBeValidated = true;

        @Builder.Default
        private boolean shouldFailOnRequestViolation = false;
        @Builder.Default
        private boolean shouldFailOnResponseViolation = false;

        @Builder.Default
        private String requestBody = REQUEST_BODY;
        @Builder.Default
        private String responseBody = RESPONSE_BODY;
    }

    @Builder
    protected record MockSetupData(
        HttpServletRequest request,
        HttpServletResponse response,
        ServletRequest cachingRequest,
        ServletResponse cachingResponse,
        RequestMetaData requestMetaData,
        ResponseMetaData responseMetaData,
        FilterChain filterChain
    ) {
    }
}
