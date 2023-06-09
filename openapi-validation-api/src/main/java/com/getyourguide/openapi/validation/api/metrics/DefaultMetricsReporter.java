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
        String violationMetricName = buildMetricName(".error");
        metricsClient.increment(violationMetricName, createTagsForViolation(violation));
    }

    @Override
    public void reportStartup(boolean isValidationEnabled) {
        String startupMetricName = buildMetricName(".startup");
        metricsClient.increment(startupMetricName, createTagsForStartup(isValidationEnabled));
    }

    private String buildMetricName(String suffix) {
        return configuration.getMetricName() + suffix;
    }

    private MetricTag[] createTagsForViolation(OpenApiViolation violation) {
        var tags = new ArrayList<MetricTag>();

        tags.add(new MetricTag("type", violation.getDirection().toString().toLowerCase()));
        tags.add(new MetricTag("method", violation.getRequestMetaData().getMethod().toLowerCase()));
        violation.getNormalizedPath().ifPresent(path -> tags.add(new MetricTag("path", path)));
        violation.getResponseStatus()
            .ifPresent(responseStatus -> tags.add(new MetricTag("status", responseStatus.toString())));

        addAdditionalTags(tags);

        return tags.toArray(MetricTag[]::new);
    }

    private MetricTag[] createTagsForStartup(boolean isValidationEnabled) {
        var tags = new ArrayList<MetricTag>();

        tags.add(new MetricTag("validation_enabled", String.valueOf(isValidationEnabled)));
        addAdditionalTags(tags);

        return tags.toArray(MetricTag[]::new);
    }

    private void addAdditionalTags(ArrayList<MetricTag> tags) {
        if (configuration.getMetricAdditionalTags() != null) {
            tags.addAll(configuration.getMetricAdditionalTags());
        }
    }

    @Builder
    @Getter
    public static class Configuration {
        private final String metricName;
        private final List<MetricTag> metricAdditionalTags;
    }
}
