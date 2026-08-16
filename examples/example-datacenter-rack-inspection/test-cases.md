# 测试用例：数据中心机柜健康巡检

## 测试环境

- MetaForge 服务端：`http://localhost:8080`（V4 含 `TaskProcessesBusinessObject` + `RuleConstrainsAttribute`）
- seed 已应用：`test/cognition/seed/agent-library-seed.sql` + `examples/example-datacenter-rack-inspection/seed-datacenter2.sql`
- 业务数据：`examples/example-datacenter-rack-inspection/data/devices/{rack-01,03,05,07}.json` + `examples/example-datacenter-rack-inspection/data/firmware/{fw-r01,r03,r07}.json`
- 测试目录：`/data/ext/source-8/metaforge`

---

## DC2-1 域定位：数据中心域进入域树（含 3 业务对象）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（ORIENT）下钻『数据中心域组』报告下面有哪些域，再下钻数据中心运维域报告其成员。"
```

**预期**：数据中心运维域下含 **机柜遥测 + 固件 + 告警** 3 个业务对象（L3）+ Agent + 机柜健康巡检任务。

**通过标准**：3 个 L3 业务对象出现在域树下钻结果。

---

## DC2-2 任务 → 业务对象关系（TaskProcessesBusinessObject）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（BRIEF）分别查询『机柜健康巡检任务』『固件升级子任务』『紧急降温子任务』，报告各自处理的业务对象。"
```

**预期**：巡检→BO_RackTelemetry；固件→BO_Firmware；紧急降温→BO_Alarm。

**通过标准**：3 个任务分别处理不同业务对象。

---

## DC2-3 业务对象字段结构（3 张表 L3-L5）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition 展开『机柜遥测』『固件』『告警』3 个业务对象的结构（BO→LE→字段），报告字段名、类型、单位。"
```

**预期**：
- 遥测：rack_id / temperature(°C) / humidity(%) / power_load(kW)
- 固件：device_type / target_version / upgrade_status
- 告警：alarm_id / severity / rack_id

**通过标准**：3 张表完整字段结构来自 L3-L5 元数据。

---

## DC2-4 规则 → 字段关系（RuleConstrainsAttribute）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（GUIDE）查询『机柜温度临界』『功率负载警告』『告警级别规则』，报告各自约束哪些字段。"
```

**预期**：Rule_TempCritical→temperature、Rule_PowerWarning→power_load、Rule_AlarmSeverity→severity。

**通过标准**：规则绑定到具体字段。

---

## DC2-5 环境校验多分支决策

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（BRIEF）查『机柜健康巡检任务』流程，再（GUIDE）查『环境校验决策』的分支。报告流程和 3 个分支。"
```

**预期**：流程 读取遥测→环境校验决策→…；分支：紧急降温（温度≥28/湿度≥60）、固件升级（功率>12/温度≥26）、正常。

**通过标准**：3 分支决策正确。

---

## DC2-6 固件升级决策嵌套

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（BRIEF）查『固件升级子任务』流程，再（GUIDE）查『升级策略决策』的分支。报告流程和分支。"
```

**预期**：流程 备份配置→升级策略决策→重装/下发→验证升级；分支：FAILED→重装、PENDING→下发。

**通过标准**：决策嵌套（子任务内再嵌决策）正确。

---

## DC2-7 正常机柜（Rack-01）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是数据中心巡检 Agent。用 metaforge_cognition 了解机柜健康巡检的阈值规则（临界 温度≥28/湿度≥60→紧急降温；警告 功率>12/温度≥26→固件升级）。读 examples/example-datacenter-rack-inspection/data/devices/rack-01.json，对照规则报告处置。"
```

**预期**：温度 24、湿度 45、功率 8.2，均未超 → **正常**。

---

## DC2-8 温度临界（Rack-03）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是数据中心巡检 Agent。用 metaforge_cognition 了解机柜健康巡检的阈值规则，读 examples/example-datacenter-rack-inspection/data/devices/rack-03.json，报告处置。"
```

**预期**：温度 29.5 ≥ 28（临界）→ **紧急降温**。

---

## DC2-9 湿度临界（Rack-05）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是数据中心巡检 Agent。用 metaforge_cognition 了解机柜健康巡检的阈值规则，读 examples/example-datacenter-rack-inspection/data/devices/rack-05.json，报告处置。"
```

**预期**：湿度 66 ≥ 60（临界）→ **紧急降温**。

---

## DC2-10 功率警告（Rack-07，走固件升级 + 策略判定）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是数据中心巡检 Agent。用 metaforge_cognition 了解机柜健康巡检的阈值规则，读 examples/example-datacenter-rack-inspection/data/devices/rack-07.json 报告处置；该机柜走固件升级，再读 examples/example-datacenter-rack-inspection/data/firmware/fw-r07.json 报告升级策略（FAILED→重装 / PENDING→下发）。"
```

**预期**：功率 13.5 > 12（警告）→ **固件升级**；fw-r07 状态 PENDING → **正常下发**。

**通过标准**：遥测阈值判定 → 固件升级 → 按固件状态决策嵌套链完整。

---

## DC2-11 固件 FAILED 策略对照（fw-r03）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition 了解固件升级子任务的升级策略（FAILED→重装，PENDING→下发），读 examples/example-datacenter-rack-inspection/data/firmware/fw-r03.json，报告其状态对应的升级策略。"
```

**预期**：fw-r03 状态 FAILED → **重装固件**。

**通过标准**：FAILED 与 PENDING 两种状态映射不同策略。

---

## DC2-12 告警业务对象与告警级别规则

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（BRIEF）查『紧急降温子任务』流程（是否含生成告警步骤），再（GUIDE）查『告警级别规则』约束的字段。"
```

**预期**：紧急降温流程 降低负载→部署降温→生成告警（处理告警业务对象）；告警级别规则→severity 字段（CRITICAL 立即处理）。

**通过标准**：告警流转 + 告警字段约束规则确认。

---

## 测试记录表

| 用例 | 结果（PASS/FAIL） | 备注 |
|------|------------------|------|
| DC2-1 域定位（3 业务对象） | | |
| DC2-2 任务→业务对象 | | |
| DC2-3 字段结构（3 表） | | |
| DC2-4 规则→字段 | | |
| DC2-5 环境校验多分支 | | |
| DC2-6 固件决策嵌套 | | |
| DC2-7 Rack-01 正常 | | |
| DC2-8 Rack-03 温度临界 | | |
| DC2-9 Rack-05 湿度临界 | | |
| DC2-10 Rack-07 功率→固件→下发 | | |
| DC2-11 固件 FAILED→重装 | | |
| DC2-12 告警流转 + 级别规则 | | |

---

## 验收结论（写在此处）

> 本测试证明：MetaForge 存储 3 个业务对象结构（L3-L5）+ 两级阈值 + 决策语义的**说明书**，
> 业务数据（机柜遥测/固件状态）独立存放；Agent 依据说明书执行多分支巡检决策——
> **同一说明书对 4 台机柜产生 正常/紧急降温/紧急降温/固件升级 四种处置**，
> 并支持决策嵌套（固件升级策略按状态 FAILED/PENDING 分流），所有阈值/字段/关系来自 metaforge 而非模型先验。
