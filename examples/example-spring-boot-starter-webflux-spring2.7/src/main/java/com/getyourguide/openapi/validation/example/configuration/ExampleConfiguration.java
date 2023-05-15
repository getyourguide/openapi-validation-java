package com.getyourguide.openapi.validation.example.configuration;

import com.getyourguide.openapi.validation.api.log.LoggerExtension;
import com.getyourguide.openapi.validation.example.logging.ExampleLoggerExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExampleConfiguration {

    @Bean
    public LoggerExtension loggerExtension() {
        return new ExampleLoggerExtension();
    }
}
