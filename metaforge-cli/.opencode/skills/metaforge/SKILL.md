---
name: metaforge
description: 使用 MetaForge 认知服务为普通用户提供业务认知——判断用户意图选择认知场景、解析实体 FQN、查询认知简报并把结果翻译成自然语言答案
metadata:
  capability: cognition-triage
  audience: agent
---

# MetaForge 认知服务

## What I do

把普通用户的自然语言诉求（"了解库存任务""改这个步骤影响什么"）转换为 MetaForge 认知服务的结构化查询，再把结构化结果翻译回自然语言答案。你不暴露任何 FQN/Bundle/算子等技术术语给用户。

两个工具：

- `metaforge_resolve`：自然语言/名称 → 精确实体 FQN（数据驱动，绝不臆造）
- `metaforge_cognition`：按模板执行认知查询（6 模板 + 算子 + 锚点）

## When to use me

用户想了解业务/平台时使用，典型触发词：**了解、看看、流程、步骤、规则、能力、影响、风险、委派、交接、平台有哪些、怎么执行**。纯代码读写/改 bug 等不涉及业务认知的请求不要用我。

## 工作流程

```
① 意图分诊  →  判断用户要哪类认知（哪个模板 + 深度 + 算子范围）
② FQN 解析  →  metaforge_resolve 把用户说的实体名解析成 FQN（唯一/多候选/零命中）
③ 认知查询  →  metaforge_cognition 按模板 + FQN + 算子查询
④ 结果翻译  →  把结构化 dimensions 翻译成自然语言答案
```

---

## ① 意图分诊（选哪个模板）

按用户诉求信号归类到 6 个模板之一：

| 模板 | 用户会怎么说（信号） | 回答什么 |
|------|---------------------|---------|
| BRIEF | "了解 X 的完整情况 / 有什么流程 / 什么规则 / 需要什么能力 / X 是什么" | 实体/任务全景 |
| GUIDE | "执行 X 这一步该怎么做 / 要注意什么 / 有什么约束" | 单步执行指导 |
| FORECAST | "改 / 删 X 有什么影响 / 风险多大 / 谁依赖它 / 要回归哪些" | 变更影响链路 |
| DELEGATE | "把 X 委派 / 交给别人做，它该知道什么 / 边界是什么" | 子任务认知边界 |
| ORIENT | "平台有哪些域 / 某域下有什么任务 / Agent / 定位业务" | 领域定位下钻 |
| DISCOVER | "平台有哪些类型 / 关系 / 包 / 库" | 元模型盘点 |

**深度推定**（传给 cognition 的 depth）：
- "大概 / 简要 / 看下" → `L1`
- 一般了解 → `L2`（默认）
- "全面 / 详细 / 完整评估" → `L3`

**原型推定**（archetype）：
- 了解/排查/评估 → `execution`（默认）
- 盘点/浏览/探索 → `exploration`

**复合意图**（用户一个请求跨多个模板）：
识别"主诉求 + 延伸诉求"，**先服务主诉求**，延伸诉求作为"建议下一步"提示给用户，**不自动串联执行**。
例："了解库存任务并评估改动它的影响" → 主诉求 BRIEF（先给全景），延伸 FORECAST → 回答后提示"需要评估这个任务变更的影响吗？"。

---

## ② FQN 解析（metaforge_resolve）

认知查询需要精确 FQN。当用户只说名称/关键词时，先调 `metaforge_resolve`：

- **唯一命中**：直接用返回的 fqn 继续查询
- **多候选**：把候选列给用户（中文名称），请用户选，不要擅自确定
- **零命中**：把工具返回的"平台现有清单"展示给用户，请用户提供更精确描述

**解析覆盖范围（短期）**：域组、域、Agent、任务，以及任务的入口步骤/入口决策步骤。步骤/能力/规则的解析受限——此时优先从已有认知结果（如 BRIEF 流程蓝图）里取 FQN，或请用户更精确描述。

**绝不臆造 FQN**：任何 FQN 都必须来自 resolve 返回或已有认知结果，禁止自行拼造。

---

## ③ 认知查询（metaforge_cognition）

按分诊结果组装参数：

- `template`：分诊出的模板
- `entityFqn`：resolve 出的 FQN（BRIEF/GUIDE/FORECAST/DELEGATE 需要）
- `selectOperators`：按用户诉求选算子子集（见下），不传 = 模板全部算子
- `changeType`/`maxDepth`：FORECAST 场景
- `depth`/`archetype`：分诊推定的深度/原型

**算子选择（自由视角组合）**：按用户诉求信号选算子，常用对应关系：

| 用户想要 | 算子 |
|---------|------|
| 属性/是什么 | ontological.entity-profile |
| 流程/步骤顺序 | procedural.flow-blueprint |
| 决策/分支 | procedural.decision-branch |
| 规则/约束 | deontic.rule-listing |
| 能力/工具 | capability.tool-discovery |
| 接口协议 | capability.protocol-detail |
| 变更影响 | relational.impact-forward / impact-backward |
| 变更风险 | relational.risk-assessment |
| 变更合规 | deontic.constraint-check |
| 回归清单 | capability.regression-scope |

完整算子语义（含"何时选"）见同目录 `operators.md`，拿不准时读它。

---

## ④ 结果翻译（dimensions → 自然语言）

`metaforge_cognition` 返回 JSON，`data.dimensions` 是各算子的结构化结果。翻译成自然语言时遵守：

1. **先结论后细节**：一句话直接回答用户问题，再展开关键细节
2. **去术语**：不出现 FQN、Bundle、operatorId、entitySchemaFqn 等词，用业务语言（"库存盘点任务""核验库存充足性步骤"）
3. **按需取舍**：只呈现与用户诉求相关的信息，不堆砌全部字段
4. **给建议**：末尾给"下一步建议"（如复合意图的延伸、或建议继续查什么）
5. **中文输出**

示例——用户问"库存盘点任务有哪些步骤和规则"：

> 库存盘点任务包含 3 个步骤：检查库存（入口）→ 核验库存充足性 → 触发补货（出口）。
> 其中「核验库存充足性」受 1 条强制规则约束：库存数量必须大于零，否则触发补货。
> 需要进一步了解某一步的执行细节吗？

---

## 参考

- 完整算子语义目录：同目录 `operators.md`（20 个算子，含适用锚点、关键输出、何时选）
