package com.getyourguide.openapi.validation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.atlassian.oai.validator.model.SimpleRequest;
import com.getyourguide.openapi.validation.api.metrics.MetricsReporter;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.core.validator.OpenApiInteractionValidatorWrapper;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class OpenApiRequestValidatorTest {

    private ThreadPoolExecutor threadPoolExecutor;
    private OpenApiInteractionValidatorWrapper validator;

    private OpenApiRequestValidator openApiRequestValidator;

    @BeforeEach
    public void setup() {
        threadPoolExecutor = mock();
        validator = mock();
        ValidationReportHandler validationReportHandler = mock();
        MetricsReporter metricsReporter = mock();

        openApiRequestValidator = new OpenApiRequestValidator(
            threadPoolExecutor,
            validationReportHandler,
            metricsReporter,
            validator
        );
    }

    @Test
    public void testWhenThreadPoolExecutorRejectsExecutionThenItShouldNotThrow() {
        Mockito.doThrow(new RejectedExecutionException()).when(threadPoolExecutor).execute(any());

        openApiRequestValidator.validateRequestObjectAsync(mock(), null);
    }

    @Test
    public void testWhenEncodedQueryParamIsPassedThenValidationShouldHappenWithQueryParamDecoded() {
        var uri = URI.create("https://api.example.com?ids=1%2C2%2C3");
        var request = new RequestMetaData("GET", uri, new HashMap<>());

        openApiRequestValidator.validateRequestObject(request, null);

        var simpleRequestArgumentCaptor = ArgumentCaptor.forClass(SimpleRequest.class);
        verify(validator).validateRequest(simpleRequestArgumentCaptor.capture());
        var ids = simpleRequestArgumentCaptor.getValue().getQueryParameterValues("ids").iterator().next();
        assertEquals("1,2,3", ids);
    }
}
