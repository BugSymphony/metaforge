# API Contracts: foundation-core 核心 API 契约手册

BC 编码时直接消费的核心 API 契约手册。覆盖 SPI 扩展点、公共 DTO、库级 API、异常体系、参数校验扩展、运行时行为规范。

---

## 1. SPI 扩展点接口

foundation-core 在 `com.metaforge.common.spi` 包中定义六大扩展点接口，业务 BC 通过实现接口 + Spring Bean 注册方式扩展基座行为。SPI 扩展仅在声明 BC 模块内生效（BC 级隔离）。

### 1.1 扩展点总览

| 扩展点 | 接口 | 触发时机 | 用途 |
|--------|------|----------|------|
| 异常处理 | `ExceptionHandlerSpi` | 全局异常拦截器无法匹配异常类型时 | BC 注册自定义业务异常到错误响应的映射 |
| 请求拦截 | `RequestInterceptorSpi` | HTTP 请求前后 | BC 注入自定义请求预处理/后处理逻辑 |
| 日志脱敏 | `LogMaskSpi` | 日志输出前 | BC 定义自定义字段的脱敏规则 |
| 健康检查 | `HealthCheckSpi` | Actuator `/health` 端点被访问时 | BC 注册自定义健康检查项 |
| 序列化 | `SerializationSpi` | Jackson ObjectMapper 配置阶段 | BC 注册自定义序列化器/反序列化器 |
| 参数校验 | `ValidationSpi` | Validator 初始化阶段 | BC 注册自定义校验注解 |

### 1.2 生命周期模型

```
发现(Discovery) → 加载(Loading) → 排序(Ordering) → 调用(Invocation)
```

- **发现**: Spring 自动扫描所有实现了 SPI 接口的 Bean
- **加载**: 实例化扩展并注入依赖
- **排序**: 按 `@Order` 或 `@Priority` 注解排序（值越小优先级越高）
- **调用**: 链式/聚合调用，具体行为取决于扩展点类型

### 1.3 BC 级隔离保证

- 每个 SPI 扩展实现仅在其 `@ComponentScan` 范围内的 Spring 上下文中注册
- metaforge-boot 按 BC 模块隔离类加载器（模块 jar 隔离）
- 扩展的 `@ConditionalOnBean` 仅匹配同 BC 模块内的其他 Bean
- 验证方式：CI 中执行"跨 BC 扩展隔离测试"——A BC 的 SPI 实现不可被 B BC 的请求触发

### 1.4 详细接口签名

#### ExceptionHandlerSpi

```java
@FunctionalInterface
public interface ExceptionHandlerSpi {

    /**
     * 处理异常并返回标准 API 响应。
     *
     * @param e 被全局异常拦截器捕获的异常
     * @return API 响应；返回 null 表示该处理器不处理此异常，交由下一个处理器
     */
    ApiResponse<?> handle(Exception e);
}
```

**协作规则**: 多个处理器按 `@Order` 顺序链式调用，首个返回非 null 结果即时短路。若所有处理器均返回 null，由基座默认处理器兜底（返回 500 Internal Server Error）。

#### RequestInterceptorSpi

```java
public interface RequestInterceptorSpi {

    /**
     * 请求进入 Controller 前的预处理。
     * @return true 继续处理请求；false 中断处理（需自行通过 response 写入响应）
     */
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response) { return true; }

    /** Controller 执行后、视图渲染前的后处理。 */
    default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {}

    /** 请求完全完成后的清理（无论是否异常）。 */
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Exception ex) {}
}
```

#### LogMaskSpi

```java
@FunctionalInterface
public interface LogMaskSpi {
    /**
     * 对日志中指定字段的值进行脱敏处理。
     *
     * @param fieldName  字段名称
     * @param fieldValue 字段原始值，可能为 null
     * @return 脱敏后的值；返回 null 或与原始值相同表示不处理
     */
    String mask(String fieldName, String fieldValue);
}
```

**默认脱敏规则**（基座内置，无需 SPI 扩展即可生效）:

| 字段名匹配模式 | 脱敏规则 | 示例 |
|---------------|----------|------|
| `*password*`, `*secret*`, `*token*` | 全部替换为 `******` | `myPassword=abc123` → `myPassword=******` |
| `*phone*`, `*mobile*` | 中间 4 位替换为 `****` | `13812345678` → `138****5678` |
| `*idCard*`, `*idNumber*` | 前 1 位 + `****` + 后 4 位 | `320102199001011234` → `3****1234` |
| `*email*` | 用户名首字符 + `***` + 域名 | `user@example.com` → `u***@example.com` |

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

