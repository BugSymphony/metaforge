# Data Model: 元认知指导层 (CognitionGuidance BC)

**Status**: Draft
**Created**: 2026-08-01
**BC Type**: Pure stateless compute/orchestration layer

---

## 重要声明

本 BC **不持有任何数据库表、JPA 实体、Flyway 脚本**。它是一个纯无状态计算/编排层：

- 所有运行时数据通过上游 BC 的 Java Interface（进程内调用）即席获取
- 仅持久化自身运营所需的 YAML 配置文件（cognition-templates.yml / cognition-perspectives.yml）
- 查询完成后不保留任何数据痕迹
- 不保存任务上下文、会话状态、历史快照或继承链

本文档描述的"数据模型"为**内存领域对象与 DTO**——定义了本 BC 内部的领域对象结构、请求/响应 DTO、配置模型、领域服务接口和上游客户端端口。

---

## 1. Core Domain Model

### 1.1 Aggregate Root

#### GuidanceResult (统一查询输出)

cognitionGuidance 端点的聚合根输出。根据 `perspectives[]` 参数动态包含对应的认知视角章节和 `context_meta`。

```java
package com.metaforge.cognition.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 统一查询输出聚合根。
 * <p>
 * 承载一次 cognitionGuidance 查询的完整认知结果，
 * 根据 perspectives[] 参数动态组合各视角章节。
 * 所有视角章节以 FQN 为核心标识，内联完整语义。
 */
public class GuidanceResult {

    private ContextMeta contextMeta;

    // Perspective chapters — only populated based on requested perspectives
    private EntityProfile entityProfile;
    private DomainLocation domainLocation;
    private CompositionTree compositionTree;
    private RelationshipGraph relationshipGraph;
    private ConstraintSet constraintSet;
    private CapabilityCatalog capabilityCatalog;
    private FlowBlueprint flowBlueprint;
    private DecisionMatrix decisionMatrix;
    private ImpactTrace impactTrace;
    private PrerequisiteChain prerequisiteChain;

    // Bundle-scope perspectives
    private BundleDirectory bundleDirectory;
    private DomainNavigation domainNavigation;
    private InstanceCatalog instanceCatalog;
    private SchemaInventory schemaInventory;

    // constructors
    private GuidanceResult(ContextMeta contextMeta) {
        this.contextMeta = contextMeta;
    }

    public static GuidanceResult create(ContextMeta contextMeta) {
        return new GuidanceResult(contextMeta);
    }

    // builder-style setters for each perspective chapter

    // getters
}
```

### 1.2 Domain Entities (Perspective Outputs)

以下实体均为**视角输出载体**——每次查询即时构造，查询结束后不持久化。

#### EntityProfile (实体画像)

```java
/**
 * entity_profile 视角输出。
 * 包含实体的完整 M1 实例内容、所属 EntitySchema 的结构说明、历史版本信息。
 */
public class EntityProfile {

    private String fqn;
    private String name;
    private String description;
    private String entitySchemaFqn;
    private Map<String, Object> content;              // 完整 M1 实例属性字段
    private List<NativeAttributeDetail> schemaAttributes;  // 所属 Schema 的字段定义
    private Integer currentVersion;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static class NativeAttributeDetail {
        private String name;
        private String type;
        private boolean required;
        private String description;
        private Map<String, Object> constraints;
    }
}
```

#### DomainLocation (领域定位)

```java
/**
 * domain_location 视角输出。
 * 从实体 FQN 沿 COMPOSITION 入边反向追溯的完整归属路径。
 */
public class DomainLocation {

    private String entityFqn;
    private List<LocationNode> path;               // 每层含 fqn/name/description/entitySchemaFqn
    private boolean complete;                       // true=完整路径, false=中途截断
    private String note;                            // 未接入 L1-L5 分类树时的标注

    public static class LocationNode {
        private String fqn;
        private String name;
        private String description;
        private String entitySchemaFqn;
        private int depth;                          // 所在层级
    }
}
```

#### CompositionTree (组成结构)

```java
/**
 * composition_tree 视角输出。
 * 包含 COMPOSITION 关系展开的树形结构。
 */
public class CompositionTree {

    private String rootFqn;
    private TraversalDirection direction;           // FORWARD / BACKWARD / BOTH
    private TreeNode root;
    private boolean truncated;
    private String truncatedReason;

    public static class TreeNode {
        private String fqn;
        private String name;
        private String entitySchemaFqn;
        private int depth;
        private List<TreeNode> children;            // FORWARD 子树
        private List<TreeNode> parentChain;          // BACKWARD 父链
    }
}
```

#### RelationshipGraph (关系图谱)

```java
/**
 * relationship_graph 视角输出。
 * 按照 AssociationType 分组的邻域实体列表和关系列表。
 */
public class RelationshipGraph {

    private String centerFqn;
    private int degrees;                            // 展开度数 1-3
    private Map<AssociationType, RelationGroup> groups;
    private boolean empty;                          // true=无关系实例
    private String emptyNote;

    public static class RelationGroup {
        private AssociationType associationType;
        private List<RelationDetail> relations;     // 关系详情列表

        public static class RelationDetail {
            private String relationFqn;
            private String sourceEntityFqn;
            private String targetEntityFqn;
            private String semanticDescription;      // 关系语义说明
        }
    }
}
```

#### ConstraintSet (约束规则)

```java
/**
 * constraint_set 视角输出。
 * 包含约束条目、硬边界、软边界三类约束。
 */
public class ConstraintSet {

    private List<ConstraintItem> constraints;       // 约束条目列表
    private List<HardBoundary> hardBoundaries;       // JSON Schema 硬边界
    private List<SoftBoundary> softBoundaries;       // ASSOCIATION_REFERENCE 软边界
    private boolean empty;
    private String emptyNote;

    public static class ConstraintItem {
        private String constraintFqn;
        private String constraintName;
        private ConstraintLevel constraintLevel;     // MANDATORY / RECOMMENDED / REFERENCE
        private String constraintDescription;
        private String sourceEntityFqn;              // 来源实体 FQN
        private String sourceType;                   // DEPENDENCY_INFLUENCE / ASSOCIATION_REFERENCE / SCHEMA
    }

    public static class HardBoundary {
        private String fieldName;
        private boolean required;
        private List<String> enumValues;             // 枚举值约束
        private Object minimum;                     // 取值范围下界
        private Object maximum;                     // 取值范围上界
        private String pattern;                     // 正则模式
    }

    public static class SoftBoundary {
        private String referenceEntityFqn;
        private String referenceEntityName;
        private String referenceDescription;
    }
}
```

#### CapabilityCatalog (能力目录)

```java
/**
 * capability_catalog 视角输出。
 * 包含能力列表、接口说明、调用方法、协议子类型详情。
 */
public class CapabilityCatalog {

    private List<CapabilityItem> capabilities;

    public static class CapabilityItem {
        private String capabilityFqn;
        private String name;
        private String description;
        private String interfaceSpec;                // 接口规范摘要
        private String callMethod;                   // 调用方式
        private List<ProtocolDetail> protocols;      // 子协议详情

        public static class ProtocolDetail {
            private String protocolFqn;
            private String protocolName;
            private String protocolDescription;
            private Map<String, Object> protocolContent;
        }
    }
}
```

#### FlowBlueprint (流程蓝图)

