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
    public void testWhenViolationThenReturnFalse() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        var isExcluded = violationExclusions.isExcluded(OpenApiViolation.builder()
            .direction(Direction.REQUEST)
            .rule("validation.request.something")
            .responseStatus(Optional.of(404))
            .message("Some violation message")
            .build());

        assertFalse(isExcluded);
    }

    @Test
    public void testWhenCustomViolationExclusionThenReturnTrue() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(true);

        var isExcluded = violationExclusions.isExcluded(OpenApiViolation.builder().build());

        assertTrue(isExcluded);
    }

    @Test
    public void testWhenInstanceFailedToMatchExactlyOneThenReturnTrue() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        var isExcluded = violationExclusions.isExcluded(OpenApiViolation.builder().message("[Path '/v1/endpoint'] Instance failed to match exactly one schema (matched 2 out of 4)").build());

        assertTrue(isExcluded);
    }

    @Test
    public void testWhen404ResponseWithApiPathNotSpecifiedThenReturnTrue() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        var isExcluded = violationExclusions.isExcluded(OpenApiViolation.builder()
            .direction(Direction.RESPONSE)
            .rule("validation.request.path.missing")
            .responseStatus(Optional.of(404))
            .message("No API path found that matches request '/nothing'")
            .build());

        assertTrue(isExcluded);
    }

    @Test
    public void testWhenRequestWithApiPathNotSpecifiedThenReturnTrue() {
        when(customViolationExclusions.isExcluded(any())).thenReturn(false);

        var isExcluded = violationExclusions.isExcluded(OpenApiViolation.builder()
            .direction(Direction.REQUEST)
            .rule("validation.request.path.missing")
            .responseStatus(Optional.empty())
            .message("No API path found that matches request '/nothing'")
            .build());

        assertTrue(isExcluded);
    }
}
