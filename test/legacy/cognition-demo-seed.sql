-- ============================================================================
-- cognition-demo-seed.sql — 元模型 / 元数据 / 关系 / 认知服务端 四层闭环测试数据 (幂等)
--
-- 目的: 为 opencode AI 命令（metaforge.catalog / navigate / task-brief /
--       step-guide / subtask / guidance）手动验证提供"复杂且闭环"的数据底座,
--       同时覆盖业务（order/erp）与 Agent（metaforge）两个视角。
--
-- 四层结构:
--   Layer 1 元模型 (metamodel_governance)  : bundle / bundle_version / package /
--                                            entity_schema / relation_schema
--   Layer 2 元数据 (metadata_management)   : metadata_entity (M1 实例)
--   Layer 3 关系   (semantic_relation_network): relation_instance
--   Layer 4 认知服务端                      : entity_relation_index 重建
--                                           （compute-engine 图遍历依赖该索引）
--
-- 数据要点:
--   * 业务 Bundle `order:1.0.0`: 完整 8 步履约流程(ENTRY→DECISION→EXIT)、6 规则、
--     4 能力(含 protocol 子类型)、2 决策、2 风险 —— 与 mock 案例对齐
--   * Agent Bundle `metaforge:1.0.0`: 3 任务(归属主题域)、2 Agent(委派)、3 角色、
--     2 权限、1 原型、common L1-L5 四域语义树、protocol 3 类
--   * 全部 INSERT 均 ON CONFLICT DO NOTHING, 可重复执行（幂等）
--   * 最后重建 entity_relation_index（清空重插, 每关系两行 OUTBOUND/INBOUND）
--
-- 用法:
--   PGPASSWORD=metaforge psql -h localhost -U metaforge -d metaforge -f cognition-demo-seed.sql
-- ============================================================================

-- ============================================================================
-- Layer 1: 元模型 (metamodel)
-- ============================================================================

-- ---- 1.1 Bundle ----
INSERT INTO metamodel_governance.bundle (fqn, name, description, owner, is_system) VALUES
  ('metaforge', 'MetaForge 语义基座', 'MetaForge 平台语义基座，提供 Agent 与通用业务语义层元模型定义', 'system', TRUE),
  ('order',     '订单领域', '订单履约业务 Bundle：步骤/规则/能力/决策/风险', 'system', FALSE),
  ('erp',       '企业资源计划', '含订单/库存/采购的复杂业务域', 'lisi', FALSE)
ON CONFLICT (fqn) DO NOTHING;

-- ---- 1.2 BundleVersion (PUBLISHED) ----
INSERT INTO metamodel_governance.bundle_version (fqn, bundle_fqn, status, source_version_fqn, upgrade_level) VALUES
  ('metaforge:1.0.0', 'metaforge', 'PUBLISHED', NULL, NULL),
  ('order:1.0.0',     'order',     'PUBLISHED', NULL, NULL),
  ('erp:1.0.0',       'erp',       'PUBLISHED', NULL, NULL)
ON CONFLICT (fqn) DO NOTHING;

-- ---- 1.3 Package ----
INSERT INTO metamodel_governance.package (fqn, bundle_version_fqn, parent_package_fqn, description, depth) VALUES
  ('metaforge:1.0.0.agent',   'metaforge:1.0.0', NULL, 'Agent 相关元模型：Task/ExecutionStep/Capability/Rule/Decision/Risk/Agent...', 0),
  ('metaforge:1.0.0.protocol', 'metaforge:1.0.0', NULL, '协议能力子类型：HTTP/MCP/CLI/本地方法', 0),
  ('metaforge:1.0.0.common',   'metaforge:1.0.0', NULL, '通用业务语义层级 L1-L5', 0),
  ('order:1.0.0.pkg_order',   'order:1.0.0', NULL, '订单履约语义域包', 0),
  ('erp:1.0.0.pkg_core',      'erp:1.0.0', NULL, 'ERP 核心域：客户/供应商主数据', 0),
  ('erp:1.0.0.pkg_sales',     'erp:1.0.0', NULL, 'ERP 销售域：订单/订单项/仓库', 0)
ON CONFLICT (fqn) DO NOTHING;

-- ---- 1.4 EntitySchema ----
-- metaforge agent/common/protocol 的 20 个 EntitySchema 由 V4 迁移创建, 此处补业务 Bundle。
INSERT INTO metamodel_governance.entity_schema
  (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes) VALUES
('order:1.0.0.pkg_order.Order', 'order:1.0.0.pkg_order', 'order:1.0.0', '订单', '订单实体 Schema',
 '[{"name":"status","type":"string","required":true,"description":"订单状态","constraints":{"enum":["active","inactive","shipped","cancelled"]}},{"name":"price","type":"number","required":false,"description":"订单金额","constraints":{"minimum":0}},{"name":"quantity","type":"integer","required":false,"description":"数量","constraints":{"minimum":0}}]'),
('order:1.0.0.pkg_order.Item', 'order:1.0.0.pkg_order', 'order:1.0.0', '商品', '商品实体 Schema',
 '[{"name":"status","type":"string","required":true,"description":"商品状态","constraints":{"enum":["active","inactive","shipped","cancelled"]}},{"name":"price","type":"number","required":false,"description":"单价","constraints":{"minimum":0}},{"name":"quantity","type":"integer","required":false,"description":"库存","constraints":{"minimum":0}}]'),
('erp:1.0.0.pkg_core.Customer', 'erp:1.0.0.pkg_core', 'erp:1.0.0', '客户', '客户主数据 Schema',
 '[{"name":"customerCode","type":"string","required":true,"description":"客户编码","constraints":{"pattern":"^C-[0-9]{4,}$"}},{"name":"name","type":"string","required":true,"description":"客户名称"},{"name":"vipLevel","type":"string","required":true,"description":"会员等级","constraints":{"enum":["GOLD","SILVER","BRONZE"]}}]'),
('erp:1.0.0.pkg_core.Supplier', 'erp:1.0.0.pkg_core', 'erp:1.0.0', '供应商', '供应商主数据 Schema',
 '[{"name":"supplierCode","type":"string","required":true,"description":"供应商编码","constraints":{"pattern":"^S-[0-9]{4,}$"}},{"name":"name","type":"string","required":true,"description":"供应商名称"}]'),
('erp:1.0.0.pkg_sales.Order', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', '销售订单', '销售订单 Schema',
 '[{"name":"orderNo","type":"string","required":true,"description":"订单号","constraints":{"pattern":"^SO-[0-9]{6,}$"}},{"name":"status","type":"string","required":true,"description":"订单状态","constraints":{"enum":["active","cancelled","shipped"]}},{"name":"amount","type":"number","required":true,"description":"订单金额","constraints":{"minimum":0}}]'),
('erp:1.0.0.pkg_sales.OrderItem', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', '销售订单项', '订单明细行 Schema',
 '[{"name":"sku","type":"string","required":true,"description":"SKU 编码","constraints":{"pattern":"^[A-Z]{2}-[0-9]{4,}$"}},{"name":"quantity","type":"integer","required":true,"description":"数量","constraints":{"minimum":1}}]'),
('erp:1.0.0.pkg_sales.Warehouse', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', '仓库', '仓库主数据 Schema',
 '[{"name":"warehouseCode","type":"string","required":true,"description":"仓库编码","constraints":{"pattern":"^WH-[0-9]{3,}$"}},{"name":"capacity","type":"integer","required":true,"description":"容量","constraints":{"minimum":100}}]')
