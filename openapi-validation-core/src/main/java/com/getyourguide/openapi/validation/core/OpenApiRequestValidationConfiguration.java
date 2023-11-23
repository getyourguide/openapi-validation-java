package com.getyourguide.openapi.validation.core;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OpenApiRequestValidationConfiguration {
    private double sampleRate;
    private int validationReportThrottleWaitSeconds;
    private boolean shouldFailOnRequestViolation;
}
