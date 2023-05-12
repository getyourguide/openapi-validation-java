package com.getyourguide.openapi.validation.api.model;

import java.net.URI;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RequestMetaData {
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    private final String method;
    private final URI uri;
    private final Map<String, String> headers;

    public String getContentType() {
        return headers.get(HEADER_CONTENT_TYPE);
    }
}
