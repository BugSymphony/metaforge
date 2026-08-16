-- ============================================================================
-- 内置 agent 库测试数据 seed（开发/测试用，手动或脚本初始化）
-- 依赖：V4 元模型（metaforge:1.0.0）已加载（common/agent/protocol 包）
-- 覆盖多业务域闭环：订单履约域 + 支付域
--   - 主题域组成 Agent/Task
--   - Agent 组成（Profile/Permission/ExecutesTask/DelegatesTo）
--   - 能力多方分配（Agent/Task/Step → Capability）
--   - 步骤执行链（StepHasNextStep）
--   - 能力引用协议（Capability → protocol.X）
-- 执行方式：psql -h localhost -U metaforge -d metaforge -f agent-library-seed.sql
-- ============================================================================

-- ============================================================
-- 一、common 主题域实例
-- ============================================================

-- L1 主题域分组
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Group_Fulfillment', '履约域组', NULL, 'metaforge:1.0.0.common.SubjectDomainGroup',
  '{"group_level":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- L2 主题域
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Domain_Inventory', '库存管理域', 'metaforge:1.0.0.common.Group_Fulfillment', 'metaforge:1.0.0.common.SubjectDomain',
  '{"keywords":["库存","盘点","调拨"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Domain_Payment', '支付结算域', 'metaforge:1.0.0.common.Group_Fulfillment', 'metaforge:1.0.0.common.SubjectDomain',
  '{"keywords":["支付","结算","退款"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- L2→L1 分组关系
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_GroupToInventory', 'GroupToInventory', 'metaforge:1.0.0.common.Group_Fulfillment', 'metaforge:1.0.0.common.Domain_Inventory', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_GroupToPayment', 'GroupToPayment', 'metaforge:1.0.0.common.Group_Fulfillment', 'metaforge:1.0.0.common.Domain_Payment', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 二、订单履约域（库存管理）
-- ============================================================

