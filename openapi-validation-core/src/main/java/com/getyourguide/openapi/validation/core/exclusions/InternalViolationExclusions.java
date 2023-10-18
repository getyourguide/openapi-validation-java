package com.getyourguide.openapi.validation.core.exclusions;

import com.getyourguide.openapi.validation.api.exclusions.ViolationExclusions;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InternalViolationExclusions {
    private static final String RULE_REQUEST_OPERATION_NOT_ALLOWED = "validation.request.operation.notAllowed";
    private static final String RULE_REQUEST_BODY_SCHEMA_ONE_OF = "validation.request.body.schema.oneOf";
    private static final String RULE_REQUEST_PATH_MISSING = "validation.request.path.missing";
    private static final String RULE_RESPONSE_BODY_SCHEMA_ONE_OF = "validation.response.body.schema.oneOf";

    private final ViolationExclusions customViolationExclusions;

    public boolean isExcluded(OpenApiViolation violation) {
        return falsePositive404(violation)
            || falsePositive400(violation)
            || falsePositive405(violation)
            || customViolationExclusions.isExcluded(violation)
            || oneOfMatchesMoreThanOneSchema(violation);
    }

    private static boolean oneOfMatchesMoreThanOneSchema(OpenApiViolation violation) {
        return (
            RULE_RESPONSE_BODY_SCHEMA_ONE_OF.equals(violation.getRule())
                || RULE_REQUEST_BODY_SCHEMA_ONE_OF.equals(violation.getRule())
            )
            && violation.getMessage()
            .matches(".*Instance failed to match exactly one schema \\(matched [1-9][0-9]* out of \\d+\\).*");
    }

    private boolean falsePositive404(OpenApiViolation violation) {
        return RULE_REQUEST_PATH_MISSING.equals(violation.getRule())
            && (
                violation.getDirection() == Direction.REQUEST
                || (violation.getDirection() == Direction.RESPONSE && violation.getResponseStatus().orElse(0) == 404)
            );
    }

    private boolean falsePositive400(OpenApiViolation violation) {
        return violation.getDirection() == Direction.REQUEST && violation.getResponseStatus().orElse(0) == 400;
    }

    private boolean falsePositive405(OpenApiViolation violation) {
        return violation.getResponseStatus().orElse(0) == 405
            && RULE_REQUEST_OPERATION_NOT_ALLOWED.equals(violation.getRule());
    }
}
