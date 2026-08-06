-- ============================================================================
-- V4: 预置 metaforge Bundle v1.0.0 — agent + protocol + common 包内容
-- 包含 20 个 EntitySchema + 21 个 RelationSchema + AuditFields 属性模板
--   agent   : 11 EntitySchema + 12 RelationSchema
--   protocol:  4 EntitySchema +  4 RelationSchema
--   common  :  5 EntitySchema +  5 RelationSchema (L1→L5 通用业务语义层级)
-- ============================================================================

-- ============================================================
-- Bundle & BundleVersion
-- ============================================================
INSERT INTO metamodel_governance.bundle (fqn, name, description, owner, is_system)
VALUES ('metaforge', 'MetaForge 语义基座', 'MetaForge 平台语义基座，提供 Agent 与通用业务语义层元模型定义', 'system', TRUE)
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.bundle_version (fqn, bundle_fqn, status, source_version_fqn, upgrade_level)
VALUES ('metaforge:1.0.0', 'metaforge', 'PUBLISHED', NULL, NULL)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- Packages (agent / protocol / common)
-- ============================================================
INSERT INTO metamodel_governance.package (fqn, bundle_version_fqn, parent_package_fqn, description, depth)
VALUES
  ('metaforge:1.0.0.agent',    'metaforge:1.0.0', NULL, 'Agent 相关元模型：EntitySchema + RelationSchema', 0),
  ('metaforge:1.0.0.protocol',  'metaforge:1.0.0', NULL, '协议能力子类型：HTTP/MCP/CLI/本地方法', 0),
  ('metaforge:1.0.0.common',    'metaforge:1.0.0', NULL, '通用业务语义层级', 0)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- agent 包 — 11 个 EntitySchema
-- ============================================================

