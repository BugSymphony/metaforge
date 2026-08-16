-- ============================================================================
-- V4: 预置 metaforge Bundle v1.0.0 — common + agent + protocol 包内容（全量重写）
-- 参考 opencode Agent 架构重构关系建模：
--   - 能力被多方使用（Agent/Task/Step），分配关系由使用方维护（使用方 → Capability）
--   - 主题域组成 Agent / Task（域归属，COMPOSITION）
--   - Agent 完整组成（Profile/Permission/ExecutesTask/DelegatesTo）+ 委派
--   - 协议由 Capability 引用（每协议一个关系，协议包维护）
-- 移除：AgentRole、CapabilityAssignedTo、TaskBelongsToSubjectDomain
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
-- Packages (common / agent / protocol)
-- ============================================================
INSERT INTO metamodel_governance.package (fqn, bundle_version_fqn, parent_package_fqn, description, depth)
VALUES
  ('metaforge:1.0.0.common',   'metaforge:1.0.0', NULL, '通用业务语义层级', 0),
  ('metaforge:1.0.0.agent',    'metaforge:1.0.0', NULL, 'Agent 相关元模型：Agent/Task/Step/Capability/Rule 等', 0),
  ('metaforge:1.0.0.protocol', 'metaforge:1.0.0', NULL, '协议能力子类型：HTTP/MCP/CLI/本地方法', 0)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- common 包 — 5 个 EntitySchema（L1→L5 通用业务语义层级）
-- ============================================================

