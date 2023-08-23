package com.getyourguide.openapi.validation;

import static com.getyourguide.openapi.validation.OpenApiValidationApplicationProperties.PROPERTY_PREFIX;

import com.getyourguide.openapi.validation.api.exclusions.ExcludedHeader;
import com.getyourguide.openapi.validation.api.log.LogLevel;
import com.getyourguide.openapi.validation.api.metrics.MetricTag;
import com.getyourguide.openapi.validation.util.CommaSeparatedStringsUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = PROPERTY_PREFIX)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OpenApiValidationApplicationProperties {
    public static final String PROPERTY_PREFIX = "openapi.validation";

    private Double sampleRate;
    private String specificationFilePath;
    private LogLevel violationLogLevel;
    private Integer validationReportThrottleWaitSeconds;
    private String validationReportMetricName;
    private String validationReportMetricAdditionalTags;
    private String excludedPaths;
    private List<String> excludedHeaders;
    private Boolean shouldFailOnRequestViolation;
    private Boolean shouldFailOnResponseViolation;

    public List<MetricTag> getValidationReportMetricAdditionalTags() {
        if (validationReportMetricAdditionalTags == null) {
            return null;
        }

        return Arrays.stream(validationReportMetricAdditionalTags.split(","))
            .map(tag -> {
                var parts = tag.split("=");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid tag format: " + tag);
                }
                return new MetricTag(parts[0].trim(), parts[1].trim());
            })
            .toList();
    }

    public Set<String> getExcludedPathsAsSet() {
        return CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet(excludedPaths);
    }

    public List<ExcludedHeader> getExcludedHeaders() {
        if (excludedHeaders == null) {
            return Collections.emptyList();
        }

        return excludedHeaders.stream()
            .map(header -> {
                var parts = header.split(":", 2);
                if (parts.length != 2) {
                    return null;
                }
                return new ExcludedHeader(parts[0].trim(), Pattern.compile(parts[1].trim(), Pattern.CASE_INSENSITIVE));
            })
            .filter(Objects::nonNull)
            .toList();
    }
}
