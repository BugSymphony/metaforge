# Foundation Adaptation Design: metadata-management BC

**Feature**: 001-metadata-full-lifecycle
**BC**: metadata-management
**Foundation**: foundation-core v1.0.0

## 概述

本文档描述 metadata-management BC 如何接入 foundation-core 提供的平台预置能力，所有接入方案遵循**非侵入原则**——仅声明式使用平台 API，禁止修改 foundation-core 源码或全局配置。

---

## 1. 构建系统集成

### 1.1 模块注册声明

在 `metaforge-parent/pom.xml` 的 `<modules>` 中加入本 BC 的聚合模块，并在 `metaforge-boot/pom.xml` 中加入 runtime 依赖。

**根 POM 注册**（`metaforge-parent/pom.xml`）：

```xml
<modules>
    <!-- 已有模块 -->
    ...
    <!-- 新增 metadata-management BC -->
    <module>metaforge-metadata</module>
</modules>
```

**启动模块注册**（`metaforge-boot/pom.xml`）：

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-metadata-core</artifactId>
</dependency>
```

### 1.2 BC POM 结构

三级 POM 继承链：

```
metaforge-parent (${revision})
  └── metaforge-metadata (pom, 聚合父模块)
        ├── metaforge-metadata-api (jar, 契约层)
        └── metaforge-metadata-core (jar, 实现层, 依赖 api)
```

**Root POM** (`metaforge-metadata/pom.xml`)：
- 继承 `metaforge-parent`
- `packaging: pom`
- `<modules>`: `metaforge-metadata-api`, `metaforge-metadata-core`
- `<dependencyManagement>`: 声明两个子模块及版本

**API POM** (`metaforge-metadata-api/pom.xml`)：
- 继承 `metaforge-metadata`
- 单一依赖：`metaforge-framework`（传递引入 common + Spring Boot）
- 禁止 `<dependencyManagement>`

**Core POM** (`metaforge-metadata-core/pom.xml`)：
- 继承 `metaforge-metadata`
- 依赖：`metaforge-metadata-api` + `metaforge-framework` + `metaforge-metamodel-api`
- 测试依赖：`metaforge-framework` (test-jar) + `spring-boot-starter-test`
- 引入 `jackson-dataformat-yaml`（YAML 导入导出）
- 引入 `com.networknt:json-schema-validator`（JSON Schema 运行时校验）
- 引入 MapStruct + MapStruct Processor

### 1.3 依赖声明合规

- 所有版本由 BOM（`metaforge-parent` 的 `<dependencyManagement>`）集中管理，BC POM 中**不出现 `<version>` 标签**
- 不声明 `<dependencyManagement>` 段
- 严禁依赖 `metaforge-boot`（违反 Maven Enforcer 规则）
- `metaforge-server`、`metaforge-boot` 为平台启动模块，BC 不得直接依赖

---

## 2. 平台预置能力接入方案

### 2.1 统一响应格式

**能力来源**: `ApiResponse<T>` (common)

**接入方案**:
- 所有 REST Controller 方法返回 `ApiResponse<T>` 包装的业务数据
- 全局异常处理自动将异常转换为 `ApiResponse<?>` 错误格式
- 不自定义响应包装类

**代码示例**:
```java
@RestController
@RequestMapping("/api/metadata")
@Tag(name = "metadata-management")
public class MetadataDraftController {

