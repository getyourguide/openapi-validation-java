# Change Log

[//]: # (https://keepachangelog.com/en/1.1.0/)

## [Unreleased]

## [1.2.1] - 2023-06-20

### Added

- Send hourly heartbeat (as metric) when validation is happening ([#15](https://github.com/getyourguide/openapi-validation-java/pull/15))

## [1.2.0] - 2023-06-15

### Added

- Send metric on startup with info if validation is enabled ([#12](https://github.com/getyourguide/openapi-validation-java/pull/12))

### Fixed

- Catch RejectedExecutionException and skip validation ([#13](https://github.com/getyourguide/openapi-validation-java/pull/13))

## [1.1.2] - 2023-06-09

### Fixed

- Disable validation instead of failing (multiple spec files) ([#9](https://github.com/getyourguide/openapi-validation-java/pull/9))

## [1.1.1] - 2023-06-01

### Fixed

- Fix problem with validating partial bodies instead of whole body ([#7](https://github.com/getyourguide/openapi-validation-java/pull/7))
- Fix wrong url in publishing ([commit](https://github.com/getyourguide/openapi-validation-java/commit/48e39d506e73cdd9df71f311cf1a3b8ff8e7d5c8))

## [1.1.0] - 2023-05-26

### Added

- Allow excluding traffic by matching headers ([#5](https://github.com/getyourguide/openapi-validation-java/pull/5))
- Allow excluding certain violations from being reported ([#4](https://github.com/getyourguide/openapi-validation-java/pull/4)).
- Support fail on request/response violation ([#3](https://github.com/getyourguide/openapi-validation-java/pull/3)).
- Support multiple spec files ([#2](https://github.com/getyourguide/openapi-validation-java/pull/2)).

## [1.0.2] - 2023-05-12

### Added

- Initial squashed GitHub public release.


[unreleased]: https://github.com/getyourguide/openapi-validation-java/compare/v1.2.1...HEAD
[1.2.1]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.2.1
[1.2.0]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.2.0
[1.1.2]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.1.2
[1.1.1]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.1.1
[1.1.0]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.1.0
[1.0.2]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.0.2
