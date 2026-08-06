# Foundation Adaptation Design: metaforge-agent-cognition

**Feature**: 001-agent-cognition-guidance | **BC**: agent-cognition | **Foundation**: foundation-core v1.0.0

## 概述

本文档描述 `metaforge-agent-cognition` BC 如何接入 foundation-core 提供的平台预置能力。本 BC 为纯查询编排层——无数据库、无持久化、无状态，仅通过即席查询上游 BC 获取元数据并组装认知简报。所有接入方案遵循**非侵入原则**：仅声明式使用平台 API，禁止修改 foundation-core 源码或全局配置。

---

## 1. 构建系统集成

### 1.1 模块结构

本 BC 采用标准 Maven 多模块结构：

```
metaforge-parent/pom.xml
  └── metaforge-agent-cognition/pom.xml (聚合父 POM)
        ├── metaforge-agent-cognition-api/pom.xml (jar, 契约层)
        └── metaforge-agent-cognition-core/pom.xml (jar, 实现层)
```

### 1.2 根 POM 注册

**`metaforge-parent/pom.xml`** 的 `<modules>` 中添加：

```xml
<module>metaforge-agent-cognition</module>
```

**`metaforge-parent/pom.xml`** 的 `<dependencyManagement>` 中添加（按需）：

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition-api</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition-core</artifactId>
    <version>${revision}</version>
</dependency>
```

### 1.3 启动模块注册

在 **`metaforge-boot/pom.xml`** 的 `<dependencies>` 中添加 runtime 依赖：

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition-core</artifactId>
</dependency>
```

### 1.4 子模块 POM 依赖声明

| 模块 | 依赖 | 说明 |
|------|------|------|
| `metaforge-agent-cognition-api` | 无额外业务依赖（纯接口/DTO 定义模块） | 不依赖 `metaforge-framework` |
| `metaforge-agent-cognition-core` | `metaforge-agent-cognition-api` | 实现层引用契约层 |
| | `metaforge-framework` | 传递引入 foundation-core 全部能力 |
| | `metaforge-metamodel-api` | 上游：元模型查询 |
| | `metaforge-metadata-api` | 上游：元数据查询 |
| | `metaforge-graph-api` | 上游：关系拓扑查询 |
| | `metaforge-compute-engine-api` | 上游：语义查询与推理 |
| | `jackson-dataformat-yaml` | YAML 配置文件解析 |
| | `metaforge-framework` (test-jar, test scope) | 测试基类 |

### 1.5 依赖声明合规

- 所有版本由 `metaforge-parent` BOM 集中管理，BC POM 中**不出现 `<version>` 标签**
- 不声明 `<dependencyManagement>` 段（Maven Enforcer `banDependencyManagementScope` 校验）
- 严禁依赖 `metaforge-boot`、`metaforge-server`（黑名单）
- API 模块不声明 `metaforge-framework` 依赖（保持契约层轻量纯粹）
- Core 模块通过 `metaforge-framework` 传递获得 `ApiResponse<T>`、`PageRequest`、`PageResult<T>`、`JsonbUtils`、`CacheManager` 等公共 DTO 与工具

---

## 2. 配置规范

### 2.1 BC 配置命名空间

所有 BC 专属配置使用 `metaforge.agent-cognition` 前缀，在 `metaforge-boot/src/main/resources/application.yml` 中集中管理：

```yaml
metaforge:
  agent-cognition:
    templates:
      base-path: classpath:config/cognition/templates/
      cache:
        enabled: true
        ttl: 30m
        max-size: 50
    perspectives:
      base-path: classpath:config/cognition/perspectives/
      cache:
        enabled: true
        ttl: 30m
        max-size: 20
      timeout-ms: 200
    query:
      max-bundle-fqns: 20
      max-perspectives: 14
      default-depth: L2
      default-tokens: 8000
      min-tokens-for-auto-degrade: 500
    traversal:
      max-composition-depth: 5
      max-relationship-degree: 3
      max-impact-depth: 3
    format:
      default: json
      allowed: [json, prompt]
```

### 2.2 继承的全局配置

