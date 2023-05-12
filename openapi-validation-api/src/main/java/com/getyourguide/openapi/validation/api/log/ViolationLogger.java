package com.getyourguide.openapi.validation.api.log;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;

public interface ViolationLogger {
    void log(OpenApiViolation violation);
}
