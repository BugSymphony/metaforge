---
name: metaforge-step-guide
description: 获取实体即时指导——执行中获取指定实体/步骤的约束级别、能力协议、决策分支与影响范围，可直接注入 Agent 上下文。调用 /metaforge.step-guide 命令或 .metaforge/scripts/metaforge-pro.sh。
metadata:
  capability: entity-guidance
  template: step-guide
  command: metaforge.step-guide
---

## 用途

在执行某个具体步骤时，为 Agent 提供**实体即时指导**：即时获取该实体的约束级别（MANDATORY/RECOMMENDED/REFERENCE）、能力协议、决策分支、影响范围与相邻上下文（`adjacent_context`），支撑单步决策。内容可直接注入上下文使用（零解析开销）。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--entity-fqn <fqn>` | ✅ | 实体 FQN（如 `order:1.0.0.Step_CheckInventory`）；缺失时命令中止 |
| `--bundles <a,b,...>` | 否 | Bundle FQN 列表（实体 FQN 前缀可即席恢复） |
| `--depth <L1\|L2\|L3>` | 否 | 认知深度（默认 L2） |
| `--archetype <type>` | 否 | 代理原型（默认 execution） |
| `--max-tokens <n>` | 否 | Token 预算（默认 8000） |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json；prompt 为可注入 Markdown） |
| `--verbose` | 否 | 排障：输出原始请求/响应与 X-Trace-Id |

## 调用示例

```bash
# 显式指定实体
.metaforge/scripts/metaforge-pro.sh cognition execute step-guide \
  --entity-fqn order:1.0.0.Step_CheckInventory

# prompt 格式（可直接注入 LLM 上下文）
.metaforge/scripts/metaforge-pro.sh cognition execute step-guide \
  --entity-fqn order:1.0.0.Step_CheckInventory --format prompt
```

或直接调用命令：`/metaforge.step-guide --entity-fqn order:1.0.0.Step_CheckInventory`

## 输出格式

- **json（默认）**: `ApiResponse<T>` 原样透传，实体级 6 视角章节（entity_profile / constraint_set / capability_catalog / decision_matrix / impact_trace / relationship_graph）+ `adjacent_context`
- **prompt**: Markdown 语义说明，可直接注入 Agent 上下文，与 json 语义一致
- **约束级别**: MANDATORY / RECOMMENDED / REFERENCE
- **视角跳过提示**: Bundle 级视角在实体级被跳过时以提示展示（FR-019）
- **归属校验失败**: 34004 → 中文提示 + 候选列表，终止执行
- **版本锚**: 各 Bundle 已发布版本号 + 查询时间戳

## 约束

- 不直接访问服务端（FR-DLV-009）——REST 调用统一经 `.metaforge/scripts/` 脚本承载
- 不向服务端发送自然语言文本（NL→结构化转换在 CLI 完成）
- 无状态幂等，可安全重复执行
