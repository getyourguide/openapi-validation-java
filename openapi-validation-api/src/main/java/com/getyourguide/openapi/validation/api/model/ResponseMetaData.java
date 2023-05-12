package com.getyourguide.openapi.validation.api.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseMetaData {
    private final Integer statusCode;
    private final String contentType;
    private final Map<String, String> headers;
}
