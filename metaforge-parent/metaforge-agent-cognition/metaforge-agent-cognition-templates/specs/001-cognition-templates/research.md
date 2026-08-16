# Research: 认知模板配置层

**Feature**: cognition-templates | **Phase**: 0 | **Date**: 2026-08-11

## 1. 算子引用映射 — 从契约到模板

**Decision**: 模板 `operatorId` 严格使用 operator-catalog-contract 中定义的算子标识符（格式: `{category}.{能力名}`）。

**Rationale**: 上游契约 `cognition-dimensions.operator-catalog` 是算子 ID 的唯一权威来源。模板校验时，引擎核心的 `OperatorRegistry` 按 `operatorId` 进行精确匹配，任何拼写偏差或引用不存在的算子将导致模板被拒绝注册（BC 宪法 III: 能力引用合法性）。

**Alternatives considered**:
- 模板单独声明算子别名 → 增加一层间接映射，引入歧义和同步维护成本
- 模板按分类分组引用算子 → 契约已声明跨分类扁平列表，分组会增加理解复杂度

### 6 个模板的算子映射

| 模板 | 引用算子 (operatorId) |
|------|---------------------|
| DISCOVER | `ontological.bundle-discovery`, `ontological.package-explorer`, `ontological.entity-schema-inventory`, `ontological.relation-schema-inventory`（通过 `params.selectOperators` 运行时选择展开层） |
| ORIENT | `ontological.domain-drilldown`, `structural.domain-locator` |
| BRIEF | `ontological.entity-profile`, `procedural.flow-blueprint`, `procedural.adjacent-step`, `deontic.rule-listing`, `deontic.level-classifier`, `capability.tool-discovery`, `capability.call-method`, `relational.direct-link` |
| GUIDE | `ontological.entity-profile`, `deontic.rule-listing`, `deontic.level-classifier`, `deontic.condition-action`, `capability.tool-discovery`, `capability.call-method`, `capability.protocol-detail`, `procedural.adjacent-step`, `procedural.decision-branch`, `relational.direct-link` |
| FORECAST | `relational.neighborhood`, `relational.impact-trace`, `deontic.rule-listing` |
| DELEGATE | `governance.scope-narrowing`, `ontological.entity-profile`, `deontic.rule-listing`, `capability.tool-discovery`, `procedural.flow-blueprint` |

共计引用 17 个不同算子（覆盖 7 个分类，EPISTEMIC 未在模板中显式引用因为 `epistemic.freshness-check` 由引擎自动附加）。

## 2. Archetype 值格式约定

**Decision**: 模板 YAML 中 `archetypes` 使用小写字符串（`execution`, `exploration`, `audit`, `orchestration`）。

**Rationale**: 参考 `docs/speckit/cognition-templates.md` 中的模板定义格式，结合 YAML 惯例（小写字符串更符合配置文件的自然风格）。引擎核心的 `AgentArchetype` 枚举在匹配时属于值比较，大小写由 YAML 解析器处理，最终由 `ArchetypeFilterService` 的 `containsArchetype()` 方法进行值相等判断。

**Alternatives considered**:
- 使用大写枚举名 (EXECUTION) → 契约示例使用大写，但与 YAML 惯例不一致
- 使用中文名称 → 增加国际化复杂性，降低可读性

## 3. 模板模块 Maven 结构

**Decision**: 创建 `pom.xml`（packaging: jar，仅 resources），`<parent>` 指向 `metaforge-agent-cognition`，不声明 Java 编译依赖。

**Rationale**: 本 BC 为纯配置层（BC 宪法 I），不含 Java 代码。`jar` packaging 是 Maven 标准方式打包 classpath 资源；无编译依赖确保与 `-core` 和 `-dimensions` 的解耦（BC 宪法 IV）。

**Alternatives considered**:
- 将模板 YAML 放在 `-core/src/main/resources/` 下 → 违反 BC 边界独立性，模板演进需修改 `-core`（违反 BC 宪法 V）
- 使用 `pom` packaging → 不适合资源分发场景，`pom` 打包不会生成 jar

## 4. 集成点 — 父 POM 与 Starter

**Decision**: 
1. 在 `metaforge-agent-cognition/pom.xml` 的 `<modules>` 中添加 `metaforge-agent-cognition-templates`
2. 在 `metaforge-agent-cognition-starter/pom.xml` 中添加对 `metaforge-agent-cognition-templates` 的依赖

**Rationale**: 父 POM 模块注册使模块参与统一构建；starter 聚合依赖使 `metaforge-boot` 只需依赖一个 `-starter` 即可获得完整认知引擎能力（包括模板文件在 classpath 上的可用性）。

**Alternatives considered**:
- 仅在 `-boot` 层直接依赖 `-templates` → 破坏 starter 作为"一站式聚合"的设计意图
- 使用 Maven scope `runtime` → 合理，但 `-starter` 本身就是运行时聚合模块，默认 scope 即可

## 5. 模板 YAML 文件 Schema 对齐

**Decision**: 严格按照 `docs/speckit/cognition-templates.md` 第三章（模板文件 Schema 规范）和第四章（各模板完整定义）生成 6 个 YAML 文件。字段类型、必填性、枚举值范围完全对齐。

**Rationale**: `docs/speckit/cognition-templates.md` 是规格的权威来源文档，包含所有字段定义、约束说明和验收场景。引擎核心的 `TemplateYamlParser` 按此 Schema 解析和校验模板文件；任何偏差会导致解析失败或注册被拒。

**Alternatives considered**: 无 — 模板 Schema 由引擎核心层定义，本模块只能消费，不能自行裁定。

## 6. 模板版本与启用状态

**Decision**: 6 个内置模板初始版本均为 `"1.0.0"`，`enabled` 均为 `true`，`stage` 均为 `P0`。

**Rationale**: MVP 阶段 6 个模板为核心消费场景的必要组成，全部启用；版本号从 1.0.0 起始符合语义化版本惯例；`stage: P0` 表示 MVP 首批交付。

## 7. 输出格式

**Decision**: 每个模板的 `outputSchema.formats` 均声明 `["json", "prompt"]`。

**Rationale**: JSON 格式供程序化消费，prompt 格式供直接注入 Agent 上下文——覆盖两类核心消费模式。

## 8. scopeBehavior 字段取值

| 模板 | acceptsScope | scopeRequired | producesUpdatedScope | scopeFields |
|------|-------------|---------------|---------------------|-------------|
| DISCOVER | true | false | true | [bundles, packages, entity_schemas] |
| ORIENT | true | false | true | [bundles, domain_groups, domains, entity_schemas] |
| BRIEF | true | false | false | [bundles, domains] |
| GUIDE | true | false | false | [entity_schemas] |
| FORECAST | true | false | false | [bundles] |
| DELEGATE | true | true | true | [bundles, packages, domains, entity_schemas] |

DELEGATE 的 `scopeRequired: true` 是唯一必填 scope 的模板，且 `acceptsScope` 自动视为 `true`（FR-007）。

## 9. contextMeta 字段取值

| 模板 | includeVersionAnchors | includeScopeApplied | includeTokenEstimate | includeSkippedEntities |
|------|----------------------|--------------------|--------------------|---------------------|
| DISCOVER | true | true | true | — |
| ORIENT | true | true | true | — |
| BRIEF | true | true | true | true |
| GUIDE | true | true | true | — |
| FORECAST | true | true | true | — |
| DELEGATE | true | true | true | true |

只有 `BRIEF` 和 `DELEGATE` 额外包含 `includeSkippedEntities: true`，用于标注 scope 外被裁剪的关联实体。
