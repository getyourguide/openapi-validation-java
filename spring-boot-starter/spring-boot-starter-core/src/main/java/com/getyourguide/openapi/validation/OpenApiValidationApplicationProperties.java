package com.getyourguide.openapi.validation;

import static com.getyourguide.openapi.validation.OpenApiValidationApplicationProperties.PROPERTY_PREFIX;

import com.getyourguide.openapi.validation.api.metrics.MetricTag;
import com.getyourguide.openapi.validation.util.CommaSeparatedStringsUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
    private Integer validationReportThrottleWaitSeconds;
    private String validationReportMetricName;
    private String validationReportMetricAdditionalTags;
    private String excludedPaths;
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
}
