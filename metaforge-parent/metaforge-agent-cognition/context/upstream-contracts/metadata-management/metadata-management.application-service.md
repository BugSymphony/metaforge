---
id: metadata-management.application-service
protocol: Java Interface
version: 1.0.0
owner: metadata-management
description: 元数据管理 BC 对外暴露的 Application Service 接口契约。下游 BC（semantic-relation-network、semantic-query-engine、agent-consumption）通过 Maven 依赖 metaforge-metadata-api 模块后进行进程内调用。
type: business
---

# Application Service Contract: metadata-management

**Protocol**: Application Service（进程内 Java Interface 调用）
**Module**: `metaforge-metadata-api`
**Version**: 1.0.0

> 下游 BC 通过 Maven 依赖 `metaforge-metadata-api` 模块，注入以下接口的 Spring Bean 实例进行进程内调用。对外发布为 SDK，仅允许依赖此模块，严禁依赖 `metaforge-metadata-core`。

---

## maven 依赖

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-metadata-api</artifactId>
    <version>${revision}</version>
</dependency>
```

---

## 1. MetadataDraftService

**职责**: 元数据草稿的全生命周期管理——创建、编辑、查询、删除。草稿完全隔离于正式版本，对外不可见。

### @OpenHostService

```java
/**
 * 元数据草稿管理服务。
 * <p>
 * 草稿是元数据实体的编辑态副本，存储于 metadata_entity_draft 表，
 * 与主表（metadata_entity）物理隔离，对外完全不可见。
 * 每次保存时实时执行 JSON Schema 结构校验，仅校验通过的数据才允许写入。
 * 同一 FQN 最多仅允许存在一条草稿。
 */
public interface MetadataDraftService {

    /**
     * 基于已发布的 EntitySchema 创建全新元数据草稿。
     * <p>
     * 前置校验：
     * <ul>
     *   <li>FQN segment 符合 [A-Za-z][A-Za-z0-9_-]* 文法且不含保留分隔符 '.'</li>
     *   <li>FQN 全局唯一（主表 + 草稿表联合查重）</li>
     *   <li>entity_schema_fqn 对应版本已发布（非 DRAFT）</li>
     *   <li>若指定 parent_fqn，父实体必须已生效</li>
     *   <li>content 符合 EntitySchema JSON Schema 全字段结构校验</li>
     * </ul>
     *
     * @param request 草稿创建请求（含 fqn、name、entitySchemaFqn、content 等）
     * @return 创建成功的草稿 DTO
     * @throws FqnConflictException 如果 FQN 已存在于主表或草稿表
     * @throws MetadataValidationException 如果 JSON Schema 结构校验失败
     */
    MetadataEntityDraftDto createDraft(CreateDraftRequest request);

    /**
     * 基于已生效版本创建修改草稿。
     * <p>
     * 草稿内容从主表全量复制，base_version 记录原版本号。
     * 创建后允许自由编辑 content，但 fqn、parent_fqn、entity_schema_fqn 不可变更。
     *
     * @param fqn 已生效元数据的 FQN
     * @return 基于生效版本创建的草稿 DTO
     * @throws EntityNotFoundException 如果 FQN 对应生效版本不存在
     * @throws FqnConflictException 如果该 FQN 已存在草稿
     */
    MetadataEntityDraftDto createDraftFromActive(String fqn);

    /**
     * 更新草稿的属性内容。
     * <p>
     * 仅允许修改 content 字段（新增/修改/删除属性字段）。
     * fqn、parent_fqn、entity_schema_fqn 创建后不可变更。
     * 更新时重新执行全字段 JSON Schema 结构校验。
     *
     * @param fqn 草稿 FQN
     * @param request 内容更新请求
     * @return 更新后的草稿 DTO
     * @throws DraftNotFoundException 如果草稿不存在
     * @throws MetadataValidationException 如果更新后的 content 校验失败
     */
    MetadataEntityDraftDto updateDraftContent(String fqn, UpdateDraftContentRequest request);

