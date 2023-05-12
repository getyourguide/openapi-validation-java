package com.getyourguide.openapi.validation.api.selector;

import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import com.getyourguide.openapi.validation.api.model.ResponseMetaData;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class DefaultTrafficSelector implements TrafficSelector {

    private static final double SAMPLE_RATE_DEFAULT = 0.001; // 1.0 = 100%

    private final double sampleRate;
    private final Set<String> excludedPaths;

    public DefaultTrafficSelector(Double sampleRate, Set<String> excludedPaths) {
        this.sampleRate = sampleRate != null ? sampleRate : SAMPLE_RATE_DEFAULT;
        this.excludedPaths = excludedPaths != null ? excludedPaths : Set.of();
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
            && isContentTypeSupported(request.getContentType());
    }

    @Override
    public boolean canResponseBeValidated(RequestMetaData request, ResponseMetaData response) {
        return !methodEquals(request.getMethod(), "OPTIONS")
            && isContentTypeSupported(response.getContentType());
    }

    private boolean isExcludedRequest(RequestMetaData request) {
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
