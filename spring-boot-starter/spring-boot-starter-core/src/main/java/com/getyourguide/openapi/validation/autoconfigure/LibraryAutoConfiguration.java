package com.getyourguide.openapi.validation.autoconfigure;

import com.getyourguide.openapi.validation.OpenApiValidationApplicationProperties;
import com.getyourguide.openapi.validation.api.exclusions.NoViolationExclusions;
import com.getyourguide.openapi.validation.api.exclusions.ViolationExclusions;
import com.getyourguide.openapi.validation.api.log.LogLevel;
import com.getyourguide.openapi.validation.api.log.LoggerExtension;
import com.getyourguide.openapi.validation.api.log.NoOpLoggerExtension;
import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.log.ViolationLogger;
import com.getyourguide.openapi.validation.api.metrics.MetricTagProvider;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.metrics.NullMetricTagProvider;
import com.getyourguide.openapi.validation.api.metrics.client.MetricsClient;
import com.getyourguide.openapi.validation.api.metrics.client.NoOpMetricsClient;
import com.getyourguide.openapi.validation.api.model.ValidatorConfiguration;
import com.getyourguide.openapi.validation.api.model.ValidatorConfigurationBuilder;
import com.getyourguide.openapi.validation.core.DefaultViolationLogger;
import com.getyourguide.openapi.validation.core.OpenApiInteractionValidatorFactory;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.core.exclusions.InternalViolationExclusions;
import com.getyourguide.openapi.validation.core.executor.VirtualThreadLimitedExecutor;
import com.getyourguide.openapi.validation.core.log.DefaultOpenApiViolationHandler;
import com.getyourguide.openapi.validation.core.log.ExclusionsOpenApiViolationHandler;
import com.getyourguide.openapi.validation.core.log.ThrottlingOpenApiViolationHandler;
import com.getyourguide.openapi.validation.core.mapper.ValidationReportToOpenApiViolationsMapper;
import com.getyourguide.openapi.validation.core.metrics.DefaultMetricsReporter;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenApiValidationApplicationProperties.class)
@AllArgsConstructor
public class LibraryAutoConfiguration {

    public static final String DEFAULT_METRIC_NAME = "openapi.validation";

    private final OpenApiValidationApplicationProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public ViolationLogger violationLogger(Optional<LoggerExtension> loggerExtension) {
        return new DefaultViolationLogger(loggerExtension.orElseGet(NoOpLoggerExtension::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricsReporter metricsReporter(
        Optional<MetricsClient> metricsClient,
        Optional<MetricTagProvider> metricTagProvider
    ) {
        var metricName = properties.getValidationReportMetricName() != null
            ? properties.getValidationReportMetricName()
            : DEFAULT_METRIC_NAME;
        return new DefaultMetricsReporter(
            metricsClient.orElseGet(NoOpMetricsClient::new),
            metricTagProvider.orElseGet(NullMetricTagProvider::new),
            DefaultMetricsReporter.Configuration.builder()
                .metricName(metricName)
                .metricAdditionalTags(properties.getValidationReportMetricAdditionalTags())
                .build()
        );
    }

    @Bean
    public OpenApiViolationHandler openApiViolationHandler(
        ViolationLogger logger,
        MetricsReporter metricsReporter,
        Optional<ViolationExclusions> violationExclusions
    ) {
        OpenApiViolationHandler handler = new DefaultOpenApiViolationHandler(logger, metricsReporter);

        if (properties.getValidationReportThrottleWaitSeconds() != 0) {
            handler =
                new ThrottlingOpenApiViolationHandler(handler, properties.getValidationReportThrottleWaitSeconds());
        }

        var exclusions = new InternalViolationExclusions(violationExclusions.orElseGet(NoViolationExclusions::new));
        handler = new ExclusionsOpenApiViolationHandler(handler, exclusions);

        return handler;
    }

    @Bean
    @ConditionalOnMissingBean
    public ValidatorConfiguration validatorConfiguration() {
        return new ValidatorConfigurationBuilder()
            // .levelResolverLevel("validation.request.body.schema.additionalProperties", LogLevel.IGNORE)
            .levelResolverLevel("validation.request.parameter.query.unexpected", LogLevel.IGNORE)
            .levelResolverDefaultLevel(
                properties.getViolationLogLevel() != null ? properties.getViolationLogLevel() : LogLevel.INFO
            )
            .build();
    }

    @Bean
    public OpenApiRequestValidator openApiRequestValidator(
        MetricsReporter metricsReporter,
        ValidatorConfiguration validatorConfiguration
    ) {
        var threadPoolExecutor = createThreadPoolExecutor();

        return new OpenApiRequestValidator(
            threadPoolExecutor,
            metricsReporter,
            new OpenApiInteractionValidatorFactory()
                .build(properties.getSpecificationFilePath(), validatorConfiguration),
            new ValidationReportToOpenApiViolationsMapper(),
            properties.toOpenApiRequestValidationConfiguration()
        );
    }

    private Executor createThreadPoolExecutor() {
        if (properties.isEnableVirtualThreads() && VirtualThreadLimitedExecutor.isSupported()) {
            return new VirtualThreadLimitedExecutor();
        }

        // Fallback to ThreadPoolExecutor with regular threads
        return new ThreadPoolExecutor(
            2,
            2,
            1000L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(10),
            new ThreadPoolExecutor.DiscardPolicy()
        );
    }
}
