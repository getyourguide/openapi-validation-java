package com.getyourguide.openapi.validation.filter.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.factory.ReactiveMetaDataFactory;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;

class BodyCachingServerHttpResponseDecoratorTest {

    @Test
    public void whenResponseContainsMultipleBuffersThenAllBuffersAreWritten() {
        var delegate = new MockServerHttpResponse();
        delegate.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        var trafficSelector = mock(TrafficSelector.class);
        var requestMetaData = mock(RequestMetaData.class);
        when(trafficSelector.canResponseBeValidated(eq(requestMetaData), any())).thenReturn(true);

        var decorator = new BodyCachingServerHttpResponseDecorator(
            delegate,
            trafficSelector,
            new ReactiveMetaDataFactory(),
            requestMetaData
        );

        decorator.writeWith(Flux.just(
            delegate.bufferFactory().wrap("{\"name\":\"GetYour".getBytes(StandardCharsets.UTF_8)),
            delegate.bufferFactory().wrap("Guide\"}".getBytes(StandardCharsets.UTF_8))
        )).block();

        assertEquals("{\"name\":\"GetYourGuide\"}", delegate.getBodyAsString().block());
        assertEquals("{\"name\":\"GetYourGuide\"}", decorator.getCachedBody());
    }
}
