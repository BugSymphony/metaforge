# Application Service 接口契约：metaforge-graph

**Protocol**: Application Service（进程内 Java Interface 调用）
**Module**: `metaforge-graph-api`
**Version**: 1.0.0

> 下游 BC（`semantic-query-engine`、`metadata-management`、`agent-consumption`）通过 Maven 依赖 `metaforge-graph-api` 模块，注入以下接口的 Spring Bean 实例进行进程内调用。严禁依赖 `metaforge-graph-core` 模块。

---

## Maven 依赖

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-graph-api</artifactId>
    <version>${revision}</version>
</dependency>
```

---

## 1. RelationDraftService

**职责**: 关系草稿的全生命周期管理——创建、编辑、查询、物理删除。草稿完全隔离于正式版本，对外不可见。

### @OpenHostService

```java
/**
 * 关系实例草稿管理服务。
 * <p>
 * 草稿是关系实例的编辑态副本，存储于 relation_instance_draft 表，
 * 与主表 relation_instance 物理隔离，对外完全不可见。
 * 每次保存时实时执行 JSON Schema 结构校验，仅校验通过的数据才允许写入。
 * 同一 FQN 最多仅允许存在一条草稿。
 */
public interface RelationDraftService {

    /**
     * 基于已发布的 RelationSchema 手动创建全新的关系草稿。
     * <p>
     * 前置校验：
     * <ul>
     *   <li>RelationSchema FQN 对应版本已发布（非 DRAFT），通过上游 api 校验</li>
     *   <li>源实体与目标实体均为生效状态（通过 metadata-management api 校验）</li>
     *   <li>源实体类型与目标实体类型符合 RelationSchema 端点约束</li>
     *   <li>同一 RelationSchema 下同一源实体与目标实体之间不存在生效关系或草稿</li>
     *   <li>content 符合 RelationSchema JSON Schema 全字段结构校验</li>
     * </ul>
     * <p>
     * 关系 FQN 由统一 FQN 生成器按 {@code {源实体FQN}#{关系类型FQN}#{目标实体FQN}} 格式自动生成，
     * 禁止调用方手动指定。
     *
     * @param request 草稿创建请求（含源实体 FQN、关系类型 FQN、目标实体 FQN、属性内容等）
     * @return 创建成功的草稿 DTO
     * @throws FqnConflictException 如果关系 FQN 已存在于主表或草稿表
     * @throws SchemaNotPublishedException 如果 RelationSchema 版本未发布
     * @throws EndpointNotActiveException 如果源端或目标端实体未生效
     * @throws SchemaValidationException 如果 JSON Schema 结构校验失败
     */
    RelationInstanceDraftDto createDraft(CreateDraftRequest request);

    /**
     * 基于已生效版本创建修改草稿。
     * <p>
     * 草稿内容从主表全量复制（副本），baseVersion 记录原版本号。
     * 创建后允许自由编辑 content，但 FQN、关联元模型版本、源/目标端点不可变更。
     *
     * @param fqn 已生效关系实例的 FQN
     * @return 基于生效版本创建的草稿 DTO
     * @throws RelationNotFoundException 如果 FQN 对应生效版本不存在
     * @throws FqnConflictException 如果该 FQN 已存在草稿
     */
    RelationInstanceDraftDto createDraftFromActive(String fqn);

    /**
     * 更新草稿的属性内容。
     * <p>
     * 仅允许修改 content 字段（新增/修改/删除属性字段）。
     * FQN、关联元模型版本、源/目标端点创建后不可变更。
     * 更新时重新执行全字段 JSON Schema 结构校验。
     *
     * @param fqn 草稿 FQN
     * @param request 内容更新请求
     * @return 更新后的草稿 DTO
     * @throws DraftNotFoundException 如果草稿不存在
     * @throws SchemaValidationException 如果更新后的 content 校验失败
     */
    RelationInstanceDraftDto updateDraftContent(String fqn, UpdateDraftContentRequest request);

