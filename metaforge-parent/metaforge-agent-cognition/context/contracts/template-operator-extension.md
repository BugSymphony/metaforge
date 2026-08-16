---
id: agent-cognition.template-operator-extension
protocol: Extension Guide
version: 1.0.0
owner: metaforge-agent-cognition
description: 模板与算子的扩展开发方式完整指南——新增认知能力 = 新增算子（实现 SPI）+ 声明模板（YAML 装配），引擎核心零改动。涵盖模板 YAML 结构/校验/注册、算子 SPI/端口/上下文/scope 语义/错误码。
type: business
---

# Extension Guide: 模板与算子扩展开发方式

**适用范围**: `metaforge-agent-cognition-dimensions`（算子实现方）、`metaforge-agent-cognition-templates`（模板装配方）、下游 BC（扩展认知能力方）
**扩展铁律**: 新增认知能力 = **新增算子（实现 SPI 接口）+ 模板装配（YAML 声明引用）**，全程无需修改 `-core` 引擎代码。8 分类（`DimensionCategory`）为封闭枚举，新增能力只能在既有分类下新增算子。

---

## 一、总体模型

```
请求 → CognitionQueryServiceImpl
      ├─ 1. TemplateResolutionService   按 templateId 解析 TemplateDefinition
      ├─ 2. ScopeResolutionService      校验 scope 合法性（scopeRequired / bundle 有效性 / entity 越界）
      ├─ 3. filterByOperators           按请求 selectOperators 裁剪算子（空/缺省=全量）
      ├─ 4. filterByArchetype           按 agent_archetype 白名单裁剪
      ├─ 5. DepthTrimmingService        按 cognition_depth 裁剪（required 豁免）
      ├─ 6. OperatorOrchestrationService 虚拟线程并行调度算子（priority 降序）
      ├─ 7. OutputAssemblyService        按 category 分组结果 + 聚合 updated_scope
      └─ 8. FormatterRegistry           按 format 分发 OutputFormatter（json/prompt）
```

扩展方只接触 **1 的模板文件** 与 **6 的算子实现**，其余均为引擎固定行为。

---

## 二、算子扩展（新增认知能力）

### 2.1 SPI 接口

实现方在 Maven 依赖 `metaforge-agent-cognition-api` 后，实现 `CognitionOperator` 接口：

```java
package com.metaforge.agent.cognition.api.spi;

public interface CognitionOperator {
    String operatorId();                                    // 算子唯一标识
    DimensionCategory category();                           // 归属分类（8 分类封闭集合）
    CognitionResult execute(CognitionQueryContext context); // 执行查询
}
```

### 2.2 推荐继承 AbstractCognitionOperator（-dimensions 提供）

`-dimensions` 模块提供抽象基类，已自动注入 4 个只读端口并封装通用工具，新算子直接继承：

```java
package com.metaforge.agent.cognition.operator.common;

public abstract class AbstractCognitionOperator implements CognitionOperator {
    @Autowired protected MetamodelReadPort  metamodelReadPort;  // 元模型治理 BC（Bundle/EntitySchema/RelationSchema/Package）
    @Autowired protected MetadataReadPort   metadataReadPort;   // 元数据管理 BC（元数据实例实体）
    @Autowired protected GraphReadPort      graphReadPort;      // 语义关系网络 BC（生效态关系实例）
    @Autowired protected ComputeEngineReadPort computeEngineReadPort; // 语义查询引擎 BC（多跳推理/影响追溯）
    // 工具方法：
    //   executeWithPort(Supplier<?>)  端口调用异常包装为 CognitionResult.failure("PORT_CALL_FAILED: ...")
    //   applyScope(data, scope)       ScopeFilterResult(inScopeItems, skippedFqns) 按 scope 五字段裁剪
    //   buildLazyNode(data, hasChildren, suggestedNextCall)  lazy 模式节点
    //   resolveContentValue(dto, key) 从 MetadataEntityDto.content 读取字段
    //   toContentMap(dto) / toEntityMap(dto)  DTO → Map
    //   wrapFailure(msg)              构建失败结果
}
```

**不依赖 `-core`**：算子与引擎之间无编译期依赖，`-core` 通过 Spring 容器在运行时发现算子 Bean（`@Component` 声明即可注册）。

### 2.3 四个只读端口（查询能力来源）

