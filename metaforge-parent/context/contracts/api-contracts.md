---
id: foundation-core-api-contracts
protocol: Library API
version: 1.0.0
owner: foundation-core
description: Core API contracts for foundation-core including SPI extension points, public DTOs, library APIs, exception hierarchy, parameter validation, and runtime behavior specifications
type: foundation
---

## 1. SPI Extension Point Interfaces

foundation-core defines six extension point interfaces in the `com.metaforge.common.spi` package. Business BCs extend base behavior by implementing these interfaces and registering them as Spring Beans.

### Extension Point Overview

| Extension Point | Interface | Trigger | Purpose |
|----------------|-----------|---------|---------|
| Exception Handling | `ExceptionHandlerSpi` | When global exception handler cannot match exception type | BC registers custom business exception to error response mapping |
| Request Interception | `RequestInterceptorSpi` | Before/after HTTP requests | BC injects custom request pre/post processing logic |
| Log Masking | `LogMaskSpi` | Before log output | BC defines custom field masking rules |
| Health Check | `HealthCheckSpi` | When Actuator `/health` endpoint is accessed | BC registers custom health check items |
| Serialization | `SerializationSpi` | During Jackson ObjectMapper configuration phase | BC registers custom serializer/deserializer |
| Parameter Validation | `ValidationSpi` | During Validator initialization | BC registers custom validation annotations |

### SPI Lifecycle

```
Discovery -> Loading -> Ordering -> Invocation
```

- **Discovery**: Spring automatically scans all beans implementing SPI interfaces
- **Loading**: Instantiate extensions and inject dependencies
- **Ordering**: Sort by `@Order` or `@Priority` annotation (lower value = higher priority)
- **Invocation**: Chain/aggregate invocation, behavior depends on extension point type

### BC-Level Isolation

- Each SPI extension implementation only registers within its `@ComponentScan` range Spring context
- Extensions isolated by module jar boundaries
- Verification: CI executes cross-BC extension isolation tests

### Interface Signatures

#### ExceptionHandlerSpi

```java
@FunctionalInterface
public interface ExceptionHandlerSpi {
    ApiResponse<?> handle(Exception e);
}
```

**Cooperation rule**: Multiple handlers chain-call by `@Order` order; first non-null result short-circuits. If all return null, base default handler returns 500 Internal Server Error.

#### RequestInterceptorSpi

```java
public interface RequestInterceptorSpi {
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response) { return true; }
    default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {}
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Exception ex) {}
}
```

#### LogMaskSpi

```java
@FunctionalInterface
public interface LogMaskSpi {
    String mask(String fieldName, String fieldValue);
}
```

**Default masking rules** (built-in, no SPI required):

| Field Match Pattern | Masking Rule | Example |
|---------------------|--------------|---------|
| `*password*`, `*secret*`, `*token*` | Replace all with `******` | `myPassword=abc123` -> `myPassword=******` |
| `*phone*`, `*mobile*` | Replace middle 4 digits with `****` | `13812345678` -> `138****5678` |
| `*idCard*`, `*idNumber*` | First 1 + `****` + last 4 | `320102199001011234` -> `3****1234` |
| `*email*` | First char + `***` + domain | `user@example.com` -> `u***@example.com` |

#### HealthCheckSpi

```java
public interface HealthCheckSpi {
    record HealthCheckResult(String name, boolean healthy, String detail) {}
    HealthCheckResult check();
}
```

#### SerializationSpi

```java
public interface SerializationSpi {
    default void customizeSerializer(ObjectMapper mapper) {}
    default void customizeDeserializer(ObjectMapper mapper) {}
}
```

#### ValidationSpi

```java
public interface ValidationSpi {
    default void registerValidators(ValidatorRegistry registry) {}
}
```

---

## 2. Public DTOs

### ApiResponse\<T\> — Unified Response Body

All REST API responses are automatically wrapped to this type.

| Field | Type | Description |
|-------|------|-------------|
| code | int | 200=success, others=error code |
| message | String | Status message |
| data | T | Business data (generic) |
| traceId | String | 32-char UUID for tracing |

**Construction**:
- Success: `ApiResponse.success(data)`
- Success (custom message): `ApiResponse.success(data, "message")`
- Error: `ApiResponse.error(errorCode, "description")`

