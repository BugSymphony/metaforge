-- ============================================================================
-- cognition-test seed 数据准备 (幂等)
-- 供 cognition-test.sh --seed 调用: 确保 erp/order 两个测试 Bundle 及其
-- 包/EntitySchema/RelationSchema/M1实例/关系实例 齐全。
-- 所有 INSERT 均使用 ON CONFLICT DO NOTHING, 可重复执行。
-- 要求: 每个元数据(实例/Schema/Bundle/关系) 均带 name + description + 属性。
-- ============================================================================

-- ============================================================
-- 1. Bundle
-- ============================================================
INSERT INTO metamodel_governance.bundle (fqn, name, description, owner, is_system) VALUES
  ('erp',   '企业资源计划', '含订单/库存/采购的复杂业务域', 'lisi', FALSE),
  ('order', '订单领域',     '订单/商品语义域', 'system', FALSE)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 2. BundleVersion (PUBLISHED)
-- ============================================================
INSERT INTO metamodel_governance.bundle_version (fqn, bundle_fqn, status, source_version_fqn, upgrade_level) VALUES
  ('erp:1.0.0',   'erp',   'PUBLISHED', NULL, NULL),
  ('order:1.0.0', 'order', 'PUBLISHED', NULL, NULL)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 3. Package
-- ============================================================
INSERT INTO metamodel_governance.package (fqn, bundle_version_fqn, parent_package_fqn, description, depth) VALUES
  ('erp:1.0.0.pkg_core',  'erp:1.0.0', NULL, 'ERP 核心域: 客户/供应商主数据', 0),
  ('erp:1.0.0.pkg_sales', 'erp:1.0.0', NULL, 'ERP 销售域: 订单/订单项/仓库', 0),
  ('order:1.0.0.pkg_order', 'order:1.0.0', NULL, '订单语义域包', 0)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 4. EntitySchema (每个都带 native_attributes 属性定义)
-- ============================================================
INSERT INTO metamodel_governance.entity_schema
  (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes) VALUES
('erp:1.0.0.pkg_core.Customer', 'erp:1.0.0.pkg_core', 'erp:1.0.0', '客户', '客户主数据 Schema',
 '[{"name":"customerCode","type":"string","required":true,"description":"客户编码","constraints":{"pattern":"^C-[0-9]{4,}$"}},{"name":"name","type":"string","required":true,"description":"客户名称"},{"name":"vipLevel","type":"string","required":true,"description":"会员等级","constraints":{"enum":["GOLD","SILVER","BRONZE"]}}]'),
('erp:1.0.0.pkg_core.Supplier', 'erp:1.0.0.pkg_core', 'erp:1.0.0', '供应商', '供应商主数据 Schema',
 '[{"name":"supplierCode","type":"string","required":true,"description":"供应商编码","constraints":{"pattern":"^S-[0-9]{4,}$"}},{"name":"name","type":"string","required":true,"description":"供应商名称"}]'),
('erp:1.0.0.pkg_sales.Order', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', '订单', '销售订单 Schema',
 '[{"name":"orderNo","type":"string","required":true,"description":"订单号","constraints":{"pattern":"^SO-[0-9]{6,}$"}},{"name":"status","type":"string","required":true,"description":"订单状态","constraints":{"enum":["active","cancelled","shipped"]}},{"name":"amount","type":"number","required":true,"description":"订单金额","constraints":{"minimum":0}}]'),
('erp:1.0.0.pkg_sales.OrderItem', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', '订单项', '订单明细行 Schema',
 '[{"name":"sku","type":"string","required":true,"description":"SKU 编码","constraints":{"pattern":"^[A-Z]{2}-[0-9]{4,}$"}},{"name":"quantity","type":"integer","required":true,"description":"数量","constraints":{"minimum":1}}]'),