    /**
     * 查询草稿详情。
     *
     * @param fqn 草稿 FQN
     * @return 草稿 DTO
     * @throws DraftNotFoundException 如果草稿不存在
     */
    MetadataEntityDraftDto getDraft(String fqn);

    /**
     * 物理删除草稿。
     * <p>
     * 删除后草稿表无残留，主表与历史表无任何变更。
     * 该 FQN 可重新创建新草稿。
     *
     * @param fqn 草稿 FQN
     * @throws DraftNotFoundException 如果草稿不存在
     */
    void deleteDraft(String fqn);
}
```

### 输入/输出 DTO

```java
/**
 * 创建草稿请求。
 */
public record CreateDraftRequest(
        @NotBlank String fqn,
        @NotBlank String name,
        String description,
        String parentFqn,
        @NotBlank String entitySchemaFqn,
        @NotNull Map<String, Object> content,
        List<Float> embedding
) {}

/**
 * 更新草稿内容请求。
 */
public record UpdateDraftContentRequest(
        @NotNull Map<String, Object> content,
        List<Float> embedding
) {}

/**
 * 草稿响应 DTO。
 */
public record MetadataEntityDraftDto(
        Long id,
        String fqn,
        String name,
        String description,
        String parentFqn,
        String entitySchemaFqn,
        Map<String, Object> content,
        List<Float> embedding,
        Integer baseVersion,
        String createdBy,
        LocalDateTime createdTime,
        String updatedBy,
        LocalDateTime updatedTime
) {}
```

---

## 2. MetadataActivationService

**职责**: 版本生效与生命周期管控——草稿生效（原子事务）、生效版本下线（引用校验+子实体校验）、历史版本重新生效。

### @OpenHostService

```java
/**
 * 元数据版本生效与生命周期管控服务。
 * <p>
 * 生效操作为原子事务：同一事务内完成（1）主表写入/覆盖唯一生效版本、
 * （2）全量快照归档至历史表（版本号递增）、（3）删除草稿表对应记录。
 * 任意一步失败全量回滚不产生脏数据。
 */
public interface MetadataActivationService {

    /**
     * 对校验通过的草稿执行生效操作。
     * <p>
     * 生效前执行全量预校验：
     * <ul>
     *   <li>JSON Schema 结构合规校验</li>
     *   <li>组合层级合法性（父实体状态为 ACTIVE）</li>
     *   <li>元模型版本有效性校验</li>
     * </ul>
     * 原子事务步骤：
     * <ul>
     *   <li>主表写入/覆盖（版本号递增）</li>
     *   <li>历史表插入全量快照</li>
     *   <li>草稿表删除对应记录</li>
     * </ul>
     * 事务成功提交后发布变更事件（操作类型 = "生效"）。
     *
     * @param fqn 草稿 FQN
     * @return 生效后的元数据 DTO
     * @throws DraftNotFoundException 如果草稿不存在
     * @throws MetadataValidationException 如果预校验失败
     * @throws ActivationFailedException 如果生效事务失败
     */
    MetadataEntityDto activate(String fqn);

    /**
     * 对生效版本执行下线操作。
     * <p>
     * 下线前校验：
     * <ul>
     *   <li>外部活跃引用检查（调用语义关系网络模块）</li>
     *   <li>生效子实体状态检查（FQN 前缀匹配）</li>
     * </ul>
     * 存在任一拦截条件则拒绝下线并返回详细清单。
     * 原子事务：
     * <ul>
     *   <li>主表删除记录</li>
     *   <li>历史表原样保留归档（不做任何修改）</li>
     * </ul>
     * 事务成功提交后发布变更事件（操作类型 = "下线"）。
     *
     * @param fqn 生效元数据 FQN
     * @throws EntityNotFoundException 如果生效版本不存在
     * @throws DeactivationBlockedException 如果存在活跃引用或生效子实体
     */
    void deactivate(String fqn);

