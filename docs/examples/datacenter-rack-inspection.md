# 示例三：数据中心机柜健康巡检

场景：数据中心巡检 Agent 处理机柜健康巡检任务——从 MetaForge 获取遥测/固件/告警三业务对象结构、两级阈值规则与固件升级策略，读取独立存放的机柜遥测与固件状态，对照规则分流处置。

## 场景概述

- **Agent 角色**：数据中心巡检 Agent
- **业务目标**：读取机柜遥测（温度/湿度/功率），按阈值判定，分流处置（紧急降温 / 固件升级 / 正常）
- **MetaForge 角色**：提供三业务对象结构 + 两级阈值 + 固件升级策略语义

## 语义说明书（存 MetaForge）

### 业务对象结构（L3-L5）

```
数据中心运维域 (Domain_DataCenterOps)
├── 机柜遥测业务对象 (BO_RackTelemetry, L3)
│   └── Attr_RackId / Attr_Temperature(°C) / Attr_Humidity(%) / Attr_PowerLoad(kW)
├── 固件业务对象 (BO_Firmware, L3)
│   └── Attr_DeviceType / Attr_TargetVersion / Attr_UpgradeStatus
└── 告警业务对象 (BO_Alarm, L3)
    └── Attr_AlarmId / Attr_Severity / Attr_AlarmRack
```

### 任务与流程（决策嵌套）

```
机柜健康巡检任务 (Task_RackHealthCheck)
  读取遥测(ENTRY) → 环境校验决策(DECISION) → [紧急降温 / 固件升级 / 正常 三分支]

紧急降温子任务：降低负载 → 部署降温 → 生成告警（处理告警对象）

固件升级子任务（决策嵌套）：
  备份配置 → 升级策略决策(DECISION) → 固件状态 FAILED→重装 / PENDING→下发
  → 验证升级
```

### 规则阈值（两级，语义化）

| 规则 | 级别 | 约束字段 | 条件 |
|------|------|---------|------|
| Rule_TempCritical | MANDATORY | Attr_Temperature | 温度≥28°C → 紧急降温 |
| Rule_HumidityCritical | MANDATORY | Attr_Humidity | 湿度≥60% → 紧急降温 |
| Rule_PowerWarning | RECOMMENDED | Attr_PowerLoad | 功率>12kW → 固件升级 |
| Rule_TempWarning | RECOMMENDED | Attr_Temperature | 温度≥26°C → 固件升级 |
| Rule_AlarmSeverity | MANDATORY | Attr_Severity | CRITICAL 告警立即处理 |

## 业务数据（独立存放）

```
examples/example-datacenter-rack-inspection/data/devices/
├── rack-01.json   { temperature: 24,   humidity: 45, power_load: 8.2 }
├── rack-03.json   { temperature: 29.5, humidity: 48, power_load: 9.0 }
├── rack-05.json   { temperature: 25,   humidity: 66, power_load: 8.5 }
└── rack-07.json   { temperature: 26.5, humidity: 50, power_load: 13.5 }

examples/example-datacenter-rack-inspection/data/firmware/
├── fw-r03.json   { upgrade_status: FAILED }
└── fw-r07.json   { upgrade_status: PENDING }
```

## 决策链路（Agent 对照说明书执行）

```
1. BRIEF 拿说明书：两级阈值（临界 28/60、警告 12/26）+ 固件升级策略
2. 读 rack-07.json → 功率 13.5 > 12（警告）→ 走固件升级
3. 读 fw-r07.json → 状态 PENDING → 升级策略：正常下发
```

## 关键结论

| 机柜 | 温度 | 湿度 | 功率 | 处置 |
|------|------|------|------|------|
| Rack-01 | 24 | 45 | 8.2 | 正常 |
| Rack-03 | 29.5 | 48 | 9.0 | **紧急降温**（温度 29.5≥28 临界） |
| Rack-05 | 25 | 66 | 8.5 | **紧急降温**（湿度 66≥60 临界） |
| Rack-07 | 26.5 | 50 | 13.5 | **固件升级**（功率>12 警告）→ fw-r07 PENDING → 下发 |

亮点：**决策嵌套**——固件升级子任务内部再按固件状态（FAILED/PENDING）分重装/下发；**告警流转**——紧急降温生成告警并受告警级别规则约束。

## 快速验证

```bash
cd /data/ext/source-8/metaforge
opencode run "你是数据中心巡检 Agent。用 metaforge_cognition（BRIEF）查 Task_RackHealthCheck 拿说明书（两级阈值）。读 examples/example-datacenter-rack-inspection/data/devices/rack-03.json，报告处置。"
```

预期：Rack-03（温度 29.5）→ 紧急降温。