以下 foundation-core 提供的全局配置 BC 不得覆盖：

| 配置项 | 所属 | 说明 |
|--------|------|------|
| `spring.threads.virtual.enabled` | foundation-core | 虚拟线程（全局启用） |
| `spring.jackson.*` | foundation-core | Jackson 序列化（统一时区/格式） |
| `spring.cache.*` | foundation-core | Caffeine 缓存默认规格 |
| `spring.messages.*` | foundation-core | i18n MessageSource |
| `management.endpoints.*` | foundation-core | Actuator 可观测性 |
| `springdoc.*` | foundation-core | OpenAPI 文档生成 |

> 本 BC **无数据库**，以下配置不适用于本 BC：`spring.datasource.*`、`spring.flyway.*`。

---

## 3. SPI 扩展点接入

### 3.1 需要实现的 SPI

| SPI | 是否实现 | 理由 |
|-----|---------|------|
| `ExceptionHandlerSpi` | **是** | 注册 BC 自定义异常类型映射（错误码 34000-34099） |
| `HealthCheckSpi` | **是** | 检查 YAML 配置加载状态与上游 BC 连通性 |
| `LogMaskSpi` | 否 | foundation-core 内置脱敏规则已覆盖通用场景 |
| `RequestInterceptorSpi` | 否 | 无 HTTP 请求拦截需求 |
| `SerializationSpi` | 否 | 使用 foundation-core 统一 Jackson ObjectMapper |
| `ValidationSpi` | 否 | 使用 JSR-380 标准注解 |

### 3.2 异常处理扩展（ExceptionHandlerSpi）

**错误码范围**：**34000-34099**

实现类位于 `metaforge-agent-cognition-core` 模块的 infrastructure 层：

```java
@Component
@Order(100)
public class AgentCognitionExceptionHandler implements ExceptionHandlerSpi {

    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof AgentCognitionException ace) {
            return ApiResponse.error(ace.getErrorCode(), ace.getMessage());
        }
        return null; // 非本 BC 异常，委托下一个 handler
    }
}
```

**错误码分配表**（定义于 `api` 模块的 `constants/ErrorCodes.java`）：

| 常量名 | 错误码 | 对应异常 | 说明 |
|--------|--------|----------|------|
| `INVALID_BUNDLE_FQN` | 34001 | `InvalidBundleFqnException` | bundle_fqns 格式非法 |
| `EMPTY_BUNDLE_FQNS` | 34002 | `EmptyBundleFqnsException` | bundle_fqns 列表为空 |
| `INVALID_ENTITY_FQN` | 34003 | `InvalidEntityFqnException` | entity_fqn 前缀不属于任何已发布 Bundle |
| `PERSPECTIVE_TIMEOUT` | 34004 | `PerspectiveTimeoutException` | 单个视角查询超时（>200ms） |
| `TEMPLATE_LOAD_FAILED` | 34005 | `TemplateLoadException` | YAML 模板配置加载/解析失败 |
| `PERSPECTIVE_CONFIG_INVALID` | 34006 | `PerspectiveConfigInvalidException` | 视角配置校验失败 |
| `UPSTREAM_BC_UNAVAILABLE` | 34007 | `UpstreamBcUnavailableException` | 上游 BC 查询调用失败 |
| `COGNITION_DEPTH_INVALID` | 34008 | `InvalidCognitionDepthException` | 认知深度参数超出有效范围 |
| `AGENT_ARCHETYPE_INVALID` | 34009 | `InvalidAgentArchetypeException` | 代理原型参数未知 |
| `SCOPE_MODE_INVALID` | 34010 | `InvalidScopeModeException` | 作用域模式参数非法 |

**异常基类**：
```java
// api 模块
public class AgentCognitionException extends BizException {
    public AgentCognitionException(int errorCode, String message) {
        super(errorCode, message);
    }
}
```

### 3.3 健康检查扩展（HealthCheckSpi）