    /**
     * 查询草稿详情。
     *
     * @param fqn 草稿 FQN
     * @return 草稿 DTO
     * @throws DraftNotFoundException 如果草稿不存在
     */
    RelationInstanceDraftDto getDraft(String fqn);

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
        @NotBlank String sourceEntityFqn,    // 源实体 FQN
        @NotBlank String relationTypeFqn,    // 关系类型 FQN（如 "order:1.0.0.composition"）
        @NotBlank String targetEntityFqn,    // 目标实体 FQN
        @NotBlank String name,               // 关系名称
        String description,                  // 关系描述
        @NotNull Map<String, Object> content,// 属性内容（JSONB）
        List<Float> embedding                // 向量嵌入（MVP 仅占位）
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
public record RelationInstanceDraftDto(
        Long id,
        String fqn,
        String name,
        String description,
        String sourceEntityFqn,
        String targetEntityFqn,
        String relationType,
        String relationSchemaFqn,
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

## 2. RelationActivationService

**职责**: 关系版本生效与生命周期管控——草稿生效（原子四步事务）、生效关系下线（依赖校验+原子操作）、历史版本重新生效。

### @OpenHostService

```java
/**
 * 关系实例版本生效与生命周期管控服务。
 * <p>
 * 生效操作为原子事务：同一事务内完成（1）主表写入/覆盖唯一生效版本、
 * （2）全量快照归档至历史表（版本号递增）、（3）删除草稿表对应记录、
 * （4）更新双向引用索引（源实体出边 + 目标实体入边）。
 * 任一步失败全量回滚，不存在中间脏状态。
 * 事务成功提交后发布 RelationChangeEvent（changeType = ACTIVATED）。
 */
public interface RelationActivationService {

    /**
     * 对校验通过的草稿执行生效操作。
     * <p>
     * 生效前执行全量预校验：
     * <ul>
     *   <li>JSON Schema 结构合规校验</li>
     *   <li>端点实体有效性校验（源端与目标端均为 ACTIVE 状态）</li>
     *   <li>基数合规校验（基于 RelationSchema 的源端/目标端基数约束）</li>
     *   <li>元模型版本有效性校验（RelationSchema 版本为 PUBLISHED）</li>
     * </ul>
     * <p>
     * 原子事务四步操作详见 FR-014。
     * 事务成功提交后以事务后回调方式发布 RelationChangeEvent。
     *
     * @param fqn 草稿 FQN
     * @return 生效后的关系实例 DTO
     * @throws DraftNotFoundException 如果草稿不存在
     * @throws SchemaValidationException 如果结构校验失败
     * @throws EndpointNotActiveException 如果端点实体已下线
     * @throws CardinalityViolationException 如果基数约束违反
     * @throws ActivationFailedException 如果生效事务失败
     */
    RelationInstanceDto activate(String fqn);

    /**
     * 对生效关系执行下线操作。
     * <p>
     * 下线前执行依赖校验：
     * <ul>
     *   <li>仅当目标关系被其他关系显式声明为 DEPENDENCY_INFLUENCE 类型端点时视为强依赖</li>
     *   <li>存在强依赖则拦截下线并返回依赖清单</li>
     *   <li>COMPOSITION、MAPPING_CORRESPONDENCE 等关系类型不阻断下线</li>
     * </ul>
     * <p>
     * 原子事务：
     * <ul>
     *   <li>主表删除记录</li>
     *   <li>双向索引记录移除</li>
     *   <li>历史表原样保留归档（不做任何修改）</li>
     * </ul>
     * 事务成功提交后发布 RelationChangeEvent（changeType = DEPRECATED）。
     *
     * @param fqn 生效关系 FQN
     * @throws RelationNotFoundException 如果生效版本不存在
     * @throws DependencyBlockedException 如果存在下游强依赖
     */
    void deprecate(String fqn);

    /**
     * 基于历史归档版本重新生效。
     * <p>
     * 无修改时（直接重新生效）：
     * 直接从历史表恢复最新归档版本到主表，版本号不变，历史表不新增记录。
     * 需修改时：调用 {@link RelationDraftService#createDraftFromActive} 创建草稿后走标准生效流程。
     *
     * @param fqn 关系 FQN
     * @return 重新生效后的关系实例 DTO
     * @throws VersionNotFoundException 如果历史表中无归档记录
     */
    RelationInstanceDto reactivate(String fqn);

    /**
     * 校验下线前置条件（不执行实际下线）。
     * <p>
     * 检查是否存在下游强依赖，返回校验结果与依赖清单。
     *
     * @param fqn 生效关系 FQN
     * @return 下线前置条件校验结果
     */
    DeactivationCheckResult checkDeactivationPreconditions(String fqn);
}
```

### 相关 DTO

```java
public record RelationInstanceDto(
        Long id,
        String fqn,
        String name,
        String description,
        String sourceEntityFqn,
        String targetEntityFqn,
        String relationType,
        String relationSchemaFqn,
        Map<String, Object> content,
        List<Float> embedding,
        Integer currentVersion,
        String createdBy,
        LocalDateTime createdTime,
        String updatedBy,
        LocalDateTime updatedTime
) {}

public record DeactivationCheckResult(
        boolean canDeprecate,
        List<String> blockingRelations   // 下游强依赖关系 FQN 清单
) {}
```

---

## 3. RelationQueryService

**职责**: 多维度查询检索——FQN 精准查询、实体出入边查询、前缀范围查询、多维过滤查询、管理员全状态聚合查询。默认仅返回主表生效版本。

### @OpenHostService

```java
/**
 * 关系实例查询检索服务。
 * <p>
 * 支持五种查询模式：FQN 精准查询、指定实体出入边查询、
 * FQN 前缀范围查询、多维过滤查询、管理员全状态聚合查询。
 * 默认仅返回主表生效版本，支持分页与排序。
 * 管理者专属接口可跨主表/草稿表/历史表聚合查询全状态数据。
 */
public interface RelationQueryService {

    /**
     * 通过 FQN 精准查询单条生效关系实例的完整属性。
     * <p>
     * 返回完整属性内容（含 name、description、embedding、content）、
     * 关联元模型信息、端点实体 FQN、当前版本号和审计信息。
     *
     * @param fqn 关系 FQN
     * @return 关系实例完整 DTO
     * @throws RelationNotFoundException 如果 FQN 不存在或已下线
     */
    RelationInstanceDto getByFqn(String fqn);

    /**
     * 查询指定实体的出边关系列表。
     * <p>
     * 可选按关系类型、目标实体类型过滤。基于双向索引驱动查询。
     *
     * @param entityFqn 实体 FQN
     * @param relationType 关系类型过滤（可选，null 表示全部）
     * @param targetEntityType 目标实体类型过滤（可选，null 表示全部）
     * @return 出边关系列表
     */
    List<RelationInstanceDto> getOutboundRelations(
            String entityFqn, String relationType, String targetEntityType);

    /**
     * 查询指定实体的入边关系列表。
     * <p>
     * 可选按关系类型、源实体类型过滤。基于双向索引驱动查询。
     *
     * @param entityFqn 实体 FQN
     * @param relationType 关系类型过滤（可选，null 表示全部）
     * @param sourceEntityType 源实体类型过滤（可选，null 表示全部）
     * @return 入边关系列表
     */
    List<RelationInstanceDto> getInboundRelations(
            String entityFqn, String relationType, String sourceEntityType);

    /**
     * 按 FQN 前缀及 RelationSchema FQN 组合条件查询生效关系列表。
     * <p>
     * 支持分页与排序。此接口作为简便快捷入口保留。
     *
     * @param fqnPrefix FQN 前缀
     * @param relationSchemaFqn RelationSchema FQN 过滤（可选）
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    PageResult<RelationInstanceDto> listByConditions(
            String fqnPrefix, String relationSchemaFqn, PageRequest pageRequest);

    /**
     * 统一多维过滤查询接口（维度间 AND，维度内 OR）。
     * <p>
     * 支持以下维度任意组合过滤：
     * <ul>
     *   <li>关系类型（relationTypes，多值 IN）</li>
     *   <li>源实体类型 / 目标实体类型（sourceEntityTypes / targetEntityTypes，多值 IN）</li>
     *   <li>源实体 FQN / 目标实体 FQN（sourceEntityFqns / targetEntityFqns，多值 IN + 前缀匹配）</li>
     *   <li>RelationSchema FQN（relationSchemaFqns，多值 IN）</li>
     *   <li>name / description 关键词（子串包含匹配 ILIKE '%keyword%'）</li>
     *   <li>时间范围（createdAtStart/End、updatedAtStart/End）</li>
     * </ul>
     * 默认仅查询主表生效态关系，支持分页与排序。
     *
     * @param request 多维过滤查询请求
     * @return 分页结果
     */
    PageResult<RelationInstanceDto> multiFilter(RelationQueryRequest request);

    /**
     * 管理员专属全状态聚合查询。
     * <p>
     * 跨主表/草稿表/历史表聚合结果，每条数据标注状态（草稿/生效/历史归档）与来源表。
     * 仅管理员角色可调用，普通消费方调用返回无权限错误。
     *
     * @param request 管理端查询请求
     * @return 聚合分页查询结果
     */
    PageResult<RelationInstanceDto> adminQuery(AdminQueryRequest request);
}
```

### 查询 DTO

```java
/**
 * 多维过滤查询请求。
 * 所有字段均为可选——维度间 AND，维度内 OR。
 */
public record RelationQueryRequest(
        List<String> relationTypes,         // 关系类型集合（多值 OR）
        List<String> sourceEntityTypes,     // 源实体类型集合（多值 OR）
        List<String> targetEntityTypes,     // 目标实体类型集合（多值 OR）
        List<String> sourceEntityFqns,      // 源实体 FQN 集合（多值 OR + 前缀匹配）
        List<String> targetEntityFqns,      // 目标实体 FQN 集合（多值 OR + 前缀匹配）
        List<String> relationSchemaFqns,    // RelationSchema FQN 集合（多值 OR）
        String nameKeyword,                 // name 关键词（子串包含）
        String descriptionKeyword,          // description 关键词（子串包含）
        LocalDateTime createdAtStart,       // 创建时间起点
        LocalDateTime createdAtEnd,         // 创建时间终点
        LocalDateTime updatedAtStart,       // 更新时间起点
        LocalDateTime updatedAtEnd,         // 更新时间终点
        PageRequest pageRequest             // 分页与排序
) {}

/**
 * 管理端查询请求。
 */
public record AdminQueryRequest(
        List<String> statuses,              // 过滤的状态列表（DRAFT/ACTIVE/DEPRECATED），空=全部
        String fqnPrefix,
        String relationSchemaFqn,
        PageRequest pageRequest
) {}
```

---

## 4. RelationHistoryService

**职责**: 历史版本追溯与差异对比——全版本列表查询、单版本详情查询、任意两版本字段级差异对比。

### @OpenHostService

```java
/**
 * 关系实例历史版本追溯服务。
 * <p>
 * 历史表仅支持 INSERT 操作（数据库层面禁止 UPDATE 和 DELETE）。
 * 支持按 FQN 查询全历史版本列表、按 FQN+版本号查询单版本完整属性快照、
 * 以及任意两个历史版本间的字段级差异对比。
 */
public interface RelationHistoryService {

    /**
     * 查询指定 FQN 的全历史正式版本列表。
     * <p>
     * 按版本号倒序排列，每条包含版本号、生效时间、操作人，
     * 默认不返回完整属性内容 content（仅元信息）。
     *
     * @param fqn 关系 FQN
     * @return 版本列表（倒序）
     */
    List<RelationVersionDto> listVersions(String fqn);

    /**
     * 查询指定 FQN + 版本号的完整历史版本详情。
     * <p>
     * 返回该版本的完整属性快照（含 content、embedding）、关联元模型 FQN、
     * 端点实体 FQN、生效时间、操作人等全量信息。
     *
     * @param fqn 关系 FQN
     * @param version 版本号
     * @return 历史版本完整 DTO
     * @throws VersionNotFoundException 如果指定版本不存在
     */
    RelationVersionDto getVersionDetail(String fqn, int version);

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
public record RelationVersionDto(
        Long id,
        String fqn,
        String name,
        String description,
        String sourceEntityFqn,
        String targetEntityFqn,
        String relationType,
        String relationSchemaFqn,
        Map<String, Object> content,
        List<Float> embedding,
        Integer version,
        String activatedBy,
        LocalDateTime activatedTime
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

## 5. RelationImportExportService

**职责**: 批量导入导出——YAML/JSON 格式导入（进入草稿态）、按多种粒度导出生效关系。

### @OpenHostService

```java
/**
 * 关系实例批量导入导出服务。
 * <p>
 * 导入支持 YAML/JSON 格式，导入默认进入草稿态，禁止直接写入主表。
 * 导入时 FQN 由统一生成器根据每条记录的源实体 FQN、关系类型 FQN、目标实体 FQN
 * 重新生成，忽略导入文件中的 FQN 字段。
 * 逐条执行结构校验，校验失败不影响其他合法数据。
 * 支持"跳过"和"报错"两种幂等策略。
 * 导出支持按 FQN 前缀、关系类型、指定 FQN 列表三种粒度。
 */
public interface RelationImportExportService {

    /**
     * 批量导入关系实例（YAML/JSON 格式）。
     * <p>
     * 逐条解析 → FQN 生成 → JSON Schema 结构校验 → 写入草稿表。
     * 单条失败不影响其他合法数据，返回完整导入结果清单。
     * 导入成功的关系仅写入草稿表，需手动执行生效后方可对外可见。
     *
     * @param request 导入请求（含文件内容、格式、幂等策略）
     * @return 导入结果（成功/失败/跳过清单及原因）
     */
    ImportResultDto importRelations(ImportRequest request);

    /**
     * 按 FQN 前缀导出生效关系。
     *
     * @param fqnPrefixes FQN 前缀集合
     * @param format 导出格式（JSON/YAML）
     * @return 导出结果（含序列化内容）
     */
    ExportResultDto exportByFqnPrefixes(List<String> fqnPrefixes, ExportFormat format);

    /**
     * 按关系类型导出生效关系。
     *
     * @param relationTypes 关系类型集合
     * @param format 导出格式
     * @return 导出结果
     */
    ExportResultDto exportByRelationTypes(List<String> relationTypes, ExportFormat format);

    /**
     * 按指定 FQN 列表导出生效关系。
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
        String errorMessage
) {}

public record ExportResultDto(
        int totalCount,
        String content,
        ExportFormat format
) {}

public enum ImportFormat { JSON, YAML }
public enum ExportFormat { JSON, YAML }
```

---

## 6. RelationTopologyService

**职责**: 关系拓扑管理与校验——双向索引状态查询、批量拓扑完整性校验、自动构建关系实例。

### @OpenHostService

```java
/**
 * 关系拓扑管理与校验服务。
 * <p>
 * 提供关系拓扑的查询、校验与自动构建能力。
 * 向外暴露引用关系查询接口，供 metadata-management BC 查询指定实体是否存在关联的依赖关系。
 */
public interface RelationTopologyService {

    /**
     * 查询指定实体是否存在关联的生效依赖关系。
     * <p>
     * 供 metadata-management BC 在执行实体下线前校验影响范围。
     * 仅检查 DEPENDENCY_INFLUENCE 类型的关联关系。
     *
     * @param entityFqn 实体 FQN
     * @return 关联的依赖关系 FQN 列表（为空表示无下游依赖）
     */
    List<String> getDependentRelations(String entityFqn);

    /**
     * 批量拓扑完整性校验。
     * <p>
     * 支持按 FQN 前缀、关系类型发起批量校验，检测以下四类问题：
     * <ul>
     *   <li>悬空边：关系指向的端点实体已不存在或已下线</li>
     *   <li>无效端点：端点实体类型与 RelationSchema 定义不匹配</li>
     *   <li>基数异常：关系的源端或目标端违反基数约束</li>
     *   <li>元模型不匹配：关系绑定的 Schema 版本不匹配</li>
     * </ul>
     *
     * @param request 拓扑校验请求
     * @return 完整性校验报告
     */
    TopologyValidationReport validateTopology(TopologyValidationRequest request);

    /**
     * 查询指定实体的出边与入边关系总数。
     *
     * @param entityFqn 实体 FQN
     * @return 包含出边数与入边数的统计结果
     */
    RelationCount getRelationCount(String entityFqn);
}
```

### 拓扑 DTO

```java
public record TopologyValidationRequest(
        String fqnPrefix,           // FQN 前缀范围（可选）
        String relationType         // 关系类型过滤（可选）
) {}

public record TopologyValidationReport(
        int totalChecked,
        int issuesFound,
        List<TopologyIssue> issues
) {}

public record TopologyIssue(
        String relationFqn,
        IssueType issueType,
        String description
) {}

public enum IssueType {
    DANGLING_EDGE,       // 悬空边
    INVALID_ENDPOINT,    // 无效端点
    CARDINALITY_ERROR,   // 基数异常
    SCHEMA_MISMATCH      // 元模型不匹配
}

public record RelationCount(
        String entityFqn,
        long outboundCount,
        long inboundCount
) {}
```

---

## 事务边界

每个 Application Service 方法为一个事务边界。`activate()` 与 `deprecate()` 为原子事务——任一步骤失败全量回滚。

## 错误码

本 BC 使用错误码范围 **32000-32099**（定义于 `metaforge-graph-api` 的 `GraphErrorCode` 常量类）。

| 错误码 | 常量名 | HTTP 状态 | 描述 |
|--------|--------|----------|------|
| 32001 | `FQN_CONFLICT` | 409 | 关系 FQN 已存在（主表或草稿表） |
| 32002 | `SCHEMA_NOT_PUBLISHED` | 422 | 绑定的 RelationSchema 版本未发布 |
| 32003 | `SCHEMA_VALIDATION_FAILED` | 422 | JSON Schema 结构校验失败 |
| 32004 | `RELATION_NOT_FOUND` | 404 | 指定 FQN 的生效关系不存在 |
| 32005 | `DRAFT_NOT_FOUND` | 404 | 草稿不存在 |
| 32006 | `VERSION_NOT_FOUND` | 404 | 历史版本不存在 |
| 32007 | `ACTIVATION_FAILED` | 500 | 生效原子事务执行失败 |
| 32008 | `DEPENDENCY_BLOCKED` | 409 | 下线被拦截（存在下游强依赖） |
| 32009 | `ENDPOINT_NOT_ACTIVE` | 422 | 源端或目标端实体未生效 |
| 32010 | `CARDINALITY_VIOLATION` | 422 | 基数约束违反 |
| 32011 | `DUPLICATE_DRAFT` | 409 | 同一 FQN 已存在草稿 |
| 32012 | `FQN_PARSE_ERROR` | 400 | FQN 解析失败 |
| 32013 | `CROSS_DOMAIN_REJECTED` | 403 | 跨域关系未经授权 |
| 32014 | `IMPORT_PARSE_FAILED` | 400 | 导入文件解析失败 |
| 32015 | `ENDPOINT_TYPE_MISMATCH` | 422 | 端点实体类型与 Schema 定义不匹配 |

## 调用示例

```java
// 引入 metaforge-graph-api 模块依赖后直接注入
@Autowired
private RelationDraftService draftService;

@Autowired
private RelationQueryService queryService;

@Autowired
private RelationActivationService activationService;

// 创建草稿
RelationInstanceDraftDto draft = draftService.createDraft(new CreateDraftRequest(
    "Order_001", "order:1.0.0.composition", "OrderItem_003",
    "订单项组成关系", "订单与订单项的组成关联", contentMap, null
));

// 查询生效关系
RelationInstanceDto active = queryService.getByFqn(draft.fqn());

// 执行生效
RelationInstanceDto activated = activationService.activate(draft.fqn());

// 多维过滤查询
PageResult<RelationInstanceDto> page = queryService.multiFilter(
    new RelationQueryRequest(
        List.of("COMPOSITION"),     // 关系类型过滤
        null, null,                 // 实体类型不限制
        null, null,                 // 实体 FQN 不限制
        null, "订单",               // name 关键词
        null, null, null, null,     // 时间范围不限制
        PageRequest.of(1, 20)
    )
);
```
