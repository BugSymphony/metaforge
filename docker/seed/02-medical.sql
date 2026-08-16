-- ============================================================
-- test3 医疗处方审核域 seed
-- 核心：验证 L3-L5 业务对象结构（表结构）作为元数据存 MetaForge，
--       任务与业务对象、规则与字段通过 agent 库关系（TaskProcessesBusinessObject /
--       RuleConstrainsAttribute）建立关联；业务数据（处方实例）独立放文件。
--
-- 域树 + 业务对象结构：
--   Domain_Healthcare (L2 SubjectDomain)
--     ├─ BO_Prescription (L3 BusinessObject = 处方表)
--     │    └─ LE_Prescription (L4 LogicalEntity)
--     │         ├─ Attr_Drug (L5, 药物)
--     │         ├─ Attr_DoseMg (L5, 单次剂量)
--     │         └─ Attr_ConcurrentDrugs (L5, 并用药物)
--     ├─ Agent_RxAgent (药剂审核Agent)
--     └─ Task_RxReview (处方审核任务)
--          ├─ TaskProcessesBusinessObject → BO_Prescription
--          ├─ 流程：Step_ReadRx(ENTRY) → DecisionStep_DosageCheck → Step_InteractionCheck → Step_ConcludeReview(EXIT)
--          │       DecisionStep_DosageCheck →[剂量超限]→ Task_RxRecheck(药师复核子任务)
--          ├─ 规则：Rule_DosageLimit →[RuleConstrainsAttribute]→ Attr_DoseMg
--          │        Rule_Interaction →[RuleConstrainsAttribute]→ Attr_ConcurrentDrugs
--          └─ 能力：Cap_DrugDB / Cap_RxReader
-- ============================================================

-- ---------- 1. 业务对象结构（L3-L5 表结构元数据） ----------

