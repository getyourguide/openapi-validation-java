package com.getyourguide.openapi.validation.api.selector;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;

public interface TrafficSelector {
    boolean shouldRequestBeValidated(RequestMetaData request);

    default boolean canRequestBeValidated(RequestMetaData request) {
        return true;
    }

    default boolean canResponseBeValidated(RequestMetaData request, ResponseMetaData response) {
        return true;
    }

    default boolean shouldFailOnRequestViolation(RequestMetaData request) {
        return false;
    }

    default boolean shouldFailOnResponseViolation(RequestMetaData request) {
        return false;
    }
}
