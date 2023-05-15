package com.getyourguide.openapi.validation.core.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;

public interface OpenApiInteractionValidatorWrapper {
    ValidationReport validateRequest(SimpleRequest request);

    ValidationReport validateResponse(String path, Request.Method method, SimpleResponse response);
}
