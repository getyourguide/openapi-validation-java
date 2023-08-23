package com.getyourguide.openapi.validation.core.exclusions;

import com.getyourguide.openapi.validation.api.exclusions.ViolationExclusions;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InternalViolationExclusions {
    private final ViolationExclusions customViolationExclusions;

    public boolean isExcluded(OpenApiViolation violation) {
        return customViolationExclusions.isExcluded(violation)
            // If it matches more than 1, then we don't want to log a validation error
            || violation.getMessage().matches(
            ".*\\[Path '[^']+'] Instance failed to match exactly one schema \\(matched [1-9][0-9]* out of \\d\\).*");
    }
}
