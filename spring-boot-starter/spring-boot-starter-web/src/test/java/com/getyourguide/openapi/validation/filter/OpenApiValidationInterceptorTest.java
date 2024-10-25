package com.getyourguide.openapi.validation.filter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

class OpenApiValidationInterceptorTest extends BaseFilterTest {

    private final OpenApiValidationInterceptor httpInterceptor =
        new OpenApiValidationInterceptor(validator, trafficSelector, metaDataFactory, contentCachingWrapperFactory, openApiViolationHandler);

    @Test
    public void testNormalFlowWithValidation() {
        var mockData = mockSetup(MockConfiguration.builder().build());

        httpInterceptor.postHandle(mockData.request(), mockData.response(), new Object(), null);

        verifyRequestValidatedAsync(mockData);
        verifyResponseValidatedAsync(mockData);
    }

    @Test
    public void testNoValidationIfNotReady() {
        var mockData = mockSetup(MockConfiguration.builder().build());
        mockData.request().setAttribute(OpenApiValidationFilter.ATTRIBUTE_SKIP_VALIDATION, true);

        httpInterceptor.preHandle(mockData.request(), mockData.response(), new Object());
        httpInterceptor.postHandle(mockData.request(), mockData.response(), new Object(), null);
        httpInterceptor.afterCompletion(mockData.request(), mockData.response(), new Object(), null);

        verifyNoValidation();
    }

    @Test
    public void testNoValidationIfNotCanRequestBeValidated() {
        var mockData = mockSetup(MockConfiguration.builder().canRequestBeValidated(false).build());

        httpInterceptor.preHandle(mockData.request(), mockData.response(), new Object());
        httpInterceptor.postHandle(mockData.request(), mockData.response(), new Object(), null);
        httpInterceptor.afterCompletion(mockData.request(), mockData.response(), new Object(), null);

        verifyNoRequestValidation();
        verifyResponseValidatedAsync(mockData);
    }

    @Test
    public void testNoValidationIfNotCanResponseBeValidated() {
        var mockData = mockSetup(MockConfiguration.builder().canResponseBeValidated(false).build());

        httpInterceptor.preHandle(mockData.request(), mockData.response(), new Object());
        httpInterceptor.postHandle(mockData.request(), mockData.response(), new Object(), null);
        httpInterceptor.afterCompletion(mockData.request(), mockData.response(), new Object(), null);

        verifyRequestValidatedAsync(mockData);
        verifyNoResponseValidation();
    }

    @Test
    public void testShouldFailOnRequestViolationWithoutViolation() {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnRequestViolation(true).build());

        httpInterceptor.preHandle(mockData.request(), mockData.response(), new Object());
        httpInterceptor.postHandle(mockData.request(), mockData.response(), new Object(), null);
        httpInterceptor.afterCompletion(mockData.request(), mockData.response(), new Object(), null);

        verifyRequestValidatedSync(mockData);
        verifyResponseValidatedAsync(mockData);
    }

    @Test
    public void testShouldFailOnReResponseViolationWithoutViolation() {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnResponseViolation(true).build());

        httpInterceptor.preHandle(mockData.request(), mockData.response(), new Object());
        httpInterceptor.postHandle(mockData.request(), mockData.response(), new Object(), null);
        httpInterceptor.afterCompletion(mockData.request(), mockData.response(), new Object(), null);

        verifyRequestValidatedAsync(mockData);
        verifyResponseValidatedSync(mockData);
    }

    @Test
    public void testShouldFailOnRequestViolationWithViolation() {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnRequestViolation(true).build());
        when(validator.validateRequestObject(eq(mockData.requestMetaData()), eq(REQUEST_BODY)))
            .thenReturn(List.of(mock(OpenApiViolation.class)));

        assertThrows(ResponseStatusException.class,
            () -> httpInterceptor.preHandle(mockData.request(), mockData.response(), new Object()));
        httpInterceptor.postHandle(mockData.request(), mockData.response(), new Object(), null);
        httpInterceptor.afterCompletion(mockData.request(), mockData.response(), new Object(), null);

        verifyRequestValidatedSync(mockData);
        verifyNoResponseValidation();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    public void testShouldSupportBodyOnGetRequests(String requestMethod) {
        var mockData = mockSetup(MockConfiguration.builder().requestBody("{\"field\": 1}}").requestMethod(requestMethod).build());

        httpInterceptor.preHandle(mockData.request(), mockData.response(), new Object());
        httpInterceptor.postHandle(mockData.request(), mockData.response(), new Object(), null);
        httpInterceptor.afterCompletion(mockData.request(), mockData.response(), new Object(), null);

        verifyRequestValidatedAsync(mockData, "{\"field\": 1}}");
        verifyResponseValidatedAsync(mockData);
    }

    @Test
    public void testShouldFailOnResponseViolationWithViolation() {
        var mockData = mockSetup(MockConfiguration.builder().shouldFailOnResponseViolation(true).build());
        when(
            validator.validateResponseObject(
                eq(mockData.requestMetaData()),
                eq(mockData.responseMetaData()), eq(REQUEST_BODY)
            )
        ).thenReturn(List.of(mock(OpenApiViolation.class)));


        httpInterceptor.preHandle(mockData.request(), mockData.response(), new Object());
        assertThrows(ResponseStatusException.class,
            () -> httpInterceptor.postHandle(mockData.request(), mockData.response(), new Object(), null));
        httpInterceptor.afterCompletion(mockData.request(), mockData.response(), new Object(), null);

        verifyRequestValidatedAsync(mockData);
        verifyResponseValidatedSync(mockData);
    }

    private void verifyNoValidation() {
        verifyNoRequestValidation();
        verifyNoResponseValidation();
    }

    private void verifyNoRequestValidation() {
        verify(validator, never()).validateRequestObjectAsync(any(), any(), anyString(), eq(openApiViolationHandler));
        verify(validator, never()).validateRequestObject(any(), anyString());
    }

    private void verifyNoResponseValidation() {
        verify(validator, never()).validateResponseObjectAsync(any(), any(), anyString(), eq(openApiViolationHandler));
        verify(validator, never()).validateResponseObject(any(), any(), anyString());
    }

    private void verifyRequestValidatedAsync(MockSetupData mockData) {
        verifyRequestValidatedAsync(mockData, REQUEST_BODY);
    }

    private void verifyRequestValidatedAsync(MockSetupData mockData, String requestBody) {
        verify(validator).validateRequestObjectAsync(
            eq(mockData.requestMetaData()),
            eq(mockData.responseMetaData()),
            eq(requestBody),
            eq(openApiViolationHandler)
        );
    }

    private void verifyRequestValidatedSync(MockSetupData mockData) {
        verify(validator).validateRequestObject(eq(mockData.requestMetaData()), eq(REQUEST_BODY));
    }

    private void verifyResponseValidatedAsync(MockSetupData mockData) {
        verify(validator).validateResponseObjectAsync(
            eq(mockData.requestMetaData()),
            eq(mockData.responseMetaData()),
            eq(RESPONSE_BODY),
            eq(openApiViolationHandler)
        );
    }

    private void verifyResponseValidatedSync(MockSetupData mockData) {
        verify(validator)
            .validateResponseObject(eq(mockData.requestMetaData()), eq(mockData.responseMetaData()), eq(RESPONSE_BODY));
    }
}