| 端口 | 上游 BC | 典型方法 | 用途 |
|------|---------|---------|------|
| `MetamodelReadPort` | metamodel-governance | `getBundle` / `listBundles` / `getEntitySchema` / `listEntitySchemas` / `getRelationSchema` / `listRelationSchemas` / `listPackages` | Bundle 发现、Package 探索、实体/关系类型盘点 |
| `MetadataReadPort` | metadata-management | `getByFqn` / `listByFqnPrefixes` / `listByEntitySchema` | 元数据实例（实体画像、实例目录） |
| `GraphReadPort` | semantic-relation-network | `getOutboundRelations(entityFqn, relationType, targetType)` / `getInboundRelations(...)` / `multiFilter(request)` / `listByConditions` | 关系实例查询、能力发现、协议引用（`relationSchemaFqnPrefix` 前缀过滤） |
| `ComputeEngineReadPort` | semantic-query-engine | `multiHopReasoning` / `diffuseForward` / `traceBackward` / `findPaths` / `computeClosure` / `getImpactPaths` | 流程蓝图（PROCESS_SEQUENCE 多跳）、决策分支、影响追溯、邻域 |

**分层/分页约定**：
- 分页参数从 `context.cursor()`（`params.cursor`）与 `context.pageSize()`（`params.page_size`，缺省取配置 `defaults.page-size`=20）解析；
- 大规模清单（Bundle/Package/EntitySchema）返回 lazy 节点（`buildLazyNode` + `has_children` + `suggested_next_call`）；
- 单实体画像/协议细节返回 full 数据。

### 2.4 CognitionQueryContext（算子入参）

```java
public record CognitionQueryContext(
    String templateId,              // 模板唯一标识
    String operatorId,              // 当前算子标识
    DimensionCategory category,     // 当前算子分类
    Scope scope,                    // 认知边界五字段（bundles/packages/domainGroups/domains/entitySchemas）
    List<String> bundleFqns,        // scope 解析出的 Bundle FQN 列表
    String entityFqn,               // 目标实体 FQN（= params.entity_fqn）
    Map<String, Object> templateParams, // 模板参数（inputSchema 定义键值对，含 selectOperators/entity_fqn/level/parent_fqn 等）
    AgentArchetype agentArchetype,  // Agent 原型（EXECUTION/EXPLORATION/AUDIT/ORCHESTRATION）
    CognitionDepth cognitionDepth,  // L1/L2/L3
    Integer cursor,                 // 分页游标
    int pageSize,                   // 每页大小
    Map<String, Object> templateConfig,  // 模板 config.global（或单层 config），全模板共享
    Map<String, Object> operatorConfig   // 模板 config.operators.{operatorId}，算子级精确配置
) {}
```

**config 双层结构**（Step 1 引入）：
- `templateConfig` ← 模板 YAML `config.global`（向后兼容单层 `config` 顶层，如 ORIENT `levelAliases`）；
- `operatorConfig` ← 模板 YAML `config.operators.{operatorId}`（对齐 opencode 每 Tool 独立 parameters），算子按需读取，未配置为空 Map。

### 2.5 CognitionResult（算子出参）

```java
public record CognitionResult(
    String operatorId,
    DimensionCategory category,
    Object data,      // 结果数据（LinkedHashMap 结构化 Map）
    boolean success,
    String error
) {
    static CognitionResult success(String operatorId, DimensionCategory category, Object data);
    static CognitionResult failure(String operatorId, DimensionCategory category, String error);
}
```

**scope 越界与 updated_scope 契约**：
- 算子内部必须按 `context.scope()` 裁剪查询范围，越界实体**不得输出**；应通过 `applyScope` 收集越界 FQN 并在 `context_meta.skipped_entities` 呈现（引擎自动聚合）；
- 若算子产出收窄后的范围，在 `data` 内放 `updated_scope` 键（如 `{"updated_scope": {"domains": [...]}}`）；**仅当模板 `scopeBehavior.producesUpdatedScope=true` 时**，`OutputAssemblyService` 才会聚合所有算子的 `updated_scope` 到 `context_meta.updated_scope`（DELEGATE 三层收窄即依赖此机制）。

### 2.6 注册与校验（零配置，Spring 自动发现）

