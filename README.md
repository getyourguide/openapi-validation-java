[![GHA check status](https://github.com/getyourguide/openapi-validation-java/actions/workflows/check.yml/badge.svg)](https://github.com/getyourguide/openapi-validation-java/actions?query=branch%3Amain)

# Java OpenAPI Validation Library

Build trust in your spec by validating live requests and responses against your OpenAPI spec.
Get informed through logs or metrics when violations occur.

Uses the following library for the validation: https://bitbucket.org/atlassian/swagger-request-validator

## Installation

1. [Add dependency in gradle](#gradle)
2. [Provide OpenAPI specification file](#provide-openapi-specification-file)

### Gradle
#### Spring 3
There are separate integrations for `spring-boot-starter-web` (servlet) and `spring-boot-starter-webflux` (reactive).

##### spring-boot-starter-web (servlet)
```groovy
dependencies {
    implementation "com.getyourguide.openapi.validation:spring-boot-starter-web:{latest-version}"
}
```

##### spring-boot-starter-webflux (reactive)
```groovy
dependencies {
    implementation "com.getyourguide.openapi.validation:spring-boot-starter-webflux:{latest-version}"
}
```

### Provide OpenAPI specification file
The library will require a valid OpenAPI specification file to be present in the classpath.

By default, it will look for `openapi.yaml`, `openapi.json`, `spec.yaml` or `spec.json`.

Do one of the following.
- Copy spec into `src/main/resources`
- Generate a single spec file that you add to `src/main/resources`

### Copy spec into `src/main/resources`
Copy all the spec files to `src/main/resources` and make sure that the main file has a correct name as mentioned
above. This can be done as part of the CI pipeline.

### Generate a single spec file
Use [openapi-generator](https://github.com/OpenAPITools/openapi-generator) to generate a single
`openapi.yaml` or `openapi.json` file and add that to `src/main/resources`.
See [openapi-generator Installation](https://github.com/OpenAPITools/openapi-generator#1---installation) for more
information on how to use it with various methods (artifact, docker, brew, npm, ...).

One example for installing openapi-generator with brew and generating a single spec file:

```shell
brew install openapi-generator
openapi-generator generate -g openapi -i spec/index.yaml -o /tmp/openapi-spec/
cp /tmp/openapi-spec/openapi.json src/main/resources/openapi.json
```
Please **adjust** the commands to fit your **folder structure**.

## Configuration

Without any configuration the library will
- Search for specification file of name (`openapi.yaml/json` or `spec.yaml/json`) in resources
- Validate 0.1% of all requests

### application.properties
The following configuration can be done within the `application.properties`.

```properties
# Control the percentage of requests that are validated. The default is 0.1% of traffic.
# Here set to validate 100% of traffic
openapi.validation.sample-rate=1.0

# Custom location of specification file within resources or filesystem.
openapi.validation.specification-file-path=/tmp/openapi-spec/openapi.json
# If it is within src/main/resources/folder/my-spec.json use
openapi.validation.specification-file-path=folder/my-spec.json

# Custom log level for violations (ignore, info, warn, error)
# Default: info
openapi.validation.violation-log-level=error

# Comma separated list of paths to be excluded from validation. Default is no excluded paths
openapi.validation.excluded-paths=/_readiness,/_liveness,/_metrics
# Allows to exclude requests based on headers. Default is no excluded headers.
# Each entry is the header plus a matching regex. The regex is case insensitive.
openapi.validation.excluded-headers[0]=User-Agent: .*(bingbot|googlebot).*

# Throttle the validation reporting (logs & metrics) to a maximum of 1 log/metric per 10 seconds.
# Default is null which results in no throttling.
openapi.validation.validation-report-throttle-wait-seconds=10

# Throttle the validation reporting (logs & metrics) to a maximum of 1 log/metric per 10 seconds.
# Default is "openapi.validation".
openapi.validation.validation-report-metric-name=validation.openapi

# Add additional tags to be logged with metrics. They should be in the format {KEY}={VALUE},{KEY}={VALUE}
# Default is no additional tags.
openapi.validation.validation-report-metric-additional-tags=service=example,team=chk

# Fail requests on request/response violations. Defaults to false.
openapi.validation.should-fail-on-request-violation=true
openapi.validation.should-fail-on-response-violation=true

# Enable virtual threads for async validation. Defaults to false.
openapi.validation.enable-virtual-threads=true
```

### DataDog metrics
To use DataDog metrics, you need to add the following dependency to your `build.gradle`:

```groovy
dependencies {
    implementation "com.getyourguide.openapi.validation:metrics-reporter-datadog-spring-boot:{latest-version}"
}
```

By default, the existing `StatsDClient` bean will be used to log metrics.
If that bean doesn't exist it will not log any metrics.

It is possible to configure the `StatsDClient` to be used with the following properties:

```properties
openapi.validation.datadog.statsd.service.host=localhost
openapi.validation.datadog.statsd.service.port=8125
```

### Logging
#### LoggingContext
```java
@Component
public class ExampleLoggerExtension implements LoggerExtension {
    @Override
    public Closeable addToLoggingContext(@NonNull Map<String, String> newTags) {
        return LoggingContext.putAll(newTags);
    }
}
```

#### Custom Logging
```java
@Slf4j
@Component
public class CustomViolationLogger implements ViolationLogger {
  @Override
  public void log(OpenApiViolation violation) {
    log.error("!!! Spec Violation on {}", violation.getRequestMetaData().getUri());
  }
}
```

### Custom traffic selection
Define which traffic should be validated. This can be used to only include specific traffic or
to enable validation based on a feature flag or experiment. 
```java
@Component
public class SampleRateTrafficSelector implements TrafficSelector {

    private final FeatureFlag featureFlags;
    
    public SampleRateTrafficSelector(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }
    
    @Override
    public boolean shouldRequestBeValidated(RequestMetaData requestMetaData) {
        return featureFlags.isEnabled("MY_FEATURE_FLAG");
    }
}
```

### Custom log levels
One can customize log levels as per the following example. The default log level is `info`.

The key to be used here is also printed in the violation log message.

```java
@Configuration
public class ValidatorConfiguration {
    @Bean
    public ValidatorConfiguration buildValidatorConfiguration() {
        return new ValidatorConfigurationBuilder()
            .levelResolverLevel("validation.request.body.schema.additionalProperties", LogLevel.ERROR)
            .levelResolverDefaultLevel(LogLevel.INFO)
            .build();
    }
}
```

### Multiple spec files
It is possible to use multiple spec files for different paths. This can be achieved as demonstrated in the following
code snipped.

It is best practice to use a catch-all spec file. If a request is not matching any of the paths defined here it will
result in a violation error with log level `warn`. 

```java
@Configuration
public class OpenApiValidatorConfiguration {
    @Bean
    public ValidatorConfiguration buildValidatorConfiguration() {
        return new ValidatorConfigurationBuilder()
            .specificationPath(Pattern.compile("/v1/.*"), "openapi-v1.yaml")
            .specificationPath(Pattern.compile("/.*"), "openapi.yaml")
            .build();
    }
}
```

### Exclude certain violations
Certain violations can get excluded from reporting. This can be achieved as demonstrated in the following snippet.

**Note:** Only use this where it is really needed. It is best practice to fix the actual violations by either adjusting
the specification, the server implementation, or the client sending wrong requests.

```java
@Component
public class ViolationExclusionsExample implements ViolationExclusions {
  @Override
  public boolean isExcluded(OpenApiViolation violation) {
    return violation.getDirection().equals(Direction.REQUEST)
            && violation.getMessage().equals("[Path '/name'] Instance type (integer) does not match any allowed primitive type (allowed: [\"string\"])");
  }
}
```

### Provide metric tags at runtime
Sometimes you want to generate your own metric tags based on the violation.
This can be achieved as demonstrated in the following snippet.

```java
@Component
public class MetricTagProviderExample implements MetricTagProvider {
  @Override
  public List<MetricTag> getTagsForViolation(OpenApiViolation violation) {
    return List.of(new MetricTag("rule", violation.getRule()));
  }
}
```

## Examples
Run examples with `./gradlew :examples:example-spring-boot-starter-web:bootRun` or `./gradlew :examples:example-spring-boot-starter-webflux:bootRun`.

## Current known limitations
These are current known limitations of the library.
Any help on resolving these is appreciated. PRs are always welcome.

- Only supports Content-Type JSON/XML/HTML
  - In order to avoid accidentally caching bigger resources (videos, streams, ...)
- Ignores the error `Instance failed to match exactly one schema (matched X out of Y)`
  - This is currently on purpose, but should be configurable
- Error responses can't (yet) be validated in webflux/reactive

## Versioning

The library is following [Semantic Versioning](https://semver.org/).

### Dependency updates
Since most of the updates to the library are dependency updates, we also follow their semver changes as well.
As there will be (most probably) a multiple dependencies updated with a version we release, we always consider
the "highest" version bump (from semver perspective) as the decisioning factor.

- A `major` version of this library indicates at least one of the dependencies has had a `major` release.
- Similarly, a `minor` version bump indicates that there was no `major` dependency update, only `minor` ones.
