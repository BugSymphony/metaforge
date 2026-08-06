# Data Model: foundation-core 基座初始化

foundation-core 为纯技术基础设施 BC，不持有业务数据。本文档定义 foundation-core 向业务 BC 暴露的公共数据结构、异常体系、SPI 接口及配置模型。

---

## 1. 统一响应体

### ApiResponse\<T\>

全平台所有 REST 接口的统一响应封装。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | int | 是 | 业务状态码，200 表示成功 |
| message | String | 是 | 提示信息，成功时为 "success" |
| data | T | 否 | 响应数据体，错误时为 null |
| traceId | String | 是 | 全链路追踪标识，格式为 32 位 UUID（无连字符） |

**构造工厂方法**:

```java
public final class ApiResponse<T> implements Serializable {
    int code;
    String message;
    T data;
    String traceId;

    public static <T> ApiResponse<T> success(T data);                  // code=200, message="success"
    public static <T> ApiResponse<T> success(T data, String message);  // code=200
    public static <T> ApiResponse<T> error(int code, String message);  // data=null
    public static <T> ApiResponse<T> of(int code, String message, T data);
}
```

**验证规则**:
- `code`: 不可为负值
- `message`: 不可为空或 null
- `traceId`: 不可为空，由 TraceIdFilter 自动注入

---

## 2. 分页契约 DTO

### PageRequest

位于 `metaforge-common`，纯 Java POJO，不依赖 Spring Data。

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码（1-based），最小 1 |
| size | int | 20 | 每页条数，最小 1，最大 100 |
| sort | String | null | 排序字段与方向，格式：`field:asc\|desc` |

**构造方法**:
```java
public PageRequest()                        // page=1, size=20
public PageRequest(int page, int size)      // 指定页码与条数
public PageRequest(int page, int size, String sort)
```

**验证规则**:
- `page >= 1`
- `1 <= size <= 100`
- `sort` 匹配正则 `^[a-zA-Z][a-zA-Z0-9_]*(:(asc|desc))?$`

### PageResult\<T\>

| 字段 | 类型 | 说明 |
|------|------|------|
| content | List\<T\> | 当前页数据列表 |
| total | long | 总记录数 |
| page | int | 当前页码 |
| size | int | 每页条数 |
| totalPages | int | 总页数，计算值 `ceil(total / size)` |

**容错行为**: 请求页超出总页数时，返回空列表且 `total` 保持正确值。

### PageUtils (metaforge-common)

内存级分页工具：
```java
public final class PageUtils {
    public static <T> PageResult<T> paginate(List<T> list, PageRequest request);
}
```

### PageHelper (metaforge-framework)

Spring ↔ Common 转换工具：
```java
public final class PageHelper {
    public static Pageable toSpringPageable(PageRequest request);
    public static <T> PageResult<T> fromSpringPage(Page<T> springPage);
}
```

---

## 3. 异常基类体系

位于 `metaforge-common`，层次化异常结构。

```
RuntimeException (JDK)
├── BaseException (abstract)
│   ├── SystemException          — 系统级异常（基础设施故障）
│   │   ├── DatabaseException    — 数据库操作异常
│   │   └── RemoteCallException  — 远程调用异常
│   └── BizException             — 业务异常基类（供 BC 扩展）
│       ├── ValidationException  — 参数校验异常
│       └── [BC 自定义扩展]      — 业务 BC 通过继承 BizException 自定义
```

### BaseException

```java
public abstract class BaseException extends RuntimeException {
    int    code;        // 错误码（5 位数字）
    String message;     // 错误消息（可国际化）
    String detail;      // 详细信息（可选，用于开发调试）

    public BaseException(int code, String message);
    public BaseException(int code, String message, Throwable cause);
    protected BaseException(int code, String message, String detail, Throwable cause);
}
```

### 错误码分配规则

| 范围 | 用途 | 示例 |
|------|------|------|
| 10000-19999 | 系统级错误 | 10001=系统内部错误, 10002=数据库异常 |
| 20000-29999 | 参数校验错误 | 20001=参数缺失, 20002=参数格式错误 |
| 30000-49999 | 预留（业务 BC 使用）| 各 BC 自行分配 |
| 50000-59999 | 第三方服务错误 | 50001=远程调用超时 |

---

## 4. 公共 DTO 基类

### BaseEntity

带审计字段的抽象实体 DTO。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 ID |
| createdTime | LocalDateTime | 创建时间（自动填充） |
| updatedTime | LocalDateTime | 更新时间（自动填充） |

**注**: BaseEntity 为 common 层 DTO 基类，仅供数据传输使用，不绑定 JPA 注解。JPA 实体的审计字段通过 `@MappedSuperclass` 在框架层或 BC 层独立定义。

---

## 5. SPI 扩展点接口

位于 `metaforge-common`，均为函数式接口或单方法接口。

### ExceptionHandlerSpi

