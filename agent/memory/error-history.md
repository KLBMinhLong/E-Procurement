# Error History (Những lỗi đã xảy ra — agent phải tránh)

## [2026-05-18] Bug: Fresh Docker recreate failed after Keycloak provider merge

- Root cause: IAM Docker build copied only `services/iam-service/pom.xml`, while the root Maven reactor now references `infra/keycloak/eprocure-keycloak-provider`.
- Fix: Copy the provider module POM in `services/iam-service/Dockerfile` before dependency resolution.
- Prevention: When adding a Maven reactor module, update every Dockerfile that runs root-level Maven goals.

## [2026-05-18] Bug: Keycloak realm import rejected User Storage component

- Root cause: `realm-eprocure.json` exported `providerType`, which Keycloak 24 does not accept in `ComponentExportRepresentation` during import.
- Fix: Remove `providerType` from the user-storage component; keep `providerId`, `name`, `subType`, and config.
- Prevention: Validate realm JSON with a fresh volume recreate, not only against an already-imported local realm.

## [2026-05-18] Bug: IAM startup failed at `sessionMapper` / `sqlSessionTemplate`

- Root cause: MyBatis XML result mapping for delegation UUID fields did not declare a PostgreSQL UUID type handler, so mapper creation failed and cascaded into `SessionAuthenticationFilter` dependency creation.
- Fix: Add `UuidTypeHandler`, register it in `application.yml`, and declare explicit UUID mappings in `DelegationMapper.xml`.
- Prevention: For XML mapper result maps using UUID columns, declare `javaType`, `jdbcType=OTHER`, and the UUID type handler explicitly.

## [2026-05-18] Bug: IAM startup failed on `TotpService` constructor binding

- Root cause: `TotpService` had both a production issuer constructor and a package-private test constructor, so Spring could not infer the bean constructor safely.
- Fix: Keep `TotpService` as an application service class and expose it through `TotpServiceConfig` with the configured issuer value.
- Prevention: Prefer explicit config beans when a service class needs multiple constructors for test seams.

## [2026-05-18] Bug: Login request JSON failed to deserialize after domain mapper customization

- Root cause: The field-visibility `domainObjectMapper` became the only `ObjectMapper` bean, so Spring MVC used it for API records such as `LoginRequest`.
- Fix: Add a primary API `ObjectMapper` and keep the field-visibility mapper under `domainObjectMapper`.
- Prevention: Keep domain/entity conversion mappers qualified so MVC serialization keeps normal Jackson record support.

## [2026-05-18] Bug: Keycloak direct grant returned `Account is not fully set up`

- Root cause: The custom user adapter exposed profile getters but did not expose first name, last name, email, and emailVerified through Keycloak attributes used by required actions/profile checks.
- Fix: Override `getFirstAttribute` and `getAttributes` in `EprocureIamUserAdapter`.
- Prevention: Validate Keycloak User Storage SPI with a direct-grant token request after realm recreate.

## [2026-05-18] Bug: Login failed while inserting session IP address

- Root cause: `iam.sessions.ip_address` is PostgreSQL `INET`, but `SessionMapper.save` bound the value as VARCHAR.
- Fix: Cast the bound IP string with `CAST(#{session.ipAddress,jdbcType=VARCHAR} AS INET)` in the insert statement.
- Prevention: Match mapper parameter binding to PostgreSQL-specific column types during local end-to-end login tests.
