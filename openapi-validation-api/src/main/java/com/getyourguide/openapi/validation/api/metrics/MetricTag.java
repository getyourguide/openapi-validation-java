package com.getyourguide.openapi.validation.api.metrics;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MetricTag {
    private final String key;
    private final String value;

    @Override
    public String toString() {
        return String.format("%s:%s", key, value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MetricTag && toString().equals(obj.toString());
    }
}
