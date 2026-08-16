---
id: agent-cognition.spi
protocol: SPI
version: 1.0.0
owner: metaforge-agent-cognition
description: 认知引擎 SPI 扩展契约。下游 BC 通过实现 CognitionOperator 注册认知算子、实现 OutputFormatter 扩展输出格式，无需修改引擎核心代码。
type: business
---

# SPI Contract: metaforge-agent-cognition

**Protocol**: SPI（服务提供者接口，进程内 Java Interface 扩展）
**Module**: `metaforge-agent-cognition-api`
**Version**: 1.0.0

> 下游 BC 通过 Maven 依赖 `metaforge-agent-cognition-api` 模块，实现以下 SPI 接口并经 Spring 容器自动发现注册。扩展遵循"声明式扩展铁律"——新增能力无需修改引擎核心代码。

---

## Maven 依赖

实现方（如 `-dimensions` 模块或下游 BC）在 `pom.xml` 中声明对 `metaforge-agent-cognition-api` 的依赖，以编译引用 SPI 接口与数据结构：

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition-api</artifactId>
    <version>${revision}</version>
</dependency>
```

> 严禁依赖 `metaforge-agent-cognition-core` 模块。`-core` 通过 Spring 容器在运行时发现 SPI 实现 Bean，实现模块与 `-core` 之间无编译期依赖。

---

## Interface Definition

### CognitionOperator

**职责**: 认知算子 SPI——认知能力的实现单元。每个认知算子 = 一个实现类（Spring Bean），类上通过 `category` 字段/注解声明所属分类（8 分类枚举封闭集合之一，如 `RELATIONAL`），由下游 BC 经 SPI 挂载。

```java
package com.metaforge.agent.cognition.api.spi;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;

public interface CognitionOperator {

    String operatorId();

    DimensionCategory category();

    CognitionResult execute(CognitionQueryContext context);
}
```

#### 输入定义

| 方法 | 输入参数 | 类型 | 描述 |
|------|---------|------|------|
| execute | context | CognitionQueryContext | 认知算子查询上下文 record——含 templateId、operatorId、category、scope、bundleFqns、entityFqn、templateParams、agentArchetype、cognitionDepth、cursor、pageSize；算子内部按 scope 裁剪查询范围 |
| operatorId | — | — | 无入参 |
| category | — | — | 无入参 |

#### 输出定义

| 方法 | 返回类型 | 描述 |
|------|---------|------|
| execute | CognitionResult | 算子执行结果对象——含 operatorId（算子标识）、category（算子所属分类）、data（结果数据）、success（是否成功）、error（失败时的错误信息） |
| operatorId | String | 算子唯一标识（如 `ontological.bundle-discovery`、`relational.direct-link`） |
| category | DimensionCategory | 算子所属 8 分类之一（如 RELATIONAL、STRUCTURAL） |

### OutputFormatter

**职责**: 输出格式化 SPI。`json`/`prompt` 各为一个实现类（`JsonOutputFormatter`/`PromptOutputFormatter`），经 Spring 注入由格式化注册表按 format 分发。新增格式 = 实现接口 + 注册，引擎核心零改动。

```java
package com.metaforge.agent.cognition.api.spi;

import com.metaforge.agent.cognition.api.dto.response.ContextMeta;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.enums.OutputFormat;

import java.util.List;
import java.util.Map;

public interface OutputFormatter {

    boolean supports(OutputFormat format);

    Object format(String templateId, Map<DimensionCategory, List<CognitionResult>> results, ContextMeta contextMeta);
}
```

#### 输入定义

| 方法 | 输入参数 | 类型 | 描述 |
|------|---------|------|------|
| supports | format | OutputFormat | 输出格式枚举（JSON/PROMPT）——格式分发键 |
| format | templateId | String | 执行的模板唯一标识 |
| format | results | Map<DimensionCategory, List\<CognitionResult\>> | 按认知分类分组的算子执行结果集合（key = 8 分类之一） |
| format | contextMeta | ContextMeta | 上下文元信息（版本锚、scope 应用、Token 估算、生成时间、跳过实体列表、裁剪标记） |

#### 输出定义

| 方法 | 返回类型 | 描述 |
|------|---------|------|
| supports | boolean | 该实现类是否支持指定输出格式 |
| format | Object | 格式化后的输出内容——json 实现返回结构化 JSON，prompt 实现返回 Markdown 文本；语义内容完全等价 |

---

## 输入/输出结构

### CognitionQueryContext

**职责**: 认知算子查询上下文 record——`CognitionOperator.execute()` 的入参载体。包含 templateId、operatorId、category、scope、bundleFqns、entityFqn、templateParams、agentArchetype、cognitionDepth、cursor、pageSize。

```java
package com.metaforge.agent.cognition.api.spi;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;

