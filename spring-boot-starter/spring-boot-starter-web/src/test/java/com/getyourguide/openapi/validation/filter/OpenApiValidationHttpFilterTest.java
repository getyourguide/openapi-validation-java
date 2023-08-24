package com.getyourguide.openapi.validation.filter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.Builder;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

class OpenApiValidationHttpFilterTest {

    public static final String REQUEST_BODY = "";
    public static final String RESPONSE_BODY = "";

    private final OpenApiRequestValidator validator = mock();
    private final TrafficSelector trafficSelector = mock();
    private final ServletMetaDataFactory metaDataFactory = mock();
    private final ContentCachingWrapperFactory contentCachingWrapperFactory = mock();

    private final OpenApiValidationHttpFilter httpFilter =
        new OpenApiValidationHttpFilter(validator, trafficSelector, metaDataFactory, contentCachingWrapperFactory);

    @Test
    public void testNormalFlowWithValidation() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().build());

        httpFilter.doFilter(mockData.request, mockData.response, mockData.chain);

        verifyChainCalled(mockData.chain, mockData.cachingRequest, mockData.cachingResponse);
        verifyRequestValidatedAsync(mockData);
        verifyResponseValidatedAsync(mockData);
    }

    @Test
    public void testNoValidationIfNotReady() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().isReady(false).build());

        httpFilter.doFilter(mockData.request, mockData.response, mockData.chain);

        verifyChainCalled(mockData.chain, mockData.request, mockData.response);
        verifyNoValidation();
    }

    @Test
    public void testNoValidationIfNotShouldRequestBeValidated() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().shouldRequestBeValidated(false).build());

        httpFilter.doFilter(mockData.request, mockData.response, mockData.chain);

        verifyChainCalled(mockData.chain, mockData.request, mockData.response);
        verifyNoValidation();
    }

    @Test
    public void testNoValidationIfNotCanRequestBeValidated() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().canRequestBeValidated(false).build());

        httpFilter.doFilter(mockData.request, mockData.response, mockData.chain);

        verifyChainCalled(mockData.chain, mockData.cachingRequest, mockData.cachingResponse);
        verifyNoRequestValidation();
        verifyResponseValidatedAsync(mockData);
    }

    @Test
    public void testNoValidationIfNotCanResponseBeValidated() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().canResponseBeValidated(false).build());

        httpFilter.doFilter(mockData.request, mockData.response, mockData.chain);

        verifyChainCalled(mockData.chain, mockData.cachingRequest, mockData.cachingResponse);
        verifyRequestValidatedAsync(mockData);
        verifyNoResponseValidation();
    }

    @Test
    public void testShouldFailOnRequestViolationWithoutViolation() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnRequestViolation(true).build());

        httpFilter.doFilter(mockData.request, mockData.response, mockData.chain);

        verifyChainCalled(mockData.chain, mockData.cachingRequest, mockData.cachingResponse);
        verifyRequestValidatedSync(mockData);
        verifyResponseValidatedAsync(mockData);
    }

    @Test
    public void testShouldFailOnReResponseViolationWithoutViolation() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnResponseViolation(true).build());

        httpFilter.doFilter(mockData.request, mockData.response, mockData.chain);

        verifyChainCalled(mockData.chain, mockData.cachingRequest, mockData.cachingResponse);
        verifyRequestValidatedAsync(mockData);
        verifyResponseValidatedSync(mockData);
    }

    @Test
    public void testShouldFailOnRequestViolationWithViolation() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnRequestViolation(true).build());
        when(validator.validateRequestObject(eq(mockData.requestMetaData), eq(REQUEST_BODY)))
            .thenReturn(ValidationResult.INVALID);

        assertThrows(ResponseStatusException.class,
            () -> httpFilter.doFilter(mockData.request, mockData.response, mockData.chain));

        verifyChainNotCalled(mockData.chain);
        verifyRequestValidatedSync(mockData);
        verifyNoResponseValidation();
    }

    @Test
    public void testShouldFailOnResponseViolationWithViolation() throws ServletException, IOException {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnResponseViolation(true).build());
        when(
            validator.validateResponseObject(
                eq(mockData.requestMetaData),
                eq(mockData.responseMetaData), eq(REQUEST_BODY)
            )
        ).thenReturn(ValidationResult.INVALID);

        assertThrows(ResponseStatusException.class,
            () -> httpFilter.doFilter(mockData.request, mockData.response, mockData.chain));

        verifyChainCalled(mockData.chain, mockData.cachingRequest, mockData.cachingResponse);
        verifyRequestValidatedAsync(mockData);
        verifyResponseValidatedSync(mockData);
    }

    private void verifyNoValidation() {
        verifyNoRequestValidation();
        verifyNoResponseValidation();
    }

    private void verifyNoRequestValidation() {
        verify(validator, never()).validateRequestObjectAsync(any(), any(), anyString());
        verify(validator, never()).validateRequestObject(any(), anyString());
    }

    private void verifyNoResponseValidation() {
        verify(validator, never()).validateResponseObjectAsync(any(), any(), anyString());
        verify(validator, never()).validateResponseObject(any(), any(), anyString());
    }

    private void verifyRequestValidatedAsync(MockSetupData mockData) {
        verify(validator).validateRequestObjectAsync(eq(mockData.requestMetaData), eq(mockData.responseMetaData), eq(REQUEST_BODY));
    }

    private void verifyRequestValidatedSync(MockSetupData mockData) {
        verify(validator).validateRequestObject(eq(mockData.requestMetaData), eq(REQUEST_BODY));
    }

    private void verifyResponseValidatedAsync(MockSetupData mockData) {
        verify(validator).validateResponseObjectAsync(
            eq(mockData.requestMetaData),
            eq(mockData.responseMetaData),
            eq(RESPONSE_BODY)
        );
    }

    private void verifyResponseValidatedSync(MockSetupData mockData) {
        verify(validator)
            .validateResponseObject(eq(mockData.requestMetaData), eq(mockData.responseMetaData), eq(RESPONSE_BODY));
    }

    private void mockTrafficSelectorMethods(
        RequestMetaData requestMetaData,
        ResponseMetaData responseMetaData,
        MockConfiguration configuration
    ) {
        when(trafficSelector.shouldRequestBeValidated(any())).thenReturn(configuration.shouldRequestBeValidated);
        when(trafficSelector.canRequestBeValidated(requestMetaData)).thenReturn(configuration.canRequestBeValidated);
        when(trafficSelector.canResponseBeValidated(requestMetaData, responseMetaData)).thenReturn(
            configuration.canResponseBeValidated);
        when(trafficSelector.shouldFailOnRequestViolation(requestMetaData)).thenReturn(
            configuration.shouldFailOnRequestViolation);
        when(trafficSelector.shouldFailOnResponseViolation(requestMetaData)).thenReturn(
            configuration.shouldFailOnResponseViolation);
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

    private ContentCachingRequestWrapper mockContentCachingRequest(
        HttpServletRequest request,
        MockConfiguration configuration
    ) {
        var cachingRequest = mock(ContentCachingRequestWrapper.class);
        when(contentCachingWrapperFactory.buildContentCachingRequestWrapper(request)).thenReturn(cachingRequest);
        if (configuration.responseBody != null) {
            when(cachingRequest.getContentType()).thenReturn("application/json");
            when(cachingRequest.getContentAsByteArray())
                .thenReturn(configuration.requestBody.getBytes(StandardCharsets.UTF_8));
        }
        return cachingRequest;
    }

    private static void verifyChainCalled(FilterChain chain, ServletRequest request, ServletResponse response)
        throws ServletException, IOException {
        verify(chain).doFilter(request, response);
    }

    private static void verifyChainNotCalled(FilterChain chain) throws ServletException, IOException {
        verify(chain, never()).doFilter(any(), any());
    }

    private MockSetupData mockSetup(MockConfiguration configuration) {
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);

        var requestMetaData = mock(RequestMetaData.class);
        when(metaDataFactory.buildRequestMetaData(request)).thenReturn(requestMetaData);

        var responseMetaData = mock(ResponseMetaData.class);
        when(metaDataFactory.buildResponseMetaData(response)).thenReturn(responseMetaData);

        var cachingRequest = mockContentCachingRequest(request, configuration);
        var cachingResponse = mockContentCachingResponse(response, configuration);
        when(metaDataFactory.buildResponseMetaData(cachingResponse)).thenReturn(responseMetaData);

        var chain = mock(FilterChain.class);
        when(validator.isReady()).thenReturn(configuration.isReady);
        mockTrafficSelectorMethods(requestMetaData, responseMetaData, configuration);

        return MockSetupData.builder()
            .request(request)
            .response(response)
            .cachingRequest(cachingRequest)
            .cachingResponse(cachingResponse)
            .chain(chain)
            .requestMetaData(requestMetaData)
            .responseMetaData(responseMetaData)
            .build();
    }

    @Builder
    private static class MockConfiguration {
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
    private record MockSetupData(
        ServletRequest request,
        ServletResponse response,
        ServletRequest cachingRequest,
        ServletResponse cachingResponse,
        FilterChain chain,
        RequestMetaData requestMetaData,
        ResponseMetaData responseMetaData
    ) {
    }
}
