package com.getyourguide.openapi.validation.metrics;

import com.getyourguide.openapi.validation.api.metrics.MetricTag;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingMetricsReporter implements MetricsReporter {

    @Override
    public void increment(String aspect, MetricTag... tags) {
        log.info("Incrementing metric {} with tags {}", aspect, tags);
    }
}
