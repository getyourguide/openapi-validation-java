package com.getyourguide.openapi.validation.example.configuration;

import com.getyourguide.openapi.validation.api.log.LogLevel;
import com.getyourguide.openapi.validation.api.log.LoggerExtension;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.ValidatorConfiguration;
import com.getyourguide.openapi.validation.api.model.ValidatorConfigurationBuilder;
import com.getyourguide.openapi.validation.example.logging.ExampleLoggerExtension;
import com.getyourguide.openapi.validation.metrics.LoggingMetricsReporter;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExampleConfiguration {
    @Bean
    public MetricsReporter metricsReporter() {
        return new LoggingMetricsReporter();
    }

    @Bean
    public LoggerExtension loggerExtension() {
        return new ExampleLoggerExtension();
    }
}