```java
@Component
public class AgentCognitionHealthCheck implements HealthCheckSpi {

    private final TemplateConfigLoader templateLoader;
    private final PerspectiveConfigLoader perspectiveLoader;

    @Override
    public HealthCheckResult check() {
        // 检查 YAML 配置文件加载状态
        boolean templatesLoaded = templateLoader.isLoaded();
        boolean perspectivesLoaded = perspectiveLoader.isLoaded();

        if (!templatesLoaded || !perspectivesLoaded) {
            return new HealthCheckResult(
                "agent-cognition-config",
                false,
                String.format("Templates: %s, Perspectives: %s",
                    templatesLoaded ? "OK" : "FAILED",
                    perspectivesLoaded ? "OK" : "FAILED")
            );
        }
        return new HealthCheckResult(
            "agent-cognition",
            true,
            "YAML 配置加载正常，所有视角可用"
        );
    }
}
```

---

## 4. API 文档集成

**能力来源**：foundation-core 预配置的 SpringDoc OpenAPI 3.0

### 4.1 接入方案

REST Controller 使用 `@Tag(name = "agent-cognition")` 标注分组：

```java
@RestController
@RequestMapping("/api/v1/cognition")
@Tag(name = "agent-cognition")
public class CognitionGuidanceController {

    @Operation(summary = "认知查询统一引擎")
    @PostMapping("/cognitionGuidance")
    public GuidanceResult cognitionGuidance(@Valid @RequestBody QueryParameters params) {
        return guidanceService.execute(params);
    }

    @Operation(summary = "一站式任务简报")
    @PostMapping("/taskBrief")
    public TaskMetacognitionBrief taskBrief(@Valid @RequestBody TaskBriefRequest request) {
        return taskBriefService.generate(request);
    }
}
```

### 4.2 禁止行为

- 不添加 `springdoc-openapi` 依赖（由 foundation-core 提供）
- 不自定义 `OpenAPI` Bean
- 不在 `application.yml` 中覆盖 `springdoc.*` 配置（使用平台默认值）

---

## 5. 平台能力消费

### 5.1 统一响应格式

**能力来源**：`ApiResponse<T>`（`com.metaforge.common.dto`）

Controller 方法返回业务 DTO，由 foundation-core 全局 `GlobalResponseBodyAdvice` 自动包装为 `ApiResponse<T>` 格式。Controller 中不手动构造 `ApiResponse` 对象。

```java
// 正确：返回业务 DTO，由全局切面自动包装
@GetMapping("/bundleCatalog")
public BundleCatalogResult bundleCatalog() {
    return bundleCatalogService.build();
}

// 错误：手动包装
public ApiResponse<BundleCatalogResult> bundleCatalog() { ... }
```

### 5.2 分页组件

**能力来源**：`PageRequest` / `PageResult<T>` / `PageHelper`（`com.metaforge.common.dto`）

`cognitionGuidance` 端点中的 `instance_catalog` 和 `domain_navigation` 视角使用分页响应：

```java
public PageResult<InstanceSummary> listInstances(PageRequest pageRequest) {
    Pageable pageable = PageHelper.toSpringPageRequest(pageRequest);
    Page<InstanceDto> page = upstreamApi.queryInstances(entityType, pageable);
    return PageHelper.fromSpringPage(page.map(summaryMapper::toSummary));
}
```

### 5.3 缓存管理

**能力来源**：Caffeine `CacheManager`（foundation-core 预配置）

本 BC 使用缓存场景：
- **模板缓存**：`agent-cognition:template:<templateName>` — YAML 模板解析后的 POJO，TTL 30 分钟（启动时一次性加载，缓存用于热加载场景）
- **视角配置缓存**：`agent-cognition:perspective:<perspectiveName>` — 视角定义配置，TTL 30 分钟

不使用缓存的场景：
- 认知查询结果（每次请求实时查询上游 BC，不跨请求缓存——无状态原则）
- changeWatch 影响报告（实时计算，不缓存）

```java
@Autowired
private CacheManager cacheManager;

public TemplateConfig loadTemplate(String templateName) {
    Cache cache = cacheManager.getCache("agent-cognition:template:" + templateName);
    // lookup / load / put
}
```

**Cache key 命名**：遵循 `agent-cognition:<entity-type>:<identifier>` 格式。

