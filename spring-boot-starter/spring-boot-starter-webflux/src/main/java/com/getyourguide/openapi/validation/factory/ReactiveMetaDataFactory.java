package com.getyourguide.openapi.validation.factory;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

public class ReactiveMetaDataFactory {

    public RequestMetaData buildRequestMetaData(ServerHttpRequest request) {
        return buildRequestMetaData(request, request.getHeaders());
    }

    public RequestMetaData buildRequestMetaData(ServerHttpRequest request, HttpHeaders headers) {
        return new RequestMetaData(request.getMethod().name(), request.getURI(), buildCaseInsensitiveHeaders(headers));
    }

    public ResponseMetaData buildResponseMetaData(ServerHttpResponse response) {
        var responseHeaders = response.getHeaders();
        var responseContentType = responseHeaders.getContentType() != null ? responseHeaders.getContentType().toString() : null;
        return new ResponseMetaData(
            response.getStatusCode() != null ? response.getStatusCode().value() : null,
            responseContentType,
            buildCaseInsensitiveHeaders(responseHeaders)
        );
    }

    private Map<String, String> buildCaseInsensitiveHeaders(HttpHeaders headers) {
        var map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((key, values) -> values.stream().findFirst().ifPresent(value -> map.put(key, value)));
        return map;
    }
}
