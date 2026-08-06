# foundation-core 平台适配方案：metaforge-graph

**Feature**: 001-relation-instance-lifecycle | **Date**: 2026-08-01

## 说明

本文档描述 `metaforge-graph` BC 如何适配 foundation-core 平台的各项通用能力。所有适配方案遵循非侵入性原则，仅包含 BC 自身的对接动作与实现方案，不重复 foundation 合同中的具体定义与规则。仅生成 foundation 合同中实际定义的、本 BC 需要对接的能力模块对应章节。

---

## 1. 构建系统集成

### Maven 聚合 POM 注册

**对接动作**：
- `metaforge-graph/pom.xml`（聚合父 POM）配置 `<modules>` 声明 `metaforge-graph-api` 与 `metaforge-graph-core` 两个子模块
- 聚合父 POM 继承 `metaforge-parent`，统一管理依赖版本
- API 子模块 POM：不声明额外业务依赖（纯接口定义模块）
- Core 子模块 POM：依赖 `metaforge-graph-api`、`metaforge-metamodel-api`、`metaforge-metadata-api`

**实施方案**：
```xml
<!-- metaforge-graph/pom.xml (聚合父 POM) -->
<parent>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-parent</artifactId>
    <version>${revision}</version>
    <relativePath>../pom.xml</relativePath>
</parent>
<artifactId>metaforge-graph</artifactId>
<packaging>pom</packaging>
<modules>
    <module>metaforge-graph-api</module>
    <module>metaforge-graph-core</module>
</modules>
```

**禁止行为**：
- 不在聚合父 POM 中声明 `<dependencyManagement>` 章节
- 不在子模块 POM 中使用 `<version>` 标签声明版本（由 BOM 统一管理）
- 不依赖 `metaforge-boot` 或 `metaforge-server`

---

## 2. 数据源与事务管理

**对接动作**：
- 复用 foundation-core 预配置的 HikariCP 连接池与 `DataSource` bean
- 复用 foundation-core 预配置的 `@Transactional` 事务管理器
- 所有 DML 操作严格限定在 `semantic_relation_network` Schema 范围内

**实施方案**：
- JPA 持久化实体通过 `@Table(schema = "semantic_relation_network")` 指定 Schema
- 仓储适配器中注入 `EntityManager` 执行 JPA 操作
- 生效/下线操作使用 `@Transactional(rollbackFor = Exception.class)` 保证原子性

**禁止行为**：
- 不定义独立的 `DataSource` bean
- 不配置独立的 `PlatformTransactionManager`
- 不执行跨 Schema 的 INSERT/UPDATE/DELETE 操作

---

## 3. Flyway 数据库迁移

**对接动作**：
- 提供 Flyway 迁移脚本，遵循 `V<n>__metaforge-graph_ddl.sql` 命名约定
- 脚本存放在 `metaforge-boot/src/main/resources/db/migration/` 统一目录

**实施方案**：
- `V<n>__metaforge-graph_ddl.sql`：创建 `semantic_relation_network` Schema 及三表 + 索引表 DDL
- `V<n+1>__metaforge-graph_init.sql`：初始化基础数据（如需）
- 启用 `pg_trgm` 扩展（GIN 索引加速 ILIKE）

**禁止行为**：
- 不在 BC 模块内配置 Flyway 或引入 `flyway-core` 依赖
- 不自定义 Flyway migration 路径

---

## 4. 全局异常处理

**对接动作**：
- 通过实现 `ExceptionHandlerSpi` 扩展全局异常处理器
- 注册自定义业务异常码映射（错误码范围：32000-32099）

**实施方案**：
- `metaforge-graph-core` 中创建 `GraphExceptionHandlerSpi` 实现 `ExceptionHandlerSpi`
- 使用 `@Order(100)` 控制处理链顺序
- 定义 `GraphBizException`（继承 `BizException`）作为 BC 统一业务异常基类

**禁止行为**：
- 不自定义 `@RestControllerAdvice`
- 不实现独立的异常处理切面
- 不使用 foundation 预定义错误码范围外的 error code

---

## 5. REST API 规范

**对接动作**：
- 响应统一由 foundation-core 全局切面自动包装为 `ApiResponse<T>` 格式
- 分页接口使用 `PageRequest` 入参、`PageResult<T>` 出参

**实施方案**：
- Controller 方法直接返回业务 DTO，由全局切面自动包装 `ApiResponse<T>`
- 分页接口：`public ApiResponse<PageResult<RelationInstanceDto>> query(...)`
- 错误时抛出 `GraphBizException`，由 `ExceptionHandlerSpi` 转换并统一包装

