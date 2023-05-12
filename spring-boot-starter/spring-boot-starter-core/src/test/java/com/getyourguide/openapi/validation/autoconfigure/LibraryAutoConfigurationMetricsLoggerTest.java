package com.getyourguide.openapi.validation.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
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
    void noPropertiesWithoutExistingStatsDClientBeanShouldNotReturnAnyDefaultMetricsReporter() {
        // Note: We don't want to return any MetricsReporter as this can conflict with other MetricsReporter beans
        //       like the one provided by metrics-reporter-datadog-spring-boot.
        contextRunner
            .run(context -> {
                assertThat(context)
                    .doesNotHaveBean(MetricsReporter.class);
            });
    }
}
