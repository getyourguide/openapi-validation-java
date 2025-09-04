package com.getyourguide.openapi.validation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.core.exclusions.InternalViolationExclusions;
import com.getyourguide.openapi.validation.core.mapper.ValidationReportToOpenApiViolationsMapper;
import com.getyourguide.openapi.validation.core.validator.OpenApiInteractionValidatorWrapper;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
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
    public void testWhenThreadPoolExecutorRejectsExecutionThenItShouldNotThrow() {
        Mockito.doThrow(new RejectedExecutionException()).when(executor).execute(any());

        openApiRequestValidator.validateRequestObjectAsync(mock(), null, null, mock());
    }

    @Test
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

    private void verifyQueryParamValueEquals(
        ArgumentCaptor<SimpleRequest> simpleRequestArgumentCaptor,
        String name,
        String expected
    ) {
        var ids = simpleRequestArgumentCaptor.getValue().getQueryParameterValues(name).iterator().next();
        assertEquals(expected, ids);
    }
}
