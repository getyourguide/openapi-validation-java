package com.getyourguide.openapi.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.getyourguide.openapi.validation.api.exclusions.ExcludedHeader;
import com.getyourguide.openapi.validation.api.metrics.MetricTag;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpenApiValidationApplicationPropertiesTest {

    private static final Double SAMPLE_RATE = 0.001;
    private static final String SPECIFICATION_FILE_PATH = "/tmp/openapi.yaml";
    private static final Integer VALIDATION_REPORT_THROTTLE_WAIT_SECONDS = 10;
    private static final String VALIDATION_REPORT_METRIC_NAME = "openapi_validation";
    private static final String VALIDATION_REPORT_METRIC_ADDITONAL_TAGS_STRING = "service=payment,team=chk";
    private static final String EXCLUDED_PATHS = "/_readiness,/_liveness,/_metrics";
    private static final List<String> EXCLUDED_HEADERS = List.of("User-Agent: .*(bingbot|googlebot).*", "x-is-bot: true");

    @Test
    void getters() {
        var loggingConfiguration = new OpenApiValidationApplicationProperties(
            SAMPLE_RATE,
            SPECIFICATION_FILE_PATH,
            VALIDATION_REPORT_THROTTLE_WAIT_SECONDS,
            VALIDATION_REPORT_METRIC_NAME,
            VALIDATION_REPORT_METRIC_ADDITONAL_TAGS_STRING,
            EXCLUDED_PATHS,
            EXCLUDED_HEADERS,
            true,
            false
        );

        assertEquals(SAMPLE_RATE, loggingConfiguration.getSampleRate());
        assertEquals(SPECIFICATION_FILE_PATH, loggingConfiguration.getSpecificationFilePath());
        assertEquals(VALIDATION_REPORT_THROTTLE_WAIT_SECONDS,
            loggingConfiguration.getValidationReportThrottleWaitSeconds());
        assertEquals(VALIDATION_REPORT_METRIC_NAME, loggingConfiguration.getValidationReportMetricName());
        assertEquals(
            List.of(new MetricTag("service", "payment"), new MetricTag("team", "chk")),
            loggingConfiguration.getValidationReportMetricAdditionalTags()
        );
        assertEquals(EXCLUDED_PATHS, loggingConfiguration.getExcludedPaths());
        assertExcludedHeaders(loggingConfiguration.getExcludedHeaders());
        assertEquals(Set.of("/_readiness", "/_liveness", "/_metrics"), loggingConfiguration.getExcludedPathsAsSet());
        assertTrue(loggingConfiguration.getShouldFailOnRequestViolation());
        assertFalse(loggingConfiguration.getShouldFailOnResponseViolation());
    }

    private void assertExcludedHeaders(List<ExcludedHeader> excludedHeaders) {
        assertEquals(EXCLUDED_HEADERS.size(), excludedHeaders.size());
        for (int i = 0; i < EXCLUDED_HEADERS.size(); i++) {
            assertExcludedHeader(excludedHeaders, i);
        }
    }

    private static void assertExcludedHeader(List<ExcludedHeader> excludedHeaders, int index) {
        var excludedHeader = EXCLUDED_HEADERS.get(index);
        var parts = excludedHeader.split(":");
        assertEquals(parts[0].trim(), excludedHeaders.get(index).headerName());
        assertEquals(parts[1].trim(), excludedHeaders.get(index).headerValuePattern().pattern());
    }
}