-- 2.1 Agent 实例
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Agent_InventoryAgent', '库存管理Agent', NULL, 'metaforge:1.0.0.agent.Agent',
  '{"agent_type":"LLM_AGENT","mode":"ALL","native":false,"hidden":false,"prompt":"你是一个库存管理专家，负责盘点与补货决策。","agent_role_summary":"库存盘点与补货执行专家","agent_owner":"供应链线","team_name":"仓储自动化","is_active":true}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 2.2 Agent 认知配置（Profile）实例
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Profile_InventoryExecution', '库存执行型配置', NULL, 'metaforge:1.0.0.agent.AgentProfile',
  '{"archetype":"execution","default_cognition_depth":"L2","preferred_perspectives":["ontological","deontic"],"token_budget_default":8000}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 2.3 Agent 权限实例
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Perm_InventoryAgent', '库存Agent权限', NULL, 'metaforge:1.0.0.agent.AgentPermission',
  '{"permission_type":"TASK_DELEGATE","authority_level":"SUPERVISED","granted":true,"allowed_bundle_fqns":["metaforge:1.0.0"],"granted_by":"system"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 2.4 Task 实例
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_InventoryCheck', '库存盘点任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"entry_step_fqn":"metaforge:1.0.0.agent.Step_CheckInventory","delegation_depth_limit":1,"priority_default":"HIGH","estimated_complexity":"MODERATE"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 2.5 ExecutionStep 实例（3 步链）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_CheckInventory', '检查库存', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY","estimated_duration":"30秒","responsible_role":"仓库管理员","input_artifacts":["订单ID"],"output_artifacts":["库存数量"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_VerifyStock', '核验库存充足性', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"DECISION","estimated_duration":"10秒","responsible_role":"库存系统","input_artifacts":["库存数量"],"output_artifacts":["是否充足"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_TriggerRestock', '触发补货', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"EXIT","estimated_duration":"20秒","responsible_role":"补货系统","input_artifacts":["是否充足"],"output_artifacts":["补货单"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 2.6 Capability 实例（2 个能力）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_InventoryAPI', '库存查询API', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"Http","endpoint":"https://inventory.example.com/api/stock","method":"GET","input_schema":{"type":"object","properties":{"sku":{"type":"string"}}},"output_schema":{"type":"object","properties":{"quantity":{"type":"integer"}}}},"call_method":"REST","version_label":"v1.2.0","provider":"仓储服务"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_RestockQueue', '补货队列工具', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"McpTool","server_name":"restock-server","tool_name":"enqueue_restock","arguments_schema":{"type":"object","properties":{"sku":{"type":"string"},"quantity":{"type":"integer"}}}},"call_method":"MCP","version_label":"v1.0.0","provider":"补货服务"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 2.7 协议实例（2 个）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.Http_InventoryQuery', '库存查询接口', NULL, 'metaforge:1.0.0.protocol.Http',
  '{"endpoint":"https://inventory.example.com/api/stock","method":"GET","headers":{"Accept":"application/json"}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.McpTool_RestockQueue', '补货队列MCP工具', NULL, 'metaforge:1.0.0.protocol.McpTool',
  '{"server_name":"restock-server","arguments_schema":{"type":"object","properties":{"sku":{"type":"string"},"quantity":{"type":"integer"}}}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 2.8 ExecutionRule 实例（2 条规则）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_InventoryAboveZero', '库存必须大于零', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"库存数量 <= 0","action":"must_trigger_restock","exception":"force_majeure","applicable_scenarios":["盘点","出库"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_48hShipping', '48小时发货约束', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"RECOMMENDED","condition":"order_status=CONFIRMED","action":"must_ship_within_48h","exception":"customs_inspection","applicable_scenarios":["现货订单"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 2.9 库存域关系实例
-- 域组成 Agent/Task
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InvDomainToAgent', 'InventoryDomainComposesAgent', 'metaforge:1.0.0.common.Domain_Inventory', 'metaforge:1.0.0.agent.Agent_InventoryAgent', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesAgent', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InvDomainToTask', 'InventoryDomainComposesTask', 'metaforge:1.0.0.common.Domain_Inventory', 'metaforge:1.0.0.agent.Task_InventoryCheck', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- Agent 组成：Profile/Permission/ExecutesTask
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InvAgentToProfile', 'InvAgentUsesProfile', 'metaforge:1.0.0.agent.Agent_InventoryAgent', 'metaforge:1.0.0.agent.Profile_InventoryExecution', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentUsesProfile', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InvAgentToPerm', 'InvAgentHasPermission', 'metaforge:1.0.0.agent.Agent_InventoryAgent', 'metaforge:1.0.0.agent.Perm_InventoryAgent', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasPermission', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InvAgentToTask', 'InvAgentExecutesTask', 'metaforge:1.0.0.agent.Agent_InventoryAgent', 'metaforge:1.0.0.agent.Task_InventoryCheck', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentExecutesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 任务组成步骤（起点）+ 步骤链
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_TaskToStepCheck', 'TaskComposesStepCheck', 'metaforge:1.0.0.agent.Task_InventoryCheck', 'metaforge:1.0.0.agent.Step_CheckInventory', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 步骤链：Check → Verify → Restock
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StepCheckToVerify', 'StepCheckToVerify', 'metaforge:1.0.0.agent.Step_CheckInventory', 'metaforge:1.0.0.agent.Step_VerifyStock', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StepVerifyToRestock', 'StepVerifyToRestock', 'metaforge:1.0.0.agent.Step_VerifyStock', 'metaforge:1.0.0.agent.Step_TriggerRestock', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":2}')
ON CONFLICT (fqn) DO NOTHING;

-- 能力多方分配：Agent/Task/Step → Capability
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InvAgentToCapAPI', 'InvAgentHasCapabilityAPI', 'metaforge:1.0.0.agent.Agent_InventoryAgent', 'metaforge:1.0.0.agent.Cap_InventoryAPI', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InvTaskToCapAPI', 'InvTaskRequiresCapabilityAPI', 'metaforge:1.0.0.agent.Task_InventoryCheck', 'metaforge:1.0.0.agent.Cap_InventoryAPI', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.TaskRequiresCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StepCheckToCapAPI', 'StepCheckUsesCapabilityAPI', 'metaforge:1.0.0.agent.Step_CheckInventory', 'metaforge:1.0.0.agent.Cap_InventoryAPI', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_InvAgentToCapRestock', 'InvAgentHasCapabilityRestock', 'metaforge:1.0.0.agent.Agent_InventoryAgent', 'metaforge:1.0.0.agent.Cap_RestockQueue', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StepRestockToCapRestock', 'StepRestockUsesCapabilityRestock', 'metaforge:1.0.0.agent.Step_TriggerRestock', 'metaforge:1.0.0.agent.Cap_RestockQueue', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 能力引用协议
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_InvAPIToHttp', 'InvAPIImplementsHttp', 'metaforge:1.0.0.agent.Cap_InventoryAPI', 'metaforge:1.0.0.protocol.Http_InventoryQuery', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsHttp', '{"protocol_type":"Http"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_RestockToMcp', 'RestockImplementsMcp', 'metaforge:1.0.0.agent.Cap_RestockQueue', 'metaforge:1.0.0.protocol.McpTool_RestockQueue', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsMcpTool', '{"protocol_type":"McpTool"}')
ON CONFLICT (fqn) DO NOTHING;

-- 规则适用
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RuleAboveZeroToVerify', 'RuleAboveZeroAppliesToVerify', 'metaforge:1.0.0.agent.Rule_InventoryAboveZero', 'metaforge:1.0.0.agent.Step_VerifyStock', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_Rule48hToTrigger', 'Rule48hAppliesToTrigger', 'metaforge:1.0.0.agent.Rule_48hShipping', 'metaforge:1.0.0.agent.Step_TriggerRestock', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 三、支付域（PaymentValidation 流程闭环）
-- ============================================================

-- 3.1 Agent / Profile / Permission
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Agent_PaymentAgent', '支付处理Agent', NULL, 'metaforge:1.0.0.agent.Agent',
  '{"agent_type":"HYBRID","mode":"SUBAGENT","native":false,"hidden":false,"prompt":"你是一个支付校验与风控协调专家。","agent_role_summary":"支付校验与风控协调专家","agent_owner":"交易线","team_name":"支付自动化","is_active":true}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Profile_PaymentAudit', '支付审计型配置', NULL, 'metaforge:1.0.0.agent.AgentProfile',
  '{"archetype":"audit","default_cognition_depth":"L3","preferred_perspectives":["deontic","relational"],"token_budget_default":6000}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Perm_PaymentAgent', '支付Agent权限', NULL, 'metaforge:1.0.0.agent.AgentPermission',
  '{"permission_type":"TASK_CREATE","authority_level":"FULL_AUTONOMY","granted":true,"allowed_bundle_fqns":["metaforge:1.0.0"],"granted_by":"system"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 3.2 Task / Steps（4 步链）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_PaymentValidation', '支付校验任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"CRITICAL","estimated_complexity":"COMPLEX"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_ValidateCard', '校验支付卡', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY","estimated_duration":"15秒","responsible_role":"支付网关","input_artifacts":["卡号"],"output_artifacts":["卡有效性"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_AuthorizeAmount', '授权金额', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"PROCESSING","estimated_duration":"25秒","responsible_role":"支付网关","input_artifacts":["卡有效性","金额"],"output_artifacts":["授权码"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_CheckRisk', '风控检查', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"DECISION","estimated_duration":"40秒","responsible_role":"风控引擎","input_artifacts":["订单信息"],"output_artifacts":["风控结论"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_ConfirmPayment', '确认支付', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"EXIT","estimated_duration":"10秒","responsible_role":"支付网关","input_artifacts":["授权码","风控结论"],"output_artifacts":["支付确认"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 3.3 支付域能力（3 个）与协议（3 个）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_CardValidate', '卡校验API', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"Http","endpoint":"https://pay.example.com/api/card/validate","method":"POST","input_schema":{"type":"object","properties":{"cardNo":{"type":"string"}}},"output_schema":{"type":"object","properties":{"valid":{"type":"boolean"}}}},"call_method":"REST","provider":"支付网关"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_AmountAuthorize', '金额授权API', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"Http","endpoint":"https://pay.example.com/api/authorize","method":"POST","input_schema":{"type":"object","properties":{"amount":{"type":"number"}}},"output_schema":{"type":"object","properties":{"authCode":{"type":"string"}}}},"call_method":"REST","provider":"支付网关"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_RiskCheck', '风控检查MCP', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"McpTool","server_name":"risk-server","tool_name":"check_order_risk","arguments_schema":{"type":"object","properties":{"orderId":{"type":"string"}}}},"call_method":"MCP","provider":"风控引擎"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.Http_CardValidate', '卡校验接口', NULL, 'metaforge:1.0.0.protocol.Http',
  '{"endpoint":"https://pay.example.com/api/card/validate","method":"POST"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.Http_AmountAuthorize', '金额授权接口', NULL, 'metaforge:1.0.0.protocol.Http',
  '{"endpoint":"https://pay.example.com/api/authorize","method":"POST"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.McpTool_RiskCheck', '风控检查工具', NULL, 'metaforge:1.0.0.protocol.McpTool',
  '{"server_name":"risk-server","arguments_schema":{"type":"object","properties":{"orderId":{"type":"string"}}}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 3.4 支付域规则（2 条）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_AmountLimit', '单笔金额上限', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"金额 > 50000","action":"must_manual_review","exception":"corporate_account","applicable_scenarios":["大额支付"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_RiskFlag', '风控标记拦截', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"风控结论 = HIGH","action":"must_block_payment","exception":"approved_by_risk_manager","applicable_scenarios":["高风险支付"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 3.5 支付域关系实例
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayDomainToAgent', 'PaymentDomainComposesAgent', 'metaforge:1.0.0.common.Domain_Payment', 'metaforge:1.0.0.agent.Agent_PaymentAgent', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesAgent', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayDomainToTask', 'PaymentDomainComposesTask', 'metaforge:1.0.0.common.Domain_Payment', 'metaforge:1.0.0.agent.Task_PaymentValidation', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayAgentToProfile', 'PayAgentUsesProfile', 'metaforge:1.0.0.agent.Agent_PaymentAgent', 'metaforge:1.0.0.agent.Profile_PaymentAudit', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentUsesProfile', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayAgentToPerm', 'PayAgentHasPermission', 'metaforge:1.0.0.agent.Agent_PaymentAgent', 'metaforge:1.0.0.agent.Perm_PaymentAgent', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasPermission', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayAgentToTask', 'PayAgentExecutesTask', 'metaforge:1.0.0.agent.Agent_PaymentAgent', 'metaforge:1.0.0.agent.Task_PaymentValidation', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentExecutesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 支付域步骤链（3 关系）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayValidateToAuthorize', 'PayValidateToAuthorize', 'metaforge:1.0.0.agent.Step_ValidateCard', 'metaforge:1.0.0.agent.Step_AuthorizeAmount', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayAuthorizeToRisk', 'PayAuthorizeToRisk', 'metaforge:1.0.0.agent.Step_AuthorizeAmount', 'metaforge:1.0.0.agent.Step_CheckRisk', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":2}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayRiskToConfirm', 'PayRiskToConfirm', 'metaforge:1.0.0.agent.Step_CheckRisk', 'metaforge:1.0.0.agent.Step_ConfirmPayment', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":3}')
ON CONFLICT (fqn) DO NOTHING;

-- 支付域能力分配
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayAgentToCapValidate', 'PayAgentHasCapabilityValidate', 'metaforge:1.0.0.agent.Agent_PaymentAgent', 'metaforge:1.0.0.agent.Cap_CardValidate', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayAgentToCapAuthorize', 'PayAgentHasCapabilityAuthorize', 'metaforge:1.0.0.agent.Agent_PaymentAgent', 'metaforge:1.0.0.agent.Cap_AmountAuthorize', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StepValidateToCap', 'StepValidateUsesCapability', 'metaforge:1.0.0.agent.Step_ValidateCard', 'metaforge:1.0.0.agent.Cap_CardValidate', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StepAuthorizeToCap', 'StepAuthorizeUsesCapability', 'metaforge:1.0.0.agent.Step_AuthorizeAmount', 'metaforge:1.0.0.agent.Cap_AmountAuthorize', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StepRiskToCap', 'StepRiskUsesCapability', 'metaforge:1.0.0.agent.Step_CheckRisk', 'metaforge:1.0.0.agent.Cap_RiskCheck', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 支付域能力引用协议
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_PayValidateToHttp', 'PayValidateImplementsHttp', 'metaforge:1.0.0.agent.Cap_CardValidate', 'metaforge:1.0.0.protocol.Http_CardValidate', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsHttp', '{"protocol_type":"Http"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_PayAuthorizeToHttp', 'PayAuthorizeImplementsHttp', 'metaforge:1.0.0.agent.Cap_AmountAuthorize', 'metaforge:1.0.0.protocol.Http_AmountAuthorize', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsHttp', '{"protocol_type":"Http"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_PayRiskToMcp', 'PayRiskImplementsMcp', 'metaforge:1.0.0.agent.Cap_RiskCheck', 'metaforge:1.0.0.protocol.McpTool_RiskCheck', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsMcpTool', '{"protocol_type":"McpTool"}')
ON CONFLICT (fqn) DO NOTHING;

-- 支付域规则适用
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RuleAmountToStep', 'RuleAmountAppliesToStep', 'metaforge:1.0.0.agent.Rule_AmountLimit', 'metaforge:1.0.0.agent.Step_AuthorizeAmount', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RuleRiskToStep', 'RuleRiskAppliesToStep', 'metaforge:1.0.0.agent.Rule_RiskFlag', 'metaforge:1.0.0.agent.Step_CheckRisk', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 四、跨域委派：支付 Agent 委派 库存 Agent（子 Agent 调用）
-- ============================================================
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayAgentDelegatesInvAgent', 'PayAgentDelegatesInventoryAgent', 'metaforge:1.0.0.agent.Agent_PaymentAgent', 'metaforge:1.0.0.agent.Agent_InventoryAgent', 'DEPENDENCY_INFLUENCE', 'metaforge:1.0.0.agent.AgentDelegatesTo', '{"delegation":"subagent"}')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 四·五、新模型示例（决策步骤 + 起点子任务 + 跨层级流程 + 任务级约束）
--   任务下只能为步骤（ExecutionStep / DecisionStep / 起点子Task），起点步骤用 COMPOSITION；
--   非起点步骤通过流程关系衔接，不挂父任务。
-- ============================================================

-- 1. 决策步骤实体
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_RiskCheck', '风控决策', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"风控结论是否为 HIGH","recommended_option":"拦截支付","rationale":"高风险交易需人工复核或拦截","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 2. 父任务实体（起点子任务示例）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_OrderFulfillment', '订单履约任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"HIGH","estimated_complexity":"COMPLEX"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 3. 任务起点子任务：订单履约 → 起点子任务 库存盘点（COMPOSITION，起点）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_FulfillToInventory', 'OrderFulfillmentComposesInventory', 'metaforge:1.0.0.agent.Task_OrderFulfillment', 'metaforge:1.0.0.agent.Task_InventoryCheck', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntrySubtask', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 4. 任务组成决策步骤：支付校验 → 起点决策步骤 风控决策（COMPOSITION，起点）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PayToDecision', 'PaymentValidationComposesRiskDecision', 'metaforge:1.0.0.agent.Task_PaymentValidation', 'metaforge:1.0.0.agent.DecisionStep_RiskCheck', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryDecisionStep', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 5. 决策步骤后继：风控决策 → 确认支付（正常分支，PROCESS_SEQUENCE）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RiskDecisionToConfirm', 'RiskDecisionToConfirm', 'metaforge:1.0.0.agent.DecisionStep_RiskCheck', 'metaforge:1.0.0.agent.Step_ConfirmPayment', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"风控通过"}')
ON CONFLICT (fqn) DO NOTHING;

