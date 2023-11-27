package com.getyourguide.openapi.validation.autoconfigure;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;

import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ReactiveMetaDataFactory;
import com.getyourguide.openapi.validation.filter.OpenApiValidationWebFilter;
import com.getyourguide.openapi.validation.filter.decorator.DecoratorBuilder;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class SpringWebFluxLibraryAutoConfiguration {
    @Bean
    @ConditionalOnWebApplication(type = Type.REACTIVE)
    public DecoratorBuilder decoratorBuilder(TrafficSelector trafficSelector, ReactiveMetaDataFactory metaDataFactory) {
        return new DecoratorBuilder(trafficSelector, metaDataFactory);
    }

    @Bean
    @ConditionalOnWebApplication(type = Type.REACTIVE)
    public ReactiveMetaDataFactory reactiveMetaDataFactory() {
        return new ReactiveMetaDataFactory();
    }

    @Bean
    @ConditionalOnWebApplication(type = Type.REACTIVE)
    public OpenApiValidationWebFilter openApiValidationWebFilter(
        OpenApiRequestValidator validator,
        TrafficSelector trafficSelector,
        ReactiveMetaDataFactory metaDataFactory,
        OpenApiViolationHandler openApiViolationHandler,
        DecoratorBuilder decoratorBuilder
    ) {
        return new OpenApiValidationWebFilter(
            validator,
            trafficSelector,
            metaDataFactory,
            decoratorBuilder,
            openApiViolationHandler
        );
    }
}
