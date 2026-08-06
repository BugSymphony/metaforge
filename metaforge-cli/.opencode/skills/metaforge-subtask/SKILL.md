---
name: metaforge-subtask
description: 子任务认知——为子 Agent 生成收窄到其职责范围的认知简报（INHERITED 三层收窄 / PURE 纯净）。调用 /metaforge.subtask 命令或 .metaforge/scripts/metaforge-pro.sh。
metadata:
  capability: sub-task-cognition
  template: sub-task-brief
  command: metaforge.subtask
---

## 用途

为编排型父 Agent 提供**子任务认知（收窄）**能力：委派子任务时给子 Agent 一份收窄到其职责范围的认知简报，保证子 Agent 只接触该知道的内容（职责隔离执行）。输出含收窄后的约束/能力/决策与 `narrowed_schema_fqns`。内容可直接注入上下文（零解析开销）。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--bundles <a,b,...>` | ✅ | Bundle FQN 列表（如 `order:1.0.0`）；缺失时命令中止 |
| `--entry-entity <fqn>` | ✅ | 入口实体 FQN（如 `order:1.0.0.Task_OrderProcessing`）；缺失时命令中止 |
| `--scope-mode <mode>` | 否 | 收窄模式：INHERITED（默认）/ PURE |
| `--depth <L1\|L2\|L3>` | 否 | 认知深度（默认 L2） |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json） |
| `--verbose` | 否 | 排障：输出原始请求/响应与 X-Trace-Id |

## 调用示例

```bash
# 继承收窄（默认）
.metaforge/scripts/metaforge-pro.sh cognition execute sub-task-brief \
  --bundles order:1.0.0 --entry-entity order:1.0.0.Task_OrderProcessing --scope-mode INHERITED

# 纯净模式
.metaforge/scripts/metaforge-pro.sh cognition execute sub-task-brief \
  --bundles order:1.0.0 --entry-entity order:1.0.0.Task_OrderProcessing --scope-mode PURE
```

或直接调用命令：`/metaforge.subtask --bundles order:1.0.0 --entry-entity order:1.0.0.Task_OrderProcessing`

## 输出格式

- **json（默认）**: `ApiResponse<T>` 原样透传，INHERITED 返回三层收窄简报 + `narrowed_schema_fqns`；PURE 仅返回 entity_profile
- **版本锚**: 各 Bundle 已发布版本号 + 查询时间戳
- **错误**: 服务端错误码与网络错误 → 简体中文提示

## 约束

- 不直接访问服务端（FR-DLV-009）——REST 调用统一经 `.metaforge/scripts/` 脚本承载
- 无状态幂等，可安全重复执行