### 5.4 JSONB 序列化

**能力来源**：`JsonbUtils`（`com.metaforge.common.util`）

本 BC 无数据库——不存在 JSONB 列的持久化/读取场景。若上游 BC 返回的 JSONB 数据涉及解析，使用 `JsonbUtils.fromJsonb()` 转换：

```java
// 上游 metadata BC 返回的 JSONB content 字段解析
Map<String, Object> content = JsonbUtils.fromJsonb(jsonbString, new TypeReference<>() {});
```

### 5.5 虚拟线程

**能力来源**：foundation-core 全局启用 `spring.threads.virtual.enabled=true`

零配置继承。Tomcat 请求处理与多视角并发查询自动运行在虚拟线程上。本 BC 的 14 个视角执行器并发调用上游 BC，虚拟线程提供足够的并发承载能力。

```java
@Async
public CompletableFuture<PerspectiveResult> executePerspective(PerspectiveConfig config) {
    // 单视角查询，200ms 超时
    return CompletableFuture.completedFuture(perspectiveExecutor.execute(config));
}
```

**禁止行为**：
- 不配置 `ThreadPoolTaskExecutor` 或自定义线程池
- 不声明 `@EnableAsync`（由 foundation-core 统一启用）
- 不在 `application.yml` 中覆盖 `spring.threads.*` 配置

### 5.6 日志脱敏

**能力来源**：foundation-core `LogMaskSpi` 内置规则

本 BC 不自定义脱敏规则。foundation-core 已覆盖 `password`、`secret`、`token`、`phone`、`mobile`、`email`、`idCard` 等通用敏感字段。本 BC 的 FQN、模板名称、视角名称等均为非敏感字段。

### 5.7 国际化（i18n）

**能力来源**：foundation-core 预配置 `MessageSource`

在 `metaforge-boot/src/main/resources/i18n/` 下添加 BC 消息文件：

- `messages_agent-cognition_zh_CN.properties`
- `messages_agent-cognition_en_US.properties`

```java
@Autowired
private MessageSource messageSource;

String msg = messageSource.getMessage(
    "agent-cognition.error.perspective_timeout",
    new Object[]{perspectiveName},
    LocaleContextHolder.getLocale()
);
```

消息 key 命名规范：`agent-cognition.<category>.<detail>`

**禁止行为**：
- 不定义独立的 `MessageSource` Bean
- 不在 BC 模块内创建独立的 i18n 资源目录

### 5.8 Jackson ObjectMapper

**能力来源**：foundation-core 统一 `ObjectMapper` Bean（`yyyy-MM-dd HH:mm:ss`、`Asia/Shanghai`、`NON_NULL`）

按需注入 `ObjectMapper` 进行 JSON 序列化/反序列化：

```java
@Autowired
private ObjectMapper objectMapper;

// prompt 格式输出构造
String markdownOutput = formatEngine.toMarkdown(guidanceResult, objectMapper);
```

**禁止行为**：不自定义 `ObjectMapper` 或覆盖 Jackson 全局配置。

---

## 6. 安全基线

### 6.1 接入方案

复用 foundation-core 预配置的安全基线：

| 安全能力 | 接入方式 | 本 BC 场景 |
|---------|---------|-----------|
| XSS 过滤器 | 零配置继承 | 所有 HTTP 请求自动经过 XSS 过滤 |
| 请求体大小限制（10MB） | 零配置继承 | `cognitionGuidance` 等端点请求体自动限制 |
| CORS | `application.yml` 全局配置 | 不自定义 `CorsFilter` |
| SQL 注入防护 | **N/A** | 本 BC 无数据库，无 SQL 注入风险 |

### 6.2 禁止行为

- 不配置独立的 `CorsFilter` 或 `WebMvcConfigurer.addCorsMappings()`
- 不引入 Spring Security 依赖
- 不自定义安全过滤器

---

## 7. 测试框架集成

**能力来源**：`BaseUnitTest` / `BaseIntegrationTest`（`metaforge-framework` test-jar）

### 7.1 单元测试