-- 6. 跨层级流程：库存域最后一步 → 进入支付子任务（StepHasNextTask）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_RestockToPayment', 'RestockToPaymentTask', 'metaforge:1.0.0.agent.Step_TriggerRestock', 'metaforge:1.0.0.agent.Task_PaymentValidation', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 7. 任务级约束：48小时发货约束适用于订单履约任务（RuleAppliesToTask）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_48hToFulfill', 'Rule48hAppliesToFulfillTask', 'metaforge:1.0.0.agent.Rule_48hShipping', 'metaforge:1.0.0.agent.Task_OrderFulfillment', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesToTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 四·六、流程关系全覆盖演示（执行步骤 / 决策步骤 / 子任务三类节点后继）
--   线性链：Task_DelegationDemo →
--     Step_DemoInit(TaskHasEntryStep) → Step_DemoWork(StepHasNextStep)
--     → DecisionStep_DemoGate1(StepHasNextDecisionStep) → DecisionStep_DemoGate2(DecisionStepHasNextDecisionStep)
--     → Step_DemoFinish(DecisionStepHasNextStep) → Task_DemoSub(StepHasNextTask)
--     → Step_DemoClose(TaskHasNextStep) → DecisionStep_DemoGate3(StepHasNextDecisionStep)
--     → Task_DemoArchive(DecisionStepHasNextTask)
--   覆盖 7 种流程关系：StepHasNextStep / StepHasNextDecisionStep / StepHasNextTask /
--     DecisionStepHasNextStep / DecisionStepHasNextDecisionStep / DecisionStepHasNextTask / TaskHasNextStep
-- ============================================================