-- 1. SubjectDomainGroup (L1 主题域分组)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.common.SubjectDomainGroup', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'SubjectDomainGroup', 'L1 主题域分组（业务域分组）——顶层业务领域分类节点，支持树形嵌套分组。意图识别的入口层。名称/描述/编码由实例保留字段 fqn、name、description 承载。',
  '[
    {"name":"group_level","type":"INTEGER","required":false,"description":"分组层级——从 1 开始，嵌套越深值越大","default_value":"1"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 2. SubjectDomain (L2 主题域)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.common.SubjectDomain', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'SubjectDomain', 'L2 主题域——主题域分组下的业务主题。Agent 与 Task 归属到此层，是意图识别的关键中间层。名称/描述/编码由实例保留字段 fqn、name、description 承载。',
  '[
    {"name":"keywords","type":"ARRAY<STRING>","required":false,"description":"意图识别关键词——如 [库存, 盘点, 调拨]"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 3. BusinessObject (L3 业务对象)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.common.BusinessObject', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'BusinessObject', 'L3 业务对象——主题域下的核心业务对象（实体级），是业务语义的抽象表达。名称/描述/编码由实例保留字段 fqn、name、description 承载。',
  '[
    {"name":"aliases","type":"ARRAY<STRING>","required":false,"description":"业务对象别名——如 [SO, 销售订单]"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 4. LogicalEntity (L4 逻辑实体)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.common.LogicalEntity', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'LogicalEntity', 'L4 逻辑实体——业务对象在逻辑层的落地表达，如数据表、视图、逻辑对象。名称/描述/编码由实例保留字段 fqn、name、description 承载。',
  '[
    {"name":"model_type","type":"ENUM","required":false,"description":"逻辑实体类型","enum_values":["TABLE","VIEW","OBJECT","DOCUMENT"]}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 5. Attribute (L5 属性)
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.common.Attribute', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'Attribute', 'L5 属性——逻辑实体下的字段/属性级定义。名称/描述/编码由实例保留字段 fqn、name、description 承载。',
  '[
    {"name":"data_type","type":"STRING","required":false,"description":"数据类型——如 STRING/INTEGER/DECIMAL"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- agent 包 — 10 个 EntitySchema（移除 AgentRole）
-- ============================================================

-- 6. Agent
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'Agent', '注册一个具体的 AI Agent 实例——包含身份标识、类型、运行模式、部署信息和激活状态。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"agent_type","type":"ENUM","required":true,"description":"Agent 类型","enum_values":["LLM_AGENT","RULE_ENGINE","HUMAN_DELEGATE","HYBRID"]},
    {"name":"mode","type":"ENUM","required":true,"description":"运行模式——能否直接响应或仅被子 Agent 调用","enum_values":["PRIMARY","SUBAGENT","ALL"]},
    {"name":"native","type":"BOOLEAN","required":false,"description":"是否内置 Agent（内置不可删除）","default_value":"false"},
    {"name":"hidden","type":"BOOLEAN","required":false,"description":"是否隐藏（不显示在 Agent 列表）","default_value":"false"},
    {"name":"prompt","type":"STRING","required":false,"description":"System Prompt 内容，覆盖默认 prompt"},
    {"name":"agent_role_summary","type":"STRING","required":false,"description":"角色简述——如 库存管理专家"},
    {"name":"agent_owner","type":"STRING","required":false,"description":"Agent 负责人/所属业务线"},
    {"name":"team_name","type":"STRING","required":false,"description":"所属团队名称"},
    {"name":"deployment_info","type":"JSONB","required":false,"description":"部署信息——JSON 对象，含 host、port、protocol"},
    {"name":"is_active","type":"BOOLEAN","required":false,"description":"是否处于激活状态","default_value":"true"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 7. AgentProfile
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.AgentProfile', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentProfile', '存储 Agent 的认知原型配置——默认认知深度、视角优先级和 Token 预算偏好。',
  '[
    {"name":"archetype","type":"ENUM","required":true,"description":"Agent 原型","enum_values":["execution","exploration","audit","orchestration"]},
    {"name":"default_cognition_depth","type":"ENUM","required":true,"description":"默认认知深度","enum_values":["L1","L2","L3"]},
    {"name":"preferred_perspectives","type":"ARRAY<STRING>","required":true,"description":"偏好视角列表"},
    {"name":"token_budget_default","type":"INTEGER","required":false,"description":"默认 Token 预算"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 8. AgentPermission
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.AgentPermission', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentPermission', '定义 Agent 被授权的操作类型、领域范围和授权级别。',
  '[
    {"name":"permission_type","type":"ENUM","required":true,"description":"权限类型","enum_values":["METADATA_READ","COGNITION_QUERY","TASK_CREATE","TASK_DELEGATE","EVENT_SUBSCRIBE","CAPABILITY_USE"]},
    {"name":"authority_level","type":"ENUM","required":true,"description":"授权级别","enum_values":["FULL_AUTONOMY","SUPERVISED","READ_ONLY"],"default_value":"SUPERVISED"},
    {"name":"granted","type":"BOOLEAN","required":true,"description":"是否授予该权限","default_value":"false"},
    {"name":"allowed_bundle_fqns","type":"ARRAY<STRING>","required":false,"description":"Bundle 白名单——Agent 可访问的 Bundle FQN 列表"},
    {"name":"granted_by","type":"STRING","required":false,"description":"授权者标识"},
    {"name":"expires_at","type":"DATETIME","required":false,"description":"权限过期时间"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 9. Task
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.Task', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'Task', '定义一种可由 Agent 执行的任务类型——是子任务委派的类型锚点。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"entry_step_fqn","type":"STRING","required":false,"description":"入口步骤 FQN"},
    {"name":"delegation_depth_limit","type":"INTEGER","required":false,"description":"委派深度限制","default_value":"1"},
    {"name":"priority_default","type":"ENUM","required":false,"description":"默认优先级","enum_values":["CRITICAL","HIGH","MEDIUM","LOW"],"default_value":"MEDIUM"},
    {"name":"estimated_complexity","type":"ENUM","required":false,"description":"预估复杂度","enum_values":["SIMPLE","MODERATE","COMPLEX"],"default_value":"MODERATE"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 10. ExecutionStep
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

-- 11. Capability
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.Capability', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'Capability', '描述一个 Agent 可调用的工具、服务或外部系统能力。包含接口规范、调用方式和版本信息。可被 Agent/Task/Step 多方引用。',
  '[
    {"name":"interface_spec","type":"JSONB","required":false,"description":"接口规范——JSON 对象，包含 input_schema、output_schema、endpoint 等字段（能力详情，按需加载）"},
    {"name":"call_method","type":"ENUM","required":false,"description":"调用方式","enum_values":["REST","MCP","INTERNAL","CLI","LOCAL"]},
    {"name":"version_label","type":"STRING","required":false,"description":"版本标签——如 v1.2.0"},
    {"name":"provider","type":"STRING","required":false,"description":"提供方标识"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 12. ExecutionRule
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.ExecutionRule', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'ExecutionRule', '描述 Agent 在执行业务操作时必须遵守的规则、约束或验证标准。',
  '[
    {"name":"constraint_level","type":"ENUM","required":true,"description":"约束级别","enum_values":["MANDATORY","RECOMMENDED","REFERENCE"]},
    {"name":"condition","type":"STRING","required":true,"description":"规则触发条件——如 order_status=CONFIRMED"},
    {"name":"action","type":"STRING","required":true,"description":"执行动作——如 must_ship_within_48h"},
    {"name":"exception","type":"STRING","required":false,"description":"例外处理"},
    {"name":"applicable_scenarios","type":"ARRAY<STRING>","required":false,"description":"适用场景列表"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 13. DecisionStep（决策步骤，原 DecisionRule 改名——决策是任务的一种执行单元，而非独立规则）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.DecisionStep', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'DecisionStep', '流程中的决策步骤节点——以自然语言条件描述分支/循环走向，Agent 自主选择后续执行单元。',
  '[
    {"name":"condition_expression","type":"STRING","required":true,"description":"条件表达式（自然语言）——如 库存不足则继续补货"},
    {"name":"recommended_option","type":"STRING","required":false,"description":"推荐选项"},
    {"name":"rationale","type":"STRING","required":true,"description":"决策理由"},
    {"name":"priority","type":"INTEGER","required":false,"description":"优先级——数字越小越优先"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 14. RiskPattern
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.RiskPattern', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RiskPattern', '记录某个业务操作中已知的失败模式或风险场景。',
  '[
    {"name":"trigger_condition","type":"STRING","required":true,"description":"触发条件"},
    {"name":"impact_description","type":"STRING","required":true,"description":"影响描述"},
    {"name":"risk_level","type":"ENUM","required":true,"description":"风险等级","enum_values":["HIGH","MEDIUM","LOW"]},
    {"name":"mitigation_measures","type":"ARRAY<STRING>","required":false,"description":"缓解措施"},
    {"name":"rollback_strategy","type":"STRING","required":false,"description":"回滚策略"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 15. CostEstimate（P3 预留）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.agent.CostEstimate', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'CostEstimate', '预估某个业务操作的成本。P3 阶段预留。',
  '[
    {"name":"operation_fqn","type":"STRING","required":true,"description":"操作 FQN"},
    {"name":"cost_value","type":"NUMBER","required":true,"description":"成本数值"},
    {"name":"cost_unit","type":"STRING","required":true,"description":"成本单位"},
    {"name":"estimation_method","type":"STRING","required":false,"description":"估算方法"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- protocol 包 — 4 个 EntitySchema
-- ============================================================

-- 16. Http
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.protocol.Http', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'Http', '描述一个通过 HTTP/REST 协议调用的工具或服务能力。',
  '[
    {"name":"endpoint","type":"STRING","required":true,"description":"接口端点"},
    {"name":"method","type":"ENUM","required":true,"description":"HTTP 请求方法","enum_values":["GET","POST","PUT","DELETE","PATCH"]},
    {"name":"headers","type":"JSONB","required":false,"description":"请求头——JSON 对象"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 17. McpTool
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.protocol.McpTool', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'McpTool', '描述一个通过 MCP 协议暴露的工具能力。',
  '[
    {"name":"server_name","type":"STRING","required":true,"description":"MCP 服务器名称"},
    {"name":"arguments_schema","type":"JSONB","required":false,"description":"参数 Schema——JSON 对象"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 18. Cli
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.protocol.Cli', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'Cli', '描述一个通过命令行接口（CLI）调用的工具或脚本能力。',
  '[
    {"name":"command","type":"STRING","required":true,"description":"CLI 命令"},
    {"name":"args_template","type":"STRING","required":false,"description":"参数模板"},
    {"name":"working_directory","type":"STRING","required":false,"description":"工作目录"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 19. LocalMethod
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('metaforge:1.0.0.protocol.LocalMethod', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'LocalMethod', '描述一个通过本地方法调用（进程内）的服务能力。',
  '[
    {"name":"class_path","type":"STRING","required":true,"description":"类路径"},
    {"name":"method_name","type":"STRING","required":true,"description":"方法名"},
    {"name":"parameters","type":"JSONB","required":false,"description":"方法参数定义"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- common 包 RelationSchema（L1→L5 树形层级，COMPOSITION）
-- ============================================================

-- 20. SubjectDomainGroupCategorizedAs (L1 树形自引用)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.common.SubjectDomainGroupCategorizedAs', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'SubjectDomainGroupCategorizedAs', '主题域分组树形嵌套',
  'metaforge:1.0.0.common.SubjectDomainGroup', 'metaforge:1.0.0.common.SubjectDomainGroup',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 21. SubjectDomainGroupContainsSubjectDomain (L1→L2)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'SubjectDomainGroupContainsSubjectDomain', '主题域分组包含主题域',
  'metaforge:1.0.0.common.SubjectDomainGroup', 'metaforge:1.0.0.common.SubjectDomain',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 22. SubjectDomainContainsBusinessObject (L2→L3)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'SubjectDomainContainsBusinessObject', '主题域包含业务对象',
  'metaforge:1.0.0.common.SubjectDomain', 'metaforge:1.0.0.common.BusinessObject',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 23. BusinessObjectRefinesLogicalEntity (L3→L4)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'BusinessObjectRefinesLogicalEntity', '业务对象细化到逻辑实体',
  'metaforge:1.0.0.common.BusinessObject', 'metaforge:1.0.0.common.LogicalEntity',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 24. LogicalEntityContainsAttribute (L4→L5)
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.common.LogicalEntityContainsAttribute', 'metaforge:1.0.0.common', 'metaforge:1.0.0',
  'LogicalEntityContainsAttribute', '逻辑实体包含属性',
  'metaforge:1.0.0.common.LogicalEntity', 'metaforge:1.0.0.common.Attribute',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- agent 包 RelationSchema（关系建模核心，使用方维护）
-- ============================================================

-- 25. 域归属：主题域组成 Agent / Task（COMPOSITION，域→agent 组成）
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.SubjectDomainComposesAgent', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'SubjectDomainComposesAgent', '主题域包含 Agent——域下部署的 Agent 成员',
  'metaforge:1.0.0.common.SubjectDomain', 'metaforge:1.0.0.agent.Agent',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.SubjectDomainComposesTask', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'SubjectDomainComposesTask', '主题域包含任务——域下可执行的任务类型',
  'metaforge:1.0.0.common.SubjectDomain', 'metaforge:1.0.0.agent.Task',
  'COMPOSITION', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 26. 能力分配（使用方 → Capability，被多方引用）
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.AgentHasCapability', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentHasCapability', 'Agent 拥有/可调用某能力',
  'metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent.Capability',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.TaskRequiresCapability', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'TaskRequiresCapability', '任务执行需要某能力',
  'metaforge:1.0.0.agent.Task', 'metaforge:1.0.0.agent.Capability',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.StepUsesCapability', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'StepUsesCapability', '步骤执行使用某能力（替代原 CapabilityAssignedTo，方向修正为使用方）',
  'metaforge:1.0.0.agent.ExecutionStep', 'metaforge:1.0.0.agent.Capability',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 27. 任务执行链（任务分解 + 步骤组成 + 步骤顺序 + 跨层级流程 + 决策规则）
-- 27a. 任务起点子任务：任务有且仅有一个起点（第一执行单元可以是子任务；非递归分解，子任务内部有自己的流程）
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.TaskHasEntrySubtask', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'TaskHasEntrySubtask', '任务起点子任务——任务有且仅有一个起点，起点可以是子任务（非递归分解，子任务内部有自己的步骤与流程）',
  'metaforge:1.0.0.agent.Task', 'metaforge:1.0.0.agent.Task',
  'COMPOSITION', '1', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 27b. 任务起点普通步骤：任务有且仅有一个起点（起点可以是普通步骤）
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.TaskHasEntryStep', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'TaskHasEntryStep', '任务起点普通步骤——任务有且仅有一个起点，起点可以是普通执行步骤',
  'metaforge:1.0.0.agent.Task', 'metaforge:1.0.0.agent.ExecutionStep',
  'COMPOSITION', '1', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 27c. 步骤顺序：简单顺序（推荐参考，非强制流程）
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.StepHasNextStep', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'StepHasNextStep', '步骤执行顺序链——前一执行完成后进入后一步骤（简单顺序，非强制）',
  'metaforge:1.0.0.agent.ExecutionStep', 'metaforge:1.0.0.agent.ExecutionStep',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 27c'. 步骤后进入决策步骤：普通步骤完成后进入决策步骤
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.StepHasNextDecisionStep', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'StepHasNextDecisionStep', '普通步骤完成后进入决策步骤（决策步骤作为后续执行单元）',
  'metaforge:1.0.0.agent.ExecutionStep', 'metaforge:1.0.0.agent.DecisionStep',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 27d. 跨层级流程：步骤完成后进入子任务/任务
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.StepHasNextTask', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'StepHasNextTask', '步骤完成后进入下一个任务/子任务（跨层级流程衔接）',
  'metaforge:1.0.0.agent.ExecutionStep', 'metaforge:1.0.0.agent.Task',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 27e. 跨层级流程：子任务/任务完成后回到父流程的后续步骤
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.TaskHasNextStep', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'TaskHasNextStep', '子任务/任务完成后进入父流程的后续步骤（跨层级流程衔接）',
  'metaforge:1.0.0.agent.Task', 'metaforge:1.0.0.agent.ExecutionStep',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 27f. 任务起点决策步骤：任务有且仅有一个起点（起点可以是决策步骤）
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.TaskHasEntryDecisionStep', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'TaskHasEntryDecisionStep', '任务起点决策步骤——任务有且仅有一个起点，起点可以是决策步骤',
  'metaforge:1.0.0.agent.Task', 'metaforge:1.0.0.agent.DecisionStep',
  'COMPOSITION', '1', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 27g. 决策步骤后继：决策分支指向普通步骤
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.DecisionStepHasNextStep', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'DecisionStepHasNextStep', '决策步骤分支走向普通步骤（决策的后继执行单元）',
  'metaforge:1.0.0.agent.DecisionStep', 'metaforge:1.0.0.agent.ExecutionStep',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 27h. 决策步骤后继：决策分支指向另一个决策步骤
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.DecisionStepHasNextDecisionStep', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'DecisionStepHasNextDecisionStep', '决策步骤分支走向另一个决策步骤（嵌套决策）',
  'metaforge:1.0.0.agent.DecisionStep', 'metaforge:1.0.0.agent.DecisionStep',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 27i. 决策步骤后继：决策分支指向子任务/任务
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.DecisionStepHasNextTask', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'DecisionStepHasNextTask', '决策步骤分支走向子任务/任务（决策进入委派）',
  'metaforge:1.0.0.agent.DecisionStep', 'metaforge:1.0.0.agent.Task',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 28. Agent 组成与委派
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.AgentUsesProfile', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentUsesProfile', 'Agent 绑定认知原型配置',
  'metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent.AgentProfile',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.AgentHasPermission', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentHasPermission', 'Agent 持有操作权限',
  'metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent.AgentPermission',
  'ASSOCIATION_REFERENCE', '1', 'N')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.AgentExecutesTask', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentExecutesTask', 'Agent 执行某任务（委派执行目标）',
  'metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent.Task',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.AgentDelegatesTo', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'AgentDelegatesTo', 'Agent 委派子代理（子 Agent 调用）',
  'metaforge:1.0.0.agent.Agent', 'metaforge:1.0.0.agent.Agent',
  'DEPENDENCY_INFLUENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 29. 约束/风险
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.RuleAppliesTo', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RuleAppliesTo', '约束规则适用于步骤（声明式约束，非流程走向）',
  'metaforge:1.0.0.agent.ExecutionRule', 'metaforge:1.0.0.agent.ExecutionStep',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.RuleAppliesToTask', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RuleAppliesToTask', '约束规则适用于任务（声明式约束，可作用于任务整体）',
  'metaforge:1.0.0.agent.ExecutionRule', 'metaforge:1.0.0.agent.Task',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 25. TaskProcessesBusinessObject (agent 任务 → common 业务对象)：任务处理哪个业务对象（处理者→被处理者）
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.TaskProcessesBusinessObject', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'TaskProcessesBusinessObject', '任务处理业务对象——任务作为执行单元消费业务对象（L3）的数据语义，供任务执行时定位与读取业务数据',
  'metaforge:1.0.0.agent.Task', 'metaforge:1.0.0.common.BusinessObject',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- 26. RuleConstrainsAttribute (agent 规则 → common 属性)：约束规则作用于业务对象的属性字段
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.RuleConstrainsAttribute', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RuleConstrainsAttribute', '规则约束属性——约束规则作用于业务对象（L3-L5）的具体属性字段，执行时以属性值对照规则条件',
  'metaforge:1.0.0.agent.ExecutionRule', 'metaforge:1.0.0.common.Attribute',
  'ASSOCIATION_REFERENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.RuleDependsOn', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RuleDependsOn', '规则间依赖链',
  'metaforge:1.0.0.agent.ExecutionRule', 'metaforge:1.0.0.agent.ExecutionRule',
  'DEPENDENCY_INFLUENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.agent.RiskAffects', 'metaforge:1.0.0.agent', 'metaforge:1.0.0',
  'RiskAffects', '风险影响步骤',
  'metaforge:1.0.0.agent.RiskPattern', 'metaforge:1.0.0.agent.ExecutionStep',
  'DEPENDENCY_INFLUENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- protocol 包 RelationSchema（能力引用协议，每协议一条，协议包维护）
-- ============================================================

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.protocol.CapabilityImplementsHttp', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'CapabilityImplementsHttp', '能力通过 HTTP 协议实现——Capability 引用 Http 协议实例',
  'metaforge:1.0.0.agent.Capability', 'metaforge:1.0.0.protocol.Http',
  'ASSOCIATION_REFERENCE', '1', '1')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.protocol.CapabilityImplementsMcpTool', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'CapabilityImplementsMcpTool', '能力通过 MCP 协议实现——Capability 引用 McpTool 协议实例',
  'metaforge:1.0.0.agent.Capability', 'metaforge:1.0.0.protocol.McpTool',
  'ASSOCIATION_REFERENCE', '1', '1')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.protocol.CapabilityImplementsCli', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'CapabilityImplementsCli', '能力通过 CLI 协议实现——Capability 引用 Cli 协议实例',
  'metaforge:1.0.0.agent.Capability', 'metaforge:1.0.0.protocol.Cli',
  'ASSOCIATION_REFERENCE', '1', '1')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('metaforge:1.0.0.protocol.CapabilityImplementsLocalMethod', 'metaforge:1.0.0.protocol', 'metaforge:1.0.0',
  'CapabilityImplementsLocalMethod', '能力通过本地方法实现——Capability 引用 LocalMethod 协议实例',
  'metaforge:1.0.0.agent.Capability', 'metaforge:1.0.0.protocol.LocalMethod',
  'ASSOCIATION_REFERENCE', '1', '1')
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
-- 导出清单 — 导出 common + agent + protocol 三个包
-- ============================================================
INSERT INTO metamodel_governance.export_manifest (bundle_version_fqn, exported_package_fqns)
VALUES ('metaforge:1.0.0', '["metaforge:1.0.0.common","metaforge:1.0.0.agent","metaforge:1.0.0.protocol"]')
ON CONFLICT (bundle_version_fqn) DO NOTHING;
