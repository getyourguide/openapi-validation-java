package com.getyourguide.openapi.validation.api.metrics;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import java.util.List;

public class NullMetricTagProvider implements MetricTagProvider {
    @Override
    public List<MetricTag> getTagsForViolation(OpenApiViolation violation) {
        return List.of();
    }
}