import java.util.List;
import java.util.Map;

public record CognitionQueryContext(
        String templateId,
        String operatorId,
        DimensionCategory category,
        Scope scope,
        List<String> bundleFqns,
        String entityFqn,
        Map<String, Object> templateParams,
        AgentArchetype agentArchetype,
        CognitionDepth cognitionDepth,
        String cursor,
        int pageSize
) {}
```

**字段定义**:

| 字段 | 类型 | 描述 |
|------|------|------|
| templateId | String | 模板唯一标识（路由来源） |
| operatorId | String | 算子唯一标识 |
| category | DimensionCategory | 算子所属 8 分类 |
| scope | Scope | 认知边界五字段（bundles/packages/domainGroups/domains/entitySchemas）——算子内部按此裁剪查询范围，越界内容不输出 |
| bundleFqns | List\<String\> | Bundle FQN 列表（scope 解析产物） |
| entityFqn | String | 查询目标实体 FQN |
| templateParams | Map\<String, Object\> | 模板专用参数，键值对由模板 inputSchema 定义 |
| agentArchetype | AgentArchetype | Agent 原型（EXECUTION/EXPLORATION/AUDIT/ORCHESTRATION） |
| cognitionDepth | CognitionDepth | 认知深度（L1/L2/L3） |
| cursor | String | 分页游标 |
| pageSize | int | 每页大小 |

### CognitionResult

**职责**: 认知算子执行结果对象——由各认知算子返回。包含 operatorId（算子标识）、category（算子所属分类）、data（结果数据）、success（是否成功）、error（失败时的错误信息）。

```java
package com.metaforge.agent.cognition.api.spi;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;

public record CognitionResult(
        String operatorId,
        DimensionCategory category,
        Object data,
        boolean success,
        String error
) {
    public static CognitionResult success(String operatorId, DimensionCategory category, Object data) {
        return new CognitionResult(operatorId, category, data, true, null);
    }

    public static CognitionResult failure(String operatorId, DimensionCategory category, String error) {
        return new CognitionResult(operatorId, category, null, false, error);
    }
}
```

**字段定义**:

| 字段 | 类型 | 描述 |
|------|------|------|
| operatorId | String | 算子标识 |
| category | DimensionCategory | 算子所属分类 |
| data | Object | 结果数据（算子产出内容，随算子语义不同而变） |
| success | boolean | 是否成功执行 |
| error | String | 失败时的错误信息（success=false 时携带） |

---

## 枚举定义（API 契约层）

### DimensionCategory

认知分类枚举 —— 8 值封闭集合，不可通过配置扩展；认知算子类上的 `category` 字段必须归入该枚举。

| 枚举值 | displayName | layer |
|--------|-------------|-------|
| ONTOLOGICAL | 本体论 | object |
| STRUCTURAL | 结构论 | object |
| RELATIONAL | 关系论 | object |
| PROCEDURAL | 流程论 | object |
| DEONTIC | 约束论 | action |
| CAPABILITY | 能力论 | action |
| EPISTEMIC | 认知论 | meta |
| GOVERNANCE | 治理 | meta |

### AgentArchetype

Agent 原型枚举 —— 4 值封闭集合，是权限/能力维度而非优先级。

| 枚举值 | 描述 |
|--------|------|
| EXECUTION | 执行型 Agent |
| EXPLORATION | 探索型 Agent |
| AUDIT | 审计型 Agent |
| ORCHESTRATION | 编排型 Agent |

### CognitionDepth

认知深度枚举 —— 3 值。

| 枚举值 | 保留比例 (required=false) |
|--------|--------------------------|
| L1 | 1/3（不低于 min-keep） |
| L2 | 2/3（不低于 min-keep） |
| L3 | 全量（不裁剪） |

### OutputFormat

输出格式枚举 —— 作为 `OutputFormatter.supports()` 的分发键与 `format` 请求参数的取值来源。

| 枚举值 | Source |
|--------|--------|
| JSON | 内置 |
| PROMPT | 内置 |

---

## SPI 注册与发现机制

SPI 实现不通过任何配置文件注册——依赖 Spring 容器自动发现与注入：

1. **算子注册（OperatorRegistry）**: 实现类声明为 Spring Bean（`@Component`），类上通过 `category` 字段/注解声明归属分类；系统启动时经 `@Autowired List<CognitionOperator>` 收集所有算子 Bean，校验每个算子类声明分类（`category` 字段）的合法性与一致性后，按 `operatorId()` 注册到算子注册表。
2. **输出格式化注册（FormatterRegistry）**: `JsonOutputFormatter`/`PromptOutputFormatter` 各为 Spring Bean，经 `@Autowired List<OutputFormatter>` 收集后由格式化注册表按 `supports(OutputFormat)` 分发。
3. **模板引用**: 算子注册完成后，在模板 YAML 的 `operators` 中通过 `operatorId` 引用（如 `ontological.bundle-discovery`），运行时按分类分组执行与裁剪。

示例：

```java
@Component
public class RelationalDirectOperator implements CognitionOperator {

