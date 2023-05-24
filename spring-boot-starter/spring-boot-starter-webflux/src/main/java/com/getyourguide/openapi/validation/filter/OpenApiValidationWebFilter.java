package com.getyourguide.openapi.validation.filter;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ValidationResult;
import com.getyourguide.openapi.validation.api.selector.TrafficSelector;
import com.getyourguide.openapi.validation.core.OpenApiRequestValidator;
import com.getyourguide.openapi.validation.factory.ReactiveMetaDataFactory;
import com.getyourguide.openapi.validation.filter.decorator.BodyCachingServerHttpRequestDecorator;
import com.getyourguide.openapi.validation.filter.decorator.BodyCachingServerHttpResponseDecorator;
import com.getyourguide.openapi.validation.filter.decorator.DecoratorBuilder;
import lombok.AllArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
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

        var alreadyDidRequestValidation = validateRequestWithFailOnViolation(requestMetaData, requestDecorated);
        var alreadyDidResponseValidation = validateResponseWithFailOnViolation(requestMetaData, responseDecorated);

        return chain.filter(serverWebExchange)
            .doFinally(signalType -> {
                // Note: Errors are not handled here. They could be handled with SignalType.ON_ERROR, but then the response body can't be accessed.
                //       Reason seems to be that those don't use the decorated response that is set here, but use the previous response object.
                if (signalType == SignalType.ON_COMPLETE) {
                    if (!alreadyDidRequestValidation) {
                        validateRequest(requestDecorated, requestMetaData, RunType.ASYNC);
                    }
                    if (!alreadyDidResponseValidation) {
                        validateResponse(responseDecorated, requestMetaData, RunType.ASYNC);
                    }
                }
            });
    }

    /**
     * Validate request and fail on violation if configured to do so.
     * @return true if validation is done as part of this method
     */
    private boolean validateRequestWithFailOnViolation(
        RequestMetaData requestMetaData,
        BodyCachingServerHttpRequestDecorator requestDecorated
    ) {
        if (!trafficSelector.shouldFailOnRequestViolation(requestMetaData)) {
            return false;
        }

        if (requestDecorated.getHeaders().containsKey("Content-Type") && requestDecorated.getHeaders().containsKey("Content-Length")) {
            requestDecorated.setOnBodyCachedListener(() -> {
                var validateRequestResult = validateRequest(requestDecorated, requestMetaData, RunType.SYNC);
                throwStatusExceptionOnViolation(validateRequestResult, "Request validation failed");
            });
        } else {
            var validateRequestResult = validateRequest(requestDecorated, requestMetaData, RunType.SYNC);
            throwStatusExceptionOnViolation(validateRequestResult, "Request validation failed");
        }
        return true;
    }

    private static void throwStatusExceptionOnViolation(ValidationResult validateRequestResult, String message) {
        if (validateRequestResult == ValidationResult.INVALID) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), message);
        }
    }

    /**
     * Validate response and fail on violation if configured to do so.
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
            var validateResponseResult = validateResponse(responseDecorated, requestMetaData, RunType.SYNC);
            throwStatusExceptionOnViolation(validateResponseResult, "Response validation failed");
        });
        return true;
    }

    private ValidationResult validateRequest(
        BodyCachingServerHttpRequestDecorator request,
        RequestMetaData requestMetaData,
        RunType runType
    ) {
        if (!trafficSelector.canRequestBeValidated(requestMetaData)) {
            return ValidationResult.NOT_APPLICABLE;
        }

        if (runType == RunType.SYNC) {
            return validator.validateRequestObject(requestMetaData, request.getCachedBody());
        } else {
            validator.validateRequestObjectAsync(requestMetaData, request.getCachedBody());
            return ValidationResult.NOT_APPLICABLE;
        }
    }

    private ValidationResult validateResponse(
        BodyCachingServerHttpResponseDecorator response,
        RequestMetaData requestMetaData,
        RunType runType
    ) {
        var responseMetaData = metaDataFactory.buildResponseMetaData(response);
        if (!trafficSelector.canResponseBeValidated(requestMetaData, responseMetaData)) {
            return ValidationResult.NOT_APPLICABLE;
        }

        if (runType == RunType.SYNC) {
            return validator.validateResponseObject(requestMetaData, responseMetaData, response.getCachedBody());
        } else {
            validator.validateResponseObjectAsync(requestMetaData, responseMetaData, response.getCachedBody());
            return ValidationResult.NOT_APPLICABLE;
        }
    }

    private enum RunType { SYNC, ASYNC }
}
