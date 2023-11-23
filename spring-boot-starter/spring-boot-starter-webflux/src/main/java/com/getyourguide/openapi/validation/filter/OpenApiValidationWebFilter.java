package com.getyourguide.openapi.validation.filter;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import com.getyourguide.openapi.validation.api.model.ValidationResult;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ReactiveMetaDataFactory;
import com.getyourguide.openapi.validation.filter.decorator.BodyCachingServerHttpRequestDecorator;
import com.getyourguide.openapi.validation.filter.decorator.BodyCachingServerHttpResponseDecorator;
import com.getyourguide.openapi.validation.filter.decorator.DecoratorBuilder;
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
                        validateRequest(requestDecorated, requestMetaData, responseMetaData, RunType.ASYNC);
                    }
                    if (!alreadyDidValidation.response) {
                        validateResponse(
                            requestMetaData, responseMetaData, responseDecorated.getCachedBody(), RunType.ASYNC);
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
            || !request.getHeaders().containsKey("Content-Type")
            || !request.getHeaders().containsKey("Content-Length")) {
            return Mono.just(alreadyDidValidation);
        }

        return request.consumeRequestBody()
            .map((optionalRequestBody) -> {
                var validateRequestResult = validateRequest(request, requestMetaData, null, RunType.SYNC);
                throwStatusExceptionOnViolation(validateRequestResult, 400, "Request validation failed");
                alreadyDidValidation.request = true;
                return alreadyDidValidation;
            });
    }

    private static void throwStatusExceptionOnViolation(ValidationResult validateRequestResult, int statusCode, String message) {
        if (validateRequestResult == ValidationResult.INVALID) {
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
            var validateResponseResult = validateResponse(
                requestMetaData,
                metaDataFactory.buildResponseMetaData(responseDecorated),
                responseDecorated.getCachedBody(),
                RunType.SYNC
            );
            throwStatusExceptionOnViolation(validateResponseResult, 500, "Response validation failed");
        });
        return true;
    }

    private ValidationResult validateRequest(
        BodyCachingServerHttpRequestDecorator request,
        RequestMetaData requestMetaData,
        @Nullable ResponseMetaData responseMetaData,
        RunType runType
    ) {
        if (!trafficSelector.canRequestBeValidated(requestMetaData)) {
            return ValidationResult.NOT_APPLICABLE;
        }

        if (runType == RunType.SYNC) {
            return validator.validateRequestObject(requestMetaData, responseMetaData, request.getCachedBody());
        } else {
            validator.validateRequestObjectAsync(requestMetaData, responseMetaData, request.getCachedBody());
            return ValidationResult.NOT_APPLICABLE;
        }
    }

    private ValidationResult validateResponse(
        RequestMetaData requestMetaData,
        @Nullable ResponseMetaData responseMetaData,
        @Nullable String responseBody,
        RunType runType
    ) {
        if (!trafficSelector.canResponseBeValidated(requestMetaData, responseMetaData)) {
            return ValidationResult.NOT_APPLICABLE;
        }

        if (runType == RunType.SYNC) {
            return validator.validateResponseObject(requestMetaData, responseMetaData, responseBody);
        } else {
            validator.validateResponseObjectAsync(requestMetaData, responseMetaData, responseBody);
            return ValidationResult.NOT_APPLICABLE;
        }
    }

    private enum RunType { SYNC, ASYNC }

    private static class AlreadyDidValidation {
        private boolean request = false;
        private boolean response = false;
    }
}
