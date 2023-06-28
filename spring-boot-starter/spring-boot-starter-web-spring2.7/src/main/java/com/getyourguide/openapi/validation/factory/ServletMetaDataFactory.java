package com.getyourguide.openapi.validation.factory;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class ServletMetaDataFactory {

    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    public RequestMetaData buildRequestMetaData(HttpServletRequest request) {
        // Params needed to avoid problem here: https://github.com/spring-projects/spring-framework/issues/24043#issuecomment-556024162
        var params = new LinkedMultiValueMap<String, String>();
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            params.addAll(e.getKey(), Arrays.asList(e.getValue()));
        }
        var uri = ServletUriComponentsBuilder.fromRequest(request).replaceQueryParams(params).build().toUri();
        return new RequestMetaData(request.getMethod(), uri, getHeaders(request));
    }

    public ResponseMetaData buildResponseMetaData(HttpServletResponse response) {
        return new ResponseMetaData(response.getStatus(), response.getContentType(), getHeaders(response));
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
