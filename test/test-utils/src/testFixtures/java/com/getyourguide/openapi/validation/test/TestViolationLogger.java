package com.getyourguide.openapi.validation.test;

import com.getyourguide.openapi.validation.api.log.ViolationLogger;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class TestViolationLogger implements ViolationLogger {
    private final List<OpenApiViolation> violations = new ArrayList<>();

    @Override
    public void log(OpenApiViolation violation) {
        violations.add(violation);
    }

    public void clearViolations() {
        violations.clear();
    }
}
