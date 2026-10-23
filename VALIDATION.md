# Verification record

Validated on 23 October 2026 with OpenJDK 17 and Maven 3.9.9.

- `mvn clean verify`: BUILD SUCCESS.
- 44 tests executed: 0 failures, 0 errors, 0 skipped.
- 35 Mockito service tests across ProjectService, UserService and TaskService.
- 2 pagination tests and 7 Spring Boot / MockMvc integration tests using H2.
- OpenAPI 3.0.3 schema validated with `openapi-spec-validator`.
- Packaged executable JAR startup and `/api/info` smoke test passed.
- Dockerfile supplied; Docker execution was not tested because Docker was
  unavailable in the build environment.

Tests use Mockito's subclass mock maker (`src/test/resources/mockito-extensions/`)
to avoid JVM agent self-attachment requirements. They mock repository interfaces;
final-class or static mocking is unnecessary.

To reproduce: install JDK 17+ and Maven 3.6.3+, then run `mvn clean verify` from
the project root. Maven downloads dependencies on first use.
