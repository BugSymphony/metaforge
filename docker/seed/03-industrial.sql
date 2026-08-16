-- ============================================================
-- test4 工业设备预测性维护域 seed（复杂度增强版）
-- 核心：双业务对象（遥测表 + 工单表）作为 L3-L5 元数据存 MetaForge，
--       主任务多分支决策 + 跨任务委派（紧急/常规）+ 工单流转；
--       业务数据（传感器读数）独立放 data/iot/。
--
-- 流程拓扑（Task_PredictiveMaintenance）：
--   Step_CollectTelemetry(ENTRY)
--     → DecisionStep_HealthAssess（健康度评估，3 分支）
--          ├─ 温度>85°C 或 振动>4.5mm/s → Task_HotlineEscalate（紧急，MANDATORY）
--          ├─ 温度>75°C 或 振动>3.5mm/s → Task_RoutineRepair（常规，RECOMMENDED）
--          └─ 正常                      → Step_MarkHealthy（EXIT）
--   常规维修子任务 Task_RoutineRepair：Step_Diagnose → Step_CreateWorkOrder → Step_AssignTechnician
--   紧急升级子任务 Task_HotlineEscalate：Step_EmergencyStop → Step_EscalateHotline → Step_GenerateAlarm
--
-- 双业务对象：主任务处理 BO_Telemetry；常规维修处理 BO_WorkOrder
-- 四级规则→字段：Rule_TempCritical/VibCritical（MANDATORY）、Rule_TempWarning/VibWarning（RECOMMENDED）
-- ============================================================

-- ---------- 1. 业务对象结构（L3-L5 表结构元数据） ----------

