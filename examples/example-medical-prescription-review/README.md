# 医疗处方审核（example-medical-prescription-review）

**场景概述**：药剂审核 Agent 处理处方审核任务——从 MetaForge 获取处方的业务对象结构（L3-L5 表结构）与规则阈值（单次剂量>500mg 转药师复核、并用药物冲突需人工复核），读取独立存放的处方数据（rx-001/002/003），对照规则给出审核结论（通过/转复核）。展示"说明书存 MetaForge、业务数据独立、Agent 结合执行"。

核心验证：**MetaForge 存储的是"数据的说明书"（业务对象表结构 + 任务 + 规则阈值），业务数据（处方实例）独立存放**，Agent 结合两者执行任务。

## 目录结构

```
examples/example-medical-prescription-review/
├── README.md                     # 本文件
├── seed-medical.sql              # 医疗域元数据（业务对象结构 + 任务 + 规则 + 关系）
├── data/
│   └── prescriptions/            # 业务数据（处方实例，独立存放）
│       ├── rx-001.json           #   布洛芬 400mg，正常
│       ├── rx-002.json           #   对乙酰氨基酚 800mg + 华法林，超剂量
│       └── rx-003.json           #   对乙酰氨基酚 500mg，临界
└── test-cases.md                 # 测试用例
```

## 核心概念：说明书 vs 数据

| | 存哪里 | 内容 |
|---|--------|------|
| **语义说明书（元数据）** | MetaForge | 处方业务对象结构（字段/类型）、任务流程、规则阈值（>500mg 转复核）、能力 |
| **业务数据（实例）** | `examples/example-medical-prescription-review/data/prescriptions/*.json` | 具体处方（RX-002 剂量 800mg） |

MetaForge **不存**"RX-002 是 800mg"这个具体值，**只存**"单次剂量 > 500mg 需转药师复核"这个阈值定义。是否超限是执行时对照得出的。

## 数据模型（test3 新增元数据）

### 业务对象结构（L3-L5 表结构，存 MetaForge）

```
医疗域组 (Group_Medical, L1)
└── 医疗审核域 (Domain_Healthcare, L2)
    ├── 处方业务对象 (BO_Prescription, L3 = 表)
    │   └── 处方逻辑实体 (LE_Prescription, L4 = 表落地)
    │       ├── Attr_Drug (L5, 字段 drug, string)
    │       ├── Attr_DoseMg (L5, 字段 single_dose_mg, number)
    │       └── Attr_ConcurrentDrugs (L5, 字段 concurrent_drugs, string[])
    ├── 药剂审核Agent (Agent_RxAgent)
    └── 处方审核任务 (Task_RxReview)
        ├── 读取处方(ENTRY) → 剂量校验决策(DECISION) → 相互作用检查 → 审核结论(EXIT)
        ├── 剂量超限 → 药师复核子任务 (Task_RxRecheck)
        ├── 规则：Rule_DosageLimit / Rule_Interaction
        └── 能力：Cap_DrugDB / Cap_RxReader
```

### 新增 V4 关系 schema（agent 库建立关系）

| 关系 | source → target | 语义 |
|------|-----------------|------|
| **`TaskProcessesBusinessObject`** | agent.Task → common.BusinessObject | 任务处理哪个业务对象（处理者→被处理者） |
| **`RuleConstrainsAttribute`** | agent.ExecutionRule → common.Attribute | 规则约束业务对象的哪个字段 |

关键：业务对象结构（L3-L5）与 agent 库的关系**在 agent 包定义**（单向依赖，common 保持数据语义纯净，与 `StepUsesCapability` 方向一致）。

### 实例关系

```
Task_RxReview ── TaskProcessesBusinessObject ──▶ BO_Prescription
Rule_DosageLimit ── RuleConstrainsAttribute ──▶ Attr_DoseMg
Rule_Interaction ── RuleConstrainsAttribute ──▶ Attr_ConcurrentDrugs
BO_Prescription ── BusinessObjectRefinesLogicalEntity ──▶ LE_Prescription
LE_Prescription ── LogicalEntityContainsAttribute ──▶ Attr_Drug / Attr_DoseMg / Attr_ConcurrentDrugs
```

## 快速开始

```bash
# 1. 重建数据库（V4 新增 2 个关系 schema）+ 应用 seed
export PGPASSWORD=metaforge
psql -h localhost -U metaforge -d metaforge \
  -c "DROP SCHEMA IF EXISTS metamodel_governance CASCADE; DROP SCHEMA IF EXISTS metadata_management CASCADE; DROP SCHEMA IF EXISTS semantic_relation_network CASCADE; DROP TABLE IF EXISTS public.flyway_schema_history CASCADE;"
# 重启 boot（跑新 V4 迁移）→ 应用基础 seed → 应用 test3 seed
psql -h localhost -U metaforge -d metaforge -f test/cognition/seed/agent-library-seed.sql
psql -h localhost -U metaforge -d metaforge -f examples/example-medical-prescription-review/seed-medical.sql

# 2. 端到端执行（RX-002 超剂量审核）
cd /data/ext/source-8/metaforge
opencode run "你是药剂审核 Agent，执行处方审核任务：resolve 处方审核 → BRIEF 拿说明书（流程/业务对象字段/规则阈值）→ 读 examples/example-medical-prescription-review/data/prescriptions/rx-002.json → 对照规则给出处置结论"
```

## 预期结论（RX-002）

- 剂量 800mg > 500mg 且为 acetaminophen → 触发 `Rule_DosageLimit` → **转药师复核**
- 并用药物仅 warfarin（无 warfarin+aspirin 对）→ 不触发 `Rule_Interaction`

详见 [test-cases.md](test-cases.md)。
