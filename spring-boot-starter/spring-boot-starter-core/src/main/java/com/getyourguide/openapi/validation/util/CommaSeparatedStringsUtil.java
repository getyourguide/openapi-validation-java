package com.getyourguide.openapi.validation.util;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommaSeparatedStringsUtil {

    public static Set<String> convertCommaSeparatedStringToSet(String excludedPaths) {
        if (excludedPaths == null || excludedPaths.isEmpty()) {
            return Set.of();
        }

        return Stream.of(excludedPaths.trim().split(",")).map(String::trim).filter(excludedPath -> !excludedPath.isEmpty()).collect(Collectors.toSet());
    }
}