BC 注册额外的异常类型与错误响应的映射。

```java
@FunctionalInterface
public interface ExceptionHandlerSpi {
    ApiResponse<?> handle(Exception e);  // 返回 null 表示不处理，交由下一个处理器
}
```

**生命周期**: 发现 → 加载 → 排序（`@Order`）→ 链式调用（短路：首个非 null 结果生效）

### RequestInterceptorSpi

HTTP 请求前后拦截。

```java
public interface RequestInterceptorSpi {
    default boolean preHandle(HttpServletRequest request) { return true; }
    default void postHandle(HttpServletRequest request, HttpServletResponse response) {}
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Exception ex) {}
}
```

### LogMaskSpi

自定义日志脱敏规则。

```java
@FunctionalInterface
public interface LogMaskSpi {
    String mask(String fieldName, String fieldValue);  // 返回脱敏后的值
}
```

### HealthCheckSpi

自定义健康检查项。

```java
@FunctionalInterface
public interface HealthCheckSpi {
    HealthCheckResult check();  // 返回 UP 或 DOWN 状态
}
```

### SerializationSpi

自定义序列化/反序列化行为扩展。

```java
public interface SerializationSpi {
    default void customizeSerializer(ObjectMapper mapper) {}
    default void customizeDeserializer(ObjectMapper mapper) {}
}
```

### ValidationSpi

自定义校验规则注册。

```java
public interface ValidationSpi {
    default void registerValidators(ValidatorRegistry registry) {}
}
```

---

## 6. Spring Boot 配置属性模型

### 跨模块共享配置（metaforge-boot 中定义）

```yaml
# ===== 数据源 (Spring Boot 原生键名) =====
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/metaforge
    username: metaforge
    password: metaforge
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  # ===== 缓存 (Spring Boot 原生键名) =====
  cache:
    type: caffeine
    caffeine:
      spec: expireAfterWrite=30m,maximumSize=1000,recordStats

  # ===== 国际化 (Spring Boot 原生键名) =====
  messages:
    basename: i18n/messages
    encoding: UTF-8
    fallback-to-system-locale: false

  # ===== 虚拟线程 (Spring Boot 原生键名) =====
  threads:
    virtual:
      enabled: true

  # ===== Jackson (Spring Boot 原生键名) =====
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false

# ===== Actuator 可观测性 =====
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized

# ===== SpringDoc OpenAPI =====
springdoc:
  swagger-ui:
    tags-sorter: alpha
  group-configs:
    - group: all
      paths-to-match: /**

# ===== 服务器端口 =====
server:
  port: 8080
```

---

## 7. 模块依赖模型

```
┌──────────────────────────────────────────────────┐
│                metaforge-boot                      │
│  (唯一启动入口, 聚合所有模块, 承载所有配置)         │
│  deps: metaforge-server + 全部 BC modules          │
├──────────────────────────────────────────────────┤
│              metaforge-server                      │
│  (Spring Boot AutoConfiguration 装配层)            │
│  deps: metaforge-framework, metaforge-common       │
├──────────────────────────────────────────────────┤
│             metaforge-framework                    │
│  (框架工具层: Spring/JPA/Web/Cache/Test 工具)       │
│  deps: metaforge-common, Spring, JPA, Caffeine... │
├──────────────────────────────────────────────────┤
│              metaforge-common                      │
│  (纯 Java 工具层: DTO/异常/SPI/工具类)             │
│  deps: Jackson(core+databind+annotations), SLF4J    │
├──────────────────────────────────────────────────┤
│              业务 BC modules                        │
│  (平铺在 metaforge-parent 根目录)                   │
│  deps: metaforge-framework (传递获得 common)        │
│  register: 在 metaforge-boot/pom.xml 声明为依赖     │
└──────────────────────────────────────────────────┘
```

**禁止依赖方向**:
- common ↗ framework / server / boot / BC
- framework ↗ server / boot / BC
- server ↗ boot / BC
- boot 不允许被任何模块依赖（通过 Enforcer 强制执行）
- BC ↗ server / boot

---

## 8. 状态机

foundation-core 不定义业务状态机。SPI 扩展点生命周期状态如下：

```
SPI Extension Lifecycle:
  ┌─────────┐    发现     ┌──────────┐    依赖满足    ┌─────────┐
  │ Defined │ ──────────→ │Discovered│ ──────────────→ │ Loaded  │
  │ (编译期) │             │(类路径扫描)│                 │(实例化) │
  └─────────┘             └──────────┘                 └────┬────┘
                                                          │
                                                          │ 排序
                                                          ▼
                                           ┌──────────────┐
                                           │   Ordered    │
                                           │ (@Order 排序) │
                                           └──────┬───────┘
                                                  │
                                                  │ 注册
                                                  ▼
                                           ┌──────────────┐
                                           │  Registered  │
                                           │ (调用链路中)  │
                                           └──────────────┘
```