```java
/**
 * flow_blueprint 视角输出。
 * 基于 PROCESS_SEQUENCE 关系拓扑的步骤序列。
 */
public class FlowBlueprint {

    private String bundleFqn;
    private List<FlowStep> steps;                    // 步骤序列
    private String entryStep;                        // 入口步骤 FQN
    private List<String> exitSteps;                  // 出口步骤 FQN 列表
    private List<BranchPoint> branchPoints;          // 分支点列表
    private boolean empty;
    private String emptyNote;

    public static class FlowStep {
        private String stepFqn;
        private String name;
        private String description;
        private List<String> preconditions;          // 前置步骤 FQN
        private List<String> outputs;                // 输出产物 FQN
        private int sequenceOrder;
    }

    public static class BranchPoint {
        private String decisionStepFqn;
        private List<String> alternativePaths;       // 可选分支步骤 FQN 列表
    }
}
```

#### DecisionMatrix (决策图谱)

```java
/**
 * decision_matrix 视角输出。
 * 包含决策点及其可选路径、触发条件、下游影响。
 */
public class DecisionMatrix {

    private List<DecisionPoint> decisionPoints;

    public static class DecisionPoint {
        private String decisionEntityFqn;
        private String decisionName;
        private List<DecisionOption> options;        // 可选路径
        private String recommendation;               // 推荐倾向

        public static class DecisionOption {
            private String targetEntityFqn;
            private String triggerCondition;         // 触发条件描述
            private List<String> downstreamImpact;    // 下游影响 FQN 列表
        }
    }
}
```

#### ImpactTrace (影响追溯)

```java
/**
 * impact_trace 视角输出。
 * 包含正向影响扩散、反向依赖溯源、影响路径详情。
 */
public class ImpactTrace {

    private String sourceFqn;
    private Map<Integer, List<ImpactEntity>> forwardImpact;     // 正向分层影响实体
    private Map<Integer, List<ImpactEntity>> backwardDependency;// 反向分层依赖实体
    private List<ImpactPath> impactPaths;                       // 详细传导路径

    public static class ImpactEntity {
        private String fqn;
        private String name;
        private String entitySchemaFqn;
        private int depth;
    }

    public static class ImpactPath {
        private String targetFqn;
        private int pathLength;
        private List<String> hopEntities;            // 途经实体 FQN
        private List<String> hopRelations;           // 途经关系 FQN
        private String semanticDescription;
    }
}
```

#### PrerequisiteChain (前置依赖)

```java
/**
 * prerequisite_chain 视角输出。
 * 按层级展开的依赖树。
 */
public class PrerequisiteChain {

    private String entityFqn;
    private List<PrerequisiteNode> dependencyTree;   // 层级化依赖树

    public static class PrerequisiteNode {
        private String entityFqn;
        private String entityName;
        private AssociationType dependencyType;       // DEPENDENCY_INFLUENCE
        private boolean blocking;                     // 是否阻塞当前实体
        private String entityStatus;                  // 实体当前状态
        private int level;                            // 依赖层级
        private List<PrerequisiteNode> children;      // 子依赖
    }
}
```

#### BundleDirectory (Bundle 目录)

```java
/**
 * bundle_directory 视角输出。
 * 已发布 Bundle 实例列表及主题域树。
 */
public class BundleDirectory {

    private List<BundleEntry> bundles;

    public static class BundleEntry {
        private String fqn;
        private String name;
        private String description;
        private String owner;
        private boolean isSystem;
        private List<SubjectDomainGroup> domainTree;  // L1 → L2 → Task 主题域树
    }

    public static class SubjectDomainGroup {
        private String fqn;
        private String name;
        private List<SubjectDomain> domains;
    }

    public static class SubjectDomain {
        private String fqn;
        private String name;
        private List<Task> tasks;
    }

    public static class Task {
        private String fqn;
        private String name;
    }
}
```

#### DomainNavigation (主题域导航)

```java
/**
 * domain_navigation 视角输出。
 * 渐进式懒加载导航路径结构。
 */
public class DomainNavigation {

    private String anchorFqn;
    private NavLevel currentLevel;                    // L1 / L2 / Task
    private List<NavNode> children;                   // 当前层子节点概要
    private boolean hasMore;                          // 是否有更多子节点
    private String nextCursor;                        // 分页游标

    public static class NavNode {
        private String fqn;
        private String name;
        private String description;
        private int childCount;                      // 子节点数量
        private boolean hasMoreChildren;
    }
}
```

#### InstanceCatalog (实例目录)

```java
/**
 * instance_catalog 视角输出。
 * 指定 Bundle 的 M1 实例及关系清单。
 */
public class InstanceCatalog {

    private String bundleFqn;
    private List<String> entityTypes;                 // 过滤的实体类型
    private List<CatalogEntity> entities;             // 实例列表
    private int totalCount;

    public static class CatalogEntity {
        private String fqn;
        private String name;
        private String entitySchemaFqn;
        private int relationCount;                   // 关联关系数量
        private List<CatalogRelation> relations;      // 关联关系（ENTITY 模式过滤）
    }

    public static class CatalogRelation {
        private String relationFqn;
        private AssociationType associationType;
        private String targetEntityFqn;
    }
}
```

#### SchemaInventory (Schema 库存)

```java
/**
 * schema_inventory 视角输出。
 * 指定 Bundle 下的 EntitySchema 清单及实例数量统计。
 */
public class SchemaInventory {

    private String bundleFqn;
    private List<SchemaEntry> schemas;

    public static class SchemaEntry {
        private String schemaFqn;
        private String name;
        private String description;
        private int instanceCount;                   // M1 实例数量
    }
}
```

### 1.3 Value Objects

#### TemplateId

```java
/**
 * 模板标识符，对应 cognition-templates.yml 中的 templateId。
 * 取值: "task-brief" | "step-guide" | "bundle-catalog" | "navigate"
 */
public record TemplateId(String value) {

    public static final TemplateId TASK_BRIEF = new TemplateId("task-brief");
    public static final TemplateId STEP_GUIDE = new TemplateId("step-guide");
    public static final TemplateId BUNDLE_CATALOG = new TemplateId("bundle-catalog");
    public static final TemplateId NAVIGATE = new TemplateId("navigate");

    public TemplateId {
        validateValue(value);
    }

    private static void validateValue(String v) {
        if (v == null || v.isBlank() || !List.of("task-brief", "step-guide", "bundle-catalog", "navigate").contains(v)) {
            throw new IllegalArgumentException("Invalid TemplateId: " + v);
        }
    }

    @Override
    public String toString() { return value; }
}
```

#### PerspectiveCode

```java
/**
 * 认知视角编码，对应 14 个内置认知视角的唯一标识。
 */
public record PerspectiveCode(String value) {

    public static final PerspectiveCode ENTITY_PROFILE        = new PerspectiveCode("entity_profile");
    public static final PerspectiveCode DOMAIN_LOCATION       = new PerspectiveCode("domain_location");
    public static final PerspectiveCode COMPOSITION_TREE      = new PerspectiveCode("composition_tree");
    public static final PerspectiveCode RELATIONSHIP_GRAPH    = new PerspectiveCode("relationship_graph");
    public static final PerspectiveCode CONSTRAINT_SET        = new PerspectiveCode("constraint_set");
    public static final PerspectiveCode CAPABILITY_CATALOG    = new PerspectiveCode("capability_catalog");
    public static final PerspectiveCode FLOW_BLUEPRINT        = new PerspectiveCode("flow_blueprint");
    public static final PerspectiveCode DECISION_MATRIX       = new PerspectiveCode("decision_matrix");
    public static final PerspectiveCode IMPACT_TRACE          = new PerspectiveCode("impact_trace");
    public static final PerspectiveCode PREREQUISITE_CHAIN     = new PerspectiveCode("prerequisite_chain");
    public static final PerspectiveCode DOMAIN_NAVIGATION     = new PerspectiveCode("domain_navigation");
    public static final PerspectiveCode INSTANCE_CATALOG      = new PerspectiveCode("instance_catalog");
    public static final PerspectiveCode BUNDLE_DIRECTORY      = new PerspectiveCode("bundle_directory");
    public static final PerspectiveCode SCHEMA_INVENTORY      = new PerspectiveCode("schema_inventory");

    private static final Set<String> VALID_CODES = Set.of(
        "entity_profile", "domain_location", "composition_tree", "relationship_graph",
        "constraint_set", "capability_catalog", "flow_blueprint", "decision_matrix",
        "impact_trace", "prerequisite_chain", "domain_navigation", "instance_catalog",
        "bundle_directory", "schema_inventory"
    );

    public PerspectiveCode {
        if (!VALID_CODES.contains(value)) {
            throw new IllegalArgumentException("Unknown perspective code: " + value);
        }
    }

    @Override
    public String toString() { return value; }
}
```

