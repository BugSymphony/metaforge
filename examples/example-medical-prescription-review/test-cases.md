# test3 测试用例：医疗处方审核（业务对象结构作为元数据）

## 测试环境

- MetaForge 服务端：`http://localhost:8080`（V4 已含 `TaskProcessesBusinessObject` + `RuleConstrainsAttribute`）
- seed 已应用：`test/cognition/seed/agent-library-seed.sql` + `examples/example-medical-prescription-review/seed-medical.sql`
- 业务数据：`examples/example-medical-prescription-review/data/prescriptions/{rx-001,rx-002,rx-003}.json`
- 测试目录：`/data/ext/source-8/metaforge`

---

## MC-1 域定位：医疗域进入域树

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（ORIENT）下钻『医疗域组』，报告下面有哪些域，再下钻医疗审核域报告其成员。"
```

**预期**：
- 医疗域组 → 医疗审核域
- 医疗审核域下：**处方业务对象**（BusinessObject）+ 药剂审核Agent + 处方审核任务

**通过标准**：L3 业务对象出现在域树下钻结果中。

---

## MC-2 任务 → 业务对象关系（TaskProcessesBusinessObject）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（BRIEF）查询『处方审核任务』，报告它的直连出边里有哪些业务对象（TaskProcessesBusinessObject 关系）。"
```

**预期**：任务出边含 `BO_Prescription`（处方业务对象），关系 `TaskProcessesBusinessObject`。

**通过标准**：Agent 报告"任务处理处方业务对象"。

---

## MC-3 业务对象字段结构（L3-L5 表结构）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition 展开『处方业务对象』的结构：沿 BO→LE→字段 展开，报告它的字段名、类型和说明。"
```

**预期**：
| 字段 | 类型 | 说明 |
|------|------|------|
| drug | string | 处方药物名称 |
| single_dose_mg | number(mg) | 单次给药剂量 |
| concurrent_drugs | string[] | 并用药物列表 |

**通过标准**：Agent 报告完整字段结构（来自 L3-L5 元数据，非猜测）。

---

## MC-4 规则 → 字段关系（RuleConstrainsAttribute）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（GUIDE）查询『单次剂量上限』规则，报告它约束了哪个字段（RuleConstrainsAttribute 关系）以及规则条件。"
```

**预期**：`Rule_DosageLimit` → 约束字段 `Attr_DoseMg`（single_dose_mg），条件 `single_dose_mg > 500 且 drug == acetaminophen`。

**通过标准**：Agent 报告规则约束到具体字段。

---

## MC-5 端到端：说明书指导执行（RX-002 超剂量）—— 核心用例

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是药剂审核 Agent，执行『处方审核任务』：1) resolve 处方审核拿 FQN；2) BRIEF 拿说明书（流程+业务对象字段+规则阈值）；3) 读 examples/example-medical-prescription-review/data/prescriptions/rx-002.json；4) 对照规则给出处置结论。报告：FQN、业务对象字段、rx-002 数据值、处置结论。"
```

**预期结论**：
- 剂量：800mg > 500mg 且 acetaminophen → **触发 Rule_DosageLimit → 转药师复核**
- 相互作用：concurrent_drugs 仅 warfarin（无 warfarin+aspirin 对）→ 不触发

**通过标准**：
- Agent 能精确报告虚构的字段结构 + 规则阈值（**来自 metaforge，非 LLM 先验**）
- 处置结论正确（转药师复核）

---

## MC-6 正常处方（RX-001）对照

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是药剂审核 Agent，执行『处方审核任务』：1) resolve 处方审核拿 FQN；2) BRIEF 拿说明书（流程+业务对象字段+规则阈值：single_dose_mg>500 且 drug==acetaminophen 转药师复核；concurrent_drugs 含 warfarin+aspirin 对必须人工复核）；3) 读 examples/example-medical-prescription-review/data/prescriptions/rx-001.json（布洛芬 400mg）；4) 对照规则给出处置结论。报告：FQN、业务对象字段、rx-001 数据值、处置结论。"
```

**预期**：剂量 400mg ≤ 上限、无相互作用 → **正常通过**（审核结论）。

**通过标准**：同一说明书、不同数据值，得到不同（且正确）的处置结论——证明执行由数据驱动。

---

## MC-7 临界处方（RX-003）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是药剂审核 Agent，执行『处方审核任务』：1) resolve 处方审核拿 FQN；2) BRIEF 拿说明书（流程+业务对象字段+规则阈值：single_dose_mg>500 且 drug==acetaminophen 转药师复核）；3) 读 examples/example-medical-prescription-review/data/prescriptions/rx-003.json（对乙酰氨基酚 500mg）；4) 对照规则给出处置结论。报告：FQN、业务对象字段、rx-003 数据值、处置结论。"
```

**预期**：500mg **等于**上限（>500 才超限）→ **不触发**剂量规则，正常通过（体现边界语义）。

**通过标准**：正确区分"等于阈值"与"超过阈值"。

---

## 测试记录表

| 用例 | 结果（PASS/FAIL） | 备注 |
|------|------------------|------|
| MC-1 域定位（含 L3） | | |
| MC-2 任务→业务对象关系 | | |
| MC-3 业务对象字段结构 | | |
| MC-4 规则→字段关系 | | |
| MC-5 RX-002 超剂量（核心） | | |
| MC-6 RX-001 正常对照 | | |
| MC-7 RX-003 临界对照 | | |

---

## 验收结论（写在此处）

> 本测试证明：MetaForge 存储业务对象结构（L3-L5 表结构）与规则阈值的**语义说明书**，
> 业务数据（处方实例）独立存放；Agent 通过 metaforge 拿说明书、读业务数据、对照规则执行——
> **同一份说明书（规则阈值），对不同数据值（RX-001/002/003）产生不同的、正确的处置结论**，
> 且所有阈值/字段信息来自 metaforge 而非模型先验。