-- 1. 演示任务实体
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_DelegationDemo', '委派编排演示任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":2,"priority_default":"MEDIUM","estimated_complexity":"COMPLEX"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 2. 执行步骤 / 决策步骤 / 子任务实体
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_DemoInit', '演示入口步骤', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY","capability_required":true}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_DemoWork', '演示处理步骤', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_DemoGate1', '演示决策步骤1', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"是否进入下一级审批","recommended_option":"继续","rationale":"演示决策→决策跳转","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_DemoGate2', '演示决策步骤2', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"是否派发子任务","recommended_option":"派发","rationale":"演示决策→执行步骤跳转","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_DemoFinish', '演示收尾步骤', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_DemoSub', '演示子任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"LOW","estimated_complexity":"SIMPLE"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_DemoSubInner', '演示子任务内部步骤', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_DemoClose', '演示后续步骤', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_DemoGate3', '演示归档决策', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"是否归档至任务库","recommended_option":"归档","rationale":"演示决策→任务跳转","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_DemoArchive', '演示归档任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"LOW","estimated_complexity":"SIMPLE"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 3. 演示起点：TaskHasEntryStep（任务 → 普通执行步骤，起点）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DemoTaskToInit', 'DemoTaskComposesInit', 'metaforge:1.0.0.agent.Task_DelegationDemo', 'metaforge:1.0.0.agent.Step_DemoInit', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 4. 演示后继：StepHasNextStep（执行 → 执行）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DemoInitToWork', 'DemoInitToWork', 'metaforge:1.0.0.agent.Step_DemoInit', 'metaforge:1.0.0.agent.Step_DemoWork', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 5. 演示后继：StepHasNextDecisionStep（执行 → 决策）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DemoWorkToGate1', 'DemoWorkToGate1', 'metaforge:1.0.0.agent.Step_DemoWork', 'metaforge:1.0.0.agent.DecisionStep_DemoGate1', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextDecisionStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 6. 演示后继：DecisionStepHasNextDecisionStep（决策 → 决策）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DemoGate1ToGate2', 'DemoGate1ToGate2', 'metaforge:1.0.0.agent.DecisionStep_DemoGate1', 'metaforge:1.0.0.agent.DecisionStep_DemoGate2', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextDecisionStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 7. 演示后继：DecisionStepHasNextStep（决策 → 执行）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DemoGate2ToFinish', 'DemoGate2ToFinish', 'metaforge:1.0.0.agent.DecisionStep_DemoGate2', 'metaforge:1.0.0.agent.Step_DemoFinish', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"派发通过"}')
ON CONFLICT (fqn) DO NOTHING;

