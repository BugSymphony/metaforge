# Implementation Plan: 认知算子实现层

**Branch**: `001-cognition-dimensions` | **Date**: 2026-08-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from [spec.md](./spec.md)

## Summary

实现 `metaforge-agent-cognition-dimensions` Maven 模块——8 分类下共 25 个 `CognitionOperator` 的具体实现，通过 Spring Bean 注册由 `-core` 的 `OperatorRegistry` 运行时发现加载。模块仅编译依赖 `-api`，不依赖 `-core`，遵循运行时解耦。完成后注册到父 POM 并集成到 `-starter` 聚合器。同时发布算子清单导出契约（operator-catalog），供 `-templates` BC 在模板 YAML 中通过 `operatorId` 引用算子。

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: 仅 `metaforge-agent-cognition-api`（含 SPI 接口 + 4 个 Port 接口 + DimensionCategory 枚举 + CognitionQueryContext/CognitionResult 记录类），通过 `${revision}` 版本对齐；`metaforge-framework` 传递依赖（提供 PageRequest/PageResult）

**Storage**: N/A（无状态计算层，不持有数据主权）

**Testing**: JUnit 5 + Mockito（Mock 4 个 Port 接口）+ Spring Boot Test

**Target Platform**: Linux server, JVM 21, Spring Boot 3.x

**Project Type**: Maven 子模块（library, jar）——集成到 monorepo `metaforge-agent-cognition` 聚合器下

**Performance Goals**: 本体论 7 算子串行 ≤ 3s（不含上游 BC 网络延迟）；单算子 Port 调用完成即返回

**Constraints**:
- 严禁编译依赖 `metaforge-agent-cognition-core`
- 严禁直接注入上游 BC 的任意 Service
- 所有上游访问仅通过 4 个只读 Port 接口
- 8 分类封闭集合，不可新增分类
- MVP 阶段仅启动时加载，无热加载

**Scale/Scope**: 24 个算子实现类 + 1 个 AbstractCognitionOperator 抽象基类 + 每类算子单元测试

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 八维不可变分类体系 | MUST | 所有算子 `category()` 返回 DimensionCategory 枚举值，无新增分类 | PASS |
| II | 算子独立扩展 | MUST | 每个算子独立实现 CognitionOperator，无耦合、无依赖，通过 SPI 注册 | PASS |
| III | 统一上游访问通道 | MUST | 所有算子通过 @Autowired 注入 4 个 Port 接口访问上游，不直接注入 Service | PASS |
| IV | 只读与无主权 | MUST | 算子 execute() 仅执行只读查询，不持有数据存储主权，不保存会话/上下文/快照 | PASS |
| V | Scope 边界强制裁剪 | MUST | 每个算子按 context.scope() 裁剪查询范围，超界标注不输出 | PASS |
| VI | 失败不扩散 | SHOULD | 算子失败返回 CognitionResult.failure，不抛异常；optional 算子失败不终止调用 | PASS |
| VII | 运行时解耦加载 | SHOULD | `-dimensions` 不编译依赖 `-core`；算子通过 Spring @Component 注册，`-core` 运行时发现 | PASS |
| VIII | 渐进式探索 | SHOULD | ONTOLOGICAL 算子返回 lazy 节点（has_children + suggested_next_call） | PASS |
| IX | 确定性计算 | SHOULD | 所有算子为确定性规则计算，不依赖 LLM 或向量相似度 | PASS |

**Gate Verdict**: ALL PASS. No violations detected.

## Project Structure

### Documentation (this feature)

```text
specs/001-cognition-dimensions/
├── plan.md                   # This file
├── research.md               # Phase 0 output
├── data-model.md             # Phase 1 output
├── quickstart.md             # Phase 1 output
└── checklists/requirements.md
```

### Source Code (BC root)

