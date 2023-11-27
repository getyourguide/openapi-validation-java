package com.getyourguide.openapi.validation.core.log;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.atlassian.oai.validator.model.Request;
import com.getyourguide.openapi.validation.api.log.OpenApiViolationHandler;
import com.getyourguide.openapi.validation.api.model.Direction;
import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import com.getyourguide.openapi.validation.api.model.RequestMetaData;
import java.net.URI;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ThrottlingOpenApiViolationHandlerTest {
    private static final Direction DIRECTION = Direction.REQUEST;
    private static final OpenApiViolationHandler delegate = mock();

    private ThrottlingOpenApiViolationHandler handler;

    @BeforeEach
    public void beforeEach() {
        handler = new ThrottlingOpenApiViolationHandler(delegate, 10);
    }

    @Test
    public void testNotThrottledIfNoEntry() {
        var violation = buildViolation(DIRECTION, Request.Method.GET, "/path", 200);

        assertThrottled(false, violation);
    }

    @Test
    public void testThrottledIfEntryExists() {
        var violation = buildViolation(DIRECTION, Request.Method.GET, "/path", 200);
        handler.onOpenApiViolation(violation);

        assertThrottled(true, violation);
    }

    @Test
    public void testNotThrottledIfSmallDifference() {
        var violation = buildViolation(DIRECTION, Request.Method.GET, "/path", 200);
        handler.onOpenApiViolation(violation);

        assertThrottled(false, buildViolation(Direction.RESPONSE, Request.Method.GET, "/path", 200));
        assertThrottled(false, buildViolation(DIRECTION, Request.Method.POST, "/path", 200));
        assertThrottled(false, buildViolation(DIRECTION, Request.Method.GET, "/other-path", 200));
        assertThrottled(false, buildViolation(DIRECTION, Request.Method.GET, "/path", 402));
    }

    @Test
    public void testThrottledIfInstanceContainsArrayIndex() {
        var violation = buildViolation(DIRECTION, Request.Method.GET, "/path", 200, "/items/1/name", "/properties/items/items/properties/name");
        handler.onOpenApiViolation(violation);

        assertThrottled(
            true,
            buildViolation(DIRECTION, Request.Method.GET, "/path", 200, "/items/2/name", "/properties/items/items/properties/name")
        );
        assertThrottled(
            true,
            buildViolation(DIRECTION, Request.Method.GET, "/path", 200, "/items/3/name", "/properties/items/items/properties/name")
        );
        assertThrottled(
            false,
            buildViolation(DIRECTION, Request.Method.GET, "/path", 200, "/items/4/description", "/properties/items/items/properties/description")
        );
    }

    private void assertThrottled(boolean expectThrottled, OpenApiViolation violation) {
        clearInvocations(delegate);
        handler.onOpenApiViolation(violation);

        verify(delegate, expectThrottled ? never() : times(1))
            .onOpenApiViolation(violation);
    }

    private OpenApiViolation buildViolation(Direction direction, Request.Method method, String path, int status) {
        return buildViolation(direction, method, path, status, "/items/1/name", "/properties/items/items/properties/name");
    }

    private OpenApiViolation buildViolation(Direction direction, Request.Method method, String path, int status, String instance, String schema) {
        return OpenApiViolation.builder()
            .direction(direction)
            .requestMetaData(
                new RequestMetaData(method.toString(), URI.create("https://example.com" + path), Collections.emptyMap())
            )
            .responseStatus(status)
            .normalizedPath(path)
            .instance(instance)
            .schema(schema)
            .build();
    }
}
