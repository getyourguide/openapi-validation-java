package com.getyourguide.openapi.validation.api.metrics;

import com.getyourguide.openapi.validation.api.model.OpenApiViolation;
import java.util.List;

public interface MetricTagProvider {
    List<MetricTag> getTagsForViolation(OpenApiViolation violation);
}
