-- ============================================================
-- example-supply-chain-replenishment seed
-- 供应链库存补货（复杂度增强版）
-- 核心：3 个业务对象（库存表/供应商表/补货单表）作为 L3-L5 元数据；
--       主任务决策嵌套（补货判断 → 供应商择优）；业务数据独立放文件。
--
-- 流程拓扑（Task_Replenishment）：
--   Step_ReadInventory(ENTRY)
--     → DecisionStep_StockCheck（决策1：补货判断）
--          ├─ 库存≤补货阈值 → Task_SupplierSelection（供应商选择子任务）
--          └─ 库存>阈值 → Step_NoReplenish(EXIT)
--   供应商选择子任务 Task_SupplierSelection（决策嵌套）：
--     Step_FetchSuppliers(ENTRY)
--       → DecisionStep_SupplierPick（决策2：供应商择优）
--            ├─ 可靠性分≥90 → Step_SelectPreferredSupplier
--            └─ 否则 → Step_SelectBestValueSupplier
--       → Step_GeneratePurchaseOrder（生成补货单，处理 BO_PurchaseOrder）
--
-- 业务对象：
--   补货任务 → BO_Inventory；供应商选择子任务 → BO_Supplier + BO_PurchaseOrder
-- 规则（3 条）：
--   Rule_ReorderRule（库存≤阈值触发补货，MANDATORY）→ 约束 StockQty/ReorderLevel
--   Rule_SupplierPriority（可靠性分≥90 优先，RECOMMENDED）→ 约束 ReliabilityScore
--   Rule_MaxStock（补货量≤最大库存，RECOMMENDED）→ 约束 MaxStock
-- ============================================================

-- ---------- 1. 业务对象结构（L3-L5 表结构元数据） ----------