#### PerspectiveScope

```java
/**
 * 视角作用域——控制视角在 Bundle 级/实体级上下文中的激活规则。
 * ENTITY: 仅实体级上下文激活，Bundle 级跳过
 * BUNDLE: 仅 Bundle 级上下文激活，实体级跳过
 * BOTH:   两级上下文均激活
 */
public enum PerspectiveScope {
    ENTITY,
    BUNDLE,
    BOTH
}
```

#### CognitionDepth

```java
/**
 * 三级认知深度，控制输出粒度与视角数量上限。
 * L1 导航级: 最多 3 个视角，Token 预算 ≤ 2000
 * L2 执行级: 最多 7 个视角（默认）
 * L3 深化级: 全部 14 个视角全量展开
 */
public enum CognitionDepth {

    L1(3),
    L2(7),
    L3(14);

    private final int maxPerspectives;

    CognitionDepth(int maxPerspectives) {
        this.maxPerspectives = maxPerspectives;
    }

    public int maxPerspectives() { return maxPerspectives; }

    /**
     * 解析认知深度，未知值回退默认 L2。
     */
    public static CognitionDepth fromString(String value) {
        if (value == null) return L2;
        try { return valueOf(value.toUpperCase()); } catch (IllegalArgumentException e) { return L2; }
    }
}
```

#### AgentArchetype

```java
/**
 * 代理原型类型，用于视角优先级排序与输出侧重调整。
 * execution:      执行型 — 约束（constraint_set）和蓝图（flow_blueprint）前置
 * exploration:    探索型 — 组成结构（composition_tree）和关系图谱（relationship_graph）前置
 * audit:          审计型 — 约束（constraint_set）和影响追溯（impact_trace）前置
 * orchestration:  编排型 — 流程蓝图（flow_blueprint）和决策图谱（decision_matrix）前置
 */
public enum AgentArchetype {

    EXECUTION,
    EXPLORATION,
    AUDIT,
    ORCHESTRATION;

    /**
     * 解析代理原型，未知值回退默认 execution。
     */
    public static AgentArchetype fromString(String value) {
        if (value == null) return EXECUTION;
        try { return valueOf(value.toUpperCase()); } catch (IllegalArgumentException e) { return EXECUTION; }
    }
}
```

#### ContextMode

```java
/**
 * 上下文模式——根据 entity_fqn 是否传入自动推导。
 * BUNDLE_LEVEL: entity_fqn 为空，遍历全部 BUNDLE scope + BOTH scope 视角的全量数据
 * ENTITY_LEVEL: entity_fqn 非空，跳过 BUNDLE scope 视角，BOTH scope 视角按图边过滤，通过 adjacent_context 提供局部导航
 */
public enum ContextMode {
    BUNDLE_LEVEL,
    ENTITY_LEVEL
}
```

#### ScopeMode

```java
/**
 * 层级化作用域模式——区分层级化子任务与无状态工具调用。
 * INHERITED: 父代理委派子任务模式，引擎执行三层范围收窄（蓝图→实体→Schema）
 * PURE:      无状态工具调用模式，仅返回当前实体声明的内容
 */
public enum ScopeMode {
    INHERITED,
    PURE
}
```

#### DataVersionAnchor (数据版本锚)

```java
/**
 * 每次认知查询的版本参照——记录各 Bundle 的已发布版本号与查询时间戳，
 * 供消费端追溯数据版本。
 */
public record DataVersionAnchor(
    String bundleFqn,            // Bundle 全限定名
    String publishedVersionFqn,  // 查询时刻该 Bundle 的已发布版本 FQN
    int latestVersionNumber,     // 最新版本号
    Instant queriedAt            // 查询时间戳
) {}
```

#### OutputFormat

```java
/**
 * 输出格式。
 * JSON:   结构化 JSON，可直接程序化消费
 * PROMPT: Markdown 格式语义说明，可直接注入大模型上下文窗口
 * 两种格式语义内容完全一致。
 */
public enum OutputFormat {
    JSON,
    PROMPT
}
```

#### ContextMeta (上下文元信息)

```java
/**
 * 所有认知查询输出的上下文元信息章节。
 * 位于统一根 JSON 结构的根层级。
 */
public class ContextMeta {

    private List<String> bundleFqns;                        // 查询的 Bundle FQN 列表
    private String entityFqn;                               // 实体级上下文 FQN（可为 null）
    private ContextMode contextMode;                        // 推导的上下文模式
    private ScopeMode scopeMode;                            // 作用域模式
    private CognitionDepth cognitionDepth;                  // 应用后的认知深度
    private AgentArchetype agentArchetype;                  // 应用的代理原型
    private List<PerspectiveCode> appliedPerspectives;      // 实际执行的视角列表
    private List<PerspectiveCode> skippedPerspectives;      // 被跳过的视角列表
    private List<String> skipReasons;                       // 跳过原因（如 "Bundle-scope perspective skipped in entity-level context"）
    private List<DataVersionAnchor> dataVersionAnchors;     // 各 Bundle 版本锚
    private long totalTokenCount;                           // 输出 Token 总量
    private boolean tokenTrimmed;                           // 是否因 max_tokens 触发裁剪
    private boolean truncated;                              // 是否存在视角截断
    private List<TruncationNote> truncations;               // 截断标注列表
    private Instant queriedAt;                              // 查询执行时间戳

    public record TruncationNote(
        PerspectiveCode perspective,
        String reason                                       // TIMEOUT / DEPTH_EXCEEDED / COUNT_EXCEEDED
    ) {}
}
```

#### AdjacentContext (局部导航)

```java
/**
 * 实体级上下文输出中附带的局部导航信息。
 * 查询 PROCESS_SEQUENCE 出边获取后几步 FQN，入边获取前一步 FQN。
 */
public record AdjacentContext(
    List<String> previousSteps,        // PROCESS_SEQUENCE 入边来源步骤 FQN
    List<String> nextSteps,            // PROCESS_SEQUENCE 出边目标步骤 FQN
    List<String> upstreamEntities,     // DEPENDENCY_INFLUENCE 入边上游实体 FQN
    List<String> downstreamEntities    // DEPENDENCY_INFLUENCE 出边下游实体 FQN
) {}
```

#### ConstraintLevel

```java
/**
 * 约束级别——由实体属性中 constraint_level 字段自动识别。
 * 无此字段时默认 REFERENCE。
 */
public enum ConstraintLevel {
    MANDATORY,    // 强制性约束
    RECOMMENDED,  // 推荐性约束
    REFERENCE     // 参考性约束（默认）

    public static ConstraintLevel fromString(String value) {
        if (value == null) return REFERENCE;
        try { return valueOf(value.toUpperCase()); } catch (IllegalArgumentException e) { return REFERENCE; }
    }
}
```

