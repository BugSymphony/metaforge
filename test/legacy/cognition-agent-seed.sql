-- ============================================================================
-- cognition agent 任务执行 seed (幂等)
-- 供 cognition-agent-test.sh --seed 调用。
-- 参考 V4__metamodel_governance_init.sql 的 metaforge Bundle 元模型:
--   agent 包: Task / ExecutionStep / Capability / ExecutionRule / DecisionRule
--             / RiskPattern / Agent / AgentRole / AgentProfile / AgentPermission
--   common 包: SubjectDomainGroup(L1) / SubjectDomain(L2) / BusinessObject(L3)
--              / LogicalEntity(L4) / Attribute(L5)
--   protocol 包: Http / McpTool / Cli / LocalMethod
--
-- 本 seed 在 V4 已注册的 Schema 之上补 M1 实例 + 关系实例,
-- 使 cognition 层对 "agent 任务执行" 场景可返回真实数据。
-- 要求: 每个元数据均带 name + description + content(属性)。
-- 全部 ON CONFLICT DO NOTHING, 可重复执行。
-- ============================================================================

-- ============================================================
-- 0. 依赖 Bundle: metaforge 由 V4 迁移脚本创建, 此处确保存在
-- ============================================================
INSERT INTO metamodel_governance.bundle (fqn, name, description, owner, is_system)
VALUES ('metaforge', 'MetaForge 语义基座', 'Agent 与通用业务语义层元模型定义', 'system', TRUE)
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.bundle_version (fqn, bundle_fqn, status, source_version_fqn, upgrade_level)
VALUES ('metaforge:1.0.0', 'metaforge', 'PUBLISHED', NULL, NULL)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 1. 补充 PROCESS_SEQUENCE RelationSchema (metaforge 用于步骤编排)
-- ============================================================
INSERT INTO metamodel_governance.relation_schema
  (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target) VALUES
