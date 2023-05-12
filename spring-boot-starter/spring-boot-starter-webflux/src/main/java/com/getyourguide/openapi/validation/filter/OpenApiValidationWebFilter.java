package com.getyourguide.openapi.validation.filter;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ReactiveMetaDataFactory;
import com.getyourguide.openapi.validation.filter.decorator.BodyCachingServerHttpRequestDecorator;
import com.getyourguide.openapi.validation.filter.decorator.BodyCachingServerHttpResponseDecorator;
import com.getyourguide.openapi.validation.filter.decorator.DecoratorBuilder;
import lombok.AllArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@AllArgsConstructor
public class OpenApiValidationWebFilter implements WebFilter {

    private final OpenApiRequestValidator validator;
    private final TrafficSelector trafficSelector;
    private final ReactiveMetaDataFactory metaDataFactory;
    private final DecoratorBuilder decoratorBuilder;

    @Override
    @NonNull
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        var request = exchange.getRequest();
        var requestMetaData = metaDataFactory.buildRequestMetaData(request);

        if (!validator.isReady() || !trafficSelector.shouldRequestBeValidated(requestMetaData)) {
            return chain.filter(exchange);
        }

        return decorateWithValidation(exchange, chain, requestMetaData);
    }

    @NonNull
    private Mono<Void> decorateWithValidation(ServerWebExchange exchange, WebFilterChain chain, RequestMetaData requestMetaData) {
        var requestDecorated = decoratorBuilder.buildBodyCachingServerHttpRequestDecorator(exchange.getRequest(), requestMetaData);
        var responseDecorated = decoratorBuilder.buildBodyCachingServerHttpResponseDecorator(exchange.getResponse(), requestMetaData);

        var serverWebExchange = exchange.mutate().request(requestDecorated).response(responseDecorated).build();

        return chain.filter(serverWebExchange)
            .doFinally(signalType -> {
                // Note: Errors are not handled here. They could be handled with SignalType.ON_ERROR, but then the response body can't be accessed.
                //       Reason seems to be that those don't use the decorated response that is set here, but use the previous response object.
                if (signalType == SignalType.ON_COMPLETE) {
                    validateRequest(requestDecorated, requestMetaData);
                    validateResponse(responseDecorated, requestMetaData);
                }
            });
    }

    private void validateRequest(BodyCachingServerHttpRequestDecorator request, RequestMetaData requestMetaData) {
        if (!trafficSelector.canRequestBeValidated(requestMetaData)) {
            return;
        }

        validator.validateRequestObjectAsync(requestMetaData, request.getCachedBody());
    }

    private void validateResponse(BodyCachingServerHttpResponseDecorator response, RequestMetaData requestMetaData) {
        var responseMetaData = metaDataFactory.buildResponseMetaData(response);
        if (!trafficSelector.canResponseBeValidated(requestMetaData, responseMetaData)) {
            return;
        }

        validator.validateResponseObjectAsync(requestMetaData, responseMetaData, response.getCachedBody());
    }
}
