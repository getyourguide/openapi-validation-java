package com.getyourguide.openapi.validation.example.configuration;

import com.getyourguide.openapi.validation.api.log.LoggerExtension;
import com.getyourguide.openapi.validation.api.metrics.client.MetricsClient;
import com.getyourguide.openapi.validation.example.logging.ExampleLoggerExtension;
import com.getyourguide.openapi.validation.metrics.client.LoggingMetricsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExampleConfiguration {
    @Bean
    public MetricsClient metricsClient() {
        return new LoggingMetricsClient();
    }

    @Bean
    public LoggerExtension loggerExtension() {
        return new ExampleLoggerExtension();
    }
}
