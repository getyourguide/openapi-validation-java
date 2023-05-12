package com.getyourguide.openapi.validation.core.throttle;

import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.model.Direction;

public interface ValidationReportThrottler {

    void throttle(ValidationReport.Message message, Direction direction, Runnable runnable);
}