-- 1.0 工业域组（L1）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Group_Industrial', '工业域组', NULL, 'metaforge:1.0.0.common.SubjectDomainGroup',
  '{"group_level":1,"description":"工业制造与设备运维域分组"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.1 设备维护域（L2）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Domain_EquipmentMaintenance', '设备维护域', 'metaforge:1.0.0.common.Group_Industrial', 'metaforge:1.0.0.common.SubjectDomain',
  '{"domain_level":2,"description":"工业设备预测性维护与工单流转"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.2 遥测业务对象（L3 = 传感器读数表）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.BO_Telemetry', '遥测业务对象', 'metaforge:1.0.0.common.Domain_EquipmentMaintenance', 'metaforge:1.0.0.common.BusinessObject',
  '{"object_type":"TABLE","logical_entity":"metaforge:1.0.0.common.LE_Telemetry","description":"设备传感器遥测读数表"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.LE_Telemetry', '遥测逻辑实体', 'metaforge:1.0.0.common.BO_Telemetry', 'metaforge:1.0.0.common.LogicalEntity',
  '{"table_name":"device_telemetry","source":"examples/example-industrial-predictive-maintenance/data/iot/","description":"传感器遥测读数的逻辑落地"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.3 遥测字段（L5）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_AssetId', '设备ID', 'metaforge:1.0.0.common.LE_Telemetry', 'metaforge:1.0.0.common.Attribute',
  '{"field":"asset_id","type":"string","description":"设备唯一标识"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_Vibration', '振动', 'metaforge:1.0.0.common.LE_Telemetry', 'metaforge:1.0.0.common.Attribute',
  '{"field":"vibration","type":"number","unit":"mm/s","description":"设备振动幅度"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_MotorTemp', '电机温度', 'metaforge:1.0.0.common.LE_Telemetry', 'metaforge:1.0.0.common.Attribute',
  '{"field":"motor_temp","type":"number","unit":"°C","description":"电机运行温度"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_Runtime', '累计运行时长', 'metaforge:1.0.0.common.LE_Telemetry', 'metaforge:1.0.0.common.Attribute',
  '{"field":"runtime_h","type":"number","unit":"h","description":"设备累计运行小时数"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.4 工单业务对象（L3 = 维护工单表）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.BO_WorkOrder', '工单业务对象', 'metaforge:1.0.0.common.Domain_EquipmentMaintenance', 'metaforge:1.0.0.common.BusinessObject',
  '{"object_type":"TABLE","logical_entity":"metaforge:1.0.0.common.LE_WorkOrder","description":"设备维护工单表"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.LE_WorkOrder', '工单逻辑实体', 'metaforge:1.0.0.common.BO_WorkOrder', 'metaforge:1.0.0.common.LogicalEntity',
  '{"table_name":"maintenance_work_order","description":"维护工单的逻辑落地"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.5 工单字段（L5）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_WorkOrderId', '工单号', 'metaforge:1.0.0.common.LE_WorkOrder', 'metaforge:1.0.0.common.Attribute',
  '{"field":"work_order_id","type":"string","description":"工单唯一编号"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_Priority', '优先级', 'metaforge:1.0.0.common.LE_WorkOrder', 'metaforge:1.0.0.common.Attribute',
  '{"field":"priority","type":"string","enum":["LOW","MEDIUM","HIGH","CRITICAL"],"description":"工单优先级"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_Status', '工单状态', 'metaforge:1.0.0.common.LE_WorkOrder', 'metaforge:1.0.0.common.Attribute',
  '{"field":"status","type":"string","enum":["OPEN","IN_PROGRESS","DONE"],"description":"工单流转状态"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 2. 域树关系 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_IndGroupToEquipment', 'IndustrialGroupContainsEquipment', 'metaforge:1.0.0.common.Group_Industrial', 'metaforge:1.0.0.common.Domain_EquipmentMaintenance', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_EquipToTelemetry', 'EquipmentContainsTelemetry', 'metaforge:1.0.0.common.Domain_EquipmentMaintenance', 'metaforge:1.0.0.common.BO_Telemetry', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_EquipToWorkOrder', 'EquipmentContainsWorkOrder', 'metaforge:1.0.0.common.Domain_EquipmentMaintenance', 'metaforge:1.0.0.common.BO_WorkOrder', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 遥测 BO → LE → 字段
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_TelemetryToLE', 'TelemetryRefinesLE', 'metaforge:1.0.0.common.BO_Telemetry', 'metaforge:1.0.0.common.LE_Telemetry', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_TLEtoAsset', 'TLEContainsAsset', 'metaforge:1.0.0.common.LE_Telemetry', 'metaforge:1.0.0.common.Attr_AssetId', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_TLEtoVib', 'TLEContainsVibration', 'metaforge:1.0.0.common.LE_Telemetry', 'metaforge:1.0.0.common.Attr_Vibration', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_TLEtoTemp', 'TLEContainsMotorTemp', 'metaforge:1.0.0.common.LE_Telemetry', 'metaforge:1.0.0.common.Attr_MotorTemp', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_TLEtoRuntime', 'TLEContainsRuntime', 'metaforge:1.0.0.common.LE_Telemetry', 'metaforge:1.0.0.common.Attr_Runtime', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 工单 BO → LE → 字段
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_WOToLE', 'WorkOrderRefinesLE', 'metaforge:1.0.0.common.BO_WorkOrder', 'metaforge:1.0.0.common.LE_WorkOrder', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_WLEtoId', 'WLEContainsWOId', 'metaforge:1.0.0.common.LE_WorkOrder', 'metaforge:1.0.0.common.Attr_WorkOrderId', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_WLEtoPriority', 'WLEContainsPriority', 'metaforge:1.0.0.common.LE_WorkOrder', 'metaforge:1.0.0.common.Attr_Priority', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_WLEtoStatus', 'WLEContainsStatus', 'metaforge:1.0.0.common.LE_WorkOrder', 'metaforge:1.0.0.common.Attr_Status', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 域 → Agent / 主任务
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_EquipToAgent', 'EquipmentComposesAgent', 'metaforge:1.0.0.common.Domain_EquipmentMaintenance', 'metaforge:1.0.0.agent.Agent_MaintenanceAgent', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesAgent', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_EquipToMainTask', 'EquipmentComposesMaintenanceTask', 'metaforge:1.0.0.common.Domain_EquipmentMaintenance', 'metaforge:1.0.0.agent.Task_PredictiveMaintenance', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 3. Agent / 主任务 / 步骤 / 决策 实体 ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Agent_MaintenanceAgent', '设备维护Agent', NULL, 'metaforge:1.0.0.agent.Agent',
  '{"run_mode":"AUTONOMOUS","description":"负责设备遥测采集、健康度评估与维护处置"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_PredictiveMaintenance', '预测性维护任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"HIGH","estimated_complexity":"COMPLEX","dataLocation":"examples/example-industrial-predictive-maintenance/data/iot/"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_CollectTelemetry', '采集遥测', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY","capability_required":true}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_HealthAssess', '健康度评估决策', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"按振动/温度两级阈值判定设备健康度","recommended_option":"超临界告警升热线，超警告升常规维修，否则标记正常","rationale":"按 MANDATORY 与 RECOMMENDED 两级阈值分流处置","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_MarkHealthy', '标记正常', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"EXIT"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 常规维修子任务
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_RoutineRepair', '常规维修子任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"MEDIUM","estimated_complexity":"MODERATE"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_Diagnose', '故障诊断', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_CreateWorkOrder', '创建工单', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_AssignTechnician', '指派技术员', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 紧急升级子任务
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_HotlineEscalate', '紧急升级子任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"CRITICAL","estimated_complexity":"MODERATE"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_EmergencyStop', '紧急停机', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_EscalateHotline', '热线升级', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_GenerateAlarm', '生成告警', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 4. 规则（两级阈值） ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_TempCritical', '电机温度临界', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"motor_temp > 85°C","action":"must_emergency_stop","exception":null,"applicable_scenarios":["健康度评估"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_VibCritical', '振动临界', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"vibration > 4.5mm/s","action":"must_emergency_stop","exception":null,"applicable_scenarios":["健康度评估"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_TempWarning', '电机温度警告', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"RECOMMENDED","condition":"motor_temp > 75°C","action":"should_schedule_repair","exception":null,"applicable_scenarios":["健康度评估"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_VibWarning', '振动警告', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"RECOMMENDED","condition":"vibration > 3.5mm/s","action":"should_schedule_repair","exception":null,"applicable_scenarios":["健康度评估"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 5. 能力 + 协议 ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_TelemetryCollect', '遥测采集MCP', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"MCP","tool":"collect_telemetry","params":["asset_id","metric"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_WorkOrderSystem', '工单系统API', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"HTTP","endpoint":"/api/workorder","method":"POST","params":["asset_id","priority","assignee"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_AlarmPush', '告警推送', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"MCP","tool":"push_alarm","params":["severity","asset_id","message"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.McpTool_TelemetryCollect', '遥测采集工具', NULL, 'metaforge:1.0.0.protocol.McpTool',
  '{"server":"iot-mcp","tool_name":"collect_telemetry","input_schema":["asset_id","metric"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.Http_WorkOrderSystem', '工单系统接口', NULL, 'metaforge:1.0.0.protocol.Http',
  '{"endpoint":"/api/workorder","method":"POST","content_type":"application/json"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.McpTool_AlarmPush', '告警推送工具', NULL, 'metaforge:1.0.0.protocol.McpTool',
  '{"server":"iot-mcp","tool_name":"push_alarm","input_schema":["severity","asset_id","message"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 6. 任务 → 业务对象（TaskProcessesBusinessObject） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_MainTaskToTelemetry', 'MaintenanceProcessesTelemetry', 'metaforge:1.0.0.agent.Task_PredictiveMaintenance', 'metaforge:1.0.0.common.BO_Telemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.TaskProcessesBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RepairToWorkOrder', 'RoutineRepairProcessesWorkOrder', 'metaforge:1.0.0.agent.Task_RoutineRepair', 'metaforge:1.0.0.common.BO_WorkOrder', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.TaskProcessesBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 7. 规则 → 字段（RuleConstrainsAttribute） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_TempCritToAttrTemp', 'TempCritConstrainsMotorTemp', 'metaforge:1.0.0.agent.Rule_TempCritical', 'metaforge:1.0.0.common.Attr_MotorTemp', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_VibCritToAttrVib', 'VibCritConstrainsVibration', 'metaforge:1.0.0.agent.Rule_VibCritical', 'metaforge:1.0.0.common.Attr_Vibration', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_TempWarnToAttrTemp', 'TempWarnConstrainsMotorTemp', 'metaforge:1.0.0.agent.Rule_TempWarning', 'metaforge:1.0.0.common.Attr_MotorTemp', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_VibWarnToAttrVib', 'VibWarnConstrainsVibration', 'metaforge:1.0.0.agent.Rule_VibWarning', 'metaforge:1.0.0.common.Attr_Vibration', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 8. 流程后继 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_MainTaskToCollect', 'MaintenanceComposesCollect', 'metaforge:1.0.0.agent.Task_PredictiveMaintenance', 'metaforge:1.0.0.agent.Step_CollectTelemetry', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_CollectToAssess', 'CollectToHealthAssess', 'metaforge:1.0.0.agent.Step_CollectTelemetry', 'metaforge:1.0.0.agent.DecisionStep_HealthAssess', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextDecisionStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 健康度评估 → 紧急升级（临界，fqn 字母序在前=主路径）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AssessToEscalate', 'AssessToEscalate', 'metaforge:1.0.0.agent.DecisionStep_HealthAssess', 'metaforge:1.0.0.agent.Task_HotlineEscalate', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextTask', '{"condition":"温度>85 或 振动>4.5"}')
ON CONFLICT (fqn) DO NOTHING;

-- 健康度评估 → 常规维修（警告）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AssessToRepair', 'AssessToRoutineRepair', 'metaforge:1.0.0.agent.DecisionStep_HealthAssess', 'metaforge:1.0.0.agent.Task_RoutineRepair', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextTask', '{"condition":"温度>75 或 振动>3.5"}')
ON CONFLICT (fqn) DO NOTHING;

-- 健康度评估 → 标记正常
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AssessToHealthy', 'AssessToMarkHealthy', 'metaforge:1.0.0.agent.DecisionStep_HealthAssess', 'metaforge:1.0.0.agent.Step_MarkHealthy', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"正常"}')
ON CONFLICT (fqn) DO NOTHING;

-- 常规维修子任务内部
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RepairToDiagnose', 'RepairComposesDiagnose', 'metaforge:1.0.0.agent.Task_RoutineRepair', 'metaforge:1.0.0.agent.Step_Diagnose', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DiagnoseToWorkOrder', 'DiagnoseToCreateWorkOrder', 'metaforge:1.0.0.agent.Step_Diagnose', 'metaforge:1.0.0.agent.Step_CreateWorkOrder', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_WorkOrderToAssign', 'WorkOrderToAssignTechnician', 'metaforge:1.0.0.agent.Step_CreateWorkOrder', 'metaforge:1.0.0.agent.Step_AssignTechnician', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":2}')
ON CONFLICT (fqn) DO NOTHING;

