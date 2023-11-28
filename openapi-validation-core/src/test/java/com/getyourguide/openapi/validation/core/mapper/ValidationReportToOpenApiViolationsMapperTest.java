package com.getyourguide.openapi.validation.core.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidationReportToOpenApiViolationsMapperTest {
    private ValidationReportToOpenApiViolationsMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new ValidationReportToOpenApiViolationsMapper();
    }

    @Test
    public void testWhenParameterNameIsPresentThenItShouldAddItToTheMessage() {
        var request = mockRequestMetaData();
        var validationReport = mockValidationReport("parameterName");

        var violations = mapper.map(validationReport, request, null, Direction.REQUEST, null);

        assertEquals(1, violations.size());
        var openApiViolation = violations.get(0);
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
        return new RequestMetaData("GET", URI.create("https://api.example.com/index"), new HashMap<>());
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
}
