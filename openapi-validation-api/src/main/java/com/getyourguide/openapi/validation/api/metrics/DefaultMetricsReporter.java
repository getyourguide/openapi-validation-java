package com.getyourguide.openapi.validation.api.metrics;

import com.getyourguide.openapi.validation.api.metrics.client.MetricsClient;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
public class DefaultMetricsReporter implements MetricsReporter {

    private final MetricsClient metricsClient;
    private final Configuration configuration;

    @Override
    public void reportViolation(OpenApiViolation violation) {
        var violationMetricName = configuration.getMetricName() + ".error";
        metricsClient.increment(violationMetricName, createTagsForViolation(violation));
    }

    private MetricTag[] createTagsForViolation(OpenApiViolation violation) {
        var tags = new ArrayList<MetricTag>();

        tags.add(new MetricTag("type", violation.getDirection().toString().toLowerCase()));
        tags.add(new MetricTag("method", violation.getRequestMetaData().getMethod().toLowerCase()));
        violation.getNormalizedPath().ifPresent(path -> tags.add(new MetricTag("path", path)));
        violation.getResponseStatus()
            .ifPresent(responseStatus -> tags.add(new MetricTag("status", responseStatus.toString())));

        if (configuration.getMetricAdditionalTags() != null) {
            tags.addAll(configuration.getMetricAdditionalTags());
        }

        return tags.toArray(MetricTag[]::new);
    }

    @Builder
    @Getter
    public static class Configuration {
        private final String metricName;
        private final List<MetricTag> metricAdditionalTags;
    }
}
