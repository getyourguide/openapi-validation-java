package com.getyourguide.openapi.validation.metrics.client;

import com.getyourguide.openapi.validation.api.metrics.MetricTag;
import com.getyourguide.openapi.validation.api.metrics.client.MetricsClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingMetricsClient implements MetricsClient {

    @Override
    public void increment(String aspect, MetricTag... tags) {
        log.info("Incrementing metric {} with tags {}", aspect, tags);
    }
}
