package com.getyourguide.openapi.validation.autoconfigure;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;

import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ContentCachingWrapperFactory;
import com.getyourguide.openapi.validation.factory.ServletMetaDataFactory;
import com.getyourguide.openapi.validation.filter.OpenApiValidationFilter;
import com.getyourguide.openapi.validation.filter.OpenApiValidationInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
    public OpenApiValidationFilter openApiValidationFilter(
        OpenApiRequestValidator validator,
        TrafficSelector trafficSelector,
        ServletMetaDataFactory metaDataFactory,
        ContentCachingWrapperFactory contentCachingWrapperFactory
    ) {
        return new OpenApiValidationFilter(
            validator,
            trafficSelector,
            metaDataFactory,
            contentCachingWrapperFactory
        );
    }

    @Bean
    @ConditionalOnWebApplication(type = Type.SERVLET)
    public WebMvcConfigurer addOpenApiValidationInterceptor(
        OpenApiRequestValidator validator,
        TrafficSelector trafficSelector,
        ServletMetaDataFactory metaDataFactory,
        ContentCachingWrapperFactory contentCachingWrapperFactory
    ) {
        var interceptor = new OpenApiValidationInterceptor(
            validator,
            trafficSelector,
            metaDataFactory,
            contentCachingWrapperFactory
        );
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(final InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
