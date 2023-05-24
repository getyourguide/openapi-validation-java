package com.getyourguide.openapi.validation.core.throttle;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;

public interface ValidationReportThrottler {

    void throttle(OpenApiViolation openApiViolation, Runnable runnable);
}