```java
class PerspectiveExecutorTest extends BaseUnitTest {

    @Mock
    private UpstreamApiClient upstreamClient;

    @InjectMocks
    private PerspectiveExecutor executor;

    @Test
    void shouldTimeoutAfter200ms() {
        when(upstreamClient.query(any())).thenAnswer(inv -> {
            Thread.sleep(300);
            return Collections.emptyList();
        });
        PerspectiveResult result = executor.execute(config);
        assertThat(result.isTruncated()).isTrue();
        assertThat(result.getTruncatedReason()).isEqualTo("TIMEOUT");
    }
}
```

### 7.2 集成测试

```java
@SpringBootTest
class CognitionGuidanceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CognitionGuidanceService guidanceService;

    @Test
    void shouldReturnTaskBriefForValidBundle() {
        QueryParameters params = QueryParameters.builder()
            .bundleFqns(List.of("org.example.erp"))
            .cognitionDepth(CognitionDepth.L2)
            .agentArchetype(AgentArchetype.EXECUTION)
            .build();

        TaskMetacognitionBrief result = guidanceService.taskBrief(params);

        assertThat(result.getContextMeta()).isNotNull();
        assertThat(result.getContextMeta().getDataVersionAnchors()).isNotEmpty();
    }
}
```

### 7.3 禁止行为

- 不引入 TestContainers 直接依赖（由 `BaseIntegrationTest` 统一管理）
- 不自定义测试数据源配置
- 不配置独立的 PostgreSQL 容器化测试基础设施

---

## 8. 合规确认清单

