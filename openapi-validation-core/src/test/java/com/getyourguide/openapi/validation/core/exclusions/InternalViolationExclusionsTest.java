package com.getyourguide.openapi.validation.core.exclusions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.getyourguide.openapi.validation.api.exclusions.ViolationExclusions;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InternalViolationExclusionsTest {
    private ViolationExclusions customViolationExclusions;
    private InternalViolationExclusions violationExclusions;

    @BeforeEach
    public void setup() {
        customViolationExclusions = mock();
        violationExclusions = new InternalViolationExclusions(customViolationExclusions);
    }

    @Test
    public void whenViolation_thenViolationNotExcluded() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        checkViolationNotExcluded(buildSimpleViolation(Direction.RESPONSE, 404));
        checkViolationNotExcluded(buildSimpleViolation(Direction.RESPONSE, 400));
        checkViolationNotExcluded(buildSimpleViolation(Direction.REQUEST, 200));
        checkViolationNotExcluded(buildSimpleViolation(Direction.REQUEST, null));
        checkViolationNotExcluded(buildSimpleViolation(Direction.RESPONSE, 200));
    }

    private static OpenApiViolation buildSimpleViolation(Direction direction, Integer responseStatus) {
        return OpenApiViolation.builder()
            .direction(direction)
            .rule("validation." + (direction == Direction.REQUEST ? "request" : "response") + ".something")
            .responseStatus(responseStatus != null ? Optional.of(responseStatus) : Optional.empty())
            .message("Some violation message")
            .build();
    }

    @Test
    public void whenCustomViolationExclusion_thenViolationExcluded() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(true);

        checkViolationExcluded(OpenApiViolation.builder().build());
    }

    @Test
    public void whenInstanceFailedToMatchExactlyOne_thenViolationExcluded() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        checkViolationExcluded(OpenApiViolation.builder()
            .rule("validation.response.body.schema.oneOf")
            .message("[Path '/v1/endpoint'] Instance failed to match exactly one schema (matched 2 out of 4)").build());
    }

    @Test
    public void whenInstanceFailedToMatchExactlyOneWithOneOf24_thenViolationExcluded() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        checkViolationExcluded(OpenApiViolation.builder()
            .rule("validation.request.body.schema.oneOf")
            .message("[Path '/v1/endpoint'] Instance failed to match exactly one schema (matched 2 out of 24)")
            .build());
    }

    @Test
    public void when404ResponseWithApiPathNotSpecified_ThenViolationExcluded() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        checkViolationExcluded(OpenApiViolation.builder()
            .direction(Direction.RESPONSE)
            .rule("validation.request.path.missing")
            .responseStatus(Optional.of(404))
            .message("No API path found that matches request '/nothing'")
            .build());
    }

    @Test
    public void whenRequestWithApiPathNotSpecified_thenViolationExcluded() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        checkViolationExcluded(OpenApiViolation.builder()
            .direction(Direction.REQUEST)
            .rule("validation.request.path.missing")
            .responseStatus(Optional.empty())
            .message("No API path found that matches request '/nothing'")
            .build());
    }

    @Test
    public void whenRequestViolationsAnd400_thenViolationExcluded() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        checkViolationExcluded(OpenApiViolation.builder()
            .direction(Direction.REQUEST)
            .responseStatus(Optional.of(400))
            .message("")
            .build());
    }

    @Test
    public void when405ResponseCodeWithOperationNotAllowedViolation_thenViolationExcluded() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        checkViolationExcluded(OpenApiViolation.builder()
            .direction(Direction.REQUEST)
            .rule("validation.request.operation.notAllowed")
            .responseStatus(Optional.of(405))
            .message("")
            .build());

        checkViolationExcluded(OpenApiViolation.builder()
            .direction(Direction.RESPONSE)
            .rule("validation.request.operation.notAllowed")
            .responseStatus(Optional.of(405))
            .message("")
            .build());
    }

    private void checkViolationNotExcluded(OpenApiViolation violation) {
        var isExcluded = violationExclusions.isExcluded(violation);

        assertFalse(isExcluded);
    }

    private void checkViolationExcluded(OpenApiViolation violation) {
        var isExcluded = violationExclusions.isExcluded(violation);

        assertTrue(isExcluded);
    }
}
