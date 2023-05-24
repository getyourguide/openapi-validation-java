package com.getyourguide.openapi.validation.core.throttle;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;

public class ValidationReportThrottlerNone implements ValidationReportThrottler {
    @Override
    public void throttle(OpenApiViolation openApiViolation, Runnable runnable) {
        runnable.run();
    }
}
