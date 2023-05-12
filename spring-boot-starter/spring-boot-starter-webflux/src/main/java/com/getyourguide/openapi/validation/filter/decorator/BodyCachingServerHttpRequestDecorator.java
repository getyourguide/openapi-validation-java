package com.getyourguide.openapi.validation.filter.decorator;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

public class BodyCachingServerHttpRequestDecorator extends ServerHttpRequestDecorator {
    private final TrafficSelector trafficSelector;
    private final RequestMetaData requestMetaData;

    @Getter
    private String cachedBody;

    public BodyCachingServerHttpRequestDecorator(
        ServerHttpRequest delegate,
        TrafficSelector trafficSelector,
        RequestMetaData requestMetaData
    ) {
        super(delegate);
        this.trafficSelector = trafficSelector;
        this.requestMetaData = requestMetaData;
    }

    @Override
    @NonNull
    public Flux<DataBuffer> getBody() {
        if (!trafficSelector.canRequestBeValidated(requestMetaData)) {
            return super.getBody();
        }

        return super.getBody().doOnNext(dataBuffer -> cachedBody = dataBuffer.toString(StandardCharsets.UTF_8));
    }
}
