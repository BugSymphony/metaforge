# Research Document: 元数据全生命周期管理

**Feature**: 001-metadata-full-lifecycle
**Date**: 2026-08-01
**Status**: Complete

## 1. JSON Schema 运行时校验方案

### 决策

使用 **NetworkNT JSON Schema Validator**（`com.networknt:json-schema-validator`）作为 JSON Schema 运行时校验引擎。

### 理由

- NetworkNT 是目前 Java 生态中最活跃的 JSON Schema 校验实现，支持 Draft 2020-12 规范子集
- 与上游 metamodel-governance BC 编译生成的 JSON Schema 格式（Draft 2020-12 平面扁平结构）完全兼容
- 支持结构化错误输出（JSON Path 定位、违规类型分类），直接满足 FR-010 要求
- 零外部依赖，体积小（~500KB），适合嵌入调用

### 备选方案及拒绝原因

| 方案 | 拒绝原因 |
|------|---------|
| Everit JSON Schema | 社区活跃度低，长期不维护 |
| 手动递归校验（基于 Jackson `JsonNode`） | 开发成本高，无法标准化，复杂约束（正则、嵌套数组）难以正确实现 |
| Spring Validation 集成 | JSON Schema 的动态性（每个 EntitySchema 不同）超出 JSR-380 静态注解能力 |

### 实现策略

- 封装 `SchemaValidationService` 领域服务（`domain/service/SchemaValidationService.java`），注入 `ObjectMapper` 与 `JsonSchemaFactory`
- 校验时动态创建 `JsonSchema` 对象（可缓存按 `entity_schema_fqn` 维度），对 `content` JSONB 字段执行 `validate()`
- 校验失败时转换为自定义 `ValidationErrorDetailDto`（包含 `jsonPath`、`violationType`、`ruleReference`）

## 2. FQN Generator 领域服务设计

### 决策

在 `metaforge-metadata-core` 模块的 `domain/service/` 下定义 `FqnGenerator` 领域服务（`@Component`），与 metamodel BC 的 `FqnGenerator` 模式一致，提供以下 API：

```java
@Component
public class FqnGenerator {
    public String generateChildFqn(String parentFqn, String segment);
    public String extractParentFqn(String fqn);
    public List<String> splitSegments(String fqn);
    public String extractRootFqn(String fqn);
    public boolean isValidSegment(String segment);
    public boolean isReservedCharInSegment(String segment);
}
```

### 理由

- FR-014a 明确要求统一 FQN 生成器具备四项核心能力
- 放置于 `domain/service/` 作为领域服务，与 metamodel BC 的 `FqnGenerator` 架构模式保持一致
- `@Component` 注解由 Spring 管理，通过构造器注入到 Application Service 使用
- M2 层 FQN（`entity_schema_fqn`）与 M1 层 FQN 分离——本生成器仅处理 M1 层 FQN（点分隔层级路径）

### Segment 合法性校验规则

- 正则：`[A-Za-z][A-Za-z0-9_-]*`
- 禁止包含保留分隔符 `.`
- 子实体 FQN = `parentFqn + "." + segment`

## 3. MapStruct 对象转换规范

### 决策

遵循 metamodel BC 已建立的标准模式：

- `@Mapper(componentModel = "spring")` 接口，存放于 `infrastructure/mapper/`
- 三个核心映射方向：
  1. `toJpo(DomainObject) → JpoObject`（领域 → JPA）
  2. `toDomain(JpoObject) → DomainObject`（JPA → 领域）
  3. `toDto(DomainObject) → DtoObject`（领域 → API DTO）

### 转换范围限制

- **仅限 infrastructure 层**：`api` 模块、`domain` 模块严禁引入 MapStruct 依赖
- DTO ↔ Domain 转换在 Application Service 层手写映射方法（与 metamodel BC 一致）
- JPO ↔ Domain 转换在 Repository Adapter 层通过 MapStruct 完成

### 特殊处理

