-- ============================================================
-- test5 数据中心机柜健康巡检域 seed（复杂度增强版，独立于 test2）
-- 核心：3 个业务对象（遥测表/固件表/告警表）作为 L3-L5 元数据；
--       主任务双分支 + 固件升级子任务内决策嵌套；业务数据独立放文件。
--
-- 流程拓扑（Task_RackHealthCheck）：
--   Step_ReadTelemetry(ENTRY)
--     → DecisionStep_EnvCheck（决策1：环境校验，两级）
--          ├─ 温度≥28°C 或 湿度≥60% → Task_EmergencyCooling（紧急降温，MANDATORY）
--          ├─ 功率>12kW 或 温度≥26°C → Task_FirmwareUpgrade（固件升级，RECOMMENDED）
--          └─ 正常 → Step_GenerateReport(EXIT)
--   紧急降温子任务 Task_EmergencyCooling：Step_ReduceLoad → Step_DeployCooling → Step_GenerateAlarm
--   固件升级子任务 Task_FirmwareUpgrade（处理 BO_Firmware）：
--     Step_BackupConfig → DecisionStep_UpgradeStrategy（决策2：升级策略，嵌套）
--          ├─ 固件状态 FAILED → Step_ReinstallFirmware
--          └─ 固件状态 PENDING → Step_DeployFirmware
--     → Step_VerifyUpgrade
--
-- 业务对象：
--   巡检任务 → BO_RackTelemetry；固件子任务 → BO_Firmware；紧急降温 → BO_Alarm
-- 规则（5 条）：
--   Rule_TempCritical(≥28)/Rule_HumidityCritical(≥60) → 紧急降温（MANDATORY）
--   Rule_PowerWarning(>12kW)/Rule_TempWarning(≥26) → 固件升级（RECOMMENDED）
--   Rule_AlarmSeverity(CRITICAL 立即处理) → 约束告警字段
-- ============================================================

-- ---------- 1. 业务对象结构（L3-L5 表结构元数据） ----------

