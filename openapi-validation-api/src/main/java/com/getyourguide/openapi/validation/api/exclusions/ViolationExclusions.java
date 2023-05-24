package com.getyourguide.openapi.validation.api.exclusions;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;

public interface ViolationExclusions {
    boolean isExcluded(OpenApiViolation violation);
}