('erp:1.0.0.pkg_sales.Warehouse', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', '仓库', '仓库主数据 Schema',
 '[{"name":"warehouseCode","type":"string","required":true,"description":"仓库编码","constraints":{"pattern":"^WH-[0-9]{3,}$"}},{"name":"capacity","type":"integer","required":true,"description":"容量","constraints":{"minimum":100}}]'),
('order:1.0.0.pkg_order.Order', 'order:1.0.0.pkg_order', 'order:1.0.0', '订单', '订单实体 Schema',
 '[{"name":"status","type":"string","required":true,"description":"订单状态","constraints":{"enum":["active","inactive","shipped","cancelled"]}},{"name":"price","type":"number","required":false,"description":"订单金额","constraints":{"minimum":0}},{"name":"quantity","type":"integer","required":false,"description":"数量","constraints":{"minimum":0}}]'),
('order:1.0.0.pkg_order.Item', 'order:1.0.0.pkg_order', 'order:1.0.0', '商品', '商品实体 Schema',
 '[{"name":"status","type":"string","required":true,"description":"商品状态","constraints":{"enum":["active","inactive","shipped","cancelled"]}},{"name":"price","type":"number","required":false,"description":"单价","constraints":{"minimum":0}},{"name":"quantity","type":"integer","required":false,"description":"库存","constraints":{"minimum":0}}]')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 5. RelationSchema (用于 relation_instance 的 relation_schema_fqn)
-- ============================================================
INSERT INTO metamodel_governance.relation_schema
  (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target) VALUES