- `FQN` 值对象 → `String fqn` 字段映射：使用 `@Mapping(target = "fqn", expression = "java(...)")` 与自定义 `default` 方法
- `VersionNumber` 值对象 → `Integer currentVersion` 字段映射：同上模式
- JSONB 字段 `content`：JPO 侧为 `String`（通过 `@JdbcTypeCode(SqlTypes.JSON)` 注解），Domain 层为 `Map<String, Object>`，MapStruct 通过 `@Named` 方法进行 `String` ↔ `Map` 互转
- JSONB 字段 `embedding`：JPO 侧为 `List<Float>`（通过 `@Type(JsonType.class)` 注解），Domain 层同为 `List<Float>`，无需 MapStruct 特殊转换

## 4. Spring AI MCP Server 集成模式

### 决策

参考 metamodel BC 的 `MetamodelMcpTools.java` 模式：

- `@Component` 注解的 Spring Bean
- 使用 `@Tool(description = "...")` 标注方法暴露 MCP 工具
- 使用 `@ToolParam(description = "...")` 标注参数说明
- 构造器注入 Application Service 接口（来自 `api` 模块）
- 工具方法调用 Application Service 获取 DTO，直接返回 DTO 对象（Spring AI 会自动序列化为结构化 JSON）

### MCP 工具清单（面向 Agent 消费）

| 工具名 | 描述 | 对应 FR |
|--------|------|--------|
| `getMetadataEntity` | FQN 精准查询生效元数据 | FR-026 |
| `queryMetadataByPrefix` | FQN 前缀范围查询（OR 并集逻辑） | FR-027 |
| `queryMetadataBySchema` | 按 EntitySchema FQN 查询 | FR-028 |
| `queryMetadataByAttribute` | 按属性条件组合查询 | FR-029 |
| `getEntityVersionHistory` | 查询全历史版本列表 | FR-023 |
| `getEntityVersionDetail` | 按版本号查询历史详情 | FR-024 |
| `compareVersions` | 两个历史版本差异对比 | FR-025 |

## 5. PostgreSQL JSONB 属性查询模式

### 决策

使用原生 PostgreSQL JSONB 操作符实现属性条件查询（FR-029）：

```java
// 精准匹配
@Query(value = "SELECT * FROM metadata_management.metadata_entity WHERE content @> CAST(:conditionJson AS jsonb)", nativeQuery = true)
List<MetadataEntityJpo> findByAttributeExactMatch(String conditionJson);

// 模糊前缀匹配（使用 jsonb_path_ops 表达式）
@Query(value = "SELECT * FROM metadata_management.metadata_entity WHERE content::jsonb->>:field = :value", nativeQuery = true)
List<MetadataEntityJpo> findByAttributeField(String field, String value);
```

### 理由

- `@>` （contains）操作符天然支持 JSONB 嵌套对象的子匹配，性能优于文本 `LIKE`
- 主表 FQN 前缀查询使用标准 PostgreSQL `LIKE`（`fqn LIKE :fqnPrefix || '%'`）配合 `btree` 索引
- FQN 前缀多值 OR 查询（FR-027）使用 `fqn LIKE ANY(ARRAY[...])` 模式

## 6. 变更事件发布机制

### 决策

使用 **Spring ApplicationEvent** 内存事件机制（与 global-plan 中 `EVT-001/EVT-002/EVT-003` 事件定义一致）。

### 实现方式

- 定义 `MetadataChangeEvent` 继承 `ApplicationEvent`，携带 `fqn`、`changeType`（ACTIVATE/DEPRECATE）、`version`、`timestamp`
- 在 `MetadataEventPublisher` 中，生效/下线事务成功提交后通过 `ApplicationEventPublisher.publishEvent()` 发布
- 下游 `semantic-relation-network` BC 通过 `@EventListener` 或 `@TransactionalEventListener(phase = AFTER_COMMIT)` 接收

### 理由

- MVP 阶段不引入消息队列（全局宪法 ASM-005），Spring ApplicationEvent 在单 JVM 内足够
- 事务内发布 `@TransactionalEventListener(phase = AFTER_COMMIT)` 确保仅在事务提交后通知，避免事务回滚时误通知（FR-036 要求）
- 若未来引入 MQ，事件格式无需变更，仅替换发布通道

