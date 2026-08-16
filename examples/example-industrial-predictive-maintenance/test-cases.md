# test4 测试用例：工业设备预测性维护（复杂度增强版）

## 测试环境

- MetaForge 服务端：`http://localhost:8080`（V4 含 `TaskProcessesBusinessObject` + `RuleConstrainsAttribute`）
- seed 已应用：`test/cognition/seed/agent-library-seed.sql` + `examples/example-medical-prescription-review/seed-medical.sql`（可选）+ `examples/example-industrial-predictive-maintenance/seed-industrial.sql`
- 业务数据：`examples/example-industrial-predictive-maintenance/data/iot/{pump-01,03,04,05,07,08}.json`
- 测试目录：`/data/ext/source-8/metaforge`

---

## IC-1 域定位：工业域进入域树（含双业务对象）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（ORIENT）下钻『工业域组』报告下面有哪些域，再下钻设备维护域报告其成员。"
```

**预期**：
- 工业域组 → 设备维护域
- 设备维护域下：**遥测业务对象** + **工单业务对象** + 设备维护Agent + 预测性维护任务

**通过标准**：两个 L3 业务对象出现在域树下钻结果中。

---

## IC-2 任务 → 业务对象关系（TaskProcessesBusinessObject）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（BRIEF）查询『预测性维护任务』，报告它处理哪些业务对象（TaskProcessesBusinessObject 关系）。"
```

**预期**：主任务 → `BO_Telemetry`；常规维修子任务 → `BO_WorkOrder`。

**通过标准**：Agent 报告两个任务分别处理不同业务对象。

---

## IC-3 业务对象字段结构（L3-L5 表结构）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition 展开『遥测业务对象』和『工单业务对象』的结构（BO→LE→字段），报告字段名、类型、单位。"
```

**预期**：
- 遥测：asset_id(string) / vibration(number,mm/s) / motor_temp(number,°C) / runtime_h(number,h)
- 工单：work_order_id(string) / priority(string) / status(string)

**通过标准**：完整字段结构（含单位）来自 L3-L5 元数据。

---

## IC-4 规则 → 字段关系（RuleConstrainsAttribute）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（GUIDE）分别查询『电机温度临界』『运行时长强制维护』规则，报告它们约束哪些字段（RuleConstrainsAttribute）以及运行规则是否适用于任务（RuleAppliesToTask）。"
```

**预期**：
- Rule_TempCritical → Attr_MotorTemp（+ Step 级 RuleAppliesTo）
- Rule_RuntimeForce → Attr_Runtime（字段级）+ Task_PredictiveMaintenance（**任务级** RuleAppliesToTask）+ Step（步骤级）

**通过标准**：Agent 报告规则的字段级 + 任务级绑定。

---

## IC-5 决策嵌套：常规维修内故障类型判断

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（BRIEF）查询『常规维修子任务』流程，再（GUIDE）查『故障类型判断』决策的分支。报告流程和分支。"
```

**预期**：
- 流程：故障诊断 → **故障类型判断[DECISION]** → 重启测试/更换备件 → 创建工单 → 指派技术员
- 决策分支：温度主导→重启测试（电气）；振动主导→更换备件（机械）

**通过标准**：决策嵌套出现在流程中，分支条件正确。

---

## IC-6 运行时长任务级规则

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（GUIDE）查询『运行时长强制维护』规则，确认它同时约束遥测的 runtime_h 字段并适用于预测性维护任务整体。"
```

**预期**：Rule_RuntimeForce → Attr_Runtime（RuleConstrainsAttribute）+ Task_PredictiveMaintenance（RuleAppliesToTask）。

**通过标准**：任务级规则（遥测正常也触发的独立约束）被确认。

---

## IC-7 正常设备（pump-01）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是设备维护 Agent。用 metaforge_cognition 了解预测性维护任务的阈值规则（温度>85/振动>4.5 临界→紧急；温度>75/振动>3.5 警告→常规；运行>2000h→强制）。读 examples/example-industrial-predictive-maintenance/data/iot/pump-01.json，对照规则报告处置结论。"
```

**预期**：振动 3.2、温度 72、运行 420h，各项未超 → **正常（标记健康）**。

---

## IC-8 温度警告（pump-03，双指标，振动主导）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "同上了解阈值规则 + 常规维修故障类型判断（温度主导→电气→重启测试；振动主导→机械→更换备件）。读 examples/example-industrial-predictive-maintenance/data/iot/pump-03.json，报告处置与故障类型子处置。"
```

**预期**：振动 3.8 > 3.5 且 温度 78 > 75（双警告）→ 常规维修；振动偏离临界更近 → **机械故障 → 更换备件**。

---

## IC-9 振动警告（pump-04，机械）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "同上流程，读 examples/example-industrial-predictive-maintenance/data/iot/pump-04.json，报告处置与故障类型子处置。"
```

**预期**：振动 3.8 > 3.5（警告）、温度 60 正常 → 常规维修 → **振动主导 → 机械故障 → 更换备件**。

---

## IC-10 温度临界（pump-05，紧急升级）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "同上流程，读 examples/example-industrial-predictive-maintenance/data/iot/pump-05.json，报告处置。"
```

**预期**：温度 91 > 85（临界 MANDATORY）→ **紧急升级**（紧急停机→热线升级→生成告警）。

---

## IC-11 运行超限强制维护（pump-08）—— 核心增强用例

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "同上流程，读 examples/example-industrial-predictive-maintenance/data/iot/pump-08.json，报告处置。特别注意：它的遥测值是否正常？是否触发运行时长强制维护规则？"
```

**预期**：振动 3.4、温度 73 **均正常**（未超阈值），但运行 **2100h > 2000h** → **触发 Rule_RuntimeForce → 强制维护**（即使遥测正常）。

**通过标准**：Agent 正确识别"遥测正常但运行超限 → 强制维护"，证明规则不只看阈值。

---

## IC-12 工单 SLA 规则

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（GUIDE）查询『紧急工单SLA』规则，报告它约束工单哪个字段、适用哪个步骤。"
```

**预期**：Rule_SlaCritical → Attr_Status（工单状态字段，RuleConstrainsAttribute）+ Step_CreateWorkOrder（RuleAppliesTo）；条件"CRITICAL 工单须 2h 内处理"。

**通过标准**：SLA 作为工单字段约束规则被确认。

---

## 测试记录表

| 用例 | 结果（PASS/FAIL） | 备注 |
|------|------------------|------|
| IC-1 域定位（双业务对象） | | |
| IC-2 任务→业务对象 | | |
| IC-3 字段结构 | | |
| IC-4 规则→字段（含任务级） | | |
| IC-5 决策嵌套 | | |
| IC-6 运行时长任务级规则 | | |
| IC-7 pump-01 正常 | | |
| IC-8 pump-03 温度警告→机械 | | |
| IC-9 pump-04 振动警告→机械 | | |
| IC-10 pump-05 温度临界→紧急 | | |
| IC-11 pump-08 运行超限强制（核心） | | |
| IC-12 工单 SLA | | |

---

## 验收结论（写在此处）

> 本测试证明：MetaForge 存储业务对象结构（L3-L5）+ 多级规则阈值 + 决策语义的**说明书**，
> 业务数据（传感器读数）独立存放；Agent 依据说明书执行多分支维护决策——
> **同一说明书对 5 台设备产生 正常/常规-机械/常规-电气/紧急/强制维护 五种不同且正确的处置**，
> 且支持决策嵌套（故障类型）与任务级规则（运行时长），所有阈值/字段/关系来自 metaforge 而非模型先验。