---

## 2. DTOs — Request / Response

### 2.1 Common Request Parameters

```java
package com.metaforge.cognition.dto;

import java.util.List;
import java.util.Map;

/**
 * 认知查询请求公共参数。
 * 所有查询端点（除 navigate）共享这些字段的子集。
 */
public abstract class BaseCognitionRequest {

    @NotNull
    protected List<String> bundleFqns;              // Bundle FQN 列表（必填）

    protected List<String> perspectives;             // 视角编码列表（可选）

    protected String entityFqn;                      // 实体 FQN（可选，传入→实体级上下文）

    protected List<String> entityTypes;              // 实体类型过滤（可选）
    protected String subjectDomainFqn;               // 主题域 FQN（可选）

    protected String scopeMode;                      // INHERITED / PURE（可选）
    protected String cognitionDepth;                 // L1 / L2 / L3（可选，默认 L2）
    protected String agentArchetype;                 // execution / exploration / audit / orchestration（可选）
    protected Integer maxTokens;                     // Token 上限（可选，默认 8000）

    protected Map<String, Object> contextParameters; // 上下文附加参数（可选）

    protected String cursor;                          // 分页游标（可选）
    protected Integer pageSize;                       // 分页大小（可选）

    protected String expand;                          // all / lazy（可选）
    protected String format;                          // json / prompt（可选，默认 json）
}
```

### 2.2 Endpoint-Specific Request DTOs

```java
/**
 * cognitionGuidance 端点请求。
 * 统一元认知查询引擎入参，继承全部公共字段。
 */
public class CognitionGuidanceRequest extends BaseCognitionRequest {
    // 全部字段继承自 BaseCognitionRequest
}

/**
 * taskBrief 端点请求（一站式简报便利封装）。
 * 仅接受 bundle_fqns（必填）、cognition_depth、agent_archetype、max_tokens、context_parameters。
 */
public class TaskBriefRequest {

    @NotNull
    private List<String> bundleFqns;

    private String cognitionDepth;
    private String agentArchetype;
    private Integer maxTokens;
    private Map<String, Object> contextParameters;
    private String format;
}

/**
 * stepGuide 端点请求（实体即时指导）。
 * 接受 entity_fqn（必填）、cognition_depth、agent_archetype、max_tokens。
 */
public class StepGuideRequest {

    @NotNull
    private String entityFqn;                        // 实体 FQN（必填）

    private String cognitionDepth;
    private String agentArchetype;
    private Integer maxTokens;
    private String format;
}

/**
 * navigate 端点请求（渐进懒加载导航）。
 */
public class NavigateRequest {

    private String anchorFqn;                        // 导航锚点 FQN（可选，空=顶层导航）
    private String level;                            // L1 / L2 / Task
    private String cursor;                           // 分页游标
    private Integer pageSize;
    private String expand;                           // all / lazy
    private String format;
}

/**
 * subTaskBrief 端点请求。
 * scope_mode=INHERITED 时引擎执行三层收窄。
 */
public class SubTaskBriefRequest {

    @NotNull
    private String entryEntityFqn;                   // 入口实体 FQN（必填）

    @NotNull
    private String scopeMode;                        // INHERITED / PURE（必填）

    private String cognitionDepth;
    private String agentArchetype;
    private Integer maxTokens;
    private List<String> perspectives;               // 可选，覆盖默认视角集
    private String format;
}
```

### 2.3 Response DTOs

```java
/**
 * cognitionGuidance 端点统一响应。
 * 根 JSON 结构: context_meta + 各认知视角章节
 */
public class CognitionGuidanceResponse {

    private ContextMeta contextMeta;

    // Perspective chapters — presence controlled by requested perspectives
    private EntityProfile entityProfile;
    private DomainLocation domainLocation;
    private CompositionTree compositionTree;
    private RelationshipGraph relationshipGraph;
    private ConstraintSet constraintSet;
    private CapabilityCatalog capabilityCatalog;
    private FlowBlueprint flowBlueprint;
    private DecisionMatrix decisionMatrix;
    private ImpactTrace impactTrace;
    private PrerequisiteChain prerequisiteChain;
    private BundleDirectory bundleDirectory;
    private DomainNavigation domainNavigation;
    private InstanceCatalog instanceCatalog;
    private SchemaInventory schemaInventory;
}

/**
 * taskBrief 端点响应。
 * 一次性返回全部 10 个认知视角章节。
 */
public class TaskBriefResponse {

    private ContextMeta contextMeta;

    private EntityProfile entityProfile;
    private DomainLocation domainLocation;
    private CompositionTree compositionTree;
    private RelationshipGraph relationshipGraph;
    private ConstraintSet constraintSet;
    private CapabilityCatalog capabilityCatalog;
    private FlowBlueprint flowBlueprint;
    private DecisionMatrix decisionMatrix;
    private ImpactTrace impactTrace;
    private PrerequisiteChain prerequisiteChain;
}

/**
 * stepGuide 端点响应。
 * 实体级过滤后的约束/能力/决策/关系 + adjacent_context。
 */
public class StepGuideResponse {

    private ContextMeta contextMeta;

    private EntityProfile entityProfile;
    private ConstraintSet constraintSet;
    private CapabilityCatalog capabilityCatalog;
    private DecisionMatrix decisionMatrix;
    private ImpactTrace impactTrace;
    private RelationshipGraph relationshipGraph;
    private AdjacentContext adjacentContext;
}

/**
 * navigate 端点响应。
 */
public class NavigateResponse {

    private ContextMeta contextMeta;
    private DomainNavigation domainNavigation;
}

/**
 * bundleCatalog 端点响应。
 */
public class BundleCatalogResponse {

    private ContextMeta contextMeta;
    private BundleDirectory bundleDirectory;
}
```

### 2.4 Error Response

```java
/**
 * 统一错误响应结构。
 */
public record ErrorResponse(
    String errorCode,                                // 错误码（如 INVALID_BUNDLE_FQN）
    String message,                                  // 人类可读描述
    List<String> candidates,                         // FQN 归属校验失败时的候选列表
    String traceId                                   // 请求追踪 ID
) {}
```

**错误码表**:

| 错误码 | 描述 | 触发条件 |
|--------|------|---------|
| `INVALID_BUNDLE_FQN` | Bundle FQN 格式非法 | bundle_fqns 不符合 FQN 格式 |
| `EMPTY_BUNDLE_FQNS` | Bundle FQN 列表为空 | bundle_fqns 为空 |
| `INVALID_ENTITY_FQN` | 实体 FQN 不存在或前缀不属于已发布 Bundle | FQN 归属校验失败 |
| `INVALID_PERSPECTIVE_CODE` | 视角编码不支持 | perspectives[] 含未知编码 |
| `INVALID_COGNITION_DEPTH` | 认知深度值非法 | cognition_depth 不匹配 L1/L2/L3 |
| `INVALID_AGENT_ARCHETYPE` | 代理原型值非法 | agent_archetype 不匹配枚举值 |
| `INVALID_SCOPE_MODE` | 作用域模式非法 | scope_mode 不匹配 INHERITED/PURE |
| `TOKEN_BUDGET_EXCEEDED` | Token 预算极低触发降维 | max_tokens < 500 时降为 L1 |
| `PERSPECTIVE_TIMEOUT` | 部分视角查询超时 | 单视角超时 > 200ms |
| `CIRCULAR_REFERENCE_DETECTED` | 流程探索检测到循环引用 | PROCESS_SEQUENCE 形成闭环 |
| `UPSTREAM_SERVICE_UNAVAILABLE` | 上游 BC 不可用 | 上游 Service 调用异常 |

