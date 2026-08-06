---
name: metaforge-task-brief
description: 获取任务认知简报——执行任务前理解任务的规则、流程、能力与决策分支，可直接注入 Agent 上下文。调用 /metaforge.task-brief 命令或 .metaforge/scripts/metaforge-pro.sh。
metadata:
  capability: task-cognition
  template: task-brief
  command: metaforge.task-brief
---

## 用途

在执行任务前，为 Agent 提供**任务认知**：将自然语言目标或显式 Bundle 转换为结构化查询，获取包含约束级别、流程步骤、可用能力与决策分支的任务认知简报（10 视角），内容可直接注入上下文使用（零解析开销）。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--bundles <a,b,...>` | ✅ | Bundle FQN 列表（如 `order:1.0.0`）；缺失时命令中止 |
| `--depth <L1\|L2\|L3>` | 否 | 认知深度（默认 L2） |
| `--archetype <type>` | 否 | 代理原型（默认 execution） |
| `--max-tokens <n>` | 否 | Token 预算（默认 8000） |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json；prompt 为可注入 Markdown） |
| `--verbose` | 否 | 排障：输出原始请求/响应与 X-Trace-Id |

## 调用示例

```bash
# 显式指定 Bundle
.metaforge/scripts/metaforge-pro.sh cognition execute task-brief --bundles order:1.0.0

# 自然语言 → FQN 推测 → 简报（多候选请选择、零命中终止）
.metaforge/scripts/metaforge-pro.sh cognition resolve "订单处理任务" --type bundle
.metaforge/scripts/metaforge-pro.sh cognition execute task-brief --bundles order:1.0.0

# prompt 格式（可直接注入 LLM 上下文）
.metaforge/scripts/metaforge-pro.sh cognition execute task-brief --bundles order:1.0.0 --format prompt
```

或直接调用命令：`/metaforge.task-brief --bundles order:1.0.0`

## 输出格式

- **json（默认）**: `ApiResponse<T>` 原样透传，含 `context_meta.data_version_anchors`（版本锚）与视角章节
- **prompt**: Markdown 语义说明，可直接注入 Agent 上下文，与 json 语义一致
- **版本锚**: 各 Bundle 已发布版本号 + 查询时间戳；对比前后两次可判断认知是否过期
- **错误**: 34001~34006 与网络错误 → 简体中文提示；多候选列候选、零命中终止并给原因

## 约束

- 不直接访问服务端（FR-DLV-009）——REST 调用统一经 `.metaforge/scripts/` 脚本承载
- 不向服务端发送自然语言文本（NL→结构化转换在 CLI 完成）
- 无状态幂等，可安全重复执行
