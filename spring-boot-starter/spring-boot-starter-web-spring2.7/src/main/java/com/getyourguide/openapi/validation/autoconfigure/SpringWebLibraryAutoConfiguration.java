package com.getyourguide.openapi.validation.autoconfigure;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;

import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ContentCachingWrapperFactory;
import com.getyourguide.openapi.validation.factory.ServletMetaDataFactory;
import com.getyourguide.openapi.validation.filter.OpenApiValidationHttpFilter;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class SpringWebLibraryAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = Type.SERVLET)
    public ServletMetaDataFactory servletMetaDataFactory() {
        return new ServletMetaDataFactory();
    }

    @Bean
    @ConditionalOnWebApplication(type = Type.SERVLET)
    public ContentCachingWrapperFactory contentCachingWrapperFactory() {
        return new ContentCachingWrapperFactory();
    }

    @Bean
    @ConditionalOnWebApplication(type = Type.SERVLET)
    public OpenApiValidationHttpFilter openApiValidationHttpFilter(
        OpenApiRequestValidator validator,
        TrafficSelector trafficSelector,
        ServletMetaDataFactory metaDataFactory,
        ContentCachingWrapperFactory contentCachingWrapperFactory
    ) {
        return new OpenApiValidationHttpFilter(validator, trafficSelector, metaDataFactory, contentCachingWrapperFactory);
    }
}