**Success response example**:
```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 1, "name": "test" },
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

**Error response example**:
```json
{
  "code": 20001,
  "message": "参数校验失败: name 不能为空",
  "data": null,
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

### PageRequest — Pagination Request DTO

| Field | Type | Default | Constraint |
|-------|------|---------|------------|
| page | int | 1 | >= 1 |
| size | int | 20 | 1 <= size <= 100 |
| sort | String | null | Format: `fieldName:asc\|desc` |

### PageResult\<T\> — Pagination Result DTO

| Field | Type | Description |
|-------|------|-------------|
| content | List\<T\> | Current page data |
| total | long | Total record count |
| page | int | Current page number |
| size | int | Page size |
| totalPages | int | Total pages |

### BaseEntity — Base Entity DTO

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| createdTime | LocalDateTime | Creation timestamp |
| updatedTime | LocalDateTime | Last update timestamp |

### PageUtils — In-memory Pagination

```java
PageResult<UserDto> page = PageUtils.paginate(allUsers, new PageRequest(1, 10));
```

### PageHelper — Spring to Common Conversion

```java
// common -> Spring
Pageable pageable = PageHelper.toSpringPageRequest(pageRequest);

// Spring -> common
PageResult<UserDto> result = PageHelper.fromSpringPage(springPage);
```

---

## 3. Library APIs

### CacheManager — Cache Operations

```java
@Autowired private CacheManager cacheManager;

// Write cache
Cache cache = cacheManager.getCache("bc-user:user:42");
cache.put("user:42", userDto);

// Read cache
Cache.ValueWrapper wrapper = cache.get("user:42");
if (wrapper != null) {
    UserDto user = (UserDto) wrapper.get();
}

// Evict cache
cache.evict("user:42");
```

**Key naming convention**: `<bc-name>:<entity>:<id>` (kebab-case bc-name, `:` separator)

**Default expiration**: `expireAfterWrite=30m`, `maximumSize=1000`

### JsonbUtils — JSONB Serialization

```java
// Object -> JSONB (for PostgreSQL JSONB column)
String jsonb = JsonbUtils.toJsonb(metadataObject);

// JSONB -> Object
MetadataDto dto = JsonbUtils.fromJsonb(jsonbString, MetadataDto.class);

// JSONB -> Object list
List<MetadataDto> list = JsonbUtils.fromJsonbList(jsonbString, MetadataDto.class);
```

**Serialization spec**:
- Date format: `yyyy-MM-dd HH:mm:ss`
- Null value strategy: NON_NULL
- Timezone: `Asia/Shanghai`
- Jackson ObjectMapper with `JavaTimeModule`

### Jackson ObjectMapper Global Configuration

Business BCs can inject the pre-configured `ObjectMapper` directly:

```java
@Autowired
private ObjectMapper objectMapper;

String json = objectMapper.writeValueAsString(obj);
MyObj obj = objectMapper.readValue(json, MyObj.class);
```

---

## 4. Exception Hierarchy

### Exception Class Structure

```
RuntimeException
├── BaseException (abstract)
│   ├── SystemException
│   │   ├── DatabaseException
│   │   └── RemoteCallException
│   └── BizException
│       ├── ValidationException
│       └── [BC Custom] extends BizException
```

### Error Code Mapping

| Exception Class | Error Code | HTTP Status | Description |
|----------------|------------|-------------|-------------|
| `SystemException` | 10001 | 500 | System internal error |
| `DatabaseException` | 10002 | 500 | Database operation failure |
| `RemoteCallException` | 10003 | 502 | Remote call failure |
| `BizException` | (subclass defined) | 422 | Business logic exception |
| `ValidationException` | 20002 | 400 | Parameter validation failure |
| `MethodArgumentNotValidException` | 20001 | 400 | JSR-380 validation failure |
| `HttpMessageNotReadableException` | 20003 | 400 | Request body parse failure |
| Other uncaught exceptions | 10000 | 500 | Unknown internal error |

### BC Custom Exception Registration

Business BCs register custom exception type mappings via `ExceptionHandlerSpi`:

```java
@Component
@Order(100)
public class CustomExceptionHandler implements ExceptionHandlerSpi {
    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof MyBusinessException myEx) {
            return ApiResponse.error(myEx.getCode(), myEx.getMessage());
        }
        return null;
    }
}
```

**Constraints**:
- Custom error codes must be in 30000-49999 range
- Conflicting error codes between BCs require manual coordination

---

## 5. Parameter Validation Extension

### Built-in Validation Annotations

Standard JSR-380 annotations: `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Email`, `@Pattern`

### Custom Validation Annotation

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {MyEnumValidator.class})
public @interface ValidEnum {
    String message() default "Invalid enum value";
    Class<? extends Enum<?>> enumClass();
}
```

### Validation Error Response Format

```json
{
  "code": 20001,
  "message": "参数校验失败: 字段 'name' 不能为空; 字段 'email' 格式不正确",
  "data": null,
  "traceId": "a1b2c3..."
}
```

---

## 6. Runtime Behavior Specification

### TraceId Specification

| Property | Specification |
|----------|---------------|
| Generation algorithm | `UUID.randomUUID().toString().replace("-", "")` — 32-char hex |
| HTTP header | `X-Trace-Id` (automatically returned in response header) |
| Log output format | `[timestamp] [thread] [level] [traceId] [logger] - message` |
| Cross-thread propagation | Auto-propagated via TaskDecorator + MDC |

### Structured Log Format

**Logback default format**:
```
[2026-07-23 14:30:00.123] [virtual-1] [INFO] [a1b2c3d4...] [c.m.s.controller.UserController] - Query user list: userId=42
```

**Log level rules**:
- `INFO`: Request entry/exit, major business operations
- `DEBUG`: Detailed parameters, intermediate computation results
- `WARN`: Recoverable exceptions, degradation behavior
- `ERROR`: Unrecoverable exceptions requiring manual intervention

### Sensitive Field Masking Rules

Built-in masking (via LogMaskSpi default implementation):

| Pattern | Rule |
|---------|------|
| `*password*`, `*secret*`, `*token*` | Replace all with `******` |
| `*phone*`, `*mobile*` | Replace middle 4 digits with `****` |
| `*email*` | First char + `***@domain` |

BCs can extend custom masking rules by implementing `LogMaskSpi`.
