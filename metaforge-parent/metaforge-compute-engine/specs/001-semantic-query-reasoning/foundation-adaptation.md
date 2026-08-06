# Foundation Adaptation Design: metaforge-compute-engine

**Based on**: `foundation-core` contracts (v1.0.0)
**BC**: metaforge-compute-engine（语义查询与推理引擎）
**Date**: 2026-08-01

---

## 1. 构建系统集成

### 1.1 模块结构注册

本 BC 采用三级 Maven 结构注册到平台构建体系：

- **父模块** `metaforge-compute-engine`（pom），作为聚合 POM，不承载业务代码
- **API 子模块** `metaforge-compute-engine-api`（jar），对外 SDK/契约层
- **Core 子模块** `metaforge-compute-engine-core`（jar），DDD 实现层

**注册操作**:

1. 在 `metaforge-parent/pom.xml` 的 `<modules>` 中添加：

```xml
<module>metaforge-compute-engine</module>
```

2. 在根 POM `<dependencyManagement>` 中添加两个子模块的版本声明：

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-compute-engine-api</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-compute-engine-core</artifactId>
    <version>${revision}</version>
</dependency>
```

3. 在 `metaforge-boot/pom.xml` 中添加 core 模块依赖，启动时加载：

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-compute-engine-core</artifactId>
</dependency>
```

### 1.2 模块依赖声明

| 模块 | 依赖 |
|------|------|
| `metaforge-compute-engine-api` | `metaforge-framework`（提供 `ApiResponse`、`PageRequest`、`PageResult`、`BaseEntity` 等公共 DTO） |
| `metaforge-compute-engine-core` | `metaforge-compute-engine-api` + `metaforge-framework` + `metaforge-metamodel-api` + `metaforge-metadata-api` + `metaforge-graph-api` + jOOQ + MapStruct |

**合规确认**:
- API 模块不包含 `metaforge-boot` 依赖（黑名单禁止项）
- Core 模块不包含 `metaforge-boot`、`metaforge-server` 依赖
- 所有版本号由 `metaforge-parent` BOM 统一管理，模块 POM 不声明 `<version>` 标签
- BC 不声明 `<dependencyManagement>` 段

---

## 2. 配置规范

### 2.1 BC 专属配置

本 BC 在 `metaforge-boot/src/main/resources/` 下创建 `application-metaforge-compute-engine.yml`，所有属性使用 `metaforge.compute-engine` 前缀：

```yaml
metaforge:
  compute-engine:
    traversal:
      max-depth: 5
      timeout-ms: 2000
      max-result-count: 500
    transitivity-rules:
      - type: COMPOSITION
        transitive: true
        direction: forward
        weight-strategy: multiply
        max-depth: 5
        description: "整体-部分层级传递，权重连乘"
      # ...其余规则
```

### 2.2 继承的全局配置

以下配置由 foundation-core 统管，BC 不得覆盖：

| 配置项 | 所属 | 生效策略 |
|--------|------|---------|
| `spring.datasource.*` | foundation-core 通用配置 | 全局生效 |
| `spring.cache.*` | foundation-core Caffeine 配置 | 全局生效（本 BC 不使用缓存） |
| `spring.jackson.*` | foundation-core Jackson 配置 | 全局生效 |
| `spring.threads.virtual.enabled` | foundation-core 虚拟线程 | 全局生效 |
| `spring.flyway.*` | foundation-core Flyway 配置 | 全局生效 |

---

## 3. SPI 扩展点接入

### 3.1 异常处理扩展（ExceptionHandlerSpi）

注册 BC 自定义异常类型映射，错误码范围 **33000-33999**：

```java
@Component
@Order(100)
public class ComputeEngineExceptionHandler implements ExceptionHandlerSpi {
    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof ComputeEngineException ce) {
            return ApiResponse.error(ce.getErrorCode(), ce.getMessage());
        }
        return null; // 非本 BC 异常，交给下一个处理器
    }
}
```

**注册异常类型**:
- `EntityNotFoundException` → 33001
- `TraversalDepthExceededException` → 33002
- `QueryTimeoutException` → 33003
- `InvalidFilterException` → 33006
- `UpstreamServiceUnavailableException` → 33010
- `CircularReferenceException` → 33011

### 3.2 健康检查扩展（HealthCheckSpi）

注册上游 BC 连通性健康检查项：

```java
@Component
public class ComputeEngineHealthCheck implements HealthCheckSpi {
    @Override
    public HealthCheckResult check() {
        // 检查 metaforge-metamodel-api 连通性
        // 检查 metadata_management.metadata_entity 表可读性
        // 检查 semantic_relation_network.relation_instance 表可读性
        return new HealthCheckResult("compute-engine-upstream", healthy, detail);
    }
}
```

### 3.3 不需要扩展的 SPI

