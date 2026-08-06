---
id: foundation-core-configuration-schema
protocol: Library API
version: 1.0.0
owner: foundation-core
description: Complete configuration properties reference for foundation-core covering datasource, cache, Jackson, virtual threads, Flyway, Actuator, and OpenAPI settings
type: foundation
---

All configuration items use Spring Boot native key names; no `metaforge.*` custom namespace. Configurations are centralized under `metaforge-boot/src/main/resources/` (`application.yml` or per-BC `application-<bc-name>.yml`).

## 1. Data Source Configuration

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.datasource.url` | String | `jdbc:postgresql://localhost:5432/metaforge` | valid JDBC URL | PostgreSQL connection URL |
| `spring.datasource.username` | String | `metaforge` | — | Database username |
| `spring.datasource.password` | String | `metaforge` | — | Database password |
| `spring.datasource.driver-class-name` | String | `org.postgresql.Driver` | PostgreSQL driver | JDBC driver class |
| `spring.datasource.hikari.maximum-pool-size` | int | 10 | 1-100 | Max connection pool size |
| `spring.datasource.hikari.minimum-idle` | int | 5 | 0-`maximum-pool-size` | Min idle connections |
| `spring.datasource.hikari.idle-timeout` | long | 300000 | >= 10000 | Idle connection timeout (ms) |
| `spring.datasource.hikari.connection-timeout` | long | 20000 | >= 1000 | Connection acquisition timeout (ms) |
| `spring.datasource.hikari.max-lifetime` | long | 1200000 | >= 30000 | Max connection lifetime (ms) |

## 2. Cache Configuration

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.cache.type` | String | `caffeine` | `caffeine` \| `none` | Cache type (MVP supports only Caffeine) |
| `spring.cache.caffeine.spec` | String | `expireAfterWrite=30m,maximumSize=1000,recordStats` | Caffeine spec string | Cache spec (TTL, capacity, stats) |

**Caffeine spec format**: `expireAfterWrite=<duration>,maximumSize=<size>,recordStats`
- `expireAfterWrite`: Time-to-live after write (d/h/m/s units)
- `maximumSize`: Max cache entries
- `recordStats`: Enable cache stats exposure to Actuator metrics

## 3. Jackson Serialization Configuration

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.jackson.date-format` | String | `yyyy-MM-dd HH:mm:ss` | SimpleDateFormat pattern | Global date/time format |
| `spring.jackson.time-zone` | String | `Asia/Shanghai` | Java TimeZone ID | Global timezone |
| `spring.jackson.default-property-inclusion` | String | `non_null` | `always` \| `non_null` \| `non_absent` \| `non_empty` \| `non_default` \| `use_defaults` | Property inclusion strategy |
| `spring.jackson.serialization.write-dates-as-timestamps` | boolean | false | — | false=string format, true=timestamp |

## 4. Virtual Thread Configuration

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.threads.virtual.enabled` | boolean | true | — | Enable virtual threads globally |

**Scope**: Tomcat request processing, `@Async` tasks, `@Scheduled` tasks

## 5. Flyway Configuration

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.flyway.enabled` | boolean | true | — | Enable Flyway migration |
| `spring.flyway.locations` | String | `classpath:db/migration` | classpath paths | Migration script path |
| `spring.flyway.baseline-on-migrate` | boolean | true | — | Baseline non-empty database |
| `spring.flyway.baseline-version` | String | `1` | — | Baseline version number |
| `spring.flyway.validate-on-migrate` | boolean | true | — | Validate checksum before migration |

## 6. Server Port Configuration

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `server.port` | int | 8080 | 1-65535 | Application HTTP port |

## 7. Actuator Observability Configuration

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `management.endpoints.web.exposure.include` | String | `health,info,metrics` | comma-separated endpoint IDs | Exposed Actuator endpoints |
| `management.endpoint.health.show-details` | String | `when-authorized` | `never` \| `when-authorized` \| `always` | Health detail display policy |

## 8. SpringDoc OpenAPI Configuration

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `springdoc.swagger-ui.tags-sorter` | String | `alpha` | `alpha` \| `order` | Swagger UI tag sort method |
| `springdoc.api-docs.path` | String | `/v3/api-docs` | valid path | OpenAPI JSON doc path |

## 9. Security Baseline Configuration

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `server.max-http-request-header-size` | String | `8KB` | — | Max request header size |
| `spring.servlet.multipart.max-file-size` | String | `10MB` | — | Max file upload size |
| `spring.servlet.multipart.max-request-size` | String | `10MB` | — | Max request body size |

## 10. Internationalization Configuration

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.messages.basename` | String | `i18n/messages` | classpath paths | i18n resource file path |
| `spring.messages.encoding` | String | `UTF-8` | — | Resource file encoding |
| `spring.messages.fallback-to-system-locale` | boolean | false | — | Fallback to system locale if no match |
| `spring.messages.use-code-as-default-message` | boolean | true | — | Use key name as default message if no match |

## Configuration Organization

### Single application.yml (recommended for <5 BCs)

All configurations centralized in `metaforge-boot/src/main/resources/application.yml`.

### Per-BC Split (recommended for >=5 BCs)

- `application.yml`: Global infrastructure configuration (datasource, cache, port, etc.)
- `application-<bc-name>.yml`: BC-specific configuration
- Import via `spring.config.import` in `application.yml`:

```yaml
spring:
  config:
    import:
      - classpath:application-bc-sample.yml
      - classpath:application-bc-user.yml
```