-- 紧急升级子任务内部
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_EscalateToStop', 'EscalateComposesEmergencyStop', 'metaforge:1.0.0.agent.Task_HotlineEscalate', 'metaforge:1.0.0.agent.Step_EmergencyStop', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StopToHotline', 'StopToEscalateHotline', 'metaforge:1.0.0.agent.Step_EmergencyStop', 'metaforge:1.0.0.agent.Step_EscalateHotline', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_HotlineToAlarm', 'HotlineToGenerateAlarm', 'metaforge:1.0.0.agent.Step_EscalateHotline', 'metaforge:1.0.0.agent.Step_GenerateAlarm', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":2}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 9. 规则 → 步骤（RuleAppliesTo） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_TempCritToCollect', 'TempCritAppliesToCollect', 'metaforge:1.0.0.agent.Rule_TempCritical', 'metaforge:1.0.0.agent.Step_CollectTelemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_VibCritToCollect', 'VibCritAppliesToCollect', 'metaforge:1.0.0.agent.Rule_VibCritical', 'metaforge:1.0.0.agent.Step_CollectTelemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_TempWarnToCollect', 'TempWarnAppliesToCollect', 'metaforge:1.0.0.agent.Rule_TempWarning', 'metaforge:1.0.0.agent.Step_CollectTelemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_VibWarnToCollect', 'VibWarnAppliesToCollect', 'metaforge:1.0.0.agent.Rule_VibWarning', 'metaforge:1.0.0.agent.Step_CollectTelemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 10. 能力关联 + 协议实现 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_CollectToCapTelemetry', 'CollectUsesTelemetryCap', 'metaforge:1.0.0.agent.Step_CollectTelemetry', 'metaforge:1.0.0.agent.Cap_TelemetryCollect', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_WorkOrderToCapWO', 'CreateWorkOrderUsesWO', 'metaforge:1.0.0.agent.Step_CreateWorkOrder', 'metaforge:1.0.0.agent.Cap_WorkOrderSystem', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AssignToCapWO', 'AssignUsesWorkOrderCap', 'metaforge:1.0.0.agent.Step_AssignTechnician', 'metaforge:1.0.0.agent.Cap_WorkOrderSystem', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StopToCapAlarm', 'EmergencyStopUsesAlarm', 'metaforge:1.0.0.agent.Step_EmergencyStop', 'metaforge:1.0.0.agent.Cap_AlarmPush', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AlarmToCapAlarm', 'GenerateAlarmUsesAlarm', 'metaforge:1.0.0.agent.Step_GenerateAlarm', 'metaforge:1.0.0.agent.Cap_AlarmPush', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AgentToCapTelemetry', 'AgentHasTelemetryCap', 'metaforge:1.0.0.agent.Agent_MaintenanceAgent', 'metaforge:1.0.0.agent.Cap_TelemetryCollect', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AgentToCapWO', 'AgentHasWorkOrderCap', 'metaforge:1.0.0.agent.Agent_MaintenanceAgent', 'metaforge:1.0.0.agent.Cap_WorkOrderSystem', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AgentToCapAlarm', 'AgentHasAlarmCap', 'metaforge:1.0.0.agent.Agent_MaintenanceAgent', 'metaforge:1.0.0.agent.Cap_AlarmPush', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AgentToMainTask', 'AgentExecutesMaintenance', 'metaforge:1.0.0.agent.Agent_MaintenanceAgent', 'metaforge:1.0.0.agent.Task_PredictiveMaintenance', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentExecutesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_TelemetryToMcp', 'TelemetryImplementsMcp', 'metaforge:1.0.0.agent.Cap_TelemetryCollect', 'metaforge:1.0.0.protocol.McpTool_TelemetryCollect', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsMcpTool', '{"protocol_type":"McpTool"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_WOToHttp', 'WorkOrderImplementsHttp', 'metaforge:1.0.0.agent.Cap_WorkOrderSystem', 'metaforge:1.0.0.protocol.Http_WorkOrderSystem', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsHttp', '{"protocol_type":"Http"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_AlarmToMcp', 'AlarmImplementsMcp', 'metaforge:1.0.0.agent.Cap_AlarmPush', 'metaforge:1.0.0.protocol.McpTool_AlarmPush', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsMcpTool', '{"protocol_type":"McpTool"}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 11. 实体关系索引（工业域新关系） ----------

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT source_entity_fqn, 'OUTBOUND', fqn FROM semantic_relation_network.relation_instance
WHERE fqn LIKE 'metaforge:1.0.0.common.Rel_Ind%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_Equip%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_Telemetry%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_TLE%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_WO%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_WLE%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_EquipTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_MainTask%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_RepairTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_TempCrit%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_VibCrit%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_TempWarn%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_VibWarn%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_CollectTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_AssessTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_DiagnoseTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_WorkOrderTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_EscalateTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_StopTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_HotlineTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_AgentTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_AlarmTo%'
   OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_TelemetryTo%' OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_WOTo%'
   OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_AlarmTo%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT target_entity_fqn, 'INBOUND', fqn FROM semantic_relation_network.relation_instance
WHERE fqn LIKE 'metaforge:1.0.0.common.Rel_Ind%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_Equip%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_Telemetry%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_TLE%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_WO%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_WLE%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_EquipTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_MainTask%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_RepairTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_TempCrit%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_VibCrit%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_TempWarn%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_VibWarn%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_CollectTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_AssessTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_DiagnoseTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_WorkOrderTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_EscalateTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_StopTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_HotlineTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_AgentTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_AlarmTo%'
   OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_TelemetryTo%' OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_WOTo%'
   OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_AlarmTo%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

-- ---------- 12. 实体描述补充 ----------

UPDATE metadata_management.metadata_entity SET description = '工业域组——工业制造与设备运维域分组' WHERE fqn = 'metaforge:1.0.0.common.Group_Industrial';
UPDATE metadata_management.metadata_entity SET description = '设备维护域——工业设备预测性维护与工单流转' WHERE fqn = 'metaforge:1.0.0.common.Domain_EquipmentMaintenance';
UPDATE metadata_management.metadata_entity SET description = '遥测业务对象——设备传感器遥测读数表' WHERE fqn = 'metaforge:1.0.0.common.BO_Telemetry';
UPDATE metadata_management.metadata_entity SET description = '遥测逻辑实体——传感器遥测读数逻辑落地，数据源 data/iot/' WHERE fqn = 'metaforge:1.0.0.common.LE_Telemetry';
UPDATE metadata_management.metadata_entity SET description = '工单业务对象——设备维护工单表' WHERE fqn = 'metaforge:1.0.0.common.BO_WorkOrder';
UPDATE metadata_management.metadata_entity SET description = '工单逻辑实体——维护工单逻辑落地' WHERE fqn = 'metaforge:1.0.0.common.LE_WorkOrder';
UPDATE metadata_management.metadata_entity SET description = '设备维护Agent——负责遥测采集、健康度评估与维护处置' WHERE fqn = 'metaforge:1.0.0.agent.Agent_MaintenanceAgent';
UPDATE metadata_management.metadata_entity SET description = '预测性维护任务——采集遥测→健康度评估→紧急/常规/正常分流处置' WHERE fqn = 'metaforge:1.0.0.agent.Task_PredictiveMaintenance';
UPDATE metadata_management.metadata_entity SET description = '采集遥测——入口步骤，采集设备传感器读数' WHERE fqn = 'metaforge:1.0.0.agent.Step_CollectTelemetry';
UPDATE metadata_management.metadata_entity SET description = '健康度评估决策——按两级阈值判定设备健康度并分流' WHERE fqn = 'metaforge:1.0.0.agent.DecisionStep_HealthAssess';
UPDATE metadata_management.metadata_entity SET description = '标记正常——出口步骤，设备健康则标记正常' WHERE fqn = 'metaforge:1.0.0.agent.Step_MarkHealthy';
UPDATE metadata_management.metadata_entity SET description = '常规维修子任务——创建工单并指派技术员' WHERE fqn = 'metaforge:1.0.0.agent.Task_RoutineRepair';
UPDATE metadata_management.metadata_entity SET description = '紧急升级子任务——紧急停机、热线升级并生成告警' WHERE fqn = 'metaforge:1.0.0.agent.Task_HotlineEscalate';
UPDATE metadata_management.metadata_entity SET description = '电机温度临界——温度>85°C 必须紧急停机' WHERE fqn = 'metaforge:1.0.0.agent.Rule_TempCritical';
UPDATE metadata_management.metadata_entity SET description = '振动临界——振动>4.5mm/s 必须紧急停机' WHERE fqn = 'metaforge:1.0.0.agent.Rule_VibCritical';
UPDATE metadata_management.metadata_entity SET description = '电机温度警告——温度>75°C 应安排常规维修' WHERE fqn = 'metaforge:1.0.0.agent.Rule_TempWarning';
UPDATE metadata_management.metadata_entity SET description = '振动警告——振动>3.5mm/s 应安排常规维修' WHERE fqn = 'metaforge:1.0.0.agent.Rule_VibWarning';

-- ---------- 完成 ----------

-- ============================================================
-- test4 增强（v2）：决策嵌套 + 运行时长强制规则 + 工单 SLA
-- 新增：
--   1. DecisionStep_FaultType（常规维修内故障类型判断，决策嵌套）
--      温度主导→电气故障→Step_RestartAndTest；振动主导→机械故障→Step_ReplacePart
--   2. Rule_RuntimeForce（运行>2000h 强制维护，任务级规则，遥测正常也触发）
--   3. Rule_SlaCritical（CRITICAL 工单须 2h 内处理，约束工单状态字段）
-- ============================================================

-- ---------- A. 新增实体 ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_FaultType', '故障类型判断', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"温度异常主导判定电气故障，振动异常主导判定机械故障","recommended_option":"电气故障重启测试，机械故障更换备件","rationale":"温度过高通常源于电气/散热，振动异常通常源于机械磨损","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_RestartAndTest', '重启测试', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP","description":"电气故障处置——断电重启并测试"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_ReplacePart', '更换备件', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP","description":"机械故障处置——更换磨损部件"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_RuntimeForce', '运行时长强制维护', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"runtime_h > 2000","action":"must_schedule_repair","exception":"停机大修中","applicable_scenarios":["健康度评估","定期点检"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_SlaCritical', '紧急工单SLA', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"priority == CRITICAL 时须 2 小时内开始处理","action":"must_handle_within_2h","exception":null,"applicable_scenarios":["工单流转"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- B. 决策嵌套：常规维修内部 ----------

-- 诊断 → 故障类型判断
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DiagnoseToFaultType', 'DiagnoseToFaultType', 'metaforge:1.0.0.agent.Step_Diagnose', 'metaforge:1.0.0.agent.DecisionStep_FaultType', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextDecisionStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 故障类型 → 电气（温度主导，重启测试，fqn 字母序 E<M → 电气在前=主路径）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_FaultToRestart', 'FaultToRestartAndTest', 'metaforge:1.0.0.agent.DecisionStep_FaultType', 'metaforge:1.0.0.agent.Step_RestartAndTest', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"温度异常主导（电气故障）"}')
ON CONFLICT (fqn) DO NOTHING;

-- 故障类型 → 机械（振动主导，更换备件）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_FaultToReplace', 'FaultToReplacePart', 'metaforge:1.0.0.agent.DecisionStep_FaultType', 'metaforge:1.0.0.agent.Step_ReplacePart', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"振动异常主导（机械故障）"}')
ON CONFLICT (fqn) DO NOTHING;

