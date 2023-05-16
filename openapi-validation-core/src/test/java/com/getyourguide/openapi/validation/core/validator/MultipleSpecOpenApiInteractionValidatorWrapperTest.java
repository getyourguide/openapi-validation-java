package com.getyourguide.openapi.validation.core.validator;

import static com.getyourguide.openapi.validation.core.validator.MultipleSpecOpenApiInteractionValidatorWrapper.MESSAGE_KEY_VALIDATOR_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class MultipleSpecOpenApiInteractionValidatorWrapperTest {
    private MultipleSpecOpenApiInteractionValidatorWrapper validator;

    @Test
    public void testCallsCorrectValidator() {
        var specificValidator = mockValidator();
        var catchAllValidator = mockValidator();

        validator = new MultipleSpecOpenApiInteractionValidatorWrapper(
            List.of(
                Pair.of(Pattern.compile("/test/.*"), specificValidator.validator),
                Pair.of(Pattern.compile(".*"), catchAllValidator.validator)
            )
        );

        assertRequestPathHitsCorrectValidator(specificValidator.validationReport, "/test/123");
        assertRequestPathHitsCorrectValidator(catchAllValidator.validationReport, "/123");

        assertResponsePathHitsCorrectValidator(specificValidator.validationReport, "/test/123");
        assertResponsePathHitsCorrectValidator(catchAllValidator.validationReport, "/123");
    }

    @Test
    public void testReturnsViolationWhenNoMatchingValidatorFound() {
        var specificValidator = mockValidator();

        validator = new MultipleSpecOpenApiInteractionValidatorWrapper(
            List.of(
                Pair.of(Pattern.compile("/test/.*"), specificValidator.validator())
            )
        );

        var path = "/123";
        var report = validator.validateRequest(new SimpleRequest.Builder("GET", path).build());

        var messages = report.getMessages();
        assertEquals(1, messages.size());
        var message = messages.get(0);
        assertEquals(MESSAGE_KEY_VALIDATOR_FOUND, message.getKey());
        assertEquals("No validator found for path: /123", message.getMessage());
    }

    private static MockValidatorResult mockValidator() {
        var catchAllValidator = mock(OpenApiInteractionValidatorWrapper.class);
        var catchAllValidationReport = mock(ValidationReport.class);
        when(catchAllValidator.validateRequest(any())).thenReturn(catchAllValidationReport);
        when(catchAllValidator.validateResponse(any(), any(), any())).thenReturn(catchAllValidationReport);
        MockValidatorResult result = new MockValidatorResult(catchAllValidator, catchAllValidationReport);
        return result;
    }

    private record MockValidatorResult(
        OpenApiInteractionValidatorWrapper validator,
        ValidationReport validationReport
    ) {
    }

    private void assertRequestPathHitsCorrectValidator(ValidationReport validationReport, String path) {
        var report = validator.validateRequest(new SimpleRequest.Builder("GET", path).build());
        assertEquals(validationReport, report);
    }

    private void assertResponsePathHitsCorrectValidator(ValidationReport validationReport, String path) {
        var report = validator.validateResponse(path, Request.Method.GET, mock());
        assertEquals(validationReport, report);
    }
}
