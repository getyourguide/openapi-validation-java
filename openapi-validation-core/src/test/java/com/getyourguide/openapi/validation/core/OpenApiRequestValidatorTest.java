package com.getyourguide.openapi.validation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.log.LogLevel;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.core.exclusions.InternalViolationExclusions;
import com.getyourguide.openapi.validation.core.mapper.ValidationReportToOpenApiViolationsMapper;
import com.getyourguide.openapi.validation.core.validator.OpenApiInteractionValidatorWrapper;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class OpenApiRequestValidatorTest {

    private Executor executor;
    private OpenApiInteractionValidatorWrapper validator;
    private ValidationReportToOpenApiViolationsMapper mapper;

    private OpenApiRequestValidator openApiRequestValidator;
    private InternalViolationExclusions internalViolationExclusions;

    @BeforeEach
    public void setup() {
        executor = mock();
        validator = mock();
        mapper = mock(ValidationReportToOpenApiViolationsMapper.class);
        when(mapper.map(any(), any(), any(), any(), any())).thenReturn(List.of());
        internalViolationExclusions = mock();

        openApiRequestValidator = new OpenApiRequestValidator(
            executor,
            mock(),
            validator,
            mapper,
            internalViolationExclusions,
            mock()
        );
    }

    @Test
    @DisplayName("When thread pool executor rejects execution then it should not throw")
    public void testWhenThreadPoolExecutorRejectsExecutionThenItShouldNotThrow() {
        Mockito.doThrow(new RejectedExecutionException()).when(executor).execute(any());

        openApiRequestValidator.validateRequestObjectAsync(mock(), null, null, mock());
    }

    @Nested
    @DisplayName("validateRequestObject")
    class ValidateRequestObjectTests {

        @Test
        @DisplayName("When encoded query param is passed then validation should happen with query param decoded")
        public void testWhenEncodedQueryParamIsPassedThenValidationShouldHappenWithQueryParamDecoded() {
            var uri = URI.create("https://api.example.com?ids=1%2C2%2C3&text=e%3Dmc2%20%26%20more&spaces=this+is+a+sparta");
            var request = new RequestMetaData("GET", uri, new HashMap<>());

            openApiRequestValidator.validateRequestObject(request, null);

            var simpleRequestArgumentCaptor = ArgumentCaptor.forClass(SimpleRequest.class);
            verify(validator).validateRequest(simpleRequestArgumentCaptor.capture());
            verifyQueryParamValueEquals(simpleRequestArgumentCaptor, "ids", "1,2,3");
            verifyQueryParamValueEquals(simpleRequestArgumentCaptor, "text", "e=mc2 & more");
            verifyQueryParamValueEquals(simpleRequestArgumentCaptor, "spaces", "this is a sparta");
        }

        @Test
        @DisplayName("When violation is excluded then it should not be returned")
        public void testWhenViolationIsExcludedThenItShouldNotBeReturned() {
            var uri = URI.create("https://api.example.com/path");
            var request = new RequestMetaData("GET", uri, new HashMap<>());
            var validationReport = mock(ValidationReport.class);
            when(validator.validateRequest(any())).thenReturn(validationReport);
            var violationExcluded = mock(OpenApiViolation.class);
            var violations = List.of(violationExcluded, mock(OpenApiViolation.class));
            when(mapper.map(any(), any(), any(), any(), any())).thenReturn(violations);
            when(internalViolationExclusions.isExcluded(violationExcluded)).thenReturn(true);

            var result = openApiRequestValidator.validateRequestObject(request, null);

            assertEquals(1, result.size());
            assertEquals(violations.get(1), result.getFirst());
        }

        @Test
        @DisplayName("When violation has log level IGNORE then it should not be returned")
        public void testWhenRequestViolationHasLogLevelIgnoreThenItShouldNotBeReturned() {
            var uri = URI.create("https://api.example.com/path");
            var request = new RequestMetaData("GET", uri, new HashMap<>());
            var validationReport = mock(ValidationReport.class);
            when(validator.validateRequest(any())).thenReturn(validationReport);

            var violationIgnored = createViolation(LogLevel.IGNORE);
            var violationError = createViolation(LogLevel.ERROR);

            var violations = List.of(violationIgnored, violationError);
            when(mapper.map(any(), any(), any(), any(), any())).thenReturn(violations);

            var result = openApiRequestValidator.validateRequestObject(request, null);

            assertEquals(1, result.size());
            assertEquals(violationError, result.getFirst());
        }

        @Test
        @DisplayName("When violation has log level IGNORE and another is excluded then both should not be returned")
        public void testWhenRequestViolationHasLogLevelIgnoreAndIsExcludedThenItShouldNotBeReturned() {
            var uri = URI.create("https://api.example.com/path");
            var request = new RequestMetaData("GET", uri, new HashMap<>());
            var validationReport = mock(ValidationReport.class);
            when(validator.validateRequest(any())).thenReturn(validationReport);

            var violationIgnored = createViolation(LogLevel.IGNORE);
            var violationExcluded = createViolation(LogLevel.WARN);
            when(internalViolationExclusions.isExcluded(violationExcluded)).thenReturn(true);
            var violationError = createViolation(LogLevel.ERROR);

            var violations = List.of(violationIgnored, violationExcluded, violationError);
            when(mapper.map(any(), any(), any(), any(), any())).thenReturn(violations);

            var result = openApiRequestValidator.validateRequestObject(request, null);

            assertEquals(1, result.size());
            assertEquals(violationError, result.getFirst());
        }

        @Test
        @DisplayName("When all violations are ignored then empty list is returned")
        public void testWhenAllRequestViolationsAreIgnoredThenEmptyListIsReturned() {
            var uri = URI.create("https://api.example.com/path");
            var request = new RequestMetaData("GET", uri, new HashMap<>());
            var validationReport = mock(ValidationReport.class);
            when(validator.validateRequest(any())).thenReturn(validationReport);

            var violation1 = createViolation(LogLevel.IGNORE);
            var violation2 = createViolation(LogLevel.IGNORE);

            var violations = List.of(violation1, violation2);
            when(mapper.map(any(), any(), any(), any(), any())).thenReturn(violations);

            var result = openApiRequestValidator.validateRequestObject(request, null);

            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("validateResponseObject")
    class ValidateResponseObjectTests {

        @Test
        @DisplayName("When violation has log level IGNORE then it should not be returned")
        public void testWhenResponseViolationHasLogLevelIgnoreThenItShouldNotBeReturned() {
            var uri = URI.create("https://api.example.com/path");
            var request = new RequestMetaData("GET", uri, new HashMap<>());
            var response = new ResponseMetaData(200, "application/json", new HashMap<>());
            var validationReport = mock(ValidationReport.class);
            when(validator.validateResponse(any(), any(), any())).thenReturn(validationReport);

            var violationIgnored = createViolation(LogLevel.IGNORE);
            var violationWarn = createViolation(LogLevel.WARN);

            var violations = List.of(violationIgnored, violationWarn);
            when(mapper.map(any(), any(), any(), any(), any())).thenReturn(violations);

            var result = openApiRequestValidator.validateResponseObject(request, response, null);

            assertEquals(1, result.size());
            assertEquals(violationWarn, result.getFirst());
        }

        @Test
        @DisplayName("When violation has log level IGNORE and another is excluded then both should not be returned")
        public void testWhenResponseViolationHasLogLevelIgnoreAndIsExcludedThenItShouldNotBeReturned() {
            var uri = URI.create("https://api.example.com/path");
            var request = new RequestMetaData("GET", uri, new HashMap<>());
            var response = new ResponseMetaData(200, "application/json", new HashMap<>());
            var validationReport = mock(ValidationReport.class);
            when(validator.validateResponse(any(), any(), any())).thenReturn(validationReport);

            var violationIgnored = createViolation(LogLevel.IGNORE);
            var violationExcluded = createViolation(LogLevel.INFO);
            when(internalViolationExclusions.isExcluded(violationExcluded)).thenReturn(true);
            var violationError = createViolation(LogLevel.ERROR);

            var violations = List.of(violationIgnored, violationExcluded, violationError);
            when(mapper.map(any(), any(), any(), any(), any())).thenReturn(violations);

            var result = openApiRequestValidator.validateResponseObject(request, response, null);

            assertEquals(1, result.size());
            assertEquals(violationError, result.getFirst());
        }

        @Test
        @DisplayName("When all violations are ignored then empty list is returned")
        public void testWhenAllResponseViolationsAreIgnoredThenEmptyListIsReturned() {
            var uri = URI.create("https://api.example.com/path");
            var request = new RequestMetaData("GET", uri, new HashMap<>());
            var response = new ResponseMetaData(200, "application/json", new HashMap<>());
            var validationReport = mock(ValidationReport.class);
            when(validator.validateResponse(any(), any(), any())).thenReturn(validationReport);

            var violation1 = createViolation(LogLevel.IGNORE);
            var violation2 = createViolation(LogLevel.IGNORE);

            var violations = List.of(violation1, violation2);
            when(mapper.map(any(), any(), any(), any(), any())).thenReturn(violations);

            var result = openApiRequestValidator.validateResponseObject(request, response, null);

            assertEquals(0, result.size());
        }
    }

    private void verifyQueryParamValueEquals(
        ArgumentCaptor<SimpleRequest> simpleRequestArgumentCaptor,
        String name,
        String expected
    ) {
        var ids = simpleRequestArgumentCaptor.getValue().getQueryParameterValues(name).iterator().next();
        assertEquals(expected, ids);
    }

    private OpenApiViolation createViolation(LogLevel level) {
        var violation = mock(OpenApiViolation.class);
        when(violation.getLevel()).thenReturn(level);
        return violation;
    }
}
