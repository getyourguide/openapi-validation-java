package com.getyourguide.openapi.validation.metrics.datadog;

import static com.getyourguide.openapi.validation.metrics.datadog.OpenApiValidationDataDogMetricsApplicationProperties.PROPERTY_PREFIX;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = PROPERTY_PREFIX)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OpenApiValidationDataDogMetricsApplicationProperties {
    public static final String PROPERTY_PREFIX = "openapi.validation.datadog";
    public static final String PROPERTY_STATSD_SERVICE_HOST = PROPERTY_PREFIX + ".statsd.service.host";
    public static final String PROPERTY_STATSD_SERVICE_PORT = PROPERTY_PREFIX + ".statsd.service.port";
    public static final String PROPERTY_METRIC_PREFIX = PROPERTY_PREFIX + ".metric-prefix";

    private Statsd statsd;
    private String metricPrefix;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Statsd {
        private Service service;

        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        @Setter
        public static class Service {
            private String host;
            private Integer port;
        }
    }
}
