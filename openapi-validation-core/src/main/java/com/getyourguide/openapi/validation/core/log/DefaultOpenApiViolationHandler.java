package com.getyourguide.openapi.validation.core.log;

import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.log.ViolationLogger;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.core.exclusions.InternalViolationExclusions;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DefaultOpenApiViolationHandler implements OpenApiViolationHandler {
    private final ViolationLogger logger;
    private final MetricsReporter metrics;
    private final InternalViolationExclusions violationExclusions;

    @Override
    public void onOpenApiViolation(OpenApiViolation violation) {
        if (violationExclusions.isExcluded(violation)) {
            return;
        }

        logger.log(violation);
        metrics.reportViolation(violation);
    }
}
