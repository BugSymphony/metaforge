# Contract: opencode 命令与 Skill 交付契约

**版本**: 1.0.0 | **类型**: 本 BC 对外交付形态契约（命令/Skill）| **日期**: 2026-08-01

> 定义 opencode 平台上 metaforge 命令与 Skill 的结构与行为。命令/Skill 均为**声明式投影**（FR-CAP-002/FR-DLV），**不得直接出现 REST URL/HTTP 方法/curl**（FR-DLV-009），统一经 `.metaforge/scripts/metaforge-pro.sh` 脚本执行。

## 命令清单（metaforge.\*）

| commandId | 模板绑定 | 能力 | 必填参数 | 关键 flag | 关键输出 |
|---|---|---|---|---|---|
| `metaforge.catalog` | bundle-catalog | 平台发现 | — | `--page-size` | Bundle 列表 + 版本锚 + 主题域概要 |
| `metaforge.navigate` | navigate | 领域导航 | `--bundles` | `--subject-domain`、`--expand`、`--page-size`、`--cursor` | 层级树 + `has_more` + `entryStepFqn` |
| `metaforge.task-brief` | task-brief | 任务认知 | `--bundles` | `--depth`、`--archetype`、`--max-tokens`、`--format` | 10 视角简报（约束/流程/能力/决策） |
| `metaforge.step-guide` | step-guide | 实体即时指导 | `--entity-fqn` | `--bundles`、`--format` | 实体级 6 视角 + 约束级别 + 决策分支 + `adjacentContext` |
| `metaforge.subtask` | sub-task-brief | 子任务认知（收窄） | `--bundles` + `--entry-entity` | `--scope-mode` | 收窄简报 + `narrowedSchemaFqns` |
| `metaforge.guidance` | cognition-guidance | 自由视角组合 | — | `--perspectives`、`--bundles`、`--entity-fqn` | 任意视角子集聚合 |

## 命令文件结构（front-matter + 正文）

每个命令为 `.opencode/commands/metaforge.<name>.md`，格式与 speckit 系列一致（NFR-005）：

```markdown
---
description: <简体中文能力说明，供 Agent 判断何时使用>
handoffs:
  - label: <可交接动作>
    agent: <目标 agent>
    prompt: <提示词>
    send: true
---

## 用途
<该能力指导的问题与行为（spec §4.1）>

## 参数
<flag 清单、必填说明、示例>

## 执行方式
调用 `metaforge-pro.sh cognition execute <templateId> <flags>`（不出现 REST URL/curl）

## 输出说明
<json/prompt 双格式说明、版本锚、错误提示>
```

## Skill 定义（metaforge-\*）

每个 Skill 为 `.opencode/skills/metaforge-<name>/SKILL.md`，**自包含**（FR-DLV-007）：含参数说明、调用示例、输出格式说明，挂载后无需二次开发。

```markdown
---
name: metaforge-task-brief        # 须匹配目录名，小写字母数字+单连字符
description: <能力说明，1-1024 字符>
metadata:
  capability: task-cognition
  template: task-brief
---

## 用途
## 参数
## 调用示例
## 输出格式
```

- Skill **内部调用对应 slash 命令**，不接受自然语言直达服务端（FR-DLV-008）
- Skill 命名与对应命令一致（`metaforge.` 前缀 + 点号 → `metaforge-` 前缀 + 连字符）
- `bundle-scope` 不提供独立命令/Skill（服务端未注册，R3）；该能力经 `metaforge.guidance` 组合 `schema_inventory` + `instance_catalog` 视角达成

## 行为契约

- **必填校验**（FR-014）: task-brief/--bundles、step-guide/--entity-fqn、subtask/--bundles+--entry-entity、navigate/--bundles；缺失 → 用法提示并中止
- **FQN 推测**: 自然语言描述经 `cognition resolve` 流水线，唯一命中自动确认、多候选请选择、零命中终止（FR-NL）
- **输出**: json（默认）/prompt 双格式原样透传，含 `data_version_anchors` 版本锚（FR-OUT/FR-VER）
- **错误**: 全部映射为简体中文提示（FR-018）
- **无状态幂等**: 可安全重复执行（FR-015）
