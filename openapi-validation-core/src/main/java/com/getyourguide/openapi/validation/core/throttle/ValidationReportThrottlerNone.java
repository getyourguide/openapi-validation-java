package com.getyourguide.openapi.validation.core.throttle;

import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.model.Direction;

public class ValidationReportThrottlerNone implements ValidationReportThrottler {
    @Override
    public void throttle(ValidationReport.Message message, Direction direction, Runnable runnable) {
        runnable.run();
    }
}