ON CONFLICT (fqn) DO NOTHING;

-- ---- 1.5 RelationSchema ----
INSERT INTO metamodel_governance.relation_schema
  (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target) VALUES
('order:1.0.0.COMPOSITION',         'order:1.0.0.pkg_order', 'order:1.0.0', 'COMPOSITION', '组成关系', 'order:1.0.0.pkg_order.Order', 'order:1.0.0.pkg_order.Item', 'COMPOSITION', '1', 'N'),
('order:1.0.0.DEPENDENCY_INFLUENCE','order:1.0.0.pkg_order', 'order:1.0.0', 'DEPENDENCY_INFLUENCE', '依赖影响关系', 'order:1.0.0.pkg_order.Order', 'order:1.0.0.pkg_order.Item', 'DEPENDENCY_INFLUENCE', 'N', 'N'),
('order:1.0.0.ASSOCIATION_REFERENCE','order:1.0.0.pkg_order', 'order:1.0.0', 'ASSOCIATION_REFERENCE', '关联引用关系', 'order:1.0.0.pkg_order.Item', 'order:1.0.0.pkg_order.Order', 'ASSOCIATION_REFERENCE', 'N', 'N'),
('order:1.0.0.PROCESS_SEQUENCE',    'order:1.0.0.pkg_order', 'order:1.0.0', 'PROCESS_SEQUENCE', '流程先后关系', 'order:1.0.0.pkg_order.Order', 'order:1.0.0.pkg_order.Order', 'PROCESS_SEQUENCE', 'N', 'N'),
('erp:1.0.0.COMPOSITION',          'erp:1.0.0.pkg_sales', 'erp:1.0.0', 'COMPOSITION', '组成关系', 'erp:1.0.0.pkg_sales.Order', 'erp:1.0.0.pkg_sales.OrderItem', 'COMPOSITION', '1', 'N'),
('erp:1.0.0.DEPENDENCY_INFLUENCE', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', 'DEPENDENCY_INFLUENCE', '依赖影响关系', 'erp:1.0.0.pkg_sales.Order', 'erp:1.0.0.pkg_core.Supplier', 'DEPENDENCY_INFLUENCE', 'N', 'N'),
('erp:1.0.0.ASSOCIATION_REFERENCE', 'erp:1.0.0.pkg_core', 'erp:1.0.0', 'ASSOCIATION_REFERENCE', '关联引用关系', 'erp:1.0.0.pkg_core.Customer', 'erp:1.0.0.pkg_sales.Order', 'ASSOCIATION_REFERENCE', 'N', 'N'),
('erp:1.0.0.PROCESS_SEQUENCE',     'erp:1.0.0.pkg_sales', 'erp:1.0.0', 'PROCESS_SEQUENCE', '流程先后关系', 'erp:1.0.0.pkg_sales.Order', 'erp:1.0.0.pkg_sales.Order', 'PROCESS_SEQUENCE', 'N', 'N'),
('erp:1.0.0.MAPPING_CORRESPONDENCE','erp:1.0.0.pkg_sales', 'erp:1.0.0', 'MAPPING_CORRESPONDENCE', '映射对应关系', 'erp:1.0.0.pkg_sales.OrderItem', 'erp:1.0.0.pkg_sales.Warehouse', 'MAPPING_CORRESPONDENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================================
-- Layer 2: 元数据 (metadata_management.metadata_entity) — M1 实例
-- ============================================================================

-- ---- 2.1 common 包 M1 实例 (L1→L5 业务语义树, 四域) ----
INSERT INTO metadata_management.metadata_entity
  (fqn, name, description, entity_schema_fqn, content, current_version, created_by, updated_by) VALUES
('metaforge:1.0.0.common.Group_OrderFulfillment', '订单履约域', '订单履约相关业务主题的顶层分组', 'metaforge:1.0.0.common.SubjectDomainGroup', '{"group_level":1}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Domain_Order', '订单域', '订单生命周期与下单相关业务主题', 'metaforge:1.0.0.common.SubjectDomain', '{"keywords":["下单","订单","履约"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Domain_Inventory', '库存域', '库存水位、预留与调拨相关业务主题', 'metaforge:1.0.0.common.SubjectDomain', '{"keywords":["库存","预留","调拨"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Domain_Payment', '支付域', '支付、结算与退款相关业务主题', 'metaforge:1.0.0.common.SubjectDomain', '{"keywords":["支付","结算","退款"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Domain_Logistics', '物流域', '发货、配送与签收相关业务主题', 'metaforge:1.0.0.common.SubjectDomain', '{"keywords":["发货","物流","配送"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.BizObj_Order', '订单业务对象', '订单领域的核心业务对象', 'metaforge:1.0.0.common.BusinessObject', '{"aliases":["SO","销售订单"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.BizObj_Inventory', '库存业务对象', '库存领域的核心业务对象', 'metaforge:1.0.0.common.BusinessObject', '{"aliases":["库存","Stock"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.BizObj_Payment', '支付业务对象', '支付领域的核心业务对象', 'metaforge:1.0.0.common.BusinessObject', '{"aliases":["支付单"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.BizObj_Shipment', '发货业务对象', '物流领域的核心业务对象', 'metaforge:1.0.0.common.BusinessObject', '{"aliases":["运单"]}', 1, 'system', 'system'),
('metaforge:1.0.0.common.LogicalEntity_TOrder', '订单逻辑实体', '订单业务对象对应的逻辑实体(表)', 'metaforge:1.0.0.common.LogicalEntity', '{"model_type":"TABLE"}', 1, 'system', 'system'),
('metaforge:1.0.0.common.LogicalEntity_Inventory', '库存逻辑实体', '库存业务对象对应的逻辑实体(表)', 'metaforge:1.0.0.common.LogicalEntity', '{"model_type":"TABLE"}', 1, 'system', 'system'),
('metaforge:1.0.0.common.LogicalEntity_PaymentOrder', '支付单逻辑实体', '支付业务对象对应的逻辑实体(表)', 'metaforge:1.0.0.common.LogicalEntity', '{"model_type":"TABLE"}', 1, 'system', 'system'),
('metaforge:1.0.0.common.LogicalEntity_Shipment', '发货单逻辑实体', '发货业务对象对应的逻辑实体(表)', 'metaforge:1.0.0.common.LogicalEntity', '{"model_type":"TABLE"}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Attribute_OrderStatus', '订单状态字段', '订单状态属性定义', 'metaforge:1.0.0.common.Attribute', '{"data_type":"STRING"}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Attribute_OrderAmount', '订单金额字段', '订单金额属性定义', 'metaforge:1.0.0.common.Attribute', '{"data_type":"DECIMAL"}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Attribute_StockQty', '库存数量字段', '库存数量属性定义', 'metaforge:1.0.0.common.Attribute', '{"data_type":"INTEGER"}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Attribute_AvailableQty', '可售数量字段', '可售库存属性定义', 'metaforge:1.0.0.common.Attribute', '{"data_type":"INTEGER"}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Attribute_PaymentStatus', '支付状态字段', '支付状态属性定义', 'metaforge:1.0.0.common.Attribute', '{"data_type":"STRING"}', 1, 'system', 'system'),
('metaforge:1.0.0.common.Attribute_TrackingNo', '物流单号字段', '物流单号属性定义', 'metaforge:1.0.0.common.Attribute', '{"data_type":"STRING"}', 1, 'system', 'system')
ON CONFLICT (fqn) DO NOTHING;

-- ---- 2.2 agent 包 M1 实例 — 完整 8 步履约流程 ----
INSERT INTO metadata_management.metadata_entity
  (fqn, name, description, entity_schema_fqn, content, current_version, created_by, updated_by) VALUES
-- ExecutionStep (PROCESS_SEQUENCE 链: ENTRY → PROCESSING → DECISION → EXIT)
('order:1.0.0.Step_ReceiveOrder', '接收订单', '接收客户提交的订单请求，完成基础校验与录入。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"ENTRY","estimated_duration":"10秒","responsible_role":"系统自动","input_artifacts":["客户订单请求"],"output_artifacts":["订单草稿"]}', 1, 'system', 'system'),
('order:1.0.0.Step_ConfirmOrder', '确认订单', '校验订单数据完整性并确认订单。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"PROCESSING","estimated_duration":"15秒","responsible_role":"系统自动","input_artifacts":["订单草稿"],"output_artifacts":["已确认订单"]}', 1, 'system', 'system'),
('order:1.0.0.Step_CheckInventory', '库存校验', '校验订单所需商品的可售库存是否充足，不足则触发补货或降级。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"PROCESSING","estimated_duration":"30秒","responsible_role":"系统自动","input_artifacts":["已确认订单","库存快照"],"output_artifacts":["库存校验结果"]}', 1, 'system', 'system'),
('order:1.0.0.Step_CheckPayment', '支付校验', '校验订单支付状态与支付方式，决定走常规或审批路径。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"DECISION","estimated_duration":"10秒","responsible_role":"系统自动","input_artifacts":["订单金额","支付方式"],"output_artifacts":["支付校验结果"]}', 1, 'system', 'system'),
('order:1.0.0.Step_ReserveStock', '锁库存', '为订单预留所需商品库存。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"PROCESSING","estimated_duration":"20秒","responsible_role":"系统自动","input_artifacts":["库存校验结果"],"output_artifacts":["库存预留单"]}', 1, 'system', 'system'),
('order:1.0.0.Step_TriggerApproval', '人工审批', '大额或跨境订单触发人工审批。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"DECISION","estimated_duration":"2小时","responsible_role":"订单审批专员","input_artifacts":["订单详情"],"output_artifacts":["审批结果"]}', 1, 'system', 'system'),
('order:1.0.0.Step_ArrangeShipping', '安排发货', '安排仓库拣货、打包并联系物流承运。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"PROCESSING","estimated_duration":"1小时","responsible_role":"仓库管理员","input_artifacts":["库存预留单"],"output_artifacts":["运单"]}', 1, 'system', 'system'),
('order:1.0.0.Step_CompleteOrder', '完成订单', '标记订单完成并归档。', 'metaforge:1.0.0.agent.ExecutionStep',
 '{"step_type":"EXIT","estimated_duration":"5秒","responsible_role":"系统自动","input_artifacts":["运单"],"output_artifacts":["已完成订单"]}', 1, 'system', 'system'),
-- Capability
('order:1.0.0.Cap_InventoryAPI', '库存查询 API', '查询商品可售库存的 HTTP 能力', 'metaforge:1.0.0.agent.Capability',
 '{"interface_spec":{"endpoint":"/api/v1/inventory/query","method":"GET","output_schema":{"available_qty":"integer","stock_qty":"integer"}},"call_method":"REST","version_label":"v1.0.0","provider":"inventory-bc"}', 1, 'system', 'system'),
('order:1.0.0.Cap_WarehouseAPI', '仓内发货 API', '触发仓库拣货/打包/出库的 HTTP 能力', 'metaforge:1.0.0.agent.Capability',
 '{"interface_spec":{"endpoint":"/api/v1/warehouse/ship","method":"POST"},"call_method":"REST","version_label":"v1.1.0","provider":"warehouse-bc"}', 1, 'system', 'system'),
('order:1.0.0.Cap_PaymentGateway', '支付网关', '查询/确认支付状态的 MCP 能力', 'metaforge:1.0.0.agent.Capability',
 '{"interface_spec":{"server":"payment-mcp-server","tools":["check_payment_status","confirm_payment"]},"call_method":"MCP","version_label":"v2.0.0","provider":"payment-bc"}', 1, 'system', 'system'),
('order:1.0.0.Cap_OrderValidator', '订单校验服务', '对订单数据进行完整性校验的工具能力', 'metaforge:1.0.0.agent.Capability',
 '{"interface_spec":{"method":"validateOrder","input_schema":{"type":"object","properties":{"orderNo":{"type":"string"}}}},"call_method":"INTERNAL","version_label":"v1.2.0","provider":"order-bc"}', 1, 'system', 'system'),
-- ExecutionRule (约束级别分级)
('order:1.0.0.Rule_48hShipping', '48小时发货承诺', '确认订单后 48 小时内必须发货', 'metaforge:1.0.0.agent.ExecutionRule',
 '{"constraint_level":"MANDATORY","condition":"order_status=CONFIRMED","action":"must_ship_within_48h","exception":"force_majeure","applicable_scenarios":["普通订单"],"references":["order:1.0.0.Step_ArrangeShipping"]}', 1, 'system', 'system'),
('order:1.0.0.Rule_InventoryAboveZero', '库存需大于零', '库存校验必须满足库存大于零，否则触发补货', 'metaforge:1.0.0.agent.ExecutionRule',
 '{"constraint_level":"MANDATORY","condition":"requested_qty > available_qty","action":"must_trigger_restock","exception":"force_majeure","applicable_scenarios":["普通订单","促销订单"],"references":["order:1.0.0.Step_CheckInventory"]}', 1, 'system', 'system'),
('order:1.0.0.Rule_InternationalPaymentCheck', '跨境支付强制审批', '跨境支付必须触发审批', 'metaforge:1.0.0.agent.ExecutionRule',
 '{"constraint_level":"MANDATORY","condition":"payment_method=international","action":"must_trigger_approval","exception":null,"applicable_scenarios":["跨境订单"],"references":["order:1.0.0.Step_CheckPayment"]}', 1, 'system', 'system'),
('order:1.0.0.Rule_LargeOrderApproval', '大额订单强制审批', '大额订单必须人工审批', 'metaforge:1.0.0.agent.ExecutionRule',
 '{"constraint_level":"MANDATORY","condition":"order_amount > 10000","action":"must_trigger_human_approval","exception":null,"applicable_scenarios":["大额订单"],"references":["order:1.0.0.Step_TriggerApproval"]}', 1, 'system', 'system'),
('order:1.0.0.Rule_StockReserveTimeout', '库存预留超时释放', '超过 24 小时未确认的预留自动释放', 'metaforge:1.0.0.agent.ExecutionRule',
 '{"constraint_level":"RECOMMENDED","condition":"reservation_age > 24h","action":"release_reservation","exception":null,"applicable_scenarios":["锁库存"],"references":["order:1.0.0.Step_ReserveStock"]}', 1, 'system', 'system'),
('order:1.0.0.Rule_RefundPolicy', '退款政策', '退款请求需遵循退款政策', 'metaforge:1.0.0.agent.ExecutionRule',
 '{"constraint_level":"REFERENCE","condition":"refund_request=valid","action":"must_follow_refund_policy","exception":null,"applicable_scenarios":["售后"],"references":["order:1.0.0.Step_CompleteOrder"]}', 1, 'system', 'system'),
-- DecisionRule
('order:1.0.0.Decision_LargeOrderApproval', '大额订单审批决策', '按订单金额选择支付后处理路径', 'metaforge:1.0.0.agent.DecisionRule',
 '{"condition_expression":"order_amount > 10000","recommended_option":"trigger_approval","rationale":"大额订单需人工审批","priority":1}', 1, 'system', 'system'),
('order:1.0.0.Decision_InternationalApproval', '跨境支付审批决策', '跨境支付走国际支付审批路径', 'metaforge:1.0.0.agent.DecisionRule',
 '{"condition_expression":"payment_method=international","recommended_option":"trigger_approval","rationale":"跨境支付需国际审批路径","priority":2}', 1, 'system', 'system'),
-- RiskPattern
('order:1.0.0.Risk_InventoryLatency', '库存接口超时风险', '库存 API 响应超时导致校验阻塞', 'metaforge:1.0.0.agent.RiskPattern',
 '{"trigger_condition":"库存 API 响应时间 > 3s","impact_description":"订单履约流程阻塞","risk_level":"HIGH","mitigation_measures":["缓存库存快照","超时熔断"],"rollback_strategy":"降级为人工校验"}', 1, 'system', 'system'),
('order:1.0.0.Risk_StockOversold', '库存超卖风险', '并发下单导致库存超卖', 'metaforge:1.0.0.agent.RiskPattern',
 '{"trigger_condition":"并发请求量 > 库存水位","impact_description":"订单无法履约","risk_level":"HIGH","mitigation_measures":["预占库存","乐观锁"],"rollback_strategy":"取消超卖订单"}', 1, 'system', 'system'),
-- Task (3 个任务, 分别归属主题域)
('metaforge:1.0.0.agent.Task_OrderFulfillment', '订单履约主任务', '从下单到发货的完整履约任务', 'metaforge:1.0.0.agent.Task',
 '{"required_role_fqn":"metaforge:1.0.0.agent.Role_OrderSpecialist","entry_step_fqn":"order:1.0.0.Step_ReceiveOrder","delegation_depth_limit":2,"priority_default":"HIGH","estimated_complexity":"COMPLEX"}', 1, 'system', 'system'),
('metaforge:1.0.0.agent.Task_InventoryCheck', '库存校验子任务', '对订单所需商品执行库存校验的子任务', 'metaforge:1.0.0.agent.Task',
 '{"required_role_fqn":"metaforge:1.0.0.agent.Role_InventorySpecialist","entry_step_fqn":"order:1.0.0.Step_CheckInventory","delegation_depth_limit":1,"priority_default":"MEDIUM","estimated_complexity":"MODERATE"}', 1, 'system', 'system'),
('metaforge:1.0.0.agent.Task_PaymentApproval', '支付审批子任务', '大额/跨境订单的支付审批子任务', 'metaforge:1.0.0.agent.Task',
 '{"required_role_fqn":"metaforge:1.0.0.agent.Role_Auditor","entry_step_fqn":"order:1.0.0.Step_TriggerApproval","delegation_depth_limit":1,"priority_default":"CRITICAL","estimated_complexity":"MODERATE"}', 1, 'system', 'system'),
-- Agent
('metaforge:1.0.0.agent.Agent_OrderBot', '订单履约机器人', '负责订单履约主流程的 Agent', 'metaforge:1.0.0.agent.Agent',
 '{"agent_type":"LLM_AGENT","agent_role_summary":"订单履约专家","agent_owner":"order-bc","team_name":"履约团队","deployment_info":{"host":"agent-01","port":9001,"protocol":"mcp"},"is_active":true}', 1, 'system', 'system'),
('metaforge:1.0.0.agent.Agent_InventoryBot', '库存管理机器人', '负责库存查询与调拨的 Agent', 'metaforge:1.0.0.agent.Agent',
 '{"agent_type":"RULE_ENGINE","agent_role_summary":"库存管理专家","agent_owner":"inventory-bc","team_name":"库存团队","deployment_info":{"host":"agent-02","port":9002,"protocol":"mcp"},"is_active":true}', 1, 'system', 'system'),
-- AgentRole
('metaforge:1.0.0.agent.Role_OrderSpecialist', '订单专员', '负责订单履约的 Agent 角色', 'metaforge:1.0.0.agent.AgentRole',
 '{"bound_archetype":"execution","required_capabilities":["order:1.0.0.Cap_OrderValidator"],"authority_level":"FULL_AUTONOMY"}', 1, 'system', 'system'),
('metaforge:1.0.0.agent.Role_InventorySpecialist', '库存专员', '负责库存管理的 Agent 角色', 'metaforge:1.0.0.agent.AgentRole',
 '{"bound_archetype":"execution","required_capabilities":["order:1.0.0.Cap_InventoryAPI"],"authority_level":"SUPERVISED"}', 1, 'system', 'system'),
('metaforge:1.0.0.agent.Role_Auditor', '审计专员', '负责审批与审计的 Agent 角色', 'metaforge:1.0.0.agent.AgentRole',
 '{"bound_archetype":"audit","required_capabilities":[],"authority_level":"SUPERVISED"}', 1, 'system', 'system'),
-- AgentProfile
('metaforge:1.0.0.agent.AgentProfile_Execution', '执行型原型', '执行型 Agent 的认知偏好', 'metaforge:1.0.0.agent.AgentProfile',
 '{"archetype":"execution","default_cognition_depth":"L2","preferred_perspectives":["constraint_set","capability_catalog","flow_blueprint"],"token_budget_default":8000}', 1, 'system', 'system'),
-- AgentPermission
('metaforge:1.0.0.agent.AgentPermission_OrderRead', '订单读权限', '允许读取订单数据的权限', 'metaforge:1.0.0.agent.AgentPermission',
 '{"permission_type":"METADATA_READ","granted":true,"allowed_bundle_fqns":["order:1.0.0","metaforge:1.0.0"],"granted_by":"admin","expires_at":null}', 1, 'system', 'system'),
('metaforge:1.0.0.agent.AgentPermission_CognitionQuery', '认知查询权限', '允许发起认知查询的权限', 'metaforge:1.0.0.agent.AgentPermission',
 '{"permission_type":"COGNITION_QUERY","granted":true,"allowed_bundle_fqns":["order:1.0.0","metaforge:1.0.0"],"granted_by":"admin","expires_at":null}', 1, 'system', 'system'),
-- CostEstimate
('metaforge:1.0.0.agent.CostEstimate_InventoryCheck', '库存校验成本', '库存校验操作的预估成本', 'metaforge:1.0.0.agent.CostEstimate',
 '{"operation_fqn":"order:1.0.0.Step_CheckInventory","cost_value":0.5,"cost_unit":"元/次","estimation_method":"线性回归"}', 1, 'system', 'system')
ON CONFLICT (fqn) DO NOTHING;

-- ---- 2.3 protocol 包 M1 实例 ----
INSERT INTO metadata_management.metadata_entity
  (fqn, name, description, entity_schema_fqn, content, current_version, created_by, updated_by) VALUES
('metaforge:1.0.0.protocol.Http_InventoryQuery', '库存查询 HTTP 能力', '库存查询的 HTTP 协议能力定义', 'metaforge:1.0.0.protocol.Http',
 '{"endpoint":"/api/v1/inventory/query","method":"GET","headers":{"X-Request-Id":"uuid"}}', 1, 'system', 'system'),
('metaforge:1.0.0.protocol.Http_WarehouseShip', '仓内发货 HTTP 能力', '仓内发货的 HTTP 协议能力定义', 'metaforge:1.0.0.protocol.Http',
 '{"endpoint":"/api/v1/warehouse/ship","method":"POST","headers":{"X-Request-Id":"uuid"}}', 1, 'system', 'system'),
('metaforge:1.0.0.protocol.McpTool_PaymentGateway', '支付网关 MCP 工具', '支付网关的 MCP 协议能力定义', 'metaforge:1.0.0.protocol.McpTool',
 '{"server_name":"payment-mcp-server","arguments_schema":{"type":"object","properties":{"paymentId":{"type":"string"}}}}', 1, 'system', 'system'),
('metaforge:1.0.0.protocol.LocalMethod_OrderValidator', '订单校验本地方法', '订单校验的本地方法调用定义', 'metaforge:1.0.0.protocol.LocalMethod',
 '{"class_path":"com.metaforge.service.OrderService","method_name":"validateOrder","parameters":{"orderNo":"string"}}', 1, 'system', 'system')
ON CONFLICT (fqn) DO NOTHING;

-- ---- 2.4 业务 Bundle M1 实例 (order / erp) ----
INSERT INTO metadata_management.metadata_entity
  (fqn, name, description, entity_schema_fqn, content, current_version, created_by, updated_by) VALUES
('order:1.0.0.pkg_order.Order_001', '订单1', '主订单', 'order:1.0.0.pkg_order.Order', '{"price":1999.5,"status":"active"}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Order_002', '订单2', '履约中', 'order:1.0.0.pkg_order.Order', '{"price":899.0,"status":"shipped"}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Order_003', '订单3', '促销订单', 'order:1.0.0.pkg_order.Order', '{"price":2400.0,"status":"active"}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_003', '商品3', '电子品类', 'order:1.0.0.pkg_order.Item', '{"price":150.0,"status":"active","quantity":3}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_004', '商品4', '图书品类', 'order:1.0.0.pkg_order.Item', '{"price":80.0,"status":"active","quantity":5}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_005', '商品5', '服装品类', 'order:1.0.0.pkg_order.Item', '{"price":50.0,"status":"cancelled","quantity":2}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_006', '商品6', '家居品类', 'order:1.0.0.pkg_order.Item', '{"price":320.0,"status":"active","quantity":1}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_007', '商品7', '美妆品类', 'order:1.0.0.pkg_order.Item', '{"price":210.0,"status":"active","quantity":4}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_008', '商品8', '食品品类', 'order:1.0.0.pkg_order.Item', '{"price":30.0,"status":"inactive","quantity":9}', 1, 'system', 'system'),
('Customer_001', '张三', '黄金会员客户', 'erp:1.0.0.pkg_core.Customer', '{"name":"张三","vipLevel":"GOLD","customerCode":"C-1001"}', 1, 'system', 'system'),
('Customer_002', '李四', '白银会员客户', 'erp:1.0.0.pkg_core.Customer', '{"name":"李四","vipLevel":"SILVER","customerCode":"C-1002"}', 1, 'system', 'system'),
('Supplier_001', '原料供应商', '华东区原材料供应商', 'erp:1.0.0.pkg_core.Supplier', '{"name":"原料供应商","supplierCode":"S-2001"}', 1, 'system', 'system'),
('Order_100', '订单100', '金额1999.5的活跃订单', 'erp:1.0.0.pkg_sales.Order', '{"amount":1999.5,"status":"active","orderNo":"SO-100001"}', 1, 'system', 'system'),
('Order_200', '订单200', '已发货订单', 'erp:1.0.0.pkg_sales.Order', '{"amount":899.0,"status":"shipped","orderNo":"SO-100002"}', 1, 'system', 'system'),
('OrderItem_101', '订单项101', '订单100的明细行', 'erp:1.0.0.pkg_sales.OrderItem', '{"sku":"AB-12345","quantity":3}', 1, 'system', 'system'),
('OrderItem_102', '订单项102', '订单200的明细行', 'erp:1.0.0.pkg_sales.OrderItem', '{"sku":"CD-67890","quantity":5}', 1, 'system', 'system'),
('Warehouse_301', '华东一号仓', '容量5000的主力仓库', 'erp:1.0.0.pkg_sales.Warehouse', '{"capacity":5000,"warehouseCode":"WH-301"}', 1, 'system', 'system')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================================
-- Layer 3: 关系 (semantic_relation_network.relation_instance)
-- ============================================================================

INSERT INTO semantic_relation_network.relation_instance
  (fqn, name, description, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content, current_version) VALUES

-- ---- 3.1 PROCESS_SEQUENCE — 完整 8 步履约链 (含决策分支) ----
('order:1.0.0.rel.ReceiveOrder_To_ConfirmOrder', '接收订单后确认', '接收订单后进入确认订单', 'order:1.0.0.Step_ReceiveOrder', 'order:1.0.0.Step_ConfirmOrder', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":1}', 1),
('order:1.0.0.rel.ConfirmOrder_To_CheckInventory', '确认后校验库存', '确认订单后进入库存校验', 'order:1.0.0.Step_ConfirmOrder', 'order:1.0.0.Step_CheckInventory', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":2}', 1),
('order:1.0.0.rel.CheckInventory_To_CheckPayment', '库存校验后支付校验', '库存校验后进入支付校验', 'order:1.0.0.Step_CheckInventory', 'order:1.0.0.Step_CheckPayment', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":3}', 1),
('order:1.0.0.rel.CheckPayment_To_ReserveStock', '支付校验后锁库存', '常规订单支付校验后直接锁库存', 'order:1.0.0.Step_CheckPayment', 'order:1.0.0.Step_ReserveStock', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":4,"condition":"order_amount<=10000 AND payment_method!=international"}', 1),
('order:1.0.0.rel.CheckPayment_To_TriggerApproval', '支付校验后人工审批', '大额/跨境订单支付校验后转人工审批', 'order:1.0.0.Step_CheckPayment', 'order:1.0.0.Step_TriggerApproval', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":5,"condition":"order_amount>10000 OR payment_method=international"}', 1),
('order:1.0.0.rel.TriggerApproval_To_ReserveStock', '审批后锁库存', '审批通过后进入锁库存', 'order:1.0.0.Step_TriggerApproval', 'order:1.0.0.Step_ReserveStock', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":6}', 1),
('order:1.0.0.rel.ReserveStock_To_ArrangeShipping', '锁库存后安排发货', '锁库存后进入安排发货', 'order:1.0.0.Step_ReserveStock', 'order:1.0.0.Step_ArrangeShipping', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":7}', 1),
('order:1.0.0.rel.ArrangeShipping_To_CompleteOrder', '发货后完成订单', '安排发货后进入完成订单', 'order:1.0.0.Step_ArrangeShipping', 'order:1.0.0.Step_CompleteOrder', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepSequencedBy', '{"step":8}', 1),

-- ---- 3.2 ASSOCIATION_REFERENCE — 规则/能力/决策绑定步骤 ----
('order:1.0.0.rel.Rule48h_Applies_ArrangeShipping', '48小时规则约束发货', '48小时发货承诺适用于安排发货', 'order:1.0.0.Rule_48hShipping', 'order:1.0.0.Step_ArrangeShipping', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{"rule":"48h"}', 1),
('order:1.0.0.rel.RuleInvZero_Applies_CheckInventory', '库存规则约束库存校验', '库存需大于零适用于库存校验', 'order:1.0.0.Rule_InventoryAboveZero', 'order:1.0.0.Step_CheckInventory', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{"rule":"inventory"}', 1),
('order:1.0.0.rel.RuleIntl_Applies_CheckPayment', '跨境规则约束支付校验', '跨境支付强制审批适用于支付校验', 'order:1.0.0.Rule_InternationalPaymentCheck', 'order:1.0.0.Step_CheckPayment', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{"rule":"international"}', 1),
('order:1.0.0.rel.RuleLargeOrder_Applies_CheckPayment', '大额规则约束支付校验', '大额订单强制审批适用于支付校验', 'order:1.0.0.Rule_LargeOrderApproval', 'order:1.0.0.Step_CheckPayment', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{"rule":"large-order"}', 1),
('order:1.0.0.rel.RuleLargeOrder_Applies_TriggerApproval', '大额规则约束人工审批', '大额订单强制审批适用于人工审批', 'order:1.0.0.Rule_LargeOrderApproval', 'order:1.0.0.Step_TriggerApproval', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{"rule":"large-order"}', 1),
('order:1.0.0.rel.RuleReserveTimeout_Applies_ReserveStock', '预留超时规则约束锁库存', '库存预留超时释放适用于锁库存', 'order:1.0.0.Rule_StockReserveTimeout', 'order:1.0.0.Step_ReserveStock', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{"rule":"reserve-timeout"}', 1),
('order:1.0.0.rel.CapInv_Assigned_CheckInventory', '库存 API 分配给库存校验', '库存查询能力分配到库存校验步骤', 'order:1.0.0.Cap_InventoryAPI', 'order:1.0.0.Step_CheckInventory', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.CapabilityAssignedTo', '{"capability":"inventory-api"}', 1),
('order:1.0.0.rel.CapPay_Assigned_CheckPayment', '支付网关分配给支付校验', '支付网关能力分配到支付校验步骤', 'order:1.0.0.Cap_PaymentGateway', 'order:1.0.0.Step_CheckPayment', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.CapabilityAssignedTo', '{"capability":"payment-gateway"}', 1),
('order:1.0.0.rel.CapWh_Assigned_ArrangeShipping', '仓内 API 分配给安排发货', '仓内发货能力分配到安排发货步骤', 'order:1.0.0.Cap_WarehouseAPI', 'order:1.0.0.Step_ArrangeShipping', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.CapabilityAssignedTo', '{"capability":"warehouse-api"}', 1),
('order:1.0.0.rel.CapVal_Assigned_ConfirmOrder', '订单校验分配给确认订单', '订单校验能力分配到确认订单步骤', 'order:1.0.0.Cap_OrderValidator', 'order:1.0.0.Step_ConfirmOrder', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.CapabilityAssignedTo', '{"capability":"order-validator"}', 1),
('order:1.0.0.rel.CheckPayment_HasDecision_LargeOrder', '支付校验关联大额决策', '支付校验步骤关联大额订单决策', 'order:1.0.0.Step_CheckPayment', 'order:1.0.0.Decision_LargeOrderApproval', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepHasDecision', '{"decision":"large-order"}', 1),
('order:1.0.0.rel.CheckPayment_HasDecision_International', '支付校验关联跨境决策', '支付校验步骤关联跨境支付决策', 'order:1.0.0.Step_CheckPayment', 'order:1.0.0.Decision_InternationalApproval', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepHasDecision', '{"decision":"international"}', 1),

-- ---- 3.3 DEPENDENCY_INFLUENCE — 前置/风险/规则依赖 ----
('order:1.0.0.rel.Precond_CheckInventory_Needs_ConfirmOrder', '库存校验前置确认订单', '库存校验依赖确认订单完成', 'order:1.0.0.Step_CheckInventory', 'order:1.0.0.Step_ConfirmOrder', 'DEPENDENCY_INFLUENCE', 'metaforge:1.0.0.agent.StepHasPrecondition', '{"precondition":"confirmed"}', 1),
('order:1.0.0.rel.Precond_ReserveStock_Needs_CheckInventory', '锁库存前置库存校验', '锁库存依赖库存校验完成', 'order:1.0.0.Step_ReserveStock', 'order:1.0.0.Step_CheckInventory', 'DEPENDENCY_INFLUENCE', 'metaforge:1.0.0.agent.StepHasPrecondition', '{"precondition":"inventory-ok"}', 1),
('order:1.0.0.rel.Precond_ArrangeShipping_Needs_ReserveStock', '安排发货前置锁库存', '安排发货依赖锁库存完成', 'order:1.0.0.Step_ArrangeShipping', 'order:1.0.0.Step_ReserveStock', 'DEPENDENCY_INFLUENCE', 'metaforge:1.0.0.agent.StepHasPrecondition', '{"precondition":"reserved"}', 1),
('order:1.0.0.rel.RiskLatency_Affects_CheckInventory', '库存延迟风险影响库存校验', '库存接口超时风险影响库存校验步骤', 'order:1.0.0.Risk_InventoryLatency', 'order:1.0.0.Step_CheckInventory', 'DEPENDENCY_INFLUENCE', 'metaforge:1.0.0.agent.RiskAffects', '{"risk":"latency"}', 1),
('order:1.0.0.rel.RiskOversold_Affects_ReserveStock', '超卖风险影响锁库存', '库存超卖风险影响锁库存步骤', 'order:1.0.0.Risk_StockOversold', 'order:1.0.0.Step_ReserveStock', 'DEPENDENCY_INFLUENCE', 'metaforge:1.0.0.agent.RiskAffects', '{"risk":"oversold"}', 1),
('order:1.0.0.rel.RuleLargeOrder_Depends_Intl', '大额规则依赖跨境规则', '大额订单审批规则依赖跨境支付规则', 'order:1.0.0.Rule_LargeOrderApproval', 'order:1.0.0.Rule_InternationalPaymentCheck', 'DEPENDENCY_INFLUENCE', 'metaforge:1.0.0.agent.RuleDependsOn', '{"depends":"intl"}', 1),

-- ---- 3.4 COMPOSITION — common L1-L5 树 + Task 归属 + protocol 构成 ----
('metaforge:1.0.0.common.rel.Group_Contains_OrderDomain', '订单履约域含订单域', '主题域分组包含订单域', 'metaforge:1.0.0.common.Group_OrderFulfillment', 'metaforge:1.0.0.common.Domain_Order', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{"level":"L1-L2"}', 1),
('metaforge:1.0.0.common.rel.Group_Contains_InventoryDomain', '订单履约域含库存域', '主题域分组包含库存域', 'metaforge:1.0.0.common.Group_OrderFulfillment', 'metaforge:1.0.0.common.Domain_Inventory', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{"level":"L1-L2"}', 1),
('metaforge:1.0.0.common.rel.Group_Contains_PaymentDomain', '订单履约域含支付域', '主题域分组包含支付域', 'metaforge:1.0.0.common.Group_OrderFulfillment', 'metaforge:1.0.0.common.Domain_Payment', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{"level":"L1-L2"}', 1),
('metaforge:1.0.0.common.rel.Group_Contains_LogisticsDomain', '订单履约域含物流域', '主题域分组包含物流域', 'metaforge:1.0.0.common.Group_OrderFulfillment', 'metaforge:1.0.0.common.Domain_Logistics', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{"level":"L1-L2"}', 1),
('metaforge:1.0.0.common.rel.OrderDomain_Contains_BizObjOrder', '订单域含订单业务对象', '订单主题域包含订单业务对象', 'metaforge:1.0.0.common.Domain_Order', 'metaforge:1.0.0.common.BizObj_Order', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{"level":"L2-L3"}', 1),
('metaforge:1.0.0.common.rel.InventoryDomain_Contains_BizObjInventory', '库存域含库存业务对象', '库存主题域包含库存业务对象', 'metaforge:1.0.0.common.Domain_Inventory', 'metaforge:1.0.0.common.BizObj_Inventory', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{"level":"L2-L3"}', 1),
('metaforge:1.0.0.common.rel.PaymentDomain_Contains_BizObjPayment', '支付域含支付业务对象', '支付主题域包含支付业务对象', 'metaforge:1.0.0.common.Domain_Payment', 'metaforge:1.0.0.common.BizObj_Payment', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{"level":"L2-L3"}', 1),
('metaforge:1.0.0.common.rel.LogisticsDomain_Contains_BizObjShipment', '物流域含发货业务对象', '物流主题域包含发货业务对象', 'metaforge:1.0.0.common.Domain_Logistics', 'metaforge:1.0.0.common.BizObj_Shipment', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{"level":"L2-L3"}', 1),
('metaforge:1.0.0.common.rel.BizObjOrder_Refines_TOrder', '订单业务对象细化订单表', '订单业务对象细化到订单逻辑实体', 'metaforge:1.0.0.common.BizObj_Order', 'metaforge:1.0.0.common.LogicalEntity_TOrder', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{"level":"L3-L4"}', 1),
('metaforge:1.0.0.common.rel.BizObjInventory_Refines_Inventory', '库存业务对象细化库存表', '库存业务对象细化到库存逻辑实体', 'metaforge:1.0.0.common.BizObj_Inventory', 'metaforge:1.0.0.common.LogicalEntity_Inventory', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{"level":"L3-L4"}', 1),
('metaforge:1.0.0.common.rel.BizObjPayment_Refines_PaymentOrder', '支付业务对象细化支付单表', '支付业务对象细化到支付单逻辑实体', 'metaforge:1.0.0.common.BizObj_Payment', 'metaforge:1.0.0.common.LogicalEntity_PaymentOrder', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{"level":"L3-L4"}', 1),
('metaforge:1.0.0.common.rel.BizObjShipment_Refines_Shipment', '发货业务对象细化发货表', '发货业务对象细化到发货逻辑实体', 'metaforge:1.0.0.common.BizObj_Shipment', 'metaforge:1.0.0.common.LogicalEntity_Shipment', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{"level":"L3-L4"}', 1),
('metaforge:1.0.0.common.rel.TOrder_Contains_OrderStatus', '订单表含状态字段', '订单逻辑实体包含订单状态属性', 'metaforge:1.0.0.common.LogicalEntity_TOrder', 'metaforge:1.0.0.common.Attribute_OrderStatus', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{"level":"L4-L5"}', 1),
('metaforge:1.0.0.common.rel.TOrder_Contains_OrderAmount', '订单表含金额字段', '订单逻辑实体包含订单金额属性', 'metaforge:1.0.0.common.LogicalEntity_TOrder', 'metaforge:1.0.0.common.Attribute_OrderAmount', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{"level":"L4-L5"}', 1),
('metaforge:1.0.0.common.rel.Inventory_Contains_StockQty', '库存表含数量字段', '库存逻辑实体包含库存数量属性', 'metaforge:1.0.0.common.LogicalEntity_Inventory', 'metaforge:1.0.0.common.Attribute_StockQty', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{"level":"L4-L5"}', 1),
('metaforge:1.0.0.common.rel.Inventory_Contains_AvailableQty', '库存表含可售字段', '库存逻辑实体包含可售数量属性', 'metaforge:1.0.0.common.LogicalEntity_Inventory', 'metaforge:1.0.0.common.Attribute_AvailableQty', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{"level":"L4-L5"}', 1),
('metaforge:1.0.0.common.rel.PaymentOrder_Contains_PaymentStatus', '支付单表含状态字段', '支付单逻辑实体包含支付状态属性', 'metaforge:1.0.0.common.LogicalEntity_PaymentOrder', 'metaforge:1.0.0.common.Attribute_PaymentStatus', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{"level":"L4-L5"}', 1),
('metaforge:1.0.0.common.rel.Shipment_Contains_TrackingNo', '发货表含物流号字段', '发货逻辑实体包含物流单号属性', 'metaforge:1.0.0.common.LogicalEntity_Shipment', 'metaforge:1.0.0.common.Attribute_TrackingNo', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{"level":"L4-L5"}', 1),
-- Task 归属主题域
('metaforge:1.0.0.agent.rel.TaskFulfillment_BelongsTo_OrderDomain', '履约任务归属订单域', '订单履约任务属于订单主题域', 'metaforge:1.0.0.agent.Task_OrderFulfillment', 'metaforge:1.0.0.common.Domain_Order', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskBelongsToSubjectDomain', '{"domain":"order"}', 1),
('metaforge:1.0.0.agent.rel.TaskInventoryCheck_BelongsTo_InventoryDomain', '库存校验归属库存域', '库存校验任务属于库存主题域', 'metaforge:1.0.0.agent.Task_InventoryCheck', 'metaforge:1.0.0.common.Domain_Inventory', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskBelongsToSubjectDomain', '{"domain":"inventory"}', 1),
('metaforge:1.0.0.agent.rel.TaskPaymentApproval_BelongsTo_PaymentDomain', '支付审批归属支付域', '支付审批任务属于支付主题域', 'metaforge:1.0.0.agent.Task_PaymentApproval', 'metaforge:1.0.0.common.Domain_Payment', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskBelongsToSubjectDomain', '{"domain":"payment"}', 1),
-- protocol → Capability 构成
('metaforge:1.0.0.protocol.rel.HttpInv_TypesAs_CapInv', '库存 HTTP 能力构成库存 API', '库存查询 HTTP 能力构成库存查询能力', 'metaforge:1.0.0.protocol.Http_InventoryQuery', 'order:1.0.0.Cap_InventoryAPI', 'COMPOSITION', 'metaforge:1.0.0.protocol.HttpTypesAs', '{"protocol":"http"}', 1),
('metaforge:1.0.0.protocol.rel.HttpWh_TypesAs_CapWh', '发货 HTTP 能力构成仓内 API', '仓内发货 HTTP 能力构成仓内发货能力', 'metaforge:1.0.0.protocol.Http_WarehouseShip', 'order:1.0.0.Cap_WarehouseAPI', 'COMPOSITION', 'metaforge:1.0.0.protocol.HttpTypesAs', '{"protocol":"http"}', 1),
('metaforge:1.0.0.protocol.rel.McpPay_TypesAs_CapPay', '支付 MCP 能力构成支付网关', '支付网关 MCP 能力构成支付网关能力', 'metaforge:1.0.0.protocol.McpTool_PaymentGateway', 'order:1.0.0.Cap_PaymentGateway', 'COMPOSITION', 'metaforge:1.0.0.protocol.McpToolTypesAs', '{"protocol":"mcp"}', 1),
('metaforge:1.0.0.protocol.rel.LocalVal_TypesAs_CapVal', '校验本地方法构成订单校验', '订单校验本地方法构成订单校验能力', 'metaforge:1.0.0.protocol.LocalMethod_OrderValidator', 'order:1.0.0.Cap_OrderValidator', 'COMPOSITION', 'metaforge:1.0.0.protocol.LocalMethodTypesAs', '{"protocol":"local"}', 1),
-- Agent 生态关系
('metaforge:1.0.0.agent.rel.OrderBot_Uses_ProfileExecution', '履约机器人绑定执行原型', '履约 Agent 绑定执行型认知原型', 'metaforge:1.0.0.agent.Agent_OrderBot', 'metaforge:1.0.0.agent.AgentProfile_Execution', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentUsesProfile', '{"preference":"execution"}', 1),
('metaforge:1.0.0.agent.rel.OrderBot_Has_RoleOrderSpecialist', '履约机器人持订单专员角色', '履约 Agent 持有订单专员角色', 'metaforge:1.0.0.agent.Agent_OrderBot', 'metaforge:1.0.0.agent.Role_OrderSpecialist', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasRole', '{"role":"order"}', 1),
('metaforge:1.0.0.agent.rel.OrderBot_Has_PermCognition', '履约机器人持认知查询权限', '履约 Agent 持有认知查询权限', 'metaforge:1.0.0.agent.Agent_OrderBot', 'metaforge:1.0.0.agent.AgentPermission_CognitionQuery', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasPermission', '{"permission":"cognition"}', 1),
('metaforge:1.0.0.agent.rel.InventoryBot_Has_RoleInventorySpecialist', '库存机器人持库存专员角色', '库存 Agent 持有库存专员角色', 'metaforge:1.0.0.agent.Agent_InventoryBot', 'metaforge:1.0.0.agent.Role_InventorySpecialist', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasRole', '{"role":"inventory"}', 1),
('metaforge:1.0.0.agent.rel.InventoryBot_Has_PermOrderRead', '库存机器人持订单读权限', '库存 Agent 持有订单读权限', 'metaforge:1.0.0.agent.Agent_InventoryBot', 'metaforge:1.0.0.agent.AgentPermission_OrderRead', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasPermission', '{"permission":"read"}', 1),
('metaforge:1.0.0.agent.rel.OrderBot_Delegates_InventoryBot', '履约机器人委派库存机器人', '履约 Agent 委派库存 Agent', 'metaforge:1.0.0.agent.Agent_OrderBot', 'metaforge:1.0.0.agent.Agent_InventoryBot', 'DEPENDENCY_INFLUENCE', 'metaforge:1.0.0.agent.AgentDelegatesTo', '{"delegate":"inventory"}', 1),
('metaforge:1.0.0.agent.rel.RoleOrder_Assigned_TaskFulfillment', '订单专员分配履约任务', '订单专员角色分配到履约任务', 'metaforge:1.0.0.agent.Role_OrderSpecialist', 'metaforge:1.0.0.agent.Task_OrderFulfillment', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RoleAssignedToTask', '{"role":"order"}', 1),
('metaforge:1.0.0.agent.rel.RoleInv_Assigned_TaskInventory', '库存专员分配库存校验任务', '库存专员角色分配到库存校验任务', 'metaforge:1.0.0.agent.Role_InventorySpecialist', 'metaforge:1.0.0.agent.Task_InventoryCheck', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RoleAssignedToTask', '{"role":"inventory"}', 1),
('metaforge:1.0.0.agent.rel.RoleAuditor_Assigned_TaskPayment', '审计专员分配支付审批任务', '审计专员角色分配到支付审批任务', 'metaforge:1.0.0.agent.Role_Auditor', 'metaforge:1.0.0.agent.Task_PaymentApproval', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RoleAssignedToTask', '{"role":"auditor"}', 1),
-- 业务关系 (order 业务对象 / erp 业务)
('order:1.0.0.pkg_order.rel.Order1_Contains_Item3', '订单1包含商品3', '订单1 组成含商品3', 'order:1.0.0.pkg_order.Order_001', 'order:1.0.0.pkg_order.Item_003', 'COMPOSITION', 'order:1.0.0.COMPOSITION', '{"quantity":3}', 1),
('order:1.0.0.pkg_order.rel.Order1_Depends_Item5', '订单1依赖商品5', '订单1 依赖商品5', 'order:1.0.0.pkg_order.Order_001', 'order:1.0.0.pkg_order.Item_005', 'DEPENDENCY_INFLUENCE', 'order:1.0.0.DEPENDENCY_INFLUENCE', '{"level":"HIGH"}', 1),
('order:1.0.0.pkg_order.rel.Order1_Seq_Order2', '订单1后置订单2', '订单1 后置处理订单2', 'order:1.0.0.pkg_order.Order_001', 'order:1.0.0.pkg_order.Order_002', 'PROCESS_SEQUENCE', 'order:1.0.0.PROCESS_SEQUENCE', '{"step":1}', 1),
('order:1.0.0.pkg_order.rel.Item3_Assoc_Order1', '商品3关联订单1', '商品3 关联订单1', 'order:1.0.0.pkg_order.Item_003', 'order:1.0.0.pkg_order.Order_001', 'ASSOCIATION_REFERENCE', 'order:1.0.0.ASSOCIATION_REFERENCE', '{"source":"direct"}', 1),
('erp.rel.Customer1_Orders_Order100', '客户1下单100', '客户张三关联订单100', 'Customer_001', 'Order_100', 'ASSOCIATION_REFERENCE', 'erp:1.0.0.ASSOCIATION_REFERENCE', '{"source":"direct"}', 1),
('erp.rel.Order100_Composes_OrderItem101', '订单100含条目101', '订单100 组成含订单项101', 'Order_100', 'OrderItem_101', 'COMPOSITION', 'erp:1.0.0.COMPOSITION', '{"quantity":3,"unitPrice":499.0}', 1),
('erp.rel.Order100_Depends_Supplier1', '订单100依赖供应商1', '订单100 依赖供应商001', 'Order_100', 'Supplier_001', 'DEPENDENCY_INFLUENCE', 'erp:1.0.0.DEPENDENCY_INFLUENCE', '{"level":"HIGH"}', 1),
('erp.rel.Order100_Seq_Order200', '订单100后处理200', '订单100 后置处理订单200', 'Order_100', 'Order_200', 'PROCESS_SEQUENCE', 'erp:1.0.0.PROCESS_SEQUENCE', '{"step":1}', 1),
('erp.rel.OrderItem101_Maps_Warehouse301', '条目101入仓301', '订单项101 映射仓库301', 'OrderItem_101', 'Warehouse_301', 'MAPPING_CORRESPONDENCE', 'erp:1.0.0.MAPPING_CORRESPONDENCE', '{"bin":"A-03"}', 1)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================================
-- Layer 4: 认知服务端 — 重建实体双向索引 (entity_relation_index)
--    compute-engine 图遍历依赖该索引表; 幂等: 先清空再插入, 每关系两行。
-- ============================================================================
DELETE FROM semantic_relation_network.entity_relation_index;
INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT source_entity_fqn, 'OUTBOUND', fqn
FROM semantic_relation_network.relation_instance
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;
INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT target_entity_fqn, 'INBOUND', fqn
FROM semantic_relation_network.relation_instance
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

-- ============================================================================
-- 结束
-- ============================================================================
