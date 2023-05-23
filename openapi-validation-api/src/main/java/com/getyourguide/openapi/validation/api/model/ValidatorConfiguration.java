package com.getyourguide.openapi.validation.api.model;

import com.getyourguide.openapi.validation.api.log.LogLevel;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ValidatorConfiguration {
    private final LogLevel levelResolverDefaultLevel;
    private final Map<String, LogLevel> levelResolverLevels;

    private final List<PathPatternSpec> specificationPaths;

    public record PathPatternSpec(Pattern pathPattern, String specificationFilePath) {
    }
}