`OperatorRegistryInitializer`（`ApplicationReadyEvent`）收集容器中全部 `CognitionOperator` Bean → `OperatorRegistry.registerAll`：

- **校验项**：`operatorId()` 非空；`category()` 非空且必须在 `DimensionCategory` 枚举内；`operatorId` 全局唯一；
- **失败语义**：校验失败的算子 WARN 日志并跳过，不影响其他算子注册、不阻塞引擎启动；
- **启动期一次性注册**：MVP 阶段新增算子需重启系统（热加载留待 P1）。

### 2.7 完整算子示例

```java
@Component
public class OntologicalDomainDrillDownOperator extends AbstractCognitionOperator {

    private static final String CONFIG_KEY_LEVEL_ALIASES = "levelAliases";

    @Override
    public String operatorId() {
        return "ontological.domain-drilldown";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.ONTOLOGICAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String level = getLevel(context);
        String entityFqn = context.entityFqn();
        String parentFqn = getParentFqn(context);
        String anchor = parentFqn != null && !parentFqn.isBlank() ? parentFqn : entityFqn;

        String levelFqn = resolveLevelFqn(level, context);          // 读取 config.levelAliases
        List<Map<String, Object>> entities = collectEntities(anchor, levelFqn, context);
        List<Map<String, Object>> lazyNodes = buildLazyNodes(entities);
        Map<String, List<Map<String, Object>>> grouped = groupByEntityType(entities);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("children_grouped", grouped);
        resultData.put("children", lazyNodes);
        resultData.put("level", level);
        resultData.put("updated_scope", Map.of("domains", discoveredDomains));

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveAliases(CognitionQueryContext context) {
        Map<String, Object> config = context.templateConfig();
        if (config != null && config.get(CONFIG_KEY_LEVEL_ALIASES) instanceof Map<?, ?> aliases) {
            return (Map<String, Object>) aliases;
        }
        return Collections.emptyMap();
    }

    private List<Map<String, Object>> expandCompositionChildren(String parentFqn) {
        Object result = executeWithPort(() ->
                graphReadPort.getOutboundRelations(parentFqn, "COMPOSITION", null));
        // ... 逐层下钻，越界实体跳过
    }
}
```

---

## 三、模板扩展（认知场景装配）

### 3.1 模板文件位置与注册

| 来源 | 位置 | 优先级 | 说明 |
|------|------|--------|------|
| classpath | `classpath:cognition/templates/*.yml`（配置项 `templates.classpath-location` 默认） | 低 | 打包进 jar，随 boot 启动扫描 |
| 外部 | `templates.external-location`（`file:` 协议目录） | **高** | 同 templateId 时覆盖 classpath；`hot-reload.enabled=true` 时支持文件热加载（轮询 `poll-interval-ms`，默认 5000ms） |

`TemplateScanner` 在 `ApplicationReadyEvent` 扫描 → `TemplateYamlParser` 解析 → `TemplateDefinition.validate()` 校验 → `TemplateRegistry` 注册。模板数量可热加载（`TemplateFileWatcher` + `triggerFullRescan()`）。

### 3.2 模板 YAML 完整结构（字段级说明）

