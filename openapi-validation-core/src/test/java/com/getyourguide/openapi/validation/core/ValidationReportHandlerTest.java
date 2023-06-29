package com.getyourguide.openapi.validation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.exclusions.ViolationExclusions;
import com.getyourguide.openapi.validation.api.log.ViolationLogger;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.core.throttle.ValidationReportThrottler;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ValidationReportHandlerTest {
    private ValidationReportThrottler throttleHelper;
    private ViolationLogger logger;
    private MetricsReporter metrics;
    private ViolationExclusions violationExclusions;

    private ValidationReportHandler validationReportHandler;

    @BeforeEach
    public void setUp() {
        throttleHelper = mock();
        logger = mock();
        metrics = mock();
        violationExclusions = mock();

        validationReportHandler = new ValidationReportHandler(throttleHelper, logger, metrics, violationExclusions);
    }

    @Test
    public void testWhenParameterNameIsPresentThenItShouldAddItToTheMessage() {
        mockNoThrottling();
        var request = mockRequestMetaData();
        var validationReport = mockValidationReport("parameterName");

        validationReportHandler.handleValidationReport(request, Direction.REQUEST, null, validationReport);

        var argumentCaptor = ArgumentCaptor.forClass(OpenApiViolation.class);
        verify(logger).log(argumentCaptor.capture());
        var openApiViolation = argumentCaptor.getValue();
        assertEquals(Optional.of("parameterName"), openApiViolation.getParameter());
        assertEquals(
            String.join("\n",
                "OpenAPI spec validation error [key]",
                "GET https://api.example.com/index",
                "User Agent: null",
                "Parameter: parameterName",
                "",
                "Violation message (toString)"),
            openApiViolation.getLogMessage());
    }

    private static RequestMetaData mockRequestMetaData() {
        var request = new RequestMetaData("GET", URI.create("https://api.example.com/index"), new HashMap<>());
        return request;
    }

    private static ValidationReport mockValidationReport(String parameterName) {
        var validationReport = mock(ValidationReport.class);
        var message = mock(ValidationReport.Message.class);
        when(message.getKey()).thenReturn("key");
        when(message.getMessage()).thenReturn("Violation message");
        when(message.toString()).thenReturn("Violation message (toString)");
        var context = mock(ValidationReport.MessageContext.class);
        var parameter = mock(Parameter.class);
        when(parameter.getName()).thenReturn(parameterName);
        when(context.getParameter()).thenReturn(Optional.of(parameter));
        when(message.getContext()).thenReturn(Optional.of(context));
        when(validationReport.getMessages()).thenReturn(List.of(message));
        return validationReport;
    }

    private void mockNoThrottling() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArguments()[1]).run();
            return null;
        }).when(throttleHelper).throttle(any(), any());
    }
}
