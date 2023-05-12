package com.getyourguide.openapi.validation.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.getyourguide.openapi.validation.filter.OpenApiValidationHttpFilter;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockServletContext;

class SpringWebLibraryAutoConfigurationApplicationContextTest {

    private ConfigurableApplicationContext context;

    @AfterEach
    void tearDown() {
        Optional.ofNullable(context)
            .ifPresent(ConfigurableApplicationContext::close);
    }

    @Test
    void webApplicationWithServletContext() {
        context = servletWebApplicationContext();

        assertThat(context.getBeansOfType(OpenApiValidationHttpFilter.class)).size().isEqualTo(1);
    }

    @Test
    void webApplicationWithReactiveContext() {
        context = reactiveWebApplicationContext();

        assertThat(context.getBeansOfType(OpenApiValidationHttpFilter.class)).size().isEqualTo(0);
    }

    @Test
    void nonWebApplicationContextShouldHaveNoFilterBeans() {
        context = nonWebApplicationContext();

        assertThat(context.getBeansOfType(OpenApiValidationHttpFilter.class)).size().isEqualTo(0);
    }

    private AnnotationConfigServletWebApplicationContext servletWebApplicationContext() {
        var servletContext = new AnnotationConfigServletWebApplicationContext();

        servletContext.register(SpringWebLibraryAutoConfiguration.class, LibraryAutoConfiguration.class, FallbackLibraryAutoConfiguration.class);
        servletContext.setServletContext(new MockServletContext());
        servletContext.refresh();

        return servletContext;
    }

    private AnnotationConfigReactiveWebApplicationContext reactiveWebApplicationContext() {
        var reactiveContext = new AnnotationConfigReactiveWebApplicationContext();

        reactiveContext.register(SpringWebLibraryAutoConfiguration.class, LibraryAutoConfiguration.class, FallbackLibraryAutoConfiguration.class);
        reactiveContext.refresh();

        return reactiveContext;
    }

    private AnnotationConfigApplicationContext nonWebApplicationContext() {
        var reactiveContext = new AnnotationConfigApplicationContext();

        reactiveContext.register(SpringWebLibraryAutoConfiguration.class, LibraryAutoConfiguration.class, FallbackLibraryAutoConfiguration.class);
        reactiveContext.refresh();

        return reactiveContext;
    }
}
