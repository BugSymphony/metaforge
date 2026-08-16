# Foundation Adaptation: metaforge-agent-cognition

**Feature**: `001-cognition-infrastructure` | **Date**: 2026-08-11

**Foundation Contracts**: `foundation-core` (v1.0.0), imported at `$BC_PATH/context/foundation-contracts/foundation-core/`

> 本文档描述本 BC 如何对接 foundation-core 平台能力，不重复陈述 foundation 合约的原始内容。所有对接方案遵循非侵入性原则，不修改 foundation 核心源码。

---

## 1. 构建系统集成

### 对接动作

- **模块注册**：在 `metaforge-parent/pom.xml` 的 `<modules>` 中新增 `metaforge-agent-cognition` 聚合父模块条目（四个子模块由 BC 内部 POM 管理）
- **POM 继承**：所有子模块 POM 继承 `com.metaforge:metaforge-parent`，版本由 `$revision` 统一管理
- **依赖声明**：
  - `-api`：仅声明 `metaforge-framework`（compile scope），无上游 BC 依赖
  - `-core`：声明 `metaforge-framework` + 四个上游 api 模块（`metaforge-metamodel-api`, `metaforge-metadata-api`, `metaforge-graph-api`, `metaforge-compute-engine-api`）+ `mapstruct`
  - `-starter`：聚合依赖 `-api`、`-core`，仅供 `metaforge-boot` 引用
- **禁止项**：不声明 `<dependencyManagement>`，不覆盖 `${revision}` 与平台版本属性

### 合规实现

- POM 中依赖版本由 BOM 统一管理，不声明 `<version>` 标签
- 不依赖黑名单中的 `metaforge-boot`、`metaforge-server`
- flatten-maven-plugin 由父 POM 统一配置，BC 无需额外声明

---

## 2. 平台能力复用

### 虚拟线程

- **对接**：不配置任何线程池 Bean，复用 foundation 提供的虚拟线程环境
- **验证**：算子执行线程名包含 `virtual-` 前缀

### 日志脱敏

- **对接**：不做额外配置。scope 参数中的 FQN 字段不属于默认脱敏规则（password/secret/token/phone/email/idCard），无需自定义脱敏
- **扩展**：如需自定义脱敏规则，实现 `LogMaskSpi` 并通过 `@Component` 注册

### API 文档 (SpringDoc)

- **对接**：`CognitionController` 类标注 `@Tag(name = "agent-cognition")`，其余由 foundation 自动处理
- **禁止**：不添加 `springdoc-openapi` 依赖，不自定义 `OpenAPI` bean，不配置 `springdoc.*` 属性

### 国际化 (i18n)

- **对接**：注入 `MessageSource` 使用（在异常消息构造与输出中使用）
- **资源文件**：在 `metaforge-boot/src/main/resources/i18n/` 目录提供 `messages_agent-cognition_zh-CN.properties`（默认中文）与 `messages_agent-cognition_en-US.properties`
- **禁止**：不定义独立的 `MessageSource` bean

### 可观测性 (Actuator)

- **对接**：不做额外配置。如需自定义健康检查，实现 `HealthCheckSpi` 并注册为 Spring Bean
- **健康检查项建议**：`templateRegistryHealth` — 检查内置模板注册状态

### 安全基线

- **对接**：不做额外配置。本 BC API 无独立认证层，scope 中 bundles 白名单即为授权依据
- **XSS/CORS/SQL注入**：由 foundation 统一处理，本 BC 不涉及数据库操作无需 SQL 注入防护

### 数据源与事务

- **对接**：不配置数据源、不配置事务管理器。本 BC 无自有数据库，不对任何表执行读写
- **禁止**：不声明 `spring-boot-starter-data-jpa` 或 `flyway-core` 依赖

### 测试基类

- **对接**：单元测试继承 `BaseUnitTest`，集成测试（Port 适配器）继承 `BaseIntegrationTest`
- **禁止**：不引入 TestContainers 依赖，不自定义测试数据库配置

---

## 3. 统一 API 契约对接

### 统一响应格式