('metaforge:1.0.0.agent.StepSequencedBy', 'metaforge:1.0.0.agent', 'metaforge:1.0.0', 'StepSequencedBy', '执行步骤先后编排',
 'metaforge:1.0.0.agent.ExecutionStep', 'metaforge:1.0.0.agent.ExecutionStep', 'PROCESS_SEQUENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 2. common 包 M1 实例 (L1→L5 业务层级)
-- ============================================================
INSERT INTO metadata_management.metadata_entity
  (fqn, name, description, entity_schema_fqn, content, current_version, created_by, updated_by) VALUES
('metaforge:1.0.0.common.Group_OrderFulfillment', '订单履约域', '订单履约相关业务主题的顶层分组', 'metaforge:1.0.0.common.SubjectDomainGroup', '{"group_level":1}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Domain_Order', '订单域', '订单生命周期与下单相关业务主题', 'metaforge:1.0.0.common.SubjectDomain', '{"keywords":["下单","订单","履约"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Domain_Inventory', '库存域', '库存水位、预留与调拨相关业务主题', 'metaforge:1.0.0.common.SubjectDomain', '{"keywords":["库存","预留","调拨"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.BusinessObject_Order', '订单业务对象', '订单领域的核心业务对象', 'metaforge:1.0.0.common.BusinessObject', '{"aliases":["SO","销售订单"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.BusinessObject_Inventory', '库存业务对象', '库存领域的核心业务对象', 'metaforge:1.0.0.common.BusinessObject', '{"aliases":["库存","Stock"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.LogicalEntity_Order', '订单逻辑实体', '订单业务对象对应的逻辑实体(表)', 'metaforge:1.0.0.common.LogicalEntity', '{"model_type":"TABLE"}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Attribute_OrderStatus', '订单状态字段', '订单状态属性定义', 'metaforge:1.0.0.common.Attribute', '{"data_type":"STRING"}', 1, 'system', 'system')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 3. agent 包 M1 实例
-- ============================================================
INSERT INTO metadata_management.metadata_entity
  (fqn, name, description, entity_schema_fqn, content, current_version, created_by, updated_by) VALUES
-- Task
('metaforge:1.0.0.agent.Task_OrderFulfillment', '订单履约主任务', '从下单到发货的完整履约任务', 'metaforge:1.0.0.agent.Task',
 '{"required_role_fqn":"metaforge:1.0.0.agent.Role_OrderSpecialist","entry_step_fqn":"order:1.0.0.Step_CheckInventory","delegation_depth_limit":2,"priority_default":"HIGH","estimated_complexity":"COMPLEX"}', 1, 'system', 'system'),
('metaforge:1.0.0.agent.Task_InventoryCheck', '库存校验子任务', '对订单所需商品执行库存校验的子任务', 'metaforge:1.0.0.agent.Task',
 '{"required_role_fqn":"metaforge:1.0.0.agent.Role_InventorySpecialist","entry_step_fqn":"order:1.0.0.Step_CheckInventory","delegation_depth_limit":1,"priority_default":"MEDIUM","estimated_complexity":"MODERATE"}', 1, 'system', 'system'),
-- ExecutionStep
('order:1.0.0.Step_CheckInventory', '库存校验', '校验订单所需商品的可售库存是否充足，不足则触发补货或降级。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"PROCESSING","estimated_duration":"30秒","responsible_role":"系统自动","input_artifacts":["确认后的订单","库存快照"],"output_artifacts":["库存校验结果"]}', 1, 'system', 'system'),
('order:1.0.0.Step_CheckPayment', '支付校验', '校验订单支付状态与支付方式。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"DECISION","estimated_duration":"10秒","responsible_role":"系统自动","input_artifacts":["订单金额","支付方式"],"output_artifacts":["支付校验结果"]}', 1, 'system', 'system'),
('order:1.0.0.Step_ReserveStock', '锁库存', '为订单预留所需商品库存。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"PROCESSING","estimated_duration":"20秒","responsible_role":"系统自动","input_artifacts":["库存校验结果"],"output_artifacts":["库存预留单"]}', 1, 'system', 'system'),
('order:1.0.0.Step_TriggerApproval', '人工审批', '大额或跨境订单触发人工审批。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"DECISION","estimated_duration":"2小时","responsible_role":"订单审批专员","input_artifacts":["订单详情"],"output_artifacts":["审批结果"]}', 1, 'system', 'system'),
-- Capability
('order:1.0.0.Cap_OrderValidator', '订单校验服务', '对订单数据进行完整性校验的工具能力', 'metaforge:1.0.0.agent.Capability',
 '{"interface_spec":{"method":"validateOrder","input_schema":{"type":"object","properties":{"orderNo":{"type":"string"}}}},"call_method":"INTERNAL","version_label":"v1.2.0","provider":"order-bc"}', 1, 'system', 'system'),
('order:1.0.0.Cap_InventoryApi', '库存查询 API', '查询商品可售库存的 HTTP 能力', 'metaforge:1.0.0.agent.Capability',
 '{"interface_spec":{"endpoint":"/api/v1/inventory/query","method":"GET"},"call_method":"REST","version_label":"v1.0.0","provider":"inventory-bc"}', 1, 'system', 'system'),
-- ExecutionRule
('order:1.0.0.Rule_InventoryAboveZero', '库存需大于零', '库存校验必须满足库存大于零，否则触发补货', 'metaforge:1.0.0.agent.ExecutionRule',
 '{"constraint_level":"MANDATORY","condition":"requested_qty > available_qty","action":"must_trigger_restock","exception":"force_majeure","applicable_scenarios":["普通订单","促销订单"],"references":["order:1.0.0.Step_CheckInventory"]}', 1, 'system', 'system'),
('order:1.0.0.Rule_PaymentBeforeShip', '先付款后发货', '未付款订单禁止进入发货流程', 'metaforge:1.0.0.agent.ExecutionRule',
 '{"constraint_level":"MANDATORY","condition":"order_status != PAID","action":"must_block_ship","exception":"货到付款","applicable_scenarios":["常规流程"],"references":["order:1.0.0.Step_CheckPayment"]}', 1, 'system', 'system'),
-- DecisionRule
('order:1.0.0.Decision_PaymentRoute', '支付分流决策', '按金额与支付方式选择支付后处理路径', 'metaforge:1.0.0.agent.DecisionRule',
 '{"condition_expression":"order_amount > 10000 OR payment_method = international","recommended_option":"trigger_approval","rationale":"大额/跨境订单需人工审批","priority":1}', 1, 'system', 'system'),
-- RiskPattern
('order:1.0.0.Risk_InventoryTimeout', '库存接口超时风险', '库存 API 响应超时导致校验阻塞', 'metaforge:1.0.0.agent.RiskPattern',
 '{"trigger_condition":"库存 API 响应时间 > 3s","impact_description":"订单履约流程阻塞","risk_level":"HIGH","mitigation_measures":["缓存库存快照","超时熔断"],"rollback_strategy":"降级为人工校验"}', 1, 'system', 'system'),
-- Agent
('metaforge:1.0.0.agent.Agent_OrderBot', '订单履约机器人', '负责订单履约主流程的 Agent', 'metaforge:1.0.0.agent.Agent',
 '{"agent_type":"LLM_AGENT","agent_role_summary":"订单履约专家","agent_owner":"order-bc","team_name":"履约团队","deployment_info":{"host":"agent-01","port":9001,"protocol":"mcp"},"is_active":true}', 1, 'system', 'system'),
('metaforge:1.0.0.agent.Agent_InventoryBot', '库存管理机器人', '负责库存查询与调拨的 Agent', 'metaforge:1.0.0.agent.Agent',
 '{"agent_type":"RULE_ENGINE","agent_role_summary":"库存管理专家","agent_owner":"inventory-bc","team_name":"库存团队","deployment_info":{"host":"agent-02","port":9002,"protocol":"mcp"},"is_active":true}', 1, 'system', 'system'),
-- AgentRole
('metaforge:1.0.0.agent.Role_OrderSpecialist', '订单专员', '负责订单履约的 Agent 角色', 'metaforge:1.0.0.agent.AgentRole',
 '{"bound_archetype":"execution","required_capabilities":["order:1.0.0.Cap_OrderValidator"],"authority_level":"FULL_AUTONOMY"}', 1, 'system', 'system'),
('metaforge:1.0.0.agent.Role_InventorySpecialist', '库存专员', '负责库存管理的 Agent 角色', 'metaforge:1.0.0.agent.AgentRole',
 '{"bound_archetype":"execution","required_capabilities":["order:1.0.0.Cap_InventoryApi"],"authority_level":"SUPERVISED"}', 1, 'system', 'system'),
-- AgentProfile
('metaforge:1.0.0.agent.AgentProfile_Execution', '执行型原型', '执行型 Agent 的认知偏好', 'metaforge:1.0.0.agent.AgentProfile',
 '{"archetype":"execution","default_cognition_depth":"L2","preferred_perspectives":["constraint_set","capability_catalog","flow_blueprint"],"token_budget_default":8000}', 1, 'system', 'system'),
-- AgentPermission
('metaforge:1.0.0.agent.AgentPermission_OrderRead', '订单读权限', '允许读取订单数据的权限', 'metaforge:1.0.0.agent.AgentPermission',
 '{"permission_type":"METADATA_READ","granted":true,"allowed_bundle_fqns":["order:1.0.0","metaforge:1.0.0"],"granted_by":"admin","expires_at":null}', 1, 'system', 'system'),
-- CostEstimate
('metaforge:1.0.0.agent.CostEstimate_InventoryCheck', '库存校验成本', '库存校验操作的预估成本', 'metaforge:1.0.0.agent.CostEstimate',
 '{"operation_fqn":"order:1.0.0.Step_CheckInventory","cost_value":0.5,"cost_unit":"元/次","estimation_method":"线性回归"}', 1, 'system', 'system')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 4. protocol 包 M1 实例
-- ============================================================
INSERT INTO metadata_management.metadata_entity
  (fqn, name, description, entity_schema_fqn, content, current_version, created_by, updated_by) VALUES
('metaforge:1.0.0.protocol.Http_InventoryQuery', '库存查询 HTTP 能力', '库存查询的 HTTP 协议能力定义', 'metaforge:1.0.0.protocol.Http',
 '{"endpoint":"/api/v1/inventory/query","method":"GET","headers":{"X-Request-Id":"uuid"}}', 1, 'system', 'system'),
('metaforge:1.0.0.protocol.McpTool_OrderValidator', '订单校验 MCP 工具', '订单校验的 MCP 协议能力定义', 'metaforge:1.0.0.protocol.McpTool',
 '{"server_name":"order-mcp","arguments_schema":{"type":"object","properties":{"orderNo":{"type":"string"}}}}', 1, 'system', 'system'),
('metaforge:1.0.0.protocol.LocalMethod_InventoryService', '库存服务本地方法', '库存服务的本地方法调用定义', 'metaforge:1.0.0.protocol.LocalMethod',
 '{"class_path":"com.metaforge.service.InventoryService","method_name":"checkInventory","parameters":{"sku":"string","qty":"integer"}}', 1, 'system', 'system')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 5. 关系实例 (agent 任务编排 + 业务层级)
-- ============================================================
INSERT INTO semantic_relation_network.relation_instance
  (fqn, name, description, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content, current_version) VALUES
-- common L1→L5 业务层级 (COMPOSITION)
('metaforge:1.0.0.Group_OrderFulfillment#metaforge:1.0.0.common.SubjectDomainGroupCategorizedAs#metaforge:1.0.0.common.Domain_Order',
 '订单域归属订单履约域', '主题域分组包含订单域', 'metaforge:1.0.0.common.Group_OrderFulfillment', 'metaforge:1.0.0.common.Domain_Order', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{"level":"L1-L2"}', 1),
('metaforge:1.0.0.common.Domain_Order#metaforge:1.0.0.common.SubjectDomainContainsBusinessObject#metaforge:1.0.0.common.BusinessObject_Order',
 '订单域包含订单业务对象', '主题域包含业务对象', 'metaforge:1.0.0.common.Domain_Order', 'metaforge:1.0.0.common.BusinessObject_Order', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{"level":"L2-L3"}', 1),
('metaforge:1.0.0.common.BusinessObject_Order#metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity#metaforge:1.0.0.common.LogicalEntity_Order',
 '订单业务对象细化订单表', '业务对象细化到逻辑实体', 'metaforge:1.0.0.common.BusinessObject_Order', 'metaforge:1.0.0.common.LogicalEntity_Order', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{"level":"L3-L4"}', 1),
('metaforge:1.0.0.common.LogicalEntity_Order#metaforge:1.0.0.common.LogicalEntityContainsAttribute#metaforge:1.0.0.common.Attribute_OrderStatus',
 '订单表包含状态字段', '逻辑实体包含属性', 'metaforge:1.0.0.common.LogicalEntity_Order', 'metaforge:1.0.0.common.Attribute_OrderStatus', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{"level":"L4-L5"}', 1),
-- Task 归属业务 (COMPOSITION)
('metaforge:1.0.0.agent.Task_OrderFulfillment#metaforge:1.0.0.agent.TaskBelongsToSubjectDomain#metaforge:1.0.0.common.Domain_Order',
 '履约任务归属订单域', '任务属于主题域', 'metaforge:1.0.0.agent.Task_OrderFulfillment', 'metaforge:1.0.0.common.Domain_Order', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskBelongsToSubjectDomain', '{"domain":"order"}', 1),
('metaforge:1.0.0.agent.Task_InventoryCheck#metaforge:1.0.0.agent.TaskBelongsToSubjectDomain#metaforge:1.0.0.common.Domain_Inventory',
 '库存校验归属库存域', '任务属于主题域', 'metaforge:1.0.0.agent.Task_InventoryCheck', 'metaforge:1.0.0.common.Domain_Inventory', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskBelongsToSubjectDomain', '{"domain":"inventory"}', 1),
-- Agent 关系
('metaforge:1.0.0.agent.Agent_OrderBot#metaforge:1.0.0.agent.AgentUsesProfile#metaforge:1.0.0.agent.AgentProfile_Execution',
 '履约机器人绑定执行原型', 'Agent 绑定执行偏好', 'metaforge:1.0.0.agent.Agent_OrderBot', 'metaforge:1.0.0.agent.AgentProfile_Execution', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentUsesProfile', '{"preference":"execution"}', 1),
('metaforge:1.0.0.agent.Agent_OrderBot#metaforge:1.0.0.agent.AgentHasRole#metaforge:1.0.0.agent.Role_OrderSpecialist',
 '履约机器人持有订单专员角色', 'Agent 持有业务角色', 'metaforge:1.0.0.agent.Agent_OrderBot', 'metaforge:1.0.0.agent.Role_OrderSpecialist', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasRole', '{"role":"order"}', 1),
('metaforge:1.0.0.agent.Agent_OrderBot#metaforge:1.0.0.agent.AgentHasPermission#metaforge:1.0.0.agent.AgentPermission_OrderRead',
 '履约机器人持有订单读权限', 'Agent 持有操作权限', 'metaforge:1.0.0.agent.Agent_OrderBot', 'metaforge:1.0.0.agent.AgentPermission_OrderRead', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasPermission', '{"permission":"read"}', 1),
('metaforge:1.0.0.agent.Agent_OrderBot#metaforge:1.0.0.agent.AgentDelegatesTo#metaforge:1.0.0.agent.Agent_InventoryBot',
 '履约机器人委派库存机器人', 'Agent 委派子代理', 'metaforge:1.0.0.agent.Agent_OrderBot', 'metaforge:1.0.0.agent.Agent_InventoryBot', 'DEPENDENCY_INFLUENCE', 'metaforge:1.0.0.agent.AgentDelegatesTo', '{"delegate":"inventory"}', 1),
('metaforge:1.0.0.agent.Role_OrderSpecialist#metaforge:1.0.0.agent.RoleAssignedToTask#metaforge:1.0.0.agent.Task_OrderFulfillment',
 '订单专员分配到履约任务', '角色分配到任务类型', 'metaforge:1.0.0.agent.Role_OrderSpecialist', 'metaforge:1.0.0.agent.Task_OrderFulfillment', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RoleAssignedToTask', '{"role":"order"}', 1),
-- 步骤编排 (PROCESS_SEQUENCE)
('order:1.0.0.Step_CheckInventory#metaforge:1.0.0.agent.StepSequencedBy#order:1.0.0.Step_CheckPayment',
 '库存校验后置支付校验', '执行步骤先后编排', 'order:1.0.0.Step_CheckInventory', 'order:1.0.0.Step_CheckPayment', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":1}', 1),
('order:1.0.0.Step_CheckPayment#metaforge:1.0.0.agent.StepSequencedBy#order:1.0.0.Step_ReserveStock',
 '支付校验后置锁库存', '执行步骤先后编排', 'order:1.0.0.Step_CheckPayment', 'order:1.0.0.Step_ReserveStock', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":2}', 1),
('order:1.0.0.Step_CheckPayment#metaforge:1.0.0.agent.StepSequencedBy#order:1.0.0.Step_TriggerApproval',
 '支付校验后置人工审批', '执行步骤先后编排', 'order:1.0.0.Step_CheckPayment', 'order:1.0.0.Step_TriggerApproval', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":3,"condition":"order_amount>10000"}', 1),
-- 规则/能力/决策/风险 绑定步骤
('order:1.0.0.Rule_InventoryAboveZero#metaforge:1.0.0.agent.RuleAppliesTo#order:1.0.0.Step_CheckInventory',
 '库存规则约束库存校验', '规则适用于步骤', 'order:1.0.0.Rule_InventoryAboveZero', 'order:1.0.0.Step_CheckInventory', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{"rule":"inventory"}', 1),
('order:1.0.0.Rule_PaymentBeforeShip#metaforge:1.0.0.agent.RuleAppliesTo#order:1.0.0.Step_CheckPayment',
 '付款规则约束支付校验', '规则适用于步骤', 'order:1.0.0.Rule_PaymentBeforeShip', 'order:1.0.0.Step_CheckPayment', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{"rule":"payment"}', 1),
('order:1.0.0.Cap_OrderValidator#metaforge:1.0.0.agent.CapabilityAssignedTo#order:1.0.0.Step_CheckInventory',
 '订单校验能力分配到库存校验', '能力分配到步骤', 'order:1.0.0.Cap_OrderValidator', 'order:1.0.0.Step_CheckInventory', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.CapabilityAssignedTo', '{"capability":"order-validator"}', 1),
('order:1.0.0.Cap_InventoryApi#metaforge:1.0.0.agent.CapabilityAssignedTo#order:1.0.0.Step_CheckInventory',
 '库存 API 分配到库存校验', '能力分配到步骤', 'order:1.0.0.Cap_InventoryApi', 'order:1.0.0.Step_CheckInventory', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.CapabilityAssignedTo', '{"capability":"inventory-api"}', 1),
('order:1.0.0.Decision_PaymentRoute#metaforge:1.0.0.agent.StepHasDecision#order:1.0.0.Step_CheckPayment',
 '支付分流决策关联支付校验', '步骤关联决策规则', 'order:1.0.0.Decision_PaymentRoute', 'order:1.0.0.Step_CheckPayment', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepHasDecision', '{"decision":"payment"}', 1),
('order:1.0.0.Risk_InventoryTimeout#metaforge:1.0.0.agent.RiskAffects#order:1.0.0.Step_CheckInventory',
 '库存超时风险影响库存校验', '风险影响步骤', 'order:1.0.0.Risk_InventoryTimeout', 'order:1.0.0.Step_CheckInventory', 'DEPENDENCY_INFLUENCE', 'metaforge:1.0.0.agent.RiskAffects', '{"risk":"inventory-timeout"}', 1),
-- protocol → Capability 构成 (COMPOSITION)
('metaforge:1.0.0.protocol.Http_InventoryQuery#metaforge:1.0.0.protocol.HttpTypesAs#order:1.0.0.Cap_InventoryApi',
 '库存 HTTP 能力构成库存 API', 'HTTP 能力构成', 'metaforge:1.0.0.protocol.Http_InventoryQuery', 'order:1.0.0.Cap_InventoryApi', 'COMPOSITION', 'metaforge:1.0.0.protocol.HttpTypesAs', '{"protocol":"http"}', 1),
('metaforge:1.0.0.protocol.McpTool_OrderValidator#metaforge:1.0.0.protocol.McpToolTypesAs#order:1.0.0.Cap_OrderValidator',
 '订单校验 MCP 能力构成订单校验服务', 'MCP 能力构成', 'metaforge:1.0.0.protocol.McpTool_OrderValidator', 'order:1.0.0.Cap_OrderValidator', 'COMPOSITION', 'metaforge:1.0.0.protocol.McpToolTypesAs', '{"protocol":"mcp"}', 1)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 6. 重建实体双向索引 (entity_relation_index)
--    compute-engine 的图遍历依赖该索引表, 由 relation_instance 全量重建。
--    幂等: 先清空再插入, 每关系两行 (source→OUTBOUND, target→INBOUND)。
-- ============================================================
DELETE FROM semantic_relation_network.entity_relation_index;
INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT source_entity_fqn, 'OUTBOUND', fqn
FROM semantic_relation_network.relation_instance
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;
INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT target_entity_fqn, 'INBOUND', fqn
FROM semantic_relation_network.relation_instance
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;
