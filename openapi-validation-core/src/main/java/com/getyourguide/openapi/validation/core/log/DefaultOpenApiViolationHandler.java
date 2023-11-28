package com.getyourguide.openapi.validation.core.log;

import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.log.ViolationLogger;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DefaultOpenApiViolationHandler implements OpenApiViolationHandler {
    private final ViolationLogger logger;
    private final MetricsReporter metrics;

    @Override
    public void onOpenApiViolation(OpenApiViolation violation) {
        logger.log(violation);
        metrics.reportViolation(violation);
    }
}
