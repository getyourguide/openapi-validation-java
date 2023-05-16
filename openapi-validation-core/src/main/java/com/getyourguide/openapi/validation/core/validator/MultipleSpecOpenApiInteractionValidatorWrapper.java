package com.getyourguide.openapi.validation.core.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

public class MultipleSpecOpenApiInteractionValidatorWrapper implements OpenApiInteractionValidatorWrapper {
    public static final String MESSAGE_KEY_VALIDATOR_FOUND = "zopenapi-validator-java.noValidatorFound";
    private final List<Pair<Pattern, OpenApiInteractionValidatorWrapper>> validators;

    public MultipleSpecOpenApiInteractionValidatorWrapper(
        List<Pair<Pattern, OpenApiInteractionValidatorWrapper>> validators
    ) {
        assert validators != null && validators.size() > 0;

        this.validators = validators;
    }

    @Override
    public ValidationReport validateRequest(SimpleRequest request) {
        return getValidatorForPath(request.getPath())
            .map(validator -> validator.validateRequest(request))
            .orElse(new SimpleValidationReport(List.of(buildNoValidatorFoundMessage(request.getPath()))));
    }

    @Override
    public ValidationReport validateResponse(String path, Request.Method method, SimpleResponse response) {
        return getValidatorForPath(path)
            .map(validator -> validator.validateResponse(path, method, response))
            .orElse(new SimpleValidationReport(List.of(buildNoValidatorFoundMessage(path))));
    }

    private Optional<OpenApiInteractionValidatorWrapper> getValidatorForPath(String path) {
        for (var validator : validators) {
            if (validator.getLeft().matcher(path).matches()) {
                return Optional.of(validator.getRight());
            }
        }

        return Optional.empty();
    }

    private static SimpleMessage buildNoValidatorFoundMessage(String path) {
        return new SimpleMessage(
            MESSAGE_KEY_VALIDATOR_FOUND,
            "No validator found for path: " + path,
            ValidationReport.Level.WARN
        );
    }

    @AllArgsConstructor
    private static class SimpleValidationReport implements ValidationReport {
        private final List<Message> messages;

        @Nonnull
        @Override
        public List<Message> getMessages() {
            return messages;
        }

        @Override
        public ValidationReport withAdditionalContext(MessageContext context) {
            return this;
        }
    }

    @AllArgsConstructor
    private static class SimpleMessage implements ValidationReport.Message {
        private final String key;
        private final String message;
        private final ValidationReport.Level level;

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public ValidationReport.Level getLevel() {
            return level;
        }

        @Override
        public List<String> getAdditionalInfo() {
            return List.of();
        }

        @Override
        public Optional<ValidationReport.MessageContext> getContext() {
            return Optional.empty();
        }

        @Override
        public ValidationReport.Message withLevel(ValidationReport.Level level) {
            return this;
        }

        @Override
        public ValidationReport.Message withAdditionalInfo(String info) {
            return this;
        }

        @Override
        public ValidationReport.Message withAdditionalContext(ValidationReport.MessageContext context) {
            return this;
        }

        @Override
        public String toString() {
            return message;
        }
    }
}
