package com.getyourguide.openapi.validation.api.selector;

import com.getyourguide.openapi.validation.api.exclusions.ExcludedHeader;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class DefaultTrafficSelector implements TrafficSelector {

    private final double sampleRate;
    private final Set<String> excludedPaths;
    private final List<ExcludedHeader> excludedHeaders;
    private final Boolean shouldFailOnRequestViolation;
    private final Boolean shouldFailOnResponseViolation;

    public DefaultTrafficSelector(Double sampleRate, Set<String> excludedPaths, List<ExcludedHeader> excludedHeaders) {
        this(sampleRate, excludedPaths, excludedHeaders, null, null);
    }

    public DefaultTrafficSelector(
        double sampleRate,
        Set<String> excludedPaths,
        List<ExcludedHeader> excludedHeaders,
        Boolean shouldFailOnRequestViolation,
        Boolean shouldFailOnResponseViolation
    ) {
        this.sampleRate = sampleRate;
        this.excludedPaths = excludedPaths != null ? excludedPaths : Set.of();
        this.excludedHeaders = excludedHeaders != null ? excludedHeaders : Collections.emptyList();
        this.shouldFailOnRequestViolation = shouldFailOnRequestViolation != null ? shouldFailOnRequestViolation : false;
        this.shouldFailOnResponseViolation =
            shouldFailOnResponseViolation != null ? shouldFailOnResponseViolation : false;
    }

    @Override
    public boolean shouldRequestBeValidated(RequestMetaData request) {
        return isRandomlySelectedBySampleRate()
            && !isExcludedRequest(request);
    }

    private boolean isRandomlySelectedBySampleRate() {
        return sampleRate > 0
            && (sampleRate >= 1 || ThreadLocalRandom.current().nextInt((int) (1 / sampleRate)) == 0);
    }

    @Override
    public boolean canRequestBeValidated(RequestMetaData request) {
        return !methodEquals(request.getMethod(), "OPTIONS")
            && !methodEquals(request.getMethod(), "HEAD")
            && isContentTypeSupported(request.getContentType());
    }

    @Override
    public boolean canResponseBeValidated(RequestMetaData request, ResponseMetaData response) {
        return !methodEquals(request.getMethod(), "OPTIONS")
            && !methodEquals(request.getMethod(), "HEAD")
            && isContentTypeSupported(response.getContentType());
    }

    @Override
    public boolean shouldFailOnRequestViolation(RequestMetaData request) {
        return shouldFailOnRequestViolation;
    }

    @Override
    public boolean shouldFailOnResponseViolation(RequestMetaData request) {
        return shouldFailOnResponseViolation;
    }

    private boolean isExcludedRequest(RequestMetaData request) {
        return isRequestExcludedByHeader(request) || isRequestExcludedByPath(request);
    }

    private boolean isRequestExcludedByHeader(RequestMetaData request) {
        return excludedHeaders.stream().anyMatch(excludedHeader -> {
            var headerValue = request.getHeaders().get(excludedHeader.headerName());
            return headerValue != null && excludedHeader.headerValuePattern().matcher(headerValue).matches();
        });
    }

    private boolean isRequestExcludedByPath(RequestMetaData request) {
        return excludedPaths.contains(request.getUri().getPath());
    }

    private static boolean methodEquals(String method, String expectedMethod) {
        return method.equalsIgnoreCase(expectedMethod);
    }

    private boolean isContentTypeSupported(String contentType) {
        if (contentType == null) {
            return true;
        }

        var contentTypeLowerCase = contentType.toLowerCase();
        return contentTypeLowerCase.contains("application/json")
            || contentTypeLowerCase.contains("application/xml")
            || contentTypeLowerCase.contains("application/xhtml+xml")
            || contentTypeLowerCase.contains("text/html")
            || contentTypeLowerCase.contains("text/xml");
    }
}
