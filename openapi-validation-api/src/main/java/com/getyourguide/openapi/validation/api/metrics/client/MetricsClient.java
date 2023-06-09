package com.getyourguide.openapi.validation.api.metrics.client;

import com.getyourguide.openapi.validation.api.metrics.MetricTag;

public interface MetricsClient {
    void increment(String aspect, MetricTag... tags);
}