    /**
     * 基于历史归档版本重新生效。
     * <p>
     * 无修改时：直接从历史表恢复最新归档版本到主表，不新增历史记录。
     * 需修改时：调用 {@link MetadataDraftService#createDraftFromActive} 创建草稿后走标准生效流程。
     *
     * @param fqn 元数据 FQN
     * @return 重新生效后的元数据 DTO
     * @throws EntityNotFoundException 如果历史表中无归档记录
     */
    MetadataEntityDto reactivate(String fqn);

    /**
     * 校验下线前置条件。
     * <p>
     * 检查外部活跃引用与生效子实体状态，返回拦截清单。
     * 不执行实际下线操作。
     *
     * @param fqn 生效元数据 FQN
     * @return 下线前置条件校验结果
     */
    DeactivationCheckResult checkDeactivationPreconditions(String fqn);
}
```

### 相关 DTO

```java
public record MetadataEntityDto(
        Long id,
        String fqn,
        String name,
        String description,
        String parentFqn,
        String entitySchemaFqn,
        Map<String, Object> content,
        List<Float> embedding,
        Integer currentVersion,
        String createdBy,
        LocalDateTime createdTime,
        String updatedBy,
        LocalDateTime updatedTime
) {}

public record DeactivationCheckResult(
        boolean canDeactivate,
        List<String> activeReferences,    // 活跃引用元数据 FQN 列表
        List<String> activeChildren       // 生效子实体 FQN 列表
) {}
```

---

## 3. MetadataQueryService

**职责**: 多维度查询检索——FQN 精准查询、FQN 前缀范围查询、元模型类型查询、属性条件组合查询、管理员全状态聚合查询。默认仅返回主表生效版本。

### @OpenHostService

```java
/**
 * 元数据查询检索服务。
 * <p>
 * 支持四种查询模式：FQN 精准查询、FQN 前缀范围查询（OR 并集逻辑）、
 * 元模型类型查询、属性条件组合查询。默认仅返回主表生效版本，支持分页与排序。
 * 管理员专属接口可跨主表/草稿表/历史表聚合查询全状态数据。
 */
public interface MetadataQueryService {

    /**
     * FQN 精准查询生效元数据完整内容。
     *
     * @param fqn 元数据 FQN
     * @return 元数据完整 DTO（含 content 全量字段）
     * @throws EntityNotFoundException 如果 FQN 不存在或已下线
     */
    MetadataEntityDto getByFqn(String fqn);

    /**
     * FQN 前缀范围查询生效元数据列表。
     * <p>
     * 支持传入多个前缀，按 OR 并集逻辑返回匹配任意前缀的所有生效元数据，
     * 结果按 FQN 排序，支持分页。
     *
     * @param fqnPrefixes FQN 前缀集合（如 ["SalesOrder_"]）
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    PageResult<MetadataEntityDto> listByFqnPrefixes(List<String> fqnPrefixes, PageRequest pageRequest);

    /**
     * 按元模型类型查询生效元数据列表。
     *
     * @param entitySchemaFqn EntitySchema 全限定名（含版本号）
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    PageResult<MetadataEntityDto> listByEntitySchema(String entitySchemaFqn, PageRequest pageRequest);

    /**
     * 按属性条件组合查询生效元数据。
     * <p>
     * 支持精准匹配（字段值完全相等）与模糊匹配（字段值前缀匹配）。
     *
     * @param query 查询请求（含属性条件列表）
     * @return 符合条件的元数据列表
     */
    List<MetadataEntityDto> queryByAttributes(MetadataQueryRequest query);

    /**
     * 管理员专属全状态聚合查询。
     * <p>
     * 跨主表/草稿表/历史表聚合结果，每条数据标注状态（草稿/生效/历史归档）与来源表。
     * 仅管理员可调用。
     *
     * @param request 管理端查询请求
     * @return 聚合查询结果
     */
    PageResult<MetadataEntityDto> adminQuery(AdminQueryRequest request);
}
```

### 查询 DTO

```java
public record MetadataQueryRequest(
        List<AttributeCondition> conditions,
        MatchMode matchMode,
        PageRequest pageRequest
) {}

public record AttributeCondition(
        @NotBlank String field,
        @NotBlank String value
) {}