## 2. 公共 DTO

### 2.1 ApiResponse\<T\> — 统一响应体

所有 REST 接口的响应均自动包装为此类型（由 `GlobalResponseBodyAdvice` 自动完成）。

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 200=成功，其他=错误码 |
| message | String | 提示信息 |
| data | T | 业务数据（泛型） |
| traceId | String | 32 位 UUID，链路追踪 |

**构造规范**:
- 成功: `ApiResponse.success(data)`
- 成功（自定义消息）: `ApiResponse.success(data, "操作成功")`
- 失败: `ApiResponse.error(errorCode, "错误描述")`

**响应示例（成功）**:
```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 1, "name": "test" },
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

**响应示例（错误）**:
```json
{
  "code": 20001,
  "message": "参数校验失败: name 不能为空",
  "data": null,
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

### 2.2 PageRequest — 分页请求 DTO

| 字段 | 类型 | 默认值 | 约束 |
|------|------|--------|------|
| page | int | 1 | ≥ 1 |
| size | int | 20 | 1 ≤ size ≤ 100 |
| sort | String | null | 格式: `fieldName:asc\|desc` |

**使用示例**:
```java
// Controller 接收分页参数
@GetMapping("/users")
public ApiResponse<PageResult<UserDto>> listUsers(PageRequest pageRequest) {
    // PageHelper 转换为 Spring Pageable
    Pageable pageable = PageHelper.toSpringPageable(pageRequest);
    // 执行 JPA 查询
    Page<User> springPage = userRepository.findAll(pageable);
    // 转换回 common DTO
    PageResult<UserDto> result = PageHelper.fromSpringPage(springPage);
    return ApiResponse.success(result);
}
```

### 2.3 PageResult\<T\> — 分页结果 DTO

| 字段 | 类型 | 说明 |
|------|------|------|
| content | List\<T\> | 当前页数据 |
| total | long | 总记录数 |
| page | int | 当前页码 |
| size | int | 每页条数 |
| totalPages | int | 总页数 |

### 2.4 BaseEntity — 基础实体 DTO

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| createdTime | LocalDateTime | 创建时间 |
| updatedTime | LocalDateTime | 更新时间 |

**注**: 仅供 DTO 继承使用，不绑定 JPA 注解。

### 2.5 PageUtils — 内存分页

```java
// 对已加载的 List 做内存截取
PageResult<UserDto> page = PageUtils.paginate(allUsers, new PageRequest(1, 10));
```

### 2.6 PageHelper — Spring ↔ Common 转换

```java
// common → Spring
Pageable pageable = PageHelper.toSpringPageRequest(pageRequest);

// Spring → common
PageResult<UserDto> result = PageHelper.fromSpringPage(springPage);
```

---

## 3. 库级 API

### 3.1 CacheManager — 缓存操作

```java
// 注入
@Autowired private CacheManager cacheManager;

// 写入缓存
Cache cache = cacheManager.getCache("bc-user:user:42");
cache.put("user:42", userDto);

// 读取缓存
Cache.ValueWrapper wrapper = cache.get("user:42");
if (wrapper != null) {
    UserDto user = (UserDto) wrapper.get();
}

// 清除缓存
cache.evict("user:42");
```

**Key 命名约定**: `<bc-name>:<entity>:<id>`
- 示例: `user-bc:user:42`, `agent-bc:mcp:config`, `order-bc:order:20260723001`
- 必须使用 `:` 作为分隔符
- `<bc-name>` 使用中划线命名（kebab-case）
- 确保多 BC 共享 CacheManager 时 Key 不碰撞

**默认过期策略**: `expireAfterWrite=30m`, `maximumSize=1000`

**自定义缓存**: BC 可通过 `CacheManager.getCache("customName")` 创建新缓存实例（默认参数），或通过注入 Caffeine 配置自定义策略。

### 3.2 JsonbUtils — JSONB 序列化

```java
// 对象 → JSONB (用于写入 PostgreSQL JSONB 列)
String jsonb = JsonbUtils.toJsonb(metadataObject);

// JSONB → 对象
MetadataDto dto = JsonbUtils.fromJsonb(jsonbString, MetadataDto.class);

// JSONB → 对象列表
List<MetadataDto> list = JsonbUtils.fromJsonbList(jsonbString, MetadataDto.class);
```

**序列化规格**:
- 日期格式: `yyyy-MM-dd HH:mm:ss`
- 空值策略: NON_NULL（null 字段不输出）
- 时区: `Asia/Shanghai`
- Jackson ObjectMapper 使用 `JavaTimeModule`
- 禁用 `WRITE_DATES_AS_TIMESTAMPS`

### 3.3 Jackson ObjectMapper 全局配置

业务 BC 可直接注入 `ObjectMapper`（已全局配置好上述规则）使用：

```java
@Autowired
private ObjectMapper objectMapper;

String json = objectMapper.writeValueAsString(obj);
MyObj obj = objectMapper.readValue(json, MyObj.class);
```

---

## 4. 异常基类体系

### 4.1 异常层次结构

```
RuntimeException
└── BaseException (abstract)                        ← metaforge-common
    ├── SystemException                             ← 系统级异常
    │   ├── DatabaseException                       ← 数据库异常
    │   └── RemoteCallException                     ← 远程调用异常
    └── BizException                                ← 业务异常基类
        ├── ValidationException                     ← 参数校验异常
        └── [BC 自定义] extends BizException         ← BC 自定义业务异常
```

### 4.2 错误码映射

| 异常类 | 错误码 | HTTP 状态码 | 说明 |
|--------|--------|-------------|------|
| `SystemException` | 10001 | 500 | 系统内部错误 |
| `DatabaseException` | 10002 | 500 | 数据库操作失败 |
| `RemoteCallException` | 10003 | 502 | 远程调用失败 |
| `BizException` | (子类定义) | 422 | 业务逻辑异常 |
| `ValidationException` | 20002 | 400 | 参数校验失败 |
| `MethodArgumentNotValidException` (Spring) | 20001 | 400 | JSR-380 校验失败 |
| `HttpMessageNotReadableException` (Spring) | 20003 | 400 | 请求体解析失败 |
| 其他未捕获异常 | 10000 | 500 | 未知内部错误 |

### 4.3 BC 自定义异常注册

业务 BC 通过实现 `ExceptionHandlerSpi` 注册自定义异常类型映射：

```java
@Component
@Order(100)
public class CustomExceptionHandler implements ExceptionHandlerSpi {
    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof MyBusinessException myEx) {
            return ApiResponse.error(myEx.getCode(), myEx.getMessage());
        }
        return null; // 交由下一个处理器或基座默认解析
    }
}
```

**约束**:
- 自定义错误码必须在 30000-49999 范围内（避免与 base 预留范围冲突）
- 多个 BC 冲突的错误码：人为协调，CI 无自动冲突检测

---

## 5. 参数校验扩展

### 5.1 内置校验注解

Spring Boot 3 + Hibernate Validator 已提供标准 JSR-380 注解：`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Email`, `@Pattern` 等。

### 5.2 自定义校验注解

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {MyEnumValidator.class})
public @interface ValidEnum {
    String message() default "枚举值非法";
    Class<? extends Enum<?>> enumClass();
}
```

### 5.3 校验异常响应格式

```json
{
  "code": 20001,
  "message": "参数校验失败: 字段 'name' 不能为空; 字段 'email' 格式不正确",
  "data": null,
  "traceId": "a1b2c3..."
}
```

校验错误明细通过 `message` 字段以分号分隔列出，`data` 字段为 null。

---

## 6. 运行时行为规范

### 6.1 TraceId 规范

| 属性 | 规范 |
|------|------|
| 生成算法 | `UUID.randomUUID().toString().replace("-", "")` — 32 位十六进制字符串 |
| HTTP 请求头 | `X-Trace-Id`（响应头自动返回） |
| 日志输出格式 | `[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] [%level] [%X{traceId}] [%logger{36}] - %msg%n` |
| 跨线程透传 | 自动透传（通过 `TaskDecorator` + MDC 上下文传播） |

### 6.2 结构化日志格式

**Logback 默认格式**:
```
[2026-08-01 14:30:00.123] [virtual-1] [INFO] [a1b2c3d4...] [c.m.s.controller.UserController] - 查询用户列表: userId=42
```

**日志级别规则**:
- `INFO`: 请求入口/出口、主要业务操作
- `DEBUG`: 详细参数、中间计算结果
- `WARN`: 可恢复的异常、降级行为
- `ERROR`: 不可恢复的异常、需要人工介入

### 6.3 敏感字段脱敏规则

内置脱敏（通过 LogMaskSpi 的默认实现）：

| 模式 | 规则 |
|------|------|
| `*password*`, `*secret*`, `*token*` | 全部替换为 `******` |
| `*phone*`, `*mobile*` | 中间 4 位替换为 `****` |
| `*email*` | 用户名首字符 + `***@域名` |

BC 可通过实现 `LogMaskSpi` 扩展自定义脱敏规则。
