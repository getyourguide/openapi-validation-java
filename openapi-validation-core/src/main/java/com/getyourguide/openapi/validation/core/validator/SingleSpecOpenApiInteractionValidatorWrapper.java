package com.getyourguide.openapi.validation.core.validator;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SingleSpecOpenApiInteractionValidatorWrapper implements OpenApiInteractionValidatorWrapper {
    private final OpenApiInteractionValidator validator;

    @Override
    public ValidationReport validateRequest(SimpleRequest request) {
        return validator.validateRequest(request);
    }

    @Override
    public ValidationReport validateResponse(String path, Request.Method method, SimpleResponse response) {
        return validator.validateResponse(path, method, response);
    }
}
