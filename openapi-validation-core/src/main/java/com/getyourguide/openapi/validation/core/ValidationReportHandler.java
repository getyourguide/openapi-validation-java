package com.getyourguide.openapi.validation.core;

import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.log.ViolationLogger;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.core.exclusions.InternalViolationExclusions;
import com.getyourguide.openapi.validation.core.mapper.ValidationReportToOpenApiViolationsMapper;
import com.getyourguide.openapi.validation.core.throttle.ValidationReportThrottler;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ValidationReportHandler {
    private final ValidationReportThrottler throttleHelper;
    private final ViolationLogger logger;
    private final MetricsReporter metrics;
    private final InternalViolationExclusions violationExclusions;
    private final ValidationReportToOpenApiViolationsMapper mapper;

    public void handleValidationReport(
        RequestMetaData request,
        @Nullable ResponseMetaData response,
        Direction direction,
        String body,
        ValidationReport result
    ) {
        // TODO get rid of this class, should be interface & default impl and split up into 3 classes that are decorators
        //      - OpenApiViolationHandler (interface)
        //      - DefaultOpenApiViolationHandler (logger & metric)
        //      - ThrottlerOpenApiViolationHandler (throttler - decorator)
        //      - ExclusionsOpenApiViolationHandler (exclusions - decorator)
        if (!result.getMessages().isEmpty()) {
            mapper.map(result, request, response,direction, body)
                .stream()
                .filter(violation -> !violationExclusions.isExcluded(violation))
                .forEach(violation -> throttleHelper.throttle(violation, () -> logValidationError(violation)));
        }
    }

    private void logValidationError(OpenApiViolation openApiViolation) {
        logger.log(openApiViolation);
        metrics.reportViolation(openApiViolation);
    }

}