---

## 3. Configuration Model

### 3.1 Template Configuration (cognition-templates.yml)

本 BC 持久化存储的唯一两类数据之一，服务启动时一次性加载至内存为 POJO 缓存。配置变更需重启服务生效。

```yaml
# cognition-templates.yml
#
# 每种 template 定义了一个预配置的视角组合、深度裁剪和代理适配策略。
# 查询端点（taskBrief、stepGuide、navigate、bundleCatalog）通过 templateId
# 引用这些预定义策略，cognitionGuidance 端点也可直接使用模板。
#
# Depth trimming:
#   - L0: 仅 entity_profile、bundle_directory
#   - L1: 最多 3 个视角，按 agent_archetype 优先级裁剪
#   - L2: 最多 7 个视角，保留规则/约束 + 目标相关
#   - L3: 全部 14 个视角全量展开

templates:
  - templateId: "task-brief"
    description: "一站式任务元认知简报组合"
    perspectives:
      - "entity_profile"
      - "domain_location"
      - "composition_tree"
      - "relationship_graph"
      - "constraint_set"
      - "capability_catalog"
      - "flow_blueprint"
      - "decision_matrix"
      - "impact_trace"
      - "prerequisite_chain"
    depthTrim: "L2"
    archetypeAdapt: "execution"
    outputFormat: "json"
    maxTokens: 8000

  - templateId: "step-guide"
    description: "实体级即时指导组合——仅执行实体级/双向视角"
    perspectives:
      - "entity_profile"
      - "constraint_set"
      - "capability_catalog"
      - "decision_matrix"
      - "impact_trace"
      - "relationship_graph"
    depthTrim: "L2"
    archetypeAdapt: "execution"
    contextMode: "ENTITY_LEVEL"

  - templateId: "bundle-catalog"
    description: "Bundle 目录概览与主题域导航"
    perspectives:
      - "bundle_directory"
      - "domain_navigation"
    depthTrim: "L1"
    archetypeAdapt: "exploration"

  - templateId: "navigate"
    description: "渐进式域导航入口"
    perspectives:
      - "domain_navigation"
    depthTrim: "L1"
    archetypeAdapt: "exploration"
    expand: "lazy"
```

**Java 配置模型**:

```java
package com.metaforge.cognition.config;

import java.util.List;

/**
 * cognition-templates.yml 的内存表示。
 */
public class TemplateConfig {

    private List<TemplateDefinition> templates;

    public record TemplateDefinition(
        String templateId,
        String description,
        List<String> perspectives,
        String depthTrim,                            // 深度裁剪级别
        String archetypeAdapt,                       // 代理原型适配
        String outputFormat,
        Integer maxTokens,
        String contextMode,                          // BUNDLE_LEVEL / ENTITY_LEVEL（可选）
        String expand                                // lazy / all（可选）
    ) {}

    public TemplateDefinition findByTemplateId(String templateId) {
        return templates.stream()
            .filter(t -> t.templateId().equals(templateId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown template: " + templateId));
    }
}
```

### 3.2 Perspective Configuration (cognition-perspectives.yml)

```yaml
# cognition-perspectives.yml
#
# 定义 14 个内置认知视角的元信息，包括：
#   - perspectiveId: 视角唯一标识
#   - scope: ENTITY / BUNDLE / BOTH（控制视角在两级上下文的激活规则）
#   - description: 中文描述
#
# 视角激活规则:
#   - BUNDLE_LEVEL 上下文: 激活 scope=BUNDLE + scope=BOTH 的视角
#   - ENTITY_LEVEL 上下文: 激活 scope=ENTITY + scope=BOTH 的视角（BOTH 视角按 entity_fqn 做图边过滤）

perspectives:
  - perspectiveId: "entity_profile"
    scope: "BOTH"
    description: "实体画像"

  - perspectiveId: "domain_location"
    scope: "ENTITY"
    description: "领域定位"

  - perspectiveId: "composition_tree"
    scope: "ENTITY"
    description: "组成结构"

  - perspectiveId: "relationship_graph"
    scope: "ENTITY"
    description: "关系图谱"

  - perspectiveId: "constraint_set"
    scope: "BOTH"
    description: "约束规则"

  - perspectiveId: "capability_catalog"
    scope: "BOTH"
    description: "能力目录"

  - perspectiveId: "flow_blueprint"
    scope: "BUNDLE"
    description: "流程蓝图"

  - perspectiveId: "decision_matrix"
    scope: "BOTH"
    description: "决策图谱"

  - perspectiveId: "impact_trace"
    scope: "ENTITY"
    description: "影响追溯"

  - perspectiveId: "prerequisite_chain"
    scope: "BOTH"
    description: "前置依赖"

  - perspectiveId: "domain_navigation"
    scope: "BUNDLE"
    description: "主题域导航"

  - perspectiveId: "instance_catalog"
    scope: "BOTH"
    description: "实例目录"

  - perspectiveId: "bundle_directory"
    scope: "BUNDLE"
    description: "Bundle目录"

  - perspectiveId: "schema_inventory"
    scope: "BUNDLE"
    description: "Schema库存"
```

**Java 配置模型**:

```java
package com.metaforge.cognition.config;

import java.util.List;

/**
 * cognition-perspectives.yml 的内存表示。
 */
public class PerspectiveConfig {

    private List<PerspectiveDefinition> perspectives;

    public record PerspectiveDefinition(
        String perspectiveId,
        PerspectiveScope scope,
        String description
    ) {}

    /**
     * 按 ContextMode 和指定 perspectiveIds 筛选应激活的视角列表。
     *
     * @param contextMode      当前上下文模式（BUNDLE_LEVEL / ENTITY_LEVEL）
     * @param requestedPerspectiveIds 请求的视角 ID 列表（空=全量）
     * @param depth            认知深度（控制视角数量上限）
     * @param archetype        代理原型（控制视角优先级排序）
     * @return 激活的视角定义列表，按代理原型优先级排序
     */
    public List<PerspectiveDefinition> resolveActivePerspectives(
        ContextMode contextMode,
        List<PerspectiveCode> requestedPerspectiveIds,
        CognitionDepth depth,
        AgentArchetype archetype
    ) {
        // Filter by scope compatibility with context mode
        // Filter by requested IDs (if non-empty)
        // Sort by archetype priority
        // Limit by depth.maxPerspectives
        // Returns sorted, limited list
    }
}
```

### 3.3 Configuration Loading

```java
package com.metaforge.cognition.config;

/**
 * 配置加载器——负责在服务启动时解析 YAML 配置文件至内存 POJO。
 * 配置一次性加载，运行时不可变。
 */
public interface ConfigLoader {

    /**
     * 加载 templates 配置。
     * <p>
     * 服务启动时由 Spring @PostConstruct 触发，解析 cognition-templates.yml
     * 为 TemplateConfig 内存对象。解析失败则服务启动失败。
     */
    TemplateConfig loadTemplates();

    /**
     * 加载 perspectives 配置。
     * <p>
     * 服务启动时由 Spring @PostConstruct 触发，解析 cognition-perspectives.yml
     * 为 PerspectiveConfig 内存对象。解析失败则服务启动失败。
     */
    PerspectiveConfig loadPerspectives();

    interface SPI {
        void onConfigChanged();  // 供监控与诊断使用，当前版本不实现热加载
    }
}
```

---

## 4. Domain Service Interfaces

本 BC 的核心业务逻辑通过以下领域服务实现。所有服务为无状态 Spring Bean。

