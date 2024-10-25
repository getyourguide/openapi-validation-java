# Change Log

[//]: # (https://keepachangelog.com/en/1.1.0/)

## [Unreleased]

## [2.2.2] - 2024-10-25

### Changed

- Support request bodies on all methods ([#148](https://github.com/getyourguide/openapi-validation-java/pull/148))
- Updated several dependencies

## [2.2.1] - 2024-09-24

### Fixed

- Ignore unknownError related to ConcurrentModificationException ([#137](https://github.com/getyourguide/openapi-validation-java/pull/137))

### Changed

- Updated several dependencies

## [2.2.0] - 2024-08-27

### Added

- Exclude graphql endpoints from validation by default ([#129](https://github.com/getyourguide/openapi-validation-java/pull/129))

### Changed

- Updated several dependencies

## [2.1.0] - 2024-06-03

### Added

- Provide custom MetricTags during runtime ([#111](https://github.com/getyourguide/openapi-validation-java/pull/111))

### Changed

- Updated several dependencies

## [2.0.3] - 2024-05-26

### Changed

- Ignore request violations with response status code 4xx ([#108](https://github.com/getyourguide/openapi-validation-java/pull/108))
- Updated several dependencies

## [2.0.2] - 2024-03-19

### Changed

- ignore unexpected body on GET/DELETE/OPTIONS/HEAD/TRACE ([#91](https://github.com/getyourguide/openapi-validation-java/pull/91))

### Fixed

- undertow can't validate request body ([#90](https://github.com/getyourguide/openapi-validation-java/pull/90))
- Handle invalid spec on a spec with multiple spec setup ([#79](https://github.com/getyourguide/openapi-validation-java/pull/79))
 
## [2.0.1] - 2023-11-29

### Fixed

- Possible NullPointerException ([#71](https://github.com/getyourguide/openapi-validation-java/pull/71))

## [2.0] - 2023-11-28

### Changed

- Use strict operation path matching ([#52](https://github.com/getyourguide/openapi-validation-java/pull/52))
- Default sample-rate to 100% ([#55](https://github.com/getyourguide/openapi-validation-java/pull/55))
- Use toml file for versions of dependencies ([#54](https://github.com/getyourguide/openapi-validation-java/pull/54))

### Removed

- Removed spring boot 2.7 support ([#53](https://github.com/getyourguide/openapi-validation-java/pull/53))

### Fixed

- Exclude 406 status code violations ([#48](https://github.com/getyourguide/openapi-validation-java/pull/48))
- On exception log 500 status code instead of 200 status code ([#65](https://github.com/getyourguide/openapi-validation-java/pull/65))
- Do not report violation on blocked request ([#66](https://github.com/getyourguide/openapi-validation-java/pull/66))

## [1.2.7] - 2023-10-23

### Changed

- Log spec file that was not found ([#44](https://github.com/getyourguide/openapi-validation-java/pull/44))
- Bump com.atlassian.oai:swagger-request-validator-core ([#42](https://github.com/getyourguide/openapi-validation-java/pull/42))
- Improve logging when there is no validator found for a path ([#40](https://github.com/getyourguide/openapi-validation-java/pull/40))

### Fixed

- Ignore violation `operation.notAllowed` with status code 404 ([#45](https://github.com/getyourguide/openapi-validation-java/pull/45))
- Ignore violation `operation.notAllowed` with status code 405 ([#43](https://github.com/getyourguide/openapi-validation-java/pull/43))

## [1.2.6] - 2023-08-30

### Changed

- Add response status code to violation log message ([#35](https://github.com/getyourguide/openapi-validation-java/pull/35))
- Bump com.atlassian.oai:swagger-request-validator-core ([#34](https://github.com/getyourguide/openapi-validation-java/pull/34))

### Fixed

- Exclude "matched 2 out of 25" in the oneOf rule ([#36](https://github.com/getyourguide/openapi-validation-java/pull/36))

## [1.2.5] - 2023-08-28

### Changed

- Ignore unexpected query parameter violation ([#31](https://github.com/getyourguide/openapi-validation-java/pull/31))

## [1.2.4] - 2023-08-24

### Added

- Send sample-rate and throttling information with startup metric ([#28](https://github.com/getyourguide/openapi-validation-java/pull/28))
- Allow configuring log level ([#26](https://github.com/getyourguide/openapi-validation-java/pull/26))

### Fixed

- Exclude false positive violations ([#24](https://github.com/getyourguide/openapi-validation-java/pull/24)) 
- Do not validate HEAD requests ([#27](https://github.com/getyourguide/openapi-validation-java/pull/27))

### Removed

- Remove heartbeat metric ([#25](https://github.com/getyourguide/openapi-validation-java/pull/25))

## [1.2.3] - 2023-07-17

### Fixed

- Update `swagger-request-validator-core` to `2.35.2` for bug fixes ([#21](https://github.com/getyourguide/openapi-validation-java/pull/21))
  - [bugfix: not required and exploded query parameters handling](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/391/bugfix-not-required-and-exploded-query)
  - [bugfix: Fix path double-normalized if same prefix as base path](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/390/bugfix-fix-path-double-normalized-if-same) 

## [1.2.2] - 2023-06-30

### Added

- Show parameter name in violation log ([#19](https://github.com/getyourguide/openapi-validation-java/pull/19))

### Fixed

- Decode query parameters before validation to avoid problems with comma separated lists ([#18](https://github.com/getyourguide/openapi-validation-java/pull/18))

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


[unreleased]: https://github.com/getyourguide/openapi-validation-java/compare/v2.2.2...HEAD
[2.2.2]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v2.2.2
[2.2.1]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v2.2.1
[2.2.0]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v2.2.0
[2.1.0]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v2.1.0
[2.0.3]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v2.0.3
[2.0.2]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v2.0.2
[2.0.1]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v2.0.1
[2.0]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v2.0
[1.2.7]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.2.7
[1.2.6]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.2.6
[1.2.5]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.2.5
[1.2.4]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.2.4
[1.2.3]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.2.3
[1.2.2]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.2.2
[1.2.1]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.2.1
[1.2.0]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.2.0
[1.1.2]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.1.2
[1.1.1]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.1.1
[1.1.0]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.1.0
[1.0.2]: https://github.com/getyourguide/openapi-validation-java/releases/tag/v1.0.2
