package com.getyourguide.openapi.validation.api.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.getyourguide.openapi.validation.api.exclusions.ExcludedHeader;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class DefaultTrafficSelectorTest {
    private static final int SAMPLE_SIZE = 10000;
    private static final double ACCEPTABLE_SAMPLE_RATE_DEVIATION = 0.02;

    private final TrafficSelector trafficSelector = new DefaultTrafficSelector(
        1.0,
        null,
        List.of(
            new ExcludedHeader("User-Agent", Pattern.compile(".*(bingbot|googlebot).*", Pattern.CASE_INSENSITIVE)),
            new ExcludedHeader("x-is-bot", Pattern.compile("true", Pattern.CASE_INSENSITIVE))
        )
    );

    @Test
    public void testIsExcludedByHeaderPattern() {
        assertHeaderIsExcluded(true,
            "user-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
        assertHeaderIsExcluded(false,
            "User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36");

        assertHeaderIsExcluded(true, "x-is-bot", "true");
        assertHeaderIsExcluded(false, "x-is-bot", "truebot");
    }

    @Test
    public void testIsExcludedByPath() {
        // Default exclusions
        assertPathIsExcluded(true, "/graphql");
        assertPathIsExcluded(true, "/graphiql");

        assertPathIsExcluded(false, "/v1/path");
    }

    private void assertHeaderIsExcluded(boolean expectedExclusion, String headerName, String headerValue) {
        var request = new RequestMetaData(
            "GET",
            URI.create("https://api.example.com/v1/path"),
            toCaseInsensitiveMap(Map.of(
                "Content-Type", "application/json",
                "Content-Length", "10",
                headerName, headerValue
            ))
        );
        assertEquals(!expectedExclusion, trafficSelector.shouldRequestBeValidated(request));
    }

    private void assertPathIsExcluded(boolean expectedExclusion, String path) {
        var request = new RequestMetaData(
            "GET",
            URI.create("https://api.example.com" + path),
            Map.of("Content-Type", "application/json")
        );
        assertEquals(!expectedExclusion, trafficSelector.shouldRequestBeValidated(request));
    }

    private Map<String, String> toCaseInsensitiveMap(Map<String, String> map) {
        var newMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        newMap.putAll(map);
        return newMap;
    }

    @Test
    public void testSampleRateIsApproximatelyCorrectForReciprocalValues() {
        List.of(0.1, 0.25, 0.5).forEach(this::assertSampleRateIsApproximatelyCorrect);
    }

    @Test
    public void testSampleRateIsApproximatelyCorrectForNonReciprocalValues() {
        List.of(0.3, 0.8).forEach(this::assertSampleRateIsApproximatelyCorrect);
    }

    private void assertSampleRateIsApproximatelyCorrect(double sampleRate) {
        var sampleRateTrafficSelector = new DefaultTrafficSelector(sampleRate, null, null);
        var request = new RequestMetaData(
            "GET",
            URI.create("https://api.example.com/v1/path"),
            Map.of("Content-Type", "application/json")
        );
        var validatedRequestCount = IntStream.range(0, SAMPLE_SIZE)
            .filter(ignored -> sampleRateTrafficSelector.shouldRequestBeValidated(request))
            .count();
        var actualSampleRate = (double) validatedRequestCount / SAMPLE_SIZE;

        assertTrue(
            Math.abs(actualSampleRate - sampleRate) < ACCEPTABLE_SAMPLE_RATE_DEVIATION,
            "Expected a sample rate close to " + sampleRate + ", but was " + actualSampleRate
        );
    }
}