```text
$BC_PATH = REPO_ROOT/metaforge-agent-cognition/metaforge-agent-cognition-dimensions/
pom.xml                       # Sub-module POM（仅依赖 -api）
src/
├── main/java/com/metaforge/agent/cognition/operator/
│   ├── common/
│   │   └── AbstractCognitionOperator.java    # 抽象基类：Port 注入 + scope 裁剪 + 异常模板
│   ├── ontological/                           # category: ONTOLOGICAL (7 算子)
│   │   ├── OntologicalBundleDiscoveryOperator.java
│   │   ├── OntologicalPackageExplorerOperator.java
│   │   ├── OntologicalEntitySchemaInventoryOperator.java
│   │   ├── OntologicalRelationSchemaInventoryOperator.java
│   │   ├── OntologicalDomainDrillDownOperator.java
│   │   ├── OntologicalInstanceCatalogOperator.java
│   │   └── OntologicalEntityProfileOperator.java
│   ├── structural/                            # category: STRUCTURAL (3 算子)
│   │   ├── StructuralDecompositionOperator.java
│   │   ├── StructuralBelongingOperator.java
│   │   └── StructuralDomainLocatorOperator.java
│   ├── relational/                            # category: RELATIONAL (3 算子)
│   │   ├── RelationalDirectLinkOperator.java
│   │   ├── RelationalNeighborhoodOperator.java
│   │   └── RelationalImpactTraceOperator.java
│   ├── procedural/                            # category: PROCEDURAL (3 算子)
│   │   ├── ProceduralFlowBlueprintOperator.java
│   │   ├── ProceduralAdjacentStepOperator.java
│   │   └── ProceduralDecisionBranchOperator.java
│   ├── deontic/                               # category: DEONTIC (3 算子)
│   │   ├── DeonticRuleListingOperator.java
│   │   ├── DeonticLevelClassifierOperator.java
│   │   └── DeonticConditionActionOperator.java
│   ├── capability/                            # category: CAPABILITY (3 算子)
│   │   ├── CapabilityToolDiscoveryOperator.java
│   │   ├── CapabilityCallMethodOperator.java
│   │   └── CapabilityProtocolDetailOperator.java
│   ├── epistemic/                             # category: EPISTEMIC (1 算子)
│   │   └── EpistemicFreshnessCheckOperator.java
│   └── governance/                            # category: GOVERNANCE (1 算子)
│       └── GovernanceScopeNarrowingOperator.java
└── test/java/com/metaforge/agent/cognition/operator/
    └── [每分类对应单元测试]
```

**Structure Decision**: Monorepo 多模块子 BC（Option 4），`$BC_PATH = REPO_ROOT/metaforge-agent-cognition/metaforge-agent-cognition-dimensions/`。按 8 分类分包组织算子实现类，`common/` 下放置抽象基类。

## Complexity Tracking

> No violations — all constitution principles pass.

## Module Registration

新增 `-dimensions` 模块需要修改以下工程文件：

### 1. metaforge-agent-cognition/pom.xml（父 POM）

在 `<modules>` 中追加：
```xml
<module>metaforge-agent-cognition-dimensions</module>
```

### 2. metaforge-agent-cognition-dimensions/pom.xml（新建）

```xml
<parent>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition</artifactId>
    <version>${revision}</version>
</parent>
<artifactId>metaforge-agent-cognition-dimensions</artifactId>
<dependencies>
    <dependency>
        <groupId>com.metaforge</groupId>
        <artifactId>metaforge-agent-cognition-api</artifactId>
    </dependency>
</dependencies>
```

### 3. metaforge-agent-cognition-starter/pom.xml（集成 Starter）

在 `<dependencies>` 中追加：
```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition-dimensions</artifactId>
</dependency>
```

### 4. metaforge-agent-cognition-core/pom.xml（运行时依赖）

`-core` 仅需在测试 scope 或 runtime scope 添加依赖（可选），或完全通过 Starter 传递。推荐方案：仅在 `-starter` 添加编译依赖，`-core` 运行时由 Spring 容器发现 Bean 自动聚合。

## BC Boundary Confirmation

- 所有算子代码在 `$BC_PATH/src/` 范围内，不引用其他 BC 内部实现
- 编译依赖仅 `metaforge-agent-cognition-api`（已发布的公共接口契约）
- 运行时上游访问仅通过 Port 接口（`-api` 定义，`-core` 注入适配器）
- 无跨 BC 直接代码调用