```yaml
templateId: GUIDE                        # 必填，正则 [A-Z][A-Z0-9_]+，全平台唯一
templateName: "单步执行指南"              # 可选，人类可读名
description: "为单步操作提供精细执行指导"   # 可选
version: "1.0.0"                         # 可选
stage: P0                                # 可选
enabled: true                            # 可选

operators:                               # 必填，至少 1 条
  - operatorId: ontological.entity-profile   # 必填，[A-Za-z][A-Za-z0-9._-]+，且必须含 "."
    priority: 100                            # 可选，默认 0；数值大优先执行（降序）
    required: true                           # 可选，默认 false；true=失败致模板整体失败
    timeoutMs: 800                           # 可选，默认全局配置 10000ms
    archetypes: [execution, audit]           # 可选，默认全部；EXECUTION/EXPLORATION/AUDIT/ORCHESTRATION 子集

inputSchema:                             # 可选
  type: object
  properties:
    selectOperators:                     # 引擎内置参数：运行时算子子集选择（空/缺省=执行全部）
      type: array
      default: []
      items: { type: string }
    entity_fqn:                          # 引擎内置约定：目标实体（算子经 context.entityFqn() 读取）
      type: string
    level:                               # 模板自定义参数示例
      type: string
  required: []                           # 可选，必填键

scopeBehavior:                           # 可选
  acceptsScope: true                     # 接受 scope 限定
  scopeRequired: false                   # 必填 scope；true 时自动修正 acceptsScope=true
  producesUpdatedScope: true             # 聚合算子 updated_scope 到 context_meta（DELEGATE 依赖）
  scopeFields: [bundles, packages, domains, entity_schemas]

outputSchema:                            # 可选
  type: GUIDE_RESULT
  formats: [json, prompt]

contextMeta:                             # 可选（文档用途，引擎按字段生成）
  includeVersionAnchors: true
  includeScopeApplied: true
  includeTokenEstimate: true

config:                                  # 可选，双层配置（Step 1 引入）
  global:                                # → templateConfig（全模板共享；兼容单层 config 顶层）
    levelAliases:
      order: "order:1.0.0.SubjectDomain"
      inventory: "order:1.0.0.InventoryDomain"
  operators:                             # → operatorConfig.{operatorId}（算子级精确配置）
    ontological.domain-drilldown:
      relationSchemaFqnPrefix: "metaforge:1.0.0.protocol."
```

### 3.3 模板校验规则（TemplateDefinition.validate）

1. `templateId` 非空且匹配 `[A-Z][A-Z0-9_]+`；
2. `operators` 至少声明 1 条算子；
3. 每条算子：`operatorId` 非空、匹配 `[A-Za-z][A-Za-z0-9._-]+`、**必须含 "."**（分类前缀分隔符）、`priority>=0`、`timeoutMs>0`；
4. `scopeBehavior.scopeRequired=true` 自动修正 `acceptsScope=true`。
5. 校验失败的模板 WARN 日志并跳过注册，不阻塞其他模板。

### 3.4 模板消费语义（运行时管线裁剪）

| 阶段 | 规则 | 失败语义 |
|------|------|---------|
| `selectOperators` 过滤 | 请求 `params.selectOperators` 命中模板声明的算子子集；空/缺省=全量 | 全不匹配 → `INVALID_OPERATOR_SELECTION`(34014) |
| archetype 过滤 | 算子 `archetypes` 白名单（缺省=全部原型） | 全被过滤 → `ARCHETYPE_NOT_SUPPORTED`(34012) |
| 深度裁剪 | L3 全量；L1/L2 按保留比例（0.33/0.67）裁剪**非 required** 算子（按 priority 降序保留，不低于 min-keep=3）；`required=true` 豁免 | 裁剪算子记入 `context_meta.truncated_perspectives` |
| 算子执行 | 按 priority 降序并行调度；每个算子独立超时（`timeoutMs`） | required 失败 → `OPERATOR_EXECUTION_ERROR`(34006)/`OPERATOR_TIMEOUT`(34007)；非 required 失败 WARN 并继续 |
| 输出组装 | 结果按 category 分组（key=分类小写名）；`producesUpdatedScope=true` 时聚合各算子 `updated_scope` | — |

> 注意：模板消费语义仅与算子条目字段（operatorId/priority/required/timeoutMs/archetypes）有关，**与算子实现类无编译期依赖**——模板 YAML 是装配层，算子实现是能力层，两者通过 operatorId 字符串在运行时绑定。

---

## 四、扩展方式速查（按场景）

| 场景 | 动作 | 位置 |
|------|------|------|
| 新增认知能力（新算子） | 实现 `CognitionOperator`（推荐继承 `AbstractCognitionOperator`），`@Component` 声明 | `-dimensions` 或下游 BC |
| 新增认知场景（新模板） | 编写 YAML 到 `classpath:cognition/templates/` 或外部目录，引用既有算子 | `-templates` 或 boot |
| 调整某模板的算子组合 | 改模板 `operators`（增删条目、调 priority/required/archetypes） | 模板 YAML |
| 给某模板指定算子级参数 | 模板 `config.operators.{operatorId}` → 算子读 `context.operatorConfig()` | 模板 YAML |
| 给某模板指定全局参数 | 模板 `config.global` → 算子读 `context.templateConfig()` | 模板 YAML |
| 新增输出格式 | 实现 `OutputFormatter`（`supports(OutputFormat)` + `format(...)`）+ `@Component` | 引擎外扩展 |
| 扩展 8 分类 | **不可行**（`DimensionCategory` 封闭枚举，设计约束） | — |

