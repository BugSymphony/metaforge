---
id: cognition-dimensions.operator-catalog
protocol: Library API
version: 1.0.0
owner: metaforge-agent-cognition-dimensions
description: 认知算子清单导出契约——25 个 CognitionOperator 的完整目录，供 -templates BC 在模板 YAML 中通过 operatorId 引用算子。
type: business
---

# Export Contract: 认知算子清单

**发布方**: `metaforge-agent-cognition-dimensions`
**消费方**: `metaforge-agent-cognition-templates`
**协议类型**: 静态清单（Library API）
**版本**: 1.0.0

> 本契约定义 `-dimensions` BC 提供的全部认知算子清单，供 `-templates` BC 在编写模板 YAML 文件时通过 `operatorId` 引用算子。算子分类由实现类的 `category()` 方法返回，归属 `DimensionCategory` 枚举。

## 一、消费方式

`-templates` BC 通过 Maven 依赖 `metaforge-agent-cognition-dimensions`（由 `-starter` 传递）获得运行时 classpath 中的算子实现类。同时可读取本契约中的算子清单作为模板 YAML 编写时的引用依据。

模板 YAML 中通过 `operatorId` 字符串引用算子——不直接依赖算子实现类的编译期类型。引擎运行时通过 `OperatorRegistry` 按 `operatorId` 解析算子实例。

## 二、引用字段说明

模板 YAML 中每个算子条目可配置以下字段：

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| operatorId | String | 是 | 算子标识（如 `ontological.bundle-discovery`），必须在本清单中存在 |
| priority | int | 否 | 执行优先级，数值越大越优先，默认 0；仅作用于执行顺序与输出排列 |
| required | boolean | 否 | 是否必须成功，默认 false；required=true 的算子失败导致模板整体失败 |
| timeoutMs | long | 否 | 算子执行超时（毫秒），默认取全局配置 |
| archetypes | String[] | 是 | 适用 AgentArchetype 白名单——必须为 EXECUTION/EXPLORATION/AUDIT/ORCHESTRATION 的子集 |

## 三、算子清单

### 本体论 (ONTOLOGICAL) — 7 个算子

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `ontological.bundle-discovery` | Bundle 发现 | 列出平台已发布的 Bundle 列表，支持 scope.bundles 过滤 | MetamodelReadPort | lazy |
| `ontological.package-explorer` | Package 探索 | 通过 Bundle FQN 或父 Package FQN 列出子 Package | MetamodelReadPort, GraphReadPort | lazy |
| `ontological.entity-schema-inventory` | 实体类型盘点 | 列出 EntitySchema 类型清单，每项含 instance_count + key_attributes | MetamodelReadPort, MetadataReadPort | lazy |
| `ontological.relation-schema-inventory` | 关系类型盘点 | 列出 RelationSchema 类型清单 | MetamodelReadPort | lazy |
| `ontological.domain-drilldown` | 领域下钻 | 沿 L1-L5 主题域树逐层下钻，支持 level null 自动发现和指定 level 精确过滤 | MetadataReadPort, GraphReadPort | lazy |
| `ontological.instance-catalog` | 实例目录 | 列出某 EntitySchema 类型下的全部 M1 元数据实例，支持分页 | MetadataReadPort | full |
| `ontological.entity-profile` | 实体画像 | 返回单个实体的完整画像——全属性字段 + EntitySchema 结构说明 + domain_location 路径 | MetadataReadPort, MetamodelReadPort | full |

### 结构论 (STRUCTURAL) — 3 个算子

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `structural.decomposition` | 拆解 | X 由哪些子部件组成——沿 COMPOSITION FORWARD 展开子树 | ComputeEngineReadPort | full |
| `structural.belonging` | 归属 | X 属于哪个更大整体——沿 COMPOSITION BACKWARD 追溯父链 | ComputeEngineReadPort | full |
| `structural.domain-locator` | 领域定位 | X 在 L1-L5 知识树中的路径坐标——沿 COMPOSITION 入边递归回溯至 L1 根节点 | GraphReadPort | full |

### 关系论 (RELATIONAL) — 3 个算子

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `relational.direct-link` | 直连 | 查询实体 1 度出边 + 入边，按 AssociationType 分组 | GraphReadPort | full |
| `relational.neighborhood` | 邻域 | N 度邻域（1-3 度）内的关联实体列表 | ComputeEngineReadPort | full |
| `relational.impact-trace` | 影响追溯 | 正向影响扩散（BFS）+ 反向依赖溯源（逆BFS）+ 影响路径详情 | ComputeEngineReadPort | full |

