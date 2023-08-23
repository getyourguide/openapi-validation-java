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
        metricsClient.increment(buildMetricName(".error"), createTagsForViolation(violation));
    }

    @Override
    public void reportStartup(
        boolean isValidationEnabled,
        double sampleRate,
        int validationReportThrottleWaitSeconds
    ) {
        metricsClient.increment(
            buildMetricName(".startup"),
            createTagsForStartup(isValidationEnabled, sampleRate, validationReportThrottleWaitSeconds)
        );
    }

    @Override
    public void reportValidationHeartbeat() {
        metricsClient.increment(buildMetricName(".validation_heartbeat"), createTagsForValidation());
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

    private MetricTag[] createTagsForStartup(
        boolean isValidationEnabled,
        double sampleRate,
        int validationReportThrottleWaitSeconds
    ) {
        var tags = new ArrayList<MetricTag>();

        tags.add(new MetricTag("validation_enabled", String.valueOf(isValidationEnabled)));
        tags.add(new MetricTag("sample_rate", String.valueOf(sampleRate)));
        tags.add(new MetricTag("throttling", String.valueOf(validationReportThrottleWaitSeconds)));
        addAdditionalTags(tags);

        return tags.toArray(MetricTag[]::new);
    }

    private MetricTag[] createTagsForValidation() {
        var tags = new ArrayList<MetricTag>();
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
