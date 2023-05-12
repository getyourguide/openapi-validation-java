package com.getyourguide.openapi.validation.core.throttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.ApiPath;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.model.Direction;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RequestBasedValidationReportThrottlerTest {
    private static final Direction DIRECTION = Direction.REQUEST;
    private static final Runnable NO_OP_RUNNABLE = () -> {
    };

    private RequestBasedValidationReportThrottler throttler;

    @BeforeEach
    public void beforeEach() {
        throttler = new RequestBasedValidationReportThrottler(10);
    }

    @Test
    public void testNotThrottledIfNoEntry() {
        var message = mockMessage(Request.Method.GET, "/path", 200);

        assertThrottled(false, message, DIRECTION);
    }

    @Test
    public void testThrottledIfEntryExists() {
        var message = mockMessage(Request.Method.GET, "/path", 200);
        throttler.throttle(message, DIRECTION, NO_OP_RUNNABLE);

        assertThrottled(true, message, DIRECTION);
    }

    @Test
    public void testNotThrottledIfSmallDifference() {
        var message = mockMessage(Request.Method.GET, "/path", 200);
        throttler.throttle(message, DIRECTION, NO_OP_RUNNABLE);

        assertThrottled(false, mockMessage(Request.Method.GET, "/path", 200), Direction.RESPONSE);
        assertThrottled(false, mockMessage(Request.Method.POST, "/path", 200), DIRECTION);
        assertThrottled(false, mockMessage(Request.Method.GET, "/other-path", 200), DIRECTION);
        assertThrottled(false, mockMessage(Request.Method.GET, "/path", 402), DIRECTION);
    }

    @Test
    public void testThrottledIfInstanceContainsArrayIndex() {
        var message = mockMessage(Request.Method.GET, "/path", 200, "/items/1/name", "/properties/items/items/properties/name");
        throttler.throttle(message, DIRECTION, NO_OP_RUNNABLE);

        assertThrottled(
            true,
            mockMessage(Request.Method.GET, "/path", 200, "/items/2/name", "/properties/items/items/properties/name"),
            DIRECTION
        );
        assertThrottled(
            true,
            mockMessage(Request.Method.GET, "/path", 200, "/items/3/name", "/properties/items/items/properties/name"),
            DIRECTION
        );
        assertThrottled(
            false,
            mockMessage(Request.Method.GET, "/path", 200, "/items/4/description", "/properties/items/items/properties/description"),
            DIRECTION
        );
    }

    private void assertThrottled(boolean expectThrottled, ValidationReport.Message message, Direction direction) {
        var ref = new Object() {
            boolean wasThrottled = true;
        };

        throttler.throttle(message, direction, () -> ref.wasThrottled = false);

        assertEquals(expectThrottled, ref.wasThrottled);
    }

    private ValidationReport.Message mockMessage(Request.Method method, String path, int status) {
        return mockMessage(method, path, status, "/items/1/name", "/properties/items/items/properties/name");
    }

    private ValidationReport.Message mockMessage(Request.Method method, String path, int status, String instance, String schema) {
        var message = mock(ValidationReport.Message.class);
        var context = mock(ValidationReport.MessageContext.class);

        when(context.getRequestMethod()).thenReturn(Optional.of(method));

        var apiOperation = mock(ApiOperation.class);
        var apiPath = mock(ApiPath.class);
        when(apiPath.normalised()).thenReturn(path);
        when(apiOperation.getApiPath()).thenReturn(apiPath);
        when(context.getApiOperation()).thenReturn(Optional.of(apiOperation));
        when(context.getResponseStatus()).thenReturn(Optional.of(status));

        var pointers = mock(ValidationReport.MessageContext.Pointers.class);
        when(pointers.getInstance()).thenReturn(instance);
        when(pointers.getSchema()).thenReturn(schema);
        when(context.getPointers()).thenReturn(Optional.of(pointers));

        when(message.getContext()).thenReturn(Optional.of(context));
        return message;
    }
}
