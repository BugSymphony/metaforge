---
description: 子任务认知——为子 Agent 生成收窄到其职责范围的认知简报（INHERITED 三层收窄 / PURE 纯净）
handoffs: 
  - label: 实体即时指导
    agent: metaforge.step-guide
    prompt: 我已获得子任务收窄简报，需要针对其中某个实体获取即时指导
    send: true
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## 用途

本命令为编排型父 Agent 提供**子任务认知（收窄）**能力（spec §4.1 子任务认知）：委派子任务时，给子 Agent 一份收窄到其职责范围的认知简报，保证子 Agent 只接触该知道的内容（职责隔离执行）。输出含收窄后的约束/能力/决策与 `narrowed_schema_fqns`（收窄可见范围）。可直接注入 Agent 上下文（零解析开销）。

**指导的问题**: 我（子 Agent）职责范围内该知道什么？
**指导的行为**: 子任务隔离执行。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--bundles <a,b,...>` | ✅ | Bundle FQN 列表（如 `order:1.0.0`）；缺失时命令中止 |
| `--entry-entity <fqn>` | ✅ | 入口实体 FQN（如 `order:1.0.0.Task_OrderProcessing`）；缺失时命令中止 |
| `--scope-mode <mode>` | 否 | 收窄模式：INHERITED（继承三层收窄，默认）/ PURE（纯净，仅返回 entity_profile） |
| `--depth <L1\|L2\|L3>` | 否 | 认知深度（默认 L2） |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json） |
| `--verbose` | 否 | 输出原始请求/响应与 X-Trace-Id |

## 执行方式

本命令**不直接访问服务端**（FR-DLV-009），仅调用脚本入口：

```bash
# 继承收窄（默认）
.metaforge/scripts/metaforge-pro.sh cognition execute sub-task-brief \
  --bundles order:1.0.0 --entry-entity order:1.0.0.Task_OrderProcessing --scope-mode INHERITED

# 纯净模式（仅实体自身维度）
.metaforge/scripts/metaforge-pro.sh cognition execute sub-task-brief \
  --bundles order:1.0.0 --entry-entity order:1.0.0.Task_OrderProcessing --scope-mode PURE
```

## 输出说明

- **json（默认）**: 原样透传 `ApiResponse<T>`，INHERITED 返回三层收窄简报 + `narrowed_schema_fqns`；PURE 仅返回 entity_profile
- **版本锚**: 各 Bundle 已发布版本号 + 查询时间戳
- **错误提示**: 服务端错误码与网络错误映射为简体中文提示
