package com.getyourguide.openapi.validation.api.metrics;

public class NoOpMetricsReporter implements MetricsReporter {
    @Override
    public void increment(String aspect, MetricTag... tags) {
        // Do nothing
    }
}