    private final DimensionCategory category = DimensionCategory.RELATIONAL;

    @Override
    public String operatorId() {
        return "relational.direct-link";
    }

    @Override
    public DimensionCategory category() {
        return category;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        // 算子内部按 context.scope() 裁剪查询范围，越界内容不输出
        return CognitionResult.success(operatorId(), category(), /* 结果数据 */ null);
    }
}
```

---

## 相关配置（application-agent-cognition.yml）

运行时行为参数统一读取 `application-agent-cognition.yml`（或 `application.yml` 中 `metaforge.agent-cognition` 段落），配置前缀 `metaforge.agent-cognition`，支持环境特定覆盖（如 `application-cognition-dev.yml`），零配置可用（所有参数具备合理默认值）。算子相关配置项：

| 配置项 | 默认值 | 描述 |
|--------|--------|------|
| `metaforge.agent-cognition.timeouts.operator-execute-default-ms` | 10000 | 单个算子默认执行超时（毫秒） |
| `metaforge.agent-cognition.depth.trim-ratio-l1` | 0.33 | L1 深度非强制算子保留比例（不低于 min-keep） |
| `metaforge.agent-cognition.depth.trim-ratio-l2` | 0.67 | L2 深度非强制算子保留比例（不低于 min-keep） |
| `metaforge.agent-cognition.depth.min-keep` | 3 | 深度裁剪最小保留数（非强制算子） |
| `metaforge.agent-cognition.templates.classpath-location` | `classpath:cognition/templates/` | 模板 YAML 扫描路径 |
| `metaforge.agent-cognition.templates.external-location` | 空 | 外部模板目录（`file:` 协议，优先级高于 classpath） |
| `metaforge.agent-cognition.templates.hot-reload.enabled` | false | 外部模板热加载开关 |
| `metaforge.agent-cognition.templates.hot-reload.poll-interval-ms` | 5000 | 热加载文件轮询间隔（毫秒） |
| `metaforge.agent-cognition.version-anchor.bundle-resolve-strategy` | `LATEST_PUBLISHED` | Bundle 版本锚解析策略 |

> 算子 SPI 实现本身无需在配置文件中声明——实现类即为 Spring Bean，由容器自动发现注册；配置文件仅控制算子的执行超时与深度裁剪等运行时参数。

---

## 特殊约束（Special Constraints）

- **算子分类合法性校验**: 系统启动时通过 Spring 容器发现所有 `CognitionOperator` Bean，校验每个算子类声明分类（`category` 字段）的合法性与一致性后注册到算子注册表，供模板 operators 引用；分类缺失或非法的算子 WARN 日志记录且不注册，不影响其他已注册算子。
- **8 分类封闭集合**: `DimensionCategory` 为封闭枚举，不可通过配置扩展；新增能力只能在既有分类下新增算子。
- **MVP 阶段仅启动时加载**: 算子在启动时一次性注册完成，后续新增算子需重启系统；热加载留待 P1 迭代。
- **新增算子 = 实现接口 + 模板引用**: 实现 `CognitionOperator`（类上 category 字段声明归属分类）+ 在模板文件 operators 中引用 operatorId，无需修改引擎核心代码。
- **新增输出格式**: 实现 `OutputFormatter` 接口（含 `supports(OutputFormat)` 与 `format(...)`）+ 注册，引擎核心零改动。
- **算子执行错误语义**: required=true 算子执行失败返回 `OPERATOR_EXECUTION_ERROR`(34009)、超时返回 `OPERATOR_TIMEOUT`(34008)；完整错误码定义见 `agent-cognition.application-service` 与 `agent-cognition.rest-api` 契约。
