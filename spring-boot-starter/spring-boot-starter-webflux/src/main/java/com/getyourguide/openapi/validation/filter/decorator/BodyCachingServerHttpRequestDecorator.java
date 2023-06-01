package com.getyourguide.openapi.validation.filter.decorator;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

public class BodyCachingServerHttpRequestDecorator extends ServerHttpRequestDecorator {
    private final TrafficSelector trafficSelector;
    private final RequestMetaData requestMetaData;

    @Setter
    private Runnable onBodyCachedListener;

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

        return super.getBody()
            .doOnNext(dataBuffer -> {
                if (cachedBody == null) {
                    cachedBody = "";
                }
                cachedBody += dataBuffer.toString(StandardCharsets.UTF_8);
            })
            .doFinally(signalType -> {
                if (signalType == SignalType.ON_COMPLETE) {
                    if (onBodyCachedListener != null) {
                        onBodyCachedListener.run();
                    }
                }
            });
    }
}
