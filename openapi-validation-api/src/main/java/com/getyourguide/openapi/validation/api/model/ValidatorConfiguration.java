package com.getyourguide.openapi.validation.api.model;

import com.getyourguide.openapi.validation.api.log.LogLevel;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ValidatorConfiguration {
    private final LogLevel levelResolverDefaultLevel;
    private final Map<String, LogLevel> levelResolverLevels;
}