## 7. YAML/JSON 导入导出库选择

### 决策

- **JSON**：复用平台 `ObjectMapper`（Jackson），无需额外依赖
- **YAML**：引入 `jackson-dataformat-yaml`（Jackson YAML 扩展），与 Jackson 生态完全兼容

### 实现策略

- 定义标准化导出格式（JSON 对象数组，每条含 `fqn`、`name`、`entitySchemaFqn`、`content`、`embedding`）
- 导入/导出均使用同一 `ObjectMapper`（配置 YAML Factory 用于 YAML 格式检测）
- 导入逐条解析 → Schema 校验 → 草稿表写入（不直接写主表，符合 FR-040）

## 8. 版本差异对比算法

### 决策

使用 **递归 JSON 扁平化 + Map Diff** 算法。

### 算法描述

1. 分别将 version-A 和 version-B 的 `content` JSONB 扁平化为 `Map<String, Object>`（key 为点分隔路径，如 `order.customer.name`）
2. 遍历两个 Map 的 Key Set：
   - key 仅存在于 version-A → 标记为 DELETED
   - key 仅存在于 version-B → 标记为 ADDED
   - key 均存在且值相同 → 跳过
   - key 均存在且值不同 → 标记为 MODIFIED
3. 按 ADDED / MODIFIED / DELETED 分类组织差异结果

### 实现封装

- `VersionDiffService` 领域服务（`domain/service/VersionDiffService.java`），输出 `List<VersionDiffDto>`
- 返回结果按 FR-025 要求分类展示（新增字段、修改字段、删除字段）

## 9. 事务管理策略

### 生效原子事务（FR-016）

```java
@Transactional
public MetadataEntityDto activate(String fqn) {
    // 1. 全量预校验（结构合规 + 组合层级 + 元模型版本）
    // 2. 主表写入/覆盖：metadataEntityRepository.save()
    // 3. 历史表归档（版本号递增）：entityVersionRepository.save()
    // 4. 草稿表删除：metadataEntityDraftRepository.deleteByFqn()
    // 5. 发布变更事件：eventPublisher.publishEvent(...)
}
```

- 使用 `@Transactional` 保证四步原子性
- 预校验失败直接抛异常，事务回滚
- 变更事件通过 `@TransactionalEventListener(phase = AFTER_COMMIT)` 延迟到事务提交后发布

### 下线原子事务（FR-020）

- `@Transactional` 保护：主表删除 + 历史表保持不变
- 下线前先调用 `DeactivationBlockedException` 检查（外部引用 + 子实体状态）

## 10. 错误码分配

### 决策

metadata-management BC 使用错误码范围 **31000-31099**（遵循 foundation-core REST API Contract 中 BC 分配规则 30000-49999）。

| 错误码 | 常量名 | 描述 |
|--------|--------|------|
| 31001 | `FQN_CONFLICT` | FQN 全局唯一性冲突 |
| 31002 | `FQN_INVALID_SEGMENT` | FQN segment 不符合文法 |
| 31003 | `SCHEMA_VALIDATION_FAILED` | JSON Schema 结构校验失败 |
| 31004 | `ENTITY_NOT_FOUND` | 生效元数据不存在 |
| 31005 | `DRAFT_NOT_FOUND` | 草稿不存在 |
| 31006 | `VERSION_NOT_FOUND` | 历史版本不存在 |
| 31007 | `ACTIVATION_FAILED` | 生效操作失败 |
| 31008 | `DEACTIVATION_BLOCKED` | 下线操作被拦截（存在活跃引用/生效子实体） |
| 31009 | `PARENT_NOT_ACTIVE` | 父实体未生效（子实体创建拦截） |
| 31010 | `SCHEMA_VERSION_NOT_PUBLISHED` | 绑定元模型版本未发布（FR-014） |
| 31011 | `IMPORT_PARSE_FAILED` | 导入文件解析失败 |
| 31012 | `FQN_SEGMENT_RESERVED_CHAR` | FQN segment 包含保留分隔符 `.` |
