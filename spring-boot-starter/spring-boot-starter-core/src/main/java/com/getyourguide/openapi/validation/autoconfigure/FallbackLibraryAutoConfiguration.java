package com.getyourguide.openapi.validation.autoconfigure;

import com.getyourguide.openapi.validation.OpenApiValidationApplicationProperties;
import com.getyourguide.openapi.validation.api.selector.DefaultTrafficSelector;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(LibraryAutoConfiguration.class)
@EnableConfigurationProperties(OpenApiValidationApplicationProperties.class)
@AllArgsConstructor
public class FallbackLibraryAutoConfiguration {

    private final OpenApiValidationApplicationProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public TrafficSelector defaultTrafficSelector() {
        return new DefaultTrafficSelector(properties.getSampleRate(), properties.getExcludedPathsAsSet());
    }

}
