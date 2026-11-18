package com.getyourguide.openapi.validation.filter.decorator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class BodyCachingServerHttpRequestDecoratorTest {

    private final TrafficSelector trafficSelector = mock();
    private final RequestMetaData requestMetaData = mock();

    @Test
    public void testShouldReplayBodyWhenDownstreamSubscribesImmediatelyAfterBodyIsConsumed() {
        var subscriptionCount = new AtomicInteger();
        var request = oneShotRequest(subscriptionCount, Flux.just(bufferWith("valid body")));
        var decorator = decoratorFor(request);

        StepVerifier.create(
                decorator.consumeRequestBody()
                    .flatMap(ignored -> readBody(decorator))
            )
            .expectNext("valid body")
            .verifyComplete();

        assertEquals(1, subscriptionCount.get());
    }

    @Test
    public void testShouldReplayAnEmptyBodyAfterItIsConsumed() {
        var subscriptionCount = new AtomicInteger();
        var request = oneShotRequest(subscriptionCount, Flux.empty());
        var decorator = decoratorFor(request);

        StepVerifier.create(
                decorator.consumeRequestBody()
                    .flatMap(ignored -> readBody(decorator))
            )
            .expectNext("")
            .verifyComplete();

        assertEquals("", decorator.getCachedBody());
        assertEquals(1, subscriptionCount.get());
    }

    private BodyCachingServerHttpRequestDecorator decoratorFor(ServerHttpRequest request) {
        when(trafficSelector.canRequestBeValidated(requestMetaData)).thenReturn(true);
        return new BodyCachingServerHttpRequestDecorator(request, trafficSelector, requestMetaData);
    }

    private static ServerHttpRequest oneShotRequest(AtomicInteger subscriptionCount, Flux<DataBuffer> body) {
        var request = mock(ServerHttpRequest.class);
        when(request.getBody()).thenReturn(Flux.defer(() -> {
            if (subscriptionCount.getAndIncrement() == 0) {
                return body;
            }
            return Flux.error(new IllegalStateException("The original request body was subscribed to more than once"));
        }));
        return request;
    }

    private static DataBuffer bufferWith(String body) {
        return DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(UTF_8));
    }

    private static reactor.core.publisher.Mono<String> readBody(BodyCachingServerHttpRequestDecorator decorator) {
        return decorator.getBody()
            .map(dataBuffer -> dataBuffer.toString(UTF_8))
            .collect(Collectors.joining());
    }
}