| # | 验证项 | 状态 | 依据 |
|---|--------|------|------|
| 1 | 聚合 POM 继承 `metaforge-parent` | ✓ | [build-system-integration](../context/foundation-contracts/foundation-core/build-system-integration.md#1) |
| 2 | 不声明 `<dependencyManagement>` | ✓ | [Maven Enforcer banDependencyManagementScope](../context/foundation-contracts/foundation-core/build-system-integration.md#4) |
| 3 | API 模块不依赖 `metaforge-framework` | ✓ | [依赖声明规则](../context/foundation-contracts/foundation-core/build-system-integration.md#3) |
| 4 | Core 模块依赖 `metaforge-framework` | ✓ | [white-list dependency](../context/foundation-contracts/foundation-core/build-system-integration.md#3) |
| 5 | 不依赖 `metaforge-boot` / `metaforge-server` | ✓ | [blacklist](../context/foundation-contracts/foundation-core/build-system-integration.md#3) |
| 6 | 不在 POM 中使用 `<version>` 标签 | ✓ | [BOM 统一管理](../context/foundation-contracts/foundation-core/build-system-integration.md#3) |
| 7 | 已在 `metaforge-boot/pom.xml` 注册 runtime 依赖 | ✗ | 待 BC 脚手架创建后注册 |
| 8 | 已在 `metaforge-parent/pom.xml` 的 `<modules>` 注册 | ✗ | 待 BC 脚手架创建后注册 |
| 9 | REST API 复用 `ApiResponse<T>` 格式 | ✓ | [api-contracts §2](../context/foundation-contracts/foundation-core/api-contracts.md#2) |
| 10 | 分页接口使用 `PageRequest`/`PageResult<T>` | ✓ | [api-contracts §2](../context/foundation-contracts/foundation-core/api-contracts.md#2) |
| 11 | 实现 `ExceptionHandlerSpi` 注册 BC 异常（34000-34099） | ✓ | [api-contracts §1 ExceptionHandlerSpi](../context/foundation-contracts/foundation-core/api-contracts.md#1) |
| 12 | 实现 `HealthCheckSpi` 注册 BC 健康检查 | ✓ | [api-contracts §1 HealthCheckSpi](../context/foundation-contracts/foundation-core/api-contracts.md#1) |
| 13 | 不实现 `LogMaskSpi`（默认规则已覆盖） | ✓ | [platform-capabilities §2](../context/foundation-contracts/foundation-core/platform-capabilities.md#2) |
| 14 | Controller 使用 `@Tag("agent-cognition")` | ✓ | [platform-capabilities §3](../context/foundation-contracts/foundation-core/platform-capabilities.md#3) |
| 15 | 不添加 `springdoc-openapi` 依赖 | ✓ | [platform-capabilities §3](../context/foundation-contracts/foundation-core/platform-capabilities.md#3) |
| 16 | 注入 `MessageSource`，不自定义 Bean | ✓ | [platform-capabilities §4](../context/foundation-contracts/foundation-core/platform-capabilities.md#4) |
| 17 | 不配置线程池（虚拟线程继承） | ✓ | [platform-capabilities §1](../context/foundation-contracts/foundation-core/platform-capabilities.md#1) |
| 18 | 不配置数据源（本 BC 无数据库） | ✓ | [platform-capabilities §7 — N/A](../context/foundation-contracts/foundation-core/platform-capabilities.md#7) |
| 19 | 不配置 Flyway（本 BC 无数据库） | ✓ | [platform-capabilities §9 — N/A](../context/foundation-contracts/foundation-core/platform-capabilities.md#9) |
| 20 | 不执行跨 Schema 写操作（本 BC 无数据库） | ✓ | [platform-capabilities §8 — N/A](../context/foundation-contracts/foundation-core/platform-capabilities.md#8) |
| 21 | 使用 `JsonbUtils` 进行 JSONB 序列化 | ✓ | [api-contracts §3 JsonbUtils](../context/foundation-contracts/foundation-core/api-contracts.md#3) |
| 22 | 使用注入的 `ObjectMapper`（不自定义配置） | ✓ | [api-contracts §3 ObjectMapper](../context/foundation-contracts/foundation-core/api-contracts.md#3) |
| 23 | 使用 Caffeine `CacheManager`，Key 命名合规 | ✓ | [api-contracts §3 CacheManager](../context/foundation-contracts/foundation-core/api-contracts.md#3) |
| 24 | 单元测试继承 `BaseUnitTest` | ✓ | [platform-capabilities §10](../context/foundation-contracts/foundation-core/platform-capabilities.md#10) |
| 25 | 集成测试继承 `BaseIntegrationTest` | ✓ | [platform-capabilities §10](../context/foundation-contracts/foundation-core/platform-capabilities.md#10) |
| 26 | 不自定义 CORS / 安全过滤器 | ✓ | [platform-capabilities §6](../context/foundation-contracts/foundation-core/platform-capabilities.md#6) |
| 27 | BC 配置使用 `metaforge.agent-cognition` 前缀 | ✓ | [configuration-schema](../context/foundation-contracts/foundation-core/configuration-schema.md) |

---

## 9. 平台能力适用性总结

| 平台能力 | 适用于本 BC | 接入方式 |
|---------|-----------|---------|
| 虚拟线程 | ✅ | 零配置继承 |
| 日志脱敏 | ✅ | 内置规则，不自定义 |
| API 文档（SpringDoc） | ✅ | `@Tag("agent-cognition")` |
| 国际化（i18n） | ✅ | 注入 `MessageSource` + 消息文件 |
| 可观测性（Actuator） | ✅ | `HealthCheckSpi` 扩展 |
| 安全基线（XSS/CORS） | ✅ | 零配置继承 |
| 统一响应（ApiResponse\<T\>） | ✅ | 全局自动包装 |
| 分页组件（PageRequest/PageResult） | ✅ | 直接使用 |
| JSONB 序列化（JsonbUtils） | ✅ | 按需使用 |
| 缓存（Caffeine CacheManager） | ✅ | 注入使用，Key 命名合规 |
| Jackson ObjectMapper | ✅ | 注入使用，不自定义 |
| 测试基类（BaseUnitTest/BaseIntegrationTest） | ✅ | 继承使用 |
| 全局异常处理（ExceptionHandlerSpi） | ✅ | 实现扩展接口 |
| 数据源（HikariCP） | ❌ **N/A** | 本 BC 无数据库 |
| Flyway 迁移 | ❌ **N/A** | 本 BC 无数据库 |
| 跨 Schema 写校验 | ❌ **N/A** | 本 BC 无数据库 |