- **对接**：REST Controller 所有方法返回 `ApiResponse<T>`，使用 `ApiResponse.success(data)` 构造成功响应
- **禁止**：不自定义响应包装类，不直接返回裸对象

### 分页组件

- **对接**：使用 `PageRequest`/`PageResult<T>`（上游 Port 方法的返回类型中引用）
- **禁止**：不自定义分页 DTO，不使用 Spring Data 的 `Page`/`Pageable` 直接暴露

### JSONB 序列化

- **对接**：Port 适配器中处理上游返回的 JSONB 数据时使用 `JsonbUtils.fromJsonb()`/`JsonbUtils.toJsonb()`
- **禁止**：不自定义序列化实现，不创建自己的 ObjectMapper

### 缓存管理

- **对接**：`TemplateRegistry` 内部使用 Caffeine（`ConcurrentHashMap`），key 命名遵循 `agent-cognition:template:<templateId>` 约定
- **禁止**：不自定义 CacheManager，不声明额外缓存配置

---

## 4. SPI 扩展点对接

### ExceptionHandlerSpi

- **对接**：创建 `AgentCognitionExceptionHandler` 实现 `ExceptionHandlerSpi`，注册为 `@Component`，`@Order(100)`
- **错误码范围**：34000-34099（在 foundation 约定的 BC 分配范围 30000-49999 内）
- **实现逻辑**：识别本 BC 自定义异常类型（`TemplateNotFoundException`, `InvalidScopeException` 等），映射为 `ApiResponse.error(code, message)`

```java
@Component
@Order(100)
public class AgentCognitionExceptionHandler implements ExceptionHandlerSpi {
    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof TemplateNotFoundException te) {
            return ApiResponse.error(AgentCognitionErrorCodes.TEMPLATE_NOT_FOUND, te.getMessage());
        }
        if (e instanceof InvalidScopeException se) {
            return ApiResponse.error(AgentCognitionErrorCodes.INVALID_SCOPE, se.getMessage());
        }
        // ... 其他异常类型映射
        return null;
    }
}
```

### HealthCheckSpi

- **建议**：实现 `HealthCheckSpi`，检查 `TemplateRegistry` 中内置模板是否全部就绪

### 其他 SPI

- `RequestInterceptorSpi`：当前无使用场景，不实现
- `LogMaskSpi`：当前 scope 中的 FQN 不属于敏感字段模式，不实现
- `SerializationSpi`：当前无自定义序列化需求，不实现
- `ValidationSpi`：当前无自定义校验注解需求，不实现

---

## 5. 配置 Schema 对接

### BC 专用配置文件

- **文件路径**：`metaforge-boot/src/main/resources/application-agent-cognition.yml`
- **属性前缀**：`metaforge.agent-cognition`
- **环境覆盖**：`application-cognition-dev.yml`（dev profile），通过 `spring.config.import` 导入

### 核心配置项

```yaml
metaforge:
  agent-cognition:
    templates:
      classpath-location: classpath:cognition/templates/
      external-location: file:${META_FORGE_CONFIG:cognition}/templates/
      hot-reload:
        enabled: false
        poll-interval-ms: 5000
    defaults:
      cognition-depth: L2
      agent-archetype: execution
      format: json
      max-tokens: 8000
      page-size: 20
    timeouts:
      operator-execute-default-ms: 10000
    depth:
      trim-ratio-l1: 0.33
      trim-ratio-l2: 0.67
      min-keep: 3
    version-anchor:
      bundle-resolve-strategy: LATEST_PUBLISHED
```

- **合规**：不覆盖 `spring.*` / `server.*` / `management.*` 等 foundation 管理的全局配置
- **零配置可用**：所有配置项具有合理默认值，缺失时系统正常启动

---

## 6. 合规总结

| 检查项 | 状态 |
|--------|------|
| 不修改 foundation-core 源码 | PASS |
| 不重复实现平台通用能力 | PASS |
| 通过 SPI 扩展点接入自定义逻辑 | PASS |
| BC 专用配置使用独立命名空间 | PASS |
| 构建集成遵循父子 POM 规范 | PASS |
| 依赖不访问黑名单模块 | PASS |