-- 电气处置 → 创建工单
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RestartToWorkOrder', 'RestartToCreateWorkOrder', 'metaforge:1.0.0.agent.Step_RestartAndTest', 'metaforge:1.0.0.agent.Step_CreateWorkOrder', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 机械处置 → 创建工单
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReplaceToWorkOrder', 'ReplaceToCreateWorkOrder', 'metaforge:1.0.0.agent.Step_ReplacePart', 'metaforge:1.0.0.agent.Step_CreateWorkOrder', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- C. 运行时长强制规则（任务级 + 字段级 + 步骤级） ----------

-- Rule_RuntimeForce → Attr_Runtime（字段级）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RuntimeToAttr', 'RuntimeForceConstrainsRuntime', 'metaforge:1.0.0.agent.Rule_RuntimeForce', 'metaforge:1.0.0.common.Attr_Runtime', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- Rule_RuntimeForce → Task（任务级，RuleAppliesToTask）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RuntimeToTask', 'RuntimeForceAppliesToTask', 'metaforge:1.0.0.agent.Rule_RuntimeForce', 'metaforge:1.0.0.agent.Task_PredictiveMaintenance', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesToTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- Rule_RuntimeForce → Step_CollectTelemetry（步骤级）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RuntimeToCollect', 'RuntimeForceAppliesToCollect', 'metaforge:1.0.0.agent.Rule_RuntimeForce', 'metaforge:1.0.0.agent.Step_CollectTelemetry', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- D. 工单 SLA 规则（字段级 + 步骤级） ----------

