package com.getyourguide.openapi.validation.core.log;

import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.core.exclusions.InternalViolationExclusions;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ExclusionsOpenApiViolationHandler implements OpenApiViolationHandler {
    private final OpenApiViolationHandler delegate;
    private final InternalViolationExclusions violationExclusions;

    @Override
    public void onOpenApiViolation(OpenApiViolation violation) {
        if (violationExclusions.isExcluded(violation)) {
            return;
        }

        delegate.onOpenApiViolation(violation);
    }
}