-- 1.0 医疗域组（L1）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Group_Medical', '医疗域组', NULL, 'metaforge:1.0.0.common.SubjectDomainGroup',
  '{"group_level":1,"description":"医疗健康域分组"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.1 医疗域（L2）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Domain_Healthcare', '医疗审核域', 'metaforge:1.0.0.common.Group_Medical', 'metaforge:1.0.0.common.SubjectDomain',
  '{"domain_level":2,"description":"处方审核与用药安全"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.2 处方业务对象（L3 = 表）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.BO_Prescription', '处方业务对象', 'metaforge:1.0.0.common.Domain_Healthcare', 'metaforge:1.0.0.common.BusinessObject',
  '{"object_type":"TABLE","logical_entity":"metaforge:1.0.0.common.LE_Prescription","description":"处方表——一次开方记录的语义结构"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.3 处方逻辑实体（L4 = 表/视图）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.LE_Prescription', '处方逻辑实体', 'metaforge:1.0.0.common.BO_Prescription', 'metaforge:1.0.0.common.LogicalEntity',
  '{"table_name":"prescription","source":"examples/example-medical-prescription-review/data/prescriptions/","description":"处方表的逻辑落地"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.4 字段（L5 = 属性）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_Drug', '药物', 'metaforge:1.0.0.common.LE_Prescription', 'metaforge:1.0.0.common.Attribute',
  '{"field":"drug","type":"string","description":"处方药物名称"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_DoseMg', '单次剂量', 'metaforge:1.0.0.common.LE_Prescription', 'metaforge:1.0.0.common.Attribute',
  '{"field":"single_dose_mg","type":"number","unit":"mg","description":"单次给药剂量（毫克）"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_ConcurrentDrugs', '并用药物', 'metaforge:1.0.0.common.LE_Prescription', 'metaforge:1.0.0.common.Attribute',
  '{"field":"concurrent_drugs","type":"string[]","description":"处方中并用的其他药物列表"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 2. 域树 → Agent / 任务 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_MedGroupToHealth', 'MedicalGroupContainsHealthcare', 'metaforge:1.0.0.common.Group_Medical', 'metaforge:1.0.0.common.Domain_Healthcare', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_HealthToBOPresc', 'HealthcareContainsPrescription', 'metaforge:1.0.0.common.Domain_Healthcare', 'metaforge:1.0.0.common.BO_Prescription', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 业务对象 → 逻辑实体 → 属性
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_BOToLE', 'PrescriptionRefinesLE', 'metaforge:1.0.0.common.BO_Prescription', 'metaforge:1.0.0.common.LE_Prescription', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_LEToDrug', 'LEContainsDrug', 'metaforge:1.0.0.common.LE_Prescription', 'metaforge:1.0.0.common.Attr_Drug', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_LEToDose', 'LEContainsDose', 'metaforge:1.0.0.common.LE_Prescription', 'metaforge:1.0.0.common.Attr_DoseMg', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_LEToConcurrent', 'LEContainsConcurrent', 'metaforge:1.0.0.common.LE_Prescription', 'metaforge:1.0.0.common.Attr_ConcurrentDrugs', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 域 → Agent / 任务
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_HealthToAgent', 'HealthcareComposesRxAgent', 'metaforge:1.0.0.common.Domain_Healthcare', 'metaforge:1.0.0.agent.Agent_RxAgent', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesAgent', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_HealthToRxTask', 'HealthcareComposesRxReview', 'metaforge:1.0.0.common.Domain_Healthcare', 'metaforge:1.0.0.agent.Task_RxReview', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 3. Agent / 任务 / 步骤 / 决策 实体 ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Agent_RxAgent', '药剂审核Agent', NULL, 'metaforge:1.0.0.agent.Agent',
  '{"run_mode":"AUTONOMOUS","description":"负责处方审核与用药安全校验"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_RxReview', '处方审核任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"HIGH","estimated_complexity":"COMPLEX","dataLocation":"examples/example-medical-prescription-review/data/prescriptions/"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_ReadRx', '读取处方', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY","capability_required":true}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_DosageCheck', '剂量校验决策', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"单次剂量是否超过药品说明书上限","recommended_option":"超限则转药师复核","rationale":"剂量超限有用药安全风险","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_InteractionCheck', '相互作用检查', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_ConcludeReview', '审核结论', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"EXIT"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_RxRecheck', '药师复核子任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"HIGH","estimated_complexity":"SIMPLE"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_RxRecheckReview', '人工复核', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 4. 规则 / 能力 实体 ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_DosageLimit', '单次剂量上限', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"single_dose_mg > 500 且 drug == acetaminophen","action":"转药师复核","exception":"肿瘤镇痛临床路径","applicable_scenarios":["处方审核"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_Interaction', '药物相互作用规则', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"concurrent_drugs 含相互作用药物对（如 warfarin+aspirin）","action":"必须人工复核","exception":null,"applicable_scenarios":["处方审核"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_DrugDB', '药品数据库API', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"HTTP","endpoint":"/api/drug/db/check","method":"POST","params":["drug","dose_mg","concurrent_drugs"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_RxReader', '处方读取MCP', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"MCP","tool":"read_prescription","params":["rx_id"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 协议
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.Http_DrugDB', '药品数据库接口', NULL, 'metaforge:1.0.0.protocol.Http',
  '{"endpoint":"/api/drug/db/check","method":"POST","content_type":"application/json"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.McpTool_RxReader', '处方读取工具', NULL, 'metaforge:1.0.0.protocol.McpTool',
  '{"server":"health-mcp","tool_name":"read_prescription","input_schema":["rx_id"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 5. 任务 → 业务对象（TaskProcessesBusinessObject 新关系） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RxTaskToBO', 'RxReviewProcessesPrescription', 'metaforge:1.0.0.agent.Task_RxReview', 'metaforge:1.0.0.common.BO_Prescription', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.TaskProcessesBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 6. 规则 → 字段（RuleConstrainsAttribute 新关系） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DosageToAttrDose', 'DosageRuleConstrainsDoseAttr', 'metaforge:1.0.0.agent.Rule_DosageLimit', 'metaforge:1.0.0.common.Attr_DoseMg', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InteractToAttrConcurrent', 'InteractionRuleConstrainsConcurrentAttr', 'metaforge:1.0.0.agent.Rule_Interaction', 'metaforge:1.0.0.common.Attr_ConcurrentDrugs', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 7. 流程后继 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RxTaskToRead', 'RxReviewComposesReadRx', 'metaforge:1.0.0.agent.Task_RxReview', 'metaforge:1.0.0.agent.Step_ReadRx', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RxReadToDosage', 'ReadRxToDosageCheck', 'metaforge:1.0.0.agent.Step_ReadRx', 'metaforge:1.0.0.agent.DecisionStep_DosageCheck', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextDecisionStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 剂量校验决策 → 相互作用检查（正常分支，fqn 字母序前 = 主路径）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RxDosageToInteraction', 'DosageToInteractionCheck', 'metaforge:1.0.0.agent.DecisionStep_DosageCheck', 'metaforge:1.0.0.agent.Step_InteractionCheck', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"剂量正常"}')
ON CONFLICT (fqn) DO NOTHING;

-- 剂量校验决策 → 药师复核子任务（超限分支）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RxDosageToRecheck', 'DosageToRxRecheck', 'metaforge:1.0.0.agent.DecisionStep_DosageCheck', 'metaforge:1.0.0.agent.Task_RxRecheck', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextTask', '{"condition":"剂量超限"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RxInteractionToConclude', 'InteractionToConclude', 'metaforge:1.0.0.agent.Step_InteractionCheck', 'metaforge:1.0.0.agent.Step_ConcludeReview', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 药师复核子任务入口
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RxRecheckToReview', 'RxRecheckComposesReview', 'metaforge:1.0.0.agent.Task_RxRecheck', 'metaforge:1.0.0.agent.Step_RxRecheckReview', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 8. 规则 → 步骤（RuleAppliesTo） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DosageRuleToRead', 'DosageRuleAppliesToReadRx', 'metaforge:1.0.0.agent.Rule_DosageLimit', 'metaforge:1.0.0.agent.Step_ReadRx', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InteractRuleToStep', 'InteractionRuleAppliesToStep', 'metaforge:1.0.0.agent.Rule_Interaction', 'metaforge:1.0.0.agent.Step_InteractionCheck', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 9. 能力关联 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReadToCapRx', 'ReadRxUsesRxReader', 'metaforge:1.0.0.agent.Step_ReadRx', 'metaforge:1.0.0.agent.Cap_RxReader', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InteractToCapDrug', 'InteractionUsesDrugDB', 'metaforge:1.0.0.agent.Step_InteractionCheck', 'metaforge:1.0.0.agent.Cap_DrugDB', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RxAgentToCapDrug', 'RxAgentHasDrugDB', 'metaforge:1.0.0.agent.Agent_RxAgent', 'metaforge:1.0.0.agent.Cap_DrugDB', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RxAgentToCapRx', 'RxAgentHasRxReader', 'metaforge:1.0.0.agent.Agent_RxAgent', 'metaforge:1.0.0.agent.Cap_RxReader', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RxAgentToTask', 'RxAgentExecutesRxReview', 'metaforge:1.0.0.agent.Agent_RxAgent', 'metaforge:1.0.0.agent.Task_RxReview', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentExecutesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 协议实现
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_DrugDBToHttp', 'DrugDBImplementsHttp', 'metaforge:1.0.0.agent.Cap_DrugDB', 'metaforge:1.0.0.protocol.Http_DrugDB', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsHttp', '{"protocol_type":"Http"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_RxReaderToMcp', 'RxReaderImplementsMcp', 'metaforge:1.0.0.agent.Cap_RxReader', 'metaforge:1.0.0.protocol.McpTool_RxReader', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsMcpTool', '{"protocol_type":"McpTool"}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 10. 实体关系索引（医疗域新关系） ----------

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT source_entity_fqn, 'OUTBOUND', fqn FROM semantic_relation_network.relation_instance
WHERE fqn LIKE 'metaforge:1.0.0.common.Rel_Med%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_Health%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_BO%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_LE%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_Rx%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_Dosage%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_Interact%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReadTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_HealthTo%'
   OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_Drug%' OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_Rx%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT target_entity_fqn, 'INBOUND', fqn FROM semantic_relation_network.relation_instance
WHERE fqn LIKE 'metaforge:1.0.0.common.Rel_Med%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_Health%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_BO%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_LE%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_Rx%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_Dosage%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_Interact%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReadTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_HealthTo%'
   OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_Drug%' OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_Rx%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

-- ---------- 11. 实体描述补充 ----------

UPDATE metadata_management.metadata_entity SET description = '医疗域组——医疗健康域分组' WHERE fqn = 'metaforge:1.0.0.common.Group_Medical';
UPDATE metadata_management.metadata_entity SET description = '医疗审核域——处方审核与用药安全' WHERE fqn = 'metaforge:1.0.0.common.Domain_Healthcare';
UPDATE metadata_management.metadata_entity SET description = '处方业务对象——处方表（一次开方记录的语义结构）' WHERE fqn = 'metaforge:1.0.0.common.BO_Prescription';
UPDATE metadata_management.metadata_entity SET description = '处方逻辑实体——处方表的逻辑落地，数据源 data/prescriptions/' WHERE fqn = 'metaforge:1.0.0.common.LE_Prescription';
UPDATE metadata_management.metadata_entity SET description = '药物字段——处方药物名称' WHERE fqn = 'metaforge:1.0.0.common.Attr_Drug';
UPDATE metadata_management.metadata_entity SET description = '单次剂量字段——单次给药剂量（毫克）' WHERE fqn = 'metaforge:1.0.0.common.Attr_DoseMg';
UPDATE metadata_management.metadata_entity SET description = '并用药物字段——处方中并用的其他药物列表' WHERE fqn = 'metaforge:1.0.0.common.Attr_ConcurrentDrugs';
UPDATE metadata_management.metadata_entity SET description = '药剂审核Agent——负责处方审核与用药安全校验' WHERE fqn = 'metaforge:1.0.0.agent.Agent_RxAgent';
UPDATE metadata_management.metadata_entity SET description = '处方审核任务——读取处方→剂量校验→相互作用检查→审核结论' WHERE fqn = 'metaforge:1.0.0.agent.Task_RxReview';
UPDATE metadata_management.metadata_entity SET description = '读取处方——入口步骤，读取待审核处方数据' WHERE fqn = 'metaforge:1.0.0.agent.Step_ReadRx';
UPDATE metadata_management.metadata_entity SET description = '剂量校验决策——判断单次剂量是否超药品说明书上限' WHERE fqn = 'metaforge:1.0.0.agent.DecisionStep_DosageCheck';
UPDATE metadata_management.metadata_entity SET description = '相互作用检查——检查并用药物是否冲突' WHERE fqn = 'metaforge:1.0.0.agent.Step_InteractionCheck';
UPDATE metadata_management.metadata_entity SET description = '审核结论——出口步骤，输出审核结果' WHERE fqn = 'metaforge:1.0.0.agent.Step_ConcludeReview';
UPDATE metadata_management.metadata_entity SET description = '药师复核子任务——剂量超限或相互作用时的药师人工复核' WHERE fqn = 'metaforge:1.0.0.agent.Task_RxRecheck';
UPDATE metadata_management.metadata_entity SET description = '单次剂量上限——acetaminophen 单次剂量>500mg 转药师复核' WHERE fqn = 'metaforge:1.0.0.agent.Rule_DosageLimit';
UPDATE metadata_management.metadata_entity SET description = '药物相互作用规则——并用药物冲突必须人工复核' WHERE fqn = 'metaforge:1.0.0.agent.Rule_Interaction';

-- ---------- 完成 ----------
