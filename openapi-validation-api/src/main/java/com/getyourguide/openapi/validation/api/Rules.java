package com.getyourguide.openapi.validation.api;

public class Rules {
    public static class Request {
        public static final String PATH_MISSING = "validation.request.path.missing";
        public static final String OPERATION_NOT_ALLOWED = "validation.request.operation.notAllowed";
        public static final String BODY_SCHEMA_ONE_OF = "validation.request.body.schema.oneOf";
    }

    public static class Response {
        public static final String BODY_SCHEMA_ONE_OF = "validation.response.body.schema.oneOf";
    }
}
