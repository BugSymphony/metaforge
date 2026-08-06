---
description: 获取任务认知简报——Agent 在执行任务前理解任务的规则、流程、能力与决策分支（任务认知能力）
handoffs: 
  - label: 实体即时指导
    agent: metaforge.step-guide
    prompt: 我已获得任务简报，需要获取其中某个实体的即时操作指导
    send: true
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## 用途

本命令为 Agent 提供**任务认知**能力（spec §4.1 任务认知）：执行任务前，将自然语言目标转换为结构化查询，获取包含约束级别、流程步骤、可用能力与决策分支的任务认知简报，内容可直接注入 Agent 上下文使用（零解析开销）。

**指导的问题**: 这个任务是什么？有哪些规则、流程、能力与决策？
**指导的行为**: 执行前全局认知，规划执行路径。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--bundles <a,b,...>` | ✅ | Bundle FQN 列表（如 `order:1.0.0`）；缺失时命令中止并提示用法 |
| `--depth <L1\|L2\|L3>` | 否 | 认知深度（默认 L2） |
| `--archetype <type>` | 否 | 代理原型：execution/exploration/audit/orchestration（默认 execution） |
| `--max-tokens <n>` | 否 | Token 预算（默认 8000） |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json；prompt 为可直接注入的 Markdown） |
| `--expand <lazy\|all>` | 否 | 展开模式（默认 lazy） |
| `--json <body>` | 否 | 原样请求体，覆盖所有参数 |
| `--verbose` | 否 | 输出原始请求/响应与 X-Trace-Id（排障用） |

也可直接传入**自然语言任务描述**（如 `订单处理任务`）：命令先调用 `cognition resolve` 从服务端数据推测 Bundle FQN（多候选请选择、零命中终止并给原因），再执行任务简报——全程不向服务端发送自然语言。

## 执行方式

本命令**不直接访问服务端**（FR-DLV-009），仅调用脚本入口：

```bash
# 方式一：显式指定 Bundle（推荐，零推测开销）
.metaforge/scripts/metaforge-pro.sh cognition execute task-brief --bundles order:1.0.0

# 方式二：自然语言描述 → FQN 推测 → 任务简报
.metaforge/scripts/metaforge-pro.sh cognition resolve "订单处理任务" --type bundle
# 然后以推测出的 FQN 执行
.metaforge/scripts/metaforge-pro.sh cognition execute task-brief --bundles order:1.0.0

# 方式三：prompt 格式（Markdown，可直接注入 LLM 上下文）
.metaforge/scripts/metaforge-pro.sh cognition execute task-brief --bundles order:1.0.0 --format prompt
```

## 输出说明

- **json（默认）**: 原样透传服务端 `ApiResponse<T>`，含 `context_meta`（版本锚 `data_version_anchors`）与各视角章节（entity_profile / domain_location / composition_tree / relationship_graph / constraint_set / capability_catalog / flow_blueprint / decision_matrix / impact_trace / prerequisite_chain）
- **prompt**: Markdown 语义说明，可直接注入 Agent 上下文，与 json 语义一致
- **版本锚**: 输出含各 Bundle 已发布版本号 + 查询时间戳；对比前后两次可判断认知是否过期
- **错误提示**: 服务端错误码（34001~34006）与网络错误均映射为简体中文提示；多候选列候选、零命中终止并给原因与平台现有清单
