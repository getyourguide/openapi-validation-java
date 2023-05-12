package com.getyourguide.openapi.validation.filter.decorator;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.factory.ReactiveMetaDataFactory;
import lombok.AllArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

@AllArgsConstructor
public class DecoratorBuilder {
    private final TrafficSelector trafficSelector;
    private final ReactiveMetaDataFactory metaDataFactory;

    public BodyCachingServerHttpRequestDecorator buildBodyCachingServerHttpRequestDecorator(ServerHttpRequest request, RequestMetaData requestMetaData) {
        return new BodyCachingServerHttpRequestDecorator(request, trafficSelector, requestMetaData);
    }

    public BodyCachingServerHttpResponseDecorator buildBodyCachingServerHttpResponseDecorator(ServerHttpResponse response, RequestMetaData requestMetaData) {
        return new BodyCachingServerHttpResponseDecorator(response, trafficSelector, metaDataFactory, requestMetaData);
    }
}
