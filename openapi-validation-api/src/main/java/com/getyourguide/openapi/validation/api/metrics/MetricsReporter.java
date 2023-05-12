package com.getyourguide.openapi.validation.api.metrics;

public interface MetricsReporter {
    void increment(String aspect, MetricTag... tags);
}