-- 8. 演示后继：StepHasNextTask（执行 → 子任务）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DemoFinishToSub', 'DemoFinishToSub', 'metaforge:1.0.0.agent.Step_DemoFinish', 'metaforge:1.0.0.agent.Task_DemoSub', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 9. 演示子任务起点：TaskHasEntryStep（子任务 → 内部入口步骤）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DemoSubToInner', 'DemoSubComposesInner', 'metaforge:1.0.0.agent.Task_DemoSub', 'metaforge:1.0.0.agent.Step_DemoSubInner', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 10. 演示后继：TaskHasNextStep（子任务完成 → 父流程下一步骤）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DemoSubToClose', 'DemoSubToClose', 'metaforge:1.0.0.agent.Task_DemoSub', 'metaforge:1.0.0.agent.Step_DemoClose', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.TaskHasNextStep', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 11. 演示后继：StepHasNextDecisionStep（执行 → 决策）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DemoCloseToGate3', 'DemoCloseToGate3', 'metaforge:1.0.0.agent.Step_DemoClose', 'metaforge:1.0.0.agent.DecisionStep_DemoGate3', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextDecisionStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 12. 演示后继：DecisionStepHasNextTask（决策 → 任务）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_DemoGate3ToArchive', 'DemoGate3ToArchive', 'metaforge:1.0.0.agent.DecisionStep_DemoGate3', 'metaforge:1.0.0.agent.Task_DemoArchive', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextTask', '{"condition":"归档"}')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 五、实体关系双向索引（compute-engine 图遍历依赖）
--    relation_instance 直接 SQL 插入绕过了应用层激活服务，
--    需手动补插 entity_relation_index（出边 OUTBOUND + 入边 INBOUND）。
-- ============================================================
INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT source_entity_fqn, 'OUTBOUND', fqn FROM semantic_relation_network.relation_instance
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT target_entity_fqn, 'INBOUND', fqn FROM semantic_relation_network.relation_instance
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

