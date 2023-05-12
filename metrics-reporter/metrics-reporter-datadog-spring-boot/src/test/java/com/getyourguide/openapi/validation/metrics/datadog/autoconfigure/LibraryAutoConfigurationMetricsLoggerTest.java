package com.getyourguide.openapi.validation.metrics.datadog.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.metrics.datadog.StatsDClientMetricsReporter;
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
    void withDataDogPropertiesShouldCreateStatsDClientMetricsReporter() {
        contextRunner
            .withPropertyValues(
                "openapi.validation.datadog.statsd.service.host=localhost",
                "openapi.validation.datadog.statsd.service.port=8125"
            )
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(MetricsReporter.class);
                assertThat(context.getBeansOfType(MetricsReporter.class))
                    .extractingByKey("metricsReporterCustomStatsDClient")
                    .isInstanceOf(StatsDClientMetricsReporter.class);
            });
    }

    @Test
    void withStatsDClientPropertiesAndExistingStatsDClientShouldCreateNewStatsDClientAndCreateStatsDClientMetricsReporter() {
        contextRunner
            .withPropertyValues(
                "openapi.validation.datadog.statsd.service.host=localhost",
                "openapi.validation.datadog.statsd.service.port=8125"
            )
            .withBean(StatsDClient.class, () -> mock(StatsDClient.class))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(MetricsReporter.class);
                assertThat(context.getBeansOfType(MetricsReporter.class))
                    .extractingByKey("metricsReporterCustomStatsDClient")
                    .isInstanceOf(StatsDClientMetricsReporter.class);
            });
    }

    @Test
    void noPropertiesWithExistingStatsDClientBean() {
        contextRunner
            .withPropertyValues()
            .withBean(StatsDClient.class, () -> mock(StatsDClient.class))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(MetricsReporter.class);
                assertThat(context.getBeansOfType(MetricsReporter.class))
                    .extractingByKey("metricsReporterStatsDClient")
                    .isInstanceOf(StatsDClientMetricsReporter.class);
            });
    }

    @Test
    void noPropertiesWithoutExistingStatsDClientBean() {
        contextRunner
            .run(context -> {
                assertThat(context)
                    .doesNotHaveBean(MetricsReporter.class);
            });
    }
}
