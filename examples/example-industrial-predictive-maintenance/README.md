# 工业设备预测性维护（example-industrial-predictive-maintenance）

**场景概述**：设备维护 Agent 处理预测性维护任务——从 MetaForge 获取遥测/工单双业务对象结构、两级阈值规则（临界>85°C/>4.5mm/s 紧急升级，警告>75°C/>3.5mm/s 常规维修）与运行时长强制规则（>2000h），读取独立存放的传感器读数（pump-01/03/04/05/08），对照规则分流处置，含故障类型决策嵌套（温度→电气/振动→机械）。

核心验证：**MetaForge 存储"数据说明书"（业务对象表结构 + 任务流程 + 多级规则阈值），业务数据（传感器读数）独立存放**，Agent 结合两者执行多分支维护决策。

## 目录结构

```
examples/example-industrial-predictive-maintenance/
├── README.md                 # 本文件
├── seed-industrial.sql       # 工业域元数据（含 v2 增强）
├── data/iot/                 # 业务数据（传感器读数，独立存放）
│   ├── pump-01.json          #   正常
│   ├── pump-03.json          #   常规-电气-重启测试
│   ├── pump-04.json          #   常规-机械-更换备件
│   ├── pump-05.json          #   紧急升级（温度临界）
│   ├── pump-07.json          #   紧急升级（振动临界）
│   └── pump-08.json          #   强制维护（运行>2000h）
└── test-cases.md             # 测试用例（IC-1 ~ IC-12）
```

## 复杂度特性

| 维度 | 实现 |
|------|------|
| **双业务对象** | 遥测表（L3-L5）+ 工单表（L3-L5） |
| **两级阈值** | MANDATORY 临界（温>85/振>4.5）+ RECOMMENDED 警告（温>75/振>3.5） |
| **多分支决策** | 健康度评估 3 分支（紧急/常规/正常） |
| **决策嵌套** | 常规维修内部再嵌套"故障类型判断"（电气/机械） |
| **任务级规则** | 运行时长 >2000h 强制维护（遥测正常也触发） |
| **工单 SLA 规则** | CRITICAL 工单须 2h 内处理（约束工单状态字段） |
| **跨任务委派** | 紧急→Task_HotlineEscalate；常规→Task_RoutineRepair |

## 数据模型

### 业务对象结构（L3-L5 元数据）

```
工业域组 (Group_Industrial, L1)
└── 设备维护域 (Domain_EquipmentMaintenance, L2)
    ├── 遥测业务对象 (BO_Telemetry, L3)
    │   └── LE_Telemetry (L4)
    │       ├── Attr_AssetId / Attr_Vibration(mm/s) / Attr_MotorTemp(°C) / Attr_Runtime(h)
    └── 工单业务对象 (BO_WorkOrder, L3)
        └── LE_WorkOrder (L4)
            ├── Attr_WorkOrderId / Attr_Priority / Attr_Status
```

### 流程拓扑

```
Task_PredictiveMaintenance
  Step_CollectTelemetry(ENTRY)
    → DecisionStep_HealthAssess（决策1）
         ├─ 温度>85 或 振动>4.5 → Task_HotlineEscalate（紧急）
         ├─ 温度>75 或 振动>3.5 → Task_RoutineRepair（常规）
         └─ 正常 → Step_MarkHealthy(EXIT)
         ⚡ 运行>2000h（任务级规则）→ 即使遥测正常也强制 → Task_RoutineRepair

Task_RoutineRepair（常规维修，决策嵌套）
  Step_Diagnose → DecisionStep_FaultType（决策2）
       ├─ 温度主导 → 电气故障 → Step_RestartAndTest
       └─ 振动主导 → 机械故障 → Step_ReplacePart
  → Step_CreateWorkOrder（SLA 规则）→ Step_AssignTechnician

Task_HotlineEscalate（紧急）
  Step_EmergencyStop → Step_EscalateHotline → Step_GenerateAlarm
```

### 规则（6 条）

| 规则 | 级别 | 约束字段 | 触发 |
|------|------|---------|------|
| Rule_TempCritical | MANDATORY | Attr_MotorTemp | 温度>85 → 紧急 |
| Rule_VibCritical | MANDATORY | Attr_Vibration | 振动>4.5 → 紧急 |
| Rule_TempWarning | RECOMMENDED | Attr_MotorTemp | 温度>75 → 常规 |
| Rule_VibWarning | RECOMMENDED | Attr_Vibration | 振动>3.5 → 常规 |
| Rule_RuntimeForce | MANDATORY | Attr_Runtime + 任务级 | 运行>2000h → 强制维护 |
| Rule_SlaCritical | MANDATORY | Attr_Status | CRITICAL 工单 2h 内处理 |

## 快速开始

```bash
# 1. 应用 seed（V4 关系 schema 已在 test3 就位）
export PGPASSWORD=metaforge
psql -h localhost -U metaforge -d metaforge -f examples/example-industrial-predictive-maintenance/seed-industrial.sql

# 2. 端到端执行（5 台设备，含嵌套决策 + 运行规则）
cd /data/ext/source-8/metaforge
opencode run "你是设备维护 Agent。先了解『预测性维护任务』说明书：用 metaforge_cognition（BRIEF）查 Task_PredictiveMaintenance，拿流程、业务对象字段、两级阈值（临界：温度>85 或 振动>4.5 → 紧急；警告：温度>75 或 振动>3.5 → 常规）、运行时长规则（>2000h 强制维护）。再了解常规维修子任务（Task_RoutineRepair）故障类型判断：温度主导→电气→重启测试；振动主导→机械→更换备件。然后读 examples/example-industrial-predictive-maintenance/data/iot/ 下 pump-01/03/04/05/08 五个读数，对照规则给出处置（含故障类型子处置）。表格汇总：设备、振动、温度、运行h、触发规则、处置结论。"
```

## 预期结果（5 台设备）

| 设备 | 处置 | 关键依据 |
|------|------|---------|
| pump-01 | 正常 | 各项未超阈值 |
| pump-03 | 常规→机械→更换备件 | 振动 3.8 + 温度 78（警告），振动主导 |
| pump-04 | 常规→机械→更换备件 | 振动 3.8 > 3.5（警告） |
| pump-05 | 紧急升级（+电气参考） | 温度 91 > 85（临界） |
| pump-08 | 强制维护 | 运行 2100h > 2000h（遥测正常也触发） |

详见 [test-cases.md](test-cases.md)。
