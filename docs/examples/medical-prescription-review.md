# 示例一：医疗处方审核

场景：药剂审核 Agent 处理处方审核任务——从 MetaForge 获取处方的业务对象结构与规则阈值，读取独立存放的处方数据，对照规则给出审核结论。

## 场景概述

- **Agent 角色**：药剂审核 Agent
- **业务目标**：审核处方（剂量是否超限、药物是否相互作用），输出通过 / 转药师复核
- **MetaForge 角色**：提供"处方是什么结构、有哪些规则阈值"的语义说明书

## 语义说明书（存 MetaForge）

### 业务对象结构（L3-L5）

```
医疗审核域 (Domain_Healthcare)
└── 处方业务对象 (BO_Prescription, L3 = 处方表)
    └── 处方逻辑实体 (LE_Prescription, L4)
        ├── Attr_Drug            (L5, 药物, string)
        ├── Attr_DoseMg          (L5, 单次剂量, number, 毫克)
        └── Attr_ConcurrentDrugs (L5, 并用药物, string[])
```

### 任务与流程

```
处方审核任务 (Task_RxReview)
  读取处方(ENTRY) → 剂量校验决策(DECISION) → 相互作用检查 → 审核结论(EXIT)
  剂量校验决策：单次剂量 > 500mg → 转药师复核；否则继续
```

### 规则阈值（语义化，不存具体值）

| 规则 | 级别 | 约束字段 | 条件 |
|------|------|---------|------|
| Rule_DosageLimit | MANDATORY | Attr_DoseMg | 单次剂量 > 500mg 且 药物=acetaminophen → 转复核 |
| Rule_Interaction | MANDATORY | Attr_ConcurrentDrugs | 并用药物含 warfarin+aspirin 对 → 人工复核 |

## 业务数据（独立存放）

```
examples/example-medical-prescription-review/data/prescriptions/
├── rx-001.json   { drug: ibuprofen,     single_dose_mg: 400, concurrent_drugs: [] }
├── rx-002.json   { drug: acetaminophen, single_dose_mg: 800, concurrent_drugs: [warfarin] }
└── rx-003.json   { drug: acetaminophen, single_dose_mg: 500, concurrent_drugs: [] }
```

## 决策链路（Agent 对照说明书执行）

```
1. resolve "处方审核" → Task_RxReview
2. BRIEF 拿说明书：流程 + 处方字段 + 规则阈值（>500mg 转复核）
3. 读 rx-002.json → 剂量 800mg、并用 warfarin
4. 对照规则：800 > 500 且 acetaminophen → 触发剂量规则 → 转药师复核
```

## 关键结论

**同一份说明书，对不同处方产生不同且正确的结论**：

| 处方 | 剂量 | 并用药物 | 结论 |
|------|------|---------|------|
| rx-001 | 400mg | 无 | 通过 |
| rx-002 | 800mg | warfarin | **转药师复核**（800>500 触发剂量规则） |
| rx-003 | 500mg | 无 | 通过（500 未超过 500，边界正确） |

所有阈值（500mg）、字段结构、规则条件均来自 MetaForge 说明书——不是模型先验，而是可查询的语义依据。

## 快速验证

Docker 环境已内置该域 seed（`docker/seed/02-medical.sql`）：

```bash
cd /data/ext/source-8/metaforge
opencode run "你是药剂审核 Agent。用 metaforge_cognition（BRIEF）查 Task_RxReview 拿说明书（处方字段/剂量阈值>500mg 转复核）。读 examples/example-medical-prescription-review/data/prescriptions/rx-002.json，对照规则报告处置结论。"
```

预期：rx-002（800mg）→ 转药师复核。
