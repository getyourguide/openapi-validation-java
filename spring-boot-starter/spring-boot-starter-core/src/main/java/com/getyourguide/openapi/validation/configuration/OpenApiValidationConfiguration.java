package com.getyourguide.openapi.validation.configuration;

import com.getyourguide.openapi.validation.api.log.LogLevel;
import com.getyourguide.openapi.validation.api.model.ValidatorConfiguration;
import com.getyourguide.openapi.validation.api.model.ValidatorConfigurationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiValidationConfiguration {
    @Bean
    public ValidatorConfiguration buildValidatorConfiguration() {
        return new ValidatorConfigurationBuilder()
            // .levelResolverLevel("validation.request.body.schema.additionalProperties", LogLevel.IGNORE)
            .levelResolverDefaultLevel(LogLevel.INFO)
            .build();
    }
}
