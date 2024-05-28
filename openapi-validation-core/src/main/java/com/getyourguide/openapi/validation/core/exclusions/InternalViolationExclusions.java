package com.getyourguide.openapi.validation.core.exclusions;

import com.getyourguide.openapi.validation.api.Rules;
import com.getyourguide.openapi.validation.api.exclusions.ViolationExclusions;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InternalViolationExclusions {

    private final ViolationExclusions customViolationExclusions;

    public boolean isExcluded(OpenApiViolation violation) {
        return falsePositive404(violation)
            || falsePositive405(violation)
            || falsePositive406(violation)
            || falsePositiveRequestWith4xxResponse(violation)
            || customViolationExclusions.isExcluded(violation)
            || oneOfMatchesMoreThanOneSchema(violation);
    }

    private static boolean oneOfMatchesMoreThanOneSchema(OpenApiViolation violation) {
        return (
            Rules.Response.BODY_SCHEMA_ONE_OF.equals(violation.getRule())
                || Rules.Request.BODY_SCHEMA_ONE_OF.equals(violation.getRule())
            )
            && violation.getMessage()
            .matches(".*Instance failed to match exactly one schema \\(matched [1-9][0-9]* out of \\d+\\).*");
    }

    private boolean falsePositive404(OpenApiViolation violation) {
        return
            (
                Rules.Request.PATH_MISSING.equals(violation.getRule())
                || Rules.Request.OPERATION_NOT_ALLOWED.equals(violation.getRule())
            ) && (
                (violation.getDirection() == Direction.REQUEST && violation.getResponseStatus().isEmpty())
                || violation.getResponseStatus().orElse(0) == 404
            );
    }

    private boolean falsePositiveRequestWith4xxResponse(OpenApiViolation violation) {
        return violation.getDirection() == Direction.REQUEST
            && violation.getResponseStatus().orElse(0) >= 400
            && violation.getResponseStatus().orElse(0) < 500;
    }

    private boolean falsePositive405(OpenApiViolation violation) {
        return violation.getResponseStatus().orElse(0) == 405
            && Rules.Request.OPERATION_NOT_ALLOWED.equals(violation.getRule());
    }

    private boolean falsePositive406(OpenApiViolation violation) {
        return violation.getResponseStatus().orElse(0) == 406
            && Rules.Response.STATUS_UNKNOWN.equals(violation.getRule());
    }
}
