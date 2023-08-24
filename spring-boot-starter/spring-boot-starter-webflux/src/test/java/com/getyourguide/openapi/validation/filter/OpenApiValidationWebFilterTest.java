package com.getyourguide.openapi.validation.filter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.api.model.ValidationResult;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ReactiveMetaDataFactory;
import com.getyourguide.openapi.validation.filter.decorator.BodyCachingServerHttpRequestDecorator;
import com.getyourguide.openapi.validation.filter.decorator.BodyCachingServerHttpResponseDecorator;
import com.getyourguide.openapi.validation.filter.decorator.DecoratorBuilder;
import java.util.List;
import lombok.Builder;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class OpenApiValidationWebFilterTest {

    public static final String REQUEST_BODY = "";
    public static final String RESPONSE_BODY = "";

    private final OpenApiRequestValidator validator = mock();
    private final TrafficSelector trafficSelector = mock();
    private final ReactiveMetaDataFactory metaDataFactory = mock();
    private final DecoratorBuilder decoratorBuilder = mock();

    private final OpenApiValidationWebFilter webFilter =
        new OpenApiValidationWebFilter(validator, trafficSelector, metaDataFactory, decoratorBuilder);

    @Test
    public void testNormalFlowWithValidation() {
        var mockData = mockSetup(MockConfiguration.builder().build());

        var mono = webFilter.filter(mockData.exchange, mockData.chain);

        StepVerifier.create(mono).expectComplete().verify();
        verifyChainCalled(mockData.chain, mockData.mutatedExchange);
        verifyRequestValidatedAsync(mockData);
        verifyResponseValidatedAsync(mockData);
    }

    @Test
    public void testNoValidationIfNotReady() {
        var mockData = mockSetup(MockConfiguration.builder().isReady(false).build());

        var mono = webFilter.filter(mockData.exchange, mockData.chain);

        StepVerifier.create(mono).expectComplete().verify();
        verifyChainCalled(mockData.chain, mockData.exchange);
        verifyNoValidation();
    }

    @Test
    public void testNoValidationIfNotShouldRequestBeValidated() {
        var mockData = mockSetup(MockConfiguration.builder().shouldRequestBeValidated(false).build());

        var mono = webFilter.filter(mockData.exchange, mockData.chain);

        StepVerifier.create(mono).expectComplete().verify();
        verifyChainCalled(mockData.chain, mockData.exchange);
        verifyNoValidation();
    }

    @Test
    public void testNoValidationIfNotCanRequestBeValidated() {
        var mockData = mockSetup(MockConfiguration.builder().canRequestBeValidated(false).build());

        var mono = webFilter.filter(mockData.exchange, mockData.chain);

        StepVerifier.create(mono).expectComplete().verify();
        verifyChainCalled(mockData.chain, mockData.mutatedExchange);
        verifyNoRequestValidation();
        verifyResponseValidatedAsync(mockData);
    }

    @Test
    public void testNoValidationIfNotCanResponseBeValidated() {
        var mockData = mockSetup(MockConfiguration.builder().canResponseBeValidated(false).build());

        var mono = webFilter.filter(mockData.exchange, mockData.chain);

        StepVerifier.create(mono).expectComplete().verify();
        verifyChainCalled(mockData.chain, mockData.mutatedExchange);
        verifyRequestValidatedAsync(mockData);
        verifyNoResponseValidation();
    }

    @Test
    public void testShouldFailOnRequestViolationWithoutViolation() {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnRequestViolation(true).build());

        var mono = webFilter.filter(mockData.exchange, mockData.chain);

        StepVerifier.create(mono).expectComplete().verify();
        verifyChainCalled(mockData.chain, mockData.mutatedExchange);
        verifyRequestValidatedSync(mockData);
        verifyResponseValidatedAsync(mockData);
    }

    @Test
    public void testShouldFailOnReResponseViolationWithoutViolation() {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnResponseViolation(true).build());

        var mono = webFilter.filter(mockData.exchange, mockData.chain);

        StepVerifier.create(mono).expectComplete().verify();
        verifyChainCalled(mockData.chain, mockData.mutatedExchange);
        verifyRequestValidatedAsync(mockData);
        verifyResponseValidatedSync(mockData);
    }

    @Test
    public void testShouldFailOnRequestViolationWithViolation() {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnRequestViolation(true).build());
        when(validator.validateRequestObject(eq(mockData.requestMetaData), any(), eq(REQUEST_BODY)))
            .thenReturn(ValidationResult.INVALID);

        assertThrows(ResponseStatusException.class, () -> webFilter.filter(mockData.exchange, mockData.chain));

        verifyChainNotCalled(mockData.chain);
        verifyRequestValidatedSync(mockData);
        verifyNoResponseValidation();
    }

    @Test
    public void testShouldFailOnResponseViolationWithViolation() {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnResponseViolation(true).build());
        when(
            validator.validateResponseObject(
                eq(mockData.requestMetaData),
                eq(mockData.responseMetaData), eq(REQUEST_BODY)
            )
        ).thenReturn(ValidationResult.INVALID);

        assertThrows(ResponseStatusException.class, () -> webFilter.filter(mockData.exchange, mockData.chain));

        verifyChainNotCalled(mockData.chain);
        verifyNoRequestValidation();
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
        verify(validator).validateRequestObjectAsync(eq(mockData.requestMetaData), any(), eq(REQUEST_BODY));
    }

    private void verifyRequestValidatedSync(MockSetupData mockData) {
        verify(validator).validateRequestObject(eq(mockData.requestMetaData), any(), eq(REQUEST_BODY));
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

    private static WebFilterChain mockChain() {
        var chain = mock(WebFilterChain.class);
        Mono<Void> filterMono = Mono.just("").then();
        when(chain.filter(any())).thenReturn(filterMono);
        return chain;
    }

    private void mockDecoratedRequests(
        ServerHttpRequest request,
        ServerHttpResponse response,
        RequestMetaData requestMetaData,
        ResponseMetaData responseMetaData,
        MockConfiguration configuration
    ) {
        var decoratedRequest = mock(BodyCachingServerHttpRequestDecorator.class);
        when(decoratorBuilder.buildBodyCachingServerHttpRequestDecorator(request, requestMetaData))
            .thenReturn(decoratedRequest);
        when(decoratedRequest.getHeaders()).thenReturn(buildHeadersForBody(configuration.requestBody));
        when(decoratedRequest.getCachedBody()).thenReturn(configuration.requestBody);
        if (configuration.requestBody != null) {
            doAnswer(invocation -> {
                invocation.getArgument(0, Runnable.class).run();
                return null;
            }).when(decoratedRequest).setOnBodyCachedListener(any());
        }

        var decoratedResponse = mock(BodyCachingServerHttpResponseDecorator.class);
        when(decoratorBuilder.buildBodyCachingServerHttpResponseDecorator(response, requestMetaData))
            .thenReturn(decoratedResponse);
        when(decoratedResponse.getHeaders()).thenReturn(buildHeadersForBody(configuration.responseBody));
        when(decoratedResponse.getCachedBody()).thenReturn(configuration.responseBody);
        if (configuration.responseBody != null) {
            doAnswer(invocation -> {
                invocation.getArgument(0, Runnable.class).run();
                return null;
            }).when(decoratedResponse).setOnBodyCachedListener(any());
        }

        when(metaDataFactory.buildResponseMetaData(decoratedResponse)).thenReturn(responseMetaData);
    }

    private static HttpHeaders buildHeadersForBody(String body) {
        var headers = new HttpHeaders();
        if (body != null) {
            headers.put(HttpHeaders.CONTENT_TYPE, List.of("application/json"));
            headers.put(HttpHeaders.CONTENT_LENGTH, List.of(String.valueOf(body.length())));
        }
        return headers;
    }

    private static ServerWebExchange mockExchangeMutation(ServerWebExchange exchange) {
        var mutatedExchange = mock(ServerWebExchange.class);
        var exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.response(any(ServerHttpResponse.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);
        return mutatedExchange;
    }

    private static void verifyChainCalled(WebFilterChain chain, ServerWebExchange mutatedExchange) {
        verify(chain).filter(mutatedExchange);
    }

    private static void verifyChainNotCalled(WebFilterChain chain) {
        verify(chain, never()).filter(any());
    }

    private MockSetupData mockSetup(MockConfiguration configuration) {
        var exchange = mock(ServerWebExchange.class);
        var request = mock(ServerHttpRequest.class);
        when(exchange.getRequest()).thenReturn(request);
        var response = mock(ServerHttpResponse.class);
        when(exchange.getResponse()).thenReturn(response);

        var requestMetaData = mock(RequestMetaData.class);
        when(metaDataFactory.buildRequestMetaData(request)).thenReturn(requestMetaData);

        var responseMetaData = mock(ResponseMetaData.class);
        when(metaDataFactory.buildResponseMetaData(response)).thenReturn(responseMetaData);

        mockDecoratedRequests(request, response, requestMetaData, responseMetaData, configuration);
        var mutatedExchange = mockExchangeMutation(exchange);

        var chain = mockChain();
        when(validator.isReady()).thenReturn(configuration.isReady);
        mockTrafficSelectorMethods(requestMetaData, responseMetaData, configuration);

        return MockSetupData.builder()
            .exchange(exchange)
            .chain(chain)
            .mutatedExchange(mutatedExchange)
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
        ServerWebExchange exchange,
        WebFilterChain chain,
        ServerWebExchange mutatedExchange,
        RequestMetaData requestMetaData,
        ResponseMetaData responseMetaData
    ) {
    }
}