**禁止行为**：
- 不手动构造 `ApiResponse<T>` 作为 Controller 返回值
- 不自定义响应包装类
- 不自定义分页 DTO

---

## 6. JSONB 序列化

**对接动作**：
- 使用 `JsonbUtils.toJsonb()` / `JsonbUtils.fromJsonb()` 进行 JSONB 字段与 Java 对象的双向转换
- 在 JPA `AttributeConverter` 中调用 `JsonbUtils`

**实施方案**：
- 定义 `ContentJsonbConverter`（实现 `AttributeConverter<Map<String, Object>, String>`）用于 content 字段
- 定义 `EmbeddingJsonbConverter`（实现 `AttributeConverter<List<Float>, String>`）用于 embedding 字段
- Converter 内部调用 `JsonbUtils.toJsonb()` / `JsonbUtils.fromJsonb()`

**禁止行为**：
- 不自定义 Jackson `ObjectMapper` 序列化配置
- 不手动使用 `ObjectMapper.writeValueAsString()` 替代 `JsonbUtils`

---

## 7. OpenAPI 文档

**对接动作**：
- 仅通过 `@Tag(name = "语义关系管理")` 注解标注 Controller 分组
- 依赖 foundation-core 预配置的 SpringDoc 自动生成 OpenAPI 3.0 文档

**实施方案**：
- 每个 Controller 类添加 `@Tag` 注解
- Controller 方法使用 `@Operation` 和 `@Parameter` 补充描述信息
- Swagger UI 通过 `/swagger-ui.html` 统一访问

**禁止行为**：
- 不配置 `springdoc-openapi` 依赖或自定义 `OpenAPI` bean
- 不自定义 API 文档生成逻辑

---

## 8. 国际化 (i18n)

**对接动作**：
- 通过 foundation-core 预配置的 `MessageSource` 注入使用
- 扩展 i18n 资源文件：`messages_metaforge-graph_zh_CN.properties` / `messages_metaforge-graph_en_US.properties`
- 资源文件放置于 `metaforge-boot/src/main/resources/i18n/`

**实施方案**：
- 在应用服务中注入 `MessageSource`，通过 `messageSource.getMessage("graph.error.fqn_conflict", null, locale)` 获取国际化消息
- 异常信息构造时调用 `MessageSource` 按当前 locale 获取消息

**禁止行为**：
- 不定义独立的 `MessageSource` bean
- 不在 BC 模块内创建独立的 i18n 资源目录

---

## 9. 缓存管理

**对接动作**：
- 使用 foundation-core 预配置的 Caffeine `CacheManager`
- 缓存 key 遵循命名约定 `metaforge-graph:<entity>:<id>`

**实施方案**：
- 注入 `CacheManager` bean
- RelationSchema JSON Schema 缓存：`metaforge-graph:schema:{relationSchemaFqn}`，TTL 30 分钟
- 如需要，可通过 `spring.cache.caffeine.spec` 调整本 BC 相关缓存规格（在 `application.yml` 中配置）

**禁止行为**：
- 不自定义 `CacheManager` bean
- 不引入 Redis 或其他外部缓存中间件

---

## 10. 健康检查

**对接动作**：
- 通过实现 `HealthCheckSpi` 注册 BC 级别的健康检查项

**实施方案**：
- 实现 `GraphHealthCheckSpi`，检查 PostgreSQL 数据库连通性
- 返回 `HealthCheckResult`，包含 name、healthy 状态、detail 描述

**禁止行为**：
- 不自定义 Actuator 端点路径
- 不覆盖默认指标注册

---

## 11. 测试基础设施

**对接动作**：
- 单元测试继承 `BaseUnitTest`（不启动 Spring 上下文）
- 集成测试继承 `BaseIntegrationTest`（自动启动 TestContainers PostgreSQL）

**实施方案**：
- 单元测试：Mockito 模拟仓储端口
- 集成测试：TestContainers 提供真实 PostgreSQL 实例，Flyway 自动执行迁移
- 不手动配置测试数据源

**禁止行为**：
- 不引入独立的 TestContainers 依赖配置
- 不自定义测试框架

---

## 12. 安全基线

**对接动作**：
- 复用 foundation-core 预配置的 XSS 过滤器、请求体大小限制（10MB）、SQL 注入防护

**实施方案**：
- JPA 参数化查询天然防 SQL 注入
- 批量导入 content 字段超 10MB 时由 foundation-core 统一拦截并返回 400 错误
- 不需要自定义安全过滤器

**禁止行为**：
- 不配置独立的 `CorsFilter` 或 `WebMvcConfigurer.addCorsMappings()`
- 不自定义安全策略