public enum MatchMode {
    EXACT,    // 精准匹配
    PREFIX    // 模糊前缀匹配
}

public record AdminQueryRequest(
        List<MetadataStatus> statuses,  // 过滤的状态列表，空=全部
        String fqnPrefix,
        String entitySchemaFqn,
        PageRequest pageRequest
) {}
```

---

## 4. MetadataHistoryService

**职责**: 历史版本追溯与差异对比——全版本列表查询、单版本详情查询、任意两版本字段级差异对比。

### @OpenHostService

```java
/**
 * 元数据历史版本追溯服务。
 * <p>
 * 历史表仅支持 INSERT 操作（数据库层面禁止 UPDATE 和 DELETE）。
 * 支持按 FQN 查询全历史版本列表、按 FQN+版本号查询单版本完整属性快照、
 * 以及任意两个历史版本间的字段级差异对比。
 */
public interface MetadataHistoryService {

    /**
     * 查询指定 FQN 的全历史版本列表。
     * <p>
     * 按版本号倒序排列，每条包含版本号、生效时间、操作人，
     * 默认不返回完整属性内容。
     *
     * @param fqn 元数据 FQN
     * @return 版本列表（倒序）
     */
    List<EntityVersionDto> listVersions(String fqn);

    /**
     * 查询指定 FQN + 版本号的完整历史版本详情。
     * <p>
     * 返回该版本的完整属性快照、关联元模型 FQN、生效时间、操作人等全量信息。
     *
     * @param fqn 元数据 FQN
     * @param version 版本号
     * @return 历史版本完整 DTO
     * @throws VersionNotFoundException 如果指定版本不存在
     */
    EntityVersionDto getVersionDetail(String fqn, int version);

    /**
     * 对比任意两个历史版本的字段级差异。
     * <p>
     * 按"新增字段（ADDED）、修改字段（MODIFIED）、删除字段（DELETED）"三类分类展示变更内容。
     *
     * @param request 差异对比请求（含 fqn、versionA、versionB）
     * @return 差异对比结果
     * @throws VersionNotFoundException 如果任一版本不存在
     */
    VersionDiffDto compareVersions(DiffRequest request);
}
```

### 历史 DTO

```java
public record EntityVersionDto(
        Long id,
        String fqn,
        String name,
        String description,
        String parentFqn,
        Integer version,
        String entitySchemaFqn,
        Map<String, Object> content,
        List<Float> embedding,
        String createdBy,
        LocalDateTime createdTime
) {}

public record DiffRequest(
        @NotBlank String fqn,
        @NotNull Integer versionA,
        @NotNull Integer versionB
) {}

public record VersionDiffDto(
        String fqn,
        Integer versionA,
        Integer versionB,
        List<FieldDiff> addedFields,
        List<FieldDiff> modifiedFields,
        List<FieldDiff> deletedFields
) {}

public record FieldDiff(
        String fieldPath,
        Object oldValue,      // 对 ADDED 为 null
        Object newValue       // 对 DELETED 为 null
) {}
```

---

## 5. MetadataImportExportService

**职责**: 批量导入导出——YAML/JSON 格式导入、按 FQN 前缀/元模型类型/FQN 列表导出。导入仅写入草稿表，需手动生效。

### @OpenHostService

```java
/**
 * 元数据批量导入导出服务。
 * <p>
 * 导入支持 YAML/JSON 格式，以 FQN 为唯一标识，幂等支持"跳过/报错"两种策略。
 * 导入全程逐条执行结构校验，校验失败不影响其他合法数据。
 * 导入成功的数据仅写入草稿表，需手动执行生效后方可对外可见。
 * 导出支持按 FQN 前缀范围、元模型类型、指定 FQN 列表三种粒度。
 */
public interface MetadataImportExportService {

    /**
     * 批量导入元数据。
     * <p>
     * 逐条解析 → FQN 文法校验 → JSON Schema 结构校验 → 写入草稿表。
     * 单条失败不影响其他合法数据，返回完整导入结果清单。
     *
     * @param request 导入请求（含文件内容、格式、幂等策略）
     * @return 导入结果（成功/失败清单及失败原因）
     */
    ImportResultDto importMetadata(ImportRequest request);