-- 1. Capability
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.Capability', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'Capability', '描述一个 Agent 在执行任务时可调用的工具、服务或外部系统能力。包含接口规范、调用方式和版本信息。',
  '[
    {"name":"interface_spec","type":"JSONB","required":true,"description":"接口规范——JSON 对象，包含 input_schema、output_schema、endpoint 等字段"},
    {"name":"call_method","type":"ENUM","required":true,"description":"调用方式","enum_values":["REST","MCP","INTERNAL"]},
    {"name":"version_label","type":"STRING","required":false,"description":"版本标签——如 v1.2.0"},
    {"name":"provider","type":"STRING","required":false,"description":"提供方标识"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 2. ExecutionRule
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.ExecutionRule', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'ExecutionRule', '描述 Agent 在执行业务操作时必须遵守的规则、约束或验证标准。',
  '[
    {"name":"constraint_level","type":"ENUM","required":true,"description":"约束级别","enum_values":["MANDATORY","RECOMMENDED","REFERENCE"]},
    {"name":"condition","type":"STRING","required":true,"description":"规则触发条件——如 order_status=CONFIRMED"},
    {"name":"action","type":"STRING","required":true,"description":"执行动作——如 must_ship_within_48h"},
    {"name":"exception","type":"STRING","required":false,"description":"例外处理——如 force_majeure(不可抗力)"},
    {"name":"applicable_scenarios","type":"ARRAY<STRING>","required":false,"description":"适用场景列表"},
    {"name":"references","type":"ARRAY<STRING>","required":false,"description":"权威引用"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 3. RiskPattern
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.RiskPattern', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RiskPattern', '记录某个业务操作中已知的失败模式或风险场景。',
  '[
    {"name":"trigger_condition","type":"STRING","required":true,"description":"触发条件——如 库存 API 响应时间 > 3s"},
    {"name":"impact_description","type":"STRING","required":true,"description":"影响描述"},
    {"name":"risk_level","type":"ENUM","required":true,"description":"风险等级","enum_values":["HIGH","MEDIUM","LOW"]},
    {"name":"mitigation_measures","type":"ARRAY<STRING>","required":false,"description":"缓解措施"},
    {"name":"rollback_strategy","type":"STRING","required":false,"description":"回滚策略"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 4. DecisionRule
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.DecisionRule', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'DecisionRule', '定义流程分支节点处的决策条件、推荐选项和决策理由。',
  '[
    {"name":"condition_expression","type":"STRING","required":true,"description":"条件表达式——如 order_amount > 10000"},
    {"name":"recommended_option","type":"STRING","required":false,"description":"推荐选项——如 走人工审批路径"},
    {"name":"rationale","type":"STRING","required":true,"description":"决策理由"},
    {"name":"priority","type":"INTEGER","required":false,"description":"优先级——数字越小越优先"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 5. ExecutionStep
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.ExecutionStep', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'ExecutionStep', '建模执行流程中的一个步骤节点。',
  '[
    {"name":"step_type","type":"ENUM","required":true,"description":"步骤类型","enum_values":["ENTRY","PROCESSING","DECISION","EXIT"]},
    {"name":"estimated_duration","type":"STRING","required":false,"description":"预估耗时——如 30秒"},
    {"name":"responsible_role","type":"STRING","required":false,"description":"负责角色——如 仓库管理员"},
    {"name":"input_artifacts","type":"ARRAY<STRING>","required":false,"description":"输入制品"},
    {"name":"output_artifacts","type":"ARRAY<STRING>","required":false,"description":"输出制品"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 6. AgentProfile
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.AgentProfile', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentProfile', '存储不同 Agent 原型的默认认知深度、视角优先级和 Token 预算偏好。',
  '[
    {"name":"archetype","type":"ENUM","required":true,"description":"Agent 类型","enum_values":["execution","exploration","audit","orchestration"]},
    {"name":"default_cognition_depth","type":"ENUM","required":true,"description":"默认认知深度","enum_values":["L1","L2","L3"]},
    {"name":"preferred_perspectives","type":"ARRAY<STRING>","required":true,"description":"偏好视角列表"},
    {"name":"token_budget_default","type":"INTEGER","required":false,"description":"默认 Token 预算"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 7. CostEstimate (P3 预留)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.CostEstimate', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'CostEstimate', '预估某个业务操作的成本——时间成本、资源成本、外部 API 调用费用。P3 阶段预留。',
  '[
    {"name":"operation_fqn","type":"STRING","required":true,"description":"操作 FQN"},
    {"name":"cost_value","type":"NUMBER","required":true,"description":"成本数值"},
    {"name":"cost_unit","type":"STRING","required":true,"description":"成本单位——秒、元、API 调用次数"},
    {"name":"estimation_method","type":"STRING","required":false,"description":"估算方法"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 8. Agent
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'Agent', '注册一个具体的 AI Agent 实例——包含身份标识、类型、部署信息和激活状态。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"agent_type","type":"ENUM","required":true,"description":"Agent 类型","enum_values":["LLM_AGENT","RULE_ENGINE","HUMAN_DELEGATE","HYBRID"]},
    {"name":"agent_role_summary","type":"STRING","required":false,"description":"角色简述——如 库存管理专家"},
    {"name":"agent_owner","type":"STRING","required":false,"description":"Agent 负责人/所属业务线"},
    {"name":"team_name","type":"STRING","required":false,"description":"所属团队名称"},
    {"name":"deployment_info","type":"JSONB","required":false,"description":"部署信息——JSON 对象，含 host、port、protocol"},
    {"name":"is_active","type":"BOOLEAN","required":false,"description":"是否处于激活状态","default_value":"true"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 9. AgentRole
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.AgentRole', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentRole', '定义 Agent 在业务团队中扮演的角色类型——区别于 AgentProfile 的认知原型。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"bound_archetype","type":"ENUM","required":false,"description":"绑定的认知原型","enum_values":["execution","exploration","audit","orchestration"]},
    {"name":"required_capabilities","type":"ARRAY<STRING>","required":false,"description":"角色所需的底层能力 FQN 列表"},
    {"name":"authority_level","type":"ENUM","required":false,"description":"权限级别","enum_values":["FULL_AUTONOMY","SUPERVISED","READ_ONLY"],"default_value":"SUPERVISED"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 10. AgentPermission
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.AgentPermission', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentPermission', '定义 Agent 被授权的操作类型和领域范围。',
  '[
    {"name":"permission_type","type":"ENUM","required":true,"description":"权限类型","enum_values":["METADATA_READ","COGNITION_QUERY","TASK_CREATE","TASK_DELEGATE","EVENT_SUBSCRIBE"]},
    {"name":"granted","type":"BOOLEAN","required":true,"description":"是否授予该权限","default_value":"false"},
    {"name":"allowed_bundle_fqns","type":"ARRAY<STRING>","required":false,"description":"Bundle 白名单——Agent 可访问的 Bundle FQN 列表"},
    {"name":"granted_by","type":"STRING","required":false,"description":"授权者标识"},
    {"name":"expires_at","type":"DATETIME","required":false,"description":"权限过期时间"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 11. Task
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.Task', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'Task', '定义一种可由 Agent 执行的任务类型——是子任务委派的类型锚点。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"required_role_fqn","type":"STRING","required":false,"description":"所需角色 FQN"},
    {"name":"entry_step_fqn","type":"STRING","required":false,"description":"入口步骤 FQN"},
    {"name":"delegation_depth_limit","type":"INTEGER","required":false,"description":"委派深度限制","default_value":"1"},
    {"name":"priority_default","type":"ENUM","required":false,"description":"默认优先级","enum_values":["CRITICAL","HIGH","MEDIUM","LOW"],"default_value":"MEDIUM"},
    {"name":"estimated_complexity","type":"ENUM","required":false,"description":"预估复杂度","enum_values":["SIMPLE","MODERATE","COMPLEX"],"default_value":"MODERATE"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- protocol 包 — 4 个 EntitySchema（协议能力子类型）
-- ============================================================

-- 12. Http
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.protocol.Http', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'Http', '描述一个通过 HTTP/REST 协议调用的工具或服务能力。',
  '[
    {"name":"endpoint","type":"STRING","required":true,"description":"接口端点"},
    {"name":"method","type":"ENUM","required":true,"description":"HTTP 请求方法","enum_values":["GET","POST","PUT","DELETE","PATCH"]},
    {"name":"headers","type":"JSONB","required":false,"description":"请求头——JSON 对象"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 13. McpTool
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.protocol.McpTool', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'McpTool', '描述一个通过 MCP 协议暴露的工具能力。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"server_name","type":"STRING","required":true,"description":"MCP 服务器名称"},
    {"name":"arguments_schema","type":"JSONB","required":false,"description":"参数 Schema——JSON 对象"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 14. Cli
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.protocol.Cli', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'Cli', '描述一个通过命令行接口（CLI）调用的工具或脚本能力。',
  '[
    {"name":"command","type":"STRING","required":true,"description":"CLI 命令——如 kubectl get pods"},
    {"name":"args_template","type":"STRING","required":false,"description":"参数模板"},
    {"name":"working_directory","type":"STRING","required":false,"description":"工作目录"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 15. LocalMethod
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.protocol.LocalMethod', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'LocalMethod', '描述一个通过本地方法调用（进程内）的服务能力。',
  '[
    {"name":"class_path","type":"STRING","required":true,"description":"类路径——如 com.metaforge.service.InventoryService"},
    {"name":"method_name","type":"STRING","required":true,"description":"方法名——如 checkInventory"},
    {"name":"parameters","type":"JSONB","required":false,"description":"方法参数定义——JSON 对象"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- common 包 — 5 个 EntitySchema（L1→L5 通用业务语义层级）
--   用于意图识别/任务定位：沿 L1主题域分组→L2主题域→L3业务对象→L4逻辑实体→L5属性 逐层下钻
-- ============================================================

-- 16. SubjectDomainGroup (L1 主题域分组/业务域分组)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.common.SubjectDomainGroup', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'SubjectDomainGroup', 'L1 主题域分组（业务域分组）——顶层业务领域分类节点，支持树形嵌套分组（分类多时逐级细分）。意图识别的入口层。名称/描述/编码由实例保留字段 fqn、name、description 承载。',
  '[
    {"name":"group_level","type":"INTEGER","required":false,"description":"分组层级——从 1 开始，嵌套越深值越大","default_value":"1"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 17. SubjectDomain (L2 主题域)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.common.SubjectDomain', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'SubjectDomain', 'L2 主题域——主题域分组下的业务主题。agent 包 Task 通过 TaskBelongsToSubjectDomain 归属到此层，是意图识别的关键中间层。名称/描述/编码由实例保留字段 fqn、name、description 承载。',
  '[
    {"name":"keywords","type":"ARRAY<STRING>","required":false,"description":"意图识别关键词——如 [库存, 盘点, 调拨]"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 18. BusinessObject (L3 业务对象)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.common.BusinessObject', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'BusinessObject', 'L3 业务对象——主题域下的核心业务对象（实体级），是业务语义的抽象表达。名称/描述/编码由实例保留字段 fqn、name、description 承载。',
  '[
    {"name":"aliases","type":"ARRAY<STRING>","required":false,"description":"业务对象别名——如 [SO, 销售订单]"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 19. LogicalEntity (L4 逻辑实体)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.common.LogicalEntity', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'LogicalEntity', 'L4 逻辑实体——业务对象在逻辑层的落地表达，如数据表、视图、逻辑对象。名称/描述/编码由实例保留字段 fqn、name、description 承载。',
  '[
    {"name":"model_type","type":"ENUM","required":false,"description":"逻辑实体类型","enum_values":["TABLE","VIEW","OBJECT","DOCUMENT"]}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 20. Attribute (L5 属性)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.common.Attribute', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'Attribute', 'L5 属性——逻辑实体下的字段/属性级定义，如 订单状态字段。名称/描述/编码由实例保留字段 fqn、name、description 承载。',
  '[
    {"name":"data_type","type":"STRING","required":false,"description":"数据类型——如 STRING/INTEGER/DECIMAL"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- agent 包 — 12 个 RelationSchema
-- ============================================================

-- 1. CapabilityAssignedTo
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.CapabilityAssignedTo', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'CapabilityAssignedTo', '能力分配到步骤',
  'metaforge:1.0.0.agent.Capability', 'metaforge:1.0.0.agent.ExecutionStep',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 2. RuleAppliesTo
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.RuleAppliesTo', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RuleAppliesTo', '规则适用于步骤',
  'metaforge:1.0.0.agent.ExecutionRule', 'metaforge:1.0.0.agent.ExecutionStep',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 3. RuleDependsOn
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.RuleDependsOn', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RuleDependsOn', '规则间依赖链',
  'metaforge:1.0.0.agent.ExecutionRule', 'metaforge:1.0.0.agent.ExecutionRule',
  'DEPENDENCY_INFLUENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 4. StepHasDecision
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.StepHasDecision', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'StepHasDecision', '步骤关联决策规则',
  'metaforge:1.0.0.agent.ExecutionStep', 'metaforge:1.0.0.agent.DecisionRule',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 5. RiskAffects
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.RiskAffects', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RiskAffects', '风险影响步骤',
  'metaforge:1.0.0.agent.RiskPattern', 'metaforge:1.0.0.agent.ExecutionStep',
  'DEPENDENCY_INFLUENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 6. StepHasPrecondition
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.StepHasPrecondition', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'StepHasPrecondition', '步骤前置依赖',
  'metaforge:1.0.0.agent.ExecutionStep', 'metaforge:1.0.0.agent.ExecutionStep',
  'DEPENDENCY_INFLUENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 7. AgentUsesProfile
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.AgentUsesProfile', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentUsesProfile', 'Agent 绑定执行偏好',
  'metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent.AgentProfile',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 8. AgentHasRole
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.AgentHasRole', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentHasRole', 'Agent 持有业务角色',
  'metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent.AgentRole',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 9. AgentHasPermission
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.AgentHasPermission', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentHasPermission', 'Agent 持有操作权限',
  'metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent.AgentPermission',
  'ASSOCIATION_REFERENCE', '1', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 10. AgentDelegatesTo
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.AgentDelegatesTo', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentDelegatesTo', 'Agent 委派子代理',
  'metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent.Agent',
  'DEPENDENCY_INFLUENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 11. RoleAssignedToTask
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.RoleAssignedToTask', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RoleAssignedToTask', '角色分配到任务类型',
  'metaforge:1.0.0.agent.AgentRole', 'metaforge:1.0.0.agent.Task',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 12. TaskBelongsToSubjectDomain (agent → common 跨包)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.TaskBelongsToSubjectDomain', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'TaskBelongsToSubjectDomain', '任务属于主题域——agent 包 Task 归属到 common 包 L2 主题域，供意图识别沿「L1→L2→任务」逐层定位',
  'metaforge:1.0.0.agent.Task', 'metaforge:1.0.0.common.SubjectDomain',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- protocol 包 — 4 个 COMPOSITION RelationSchema（子类型↔Capability）
-- ============================================================

-- 13. HttpTypesAs
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.protocol.HttpTypesAs', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'HttpTypesAs', 'HTTP 能力构成——COMPOSITION 关联到 Capability',
  'metaforge:1.0.0.protocol.Http', 'metaforge:1.0.0.agent.Capability',
  'COMPOSITION', '1', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 14. McpToolTypesAs
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.protocol.McpToolTypesAs', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'McpToolTypesAs', 'MCP 工具能力构成——COMPOSITION 关联到 Capability',
  'metaforge:1.0.0.protocol.McpTool', 'metaforge:1.0.0.agent.Capability',
  'COMPOSITION', '1', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 15. CliTypesAs
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.protocol.CliTypesAs', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'CliTypesAs', 'CLI 命令能力构成——COMPOSITION 关联到 Capability',
  'metaforge:1.0.0.protocol.Cli', 'metaforge:1.0.0.agent.Capability',
  'COMPOSITION', '1', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 16. LocalMethodTypesAs
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.protocol.LocalMethodTypesAs', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'LocalMethodTypesAs', '本地方法能力构成——COMPOSITION 关联到 Capability',
  'metaforge:1.0.0.protocol.LocalMethod', 'metaforge:1.0.0.agent.Capability',
  'COMPOSITION', '1', '1')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- common 包 — 5 个 COMPOSITION RelationSchema（L1→L5 树形层级）
--   全部 COMPOSITION（传递性），父侧基数=1 保证每个子节点唯一父
-- ============================================================

-- 17. SubjectDomainGroupCategorizedAs (L1 树形分组，自引用)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.common.SubjectDomainGroupCategorizedAs', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'SubjectDomainGroupCategorizedAs', '主题域分组树形嵌套——L1 分组可多级细分，分类多时逐级展开',
  'metaforge:1.0.0.common.SubjectDomainGroup', 'metaforge:1.0.0.common.SubjectDomainGroup',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 18. SubjectDomainGroupContainsSubjectDomain (L1→L2)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'SubjectDomainGroupContainsSubjectDomain', '主题域分组包含主题域——L1→L2',
  'metaforge:1.0.0.common.SubjectDomainGroup', 'metaforge:1.0.0.common.SubjectDomain',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 19. SubjectDomainContainsBusinessObject (L2→L3)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'SubjectDomainContainsBusinessObject', '主题域包含业务对象——L2→L3',
  'metaforge:1.0.0.common.SubjectDomain', 'metaforge:1.0.0.common.BusinessObject',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 20. BusinessObjectRefinesLogicalEntity (L3→L4)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'BusinessObjectRefinesLogicalEntity', '业务对象细化到逻辑实体——L3→L4',
  'metaforge:1.0.0.common.BusinessObject', 'metaforge:1.0.0.common.LogicalEntity',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 21. LogicalEntityContainsAttribute (L4→L5)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.common.LogicalEntityContainsAttribute', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'LogicalEntityContainsAttribute', '逻辑实体包含属性——L4→L5',
  'metaforge:1.0.0.common.LogicalEntity', 'metaforge:1.0.0.common.Attribute',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- AuditFields 属性模板
-- ============================================================
INSERT INTO metamodel_governance.attribute_template (fqn, bundle_version_fqn, name, description, attribute_definitions)
VALUES (
    'metaforge:1.0.0.AuditFields',
    'metaforge:1.0.0',
    'AuditFields',
    '审计字段模板（createdAt/createdBy/updatedAt/updatedBy）',
    '[
      {"name": "createdAt", "type": "string", "format": "date-time", "required": true, "description": "创建时间"},
      {"name": "createdBy", "type": "string", "required": true, "description": "创建人"},
      {"name": "updatedAt", "type": "string", "format": "date-time", "required": true, "description": "更新时间"},
      {"name": "updatedBy", "type": "string", "required": true, "description": "更新人"}
    ]'
) ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 导出清单 — 导出 agent + protocol + common 三个包
-- ============================================================
INSERT INTO metamodel_governance.export_manifest (bundle_version_fqn, exported_package_fqns)
VALUES ('metaforge:1.0.0', '["metaforge:1.0.0.agent","metaforge:1.0.0.protocol","metaforge:1.0.0.common"]')
ON CONFLICT (bundle_version_fqn) DO NOTHING;