-- 1.0 供应链域组（L1）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Group_SupplyChain', '供应链域组', NULL, 'metaforge:1.0.0.common.SubjectDomainGroup',
  '{"group_level":1,"description":"供应链与库存管理域分组"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.1 库存管理域（L2）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Domain_InventoryMgmt', '库存管理域', 'metaforge:1.0.0.common.Group_SupplyChain', 'metaforge:1.0.0.common.SubjectDomain',
  '{"domain_level":2,"description":"供应链库存补货与供应商管理"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.2 库存业务对象（L3 = 库存快照表）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.BO_Inventory', '库存业务对象', 'metaforge:1.0.0.common.Domain_InventoryMgmt', 'metaforge:1.0.0.common.BusinessObject',
  '{"object_type":"TABLE","logical_entity":"metaforge:1.0.0.common.LE_Inventory","description":"商品库存快照表"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.LE_Inventory', '库存逻辑实体', 'metaforge:1.0.0.common.BO_Inventory', 'metaforge:1.0.0.common.LogicalEntity',
  '{"table_name":"inventory_snapshot","source":"examples/example-supply-chain-replenishment/data/inventory/","description":"商品库存快照逻辑落地"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 库存字段（L5）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_SkuId', '商品SKU', 'metaforge:1.0.0.common.LE_Inventory', 'metaforge:1.0.0.common.Attribute',
  '{"field":"sku_id","type":"string","description":"商品唯一标识"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_StockQty', '当前库存', 'metaforge:1.0.0.common.LE_Inventory', 'metaforge:1.0.0.common.Attribute',
  '{"field":"stock_qty","type":"number","unit":"件","description":"当前库存数量"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_ReorderLevel', '补货阈值', 'metaforge:1.0.0.common.LE_Inventory', 'metaforge:1.0.0.common.Attribute',
  '{"field":"reorder_level","type":"number","unit":"件","description":"低于该阈值触发补货"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_DailyDemand', '日均需求', 'metaforge:1.0.0.common.LE_Inventory', 'metaforge:1.0.0.common.Attribute',
  '{"field":"daily_demand","type":"number","unit":"件/天","description":"日均需求量"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_MaxStock', '最大库存', 'metaforge:1.0.0.common.LE_Inventory', 'metaforge:1.0.0.common.Attribute',
  '{"field":"max_stock","type":"number","unit":"件","description":"最大库存上限"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.3 供应商业务对象（L3 = 供应商表）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.BO_Supplier', '供应商业务对象', 'metaforge:1.0.0.common.Domain_InventoryMgmt', 'metaforge:1.0.0.common.BusinessObject',
  '{"object_type":"TABLE","logical_entity":"metaforge:1.0.0.common.LE_Supplier","description":"合格供应商表"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.LE_Supplier', '供应商逻辑实体', 'metaforge:1.0.0.common.BO_Supplier', 'metaforge:1.0.0.common.LogicalEntity',
  '{"table_name":"supplier","source":"examples/example-supply-chain-replenishment/data/suppliers/","description":"供应商目录逻辑落地"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 供应商字段（L5）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_SupplierId', '供应商ID', 'metaforge:1.0.0.common.LE_Supplier', 'metaforge:1.0.0.common.Attribute',
  '{"field":"supplier_id","type":"string","description":"供应商唯一标识"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_LeadTime', '交期', 'metaforge:1.0.0.common.LE_Supplier', 'metaforge:1.0.0.common.Attribute',
  '{"field":"lead_time_days","type":"number","unit":"天","description":"供货提前期"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_ReliabilityScore', '可靠性评分', 'metaforge:1.0.0.common.LE_Supplier', 'metaforge:1.0.0.common.Attribute',
  '{"field":"reliability_score","type":"number","unit":"分","description":"供应商历史可靠性评分(0-100)"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_UnitPrice', '单价', 'metaforge:1.0.0.common.LE_Supplier', 'metaforge:1.0.0.common.Attribute',
  '{"field":"unit_price","type":"number","unit":"元","description":"单位供货价"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 1.4 补货单业务对象（L3 = 采购单表）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.BO_PurchaseOrder', '补货单业务对象', 'metaforge:1.0.0.common.Domain_InventoryMgmt', 'metaforge:1.0.0.common.BusinessObject',
  '{"object_type":"TABLE","logical_entity":"metaforge:1.0.0.common.LE_PurchaseOrder","description":"补货采购单表"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.LE_PurchaseOrder', '补货单逻辑实体', 'metaforge:1.0.0.common.BO_PurchaseOrder', 'metaforge:1.0.0.common.LogicalEntity',
  '{"table_name":"purchase_order","description":"补货采购单逻辑落地"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 补货单字段（L5）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_OrderId', '补货单号', 'metaforge:1.0.0.common.LE_PurchaseOrder', 'metaforge:1.0.0.common.Attribute',
  '{"field":"order_id","type":"string","description":"补货单唯一编号"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_OrderQuantity', '补货数量', 'metaforge:1.0.0.common.LE_PurchaseOrder', 'metaforge:1.0.0.common.Attribute',
  '{"field":"quantity","type":"number","unit":"件","description":"补货数量（≤最大库存）"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.common.Attr_OrderStatus', '补货单状态', 'metaforge:1.0.0.common.LE_PurchaseOrder', 'metaforge:1.0.0.common.Attribute',
  '{"field":"status","type":"string","enum":["DRAFT","PLACED","RECEIVED"],"description":"补货单流转状态"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 2. 域树关系 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_SCGroupToDomain', 'SCGroupContainsDomain', 'metaforge:1.0.0.common.Group_SupplyChain', 'metaforge:1.0.0.common.Domain_InventoryMgmt', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_DomainToInventory', 'DomainContainsInventory', 'metaforge:1.0.0.common.Domain_InventoryMgmt', 'metaforge:1.0.0.common.BO_Inventory', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_DomainToSupplier', 'DomainContainsSupplier', 'metaforge:1.0.0.common.Domain_InventoryMgmt', 'metaforge:1.0.0.common.BO_Supplier', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_DomainToPurchaseOrder', 'DomainContainsPurchaseOrder', 'metaforge:1.0.0.common.Domain_InventoryMgmt', 'metaforge:1.0.0.common.BO_PurchaseOrder', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainContainsBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 库存 BO → LE → 字段
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_InvToLE', 'InventoryRefinesLE', 'metaforge:1.0.0.common.BO_Inventory', 'metaforge:1.0.0.common.LE_Inventory', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_ILEtoSku', 'ILEContainsSku', 'metaforge:1.0.0.common.LE_Inventory', 'metaforge:1.0.0.common.Attr_SkuId', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_ILEtoStock', 'ILEContainsStock', 'metaforge:1.0.0.common.LE_Inventory', 'metaforge:1.0.0.common.Attr_StockQty', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_ILEtoReorder', 'ILEContainsReorderLevel', 'metaforge:1.0.0.common.LE_Inventory', 'metaforge:1.0.0.common.Attr_ReorderLevel', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_ILEtoDemand', 'ILEContainsDemand', 'metaforge:1.0.0.common.LE_Inventory', 'metaforge:1.0.0.common.Attr_DailyDemand', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_ILEtoMax', 'ILEContainsMaxStock', 'metaforge:1.0.0.common.LE_Inventory', 'metaforge:1.0.0.common.Attr_MaxStock', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 供应商 BO → LE → 字段
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_SuppToLE', 'SupplierRefinesLE', 'metaforge:1.0.0.common.BO_Supplier', 'metaforge:1.0.0.common.LE_Supplier', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_SLEtoSuppId', 'SLEContainsSupplierId', 'metaforge:1.0.0.common.LE_Supplier', 'metaforge:1.0.0.common.Attr_SupplierId', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_SLEtoLeadTime', 'SLEContainsLeadTime', 'metaforge:1.0.0.common.LE_Supplier', 'metaforge:1.0.0.common.Attr_LeadTime', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_SLEtoReliability', 'SLEContainsReliability', 'metaforge:1.0.0.common.LE_Supplier', 'metaforge:1.0.0.common.Attr_ReliabilityScore', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_SLEtoPrice', 'SLEContainsPrice', 'metaforge:1.0.0.common.LE_Supplier', 'metaforge:1.0.0.common.Attr_UnitPrice', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 补货单 BO → LE → 字段
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_POToLE', 'PurchaseOrderRefinesLE', 'metaforge:1.0.0.common.BO_PurchaseOrder', 'metaforge:1.0.0.common.LE_PurchaseOrder', 'COMPOSITION', 'metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_POLEtoId', 'POLEContainsOrderId', 'metaforge:1.0.0.common.LE_PurchaseOrder', 'metaforge:1.0.0.common.Attr_OrderId', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_POLEtoQty', 'POLEContainsQuantity', 'metaforge:1.0.0.common.LE_PurchaseOrder', 'metaforge:1.0.0.common.Attr_OrderQuantity', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.common.Rel_POLEtoStatus', 'POLEContainsStatus', 'metaforge:1.0.0.common.LE_PurchaseOrder', 'metaforge:1.0.0.common.Attr_OrderStatus', 'COMPOSITION', 'metaforge:1.0.0.common.LogicalEntityContainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 域 → Agent / 任务
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_SCDomainToAgent', 'SCDomainComposesAgent', 'metaforge:1.0.0.common.Domain_InventoryMgmt', 'metaforge:1.0.0.agent.Agent_ReplenishmentAgent', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesAgent', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_SCDomainToTask', 'SCDomainComposesTask', 'metaforge:1.0.0.common.Domain_InventoryMgmt', 'metaforge:1.0.0.agent.Task_Replenishment', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 3. Agent / 任务 / 步骤 / 决策 实体 ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Agent_ReplenishmentAgent', '补货执行Agent', NULL, 'metaforge:1.0.0.agent.Agent',
  '{"run_mode":"AUTONOMOUS","description":"负责库存检查、补货决策与供应商选择"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_Replenishment', '库存补货任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"HIGH","estimated_complexity":"COMPLEX","dataLocation":"examples/example-supply-chain-replenishment/data/inventory/"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_ReadInventory', '读取库存', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY","capability_required":true}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_StockCheck', '补货判断决策', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"当前库存是否低于补货阈值","recommended_option":"低于阈值则生成补货单并选供应商","rationale":"库存低于阈值触发补货","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_NoReplenish', '无需补货', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"EXIT"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- 供应商选择子任务（决策嵌套）
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Task_SupplierSelection', '供应商选择子任务', NULL, 'metaforge:1.0.0.agent.Task',
  '{"delegation_depth_limit":1,"priority_default":"MEDIUM","estimated_complexity":"MODERATE"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_FetchSuppliers', '获取供应商', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"ENTRY"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.DecisionStep_SupplierPick', '供应商择优决策', NULL, 'metaforge:1.0.0.agent.DecisionStep',
  '{"condition_expression":"按可靠性评分与交期价格择优","recommended_option":"可靠性≥90 选优选供应商，否则按性价比","rationale":"高可靠性优先保障供应，否则权衡价格交期","priority":1}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_SelectPreferredSupplier', '选择优选供应商', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP","description":"可靠性≥90 的优选供应商"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_SelectBestValueSupplier', '选择性价比供应商', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP","description":"按价格与交期权衡选择"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Step_GeneratePurchaseOrder', '生成补货单', NULL, 'metaforge:1.0.0.agent.ExecutionStep',
  '{"step_type":"STEP"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 4. 规则（3 条） ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_ReorderRule', '补货触发规则', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"MANDATORY","condition":"stock_qty <= reorder_level","action":"must_generate_purchase_order","exception":null,"applicable_scenarios":["库存检查"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_SupplierPriority', '供应商优先规则', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"RECOMMENDED","condition":"reliability_score >= 90","action":"should_select_preferred_supplier","exception":"缺货紧急时按交期优先","applicable_scenarios":["供应商选择"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Rule_MaxStock', '最大库存规则', NULL, 'metaforge:1.0.0.agent.ExecutionRule',
  '{"constraint_level":"RECOMMENDED","condition":"补货后库存 <= max_stock","action":"should_cap_order_quantity","exception":null,"applicable_scenarios":["生成补货单"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 5. 能力 + 协议 ----------

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_InventoryQuery', '库存查询API', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"HTTP","endpoint":"/api/inventory/query","method":"GET","params":["sku_id"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_SupplierSelect', '供应商选择MCP', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"MCP","tool":"select_supplier","params":["sku_id","priority"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.agent.Cap_PurchaseOrderSystem', '补货单系统', NULL, 'metaforge:1.0.0.agent.Capability',
  '{"interface_spec":{"type":"HTTP","endpoint":"/api/purchase-order","method":"POST","params":["sku_id","supplier_id","quantity"]}}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.Http_InventoryQuery', '库存查询接口', NULL, 'metaforge:1.0.0.protocol.Http',
  '{"endpoint":"/api/inventory/query","method":"GET","content_type":"application/json"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.McpTool_SupplierSelect', '供应商选择工具', NULL, 'metaforge:1.0.0.protocol.McpTool',
  '{"server":"sc-mcp","tool_name":"select_supplier","input_schema":["sku_id","priority"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('metaforge:1.0.0.protocol.Http_PurchaseOrderSystem', '补货单接口', NULL, 'metaforge:1.0.0.protocol.Http',
  '{"endpoint":"/api/purchase-order","method":"POST","content_type":"application/json"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 6. 任务 → 业务对象（TaskProcessesBusinessObject） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReplenishToInventory', 'ReplenishProcessesInventory', 'metaforge:1.0.0.agent.Task_Replenishment', 'metaforge:1.0.0.common.BO_Inventory', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.TaskProcessesBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_SupplierSelToSupplier', 'SupplierSelProcessesSupplier', 'metaforge:1.0.0.agent.Task_SupplierSelection', 'metaforge:1.0.0.common.BO_Supplier', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.TaskProcessesBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_SupplierSelToPO', 'SupplierSelProcessesPO', 'metaforge:1.0.0.agent.Task_SupplierSelection', 'metaforge:1.0.0.common.BO_PurchaseOrder', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.TaskProcessesBusinessObject', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 7. 规则 → 字段（RuleConstrainsAttribute） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReorderToStock', 'ReorderConstrainsStock', 'metaforge:1.0.0.agent.Rule_ReorderRule', 'metaforge:1.0.0.common.Attr_StockQty', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReorderToLevel', 'ReorderConstrainsLevel', 'metaforge:1.0.0.agent.Rule_ReorderRule', 'metaforge:1.0.0.common.Attr_ReorderLevel', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_SupplierPriToReliability', 'SupplierPriConstrainsReliability', 'metaforge:1.0.0.agent.Rule_SupplierPriority', 'metaforge:1.0.0.common.Attr_ReliabilityScore', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_MaxStockToMax', 'MaxStockConstrainsMax', 'metaforge:1.0.0.agent.Rule_MaxStock', 'metaforge:1.0.0.common.Attr_MaxStock', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleConstrainsAttribute', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 8. 流程后继 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReplenishToRead', 'ReplenishComposesRead', 'metaforge:1.0.0.agent.Task_Replenishment', 'metaforge:1.0.0.agent.Step_ReadInventory', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReadToStockCheck', 'ReadToStockCheck', 'metaforge:1.0.0.agent.Step_ReadInventory', 'metaforge:1.0.0.agent.DecisionStep_StockCheck', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextDecisionStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 补货判断 → 供应商选择（需补货，fqn 字母序 SupplierSelection<NoReplenish 前=主路径）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StockCheckToSupplier', 'StockCheckToSupplierSel', 'metaforge:1.0.0.agent.DecisionStep_StockCheck', 'metaforge:1.0.0.agent.Task_SupplierSelection', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextTask', '{"condition":"库存≤补货阈值"}')
ON CONFLICT (fqn) DO NOTHING;

-- 补货判断 → 无需补货
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_StockCheckToNone', 'StockCheckToNoReplenish', 'metaforge:1.0.0.agent.DecisionStep_StockCheck', 'metaforge:1.0.0.agent.Step_NoReplenish', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"库存>补货阈值"}')
ON CONFLICT (fqn) DO NOTHING;

