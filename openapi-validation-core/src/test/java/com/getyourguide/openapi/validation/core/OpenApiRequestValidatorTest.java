package com.getyourguide.openapi.validation.core;

import static org.mockito.Mockito.mock;

import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.ValidatorConfiguration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class OpenApiRequestValidatorTest {

    private ThreadPoolExecutor threadPoolExecutor;
    private ValidationReportHandler validationReportHandler;
    private MetricsReporter metricsReporter;

    private OpenApiRequestValidator openApiRequestValidator;

    @BeforeEach
    public void setup() {
        threadPoolExecutor = mock();
        validationReportHandler = mock();
        metricsReporter = mock();

        openApiRequestValidator = new OpenApiRequestValidator(
            threadPoolExecutor,
            validationReportHandler,
            metricsReporter,
            "",
            new ValidatorConfiguration(null, null, null)
        );
    }

    @Test
    public void testWhenThreadPoolExecutorRejectsExecutionThenItShouldNotThrow() {
        Mockito.doThrow(new RejectedExecutionException()).when(threadPoolExecutor).execute(Mockito.any());

        openApiRequestValidator.validateRequestObjectAsync(mock(), null);
    }
}
