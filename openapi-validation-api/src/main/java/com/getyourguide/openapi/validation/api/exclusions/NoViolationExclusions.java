package com.getyourguide.openapi.validation.api.exclusions;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;

public class NoViolationExclusions implements ViolationExclusions {
    @Override
    public boolean isExcluded(OpenApiViolation violation) {
        return false;
    }
}
