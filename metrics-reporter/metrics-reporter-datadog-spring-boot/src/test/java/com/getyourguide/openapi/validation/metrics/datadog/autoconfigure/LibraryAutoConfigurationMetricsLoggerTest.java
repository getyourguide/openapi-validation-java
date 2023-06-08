package com.getyourguide.openapi.validation.metrics.datadog.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.getyourguide.openapi.validation.api.metrics.client.MetricsClient;
import com.getyourguide.openapi.validation.metrics.datadog.client.StatsDClientMetricsClient;
import com.timgroup.statsd.StatsDClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LibraryAutoConfigurationMetricsLoggerTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConditionEvaluationReportLoggingListener()) // to print out conditional config report to log
            .withUserConfiguration(LibraryAutoConfiguration.class)
            .withUserConfiguration(FallbackLibraryAutoConfiguration.class);
    }

    @Test
    void withDataDogPropertiesShouldCreateStatsDClientMetricsClient() {
        contextRunner
            .withPropertyValues(
                "openapi.validation.datadog.statsd.service.host=localhost",
                "openapi.validation.datadog.statsd.service.port=8125"
            )
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(MetricsClient.class);
                assertThat(context.getBeansOfType(MetricsClient.class))
                    .extractingByKey("metricsClientCustomStatsDClient")
                    .isInstanceOf(StatsDClientMetricsClient.class);
            });
    }

    @Test
    void withStatsDClientPropertiesAndExistingStatsDClientShouldCreateNewStatsDClientAndCreateStatsDClientMetricsClient() {
        contextRunner
            .withPropertyValues(
                "openapi.validation.datadog.statsd.service.host=localhost",
                "openapi.validation.datadog.statsd.service.port=8125"
            )
            .withBean(StatsDClient.class, () -> mock(StatsDClient.class))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(MetricsClient.class);
                assertThat(context.getBeansOfType(MetricsClient.class))
                    .extractingByKey("metricsClientCustomStatsDClient")
                    .isInstanceOf(StatsDClientMetricsClient.class);
            });
    }

    @Test
    void noPropertiesWithExistingStatsDClientBean() {
        contextRunner
            .withPropertyValues()
            .withBean(StatsDClient.class, () -> mock(StatsDClient.class))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(MetricsClient.class);
                assertThat(context.getBeansOfType(MetricsClient.class))
                    .extractingByKey("metricsClientStatsDClient")
                    .isInstanceOf(StatsDClientMetricsClient.class);
            });
    }

    @Test
    void noPropertiesWithoutExistingStatsDClientBean() {
        contextRunner
            .run(context -> {
                assertThat(context)
                    .doesNotHaveBean(MetricsClient.class);
            });
    }
}
