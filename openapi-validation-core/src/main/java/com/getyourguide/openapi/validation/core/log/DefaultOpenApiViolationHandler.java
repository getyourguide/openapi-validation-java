package com.getyourguide.openapi.validation.core.log;

import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.log.ViolationLogger;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.core.exclusions.InternalViolationExclusions;
import com.getyourguide.openapi.validation.core.throttle.ValidationReportThrottler;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DefaultOpenApiViolationHandler implements OpenApiViolationHandler {
    private final ValidationReportThrottler throttleHelper;
    private final ViolationLogger logger;
    private final MetricsReporter metrics;
    private final InternalViolationExclusions violationExclusions;

    @Override
    public void onOpenApiViolation(OpenApiViolation violation) {
        if (violationExclusions.isExcluded(violation)) {
            return;
        }

        throttleHelper.throttle(violation, () -> logValidationError(violation));
    }

    private void logValidationError(OpenApiViolation openApiViolation) {
        logger.log(openApiViolation);
        metrics.reportViolation(openApiViolation);
    }
}