### 流程论 (PROCEDURAL) — 3 个算子

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `procedural.flow-blueprint` | 流程蓝图 | 端到端步骤序列——沿 PROCESS_SEQUENCE 构建最长路径有序序列，每步标注 ENTRY/DECISION/EXIT | ComputeEngineReadPort | full |
| `procedural.adjacent-step` | 前后导航 | X 的前一步（入边）+ 后一步（出边）局部导航 | GraphReadPort | full |
| `procedural.decision-branch` | 决策分支 | PROCESS_SEQUENCE 出边 >1 的决策分支点识别——每选项含条件、推荐倾向（PRIMARY/ALTERNATIVE）、下游影响 | GraphReadPort, ComputeEngineReadPort | full |

### 约束论 (DEONTIC) — 3 个算子

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `deontic.rule-listing` | 规则清单 | 适用于 X 的约束规则列表——DEPENDENCY_INFLUENCE 入边 + ASSOCIATION_REFERENCE 边 + 实体详情 | GraphReadPort, MetadataReadPort | full |
| `deontic.level-classifier` | 级别分类 | 约束级别分类——MANDATORY/RECOMMENDED/REFERENCE，来自实体属性 constraint_level/level 字段 | MetadataReadPort | full |
| `deontic.condition-action` | 条件动作 | 约束的触发条件与执行动作——condition 字段 + action 字段 | MetadataReadPort | full |

### 能力论 (CAPABILITY) — 3 个算子

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `capability.tool-discovery` | 工具发现 | X 关联的能力清单——ASSOCIATION_REFERENCE 出入边 | GraphReadPort | full |
| `capability.call-method` | 调用方式 | 调用方式识别——REST/MCP/CLI/LocalMethod，来自 call_method 字段 | MetadataReadPort | full |
| `capability.protocol-detail` | 协议细节 | 读取 interface_spec 按类型解析结构化 protocol（Http/McpTool/Cli/LocalMethod），未声明 type 时按字段特征推断；COMPOSITION 边展开 protocol 子类型 | MetadataReadPort, GraphReadPort | full |

### 认知论 (EPISTEMIC) — 1 个算子

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `epistemic.freshness-check` | 时效性检查 | 认知新鲜度判定——对比 context_meta 中的 version_anchors 与 Agent 侧缓存的版本号；所有模板输出自动携带，不独立出 API | 无（纯 context_meta 操作） | full |

### 治理 (GOVERNANCE) — 1 个算子

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `governance.scope-narrowing` | 范围收窄 | 三层收窄——以 entry_entity_fqn 为锚：(1)蓝图收窄（沿 PROCESS_SEQUENCE 前后 1-2 步）；(2)实体 FQN 收集；(3)Schema 反查去重 | GraphReadPort, MetadataReadPort, MetamodelReadPort | full |

## 四、聚合统计

| 分类 | 算子数量 |
|------|---------|
| ONTOLOGICAL | 7 |
| STRUCTURAL | 3 |
| RELATIONAL | 3 |
| PROCEDURAL | 3 |
| DEONTIC | 3 |
| CAPABILITY | 3 |
| EPISTEMIC | 1 |
| GOVERNANCE | 1 |
| **合计** | **24** |

## 五、分类枚举参考

| 枚举值 | 中文名 | 算子数 |
|--------|-------|--------|
| ONTOLOGICAL | 本体论 | 8 |
| STRUCTURAL | 结构论 | 3 |
| RELATIONAL | 关系论 | 3 |
| PROCEDURAL | 流程论 | 3 |
| DEONTIC | 约束论 | 3 |
| CAPABILITY | 能力论 | 3 |
| EPISTEMIC | 认知论 | 1 |
| GOVERNANCE | 治理 | 1 |

## 六、模板引用示例

`-templates` BC 在模板 YAML 文件中，通过 `operators` 字段引用算子，格式如下：

```yaml
templateId: DISCOVER
operators:
  - operatorId: ontological.bundle-discovery
    priority: 100
    required: true
    timeoutMs: 800
    archetypes: [EXECUTION, EXPLORATION, AUDIT, ORCHESTRATION]
```

## Special Constraints

- `-dimensions` BC 本身不关心自己被哪个模板消费——算子仅根据 `CognitionQueryContext` 执行查询返回结果。本契约仅提供算子清单作为模板编写的引用依据。
- 算子 operatorId 全局唯一，命名格式为 `{category}.{能力名}`。
- 8 分类为封闭集合，不可通过配置扩展；新增能力只能在既有分类下新增算子。
- 算子注册在启动时完成，校验失败（分类非法、operatorId 重复）的算子记录告警并跳过，不阻塞引擎启动。
