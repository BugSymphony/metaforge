# Operator Catalog: metaforge-agent-cognition-dimensions

**协议类型**: 算子清单导出契约
**版本**: 1.0.0
**发布方**: `metaforge-agent-cognition-dimensions`
**消费方**: `metaforge-agent-cognition-templates`

> 本契约定义 `-dimensions` BC 提供的全部认知算子清单，供 `-templates` BC 在编写模板 YAML 文件时通过 `operatorId` 引用算子。算子分类由实现类的 `category()` 方法返回，归属 `DimensionCategory` 枚举。

## 一、本体论 (ONTOLOGICAL)

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `ontological.bundle-discovery` | Bundle 发现 | 列出平台已发布的 Bundle 列表，支持 scope.bundles 过滤 | MetamodelReadPort | lazy |
| `ontological.package-explorer` | Package 探索 | 通过 Bundle FQN 或父 Package FQN 列出子 Package | MetamodelReadPort, GraphReadPort | lazy |
| `ontological.entity-schema-inventory` | 实体类型盘点 | 列出 EntitySchema 类型清单，每项含 instance_count + key_attributes | MetamodelReadPort, MetadataReadPort | lazy |
| `ontological.relation-schema-inventory` | 关系类型盘点 | 列出 RelationSchema 类型清单 | MetamodelReadPort | lazy |
| `ontological.domain-drilldown` | 领域下钻 | 沿 L1-L5 主题域树逐层下钻，支持 level null 自动发现和指定 level 精确过滤 | MetadataReadPort, GraphReadPort | lazy |
| `ontological.instance-catalog` | 实例目录 | 列出某 EntitySchema 类型下的全部 M1 元数据实例，支持分页 | MetadataReadPort | full |
| `ontological.entity-profile` | 实体画像 | 返回单个实体的完整画像——全属性字段 + EntitySchema 结构说明 + domain_location 路径 | MetadataReadPort, MetamodelReadPort | full |

## 二、结构论 (STRUCTURAL)

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `structural.decomposition` | 拆解 | X 由哪些子部件组成——沿 COMPOSITION FORWARD 展开子树 | ComputeEngineReadPort | full |
| `structural.belonging` | 归属 | X 属于哪个更大整体——沿 COMPOSITION BACKWARD 追溯父链 | ComputeEngineReadPort | full |
| `structural.domain-locator` | 领域定位 | X 在 L1-L5 知识树中的路径坐标——沿 COMPOSITION 入边递归回溯至 L1 根节点 | GraphReadPort | full |

## 三、关系论 (RELATIONAL)

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `relational.direct-link` | 直连 | 查询实体 1 度出边 + 入边，按 AssociationType 分组 | GraphReadPort | full |
| `relational.neighborhood` | 邻域 | N 度邻域（1-3 度）内的关联实体列表 | ComputeEngineReadPort | full |
| `relational.impact-trace` | 影响追溯 | 正向影响扩散（BFS）+ 反向依赖溯源（逆BFS）+ 影响路径详情 | ComputeEngineReadPort | full |

## 四、流程论 (PROCEDURAL)

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `procedural.flow-blueprint` | 流程蓝图 | 端到端步骤序列——沿 PROCESS_SEQUENCE 构建最长路径有序序列，每步标注 ENTRY/DECISION/EXIT | ComputeEngineReadPort | full |
| `procedural.adjacent-step` | 前后导航 | X 的前一步（入边）+ 后一步（出边）局部导航 | GraphReadPort | full |
| `procedural.decision-branch` | 决策分支 | PROCESS_SEQUENCE 出边 >1 的决策分支点识别——每选项含条件、推荐倾向（PRIMARY/ALTERNATIVE）、下游影响 | GraphReadPort, ComputeEngineReadPort | full |

## 五、约束论 (DEONTIC)

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `deontic.rule-listing` | 规则清单 | 适用于 X 的约束规则列表——DEPENDENCY_INFLUENCE 入边 + ASSOCIATION_REFERENCE 边 + 实体详情 | GraphReadPort, MetadataReadPort | full |
| `deontic.level-classifier` | 级别分类 | 约束级别分类——MANDATORY/RECOMMENDED/REFERENCE，来自实体属性 constraint_level/level 字段 | MetadataReadPort | full |
| `deontic.condition-action` | 条件动作 | 约束的触发条件与执行动作——condition 字段 + action 字段 | MetadataReadPort | full |

## 六、能力论 (CAPABILITY)

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `capability.tool-discovery` | 工具发现 | X 关联的能力清单——ASSOCIATION_REFERENCE 出入边 | GraphReadPort | full |
| `capability.call-method` | 调用方式 | 调用方式识别——REST/MCP/CLI/LocalMethod，来自 call_method 字段 | MetadataReadPort | full |
| `capability.protocol-detail` | 协议细节 | 读取 interface_spec 按类型解析结构化 protocol（Http/McpTool/Cli/LocalMethod），未声明 type 时按字段特征推断；COMPOSITION 边展开 protocol 子类型 | MetadataReadPort, GraphReadPort | full |

## 七、认知论 (EPISTEMIC)

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `epistemic.freshness-check` | 时效性检查 | 认知新鲜度判定——对比 context_meta 中的 version_anchors 与 Agent 侧缓存的版本号；所有模板输出自动携带，不独立出 API | 无（纯 context_meta 操作） | full |

## 八、治理 (GOVERNANCE)

| operatorId | 中文名称 | 描述 | 依赖 Port | 返回模式 |
|------------|---------|------|----------|---------|
| `governance.scope-narrowing` | 范围收窄 | 三层收窄——以 entry_entity_fqn 为锚：(1)蓝图收窄（沿 PROCESS_SEQUENCE 前后 1-2 步）；(2)实体 FQN 收集；(3)Schema 反查去重 | GraphReadPort, MetadataReadPort, MetamodelReadPort | full |

## 聚合统计

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

## 模板引用方式

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

> **注意**：`-dimensions` BC 本身不关心自己被哪个模板消费——算子仅根据 `CognitionQueryContext` 执行查询返回结果。本契约仅提供算子清单作为模板编写的引用依据。
