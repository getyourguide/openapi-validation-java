package com.getyourguide.openapi.validation.core;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.getyourguide.openapi.validation.api.log.LogLevel;
import com.getyourguide.openapi.validation.api.model.ValidatorConfiguration;
import com.getyourguide.openapi.validation.core.validator.OpenApiInteractionValidatorWrapper;
import com.getyourguide.openapi.validation.core.validator.SingleSpecOpenApiInteractionValidatorWrapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
public class OpenApiInteractionValidatorFactory {
    @Nullable
    public OpenApiInteractionValidatorWrapper build(String specificationFilePath, ValidatorConfiguration configuration) {
        var specOptional = loadOpenAPISpec(specificationFilePath);
        if (specOptional.isEmpty()) {
            log.info("OpenAPI spec file could not be found [validation disabled]");
            return null;
        }

        var spec = specOptional.get();
        try {
            var validator = OpenApiInteractionValidator
                .createForInlineApiSpecification(spec)
                .withResolveRefs(true)
                .withResolveCombinators(true) // Inline to avoid problems with allOf
                .withLevelResolver(buildLevelResolver(configuration))
                .build();
            return new SingleSpecOpenApiInteractionValidatorWrapper(validator);
        } catch (Throwable e) {
            log.error("Could not initialize OpenApiInteractionValidator [validation disabled]", e);
            return null;
        }
    }

    @NonNull
    private Optional<String> loadOpenAPISpec(String specificationFilePath) {
        return loadSpecFromPath(specificationFilePath)
            .or(() -> loadSpecFromResources(specificationFilePath))
            .or(() -> loadSpecFromResources("openapi.yaml"))
            .or(() -> loadSpecFromResources("openapi.json"))
            .or(() -> loadSpecFromResources("spec.yaml"))
            .or(() -> loadSpecFromResources("spec.json"));
    }

    private Optional<String> loadSpecFromPath(String path) {
        if (path == null) {
            return Optional.empty();
        }

        try {
            File file = new File(path);
            if (!file.exists()) {
                return Optional.empty();
            }

            return Optional.of(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Error while loading spec from " + path, e);
            return Optional.empty();
        }
    }

    private Optional<String> loadSpecFromResources(String resourceFileLocation) {
        if (resourceFileLocation == null) {
            return Optional.empty();
        }

        try {
            ClassLoader classLoader = OpenApiRequestValidator.class.getClassLoader();
            try (var is = classLoader.getResourceAsStream(resourceFileLocation)) {
                if (is == null) {
                    return Optional.empty();
                }

                try (
                    var isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                    var reader = new BufferedReader(isr)
                ) {
                    return Optional.of(reader.lines().collect(Collectors.joining(System.lineSeparator())));
                }
            }
        } catch (Throwable e) {
            log.warn("Error while loading spec from resource " + resourceFileLocation, e);
            return Optional.empty();
        }
    }

    private LevelResolver buildLevelResolver(ValidatorConfiguration configuration) {
        var builder = LevelResolver.create();
        if (configuration.getLevelResolverLevels() != null && !configuration.getLevelResolverLevels().isEmpty()) {
            builder.withLevels(
                configuration.getLevelResolverLevels().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> mapLevel(entry.getValue()).orElse(ValidationReport.Level.INFO)))
            );
        }
        return builder
            // this will cause all messages to be warn by default
            .withDefaultLevel(mapLevel(configuration.getLevelResolverDefaultLevel()).orElse(ValidationReport.Level.INFO))
            .build();
    }

    private Optional<ValidationReport.Level> mapLevel(LogLevel level) {
        if (level == null) {
            return Optional.empty();
        }

        return switch (level) {
            case ERROR -> Optional.of(ValidationReport.Level.ERROR);
            case WARN -> Optional.of(ValidationReport.Level.WARN);
            case INFO -> Optional.of(ValidationReport.Level.INFO);
            case IGNORE -> Optional.of(ValidationReport.Level.IGNORE);
        };
    }
}
