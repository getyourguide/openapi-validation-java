package com.getyourguide.openapi.validation.api.model;

import com.getyourguide.openapi.validation.api.log.LogLevel;
import java.util.HashMap;
import java.util.Map;

public class ValidatorConfigurationBuilder {
    private LogLevel levelResolverDefaultLevel;
    private Map<String, LogLevel> levelResolverLevels;

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

    public ValidatorConfiguration build() {
        return new ValidatorConfiguration(
            levelResolverDefaultLevel,
            levelResolverLevels
        );
    }

    public String toString() {
        return "ValidatorConfigurationBuilder(" +
            "levelResolverDefaultLevel=" + this.levelResolverDefaultLevel + ", " +
            "levelResolverLevels=" + this.levelResolverLevels
            + ")";
    }
}
