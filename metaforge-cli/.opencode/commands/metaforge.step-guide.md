---
description: 获取实体即时指导——Agent 执行中获取指定实体/步骤的约束级别、能力协议、决策分支与影响范围（实体即时指导能力）
handoffs: 
  - label: 任务认知简报
    agent: metaforge.task-brief
    prompt: 我已获得实体指导，需要获取所属任务的完整认知简报
    send: true
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## 用途

本命令为 Agent 提供**实体即时指导**能力（spec §4.1 实体即时指导）：执行到某个具体步骤时，即时获取该实体的约束级别（MANDATORY/RECOMMENDED/REFERENCE）、能力协议、决策分支、影响范围与相邻上下文（`adjacent_context`），支撑单步决策。输出可直接注入 Agent 上下文使用（零解析开销）。

**指导的问题**: 这个实体/步骤该怎么做？有哪些约束与边界？
**指导的行为**: 执行中单步决策，规避越界。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--entity-fqn <fqn>` | ✅ | 实体 FQN（如 `order:1.0.0.Step_CheckInventory`）；缺失时命令中止并提示用法 |
| `--bundles <a,b,...>` | 否 | Bundle FQN 列表（实体 FQN 前缀可即席恢复，一般可省略） |
| `--depth <L1\|L2\|L3>` | 否 | 认知深度（默认 L2） |
| `--archetype <type>` | 否 | 代理原型（默认 execution） |
| `--max-tokens <n>` | 否 | Token 预算（默认 8000） |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json；prompt 为可注入 Markdown） |
| `--expand <lazy\|all>` | 否 | 展开模式（默认 lazy） |
| `--json <body>` | 否 | 原样请求体，覆盖所有参数 |
| `--verbose` | 否 | 输出原始请求/响应与 X-Trace-Id |

## 执行方式

本命令**不直接访问服务端**（FR-DLV-009），仅调用脚本入口：

```bash
# 显式指定实体（推荐）
.metaforge/scripts/metaforge-pro.sh cognition execute step-guide --entity-fqn order:1.0.0.Step_CheckInventory

# prompt 格式（可直接注入 LLM 上下文）
.metaforge/scripts/metaforge-pro.sh cognition execute step-guide \
  --entity-fqn order:1.0.0.Step_CheckInventory --format prompt

# 实体 FQN 归属校验失败时（34004），命令终止并展示候选列表
.metaforge/scripts/metaforge-pro.sh cognition execute step-guide --entity-fqn ghost:1.0.0.Step_X
```

## 输出说明

- **json（默认）**: 原样透传 `ApiResponse<T>`，实体级 6 视角章节（entity_profile / constraint_set / capability_catalog / decision_matrix / impact_trace / relationship_graph），含约束级别、能力协议、决策分支、影响层与 `adjacent_context`
- **prompt**: Markdown 语义说明，可直接注入 Agent 上下文，与 json 语义一致
- **视角跳过提示**: Bundle 级视角（如 domain_navigation）在实体级上下文被跳过时，以提示形式展示（FR-019），避免误认为数据缺失
- **归属校验**: 实体 FQN 不属于任何已发布 Bundle → 34004 中文提示 + 候选列表，终止执行
- **版本锚**: 输出含各 Bundle 已发布版本号 + 查询时间戳