### 4.1 TemplateResolutionService

```java
package com.metaforge.cognition.service;

import java.util.List;

/**
 * 模板解析服务。
 * 负责根据 templateId 解析对应模板定义、合并请求覆盖参数、确定最终的视角执行计划。
 */
public interface TemplateResolutionService {

    /**
     * 解析模板——根据 templateId 查找配置，合并请求级覆盖参数，
     * 返回最终的执行计划（视角列表、深度限制、原型适配）。
     *
     * @param templateId 模板 ID
     * @param overrides  请求级覆盖参数（perspectives/depth/archetype/tokens）
     * @return 解析后的执行计划
     */
    ExecutionPlan resolve(TemplateId templateId, RequestOverrides overrides);

    /**
     * 不依赖模板，直接从请求参数构建执行计划（用于 cognitionGuidance 端点）。
     */
    ExecutionPlan resolveFromRequest(String cognitionDepth, String agentArchetype,
                                      List<String> perspectives, Integer maxTokens);

    /**
     * 执行计划——包含最终确定的视角列表、深度限制、Token 预算等。
     */
    public record ExecutionPlan(
        List<PerspectiveCode> perspectives,
        CognitionDepth depth,
        AgentArchetype archetype,
        int maxTokens,
        OutputFormat outputFormat,
        ContextMode contextMode,
        ScopeMode scopeMode
    ) {}

    public record RequestOverrides(
        List<String> perspectives,
        String cognitionDepth,
        String agentArchetype,
        Integer maxTokens,
        String outputFormat,
        ContextMode contextMode,
        ScopeMode scopeMode
    ) {}
}
```

### 4.2 PerspectiveOrchestrationService

```java
package com.metaforge.cognition.service;

import java.util.concurrent.CompletableFuture;

/**
 * 视角编排服务。
 * 按执行计划并行调用各视角执行器，聚合视角输出为统一 GuidanceResult。
 * 每视角超时阈值 200ms，超时视角标注 truncated=true、reason=TIMEOUT。
 */
public interface PerspectiveOrchestrationService {

    /**
     * 执行完整认知查询流程。
     * <p>
     * 步骤：
     * 1. 根据 contextMode 过滤视角 scope 兼容性
     * 2. 并行启动各视角执行器（CompletableFuture.supplyAsync）
     * 3. 每视角设 200ms 超时（orTimeout）
     * 4. 收集已完成视角结果，超时视角标注 truncated
     * 5. 组装 ContextMeta（version anchors + 截断汇总）
     * 6. 调用 Token 裁剪（如需要）
     * 7. 构建 GuidanceResult 返回
     *
     * @param executionPlan 执行计划
     * @param requestParams 原始请求参数
     * @return 统一认知指导结果
     */
    GuidanceResult orchestrate(ExecutionPlan executionPlan, OrchestrationContext requestParams);

    record OrchestrationContext(
        List<String> bundleFqns,
        String entityFqn,
        List<String> entityTypes,
        String subjectDomainFqn,
        Map<String, Object> contextParameters,
        String cursor,
        Integer pageSize,
        String expand
    ) {}
}
```

### 4.3 PerspectiveExecutor (14 Perspectives)

```java
package com.metaforge.agent.cognition.api.perspective;

/**
 * 视角执行器 SPI——每个内置认知视角实现此接口。
 * <p>
 * 本接口位于 api 模块，允许第三方开发者仅依赖 metaforge-agent-cognition-api
 * 即可开发自定义视角插件。每个视角实现为一个独立 Spring Bean，
 * 由 PerspectiveOrchestrationService 调度执行。执行超时阈值 200ms。
 */
public interface PerspectiveExecutor {

    /** 获取此执行器支持的视角编码 */
    PerspectiveCode supportedPerspective();

    /**
     * 执行视角查询并返回视角输出对象。
     *
     * @param ctx  视角执行上下文（Bundle FQN 列表、实体 FQN、过滤条件等）
     * @return 视角输出（可为 null 表示该视角不适用当前上下文）
     */
    Object execute(PerspectiveExecutionContext ctx);
}
```

**14 个内置视角执行器**:

| 执行器类 | 视角编码 | Scope | 输出类型 |
|---------|---------|-------|---------|
| `EntityProfileExecutor` | `entity_profile` | BOTH | `EntityProfile` |
| `DomainLocationExecutor` | `domain_location` | ENTITY | `DomainLocation` |
| `CompositionTreeExecutor` | `composition_tree` | ENTITY | `CompositionTree` |
| `RelationshipGraphExecutor` | `relationship_graph` | ENTITY | `RelationshipGraph` |
| `ConstraintSetExecutor` | `constraint_set` | BOTH | `ConstraintSet` |
| `CapabilityCatalogExecutor` | `capability_catalog` | BOTH | `CapabilityCatalog` |
| `FlowBlueprintExecutor` | `flow_blueprint` | BUNDLE | `FlowBlueprint` |
| `DecisionMatrixExecutor` | `decision_matrix` | BOTH | `DecisionMatrix` |
| `ImpactTraceExecutor` | `impact_trace` | ENTITY | `ImpactTrace` |
| `PrerequisiteChainExecutor` | `prerequisite_chain` | BOTH | `PrerequisiteChain` |
| `DomainNavigationExecutor` | `domain_navigation` | BUNDLE | `DomainNavigation` |
| `InstanceCatalogExecutor` | `instance_catalog` | BOTH | `InstanceCatalog` |
| `BundleDirectoryExecutor` | `bundle_directory` | BUNDLE | `BundleDirectory` |
| `SchemaInventoryExecutor` | `schema_inventory` | BUNDLE | `SchemaInventory` |

```java
package com.metaforge.agent.cognition.api.perspective;

import java.util.List;
import java.util.Map;

/**
 * 视角执行上下文——传递给每个 PerspectiveExecutor 的执行参数。
 */
public record PerspectiveExecutionContext(
    ContextMode contextMode,                        // 当前上下文模式
    List<String> bundleFqns,                        // Bundle FQN 列表
    String entityFqn,                               // 实体 FQN（ENTITY_LEVEL 非空）
    List<String> entityTypes,                       // 实体类型过滤
    String subjectDomainFqn,                        // 主题域 FQN
    Map<String, Object> contextParameters,           // 附加参数
    String cursor,
    Integer pageSize,
    String expand                                   // lazy / all
) {}
```

### 4.4 TokenBudgetService

```java
package com.metaforge.cognition.service;

/**
 * Token 预算控制服务。
 * 当 GuidanceResult 构建完毕、总 Token 量超过 max_tokens 时执行自动裁剪。
 * 裁剪策略：保留全视角覆盖，截断各视角非核心内容，不删除任何视角章节。
 * max_tokens < 500 时自动降为 L1 模式（提前在 TemplateResolutionService 处理）。
 */
public interface TokenBudgetService {

    /**
     * 裁剪 GuidanceResult 以控制在 maxTokens 预算内。
     * <p>
     * 裁剪优先级（从后往前截断）：
     * 1. 截断 entity_profile.content 中非核心字段
     * 2. 截断 composition_tree 深层子节点
     * 3. 截断 relationship_graph 非核心关系组
     * 4. 截断 instance_catalog 实例列表
     * 5. 截断 description 字段
     *
     * @param result   原始结果
     * @param maxTokens Token 预算上限
     * @return 裁剪后的结果，设置 tokenTrimmed=true
     */
    GuidanceResult trim(GuidanceResult result, int maxTokens);

    /**
     * 估算 GuidanceResult 的当前 Token 总量。
     */
    long estimateTokens(GuidanceResult result);
}
```

