package com.getyourguide.openapi.validation.api.log;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;

public interface OpenApiViolationHandler {
    void onOpenApiViolation(OpenApiViolation violation);
}