-- 1.0 数据中心域组（L1）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Group_DCOps', '数据中心域组', NULL, 'metaforge:1.0.0.common.SubjectDomainGroup',
  '{"group_level":1,"description":"数据中心基础设施运维域分组"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.1 数据中心运维域（L2，独立于 test2 的 Domain_Datacenter）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Domain_DataCenterOps', '数据中心运维域', 'metaforge:1.0.0.common.Group_DCOps', 'metaforge:1.0.0.common.SubjectDomain',
  '{"domain_level":2,"description":"数据中心机柜环境健康巡检与固件运维"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.2 机柜遥测业务对象（L3 = 遥测表）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.BO_RackTelemetry', '机柜遥测业务对象', 'metaforge:1.0.0.common.Domain_DataCenterOps', 'metaforge:1.0.0.common.BusinessObject',
  '{"object_type":"TABLE","logical_entity":"metaforge:1.0.0.common.LE_RackTelemetry","description":"机柜环境遥测读数表"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.LE_RackTelemetry', '机柜遥测逻辑实体', 'metaforge:1.0.0.common.BO_RackTelemetry', 'metaforge:1.0.0.common.LogicalEntity',
  '{"table_name":"rack_telemetry","source":"examples/example-datacenter-rack-inspection/data/devices/","description":"机柜环境遥测读数逻辑落地"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 遥测字段（L5）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_RackId', '机柜ID', 'metaforge:1.0.0.common.LE_RackTelemetry', 'metaforge:1.0.0.common.Attribute',
  '{"field":"rack_id","type":"string","description":"机柜唯一标识"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_Temperature', '机柜温度', 'metaforge:1.0.0.common.LE_RackTelemetry', 'metaforge:1.0.0.common.Attribute',
  '{"field":"temperature","type":"number","unit":"°C","description":"机柜环境温度"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_Humidity', '机柜湿度', 'metaforge:1.0.0.common.LE_RackTelemetry', 'metaforge:1.0.0.common.Attribute',
  '{"field":"humidity","type":"number","unit":"%","description":"机柜环境湿度百分比"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_PowerLoad', '功率负载', 'metaforge:1.0.0.common.LE_RackTelemetry', 'metaforge:1.0.0.common.Attribute',
  '{"field":"power_load","type":"number","unit":"kW","description":"机柜实时功率负载"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.3 固件业务对象（L3 = 固件表）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.BO_Firmware', '固件业务对象', 'metaforge:1.0.0.common.Domain_DataCenterOps', 'metaforge:1.0.0.common.BusinessObject',
  '{"object_type":"TABLE","logical_entity":"metaforge:1.0.0.common.LE_Firmware","description":"设备固件升级记录表"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.LE_Firmware', '固件逻辑实体', 'metaforge:1.0.0.common.BO_Firmware', 'metaforge:1.0.0.common.LogicalEntity',
  '{"table_name":"firmware_upgrade","source":"examples/example-datacenter-rack-inspection/data/firmware/","description":"固件升级记录逻辑落地"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 固件字段（L5）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_DeviceType', '设备类型', 'metaforge:1.0.0.common.LE_Firmware', 'metaforge:1.0.0.common.Attribute',
  '{"field":"device_type","type":"string","description":"设备类型"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_TargetVersion', '目标版本', 'metaforge:1.0.0.common.LE_Firmware', 'metaforge:1.0.0.common.Attribute',
  '{"field":"target_version","type":"string","description":"固件目标版本号"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_UpgradeStatus', '升级状态', 'metaforge:1.0.0.common.LE_Firmware', 'metaforge:1.0.0.common.Attribute',
  '{"field":"upgrade_status","type":"string","enum":["PENDING","DONE","FAILED"],"description":"固件升级状态"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.4 告警业务对象（L3 = 告警表）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.BO_Alarm', '告警业务对象', 'metaforge:1.0.0.common.Domain_DataCenterOps', 'metaforge:1.0.0.common.BusinessObject',
  '{"object_type":"TABLE","logical_entity":"metaforge:1.0.0.common.LE_Alarm","description":"机柜告警记录表"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.LE_Alarm', '告警逻辑实体', 'metaforge:1.0.0.common.BO_Alarm', 'metaforge:1.0.0.common.LogicalEntity',
  '{"table_name":"rack_alarm","description":"机柜告警记录逻辑落地"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 告警字段（L5）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_AlarmId', '告警ID', 'metaforge:1.0.0.common.LE_Alarm', 'metaforge:1.0.0.common.Attribute',
  '{"field":"alarm_id","type":"string","description":"告警唯一编号"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_Severity', '告警级别', 'metaforge:1.0.0.common.LE_Alarm', 'metaforge:1.0.0.common.Attribute',
  '{"field":"severity","type":"string","enum":["LOW","MEDIUM","HIGH","CRITICAL"],"description":"告警严重级别"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_AlarmRack', '告警机柜', 'metaforge:1.0.0.common.LE_Alarm', 'metaforge:1.0.0.common.Attribute',
  '{"field":"rack_id","type":"string","description":"产生告警的机柜ID"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 2. 域树关系 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_DCGroupToDomain', 'DCGroupContainsDomain', 'metaforge:1.0.0.common.Group_DCOps', 'metaforge:1.0.0.common.Domain_DataCenterOps', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_DomainToTelemetry', 'DomainContainsTelemetry', 'metaforge:1.0.0.common.Domain_DataCenterOps', 'metaforge:1.0.0.common.BO_RackTelemetry', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_DomainToFirmware', 'DomainContainsFirmware', 'metaforge:1.0.0.common.Domain_DataCenterOps', 'metaforge:1.0.0.common.BO_Firmware', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_DomainToAlarm', 'DomainContainsAlarm', 'metaforge:1.0.0.common.Domain_DataCenterOps', 'metaforge:1.0.0.common.BO_Alarm', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 遥测 BO → LE → 字段
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_TelemetryToLE', 'TelemetryRefinesLE', 'metaforge:1.0.0.common.BO_RackTelemetry', 'metaforge:1.0.0.common.LE_RackTelemetry', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_RTLEtoRack', 'RTLEContainsRackId', 'metaforge:1.0.0.common.LE_RackTelemetry', 'metaforge:1.0.0.common.Attr_RackId', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_RTLEtoTemp', 'RTLEContainsTemp', 'metaforge:1.0.0.common.LE_RackTelemetry', 'metaforge:1.0.0.common.Attr_Temperature', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_RTLEtoHumidity', 'RTLEContainsHumidity', 'metaforge:1.0.0.common.LE_RackTelemetry', 'metaforge:1.0.0.common.Attr_Humidity', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_RTLEtoPower', 'RTLEContainsPower', 'metaforge:1.0.0.common.LE_RackTelemetry', 'metaforge:1.0.0.common.Attr_PowerLoad', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 固件 BO → LE → 字段
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_FirmwareToLE', 'FirmwareRefinesLE', 'metaforge:1.0.0.common.BO_Firmware', 'metaforge:1.0.0.common.LE_Firmware', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_FWLEtoDeviceType', 'FWLEContainsDeviceType', 'metaforge:1.0.0.common.LE_Firmware', 'metaforge:1.0.0.common.Attr_DeviceType', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_FWLEtoVersion', 'FWLEContainsVersion', 'metaforge:1.0.0.common.LE_Firmware', 'metaforge:1.0.0.common.Attr_TargetVersion', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_FWLEtoStatus', 'FWLEContainsStatus', 'metaforge:1.0.0.common.LE_Firmware', 'metaforge:1.0.0.common.Attr_UpgradeStatus', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 告警 BO → LE → 字段
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_AlarmToLE', 'AlarmRefinesLE', 'metaforge:1.0.0.common.BO_Alarm', 'metaforge:1.0.0.common.LE_Alarm', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_ALLEtoId', 'ALLEContainsAlarmId', 'metaforge:1.0.0.common.LE_Alarm', 'metaforge:1.0.0.common.Attr_AlarmId', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_ALLEtoSeverity', 'ALLEContainsSeverity', 'metaforge:1.0.0.common.LE_Alarm', 'metaforge:1.0.0.common.Attr_Severity', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_ALLEtoRack', 'ALLEContainsRack', 'metaforge:1.0.0.common.LE_Alarm', 'metaforge:1.0.0.common.Attr_AlarmRack', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 域 → Agent / 主任务
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DCDomainToAgent', 'DCDomainComposesAgent', 'metaforge:1.0.0.common.Domain_DataCenterOps', 'metaforge:1.0.0.agent.Agent_DCOpsAgent', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesAgent', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DCDomainToTask', 'DCDomainComposesTask', 'metaforge:1.0.0.common.Domain_DataCenterOps', 'metaforge:1.0.0.agent.Task_RackHealthCheck', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 3. Agent / 主任务 / 步骤 / 决策 实体 ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Agent_DCOpsAgent', '数据中心巡检Agent', NULL, 'metaforge:1.0.0.agent.Agent',
  '{"run_mode":"AUTONOMOUS","description":"负责机柜环境巡检、异常处置与固件运维"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_RackHealthCheck', '机柜健康巡检任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"HIGH","estimated_complexity":"COMPLEX","dataLocation":"examples/example-datacenter-rack-inspection/data/devices/"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_ReadTelemetry', '读取遥测', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY","capability_required":true}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_EnvCheck', '环境校验决策', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"按温度/湿度/功率两级阈值判定机柜环境","recommended_option":"临界超限紧急降温，警告超限固件升级，否则正常","rationale":"MANDATORY 临界与 RECOMMENDED 警告分流处置","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_GenerateReport', '生成巡检报告', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"EXIT"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 紧急降温子任务
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_EmergencyCooling', '紧急降温子任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"CRITICAL","estimated_complexity":"MODERATE"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_ReduceLoad', '降低负载', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_DeployCooling', '部署降温', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_GenerateAlarm', '生成告警', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 固件升级子任务
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_FirmwareUpgrade', '固件升级子任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"MEDIUM","estimated_complexity":"MODERATE"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_BackupConfig', '备份配置', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_UpgradeStrategy', '升级策略决策', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"按固件升级状态选择策略","recommended_option":"FAILED 重装，PENDING 正常下发","rationale":"失败固件需重装，待处理固件正常下发","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_ReinstallFirmware', '重装固件', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP","description":"升级失败后重装固件"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_DeployFirmware', '下发固件', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP","description":"正常下发目标版本固件"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_VerifyUpgrade', '验证升级', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 4. 规则（两级阈值 + 告警） ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_TempCritical', '机柜温度临界', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"temperature >= 28°C","action":"must_emergency_cooling","exception":null,"applicable_scenarios":["环境校验"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_HumidityCritical', '机柜湿度临界', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"humidity >= 60%","action":"must_emergency_cooling","exception":null,"applicable_scenarios":["环境校验"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_PowerWarning', '功率负载警告', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"RECOMMENDED","condition":"power_load > 12kW","action":"should_schedule_upgrade","exception":null,"applicable_scenarios":["环境校验"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_TempWarning', '机柜温度警告', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"RECOMMENDED","condition":"temperature >= 26°C","action":"should_schedule_upgrade","exception":null,"applicable_scenarios":["环境校验"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_AlarmSeverity', '告警级别规则', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"severity == CRITICAL 须立即处理","action":"must_handle_immediately","exception":null,"applicable_scenarios":["告警流转"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 5. 能力 + 协议 ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_TelemetryCollect', '遥测采集MCP', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"MCP","tool":"collect_rack_telemetry","params":["rack_id"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_CoolingSystem', '降温系统API', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"HTTP","endpoint":"/api/dc/cooling/deploy","method":"POST","params":["rack_id","mode"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_AlarmPush', '告警推送MCP', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"MCP","tool":"push_alarm","params":["severity","rack_id","message"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_FirmwareRepo', '固件仓库', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"HTTP","endpoint":"/api/dc/firmware/deploy","method":"POST","params":["device_type","version"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.McpTool_TelemetryCollect', '遥测采集工具', NULL, 'metaforge:1.0.0.protocol.McpTool',
  '{"server":"dc-mcp","tool_name":"collect_rack_telemetry","input_schema":["rack_id"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.Http_CoolingSystem', '降温系统接口', NULL, 'metaforge:1.0.0.protocol.Http',
  '{"endpoint":"/api/dc/cooling/deploy","method":"POST","content_type":"application/json"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.McpTool_AlarmPush', '告警推送工具', NULL, 'metaforge:1.0.0.protocol.McpTool',
  '{"server":"dc-mcp","tool_name":"push_alarm","input_schema":["severity","rack_id","message"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.Http_FirmwareRepo', '固件仓库接口', NULL, 'metaforge:1.0.0.protocol.Http',
  '{"endpoint":"/api/dc/firmware/deploy","method":"POST","content_type":"application/json"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 6. 任务 → 业务对象（TaskProcessesBusinessObject） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_HealthTaskToTelemetry', 'HealthCheckProcessesTelemetry', 'metaforge:1.0.0.agent.Task_RackHealthCheck', 'metaforge:1.0.0.common.BO_RackTelemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.TaskProcessesBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_FwTaskToFirmware', 'FwUpgradeProcessesFirmware', 'metaforge:1.0.0.agent.Task_FirmwareUpgrade', 'metaforge:1.0.0.common.BO_Firmware', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.TaskProcessesBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_CoolingTaskToAlarm', 'CoolingProcessesAlarm', 'metaforge:1.0.0.agent.Task_EmergencyCooling', 'metaforge:1.0.0.common.BO_Alarm', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.TaskProcessesBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 7. 规则 → 字段（RuleConstrainsAttribute） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_TempCritToAttr', 'TempCritConstrainsTemp', 'metaforge:1.0.0.agent.Rule_TempCritical', 'metaforge:1.0.0.common.Attr_Temperature', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_HumCritToAttr', 'HumCritConstrainsHumidity', 'metaforge:1.0.0.agent.Rule_HumidityCritical', 'metaforge:1.0.0.common.Attr_Humidity', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PowerWarnToAttr', 'PowerWarnConstrainsPower', 'metaforge:1.0.0.agent.Rule_PowerWarning', 'metaforge:1.0.0.common.Attr_PowerLoad', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_TempWarnToAttr', 'TempWarnConstrainsTemp', 'metaforge:1.0.0.agent.Rule_TempWarning', 'metaforge:1.0.0.common.Attr_Temperature', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AlarmSevToAttr', 'AlarmSevConstrainsSeverity', 'metaforge:1.0.0.agent.Rule_AlarmSeverity', 'metaforge:1.0.0.common.Attr_Severity', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 8. 流程后继 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_HealthTaskToRead', 'HealthCheckComposesRead', 'metaforge:1.0.0.agent.Task_RackHealthCheck', 'metaforge:1.0.0.agent.Step_ReadTelemetry', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReadToEnvCheck', 'ReadToEnvCheck', 'metaforge:1.0.0.agent.Step_ReadTelemetry', 'metaforge:1.0.0.agent.DecisionStep_EnvCheck', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextDecisionStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 环境校验 → 紧急降温（临界，fqn 字母序在前=主路径）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_EnvToCooling', 'EnvToCooling', 'metaforge:1.0.0.agent.DecisionStep_EnvCheck', 'metaforge:1.0.0.agent.Task_EmergencyCooling', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextTask', '{"condition":"温度≥28 或 湿度≥60"}')
ON CONFLICT (fqn) DO NOTHING;

-- 环境校验 → 固件升级（警告）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_EnvToFwUpgrade', 'EnvToFwUpgrade', 'metaforge:1.0.0.agent.DecisionStep_EnvCheck', 'metaforge:1.0.0.agent.Task_FirmwareUpgrade', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextTask', '{"condition":"功率>12kW 或 温度≥26"}')
ON CONFLICT (fqn) DO NOTHING;

-- 环境校验 → 生成报告（正常）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_EnvToReport', 'EnvToGenerateReport', 'metaforge:1.0.0.agent.DecisionStep_EnvCheck', 'metaforge:1.0.0.agent.Step_GenerateReport', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"正常"}')
ON CONFLICT (fqn) DO NOTHING;

-- 紧急降温子任务内部
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_CoolingToReduce', 'CoolingComposesReduceLoad', 'metaforge:1.0.0.agent.Task_EmergencyCooling', 'metaforge:1.0.0.agent.Step_ReduceLoad', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReduceToDeploy', 'ReduceToDeployCooling', 'metaforge:1.0.0.agent.Step_ReduceLoad', 'metaforge:1.0.0.agent.Step_DeployCooling', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DeployToAlarm', 'DeployToGenerateAlarm', 'metaforge:1.0.0.agent.Step_DeployCooling', 'metaforge:1.0.0.agent.Step_GenerateAlarm', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":2}')
ON CONFLICT (fqn) DO NOTHING;