-- 供应商选择子任务内部（决策嵌套）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_SupplierSelToFetch', 'SupplierSelComposesFetch', 'metaforge:1.0.0.agent.Task_SupplierSelection', 'metaforge:1.0.0.agent.Step_FetchSuppliers', 'COMPOSITION', 'metaforge:1.0.0.agent.TaskHasEntryStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_FetchToPick', 'FetchToSupplierPick', 'metaforge:1.0.0.agent.Step_FetchSuppliers', 'metaforge:1.0.0.agent.DecisionStep_SupplierPick', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextDecisionStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- 供应商择优 → 优选（可靠性≥90，fqn 字母序 Preferred<BestValue 前=主路径）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PickToPreferred', 'PickToPreferredSupplier', 'metaforge:1.0.0.agent.DecisionStep_SupplierPick', 'metaforge:1.0.0.agent.Step_SelectPreferredSupplier', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"可靠性≥90"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PickToBestValue', 'PickToBestValueSupplier', 'metaforge:1.0.0.agent.DecisionStep_SupplierPick', 'metaforge:1.0.0.agent.Step_SelectBestValueSupplier', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.DecisionStepHasNextStep', '{"condition":"可靠性<90"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_PreferredToPO', 'PreferredToGeneratePO', 'metaforge:1.0.0.agent.Step_SelectPreferredSupplier', 'metaforge:1.0.0.agent.Step_GeneratePurchaseOrder', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_BestValueToPO', 'BestValueToGeneratePO', 'metaforge:1.0.0.agent.Step_SelectBestValueSupplier', 'metaforge:1.0.0.agent.Step_GeneratePurchaseOrder', 'PROCESS_SEQUENCE', 'metaforge:1.0.0.agent.StepHasNextStep', '{"order":1}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 9. 规则 → 步骤（RuleAppliesTo） ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReorderToRead', 'ReorderAppliesToRead', 'metaforge:1.0.0.agent.Rule_ReorderRule', 'metaforge:1.0.0.agent.Step_ReadInventory', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_SupplierPriToFetch', 'SupplierPriAppliesToFetch', 'metaforge:1.0.0.agent.Rule_SupplierPriority', 'metaforge:1.0.0.agent.Step_FetchSuppliers', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_MaxStockToPO', 'MaxStockAppliesToPO', 'metaforge:1.0.0.agent.Rule_MaxStock', 'metaforge:1.0.0.agent.Step_GeneratePurchaseOrder', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.RuleAppliesTo', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 10. 能力关联 + 协议实现 ----------

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_ReadToCapInv', 'ReadUsesInventoryCap', 'metaforge:1.0.0.agent.Step_ReadInventory', 'metaforge:1.0.0.agent.Cap_InventoryQuery', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_FetchToCapSupplier', 'FetchUsesSupplierCap', 'metaforge:1.0.0.agent.Step_FetchSuppliers', 'metaforge:1.0.0.agent.Cap_SupplierSelect', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_GenPOToCapPO', 'GenPOUsesPOSystem', 'metaforge:1.0.0.agent.Step_GeneratePurchaseOrder', 'metaforge:1.0.0.agent.Cap_PurchaseOrderSystem', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.StepUsesCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AgentToCapInv', 'AgentHasInventoryCap', 'metaforge:1.0.0.agent.Agent_ReplenishmentAgent', 'metaforge:1.0.0.agent.Cap_InventoryQuery', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AgentToCapSupplier', 'AgentHasSupplierCap', 'metaforge:1.0.0.agent.Agent_ReplenishmentAgent', 'metaforge:1.0.0.agent.Cap_SupplierSelect', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AgentToCapPO', 'AgentHasPOCap', 'metaforge:1.0.0.agent.Agent_ReplenishmentAgent', 'metaforge:1.0.0.agent.Cap_PurchaseOrderSystem', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentHasCapability', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.agent.Rel_AgentToTask', 'AgentExecutesReplenishment', 'metaforge:1.0.0.agent.Agent_ReplenishmentAgent', 'metaforge:1.0.0.agent.Task_Replenishment', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.agent.AgentExecutesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_InvToHttp', 'InvImplementsHttp', 'metaforge:1.0.0.agent.Cap_InventoryQuery', 'metaforge:1.0.0.protocol.Http_InventoryQuery', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsHttp', '{"protocol_type":"Http"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_SuppToMcp', 'SuppImplementsMcp', 'metaforge:1.0.0.agent.Cap_SupplierSelect', 'metaforge:1.0.0.protocol.McpTool_SupplierSelect', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsMcpTool', '{"protocol_type":"McpTool"}')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('metaforge:1.0.0.protocol.Rel_POToHttp', 'POImplementsHttp', 'metaforge:1.0.0.agent.Cap_PurchaseOrderSystem', 'metaforge:1.0.0.protocol.Http_PurchaseOrderSystem', 'ASSOCIATION_REFERENCE', 'metaforge:1.0.0.protocol.CapabilityImplementsHttp', '{"protocol_type":"Http"}')
ON CONFLICT (fqn) DO NOTHING;

