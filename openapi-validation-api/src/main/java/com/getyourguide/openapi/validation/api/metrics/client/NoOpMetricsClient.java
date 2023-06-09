package com.getyourguide.openapi.validation.api.metrics.client;

import com.getyourguide.openapi.validation.api.metrics.MetricTag;

public class NoOpMetricsClient implements MetricsClient {
    @Override
    public void increment(String aspect, MetricTag... tags) {
        // no-op
    }
}
