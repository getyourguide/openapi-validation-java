package com.getyourguide.openapi.validation.api.metrics;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;

public interface MetricsReporter {
    void reportViolation(OpenApiViolation violation);

    void reportStartup(boolean isValidationEnabled, double sampleRate, int validationReportThrottleWaitSeconds);
}
