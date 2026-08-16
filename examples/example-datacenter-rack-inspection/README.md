# 数据中心机柜健康巡检（example-datacenter-rack-inspection）

**场景概述**：数据中心巡检 Agent 处理机柜健康巡检任务——从 MetaForge 获取遥测/固件/告警三业务对象结构、两级阈值规则（临界 温度≥28°C/湿度≥60% 紧急降温，警告 功率>12kW/温度≥26°C 固件升级），读取独立存放的机柜遥测与固件状态（rack-01/03/05/07 + fw-r03/r07），对照规则分流处置，含固件升级策略决策嵌套（FAILED→重装/PENDING→下发）与告警流转。

核心验证：**MetaForge 存储"数据说明书"（3 个业务对象表结构 + 任务流程 + 两级阈值规则 + 决策语义），业务数据（机柜遥测/固件状态）独立存放**，Agent 结合两者执行多分支巡检决策。

## 目录结构

```
examples/example-datacenter-rack-inspection/
├── README.md                    # 本文件
├── seed-datacenter2.sql         # 数据中心域元数据（3 业务对象 + 双子任务 + 决策嵌套）
├── data/
│   ├── devices/                 # 机柜遥测（独立存放）
│   │   ├── rack-01.json         #   正常
│   │   ├── rack-03.json         #   紧急降温（温度临界）
│   │   ├── rack-05.json         #   紧急降温（湿度临界）
│   │   └── rack-07.json         #   固件升级（功率警告）
│   └── firmware/                # 固件升级状态
│       ├── fw-r01.json          #   PENDING
│       ├── fw-r03.json          #   FAILED（→重装）
│       └── fw-r07.json          #   PENDING（→下发）
└── test-cases.md                # 测试用例（DC2-1 ~ DC2-12）
```

## 复杂度特性

| 维度 | 实现 |
|------|------|
| **3 个业务对象** | 遥测表 + 固件表 + 告警表（L3-L5） |
| **两级阈值** | MANDATORY 临界（温度≥28/湿度≥60）+ RECOMMENDED 警告（功率>12kW/温度≥26） |
| **多分支决策** | 环境校验 3 分支（紧急降温/固件升级/正常） |
| **决策嵌套** | 固件升级子任务内再嵌"升级策略"（FAILED→重装 / PENDING→下发） |
| **双子任务委派** | 紧急降温（→告警表）+ 固件升级（→固件表） |
| **告警流转** | 紧急降温生成告警（约束告警级别字段） |

## 数据模型

### 业务对象结构（L3-L5 元数据）

```
数据中心域组 (Group_DCOps, L1)
└── 数据中心运维域 (Domain_DataCenterOps, L2)
    ├── 机柜遥测业务对象 (BO_RackTelemetry, L3)
    │   └── LE_RackTelemetry (L4)
    │       ├── Attr_RackId / Attr_Temperature(°C) / Attr_Humidity(%) / Attr_PowerLoad(kW)
    ├── 固件业务对象 (BO_Firmware, L3)
    │   └── LE_Firmware (L4)
    │       ├── Attr_DeviceType / Attr_TargetVersion / Attr_UpgradeStatus
    └── 告警业务对象 (BO_Alarm, L3)
        └── LE_Alarm (L4)
            ├── Attr_AlarmId / Attr_Severity / Attr_AlarmRack
```

### 流程拓扑

```
Task_RackHealthCheck（机柜健康巡检主任务）
  Step_ReadTelemetry(ENTRY)
    → DecisionStep_EnvCheck（决策1）
         ├─ 温度≥28 或 湿度≥60 → Task_EmergencyCooling（紧急降温）
         ├─ 功率>12kW 或 温度≥26 → Task_FirmwareUpgrade（固件升级）
         └─ 正常 → Step_GenerateReport(EXIT)

Task_EmergencyCooling（紧急降温）
  Step_ReduceLoad → Step_DeployCooling → Step_GenerateAlarm（→BO_Alarm）

Task_FirmwareUpgrade（固件升级，决策嵌套）
  Step_BackupConfig → DecisionStep_UpgradeStrategy（决策2）
       ├─ FAILED → Step_ReinstallFirmware
       └─ PENDING → Step_DeployFirmware
  → Step_VerifyUpgrade
```

### 规则（5 条）

| 规则 | 级别 | 约束字段 | 触发 |
|------|------|---------|------|
| Rule_TempCritical | MANDATORY | Attr_Temperature | 温度≥28 → 紧急降温 |
| Rule_HumidityCritical | MANDATORY | Attr_Humidity | 湿度≥60 → 紧急降温 |
| Rule_PowerWarning | RECOMMENDED | Attr_PowerLoad | 功率>12kW → 固件升级 |
| Rule_TempWarning | RECOMMENDED | Attr_Temperature | 温度≥26 → 固件升级 |
| Rule_AlarmSeverity | MANDATORY | Attr_Severity | CRITICAL 告警立即处理 |

## 快速开始

```bash
# 1. 应用 seed（V4 关系 schema 已就绪）
export PGPASSWORD=metaforge
psql -h localhost -U metaforge -d metaforge -f examples/example-datacenter-rack-inspection/seed-datacenter2.sql

# 2. 端到端执行（4 机柜 + 固件策略）
cd /data/ext/source-8/metaforge
opencode run "你是数据中心巡检 Agent。先用 metaforge_cognition（BRIEF）查 Task_RackHealthCheck 拿说明书（流程/遥测字段/两级阈值：临界 温度≥28 或 湿度≥60→紧急降温；警告 功率>12kW 或 温度≥26→固件升级）。再了解 Task_FirmwareUpgrade 的升级策略（FAILED→重装，PENDING→下发）。读 examples/example-datacenter-rack-inspection/data/devices/ 下 rack-01/03/05/07 遥测对照阈值给处置；对走固件升级的机柜读 examples/example-datacenter-rack-inspection/data/firmware/ 对应文件判断升级策略。表格汇总：机柜、温度、湿度、功率、触发规则、处置结论。"
```

## 预期结果（4 机柜）

| 机柜 | 温度 | 湿度 | 功率 | 处置 |
|------|------|------|------|------|
| Rack-01 | 24 | 45 | 8.2 | 正常 |
| Rack-03 | 29.5 | 48 | 9.0 | 紧急降温（温度≥28 临界） |
| Rack-05 | 25 | 66 | 8.5 | 紧急降温（湿度≥60 临界） |
| Rack-07 | 26.5 | 50 | 13.5 | 固件升级（功率>12 警告）→ 读 fw-r07(PENDING)→下发 |

详见 [test-cases.md](test-cases.md)。