    /**
     * 按 FQN 前缀范围导出生效元数据。
     *
     * @param fqnPrefixes FQN 前缀集合
     * @param format 导出格式（JSON/YAML）
     * @return 导出结果（含序列化内容）
     */
    ExportResultDto exportByFqnPrefixes(List<String> fqnPrefixes, ExportFormat format);

    /**
     * 按元模型类型导出生效元数据。
     *
     * @param entitySchemaFqn EntitySchema FQN
     * @param format 导出格式
     * @return 导出结果
     */
    ExportResultDto exportByEntitySchema(String entitySchemaFqn, ExportFormat format);

    /**
     * 按指定 FQN 列表导出生效元数据。
     *
     * @param fqns FQN 列表
     * @param format 导出格式
     * @return 导出结果
     */
    ExportResultDto exportByFqns(List<String> fqns, ExportFormat format);
}
```

### 导入导出 DTO

```java
public record ImportRequest(
        @NotBlank String content,           // 导入文件内容
        @NotNull ImportFormat format,       // JSON 或 YAML
        @NotNull ImportStrategy strategy    // SKIP 或 ERROR
) {}

public record ImportResultDto(
        int totalCount,
        int successCount,
        int skipCount,
        int failureCount,
        List<ImportItemResult> items
) {}

public record ImportItemResult(
        String fqn,
        boolean success,
        String errorMessage           // 失败时为违规原因
) {}

public record ExportResultDto(
        int totalCount,
        String content,               // 序列化的导出内容（JSON/YAML 字符串）
        ExportFormat format
) {}

public enum ImportFormat { JSON, YAML }
public enum ExportFormat { JSON, YAML }
```

---

## Error Codes

本 BC 使用错误码范围 **31000-31099**。

| 错误码 | 常量名 | HTTP 状态 | 描述 |
|--------|--------|----------|------|
| 31001 | `FQN_CONFLICT` | 409 | FQN 全局唯一性冲突（主表或草稿表已存在） |
| 31002 | `FQN_INVALID_SEGMENT` | 400 | FQN segment 不符合 [A-Za-z][A-Za-z0-9_-]* 文法 |
| 31003 | `SCHEMA_VALIDATION_FAILED` | 422 | JSON Schema 结构校验失败 |
| 31004 | `ENTITY_NOT_FOUND` | 404 | 生效元数据不存在或已下线 |
| 31005 | `DRAFT_NOT_FOUND` | 404 | 草稿不存在 |
| 31006 | `VERSION_NOT_FOUND` | 404 | 历史版本不存在 |
| 31007 | `ACTIVATION_FAILED` | 500 | 生效原子事务执行失败 |
| 31008 | `DEACTIVATION_BLOCKED` | 409 | 下线被拦截（存在活跃引用/生效子实体） |
| 31009 | `PARENT_NOT_ACTIVE` | 422 | 父实体未生效或不存在 |
| 31010 | `SCHEMA_VERSION_NOT_PUBLISHED` | 422 | 绑定的元模型版本未发布 |
| 31011 | `IMPORT_PARSE_FAILED` | 400 | 导入文件解析失败 |
| 31012 | `FQN_SEGMENT_RESERVED_CHAR` | 400 | FQN segment 包含保留分隔符 `.` |

### 调用方（下游 BC）使用示例

```java
// 引入 metaforge-metadata-api 模块依赖后直接注入
@Autowired
private MetadataQueryService metadataQueryService;

MetadataEntityDto entity = metadataQueryService.getByFqn("SalesOrder_001");
PageResult<MetadataEntityDto> page = metadataQueryService.listByFqnPrefixes(
    List.of("SalesOrder_"), PageRequest.of(1, 20));
```

### Transaction Boundary

每个 Application Service 方法为一个事务边界。`activate()` 和 `deactivate()` 为原子事务——任意步骤失败全量回滚。
