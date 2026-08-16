# Implementation Plan: 认知模板配置层 (cognition-templates)

**Branch**: `001-cognition-templates` | **Date**: 2026-08-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-cognition-templates/spec.md`

## Summary

将 `metaforge-agent-cognition-templates` 从纯文档目录升级为正式的 Maven 子模块（packaging: jar，仅 resources），包含 6 个内置认知模板 YAML 文件。模块被注册到父 POM 的 `<modules>` 聚合列表，并通过 starter 聚合模块以运行时依赖方式注入 classpath，使引擎核心的 TemplateScanner 可在启动时发现并注册模板。模板文件中的 `operatorId` 引用源自上游 `agent-cognition-dimensions` 的算子清单契约（operator-catalog-contract，24 个算子，8 分类）。本模块不含任何 Java 代码，完全遵循 BC 宪法"纯声明配置层"定位。

## Technical Context

**Language/Version**: N/A（纯 YAML 配置，不含 Java 代码）

**Primary Dependencies**: Maven 父 POM `metaforge-agent-cognition`（版本继承 `${revision}`）；运行时由 `metaforge-agent-cognition-starter` 传递引入 `metaforge-agent-cognition-dimensions`（算子 classpath 依赖）

**Storage**: 文件系统 — YAML 文件存放于 `src/main/resources/cognition/templates/*.yml`

**Testing**: 模板 YAML 结构合法性校验（YAML 语法、必填字段完整性、operatorId 有效性、archetype 闭合性、scopeBehavior 字段一致性），由 `metaforge-agent-cognition-core` 的 `TemplateYamlParser` 执行校验；本模块自身无测试代码（纯配置）

**Target Platform**: Java 21 / Maven — 以 jar（仅 resources）形式被 `metaforge-boot` 装配层引入 classpath

**Project Type**: Monorepo 多模块子 BC — 纯资源配置 jar，Option 4 (Monorepo Multi-module Sub-BC) + 特殊变体（无 Java 源码）

**Performance Goals**: 6 个模板文件总大小 < 30KB，类路径扫描 + YAML 解析 + 校验总耗时 < 500ms

**Constraints**: 不含 Java 代码（packaging: jar，仅 resources）；不编译依赖 `-core` 或 `-dimensions`；模板文件编码 UTF-8；扩展名仅 `.yml`

**Scale/Scope**: MVP 阶段 6 个内置模板，每个模板声明 2–10 个算子；后续按需新增模板文件

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### BC-Specific Principles

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 纯声明配置层定位 | MUST | 本模块不含任何 Java 源码，仅包含 `src/main/resources/` 下的 YAML 配置文件和 `pom.xml`（packaging: jar，仅 resources）。全部产物为供引擎消费的配置数据。 | PASS |
| II | 场景定义自包含 | MUST | 每个模板 YAML 文件完整声明 templateId、operators、inputSchema、scopeBehavior、outputSchema、contextMeta。任一要素缺失，校验失败，不得注册。 | PASS |
| III | 能力引用合法性 | MUST | 模板 `operatorId` 仅引用 operator-catalog-contract 中已注册的 24 个算子 ID。引用不存在算子 → 校验失败，不影响其他已注册模板。 | PASS |
| IV | 配置即数据解耦 | MUST | 模板模块不编译依赖 `-core`、`-dimensions`。仅通过 Maven 父 POM 建立依赖关系；运行时由 `-starter` 传递引入 classpath。模块演进不要求修改其他模块。 | PASS |
| V | 增量式模板演进 | SHOULD | 新增模板仅需在本模块新增 YAML 文件。templateId 全局唯一（大写+数字+下划线）。文件名遵循 `{小写templateId}-template.yml` 规范。 | PASS |
| VI | 边界行为显式声明 | SHOULD | 每个模板显式声明 scopeBehavior（acceptsScope / scopeRequired / producesUpdatedScope / scopeFields），无隐式缺省行为。 | PASS |
| VII | 只声明不实现 | MUST | 模板 YAML 仅声明消费场景"是什么"，不包含智能推断、可执行逻辑或语义相似度语义。全部为确定性配置。 | PASS |
| VIII | 生命周期与版本治理 | SHOULD | 每个模板记录 `version` 字段；通过 `enabled` 字段支持启停控制。停用模板保留注册信息但不被路由。 | PASS |

### Global Constitution (Inherited MUST Principles — Non-overridable)

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 元模型唯一权威性 | MUST | 模板 scopeFields 引用的 bundles/packages/entity_schemas 概念来源于 `metamodel-governance` 定义的元模型结构，不自行定义语义。 | PASS |
| II | 显式导入边界管控 | MUST | N/A — 本 BC 为纯配置层，不直接执行导入授权逻辑。授权由 `agent-consumption` / `-core` 的 ScopeResolutionService 执行。 | PASS |
| III | 全链路权限过滤 | MUST | N/A — 本 BC 为配置数据提供方，不参与运行时权限过滤。过滤由 `-core` 的 ArchetypeFilterService 基于模板声明的 archetype 白名单执行。 | PASS |
| IV | 版本统一收敛 | MUST | 模板自身 `version` 字段遵循语义化版本。不直接操作 Bundle 版本，版本锚通过 contextMeta 传递给引擎。 | PASS |
| IX | 纯元数据边界坚守 | MUST | 本模块仅存储模板配置数据（YAML），不触碰任何业务元数据或业务交易数据。 | PASS |
| X | 文档中文规范 | MUST | 模板 YAML 中的 `templateName` 和 `description` 使用简体中文；YAML 字段名和 operatorId 保留英文。 | PASS |

**Gate Verdict**: ALL principles **PASS**. No violations.

## Foundation Check

*No foundation contracts imported for this BC. Skipping.*

## Project Structure

### Documentation (this feature)

```text
specs/001-cognition-templates/
├── spec.md                   # Feature specification
├── plan.md                   # This file
├── research.md               # Phase 0 output
├── data-model.md             # Phase 1 output
├── quickstart.md             # Phase 1 output
├── contracts/                # Phase 1: interface contract definitions
│   └── template-contract.md  # 认知模板契约——定义模板 YAML 文件格式规范
└── checklists/
    └── requirements.md       # Spec quality checklist
```

### Source Code (BC root)

```text
metaforge-agent-cognition-templates/
├── pom.xml                              # Maven 模块定义（packaging: jar, 仅 resources）
│                                        # 依赖：仅继承 metaforge-agent-cognition 父 POM
├── context/
│   ├── constitution.md                  # BC 宪法
│   ├── feature.json                     # 当前特性目录指针
│   ├── contracts/                       # BC 对外导出契约（本 BC 为纯配置层，暂不导出）
│   └── upstream-contracts/
│       └── agent-cognition-dimensions/
│           └── operator-catalog-contract.md  # 导入的算子清单契约
├── specs/                               # 特性规格与计划文档
└── src/
    └── main/
        └── resources/
            └── cognition/
                └── templates/
                    ├── discover-template.yml
                    ├── orient-template.yml
                    ├── brief-template.yml
                    ├── guide-template.yml
                    ├── forecast-template.yml
                    └── delegate-template.yml
```

**Structure Decision**:
- **Selected structure type**: Monorepo 多模块子 BC (Option 4 变体 — 无 Java 源码)
- **BC relative path to REPO_ROOT**: `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-templates/`
- **Real directory layout**: 仅 `pom.xml` + `src/main/resources/cognition/templates/*.yml`，无 `src/main/java/` 和 `src/test/` 目录
- **Selection rationale**: 与 `metaforge-agent-cognition` 的其他子模块（api/core/dimensions/starter）保持一致的 Maven 多模块结构，便于统一构建与版本管理
- **Internal architecture note**: 纯配置层，无分层架构。所有 YAML 文件按 `{templateId}-template.yml` 命名规范平铺在 `cognition/templates/` 下
- **Cross-BC dependency status**: 
  - 上游依赖 (import): `agent-cognition-dimensions`（算子清单契约，供模板编写时引用 operatorId）
  - 下游消费者: `metaforge-agent-cognition-core`（通过 TemplateScanner 在运行时扫描本模块的 classpath 资源）
  - 无编译依赖: 本模块不依赖任何其他模块的编译产物

**BC Boundary Confirmation**:
- 本 BC 全部产物限制在 `$BC_PATH` 范围内，不引用其他 BC 的内部实现代码
- 算子引用（operatorId）通过上游契约 `operator-catalog-contract.md` 完成，无跨 BC 直接代码调用
- 本 BC 暂不导出公共契约（纯配置层，下游通过 classpath 资源路径约定消费）

**Parent POM Integration**: 在 `metaforge-agent-cognition/pom.xml` 的 `<modules>` 中添加：
```xml
<module>metaforge-agent-cognition-templates</module>
```

**Starter POM Integration**: 在 `metaforge-agent-cognition-starter/pom.xml` 的 `<dependencies>` 中添加：
```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition-templates</artifactId>
</dependency>
```

## Complexity Tracking

> No violations detected. All constitution checks PASS.
