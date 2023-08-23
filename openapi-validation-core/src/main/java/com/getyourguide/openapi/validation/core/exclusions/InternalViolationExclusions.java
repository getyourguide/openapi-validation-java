package com.getyourguide.openapi.validation.core.exclusions;

import com.getyourguide.openapi.validation.api.exclusions.ViolationExclusions;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InternalViolationExclusions {
    private final ViolationExclusions customViolationExclusions;

    public boolean isExcluded(OpenApiViolation violation) {
        return falsePositive404(violation)
            || falsePositive400(violation)
            || customViolationExclusions.isExcluded(violation)
            // If it matches more than 1, then we don't want to log a validation error
            || violation.getMessage().matches(
            ".*\\[Path '[^']+'] Instance failed to match exactly one schema \\(matched [1-9][0-9]* out of \\d\\).*");
    }

    private boolean falsePositive404(OpenApiViolation violation) {
        return "validation.request.path.missing".equals(violation.getRule())
            && (
                violation.getDirection() == Direction.REQUEST
                || (violation.getDirection() == Direction.RESPONSE && violation.getResponseStatus().orElse(0) == 404)
            );
    }

    private boolean falsePositive400(OpenApiViolation violation) {
        return violation.getDirection() == Direction.REQUEST && violation.getResponseStatus().orElse(0) == 400;
    }
}
