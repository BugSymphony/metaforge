---
id: foundation-core-platform-capabilities
protocol: Library API
version: 1.0.0
owner: foundation-core
description: Declaration of platform capabilities automatically enabled by foundation-core that business BCs must not reimplement
type: foundation
---

## Capability Inventory

### 1. Virtual Threads

**Source**: FR-006

**Description**: All platform HTTP request processing thread pools and `@Async` task thread pools use Java 21 virtual threads. Tomcat request handling and `@Async` annotated methods automatically run on virtual threads.

**BC Constraint**: BC must not configure any thread pools. BC must not customize `ThreadPoolTaskExecutor` or override the `TaskExecutor` bean.

---

### 2. Log Masking

**Source**: FR-010

**Description**: Log output automatically masks sensitive fields (password, secret, token, phone, email, idCard, etc.). Logback/log4j2 format is standardized with TraceId, timestamp, and thread name.

**BC Constraint**: BC must not configure log masking rules (common sensitive fields are already covered). For custom masking rules, implement `LogMaskSpi` extension point. BC must not create its own log masking utilities.

---

### 3. API Documentation (OpenAPI 3.0 / SpringDoc)

**Source**: FR-014

**Description**: All REST interfaces automatically generate OpenAPI 3.0 documentation. Swagger UI accessible via `/swagger-ui.html`. BCs annotate controllers with `@Tag(name = "<bc-name>")` for Swagger UI grouping.

**BC Constraint**: BC must not configure SpringDoc or introduce additional API documentation libraries. BC must not add `springdoc-openapi` dependency or customize `OpenAPI` bean.

---

### 4. Internationalization (i18n)

**Source**: FR-015

**Description**: Global `MessageSource` is pre-configured with resource file path `i18n/messages`, default support for `zh-CN` and `en-US`. `Accept-Language` header auto-detection. Spring `MessageSource` bean is ready for BC injection.

**BC Constraint**: BC must not configure `MessageSource`. To extend i18n messages, add `messages_<bc-name>_<locale>.properties` files under `metaforge-boot/src/main/resources/i18n/`. BC must not define independent `MessageSource` beans.

---

### 5. Observability (Actuator + Metrics)

**Source**: FR-016

**Description**: Exposed endpoints: `/actuator/health`, `/actuator/info`, `/actuator/metrics`. JVM metrics (memory, GC, threads) and HTTP request metrics (request count, response time distribution) are auto-collected.

**BC Constraint**: BC must not configure Actuator. For custom health checks, implement `HealthCheckSpi`. BC must not customize Actuator endpoint paths or override default metric registrations.

---

### 6. Security Baseline

**Source**: FR-017

**Description**: XSS filter is configured with 10MB request body size limit. CORS is standardized (same-origin by default; cross-origin configured via `application.yml`). SQL injection protection is natively provided by JPA/Hibernate parameterized queries.

**BC Constraint**: BC must not configure security filters. For custom CORS rules, configure in `metaforge-boot/application.yml`. BC must not add `CorsFilter` or `WebMvcConfigurer.addCorsMappings()` internally.

---

### 7. Data Source (HikariCP Connection Pool + Transaction Management)

**Source**: FR-018

**Description**: PostgreSQL data source is pre-configured with HikariCP connection pool. Transaction manager is configured. `@Transactional` annotation works by default. Flyway executes database migrations automatically at startup.

**BC Constraint**: BC must not configure data source or transaction manager. All database connections use the unified data source. BC must not define independent data source beans. Multi-BC logical isolation via separate schemas.

---

### 8. Cross-Schema Write Validation

**Source**: FR-020

**Description**: Provides cross-schema write operation validation, enforcing single data ownership principle (a BC can only write to its own schema). Schemas are created per BC during Flyway initialization.

**BC Constraint**: BC can only execute DML operations (INSERT/UPDATE/DELETE) within its own schema. Cross-BC schema SELECT queries are allowed within declared contract field scope.

---

### 9. Flyway Database Migration Unified Management

**Source**: FR-020a

**Description**: Flyway automatically executes all BC migration scripts in global version order at application startup. Migration scripts are stored in `metaforge-boot/src/main/resources/db/migration/` using a flat directory structure.

**BC Constraint**: BC must not configure Flyway or introduce flyway-core dependency. Each BC provides migration scripts following the `V<n>__<bc-name>_ddl.sql` and `V<n+1>__<bc-name>_init.sql` naming convention, submitted to `metaforge-boot/db/migration/`.

---

### 10. Test Base (BaseUnitTest / BaseIntegrationTest + TestContainers)

**Source**: FR-028 / FR-029

**Description**: `metaforge-framework` provides `BaseUnitTest` (pure unit test base, no Spring context, Mockito-based) and `BaseIntegrationTest` (integration test base with built-in TestContainers PostgreSQL container auto-start/stop, Spring Boot Test context). Business BCs inherit these via test-scope `metaforge-framework` dependency.

**BC Constraint**: BC must not introduce TestContainers dependencies or configure test data sources. BC must not customize PostgreSQL containerized test infrastructure. Test configuration auto-isolates from production configuration.