-- Rule_SlaCritical → Attr_Status（约束工单状态字段）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_SlaToStatus', 'SlaConstrainsStatus', 'metaforge:1.0.0.agent.Rule_SlaCritical', 'metaforge:1.0.0.common.Attr_Status', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- Rule_SlaCritical → Step_CreateWorkOrder
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_SlaToWorkOrder', 'SlaAppliesToCreateWorkOrder', 'metaforge:1.0.0.agent.Rule_SlaCritical', 'metaforge:1.0.0.agent.Step_CreateWorkOrder', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- E. 增强关系索引 ----------

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT source_entity_fqn, 'OUTBOUND', fqn FROM semantic_relation_network.relation_instance
WHERE fqn LIKE 'metaforge:1.0.0.agent.Rel_DiagnoseToFault%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_FaultTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_RestartTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReplaceTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_RuntimeTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_SlaTo%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT target_entity_fqn, 'INBOUND', fqn FROM semantic_relation_network.relation_instance
WHERE fqn LIKE 'metaforge:1.0.0.agent.Rel_DiagnoseToFault%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_FaultTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_RestartTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReplaceTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_RuntimeTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_SlaTo%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

-- ---------- F. 增强实体描述 ----------

UPDATE metadata_management.metadata_entity SET description = '故障类型判断——温度主导判电气、振动主导判机械' WHERE fqn = 'metaforge:1.0.0.agent.DecisionStep_FaultType';
UPDATE metadata_management.metadata_entity SET description = '重启测试——电气故障处置，断电重启并测试' WHERE fqn = 'metaforge:1.0.0.agent.Step_RestartAndTest';
UPDATE metadata_management.metadata_entity SET description = '更换备件——机械故障处置，更换磨损部件' WHERE fqn = 'metaforge:1.0.0.agent.Step_ReplacePart';
UPDATE metadata_management.metadata_entity SET description = '运行时长强制维护——运行>2000h 即使遥测正常也强制安排维护' WHERE fqn = 'metaforge:1.0.0.agent.Rule_RuntimeForce';
UPDATE metadata_management.metadata_entity SET description = '紧急工单SLA——CRITICAL 工单须 2 小时内开始处理' WHERE fqn = 'metaforge:1.0.0.agent.Rule_SlaCritical';

-- ---------- 完成 ----------
