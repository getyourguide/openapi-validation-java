package com.getyourguide.openapi.validation.autoconfigure;

import com.getyourguide.openapi.validation.OpenApiValidationApplicationProperties;
import com.getyourguide.openapi.validation.api.log.LoggerExtension;
import com.getyourguide.openapi.validation.api.log.NoOpLoggerExtension;
import com.getyourguide.openapi.validation.api.log.ViolationLogger;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.metrics.NoOpMetricsReporter;
import com.getyourguide.openapi.validation.api.model.ValidatorConfiguration;
import com.getyourguide.openapi.validation.configuration.OpenApiValidationConfiguration;
import com.getyourguide.openapi.validation.core.DefaultViolationLogger;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.core.ValidationReportHandler;
import com.getyourguide.openapi.validation.core.throttle.RequestBasedValidationReportThrottler;
import com.getyourguide.openapi.validation.core.throttle.ValidationReportThrottler;
import com.getyourguide.openapi.validation.core.throttle.ValidationReportThrottlerNone;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenApiValidationApplicationProperties.class)
@AllArgsConstructor
public class LibraryAutoConfiguration {

    public static final String DEFAULT_METRIC_NAME = "openapi.validation.error";

    private final OpenApiValidationApplicationProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public ValidationReportThrottler requestBasedThrottleHelper() {
        if (properties.getValidationReportThrottleWaitSeconds() == null
            || properties.getValidationReportThrottleWaitSeconds() == 0) {
            return new ValidationReportThrottlerNone();
        }
        return new RequestBasedValidationReportThrottler(properties.getValidationReportThrottleWaitSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    public ViolationLogger violationLogger(Optional<LoggerExtension> loggerExtension) {
        return new DefaultViolationLogger(loggerExtension.orElseGet(NoOpLoggerExtension::new));
    }

    @Bean
    public ValidationReportHandler validationReportHandler(
        ValidationReportThrottler validationReportThrottler,
        ViolationLogger logger,
        Optional<MetricsReporter> metrics
    ) {
        var metricName =
            properties.getValidationReportMetricName() != null ? properties.getValidationReportMetricName() :
                DEFAULT_METRIC_NAME;
        return new ValidationReportHandler(
            validationReportThrottler,
            logger,
            metrics.orElseGet(NoOpMetricsReporter::new),
            ValidationReportHandler.Configuration.builder()
                .metricName(metricName)
                .metricAdditionalTags(properties.getValidationReportMetricAdditionalTags())
                .build()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ValidatorConfiguration validatorConfiguration() {
        return (new OpenApiValidationConfiguration()).buildValidatorConfiguration();
    }

    @Bean
    public OpenApiRequestValidator openApiRequestValidator(
        ValidationReportHandler validationReportHandler,
        ValidatorConfiguration validatorConfiguration
    ) {
        return new OpenApiRequestValidator(validationReportHandler, properties.getSpecificationFilePath(),
            validatorConfiguration);
    }
}