---

## 五、错误码速查（34000-34099）

| 错误码 | 常量 | 触发场景 |
|--------|------|---------|
| 34001 | `TEMPLATE_NOT_FOUND` | templateId 未注册 |
| 34002 | `TEMPLATE_INVALID` | 模板配置校验失败 |
| 34003 | `INVALID_SCOPE` | scope 中 bundle/package/entitySchema FQN 无效 |
| 34004 | `ENTITY_OUT_OF_SCOPE` | 查询实体超出 scope 边界 |
| 34005 | `MISSING_SCOPE` | scopeRequired 模板缺少 scope |
| 34006 | `OPERATOR_EXECUTION_ERROR` | required=true 算子执行失败 |
| 34007 | `OPERATOR_TIMEOUT` | required=true 算子超时 |
| 34008 | `UPSTREAM_UNAVAILABLE` | 上游 BC 不可用/超时 |
| 34009 | `UNSUPPORTED_OPERATOR` | 模板引用未注册的 operatorId |
| 34010 | `INVALID_FORMAT` | format 参数无效 |
| 34011 | `UNKNOWN_OPERATOR_REF` | operatorId 分类前缀不存在 |
| 34012 | `ARCHETYPE_NOT_SUPPORTED` | 请求 archetype 在模板中无算子支持 |
| 34013 | `INVALID_LEVEL` | 请求 level 无法解析为 EntitySchema（domain-drilldown） |
| 34014 | `INVALID_OPERATOR_SELECTION` | selectOperators 无任何算子匹配模板 |

---

## 六、相关配置（application-agent-cognition.yml）

| 配置项 | 默认值 | 描述 |
|--------|--------|------|
| `metaforge.agent-cognition.templates.classpath-location` | `classpath:cognition/templates/` | 模板扫描路径 |
| `metaforge.agent-cognition.templates.external-location` | 空 | 外部模板目录（file: 协议，优先级高于 classpath） |
| `metaforge.agent-cognition.templates.hot-reload.enabled` | false | 外部模板热加载开关 |
| `metaforge.agent-cognition.templates.hot-reload.poll-interval-ms` | 5000 | 热加载轮询间隔 |
| `metaforge.agent-cognition.timeouts.operator-execute-default-ms` | 10000 | 算子默认执行超时 |
| `metaforge.agent-cognition.depth.trim-ratio-l1` | 0.33 | L1 非强制算子保留比例 |
| `metaforge.agent-cognition.depth.trim-ratio-l2` | 0.67 | L2 非强制算子保留比例 |
| `metaforge.agent-cognition.depth.min-keep` | 3 | 深度裁剪最小保留数 |
| `metaforge.agent-cognition.defaults.cognition-depth` | L2 | 请求缺省深度 |
| `metaforge.agent-cognition.defaults.agent-archetype` | execution | 请求缺省原型 |
| `metaforge.agent-cognition.defaults.format` | json | 请求缺省格式 |
| `metaforge.agent-cognition.defaults.max-tokens` | 8000 | 请求缺省 Token 预算 |
| `metaforge.agent-cognition.defaults.page-size` | 20 | 算子分页缺省大小 |
| `metaforge.agent-cognition.version-anchor.bundle-resolve-strategy` | LATEST_PUBLISHED | Bundle 版本锚解析策略 |

---

## 七、Special Constraints

- **声明式扩展铁律**：新增算子/模板不得修改 `-core`、`-api`、`-starter` 引擎代码；`-core` 对扩展无编译期依赖。
- **8 分类封闭**：`DimensionCategory` 不可配置扩展；新能力归入既有分类。
- **operatorId 全局唯一**，命名 `{category}.{能力名}`（含连字符合法）。
- **operatorId 重复或 category 非法**：算子注册 WARN 跳过，不影响引擎启动。
- **MVP 启动期注册**：新算子需重启生效；外部模板可热加载，classpath 模板需重启。
- **算子不感知模板**：算子仅依据 `CognitionQueryContext` 查询返回，不关心被哪个模板消费。
- **scope 越界禁输出**：算子输出必须受 scope 约束，越界实体进 `context_meta.skipped_entities`。
