package com.getyourguide.openapi.validation.filter;

import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ReactiveMetaDataFactory;
import com.getyourguide.openapi.validation.filter.decorator.BodyCachingServerHttpRequestDecorator;
import com.getyourguide.openapi.validation.filter.decorator.BodyCachingServerHttpResponseDecorator;
import com.getyourguide.openapi.validation.filter.decorator.DecoratorBuilder;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
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
    private final OpenApiViolationHandler openApiViolationHandler;

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

        final var alreadyDidValidation = new AlreadyDidValidation();
        alreadyDidValidation.response = validateResponseWithFailOnViolation(requestMetaData, responseDecorated);
        return optionalValidateRequestWithFailOnViolation(requestMetaData, requestDecorated, alreadyDidValidation)
            .flatMap(dataBuffer -> chain.filter(serverWebExchange))
            .doFinally(signalType -> {
                // Note: Errors are not handled here. They could be handled with SignalType.ON_ERROR, but then the response body can't be accessed.
                //       Reason seems to be that those don't use the decorated response that is set here, but use the previous response object.
                if (signalType == SignalType.ON_COMPLETE) {
                    var responseMetaData = metaDataFactory.buildResponseMetaData(responseDecorated);
                    if (!alreadyDidValidation.request) {
                        var violations =
                            validateRequest(requestDecorated, requestMetaData, responseMetaData, RunType.ASYNC);
                        violations.forEach(openApiViolationHandler::onOpenApiViolation);
                    }
                    if (!alreadyDidValidation.response) {
                        var violations = validateResponse(
                            requestMetaData, responseMetaData, responseDecorated.getCachedBody(), RunType.ASYNC);
                        violations.forEach(openApiViolationHandler::onOpenApiViolation);
                    }
                }
            });
    }

    private Mono<AlreadyDidValidation> optionalValidateRequestWithFailOnViolation(
        RequestMetaData requestMetaData,
        BodyCachingServerHttpRequestDecorator request,
        AlreadyDidValidation alreadyDidValidation
    ) {
        if (!trafficSelector.shouldFailOnRequestViolation(requestMetaData)
            || !request.getHeaders().containsHeader("Content-Type")
            || !request.getHeaders().containsHeader("Content-Length")) {
            return Mono.just(alreadyDidValidation);
        }

        return request.consumeRequestBody()
            .map((optionalRequestBody) -> {
                var violations = validateRequest(request, requestMetaData, null, RunType.SYNC);
                throwStatusExceptionOnViolation(violations, 400, "Request validation failed");
                alreadyDidValidation.request = true;
                return alreadyDidValidation;
            });
    }

    private static void throwStatusExceptionOnViolation(List<OpenApiViolation> violations, int statusCode, String message) {
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.valueOf(statusCode), message);
        }
    }

    /**
     * Validate response and fail on violation if configured to do so.
     *
     * @return true if validation is done as part of this method
     */
    private boolean validateResponseWithFailOnViolation(
        RequestMetaData requestMetaData,
        BodyCachingServerHttpResponseDecorator responseDecorated
    ) {
        if (!trafficSelector.shouldFailOnResponseViolation(requestMetaData)) {
            return false;
        }

        responseDecorated.setOnBodyCachedListener(() -> {
            var violations = validateResponse(
                requestMetaData,
                metaDataFactory.buildResponseMetaData(responseDecorated),
                responseDecorated.getCachedBody(),
                RunType.SYNC
            );
            violations.forEach(openApiViolationHandler::onOpenApiViolation);
            throwStatusExceptionOnViolation(violations, 500, "Response validation failed");
        });
        return true;
    }

    private List<OpenApiViolation> validateRequest(
        BodyCachingServerHttpRequestDecorator request,
        RequestMetaData requestMetaData,
        @Nullable ResponseMetaData responseMetaData,
        RunType runType
    ) {
        if (!trafficSelector.canRequestBeValidated(requestMetaData)) {
            return List.of();
        }

        if (runType == RunType.SYNC) {
            return validator.validateRequestObject(requestMetaData, responseMetaData, request.getCachedBody());
        } else {
            validator.validateRequestObjectAsync(
                requestMetaData, responseMetaData, request.getCachedBody(), openApiViolationHandler);
            return List.of();
        }
    }

    private List<OpenApiViolation> validateResponse(
        RequestMetaData requestMetaData,
        @Nullable ResponseMetaData responseMetaData,
        @Nullable String responseBody,
        RunType runType
    ) {
        if (!trafficSelector.canResponseBeValidated(requestMetaData, responseMetaData)) {
            return List.of();
        }

        if (runType == RunType.SYNC) {
            return validator.validateResponseObject(requestMetaData, responseMetaData, responseBody);
        } else {
            validator
                .validateResponseObjectAsync(requestMetaData, responseMetaData, responseBody, openApiViolationHandler);
            return List.of();
        }
    }

    private enum RunType { SYNC, ASYNC }

    private static class AlreadyDidValidation {
        private boolean request = false;
        private boolean response = false;
    }
}