### 4.5 OutputFormattingService

```java
package com.metaforge.cognition.service;

/**
 * 输出格式化服务。
 * 将 GuidanceResult 转换为 json 或 prompt（Markdown）格式。
 */
public interface OutputFormattingService {

    /**
     * 格式化 GuidanceResult。
     *
     * @param result  认知指导结果
     * @param format  目标格式（json / prompt）
     * @return 格式化后的字符串内容
     */
    String format(GuidanceResult result, OutputFormat format);
}
```

### 4.6 VersionAnchorService

```java
package com.metaforge.cognition.service;

import java.util.List;

/**
 * 版本锚定服务。
 * 查询时刻获取各 Bundle 的已发布版本号，组装 DataVersionAnchor 列表。
 */
public interface VersionAnchorService {

    /**
     * 为指定 Bundle FQN 列表获取当前数据版本锚。
     *
     * @param bundleFqns Bundle FQN 列表
     * @return 每个 Bundle 的版本锚
     */
    List<DataVersionAnchor> resolveAnchors(List<String> bundleFqns);
}
```

### 4.7 FqnValidationService

```java
package com.metaforge.cognition.service;

/**
 * FQN 校验服务。
 * 校验 bundle_fqns 格式合法性、entity_fqn 归属校验、FQN 前缀匹配。
 */
public interface FqnValidationService {

    /**
     * 校验 bundle_fqns 格式合法性。
     *
     * @throws InvalidBundleFqnException 如果格式非法
     * @throws EmptyBundleFqnsException 如果列表为空
     */
    void validateBundleFqns(List<String> bundleFqns);

    /**
     * 校验 entity_fqn 前缀是否属于已发布 Bundle。
     * <p>
     * 通过 FQN 前缀匹配已发布 Bundle FQN，不依赖预存上下文。
     *
     * @param entityFqn 实体 FQN
     * @return 匹配到的 bundleFqn（即席恢复的 Bundle 范围）
     * @throws InvalidEntityFqnException 如果前缀不属于任何已发布 Bundle
     */
    String resolveBundleFromEntityFqn(String entityFqn);
}
```

### 4.8 ScopeNarrowingService

```java
package com.metaforge.cognition.service;

/**
 * 层级化作用域收窄服务。
 * INHERITED 模式下对入口实体执行三层范围收窄。
 */
public interface ScopeNarrowingService {

    /**
     * 执行三层范围收窄。
     * <p>
     * 收窄步骤：
     * 1. 蓝图收窄 — 定向邻接遍历（relationTypes=[PROCESS_SEQUENCE]，从 entryFqn 出发）
     * 2. 实体收集 — 通过图查询收集关联实体 FQN
     * 3. Schema 收窄 — 从实体反查 entity_schema_fqn 去重
     *
     * @param entryEntityFqn 入口实体 FQN
     * @return 收窄后的作用域（收窄后的实体 FQN 列表、Schema FQN 列表、蓝图步骤 FQN 列表）
     */
    NarrowedScope narrow(String entryEntityFqn);

    record NarrowedScope(
        List<String> blueprintStepFqns,               // 蓝图收窄后的步骤 FQN 列表
        List<String> relatedEntityFqns,               // 实体收集的关联实体 FQN 列表
        List<String> relatedSchemaFqns                // Schema 收窄后的 EntitySchema FQN 列表（去重）
    ) {}
}
```

### 4.9 ChangeWatchService

```java
package com.metaforge.cognition.service;

/**
 * 变更影响感知服务。
 * 以 best-effort 语义监听元数据变更事件和关系变更事件，
 * 委托影响追溯视角计算受影响实体范围，生成影响报告。
 */
public interface ChangeWatchService {

    /**
     * 处理元数据变更事件（best-effort）。
     * <p>
     * 以变更实体 FQN 为起点正向扩散影响，生成影响报告。
     * 不重放错过的事件，不引入持久化事件日志。
     *
     * @param changedEntityFqn 变更实体 FQN
     * @return 影响报告
     */
    ImpactTrace handleMetadataChange(String changedEntityFqn);

    /**
     * 处理关系变更事件（best-effort）。
     *
     * @param changedRelationFqn 变更关系 FQN
     * @return 影响报告
     */
    ImpactTrace handleRelationChange(String changedRelationFqn);
}
```

---

## 5. Upstream Client Ports

本 BC 通过进程内 Java Interface 调用上游 BC 的服务。以下端口为防腐层适配接口——将上游 DTO 转换为本 BC 领域对象。

### 5.1 MetamodelClientPort

```java
package com.metaforge.cognition.port.client;

import java.util.List;

/**
 * 元模型治理 BC 客户端端口。
 * <p>
 * 适配 metaforge-metamodel-api 的 Application Service 接口，
 * 将上游 DTO 转换为本 BC 的领域对象。
 */
public interface MetamodelClientPort {

    /**
     * 按 Bundle FQN 查询 Bundle 信息。
     */
    BundleDto getBundle(String fqn);

    /**
     * 分页查询所有已发布 Bundle 列表。
     */
    List<BundleDto> listBundles(int page, int size);

    /**
     * 按 Bundle FQN 查询该 Bundle 的最新已发布版本。
     */
    BundleVersionDto getLatestPublishedVersion(String bundleFqn);

    /**
     * 按 FQN 查询 EntitySchema 定义。
     */
    EntitySchemaDto getEntitySchema(String fqn);

    /**
     * 按 FQN 前缀集合查询 EntitySchema 列表。
     */
    List<EntitySchemaDto> listEntitySchemasByPrefixes(List<String> fqnPrefixes);

    /**
     * 检查指定 FQN 前缀是否属于已发布 Bundle。
     *
     * @param fqnPrefix FQN 前缀
     * @return 匹配的 Bundle FQN（可选）
     */
    Optional<String> resolveBundleFqnByPrefix(String fqnPrefix);
}
```

### 5.2 MetadataClientPort

```java
package com.metaforge.cognition.port.client;

import java.util.List;

/**
 * 元数据管理 BC 客户端端口。
 * <p>
 * 适配 metaforge-metadata-api 的 Application Service 接口。
 * 仅查询生效版本（主表数据）。
 */
public interface MetadataClientPort {

    /**
     * 按 FQN 精准查询生效元数据完整内容。
     */
    MetadataEntityDto getByFqn(String fqn);

    /**
     * 按 FQN 前缀集合查询生效元数据列表（结果按 FQN 排序）。
     */
    List<MetadataEntityDto> listByFqnPrefixes(List<String> fqnPrefixes, int page, int size);

    /**
     * 按 EntitySchema FQN 查询该类型的所有生效元数据实例。
     */
    List<MetadataEntityDto> listByEntitySchema(String entitySchemaFqn, int page, int size);

    /**
     * 按属性条件组合查询生效元数据。
     */
    List<MetadataEntityDto> queryByAttributes(List<AttributeCondition> conditions, String matchMode);
}
```

### 5.3 GraphClientPort

