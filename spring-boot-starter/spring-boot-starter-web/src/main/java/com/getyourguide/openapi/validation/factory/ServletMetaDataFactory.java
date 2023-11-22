package com.getyourguide.openapi.validation.factory;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.TreeMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class ServletMetaDataFactory {

    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    public RequestMetaData buildRequestMetaData(HttpServletRequest request) {
        var uri = ServletUriComponentsBuilder.fromRequest(request).build().toUri();
        return new RequestMetaData(request.getMethod(), uri, getHeaders(request));
    }

    public ResponseMetaData buildResponseMetaData(HttpServletResponse response) {
        return buildResponseMetaData(response, null);
    }

    public ResponseMetaData buildResponseMetaData(HttpServletResponse response, Exception exception) {
        var status = response.getStatus() == 200 && exception != null ? 500 : response.getStatus();
        return new ResponseMetaData(status, response.getContentType(), getHeaders(response));
    }

    private static TreeMap<String, String> getHeaders(HttpServletRequest request) {
        var headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            var headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    private static TreeMap<String, String> getHeaders(HttpServletResponse response) {
        var headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        for (var headerName : response.getHeaderNames()) {
            headers.put(headerName, response.getHeader(headerName));
        }
        headers.put(HEADER_CONTENT_TYPE, response.getContentType()); // This one is not yet in the headers
        return headers;
    }
}
