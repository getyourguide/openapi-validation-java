package com.getyourguide.openapi.validation.filter.decorator;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.factory.ReactiveMetaDataFactory;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

public class BodyCachingServerHttpResponseDecorator extends ServerHttpResponseDecorator {

    private final TrafficSelector trafficSelector;
    private final ReactiveMetaDataFactory metaDataFactory;
    private final RequestMetaData requestMetaData;

    @Getter
    private String cachedBody;

    public BodyCachingServerHttpResponseDecorator(
        ServerHttpResponse delegate,
        TrafficSelector trafficSelector,
        ReactiveMetaDataFactory metaDataFactory,
        RequestMetaData requestMetaData
    ) {
        super(delegate);
        this.trafficSelector = trafficSelector;
        this.metaDataFactory = metaDataFactory;
        this.requestMetaData = requestMetaData;
    }

    @Override
    @NonNull
    public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
        var responseMetaData = metaDataFactory.buildResponseMetaData(this);
        if (!trafficSelector.canResponseBeValidated(requestMetaData, responseMetaData)) {
            return super.writeWith(body);
        }

        var buffer = Mono.from(body).doOnNext(dataBuffer -> cachedBody = dataBuffer.toString(StandardCharsets.UTF_8));
        return super.writeWith(buffer);
    }
}
