package com.getyourguide.openapi.validation.metrics.datadog.autoconfigure;

import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.metrics.datadog.StatsDClientMetricsReporter;
import com.timgroup.statsd.StatsDClient;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(LibraryAutoConfiguration.class)
@AllArgsConstructor
public class FallbackLibraryAutoConfiguration {

    @Bean
    @ConditionalOnBean(StatsDClient.class)
    @ConditionalOnMissingBean
    public MetricsReporter metricsReporterStatsDClient(StatsDClient statsDClient) {
        return new StatsDClientMetricsReporter(statsDClient);
    }

}