-- 固件升级子任务内部（含决策嵌套）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_FwTaskToBackup', 'FwUpgradeComposesBackup', 'metaforge:1.0.0.agent.Task_FirmwareUpgrade', 'metaforge:1.0.0.agent.Step_BackupConfig', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_BackupToStrategy', 'BackupToStrategy', 'metaforge:1.0.0.agent.Step_BackupConfig', 'metaforge:1.0.0.agent.DecisionStep_UpgradeStrategy', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextDecisionStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 升级策略 → 重装（FAILED，fqn 字母序 Reinstall<Deploy 前=主路径）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StrategyToReinstall', 'StrategyToReinstall', 'metaforge:1.0.0.agent.DecisionStep_UpgradeStrategy', 'metaforge:1.0.0.agent.Step_ReinstallFirmware', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"固件状态 FAILED"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StrategyToDeploy', 'StrategyToDeploy', 'metaforge:1.0.0.agent.DecisionStep_UpgradeStrategy', 'metaforge:1.0.0.agent.Step_DeployFirmware', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"固件状态 PENDING"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReinstallToVerify', 'ReinstallToVerify', 'metaforge:1.0.0.agent.Step_ReinstallFirmware', 'metaforge:1.0.0.agent.Step_VerifyUpgrade', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DeployToVerify', 'DeployToVerify', 'metaforge:1.0.0.agent.Step_DeployFirmware', 'metaforge:1.0.0.agent.Step_VerifyUpgrade', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 9. 规则 → 步骤（RuleAppliesTo） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_TempCritToRead', 'TempCritAppliesToRead', 'metaforge:1.0.0.agent.Rule_TempCritical', 'metaforge:1.0.0.agent.Step_ReadTelemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_HumCritToRead', 'HumCritAppliesToRead', 'metaforge:1.0.0.agent.Rule_HumidityCritical', 'metaforge:1.0.0.agent.Step_ReadTelemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PowerWarnToRead', 'PowerWarnAppliesToRead', 'metaforge:1.0.0.agent.Rule_PowerWarning', 'metaforge:1.0.0.agent.Step_ReadTelemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_TempWarnToRead', 'TempWarnAppliesToRead', 'metaforge:1.0.0.agent.Rule_TempWarning', 'metaforge:1.0.0.agent.Step_ReadTelemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AlarmSevToAlarm', 'AlarmSevAppliesToAlarm', 'metaforge:1.0.0.agent.Rule_AlarmSeverity', 'metaforge:1.0.0.agent.Step_GenerateAlarm', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 10. 能力关联 + 协议实现 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReadToCapTelemetry', 'ReadUsesTelemetryCap', 'metaforge:1.0.0.agent.Step_ReadTelemetry', 'metaforge:1.0.0.agent.Cap_TelemetryCollect', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DeployCoolToCap', 'DeployCoolUsesCooling', 'metaforge:1.0.0.agent.Step_DeployCooling', 'metaforge:1.0.0.agent.Cap_CoolingSystem', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_GenAlarmToCap', 'GenAlarmUsesAlarmPush', 'metaforge:1.0.0.agent.Step_GenerateAlarm', 'metaforge:1.0.0.agent.Cap_AlarmPush', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DeployFwToCap', 'DeployFwUsesFirmwareRepo', 'metaforge:1.0.0.agent.Step_DeployFirmware', 'metaforge:1.0.0.agent.Cap_FirmwareRepo', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DCAgentToTelemetry', 'DCAgentHasTelemetry', 'metaforge:1.0.0.agent.Agent_DCOpsAgent', 'metaforge:1.0.0.agent.Cap_TelemetryCollect', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DCAgentToCooling', 'DCAgentHasCooling', 'metaforge:1.0.0.agent.Agent_DCOpsAgent', 'metaforge:1.0.0.agent.Cap_CoolingSystem', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DCAgentToAlarm', 'DCAgentHasAlarm', 'metaforge:1.0.0.agent.Agent_DCOpsAgent', 'metaforge:1.0.0.agent.Cap_AlarmPush', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DCAgentToFw', 'DCAgentHasFirmware', 'metaforge:1.0.0.agent.Agent_DCOpsAgent', 'metaforge:1.0.0.agent.Cap_FirmwareRepo', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DCAgentToTask', 'DCAgentExecutesHealthCheck', 'metaforge:1.0.0.agent.Agent_DCOpsAgent', 'metaforge:1.0.0.agent.Task_RackHealthCheck', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentExecutesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_DCTelemetryToMcp', 'DCTelemetryImplementsMcp', 'metaforge:1.0.0.agent.Cap_TelemetryCollect', 'metaforge:1.0.0.protocol.McpTool_TelemetryCollect', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsMcpTool', '{"protocol_type":"McpTool"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_DCCoolingToHttp', 'DCCoolingImplementsHttp', 'metaforge:1.0.0.agent.Cap_CoolingSystem', 'metaforge:1.0.0.protocol.Http_CoolingSystem', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsHttp', '{"protocol_type":"Http"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_DCAlarmToMcp', 'DCAlarmImplementsMcp', 'metaforge:1.0.0.agent.Cap_AlarmPush', 'metaforge:1.0.0.protocol.McpTool_AlarmPush', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsMcpTool', '{"protocol_type":"McpTool"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_DCFwToHttp', 'DCFirmwareImplementsHttp', 'metaforge:1.0.0.agent.Cap_FirmwareRepo', 'metaforge:1.0.0.protocol.Http_FirmwareRepo', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsHttp', '{"protocol_type":"Http"}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 11. 实体关系索引（test5 新关系） ----------

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT source_entity_fqn, 'OUTBOUND', fqn FROM semantic_relation_network.relation_instance
WHERE fqn LIKE 'metaforge:1.0.0.common.Rel_DC%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_RTLE%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_FWLE%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_ALLE%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_TelemetryTo%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_FirmwareTo%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_AlarmTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_HealthTask%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_FwTask%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_CoolingTask%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_TempCrit%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_HumCrit%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_PowerWarn%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_TempWarn%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_AlarmSev%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReadTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_EnvTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_CoolingTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReduceTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_DeployTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_BackupTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_StrategyTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReinstallTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_DCAgentTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_GenAlarm%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_DeployFwTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_DeployCoolTo%'
   OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_DC%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT target_entity_fqn, 'INBOUND', fqn FROM semantic_relation_network.relation_instance
WHERE fqn LIKE 'metaforge:1.0.0.common.Rel_DC%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_RTLE%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_FWLE%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_ALLE%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_TelemetryTo%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_FirmwareTo%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_AlarmTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_HealthTask%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_FwTask%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_CoolingTask%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_TempCrit%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_HumCrit%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_PowerWarn%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_TempWarn%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_AlarmSev%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReadTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_EnvTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_CoolingTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReduceTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_DeployTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_BackupTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_StrategyTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReinstallTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_DCAgentTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_GenAlarm%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_DeployFwTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_DeployCoolTo%'
   OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_DC%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

-- ---------- 12. 实体描述补充 ----------

UPDATE metadata_management.metadata_entity SET description = '数据中心域组——数据中心基础设施运维域分组' WHERE fqn = 'metaforge:1.0.0.common.Group_DCOps';
UPDATE metadata_management.metadata_entity SET description = '数据中心运维域——机柜环境健康巡检与固件运维' WHERE fqn = 'metaforge:1.0.0.common.Domain_DataCenterOps';
UPDATE metadata_management.metadata_entity SET description = '机柜遥测业务对象——机柜环境遥测读数表' WHERE fqn = 'metaforge:1.0.0.common.BO_RackTelemetry';
UPDATE metadata_management.metadata_entity SET description = '固件业务对象——设备固件升级记录表' WHERE fqn = 'metaforge:1.0.0.common.BO_Firmware';
UPDATE metadata_management.metadata_entity SET description = '告警业务对象——机柜告警记录表' WHERE fqn = 'metaforge:1.0.0.common.BO_Alarm';
UPDATE metadata_management.metadata_entity SET description = '数据中心巡检Agent——负责机柜环境巡检、异常处置与固件运维' WHERE fqn = 'metaforge:1.0.0.agent.Agent_DCOpsAgent';
UPDATE metadata_management.metadata_entity SET description = '机柜健康巡检任务——读取遥测→环境校验→紧急降温/固件升级/正常' WHERE fqn = 'metaforge:1.0.0.agent.Task_RackHealthCheck';
UPDATE metadata_management.metadata_entity SET description = '环境校验决策——按温度/湿度/功率两级阈值分流处置' WHERE fqn = 'metaforge:1.0.0.agent.DecisionStep_EnvCheck';
UPDATE metadata_management.metadata_entity SET description = '紧急降温子任务——降低负载、部署降温并生成告警' WHERE fqn = 'metaforge:1.0.0.agent.Task_EmergencyCooling';
UPDATE metadata_management.metadata_entity SET description = '固件升级子任务——备份→升级策略→重装/下发→验证' WHERE fqn = 'metaforge:1.0.0.agent.Task_FirmwareUpgrade';
UPDATE metadata_management.metadata_entity SET description = '升级策略决策——按固件状态 FAILED 重装 / PENDING 下发' WHERE fqn = 'metaforge:1.0.0.agent.DecisionStep_UpgradeStrategy';
UPDATE metadata_management.metadata_entity SET description = '机柜温度临界——温度≥28°C 必须紧急降温' WHERE fqn = 'metaforge:1.0.0.agent.Rule_TempCritical';
UPDATE metadata_management.metadata_entity SET description = '机柜湿度临界——湿度≥60% 必须紧急降温' WHERE fqn = 'metaforge:1.0.0.agent.Rule_HumidityCritical';
UPDATE metadata_management.metadata_entity SET description = '功率负载警告——功率>12kW 应安排固件升级' WHERE fqn = 'metaforge:1.0.0.agent.Rule_PowerWarning';
UPDATE metadata_management.metadata_entity SET description = '机柜温度警告——温度≥26°C 应安排固件升级' WHERE fqn = 'metaforge:1.0.0.agent.Rule_TempWarning';
UPDATE metadata_management.metadata_entity SET description = '告警级别规则——CRITICAL 告警须立即处理' WHERE fqn = 'metaforge:1.0.0.agent.Rule_AlarmSeverity';

-- ---------- 完成 ----------