```java
package com.metaforge.cognition.port.client;

import java.util.List;

/**
 * 语义关系网络 BC 客户端端口。
 * <p>
 * 适配 metaforge-graph-api 的 Application Service 接口。
 * 提供图查询与拓扑分析能力。
 */
public interface GraphClientPort {

    /**
     * 查询实体的出边关系列表。
     *
     * @param entityFqn        实体 FQN
     * @param relationTypes    关系类型过滤（空=全部）
     * @param targetEntityTypes 目标实体类型过滤（空=全部）
     */
    List<RelationInstanceDto> getOutboundRelations(String entityFqn,
        List<AssociationType> relationTypes, List<String> targetEntityTypes);

    /**
     * 查询实体的入边关系列表。
     */
    List<RelationInstanceDto> getInboundRelations(String entityFqn,
        List<AssociationType> relationTypes, List<String> sourceEntityTypes);

    /**
     * 多维过滤查询——维度间 AND，维度内 OR。
     */
    List<RelationInstanceDto> multiFilter(RelationQueryCriteria criteria);

    /**
     * 查询实体关联的依赖关系（DEPENDENCY_INFLUENCE 类型）。
     */
    List<String> getDependentRelations(String entityFqn);

    /**
     * 查询实体的关系计数。
     */
    RelationCount getRelationCount(String entityFqn);

    record RelationCount(long outboundCount, long inboundCount) {}

    record RelationQueryCriteria(
        List<AssociationType> relationTypes,
        List<String> sourceEntityTypes,
        List<String> targetEntityTypes,
        List<String> sourceEntityFqns,
        List<String> targetEntityFqns,
        String fqnPrefix,
        int page,
        int size
    ) {}
}
```

### 5.4 ComputeEngineClientPort

```java
package com.metaforge.cognition.port.client;

import java.util.List;

/**
 * 语义查询引擎 BC 客户端端口。
 * <p>
 * 适配 metaforge-compute-engine-api 的 Application Service 接口。
 * 提供高级图计算与路径推理能力。
 */
public interface ComputeEngineClientPort {

    /**
     * 多度邻接查询——以起点实体为中心的图扩展。
     *
     * @param sourceFqn     起点 FQN
     * @param direction      遍历方向
     * @param maxDepth       最大深度
     * @param relationTypes  关注关系类型（空=全类型）
     */
    GraphQueryResult queryAdjacency(String sourceFqn, String direction,
        int maxDepth, List<AssociationType> relationTypes);

    /**
     * 组合层级树查询——基于 COMPOSITION 关系递归展开。
     *
     * @param rootFqn    根节点 FQN
     * @param direction  FORWARD / BACKWARD / BOTH
     * @param maxDepth    最大深度
     */
    GraphQueryResult queryCompositionTree(String rootFqn, String direction, int maxDepth);

    /**
     * 子图提取查询——以中心实体为种子扩展子图。
     */
    GraphQueryResult querySubgraph(List<String> centerFqns, int expandDepth,
        List<AssociationType> relationTypes);

    /**
     * 正向影响扩散查询。
     */
    ImpactTraceResult diffuseForward(String sourceFqn, List<AssociationType> relationTypes,
        int maxDepth);

    /**
     * 反向依赖溯源查询。
     */
    ImpactTraceResult traceBackward(String sourceFqn, List<AssociationType> relationTypes,
        int maxDepth);

    /**
     * 影响路径详情查询（两点间所有传导路径）。
     */
    ImpactTraceResult getImpactPaths(String sourceFqn, String targetFqn,
        List<AssociationType> relationTypes, int maxDepth);

    /**
     * 传递闭包推理。
     */
    ClosureResult computeClosure(String sourceFqn, List<AssociationType> relationTypes);

    /**
     * 批量语义查询——一次传入最多 200 个 FQN。
     */
    GraphQueryResult queryBatch(List<String> fqns, int page, int size);

    /**
     * 多条件复合检索（唯一支持分页的查询模式）。
     */
    PageResult<EntitySummary> searchCompound(List<String> entityTypes,
        List<AttributeCondition> conditions, int page, int size);
}
```

### 5.5 Upstream DTO to Domain Object Mapping

```java
package com.metaforge.cognition.port.client.mapper;

/**
 * 上游客户端端口 DTO → 本 BC 领域对象的转换映射表。
 * 所有映射在客户端端口适配器中完成。
 */
public interface UpstreamDtomapper {

    /** BundleDto (metamodel-api) → EntityProfile */
    EntityProfile bundleToEntityProfile(BundleDto dto);

    /** MetadataEntityDto (metadata-api) → EntityProfile */
    EntityProfile metadataToEntityProfile(MetadataEntityDto dto);

    /** EntitySchemaDto (metamodel-api) → EntityProfile.NativeAttributeDetail 列表 */
    List<EntityProfile.NativeAttributeDetail> schemaToAttributes(EntitySchemaDto schemaDto);

    /** RelationInstanceDto (graph-api) → RelationshipGraph.RelationGroup.RelationDetail */
    RelationshipGraph.RelationGroup.RelationDetail relationToDetail(RelationInstanceDto dto);

    /** BundleDto + BundleVersionDto (metamodel-api) → DataVersionAnchor */
    DataVersionAnchor toVersionAnchor(BundleDto bundle, BundleVersionDto version);
}
```

---

## 6. Object Relationships Summary

```
                         ┌───────────────────────────┐
                         │      ConfigLoader         │
                         │  (startup, load once)      │
                         └──────────┬────────────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                                           │
     ┌────────▼────────┐                     ┌────────────▼────────┐
     │ TemplateConfig  │                     │ PerspectiveConfig   │
     │ (cognition-     │                     │ (cognition-         │
     │  templates.yml) │                     │  perspectives.yml)  │
     └────────┬────────┘                     └────────┬────────────┘
              │                                       │
              │  ┌────────────────────────────────────┘
              │  │
     ┌────────▼──▼──────────┐
     │TemplateResolutionSvc │ ← resolves ExecutionPlan
     └────────┬─────────────┘
              │
     ┌────────▼──────────────────┐
     │PerspectiveOrchestrationSvc│ ← orchestrates 14 executors
     └────────┬──────────────────┘
              │
     ┌────────▼────────┐    ┌─────────────────┐
     │TokenTrimmingSvc  │    │VersionAnchorSvc │
     └────────┬─────────┘    └────────┬────────┘
              │                       │
     ┌────────▼───────────────────────▼──┐
     │          GuidanceResult           │ (aggregate root)
     │  ├─ contextMeta                   │
     │  ├─ entityProfile                 │
     │  ├─ domainLocation                │
     │  ├─ compositionTree               │
     │  ├─ relationshipGraph             │
     │  ├─ constraintSet                 │
     │  ├─ capabilityCatalog             │
     │  ├─ flowBlueprint                 │
     │  ├─ decisionMatrix                │
     │  ├─ impactTrace                   │
     │  ├─ prerequisiteChain             │
     │  ├─ bundleDirectory               │
     │  ├─ domainNavigation              │
     │  ├─ instanceCatalog               │
     │  └─ schemaInventory               │
     └────────┬──────────────────────────┘
              │
     ┌────────▼──────────────┐
     │OutputFormattingService │ → json / prompt
     └───────────────────────┘
```

**Upstream client ports** (not persisted, invoked per-request):

```
┌──────────────────────┐
│  MetamodelClientPort │ ──── metaforge-metamodel-api
│  (Bundle/Version/    │      BundleManagementService
│   EntitySchema CRUD) │      BundleVersionManagementService
└──────────────────────┘      ElementDefinitionService

┌──────────────────────┐
│  MetadataClientPort  │ ──── metaforge-metadata-api
│  (M1 instance query) │      MetadataQueryService
└──────────────────────┘

┌──────────────────────┐
│  GraphClientPort     │ ──── metaforge-graph-api
│  (relation/topology  │      RelationQueryService
│   query)              │      RelationTopologyService
└──────────────────────┘

┌──────────────────────┐
│ ComputeEngineClient  │ ──── metaforge-compute-engine-api
│ Port                 │      GraphQueryService
│ (advanced graph      │      PathReasoningService
│  computation)         │      ImpactTracingService
└──────────────────────┘
```
