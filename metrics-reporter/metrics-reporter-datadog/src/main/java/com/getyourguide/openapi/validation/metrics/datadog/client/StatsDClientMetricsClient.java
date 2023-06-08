package com.getyourguide.openapi.validation.metrics.datadog.client;

import com.getyourguide.openapi.validation.api.metrics.MetricTag;
import com.getyourguide.openapi.validation.api.metrics.client.MetricsClient;
import com.timgroup.statsd.StatsDClient;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StatsDClientMetricsClient implements MetricsClient {
    private final StatsDClient statsDClient;

    @Override
    public void increment(String aspect, MetricTag... tags) {
        statsDClient.increment(aspect, mapTags(tags));
    }

    private static String[] mapTags(MetricTag[] tags) {
        return Optional.of(tags)
            .map(nonNullTags ->
                Arrays.stream(nonNullTags).map(tag -> tag.getKey() + ":" + tag.getValue()).toArray(String[]::new)
            )
            .orElse(new String[0]);
    }
}