-- ============================================================
-- 六、实体描述（description）补充——保证所有元数据实体响应含 fqn/name/description
-- ============================================================
UPDATE metadata_management.metadata_entity SET description = '履约域组——顶层业务域分组' WHERE fqn = 'metaforge:1.0.0.common.Group_Fulfillment';
UPDATE metadata_management.metadata_entity SET description = '库存管理域——负责库存、盘点、调拨' WHERE fqn = 'metaforge:1.0.0.common.Domain_Inventory';
UPDATE metadata_management.metadata_entity SET description = '支付结算域——负责支付、结算、退款' WHERE fqn = 'metaforge:1.0.0.common.Domain_Payment';
UPDATE metadata_management.metadata_entity SET description = '库存管理Agent——负责库存盘点与补货决策' WHERE fqn = 'metaforge:1.0.0.agent.Agent_InventoryAgent';
UPDATE metadata_management.metadata_entity SET description = '支付处理Agent——负责支付校验与风控协调' WHERE fqn = 'metaforge:1.0.0.agent.Agent_PaymentAgent';
UPDATE metadata_management.metadata_entity SET description = '库存执行型配置——执行型认知原型' WHERE fqn = 'metaforge:1.0.0.agent.Profile_InventoryExecution';
UPDATE metadata_management.metadata_entity SET description = '支付审计型配置——审计型认知原型' WHERE fqn = 'metaforge:1.0.0.agent.Profile_PaymentAudit';
UPDATE metadata_management.metadata_entity SET description = '库存Agent权限——委派与能力使用授权' WHERE fqn = 'metaforge:1.0.0.agent.Perm_InventoryAgent';
UPDATE metadata_management.metadata_entity SET description = '支付Agent权限——任务创建与自主执行授权' WHERE fqn = 'metaforge:1.0.0.agent.Perm_PaymentAgent';
UPDATE metadata_management.metadata_entity SET description = '库存盘点任务——以入口步 CheckInventory 起始的库存检查流程' WHERE fqn = 'metaforge:1.0.0.agent.Task_InventoryCheck';
UPDATE metadata_management.metadata_entity SET description = '支付校验任务——以入口步 ValidateCard 起始的支付校验流程' WHERE fqn = 'metaforge:1.0.0.agent.Task_PaymentValidation';
UPDATE metadata_management.metadata_entity SET description = '检查库存——入口步骤，读取订单对应库存数量' WHERE fqn = 'metaforge:1.0.0.agent.Step_CheckInventory';
UPDATE metadata_management.metadata_entity SET description = '核验库存充足性——决策步骤，判断库存是否满足订单' WHERE fqn = 'metaforge:1.0.0.agent.Step_VerifyStock';
UPDATE metadata_management.metadata_entity SET description = '触发补货——出口步骤，生成补货单' WHERE fqn = 'metaforge:1.0.0.agent.Step_TriggerRestock';
UPDATE metadata_management.metadata_entity SET description = '校验支付卡——入口步骤，验证卡有效性' WHERE fqn = 'metaforge:1.0.0.agent.Step_ValidateCard';
UPDATE metadata_management.metadata_entity SET description = '授权金额——处理步骤，向支付网关申请金额授权' WHERE fqn = 'metaforge:1.0.0.agent.Step_AuthorizeAmount';
UPDATE metadata_management.metadata_entity SET description = '风控检查——决策步骤，评估交易风险' WHERE fqn = 'metaforge:1.0.0.agent.Step_CheckRisk';
UPDATE metadata_management.metadata_entity SET description = '确认支付——出口步骤，完成支付确认' WHERE fqn = 'metaforge:1.0.0.agent.Step_ConfirmPayment';
UPDATE metadata_management.metadata_entity SET description = '库存查询API——查询商品实时库存' WHERE fqn = 'metaforge:1.0.0.agent.Cap_InventoryAPI';
UPDATE metadata_management.metadata_entity SET description = '补货队列工具——将补货请求入队' WHERE fqn = 'metaforge:1.0.0.agent.Cap_RestockQueue';
UPDATE metadata_management.metadata_entity SET description = '卡校验API——校验支付卡有效性' WHERE fqn = 'metaforge:1.0.0.agent.Cap_CardValidate';
UPDATE metadata_management.metadata_entity SET description = '金额授权API——向支付网关申请金额授权' WHERE fqn = 'metaforge:1.0.0.agent.Cap_AmountAuthorize';
UPDATE metadata_management.metadata_entity SET description = '风控检查MCP——评估订单交易风险' WHERE fqn = 'metaforge:1.0.0.agent.Cap_RiskCheck';
UPDATE metadata_management.metadata_entity SET description = '库存必须大于零——库存数量<=0 时触发补货' WHERE fqn = 'metaforge:1.0.0.agent.Rule_InventoryAboveZero';
UPDATE metadata_management.metadata_entity SET description = '48小时发货约束——订单确认后 48 小时内发货' WHERE fqn = 'metaforge:1.0.0.agent.Rule_48hShipping';
UPDATE metadata_management.metadata_entity SET description = '单笔金额上限——金额>50000 需人工复核' WHERE fqn = 'metaforge:1.0.0.agent.Rule_AmountLimit';
UPDATE metadata_management.metadata_entity SET description = '风控标记拦截——风控结论 HIGH 时拦截支付' WHERE fqn = 'metaforge:1.0.0.agent.Rule_RiskFlag';
UPDATE metadata_management.metadata_entity SET description = '库存查询接口——HTTP 协议实例' WHERE fqn = 'metaforge:1.0.0.protocol.Http_InventoryQuery';
UPDATE metadata_management.metadata_entity SET description = '补货队列MCP工具——MCP 协议实例' WHERE fqn = 'metaforge:1.0.0.protocol.McpTool_RestockQueue';
UPDATE metadata_management.metadata_entity SET description = '卡校验接口——HTTP 协议实例' WHERE fqn = 'metaforge:1.0.0.protocol.Http_CardValidate';
UPDATE metadata_management.metadata_entity SET description = '金额授权接口——HTTP 协议实例' WHERE fqn = 'metaforge:1.0.0.protocol.Http_AmountAuthorize';
UPDATE metadata_management.metadata_entity SET description = '风控检查工具——MCP 协议实例' WHERE fqn = 'metaforge:1.0.0.protocol.McpTool_RiskCheck';
UPDATE metadata_management.metadata_entity SET description = '风控决策——支付流程中的决策步骤，判断是否拦截高风险支付' WHERE fqn = 'metaforge:1.0.0.agent.DecisionStep_RiskCheck';
UPDATE metadata_management.metadata_entity SET description = '订单履约任务——以起点子任务(库存盘点)起始的订单履约流程' WHERE fqn = 'metaforge:1.0.0.agent.Task_OrderFulfillment';
UPDATE metadata_management.metadata_entity SET description = '委派编排演示任务——覆盖全部流程关系的线性演示链' WHERE fqn = 'metaforge:1.0.0.agent.Task_DelegationDemo';

-- 关系实例描述：以对应关系 schema 描述兜底（保证所有关系实例含 fqn/name/description）
UPDATE semantic_relation_network.relation_instance ri
SET description = rs.description
FROM metamodel_governance.relation_schema rs
WHERE ri.relation_schema_fqn = rs.fqn
  AND (ri.description IS NULL OR ri.description = '');

-- ============================================================================
-- 完成
-- ============================================================================
