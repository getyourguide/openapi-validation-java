package com.getyourguide.openapi.validation.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ServletMetaDataFactoryTest {
    private final ServletMetaDataFactory servletMetaDataFactory = new ServletMetaDataFactory();

    @Test
    public void testRequestMetaDataBuiltCorrectly() {
        var request = mockRequest("/v1/test", "ids=1,2,3");

        var requestMetaData = servletMetaDataFactory.buildRequestMetaData(request);

        assertEquals("/v1/test", requestMetaData.getUri().getPath());
        assertEquals("ids=1,2,3", requestMetaData.getUri().getQuery());
        assertEquals("application/json", requestMetaData.getContentType());
        assertEquals("GET", requestMetaData.getMethod());
        assertEquals(
            Map.of("Accept", "application/json", "Accept-Language", "de-DE", "Content-Type", "application/json"),
            requestMetaData.getHeaders());
    }

    @Test
    public void testRequestMetaDataBuiltCorrectlyWithAlreadyEncodedQueryString() {
        // Note: This seems to be the case where some clients send comma separated list with `,` being encoded
        var request = mockRequest("/v1/test", "ids=1%2C2%2C3");

        var requestMetaData = servletMetaDataFactory.buildRequestMetaData(request);

        assertEquals("ids=1,2,3", requestMetaData.getUri().getQuery());
    }

    private static HttpServletRequest mockRequest(String path, String queryString) {
        HttpServletRequest request = mock();
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("api.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn(path);
        when(request.getQueryString()).thenReturn(queryString);
        when(request.getHeaderNames())
            .thenReturn(Collections.enumeration(List.of("Accept", "Accept-Language", "Content-Type")));
        when(request.getHeader("Accept")).thenReturn("application/json");
        when(request.getHeader("Accept-Language")).thenReturn("de-DE");
        when(request.getHeader("Content-Type")).thenReturn("application/json");
        return request;
    }
}
