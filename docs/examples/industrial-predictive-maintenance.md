# 示例二：工业设备预测性维护

场景：设备维护 Agent 处理预测性维护任务——从 MetaForge 获取遥测/工单双业务对象结构、两级阈值规则与故障类型判断，读取独立存放的传感器读数，对照规则分流处置。

## 场景概述

- **Agent 角色**：设备维护 Agent
- **业务目标**：采集设备遥测，按振动/温度阈值判定健康度，分流处置（紧急升级 / 常规维修 / 正常）
- **MetaForge 角色**：提供遥测/工单结构 + 两级阈值 + 故障类型判断语义

## 语义说明书（存 MetaForge）

### 业务对象结构（L3-L5）

```
设备维护域 (Domain_EquipmentMaintenance)
├── 遥测业务对象 (BO_Telemetry, L3)
│   └── Attr_AssetId / Attr_Vibration(mm/s) / Attr_MotorTemp(°C) / Attr_Runtime(h)
└── 工单业务对象 (BO_WorkOrder, L3)
    └── Attr_WorkOrderId / Attr_Priority / Attr_Status
```

### 任务与流程（决策嵌套）

```
预测性维护任务 (Task_PredictiveMaintenance)
  采集遥测(ENTRY) → 健康度评估决策(DECISION) → [紧急/常规/正常 三分支]
  ⚡ 运行时长 >2000h → 即使遥测正常也强制维护（任务级规则）

常规维修子任务（决策嵌套）：
  故障诊断 → 故障类型判断(DECISION) → 温度主导→电气→重启测试 / 振动主导→机械→更换备件
  → 创建工单 → 指派技术员
```

### 规则阈值（两级，语义化）

| 规则 | 级别 | 约束字段 | 条件 |
|------|------|---------|------|
| Rule_TempCritical | MANDATORY | Attr_MotorTemp | 温度>85°C → 紧急升级 |
| Rule_VibCritical | MANDATORY | Attr_Vibration | 振动>4.5mm/s → 紧急升级 |
| Rule_TempWarning | RECOMMENDED | Attr_MotorTemp | 温度>75°C → 常规维修 |
| Rule_VibWarning | RECOMMENDED | Attr_Vibration | 振动>3.5mm/s → 常规维修 |
| Rule_RuntimeForce | MANDATORY | Attr_Runtime（任务级） | 运行>2000h → 强制维护 |

## 业务数据（独立存放）

```
examples/example-industrial-predictive-maintenance/data/iot/
├── pump-01.json   { vibration: 3.2, motor_temp: 72, runtime_h: 420 }
├── pump-03.json   { vibration: 3.8, motor_temp: 78, runtime_h: 850 }
├── pump-04.json   { vibration: 3.8, motor_temp: 60, runtime_h: 900 }
├── pump-05.json   { vibration: 4.2, motor_temp: 91, runtime_h: 1600 }
└── pump-08.json   { vibration: 3.4, motor_temp: 73, runtime_h: 2100 }
```

## 决策链路（Agent 对照说明书执行）

```
1. BRIEF 拿说明书：两级阈值（临界 85/4.5、警告 75/3.5）+ 运行强制规则（2000h）
2. 读 pump-08.json → 振动 3.4、温度 73（遥测正常），但运行 2100h
3. 对照规则：遥测未超阈值，但 2100 > 2000 → 触发任务级规则 → 强制维护
```

## 关键结论

| 设备 | 振动 | 温度 | 运行h | 处置 |
|------|------|------|-------|------|
| pump-01 | 3.2 | 72 | 420 | 正常 |
| pump-03 | 3.8 | 78 | 850 | 常规维修 → 振动主导 → 机械 → 更换备件 |
| pump-04 | 3.8 | 60 | 900 | 常规维修 → 机械 → 更换备件 |
| pump-05 | 4.2 | 91 | 1600 | **紧急升级**（温度 91>85 临界） |
| pump-08 | 3.4 | 73 | 2100 | **强制维护**（遥测正常但运行超限） |

亮点：**任务级规则（运行>2000h）独立于遥测阈值**——即使设备读数正常，运行时长超限也强制维护，展示说明书"规则不只是阈值"。

## 快速验证

```bash
cd /data/ext/source-8/metaforge
opencode run "你是设备维护 Agent。用 metaforge_cognition（BRIEF）查 Task_PredictiveMaintenance 拿说明书（两级阈值 + 运行>2000h 强制规则）。读 examples/example-industrial-predictive-maintenance/data/iot/pump-08.json，报告处置（特别注意遥测正常但运行超限）。"
```

预期：pump-08 → 强制维护。
