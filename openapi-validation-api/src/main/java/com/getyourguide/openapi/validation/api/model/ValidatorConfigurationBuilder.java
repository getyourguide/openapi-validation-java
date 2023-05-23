package com.getyourguide.openapi.validation.api.model;

import com.getyourguide.openapi.validation.api.log.LogLevel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ValidatorConfigurationBuilder {
    private LogLevel levelResolverDefaultLevel;
    private Map<String, LogLevel> levelResolverLevels;
    private List<ValidatorConfiguration.PathPatternSpec> specificationPaths;

    public ValidatorConfigurationBuilder levelResolverDefaultLevel(LogLevel levelResolverDefaultLevel) {
        this.levelResolverDefaultLevel = levelResolverDefaultLevel;
        return this;
    }

    public ValidatorConfigurationBuilder levelResolverLevel(String messageKey, LogLevel level) {
        if (this.levelResolverLevels == null) {
            this.levelResolverLevels = new HashMap<>();
        }
        this.levelResolverLevels.put(messageKey, level);
        return this;
    }

    public ValidatorConfigurationBuilder specificationPath(Pattern pathPattern, String specPath) {
        if (this.specificationPaths == null) {
            this.specificationPaths = new ArrayList<>();
        }
        this.specificationPaths.add(new ValidatorConfiguration.PathPatternSpec(pathPattern, specPath));
        return this;
    }

    public ValidatorConfiguration build() {
        return new ValidatorConfiguration(
            levelResolverDefaultLevel,
            levelResolverLevels,
            specificationPaths
        );
    }

    public String toString() {
        return "ValidatorConfigurationBuilder("
            + "levelResolverDefaultLevel=" + this.levelResolverDefaultLevel + ", "
            + "levelResolverLevels=" + this.levelResolverLevels
            + ")";
    }
}
