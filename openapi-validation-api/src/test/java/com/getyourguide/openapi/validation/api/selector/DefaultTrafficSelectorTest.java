package com.getyourguide.openapi.validation.api.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.getyourguide.openapi.validation.api.exclusions.ExcludedHeader;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DefaultTrafficSelectorTest {
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
}
