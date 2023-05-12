package com.getyourguide.openapi.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.getyourguide.openapi.validation.api.metrics.MetricTag;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpenApiValidationApplicationPropertiesTest {

    private static final Double SAMPLE_RATE = 0.001;
    private static final String SPECIFICATION_FILE_PATH = "/tmp/openapi.yaml";
    private static final Integer VALIDATION_REPORT_THROTTLE_WAIT_SECONDS = 10;
    private static final String VALIDATION_REPORT_METRIC_NAME = "openapi_validation_error";
    private static final String VALIDATION_REPORT_METRIC_ADDITONAL_TAGS_STRING = "service=payment,team=chk";
    private static final String EXCLUDED_PATHS = "/_readiness,/_liveness,/_metrics";

    @Test
    void getters() {
        var loggingConfiguration = new OpenApiValidationApplicationProperties(
            SAMPLE_RATE,
            SPECIFICATION_FILE_PATH,
            VALIDATION_REPORT_THROTTLE_WAIT_SECONDS,
            VALIDATION_REPORT_METRIC_NAME,
            VALIDATION_REPORT_METRIC_ADDITONAL_TAGS_STRING,
            EXCLUDED_PATHS
        );

        assertEquals(SAMPLE_RATE, loggingConfiguration.getSampleRate());
        assertEquals(SPECIFICATION_FILE_PATH, loggingConfiguration.getSpecificationFilePath());
        assertEquals(VALIDATION_REPORT_THROTTLE_WAIT_SECONDS, loggingConfiguration.getValidationReportThrottleWaitSeconds());
        assertEquals(VALIDATION_REPORT_METRIC_NAME, loggingConfiguration.getValidationReportMetricName());
        assertEquals(
            List.of(new MetricTag("service", "payment"), new MetricTag("team", "chk")),
            loggingConfiguration.getValidationReportMetricAdditionalTags()
        );
        assertEquals(EXCLUDED_PATHS, loggingConfiguration.getExcludedPaths());
        assertEquals(Set.of("/_readiness","/_liveness","/_metrics"), loggingConfiguration.getExcludedPathsAsSet());
    }
}