-- ---------- 11. 实体关系索引（供应链新关系） ----------

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT source_entity_fqn, 'OUTBOUND', fqn FROM semantic_relation_network.relation_instance
WHERE fqn LIKE 'metaforge:1.0.0.common.Rel_SC%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_DomainTo%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_Inv%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_ILE%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_Supp%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_SLE%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_PO%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_POLE%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_Replenish%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_SupplierSel%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_Reorder%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_SupplierPri%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_MaxStock%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReadTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_StockCheckTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_FetchTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_PickTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_PreferredTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_BestValueTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_GenPOTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_AgentTo%' OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_InvTo%'
   OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_SuppTo%' OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_POTo%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT target_entity_fqn, 'INBOUND', fqn FROM semantic_relation_network.relation_instance
WHERE fqn LIKE 'metaforge:1.0.0.common.Rel_SC%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_DomainTo%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_Inv%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_ILE%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_Supp%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_SLE%'
   OR fqn LIKE 'metaforge:1.0.0.common.Rel_PO%' OR fqn LIKE 'metaforge:1.0.0.common.Rel_POLE%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_Replenish%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_SupplierSel%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_Reorder%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_SupplierPri%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_MaxStock%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_ReadTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_StockCheckTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_FetchTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_PickTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_PreferredTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_BestValueTo%' OR fqn LIKE 'metaforge:1.0.0.agent.Rel_GenPOTo%'
   OR fqn LIKE 'metaforge:1.0.0.agent.Rel_AgentTo%' OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_InvTo%'
   OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_SuppTo%' OR fqn LIKE 'metaforge:1.0.0.protocol.Rel_POTo%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