以下 SPI 扩展点本 BC 不需要实现：
- **RequestInterceptorSpi**: 无 HTTP 请求拦截需求
- **LogMaskSpi**: foundation-core 内置脱敏规则已覆盖通用场景
- **SerializationSpi**: 使用 foundation-core 统一 Jackson ObjectMapper 配置
- **ValidationSpi**: 使用 JSR-380 标准注解 + 上游 api 模块定义的 DTO 校验

---

## 4. 平台能力消费

### 4.1 统一响应格式

所有 REST Controller 方法返回业务 DTO 或 `PageResult<T>`，由 foundation-core 全局 `GlobalResponseBodyAdvice` 自动包装为 `ApiResponse<T>` 格式。Controller 中不手动构造 `ApiResponse` 对象。

### 4.2 JSONB 序列化

属性字段 content 的 JSONB 序列化/反序列化统一使用 `JsonbUtils`：
- `JsonbUtils.toJsonb(Map<String, Object>)` → 写入查询过滤条件中的 JSONB 等值匹配
- `JsonbUtils.fromJsonb(String, Class<T>)` → jOOQ Record `content` 字段 → `Map<String, Object>`

### 4.3 分页组件

检索型查询（FR-005 多条件复合检索）注入 `PageRequest`，返回 `PageResult<T>`。使用 `PageHelper.toSpringPageRequest()` 转换后传递给上游 api 服务调用。

### 4.4 虚拟线程

零配置继承。Tomcat 请求处理和 `@Async` 方法自动运行在虚拟线程上。本 BC 图形遍历查询为同步阻塞模型，虚拟线程提供足够的并发承载能力。

### 4.5 OpenAPI 文档

Controller 类使用 `@Tag(name = "compute-engine")` 标注分组，由 foundation-core SpringDoc 自动生成 OpenAPI 文档。不添加 `springdoc-openapi` 依赖（已由 foundation-core 提供）。

### 4.6 国际化

在 `metaforge-boot/src/main/resources/i18n/` 下添加 `messages_compute-engine_zh_CN.properties`，按需定义 BC 级 i18n 消息。注入 Spring `MessageSource` 使用。

### 4.7 缓存

本 BC 为纯无状态计算层，查询结果不跨请求缓存。不使用 Caffeine CacheManager。每次请求实时通过 jOOQ 跨 Schema 查询最新生效态数据。

### 4.8 测试基类

集成测试继承 `BaseIntegrationTest`（已内置 TestContainers PostgreSQL 容器自动启停与 Spring Boot Test 上下文）。单元测试继承 `BaseUnitTest`（纯 Mockito，无 Spring 上下文）。

---

## 5. 数据源与 jOOQ 集成

### 5.1 jOOQ DSLContext 配置

本 BC 在 core 模块的 infrastructure 层配置 jOOQ `DSLContext` bean：

```java
@Configuration
public class JooqConfig {
    @Bean
    public DSLContext dslContext(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
```

`DataSource` 由 foundation-core `DataSourceConfig` 自动配置的 HikariCP 连接池统一提供。

### 5.2 跨 Schema 查询

通过 jOOQ `DSL.table(name("schema_name", "table_name"))` 引用上游 BC 的表：
- `metadata_management.metadata_entity`
- `semantic_relation_network.relation_instance`
- `semantic_relation_network.entity_relation_index`

仅执行 SELECT 查询，严禁 INSERT/UPDATE/DELETE 操作。符合 Foundation Contract 跨 Schema 写校验约束。

### 5.3 数据库视图/函数（可选）

若需创建数据库视图或函数辅助复杂 CTE 查询，Flyway 迁移脚本置于 `metaforge-boot/src/main/resources/db/migration/` 下，命名遵循 `V<n>__compute_engine_<描述>.sql` 格式。

---

## 6. 合规确认清单

| 验证项 | 状态 |
|--------|------|
| POM 继承 `metaforge-parent` | ✓ |
| 依赖 `metaforge-framework`（提供公共 DTO/工具） | ✓ |
| 不声明 `<dependencyManagement>` | ✓ |
| 不依赖 `metaforge-boot` 或 `metaforge-server` | ✓ |
| 不在 POM 中覆盖版本属性 | ✓ |
| REST API 复用 `ApiResponse<T>` 格式 | ✓ |
| 实现 `ExceptionHandlerSpi` 注册 BC 异常 | ✓ |
| 实现 `HealthCheckSpi` 注册 BC 健康检查 | ✓ |
| 使用 `JsonbUtils` 统一 JSONB 序列化 | ✓ |
| 检索查询使用 `PageRequest`/`PageResult<T>` | ✓ |
| Controller 使用 `@Tag` 注解（不自定义 SpringDoc） | ✓ |
| 注入 `MessageSource`（不自定义 bean） | ✓ |
| 不配置线程池（虚拟线程继承） | ✓ |
| 不配置数据源（统一数据源继承） | ✓ |
| 仅跨 Schema SELECT 查询（不写操作） | ✓ |
| 测试继承 `BaseUnitTest`/`BaseIntegrationTest` | ✓ |
| Flyway 脚本统一管理 | ✓ |
| 模块已在根 POM 注册 | ✗（待 BC 脚手架创建后注册） |