    @PostMapping("/drafts")
    public ApiResponse<MetadataEntityDraftDto> createDraft(@Valid @RequestBody CreateDraftRequest request) {
        MetadataEntityDraftDto result = metadataDraftService.createDraft(request);
        return ApiResponse.success(result, "草稿创建成功");
    }
}
```

### 2.2 异常处理扩展

**能力来源**: `ExceptionHandlerSpi` (common)

**接入方案**:
- 实现 `ExceptionHandlerSpi` 接口并注册为 `@Component`
- 在 `handle()` 方法中匹配本 BC 的自定义异常类型，返回 `ApiResponse.error()`
- 未匹配的异常返回 `null`，委托给下一个 handler 或默认 handler

**实施方案**:

```java
@Component
public class MetadataExceptionHandler implements ExceptionHandlerSpi {
    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof MetadataValidationException ex) {
            return ApiResponse.error(ErrorCodes.SCHEMA_VALIDATION_FAILED, ex.getMessage());
        }
        if (e instanceof FqnConflictException ex) {
            return ApiResponse.error(ErrorCodes.FQN_CONFLICT, ex.getMessage());
        }
        if (e instanceof EntityNotFoundException ex) {
            return ApiResponse.error(ErrorCodes.ENTITY_NOT_FOUND, ex.getMessage());
        }
        // ... 其他异常类型匹配
        return null; // 委托给下一个 handler
    }
}
```

**自定义异常基类**:
- 继承 `com.metaforge.common.exception.BizException`
- 构造器中传入错误码（来自 `MetadataErrorCodes` 常量类）

### 2.3 国际化消息

**能力来源**: `MessageSource` (平台预配置)

**接入方案**:
- 通过 `@Autowired private MessageSource messageSource` 注入
- 在 `metaforge-boot/src/main/resources/i18n/` 目录下添加本 BC 的扩展消息文件：
  - `messages_metadata_zh_CN.properties`
  - `messages_metadata_en_US.properties`

**消息文件示例** (`messages_metadata_zh_CN.properties`):
```properties
metadata.error.fqn_conflict=FQN 已存在：{0}
metadata.error.schema_validation=JSON Schema 校验失败：字段 {0} 违反 {1} 约束
metadata.error.entity_not_found=元数据实体 {0} 不存在或已下线
metadata.success.draft_created=草稿 {0} 创建成功
metadata.success.activated=元数据 {0} 生效成功，版本号 {1}
```

### 2.4 分页组件

**能力来源**: `PageRequest` / `PageResult<T>` / `PageHelper` (common/framework)

**接入方案**:
- 查询接口接收 `PageRequest` 参数
- 使用 `PageHelper.toSpringPageRequest()` 转换为 Spring Data `Pageable`
- 使用 `PageHelper.fromSpringPage()` 将 `org.springframework.data.domain.Page` 转换为 `PageResult<T>`

```java
Pageable pageable = PageHelper.toSpringPageRequest(pageRequest);
Page<MetadataEntityJpo> jpoPage = jpaRepository.findByFqnPrefix(fqnPrefix, pageable);
PageResult<MetadataEntityDto> result = PageHelper.fromSpringPage(jpoPage.map(mapper::toDto));
```

### 2.5 JSONB 序列化

**能力来源**: `JsonbUtils` (common)

**接入方案**:
- JPO 层：entity 字段使用 `@JdbcTypeCode(SqlTypes.JSON)` 注解，类型为 `String`
- Domain 层：content 使用 `Map<String, Object>`；embedding 使用 `List<Float>`（JPO 层通过 `@Type(JsonType.class)` 注解映射到 JSONB）
- 转换使用 `JsonbUtils.toJsonb(Map)` 和 `JsonbUtils.fromJsonb(String, Class)`

### 2.6 缓存接入

**能力来源**: Caffeine `CacheManager` (framework)

**接入方案**:
- 按需注入 `CacheManager`
- 缓存 key 命名：`metadata:<entity-type>:<identifier>`
- 场景：上游 EntitySchema 的 JSON Schema 缓存（以 `entity_schema_fqn` 为 key）

### 2.7 健康检查扩展

**能力来源**: `HealthCheckSpi` (common)

**接入方案**:
- 实现 `HealthCheckSpi.check()` 方法
- 检查项示例：数据库连接可用性（通过执行 `SELECT 1` 验证）

### 2.8 测试基类

**能力来源**: `BaseUnitTest` / `BaseIntegrationTest` (framework test-jar)

**接入方案**:
- 单元测试继承 `BaseUnitTest`（无 Spring 上下文，纯 Mockito）
- 集成测试继承 `BaseIntegrationTest`（自动启动 TestContainers PostgreSQL 容器）
- 禁止自定义 TestContainers 配置或测试数据源

### 2.9 虚拟线程

**能力来源**: 平台全局启用

**接入方案**:
- 无任何配置——平台已全局启用 `spring.threads.virtual.enabled=true`
- 严禁配置 `ThreadPoolTaskExecutor` 或自定义线程池

### 2.10 API 文档

**能力来源**: SpringDoc OpenAPI (server 自动配置)

**接入方案**:
- Controller 类使用 `@Tag(name = "metadata-management")` 标注分组
- 禁止添加 `springdoc-openapi` 依赖或自定义 `OpenAPI` Bean

### 2.11 日志与安全基线

**能力来源**: 平台全局配置

**接入方案**:
- 使用 SLF4J 日志门面输出日志（Lombok `@Slf4j`）
- 敏感字段脱敏由平台内置规则处理，不自定义
- 禁止配置 CORS、XSS Filter、`WebMvcConfigurer.addCorsMappings()`

---

## 3. 数据源与 Flyway 集成

### 3.1 数据源

- 使用平台统一配置的 HikariCP 连接池 + 单 PostgreSQL 数据源
- 所有数据库操作通过 JPA/Hibernate 或 `JdbcTemplate` 执行
- 仅对 `metadata_management` Schema 执行写操作

### 3.2 Flyway 迁移脚本

迁移脚本存放于 `metaforge-boot/src/main/resources/db/migration/`，命名规范：

```
V005__metadata_ddl.sql       # 建表 DDL（三表）
V006__metadata_init.sql      # 初始数据（如有）
```

**DDL 关键约束**:
- `metadata_entity.fqn` UNIQUE 索引
- `metadata_entity_draft.fqn` UNIQUE 索引
- `entity_version` (fqn, version) 联合唯一索引
- `entity_version` 表无 UPDATE/DELETE 权限（通过数据库角色控制或应用层禁止）
- 所有 JSONB 字段使用 `jsonb` 类型

---

## 4. 跨 Schema 读约束

本 BC 的 `metadata_management` Schema 为权威写域。跨 BC 读操作仅在以下条件同时满足时执行：

1. 仅执行 SELECT 查询
2. 读取内容限定在合约字段范围
3. 不缓存或复制被读取数据到自有 Schema

对上游 `metamodel_governance` Schema 的访问：通过 `metaforge-metamodel-api` 的 Java 接口调用，不直接执行跨 Schema SQL。

---

## 5. 配置项（`metaforge.metadata.*` 命名空间）

所有 BC 特有配置使用 `metaforge.metadata.*` 命名空间，通过 `@ConfigurationProperties(prefix = "metaforge.metadata")` 绑定。

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `metaforge.metadata.schema.validation.cache-ttl` | `Duration` | `30m` | JSON Schema 校验缓存 TTL |
| `metaforge.metadata.import.max-batch-size` | `int` | `500` | 批量导入单批次最大条数 |
| `metaforge.metadata.export.default-format` | `String` | `json` | 导出默认格式（json/yaml） |
| `metaforge.metadata.history.readonly` | `boolean` | `true` | 历史表只读保护开关 |