-- ---------- 12. 实体描述补充 ----------

UPDATE metadata_management.metadata_entity SET description = '供应链域组——供应链与库存管理域分组' WHERE fqn = 'metaforge:1.0.0.common.Group_SupplyChain';
UPDATE metadata_management.metadata_entity SET description = '库存管理域——供应链库存补货与供应商管理' WHERE fqn = 'metaforge:1.0.0.common.Domain_InventoryMgmt';
UPDATE metadata_management.metadata_entity SET description = '库存业务对象——商品库存快照表' WHERE fqn = 'metaforge:1.0.0.common.BO_Inventory';
UPDATE metadata_management.metadata_entity SET description = '供应商业务对象——合格供应商表' WHERE fqn = 'metaforge:1.0.0.common.BO_Supplier';
UPDATE metadata_management.metadata_entity SET description = '补货单业务对象——补货采购单表' WHERE fqn = 'metaforge:1.0.0.common.BO_PurchaseOrder';
UPDATE metadata_management.metadata_entity SET description = '补货执行Agent——负责库存检查、补货决策与供应商选择' WHERE fqn = 'metaforge:1.0.0.agent.Agent_ReplenishmentAgent';
UPDATE metadata_management.metadata_entity SET description = '库存补货任务——读取库存→补货判断→供应商择优→生成补货单' WHERE fqn = 'metaforge:1.0.0.agent.Task_Replenishment';
UPDATE metadata_management.metadata_entity SET description = '补货判断决策——库存≤补货阈值则触发补货' WHERE fqn = 'metaforge:1.0.0.agent.DecisionStep_StockCheck';
UPDATE metadata_management.metadata_entity SET description = '供应商选择子任务——获取供应商→择优→生成补货单' WHERE fqn = 'metaforge:1.0.0.agent.Task_SupplierSelection';
UPDATE metadata_management.metadata_entity SET description = '供应商择优决策——可靠性≥90 选优选，否则按性价比' WHERE fqn = 'metaforge:1.0.0.agent.DecisionStep_SupplierPick';
UPDATE metadata_management.metadata_entity SET description = '补货触发规则——库存≤补货阈值必须生成补货单' WHERE fqn = 'metaforge:1.0.0.agent.Rule_ReorderRule';
UPDATE metadata_management.metadata_entity SET description = '供应商优先规则——可靠性≥90 优先选择' WHERE fqn = 'metaforge:1.0.0.agent.Rule_SupplierPriority';
UPDATE metadata_management.metadata_entity SET description = '最大库存规则——补货后库存不超过最大库存' WHERE fqn = 'metaforge:1.0.0.agent.Rule_MaxStock';

-- ---------- 完成 ----------