('erp:1.0.0.COMPOSITION', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', 'COMPOSITION', '组成关系', 'erp:1.0.0.pkg_sales.Order', 'erp:1.0.0.pkg_sales.OrderItem', 'COMPOSITION', '1', 'N'),
('erp:1.0.0.DEPENDENCY_INFLUENCE', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', 'DEPENDENCY_INFLUENCE', '依赖影响关系', 'erp:1.0.0.pkg_sales.Order', 'erp:1.0.0.pkg_core.Supplier', 'DEPENDENCY_INFLUENCE', 'N', 'N'),
('erp:1.0.0.ASSOCIATION_REFERENCE', 'erp:1.0.0.pkg_core', 'erp:1.0.0', 'ASSOCIATION_REFERENCE', '关联引用关系', 'erp:1.0.0.pkg_core.Customer', 'erp:1.0.0.pkg_sales.Order', 'ASSOCIATION_REFERENCE', 'N', 'N'),
('erp:1.0.0.PROCESS_SEQUENCE', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', 'PROCESS_SEQUENCE', '流程先后关系', 'erp:1.0.0.pkg_sales.Order', 'erp:1.0.0.pkg_sales.Order', 'PROCESS_SEQUENCE', 'N', 'N'),
('erp:1.0.0.MAPPING_CORRESPONDENCE', 'erp:1.0.0.pkg_sales', 'erp:1.0.0', 'MAPPING_CORRESPONDENCE', '映射对应关系', 'erp:1.0.0.pkg_sales.OrderItem', 'erp:1.0.0.pkg_sales.Warehouse', 'MAPPING_CORRESPONDENCE', 'N', 'N'),
('order:1.0.0.COMPOSITION', 'order:1.0.0.pkg_order', 'order:1.0.0', 'COMPOSITION', '组成关系', 'order:1.0.0.pkg_order.Order', 'order:1.0.0.pkg_order.Item', 'COMPOSITION', '1', 'N'),
('order:1.0.0.DEPENDENCY_INFLUENCE', 'order:1.0.0.pkg_order', 'order:1.0.0', 'DEPENDENCY_INFLUENCE', '依赖影响关系', 'order:1.0.0.pkg_order.Order', 'order:1.0.0.pkg_order.Item', 'DEPENDENCY_INFLUENCE', 'N', 'N'),
('order:1.0.0.ASSOCIATION_REFERENCE', 'order:1.0.0.pkg_order', 'order:1.0.0', 'ASSOCIATION_REFERENCE', '关联引用关系', 'order:1.0.0.pkg_order.Item', 'order:1.0.0.pkg_order.Order', 'ASSOCIATION_REFERENCE', 'N', 'N'),
('order:1.0.0.PROCESS_SEQUENCE', 'order:1.0.0.pkg_order', 'order:1.0.0', 'PROCESS_SEQUENCE', '流程先后关系', 'order:1.0.0.pkg_order.Order', 'order:1.0.0.pkg_order.Order', 'PROCESS_SEQUENCE', 'N', 'N')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 6. M1 实例 (metadata_entity) — 每个均含 name/description/content
--    erp 用裸 FQN, order 用版本化 FQN
-- ============================================================
INSERT INTO metadata_management.metadata_entity
  (fqn, name, description, entity_schema_fqn, content, current_version, created_by, updated_by) VALUES
('Customer_001', '张三', '黄金会员客户', 'erp:1.0.0.pkg_core.Customer', '{"name":"张三","vipLevel":"GOLD","customerCode":"C-1001"}', 1, 'system', 'system'),
('Customer_002', '李四', '白银会员客户', 'erp:1.0.0.pkg_core.Customer', '{"name":"李四","vipLevel":"SILVER","customerCode":"C-1002"}', 1, 'system', 'system'),
('Supplier_001', '原料供应商', '华东区原材料供应商', 'erp:1.0.0.pkg_core.Supplier', '{"name":"原料供应商","supplierCode":"S-2001"}', 1, 'system', 'system'),
('Order_100', '订单100', '金额1999.5的活跃订单', 'erp:1.0.0.pkg_sales.Order', '{"amount":1999.5,"status":"active","orderNo":"SO-100001"}', 1, 'system', 'system'),
('Order_200', '订单200', '已发货订单', 'erp:1.0.0.pkg_sales.Order', '{"amount":899.0,"status":"shipped","orderNo":"SO-100002"}', 1, 'system', 'system'),
('OrderItem_101', '订单项101', '订单100的明细行', 'erp:1.0.0.pkg_sales.OrderItem', '{"sku":"AB-12345","quantity":3}', 1, 'system', 'system'),
('OrderItem_102', '订单项102', '订单200的明细行', 'erp:1.0.0.pkg_sales.OrderItem', '{"sku":"CD-67890","quantity":5}', 1, 'system', 'system'),
('Warehouse_301', '华东一号仓', '容量5000的主力仓库', 'erp:1.0.0.pkg_sales.Warehouse', '{"capacity":5000,"warehouseCode":"WH-301"}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Order_001', '订单1', '主订单', 'order:1.0.0.pkg_order.Order', '{"price":1999.5,"status":"active"}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Order_002', '订单2', '履约中', 'order:1.0.0.pkg_order.Order', '{"price":899.0,"status":"shipped"}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Order_003', '订单3', '促销订单', 'order:1.0.0.pkg_order.Order', '{"price":2400.0,"status":"active"}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_003', '商品3', '电子品类', 'order:1.0.0.pkg_order.Item', '{"price":150.0,"status":"active","quantity":3}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_004', '商品4', '图书品类', 'order:1.0.0.pkg_order.Item', '{"price":80.0,"status":"active","quantity":5}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_005', '商品5', '服装品类', 'order:1.0.0.pkg_order.Item', '{"price":50.0,"status":"cancelled","quantity":2}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_006', '商品6', '家居品类', 'order:1.0.0.pkg_order.Item', '{"price":320.0,"status":"active","quantity":1}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_007', '商品7', '美妆品类', 'order:1.0.0.pkg_order.Item', '{"price":210.0,"status":"active","quantity":4}', 1, 'system', 'system'),
('order:1.0.0.pkg_order.Item_008', '商品8', '食品品类', 'order:1.0.0.pkg_order.Item', '{"price":30.0,"status":"inactive","quantity":9}', 1, 'system', 'system')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 7. 关系实例 (relation_instance) — 每个均带 name/description/content
-- ============================================================
INSERT INTO semantic_relation_network.relation_instance
  (fqn, name, description, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content, current_version) VALUES
('Customer_001#erp:1.0.0.ASSOCIATION_REFERENCE#Order_100', '客户1下单100', '客户张三关联订单100', 'Customer_001', 'Order_100', 'ASSOCIATION_REFERENCE', 'erp:1.0.0.ASSOCIATION_REFERENCE', '{"source":"direct"}', 1),
('Order_100#erp:1.0.0.COMPOSITION#OrderItem_101', '订单100包含条目101', '订单100 组成含订单项101', 'Order_100', 'OrderItem_101', 'COMPOSITION', 'erp:1.0.0.COMPOSITION', '{"note":"final","quantity":9,"unitPrice":499.0}', 1),
('Order_100#erp:1.0.0.DEPENDENCY_INFLUENCE#Supplier_001', '100依赖供应商1', '订单100 依赖供应商001', 'Order_100', 'Supplier_001', 'DEPENDENCY_INFLUENCE', 'erp:1.0.0.DEPENDENCY_INFLUENCE', '{"level":"HIGH"}', 1),
('Order_100#erp:1.0.0.PROCESS_SEQUENCE#Order_200', '100后处理200', '订单100 后置处理订单200', 'Order_100', 'Order_200', 'PROCESS_SEQUENCE', 'erp:1.0.0.PROCESS_SEQUENCE', '{"step":1}', 1),
('OrderItem_101#erp:1.0.0.MAPPING_CORRESPONDENCE#Warehouse_301', '条目101入仓301', '订单项101 映射仓库301', 'OrderItem_101', 'Warehouse_301', 'MAPPING_CORRESPONDENCE', 'erp:1.0.0.MAPPING_CORRESPONDENCE', '{"bin":"A-03"}', 1),
('order:1.0.0.pkg_order.Order_001#order:1.0.0.COMPOSITION#order:1.0.0.pkg_order.Item_003', '订单1包含商品3', '订单1 组成含商品3', 'order:1.0.0.pkg_order.Order_001', 'order:1.0.0.pkg_order.Item_003', 'COMPOSITION', 'order:1.0.0.COMPOSITION', '{"quantity":3}', 1),
('order:1.0.0.pkg_order.Order_001#order:1.0.0.DEPENDENCY_INFLUENCE#order:1.0.0.pkg_order.Item_005', '订单1依赖商品5', '订单1 依赖商品5', 'order:1.0.0.pkg_order.Order_001', 'order:1.0.0.pkg_order.Item_005', 'DEPENDENCY_INFLUENCE', 'order:1.0.0.DEPENDENCY_INFLUENCE', '{"level":"HIGH"}', 1),
('order:1.0.0.pkg_order.Order_001#order:1.0.0.PROCESS_SEQUENCE#order:1.0.0.pkg_order.Order_002', '订单1后置订单2', '订单1 后置订单2', 'order:1.0.0.pkg_order.Order_001', 'order:1.0.0.pkg_order.Order_002', 'PROCESS_SEQUENCE', 'order:1.0.0.PROCESS_SEQUENCE', '{"step":1}', 1),
('order:1.0.0.pkg_order.Order_002#order:1.0.0.COMPOSITION#order:1.0.0.pkg_order.Item_004', '订单2包含商品4', '订单2 组成含商品4', 'order:1.0.0.pkg_order.Order_002', 'order:1.0.0.pkg_order.Item_004', 'COMPOSITION', 'order:1.0.0.COMPOSITION', '{"quantity":5}', 1),
('order:1.0.0.pkg_order.Order_003#order:1.0.0.COMPOSITION#order:1.0.0.pkg_order.Item_005', '订单3包含商品5', '订单3 组成含商品5', 'order:1.0.0.pkg_order.Order_003', 'order:1.0.0.pkg_order.Item_005', 'COMPOSITION', 'order:1.0.0.COMPOSITION', '{"quantity":2}', 1)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 8. 重建实体双向索引 (entity_relation_index)
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
