package com.getyourguide.openapi.validation.metrics.datadog.autoconfigure;

import static com.getyourguide.openapi.validation.metrics.datadog.OpenApiValidationDataDogMetricsApplicationProperties.PROPERTY_STATSD_SERVICE_HOST;
import static com.getyourguide.openapi.validation.metrics.datadog.OpenApiValidationDataDogMetricsApplicationProperties.PROPERTY_STATSD_SERVICE_PORT;

import com.getyourguide.openapi.validation.api.metrics.client.MetricsClient;
import com.getyourguide.openapi.validation.metrics.datadog.OpenApiValidationDataDogMetricsApplicationProperties;
import com.getyourguide.openapi.validation.metrics.datadog.client.StatsDClientMetricsClient;
import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenApiValidationDataDogMetricsApplicationProperties.class)
@AllArgsConstructor
public class LibraryAutoConfiguration {
    private final OpenApiValidationDataDogMetricsApplicationProperties properties;

    @Bean
    @ConditionalOnClass(name = "com.timgroup.statsd.StatsDClient")
    @ConditionalOnProperty({PROPERTY_STATSD_SERVICE_HOST, PROPERTY_STATSD_SERVICE_PORT})
    public MetricsClient metricsClientCustomStatsDClient() {
        var statsDClient = new NonBlockingStatsDClientBuilder()
            .prefix(properties.getMetricPrefix())
            .hostname(properties.getStatsd().getService().getHost())
            .port(properties.getStatsd().getService().getPort())
            .build();
        return new StatsDClientMetricsClient(statsDClient);
    }
}
