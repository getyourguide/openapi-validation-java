package com.getyourguide.openapi.validation.api.exclusions;

import java.util.regex.Pattern;

public record ExcludedHeader(String headerName, Pattern headerValuePattern) { }
